package au.org.raid.api.service.raid;

import au.org.raid.api.exception.ValidationFailureException;
import au.org.raid.api.factory.HandleFactory;
import au.org.raid.api.factory.IdFactory;
import au.org.raid.api.factory.RaidRecordFactory;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.api.repository.ServicePointRepository;
import au.org.raid.api.service.*;
import au.org.raid.api.service.datacite.DataciteService;
import au.org.raid.api.service.keycloak.KeycloakService;
import au.org.raid.api.service.raid.id.IdentifierParser;
import au.org.raid.api.util.FileUtil;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.util.TokenUtil;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.Matchers;
import org.jooq.JSONB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaidServiceTest {
    private static final String USER_ID = "user-id";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    @Mock
    private RaidRepository raidRepository;
    @Mock
    private ServicePointRepository servicePointRepository;
    @Mock
    private RaidRecordFactory raidRecordFactory;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private IdFactory idFactory;
    @Mock
    private RaidChecksumService checksumService;
    @Mock
    private RaidHistoryService raidHistoryService;
    @Mock
    private RaidIngestService raidIngestService;
    @Mock
    private DataciteService dataciteService;
    @Mock
    private OrcidIntegrationService orcidIntegrationService;
    @Mock
    private HandleFactory handleFactory;
    @Mock
    private ContributorService contributorService;
    @Mock
    private KeycloakService keycloakService;
    @InjectMocks
    private RaidService raidService;

    @Test
    @DisplayName("Mint a raid")
    void mintRaid() throws IOException {
        final long servicePointId = 123;
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = new Handle(prefix, suffix);
        final var createRaidRequest = createRaidRequest();
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var userId = "user-id";

        final var servicePointRecord = new ServicePointRecord()
                .setPrefix(prefix)
                .setRepositoryId(repositoryId)
                .setPassword(password);

        final var raidDto = new RaidDto().identifier(new Id().id(handle.toString()));

        final var id = new Id().id(handle.toString());

        when(servicePointRepository.findById(servicePointId)).thenReturn(Optional.of(servicePointRecord));
        when(handleFactory.createWithPrefix(prefix)).thenReturn(handle);

        when(idFactory.create(handle.toString(), servicePointRecord)).thenReturn(id);
        when(raidHistoryService.save(createRaidRequest)).thenReturn(raidDto);

        try (MockedStatic<TokenUtil> tokenUtil = Mockito.mockStatic(TokenUtil.class)) {
            tokenUtil.when(TokenUtil::getUserId).thenReturn(userId);

            raidService.mint(createRaidRequest, servicePointId);
            verify(raidIngestService).create(raidDto);
            verify(dataciteService).mint(createRaidRequest, handle.toString(), repositoryId, password);
            verify(orcidIntegrationService).setContributorStatus(createRaidRequest.getContributor());
            verify(orcidIntegrationService).updateOrcidRecord(raidDto);
        }
    }

    @Test
    @DisplayName("Read a raid")
    void readRaid() throws IOException {
        final var raidJson = raidJson();
        final String handle = "test-handle";
        final Long servicePointId = 999L;
        final RaidRecord raidRecord = new RaidRecord();
        final ServicePointRecord servicePointRecord = new ServicePointRecord();
        servicePointRecord.setId(servicePointId);

        raidRecord.setMetadata(JSONB.valueOf(raidJson));

        final var expected = objectMapper.readValue(raidJson(), RaidDto.class);
        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(expected));

        final var result = raidService.findByHandle(handle);
        assertThat(result.get(), Matchers.is(expected));
    }


    @Test
    @DisplayName("Patching contributors saves changes and returns updated raid")
    void patchContributors() throws JsonProcessingException {

        final var prefix = "10378.1";
        final var suffix = "1696639";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var raidJson = raidJson();
        final var servicePointId = 20_000_000L;
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var servicePointGroupId = "service-point-group-id";

        final var servicePointRecord = new ServicePointRecord()
                .setRepositoryId(repositoryId)
                .setPassword(password);

        final var contributor = new Contributor()
                .id("https://orcid.org/0009-0002-5128-5184")
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_)
                .contact(true)
                .leader(true);

        final var raidDto = objectMapper.readValue(raidJson, RaidDto.class);
        final var expected = objectMapper.readValue(raidJson, RaidDto.class);

        expected.contributor(List.of(contributor));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(expected));

        when(raidHistoryService.save(expected)).thenReturn(expected);
        when(raidIngestService.update(expected)).thenReturn(expected);

        when(servicePointRepository.findById(servicePointId)).thenReturn(Optional.of(servicePointRecord));

        final var result = raidService.patchContributors(prefix, suffix, List.of(contributor));
        assertThat(result, Matchers.is(expected));

        verify(dataciteService).update(expected, handle, repositoryId, password);
        verify(orcidIntegrationService).setContributorStatus(expected.getContributor());
        verify(orcidIntegrationService).updateOrcidRecord(expected);
    }

    @Test
    @DisplayName("Updating a raid saves changes and returns updated raid")
    void update() throws JsonProcessingException {
        final var handle = "10378.1/1696639";
        final var raidJson = raidJson();
        final var servicePointId = 20_000_000L;
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var servicePointGroupId = "service-point-group-id";

        final var servicePointRecord = new ServicePointRecord()
                .setRepositoryId(repositoryId)
                .setPassword(password);

        final var updateRequest = objectMapper.readValue(raidJson, RaidUpdateRequest.class);
        final var expected = objectMapper.readValue(raidJson, RaidDto.class);

        when(raidHistoryService.findByHandleAndVersion(handle, 1)).thenReturn(Optional.of(objectMapper.writeValueAsString(expected)));

        when(mapper.readValue(anyString(), eq(RaidDto.class))).thenReturn(expected);

        when(checksumService.fromRaidUpdateRequest(updateRequest)).thenReturn("a");
        when(checksumService.fromRaidDto(expected)).thenReturn("b");

        when(raidHistoryService.save(updateRequest)).thenReturn(expected);
        when(raidIngestService.update(expected)).thenReturn(expected);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.<String, Object>of("service_point_group_id", servicePointGroupId);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);

            when(servicePointRepository.findById(servicePointId)).thenReturn(Optional.of(servicePointRecord));

            final var result = raidService.update(updateRequest, servicePointId);
            assertThat(result, Matchers.is(expected));

            verify(dataciteService).update(updateRequest, handle, repositoryId, password);
            verify(orcidIntegrationService).setContributorStatus(expected.getContributor());
            verify(orcidIntegrationService).updateOrcidRecord(expected);
        }
    }

    @Test
    @DisplayName("No update is performed if no diff is detected")
    void noUpdateWhenNoDiff() throws JsonProcessingException, ValidationFailureException {

        final var servicePointId = 20_000_000L;
        final var raidJson = raidJson();
        final var repositoryId = "repository-id";
        final var password = "_password";
        final var servicePointGroupId = "service-point-group-id";

        final var servicePointRecord = new ServicePointRecord()
                .setId(servicePointId)
                .setRepositoryId(repositoryId)
                .setPassword(password);

        final var updateRequest = objectMapper.readValue(raidJson, RaidUpdateRequest.class);
        final var expected = objectMapper.readValue(raidJson, RaidDto.class);


        final var id = new IdentifierParser().parseUrlWithException(updateRequest.getIdentifier().getId());
        final var handle = id.handle().format();

        when(servicePointRepository.findById(servicePointId)).thenReturn(Optional.of(servicePointRecord));

        when(checksumService.fromRaidDto(expected)).thenReturn("1");
        when(checksumService.fromRaidUpdateRequest(updateRequest)).thenReturn("1");

        when(raidHistoryService.findByHandleAndVersion(handle, 1)).thenReturn(Optional.of(objectMapper.writeValueAsString(expected)));

        when(mapper.readValue(anyString(), eq(RaidDto.class))).thenReturn(expected);

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.<String, Object>of("service_point_group_id", servicePointGroupId);

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);

            final var result = raidService.update(updateRequest, servicePointId);

            assertThat(result, Matchers.is(expected));

            verifyNoInteractions(dataciteService);
            verifyNoInteractions(raidIngestService);
        }
    }

    @Test
    @DisplayName("Retries minting handle with Datacite if handle is already in use")
    void retriesMint() throws IOException {
        final long servicePointId = 123;
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = new Handle(prefix, suffix);
        final var createRaidRequest = createRaidRequest();
        final var repositoryId = "repository-id";
        final var password = "_password";

        final var servicePointRecord = new ServicePointRecord()
                .setPrefix(prefix)
                .setRepositoryId(repositoryId)
                .setPassword(password);

        final var id = new Id();

        final var exception = new HttpClientErrorException(HttpStatusCode.valueOf(422));

        when(servicePointRepository.findById(servicePointId)).thenReturn(Optional.of(servicePointRecord));
        when(handleFactory.createWithPrefix(prefix)).thenReturn(handle);

        when(idFactory.create(handle.toString(), servicePointRecord)).thenReturn(id);

        doThrow(exception)
                .when(dataciteService).mint(createRaidRequest, handle.toString(), repositoryId, password);

        assertThrows(exception.getClass(), () -> raidService.mint(createRaidRequest, servicePointId));

        verifyNoInteractions(raidIngestService);
        verifyNoInteractions(raidHistoryService);

        verify(handleFactory, times(3)).createWithPrefix(prefix);
        verify(dataciteService, times(3)).mint(createRaidRequest, handle.toString(), repositoryId, password);
    }

    @Test
    @DisplayName("Empty optional is returned if raid not found")
    void raidNotFound() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.empty());

        assertThat(raidService.getPermissions(prefix, suffix), is(Optional.empty()));
        verifyNoInteractions(servicePointRepository);
    }

    @Test
    @DisplayName("Can read and write when service point user")
    void servicePointUser() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointId = new BigDecimal(1L);
        final var servicePointGroupId = "service-point-group-id";
        final var servicePointRecord = new ServicePointRecord().setId(servicePointId.longValue());

        final var raid = new RaidDto()
            .identifier(new Id()
                    .owner(new Owner()
                        .servicePoint(servicePointId)))
                .access(new Access()
                    .type(new AccessType()
                        .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_)));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(raid));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.<String, Object>of("service_point_group_id", servicePointGroupId);
            final var authorities = List.of((GrantedAuthority) () -> "ROLE_service-point-user");

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);
            when(authentication.getAuthorities()).thenReturn(authorities);

            when(servicePointRepository.findByGroupId(servicePointGroupId)).thenReturn(Optional.of(servicePointRecord));

            final var permissions = raidService.getPermissions(prefix, suffix)
                    .orElseThrow();

            assertTrue(permissions.isRead());
            assertTrue(permissions.isWrite());
        }
    }

    @Test
    @DisplayName("Can read and write when raid user assigned to raid")
    void raidUser() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointId = new BigDecimal(1L);
        final var servicePointGroupId = "service-point-group-id";
        final var servicePointRecord = new ServicePointRecord().setId(servicePointId.longValue());

        final var raid = new RaidDto()
                .identifier(new Id()
                        .owner(new Owner()
                                .servicePoint(servicePointId)))
                .access(new Access()
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_)));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(raid));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.of(
                    "service_point_group_id", servicePointGroupId,
                    "user_raids", List.of(handle)
            );


            final var authorities = List.of((GrantedAuthority) () -> "ROLE_raid-user");

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);
            when(authentication.getAuthorities()).thenReturn(authorities);

            when(servicePointRepository.findByGroupId(servicePointGroupId)).thenReturn(Optional.of(servicePointRecord));

            final var permissions = raidService.getPermissions(prefix, suffix)
                    .orElseThrow();

            assertTrue(permissions.isRead());
            assertTrue(permissions.isWrite());
        }
    }

    @Test
    @DisplayName("Can only read when raid user not assigned to raid")
    void raidUserNotAssigned() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointId = new BigDecimal(1L);
        final var servicePointGroupId = "service-point-group-id";
        final var servicePointRecord = new ServicePointRecord().setId(servicePointId.longValue());

        final var raid = new RaidDto()
                .identifier(new Id()
                        .owner(new Owner()
                                .servicePoint(servicePointId)))
                .access(new Access()
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_)));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(raid));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.of(
                    "service_point_group_id", servicePointGroupId,
                    "user_raids", Collections.emptyList()
            );

            final var authorities = List.of((GrantedAuthority) () -> "ROLE_raid-user");

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);
            when(authentication.getAuthorities()).thenReturn(authorities);

            when(servicePointRepository.findByGroupId(servicePointGroupId)).thenReturn(Optional.of(servicePointRecord));

            final var permissions = raidService.getPermissions(prefix, suffix)
                    .orElseThrow();

            assertTrue(permissions.isRead());
            assertFalse(permissions.isWrite());
        }
    }

    @Test
    @DisplayName("Can only read when raid admin not assigned to raid")
    void raidAdminNotAssigned() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointId = new BigDecimal(1L);
        final var servicePointGroupId = "service-point-group-id";
        final var servicePointRecord = new ServicePointRecord().setId(servicePointId.longValue());

        final var raid = new RaidDto()
                .identifier(new Id()
                        .owner(new Owner()
                                .servicePoint(servicePointId)))
                .access(new Access()
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_)));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(raid));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.of(
                    "service_point_group_id", servicePointGroupId,
                    "user_raids", Collections.emptyList()
            );

            final var authorities = List.of((GrantedAuthority) () -> "ROLE_raid-admin");

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);
            when(authentication.getAuthorities()).thenReturn(authorities);

            when(servicePointRepository.findByGroupId(servicePointGroupId)).thenReturn(Optional.of(servicePointRecord));

            final var permissions = raidService.getPermissions(prefix, suffix)
                    .orElseThrow();

            assertTrue(permissions.isRead());
            assertFalse(permissions.isWrite());
        }
    }

    @Test
    @DisplayName("Cannot read or write when raid user not assigned to raid and raid is embargoed")
    void raidUserNotAssignedToEmbargoedRaid() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointId =new BigDecimal(1L);
        final var servicePointGroupId = "service-point-group-id";
        final var servicePointRecord = new ServicePointRecord().setId(servicePointId.longValue());

        final var raid = new RaidDto()
                .identifier(new Id()
                        .owner(new Owner()
                                .servicePoint(servicePointId)))
                .access(new Access()
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_)));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(raid));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.of(
                    "service_point_group_id", servicePointGroupId,
                    "user_raids", Collections.emptyList()
            );

            final var authorities = List.of((GrantedAuthority) () -> "ROLE_raid-user");

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);
            when(authentication.getAuthorities()).thenReturn(authorities);

            when(servicePointRepository.findByGroupId(servicePointGroupId)).thenReturn(Optional.of(servicePointRecord));

            final var permissions = raidService.getPermissions(prefix, suffix)
                    .orElseThrow();

            assertFalse(permissions.isRead());
            assertFalse(permissions.isWrite());
        }
    }

    @Test
    @DisplayName("Cannot read or write when raid admin not assigned to raid and raid is embargoed")
    void raidAdminNotAssignedToEmbargoedRaid() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointId = new BigDecimal(1L);
        final var servicePointGroupId = "service-point-group-id";
        final var servicePointRecord = new ServicePointRecord().setId(servicePointId.longValue());

        final var raid = new RaidDto()
                .identifier(new Id()
                        .owner(new Owner()
                                .servicePoint(servicePointId)))
                .access(new Access()
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_)));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(raid));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.of(
                    "service_point_group_id", servicePointGroupId,
                    "user_raids", Collections.emptyList()
            );

            final var authorities = List.of((GrantedAuthority) () -> "ROLE_raid-admin");

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);
            when(authentication.getAuthorities()).thenReturn(authorities);

            when(servicePointRepository.findByGroupId(servicePointGroupId)).thenReturn(Optional.of(servicePointRecord));

            final var permissions = raidService.getPermissions(prefix, suffix)
                    .orElseThrow();

            assertFalse(permissions.isRead());
            assertFalse(permissions.isWrite());
        }
    }

    @Test
    @DisplayName("Can read and write when raid admin assigned to raid")
    void raidAdmin() {
        final var prefix = "_prefix";
        final var suffix = "_suffix";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointId = new BigDecimal(1L);
        final var servicePointGroupId = "service-point-group-id";
        final var servicePointRecord = new ServicePointRecord().setId(servicePointId.longValue());

        final var raid = new RaidDto()
                .identifier(new Id()
                        .owner(new Owner()
                                .servicePoint(servicePointId)))
                .access(new Access()
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_)));

        when(raidHistoryService.findByHandle(handle)).thenReturn(Optional.of(raid));

        try (MockedStatic<SecurityContextHolder> securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            final var securityContext = mock(SecurityContext.class);
            final var authentication = mock(JwtAuthenticationToken.class);
            final var token = mock(Jwt.class);
            final var claims = Map.of(
                    "service_point_group_id", servicePointGroupId,
                    "admin_raids", List.of(handle)
            );


            final var authorities = List.of((GrantedAuthority) () -> "ROLE_raid-admin");

            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getToken()).thenReturn(token);
            when(token.getClaims()).thenReturn(claims);
            when(authentication.getAuthorities()).thenReturn(authorities);

            when(servicePointRepository.findByGroupId(servicePointGroupId)).thenReturn(Optional.of(servicePointRecord));

            final var permissions = raidService.getPermissions(prefix, suffix)
                    .orElseThrow();

            assertTrue(permissions.isRead());
            assertTrue(permissions.isWrite());
        }
    }


    private String raidJson() {
        return FileUtil.resourceContent("/fixtures/raid.json");
    }

    private RaidCreateRequest createRaidRequest() throws IOException {
        final String json = FileUtil.resourceContent("/fixtures/create-raid.json");
        return objectMapper.readValue(json, RaidCreateRequest.class);
    }
}