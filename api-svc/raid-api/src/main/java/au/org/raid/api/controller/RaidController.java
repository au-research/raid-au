package au.org.raid.api.controller;

import au.org.raid.api.dto.RaidPermissionsDto;
import au.org.raid.api.exception.BudgetException;
import au.org.raid.api.exception.ServicePointNotFoundException;
import au.org.raid.api.exception.ValidationException;
import au.org.raid.api.service.Handle;
import au.org.raid.api.service.RaidHistoryService;
import au.org.raid.api.service.RaidIngestService;
import au.org.raid.api.service.ServicePointService;
import au.org.raid.api.service.limiter.RaidLimiter;
import au.org.raid.api.service.raid.RaidService;
import au.org.raid.api.util.TokenUtil;
import au.org.raid.api.validator.ValidationService;
import au.org.raid.idl.raidv2.api.RaidApi;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@SecurityScheme(name = "bearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class RaidController implements RaidApi {
    public static final String SERVICE_POINT_GROUP_ID_CLAIM = "service_point_group_id";

    private final ValidationService validationService;
    private final RaidService raidService;
    private final RaidIngestService raidIngestService;
    private final RaidHistoryService raidHistoryService;
    private final ServicePointService servicePointService;
    private final ObjectMapper objectMapper;
    private final RaidLimiter raidLimiter;

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

        return raidOptional.<ResponseEntity<Object>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<RaidDto> mintRaid(final RaidCreateRequest request) {
        final var servicePointId = getServicePointId();

        final var failures = new ArrayList<>(validationService.validateForCreate(request));

        if (!failures.isEmpty()) {
            throw new ValidationException(failures);
        }


        final var budgetFailures = raidLimiter.check(request, servicePointId);
        if (!budgetFailures.isEmpty()) {
            throw new BudgetException(budgetFailures);
        }

        final var raidDto = raidService.mint(request, servicePointId);

        return ResponseEntity.created(URI.create(raidDto.getIdentifier().getId())).body(raidDto);
    }

    @Override
    public ResponseEntity<List<RaidDto>> findAllRaids(final List<String> includeFields, final String contributorId, final String organisationId) {
        List<RaidDto> raids;

        if (contributorId != null) {
            log.debug("Fetching raids for contributor {}", contributorId);
            raids = raidIngestService.findAllByContributor(contributorId);
        } else if (organisationId != null) {
            raids = raidIngestService.findAllByOrganisation(organisationId);
        } else if (TokenUtil.hasRole(TokenUtil.OPERATOR_ROLE)) {
            log.debug("Fetching raids for operator");
            raids = raidIngestService.findAll()
                    .stream()
                    .toList();
        } else {
            final var servicePointId = getServicePointId();

            raids = raidIngestService.findAllByServicePointIdOrHandleIn(servicePointId);
        }

        if (includeFields != null && !includeFields.isEmpty()) {
            return ResponseEntity.ok(filterFields(raids, includeFields));
        }

        return ResponseEntity.ok(raids);
    }

    @Override
    public ResponseEntity<RaidDto> updateRaid(final String prefix, final String suffix, RaidUpdateRequest request) {
        final var handle = String.join("/", prefix, suffix);

        final var failures = new ArrayList<>(validationService.validateForUpdate(handle, request));

        if (!failures.isEmpty()) {
            throw new ValidationException(failures);
        }

        final var servicePointId = getServicePointId();

        final var budgetFailures = raidLimiter.check(request, servicePointId);
        if (!budgetFailures.isEmpty()) {
            throw new BudgetException(budgetFailures);
        }

        return ResponseEntity.ok(raidService.update(request, servicePointId));
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

    @PostMapping("/raid/post-to-datacite")
    public ResponseEntity<RaidDto> postToDatacite(@Valid @RequestBody final RaidDto raidDto) {
        final var handle = new Handle(raidDto.getIdentifier().getId());

        // return bad request if not a doi
        if (!handle.toString().startsWith("10.")) {
            return ResponseEntity.badRequest().build();
        }

        raidService.postToDatacite(raidDto);

        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<RaidDto> patchRaid(final String prefix, final String suffix, final RaidPatchRequest request) {

        final var failures = new ArrayList<>(validationService.validateForPatch(request));

        if (!failures.isEmpty()) {
            throw new ValidationException(failures);
        }


        final var budgetFailures = raidLimiter.check(request, getServicePointId());
        if (!budgetFailures.isEmpty()) {
            throw new BudgetException(budgetFailures);
        }


        return ResponseEntity.ok(
                raidService.patchContributors(prefix, suffix, request.getContributor()));
    }


    @Override
    public ResponseEntity<List<RaidDto>> findAllPublicRaids() {
        return ResponseEntity.ok(raidService.findAllPublic());
    }

    @Override
    public ResponseEntity<List<RaidDto>> findAllEmbargoedRaids() {
        return ResponseEntity.ok(raidService.findAllEmbargoed());
    }

    @GetMapping(value = "/raid/non-legacy")
    public ResponseEntity<List<RaidDto>> findAllNonLegacy() {
        return ResponseEntity.ok(raidService.findAllNonLegacy());
    }

    @Override
    public ResponseEntity<RaidCountResponse> countRaids(
            final Long servicePointId,
            final LocalDate startDate,
            final LocalDate endDate) {
        return ResponseEntity.ok(raidService.countRaids(servicePointId, startDate, endDate));
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

}

