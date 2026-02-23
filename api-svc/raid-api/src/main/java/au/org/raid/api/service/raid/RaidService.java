package au.org.raid.api.service.raid;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.dto.OrganisationCountDto;
import au.org.raid.api.dto.RaidCountDto;
import au.org.raid.api.dto.RaidPermissionsDto;
import au.org.raid.api.dto.ServicePointCountDto;
import au.org.raid.api.exception.InvalidVersionException;
import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.exception.ServicePointNotFoundException;
import au.org.raid.api.exception.UnknownServicePointException;
import au.org.raid.api.factory.HandleFactory;
import au.org.raid.api.factory.IdFactory;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.api.repository.ServicePointRepository;
import au.org.raid.api.service.*;
import au.org.raid.api.service.datacite.DataciteService;
import au.org.raid.api.service.keycloak.KeycloakService;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.util.TokenUtil;
import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.RaidCreateRequest;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static au.org.raid.api.util.TokenUtil.OPERATOR_ROLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class RaidService {
    private static final int MAX_MINT_RETRIES = 2;
    private static final String SERVICE_POINT_USER_ROLE = "ROLE_service-point-user";
    private static final String RAID_USER_ROLE = "ROLE_raid-user";
    private static final String RAID_ADMIN_ROLE = "ROLE_raid-admin";
    public static final String SERVICE_POINT_GROUP_ID_CLAIM = "service_point_group_id";

    private final DataciteService dataciteSvc;
    private final ServicePointRepository servicePointRepository;
    private final IdFactory idFactory;
    private final RaidChecksumService checksumService;
    private final RaidHistoryService raidHistoryService;
    private final RaidIngestService raidIngestService;
    private final HandleFactory handleFactory;
    private final ContributorService contributorService;
    private final KeycloakService keycloakService;
    private final OrcidIntegrationService orcidIntegrationService;
    private final ObjectMapper objectMapper;

    private final RaidRepository raidRepository;
    private final RorClient rorClient;

    @Transactional
    public RaidDto mint(
            final RaidCreateRequest raid,
            final long servicePointId
    ) {
        final var servicePointRecord =
                servicePointRepository.findById(servicePointId).orElseThrow(() ->
                        new UnknownServicePointException(servicePointId));

        mintHandle(raid, servicePointRecord, 0);

        orcidIntegrationService.setContributorStatus(raid.getContributor());

        final var raidDto = raidHistoryService.save(raid);
        raidIngestService.create(raidDto);

        keycloakService.addHandleToAdminRaids(new Handle(raidDto.getIdentifier().getId()).toString());

        orcidIntegrationService.updateOrcidRecord(raidDto);
        return raidDto;
    }

    private void mintHandle(final RaidCreateRequest request, final ServicePointRecord servicePointRecord, int mintRetries) {
        try {
            final var handle = handleFactory.createWithPrefix(servicePointRecord.getPrefix());
            request.setIdentifier(idFactory.create(handle.toString(), servicePointRecord));
            dataciteSvc.mint(request, handle.toString(), servicePointRecord.getRepositoryId(), servicePointRecord.getPassword());
        } catch (final HttpClientErrorException e) {
            if (mintRetries < MAX_MINT_RETRIES && e.getStatusCode().equals(HttpStatusCode.valueOf(422))) {
                mintRetries++;
                log.info("Re-attempting mint of raid in Datacite. Retry {} of {}", mintRetries, MAX_MINT_RETRIES, e);
                mintHandle(request, servicePointRecord, mintRetries);
            } else {
                throw e;
            }
        }
    }

    @SneakyThrows
    @Transactional
    public RaidDto update(final RaidUpdateRequest raid, final long userServicePointId) {
        final var raidServicePointId = raid.getIdentifier().getOwner().getServicePoint();

        if (!TokenUtil.hasRole(OPERATOR_ROLE) && raidServicePointId != userServicePointId) {
            throw new IllegalAccessException("User service point id (%d) does not match raid service point id (%d)"
                    .formatted(userServicePointId, raidServicePointId));
        }

        final var servicePointRecord =
                servicePointRepository.findById(raidServicePointId).orElseThrow(() ->
                        new UnknownServicePointException(raid.getIdentifier().getOwner().getServicePoint()));

        final Integer version = raid.getIdentifier().getVersion();

        if (version == null) {
            throw new InvalidVersionException(version);
        }

        final var handle = new Handle(raid.getIdentifier().getId()).toString();

        final var raidString = raidHistoryService.findByHandleAndVersion(handle, version)
                .orElseThrow(() -> new ResourceNotFoundException(handle));

        final var existing = objectMapper.readValue(raidString, RaidDto.class);

        final var existingChecksum = checksumService.fromRaidDto(existing);
        final var updateChecksum = checksumService.fromRaidUpdateRequest(raid);

        if (updateChecksum.equals(existingChecksum)) {
            return existing;
        }

        contributorService.setStatus(raid.getContributor());

        orcidIntegrationService.setContributorStatus(raid.getContributor());

        final var raidDto = raidHistoryService.save(raid);

        dataciteSvc.update(raid, handle, servicePointRecord.getRepositoryId(), servicePointRecord.getPassword());

        final var saved =  raidIngestService.update(raidDto);
        orcidIntegrationService.updateOrcidRecord(saved);
        return saved;
    }

    @Transactional
    public RaidDto patchContributors(final String prefix, final String suffix, final List<Contributor> contributors) {
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var raid = raidHistoryService.findByHandle(handle)
                .orElseThrow(() -> new ResourceNotFoundException(handle));

        final var servicePointId = raid.getIdentifier().getOwner().getServicePoint();

        final var servicePointRecord = servicePointRepository.findById(servicePointId)
                .orElseThrow(() -> new ServicePointNotFoundException(servicePointId));

        orcidIntegrationService.setContributorStatus(contributors);
        raid.setContributor(contributors);

        raidHistoryService.save(raid);
        dataciteSvc.update(raid, handle, servicePointRecord.getRepositoryId(), servicePointRecord.getPassword());

        final var saved = raidIngestService.update(raid);

        orcidIntegrationService.updateOrcidRecord(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<RaidDto> findByHandle(String handle) {
        final var optional = raidHistoryService.findByHandle(handle);

        if (optional.isPresent()) {
            return optional;
        }

        return raidIngestService.findByHandle(handle);
    }

    public Optional<RaidPermissionsDto> getPermissions(final String prefix, final String suffix) {
        final var handle = "%s/%s".formatted(prefix, suffix);

        final var raidOptional = raidHistoryService.findByHandle(handle);

        if (raidOptional.isEmpty()) {
            return Optional.empty();
        }

        var servicePointMatch = false;
        var canWrite = false;
        var canRead = raidOptional.get().getAccess().getType().getId().equals(SchemaValues.ACCESS_TYPE_OPEN.getUri());

        final var token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();

        final var groupId = (String) token.getClaims().get(SERVICE_POINT_GROUP_ID_CLAIM);

        final var servicePoint = servicePointRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ServicePointNotFoundException(groupId));

        final var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (raidOptional.get().getIdentifier().getOwner().getServicePoint().equals(servicePoint.getId())) {
            servicePointMatch = true;
        }

        if (authorities.contains(SERVICE_POINT_USER_ROLE)) {
            if (servicePointMatch) {
                return Optional.of(RaidPermissionsDto.builder()
                        .servicePointMatch(servicePointMatch)
                        .read(true)
                        .write(true)
                        .build());
            }
        }

        if (authorities.contains(RAID_USER_ROLE)) {
            final var userRaids = token.getClaims().get("user_raids");

            if (userRaids instanceof List) {
                canRead = (canRead) ? canRead : ((List<?>) userRaids).contains(handle);
                canWrite = ((List<?>) userRaids).contains(handle);
            }
        }

        if (authorities.contains(RAID_ADMIN_ROLE)) {
            final var adminRaids = token.getClaims().get("admin_raids");
            if (adminRaids instanceof List) {
                canRead = (canRead) ? canRead : ((List<?>) adminRaids).contains(handle);
                canWrite = (canWrite) ? canWrite : ((List<?>) adminRaids).contains(handle);
            }
        }

        return Optional.of(RaidPermissionsDto.builder()
                .servicePointMatch(servicePointMatch)
                .read(canRead)
                .write(canWrite)
                .build());
    }

    public List<RaidDto> findAllPublic() {
        final var raidRecords = raidRepository.findAllPublic();
        final var raids = new ArrayList<RaidDto>();

        for (final var record : raidRecords) {
            final var raidDto = raidHistoryService.findByHandle(record.getHandle())
                    .orElseThrow(() -> new ResourceNotFoundException(record.getHandle()));
            raids.add(raidDto);
        }

        return raids;
    }

    public List<RaidDto> findAllNonLegacy() {
        final var raidRecords = raidRepository.findAllNonLegacyRaids();
        final var raids = new ArrayList<RaidDto>();

        for (final var record : raidRecords) {
            final var raidDto = raidHistoryService.findByHandle(record.getHandle())
                    .orElseThrow(() -> new ResourceNotFoundException(record.getHandle()));
            raids.add(raidDto);
        }

        return raids;
    }

    @Transactional(readOnly = true)
    public RaidCountDto countRaids(final Long servicePointId,
                                   final LocalDate startDate,
                                   final LocalDate endDate) {
        final var startDateTime = startDate != null
                ? startDate.atStartOfDay()
                : null;
        final var endDateTime = endDate != null
                ? endDate.plusDays(1).atStartOfDay()
                : null;

        final int count = raidRepository.countByFilters(servicePointId, startDateTime, endDateTime);

        String servicePointName = null;
        if (servicePointId != null) {
            servicePointName = servicePointRepository.findById(servicePointId)
                    .map(ServicePointRecord::getName)
                    .orElse(null);
        }

        final var rows = raidRepository.countByOrganisationAndServicePoint(
                servicePointId, startDateTime, endDateTime);

        // Group rows by org PID, preserving insertion order
        final var orgMap = new LinkedHashMap<String, List<ServicePointCountDto>>();
        for (final var row : rows) {
            final var orgPid = row.value1();
            final var spId = row.value2();
            final var spName = row.value3();
            final var spCount = row.value4();

            orgMap.computeIfAbsent(orgPid, k -> new ArrayList<>())
                    .add(ServicePointCountDto.builder()
                            .id(spId)
                            .name(spName)
                            .count(spCount)
                            .build());
        }

        final var organisations = orgMap.entrySet().stream()
                .map(entry -> {
                    final var orgPid = entry.getKey();
                    final var servicePoints = entry.getValue();
                    final var orgCount = servicePoints.stream()
                            .mapToLong(ServicePointCountDto::getCount)
                            .sum();

                    String name = null;
                    try {
                        name = rorClient.getOrganisationName(orgPid);
                    } catch (Exception e) {
                        log.warn("Failed to resolve organisation name for {}", orgPid, e);
                    }

                    return OrganisationCountDto.builder()
                            .id(orgPid)
                            .name(name)
                            .count(orgCount)
                            .servicePoints(servicePoints)
                            .build();
                })
                .toList();

        return RaidCountDto.builder()
                .count(count)
                .servicePointId(servicePointId)
                .servicePointName(servicePointName)
                .startDate(startDate)
                .endDate(endDate)
                .organisations(organisations)
                .build();
    }

    public void postToDatacite(@Valid RaidDto raid) {
        final var handle = new Handle(raid.getIdentifier().getId());

        //TODO: Check prefix

        final var servicePointId = raid.getIdentifier().getOwner().getServicePoint();

        final var servicePointRecord = servicePointRepository.findById(servicePointId)
                .orElseThrow(() -> new ServicePointNotFoundException(servicePointId));

        dataciteSvc.update(raid, handle.toString(), servicePointRecord.getRepositoryId(), servicePointRecord.getPassword());
    }
}