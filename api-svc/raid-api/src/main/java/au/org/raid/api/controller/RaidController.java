package au.org.raid.api.controller;

import au.org.raid.api.dto.RaidPermissionsDto;
import au.org.raid.api.exception.ServicePointNotFoundException;
import au.org.raid.api.exception.ValidationException;
import au.org.raid.api.model.datacite.factory.ResearchProjectFactory;
import au.org.raid.api.model.schemaorg.ResearchProject;
import au.org.raid.api.service.RaidHistoryService;
import au.org.raid.api.service.RaidIngestService;
import au.org.raid.api.service.ServicePointService;
import au.org.raid.api.service.raid.RaidService;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.util.TokenUtil;
import au.org.raid.api.validator.ValidationService;
import au.org.raid.idl.raidv2.api.RaidApi;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@CrossOrigin
@RestController
@RequiredArgsConstructor
@SecurityScheme(name = "bearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class RaidController implements RaidApi {
    public static final String SERVICE_POINT_GROUP_ID_CLAIM = "service_point_group_id";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String SERVICE_POINT_USER_ROLE = "service-point-user";

    private final ValidationService validationService;
    private final RaidService raidService;
    private final RaidIngestService raidIngestService;
    private final RaidHistoryService raidHistoryService;
    private final ServicePointService servicePointService;
    private final ObjectMapper objectMapper;
    private final ResearchProjectFactory researchProjectFactory;

    @Override
    @SneakyThrows
    public ResponseEntity<RaidDto> findRaidByName(final String prefix, final String suffix) {
        final var handle = String.join("/", prefix, suffix);
        var raidOptional = raidService.findByHandle(handle)
                .or(Optional::empty);

        return ResponseEntity.of(raidOptional);
    }


    @Override
    @SneakyThrows
    public ResponseEntity<Object> findRaidByNameAndVersion(final String prefix, final String suffix, final Integer version) {
        final var handle = String.join("/", prefix, suffix);
        var raidOptional = raidHistoryService.findByHandleAndVersion(handle, version);

        if (raidOptional.isPresent()) {
            return ResponseEntity.ok(objectMapper.writeValueAsString(raidOptional.get()));
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<RaidDto> mintRaid(final RaidCreateRequest request) {
        final var servicePointId = getServicePointId();

        final var failures = new ArrayList<>(validationService.validateForCreate(request));

        if (!failures.isEmpty()) {
            throw new ValidationException(failures);
        }

        final var raidDto = raidService.mint(request, servicePointId);

        return ResponseEntity.created(URI.create(raidDto.getIdentifier().getId())).body(raidDto);
    }

    @Override
    public ResponseEntity<List<RaidDto>> findAllRaids(final List<String> includeFields, final String contributorId, final String organisationId) {
        //TODO: filter for service point owner/raid admin/raid user if embargoed
        List<RaidDto> raids;

        if (contributorId != null) {
            raids = raidIngestService.findAllByContributor(contributorId);

        } else if (organisationId != null) {
            raids = raidIngestService.findAllByOrganisation(organisationId);

        } else {
            final var servicePointId = getServicePointId();

            raids = raidIngestService.findAllByServicePointIdOrHandleIn(servicePointId)
                    .stream().filter(this::isViewable)
                    .toList();
        }

        if (includeFields != null && !includeFields.isEmpty()) {
            return ResponseEntity.ok(filterFields(raids, includeFields));
        }

        return ResponseEntity.ok(raids);
    }

    @GetMapping("/schemaorg")
    public ResponseEntity<List<ResearchProject>> findAllRaids() {
        //TODO: filter for service point owner/raid admin/raid user if embargoed
        List<ResearchProject> raids;

        final var servicePointId = getServicePointId();

        raids = raidIngestService.findAllByServicePointIdOrHandleIn(servicePointId)
                .stream().filter(this::isViewable)
                .map(researchProjectFactory::create)
                .toList();

        return ResponseEntity.ok(raids);
    }

    @Override
    public ResponseEntity<RaidDto> updateRaid(final String prefix, final String suffix, RaidUpdateRequest request) {
        final var handle = String.join("/", prefix, suffix);

        final var failures = new ArrayList<>(validationService.validateForUpdate(handle, request));

        if (!failures.isEmpty()) {
            throw new ValidationException(failures);
        }

        return ResponseEntity.ok(raidService.update(request));
    }

    @Override
    public ResponseEntity<List<RaidChange>> raidHistory(final String prefix, final String suffix) {

        final var handle = prefix + "/" + suffix;

        return ResponseEntity.ok(raidHistoryService.findAllChangesByHandle(handle));
    }

    @GetMapping("/raid/{prefix}/{suffix}/permissions")
    public ResponseEntity<RaidPermissionsDto> permissions(@PathVariable final String prefix,
                                                          @PathVariable final String suffix) {

        return ResponseEntity.of(raidService.getPermissions(prefix, suffix));
    }

    @Override
    public ResponseEntity<RaidDto> patchRaid(final String prefix, final String suffix, final RaidPatchRequest request) {

        final var failures = new ArrayList<>(validationService.validateForPatch(request));

        if (!failures.isEmpty()) {
            throw new ValidationException(failures);
        }

        return ResponseEntity.ok(
                raidService.patchContributors(prefix, suffix, request.getContributor()));
    }


    @Override
    public ResponseEntity<List<RaidDto>> findAllPublicRaids() {
        return ResponseEntity.ok(raidService.findAllPublic());
    }

    private long getServicePointId() {

        final var token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();
        final var groupId = (String) token.getClaims().get(SERVICE_POINT_GROUP_ID_CLAIM);

        final var servicePoint = servicePointService.findByGroupId(groupId)
                .orElseThrow(() -> new ServicePointNotFoundException(groupId));

        return servicePoint.getId();
    }

    private List<RaidDto> filterFields(final List<RaidDto> raids, final List<String> includeFields) {
        final var filteredList = new ArrayList<RaidDto>();

        for (final var raid : raids) {
            filteredList.add(filterRaid(raid, includeFields));
        }

        return filteredList;
    }

    @SneakyThrows
    private RaidDto filterRaid(final RaidDto raidDto, final List<String> includeFields) {
        final var filtered = new RaidDto();

        for (final var fieldName : includeFields) {
            final var getter = "get%s".formatted(StringUtils.capitalize(fieldName));
            final var setter = "set%s".formatted(StringUtils.capitalize(fieldName));

            final var method = raidDto.getClass().getMethod(getter);

            final var value = method.invoke(raidDto);

            filtered.getClass().getMethod(setter, method.getReturnType()).invoke(filtered, method.getReturnType().cast(value));
        }

        return filtered;
    }

    private boolean isRaidAdmin(final RaidDto raidDto) {
        final var raidName = raidDto.getIdentifier().getId();

        final var handle = raidName.substring(raidName.lastIndexOf("/", raidName.lastIndexOf("/") - 1) + 1);

        return TokenUtil.getAdminRaids().contains(handle);
    }

    private boolean isRaidUser(final RaidDto raidDto) {
        final var raidName = raidDto.getIdentifier().getId();

        final var handle = raidName.substring(raidName.lastIndexOf("/", raidName.lastIndexOf("/") - 1) + 1);

        return TokenUtil.getUserRaids().contains(handle);
    }

    private boolean isServicePointUser(final RaidDto raidDto) {
        final var token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();

        if (!((List<?>) ((Map<?,?>) token.getClaim(REALM_ACCESS_CLAIM)).get(ROLES_CLAIM)).contains(SERVICE_POINT_USER_ROLE)) {
            return false;
        }
        final var userServicePoint = getServicePointId();
        return raidDto.getIdentifier().getOwner().getServicePoint().equals(userServicePoint);
    }

    private boolean isViewable(final RaidDto raidDto) {
        if (raidDto.getAccess().getType().getId().equals(SchemaValues.ACCESS_TYPE_OPEN.getUri())) {
            return true;
        }

        if (isRaidAdmin(raidDto)) {
            return true;
        }

        if (isRaidUser(raidDto)) {
            return true;
        }

        return isServicePointUser(raidDto);
    }
}

