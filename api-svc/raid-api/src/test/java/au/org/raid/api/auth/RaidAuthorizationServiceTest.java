package au.org.raid.api.auth;

import au.org.raid.api.service.RaidHistoryService;
import au.org.raid.api.service.ServicePointService;
import au.org.raid.api.service.keycloak.KeycloakService;
import au.org.raid.api.service.keycloak.dto.RaidPermissionsResponse;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static au.org.raid.api.config.SecurityConfig.SecurityConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RaidAuthorizationService Tests")
class RaidAuthorizationServiceTest {

    @Mock
    private ServicePointService servicePointService;

    @Mock
    private RaidHistoryService raidHistoryService;

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private RaidAuthorizationService raidAuthorizationService;

    private MockHttpServletRequest request;
    private RequestAuthorizationContext context;
    private Jwt jwt;
    private JwtAuthenticationToken jwtAuthenticationToken;

    private static final String TEST_HANDLE = "test/handle";
    private static final String TEST_GROUP_ID = "test-group-id";
    private static final String TEST_USER_ID = "test-user-id";
    private static final Long TEST_SERVICE_POINT_ID = 1L;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        context = new RequestAuthorizationContext(request);

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(TEST_USER_ID)
                .claim(SERVICE_POINT_GROUP_ID_CLAIM, TEST_GROUP_ID)
                .build();
    }

    private JwtAuthenticationToken createJwtToken(String... roles) {
        Collection<GrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Authentication createNonJwtAuthentication(String... roles) {
        Collection<GrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();

        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return authorities;
            }
            @Override
            public Object getCredentials() { return null; }
            @Override
            public Object getDetails() { return null; }
            @Override
            public Object getPrincipal() { return null; }
            @Override
            public boolean isAuthenticated() { return true; }
            @Override
            public void setAuthenticated(boolean isAuthenticated) {}
            @Override
            public String getName() { return "test"; }
        };
    }

    private RaidDto createTestRaid(boolean embargoed) {
        var raid = new RaidDto();
        var identifier = new Id();
        var owner = new Owner();
        owner.setServicePoint(new BigDecimal(TEST_SERVICE_POINT_ID));
        identifier.setOwner(owner);
        raid.setIdentifier(identifier);

        var access = new Access();
        var accessType = new AccessType();
        accessType.setId(embargoed ? AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_ : AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_);
        access.setType(accessType);
        raid.setAccess(access);

        return raid;
    }

    private ServicePoint createTestServicePoint() {
        var servicePoint = new ServicePoint();
        servicePoint.setId(TEST_SERVICE_POINT_ID);
        return servicePoint;
    }

    @Nested
    @DisplayName("Read Access Manager Tests")
    class ReadAccessManagerTests {

        @Test
        @DisplayName("Should allow operator role")
        void shouldAllowOperatorRole() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(OPERATOR_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow contributor writer role")
        void shouldAllowContributorWriterRole() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(CONTRIBUTOR_WRITER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow service point owner")
        void shouldAllowServicePointOwner() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var raid = createTestRaid(false);
            var servicePoint = createTestServicePoint();

            when(servicePointService.findByGroupId(TEST_GROUP_ID)).thenReturn(Optional.of(servicePoint));
            when(raidHistoryService.findByHandle(TEST_HANDLE)).thenReturn(Optional.of(raid));

            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow raid admin with valid handle")
        void shouldAllowRaidAdminWithValidHandle() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(RAID_ADMIN_ROLE);
            var permissions = new RaidPermissionsResponse(List.of(), List.of(TEST_HANDLE));
            when(keycloakService.getRaidPermissions(eq(TEST_USER_ID))).thenReturn(permissions);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow raid user with valid handle")
        void shouldAllowRaidUserWithValidHandle() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(RAID_USER_ROLE);
            var permissions = new RaidPermissionsResponse(List.of(TEST_HANDLE), List.of());
            when(keycloakService.getRaidPermissions(eq(TEST_USER_ID))).thenReturn(permissions);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow PID searcher for PID search")
        void shouldAllowPidSearcherForPidSearch() {
            // Given
            request.setRequestURI("/raid/test/handle");
            request.setParameter("contributor.id", "123");
            var auth = createJwtToken(PID_SEARCHER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny PID searcher without PID search parameters")
        void shouldDenyPidSearcherWithoutPidSearchParams() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(PID_SEARCHER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny user without SERVICE_POINT_USER_ROLE or RAID_ADMIN_ROLE")
        void shouldDenyServicePointUserWithoutProperRole() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken("SOME_OTHER_ROLE");
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow service point user for short path")
        void shouldAllowServicePointUserForShortPath() {
            // Given
            request.setRequestURI("/raid");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny non-JWT authentication for service point access")
        void shouldDenyNonJwtAuthenticationForServicePointAccess() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createNonJwtAuthentication(SERVICE_POINT_USER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }
    }

    @Nested
    @DisplayName("Write Access Manager Tests")
    class WriteAccessManagerTests {

        @Test
        @DisplayName("Should allow service point owner")
        void shouldAllowServicePointOwner() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var raid = createTestRaid(false);
            var servicePoint = createTestServicePoint();

            when(servicePointService.findByGroupId(TEST_GROUP_ID)).thenReturn(Optional.of(servicePoint));
            when(raidHistoryService.findByHandle(TEST_HANDLE)).thenReturn(Optional.of(raid));

            var manager = raidAuthorizationService.createWriteAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow raid admin")
        void shouldAllowRaidAdmin() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(RAID_ADMIN_ROLE);
            var permissions = new RaidPermissionsResponse(List.of(), List.of(TEST_HANDLE));
            when(keycloakService.getRaidPermissions(eq(TEST_USER_ID))).thenReturn(permissions);
            var manager = raidAuthorizationService.createWriteAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny unauthorized user")
        void shouldDenyUnauthorizedUser() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken("UNAUTHORIZED_ROLE");
            var manager = raidAuthorizationService.createWriteAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }
    }

    @Nested
    @DisplayName("Patch Access Manager Tests")
    class PatchAccessManagerTests {

        @Test
        @DisplayName("Should allow contributor writer")
        void shouldAllowContributorWriter() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(CONTRIBUTOR_WRITER_ROLE);
            var manager = raidAuthorizationService.createPatchAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should allow service point owner")
        void shouldAllowServicePointOwner() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var raid = createTestRaid(false);
            var servicePoint = createTestServicePoint();

            when(servicePointService.findByGroupId(TEST_GROUP_ID)).thenReturn(Optional.of(servicePoint));
            when(raidHistoryService.findByHandle(TEST_HANDLE)).thenReturn(Optional.of(raid));

            var manager = raidAuthorizationService.createPatchAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle resource not found exception")
        void shouldHandleResourceNotFoundException() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var servicePoint = createTestServicePoint();

            when(servicePointService.findByGroupId(TEST_GROUP_ID)).thenReturn(Optional.of(servicePoint));
            when(raidHistoryService.findByHandle(TEST_HANDLE)).thenReturn(Optional.empty());

            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should handle service point not found exception")
        void shouldHandleServicePointNotFoundException() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);

            when(servicePointService.findByGroupId(TEST_GROUP_ID)).thenReturn(Optional.empty());

            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should handle missing group ID claim")
        void shouldHandleMissingGroupIdClaim() {
            // Given
            request.setRequestURI("/raid/test/handle");
            jwt = Jwt.withTokenValue("token")
                    .header("alg", "RS256")
                    .claim("sub", "test-user") // Add minimal claim to satisfy JWT requirements
                    // Notably missing SERVICE_POINT_GROUP_ID_CLAIM
                    .build();
            var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + SERVICE_POINT_USER_ROLE)));

            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should handle invalid path format")
        void shouldHandleInvalidPathFormat() {
            // Given
            request.setRequestURI("/invalid");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted()); // Short path should be allowed for service point users
        }

        @Test
        @DisplayName("Should handle generic exception")
        void shouldHandleGenericException() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);

            when(servicePointService.findByGroupId(anyString())).thenThrow(new RuntimeException("Database error"));

            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should detect PID search with contributor.id parameter")
        void shouldDetectPidSearchWithContributorId() {
            // Given
            request.setRequestURI("/raid/test/handle");
            request.setParameter("contributor.id", "123");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted()); // Service point user should be denied for PID search
        }

        @Test
        @DisplayName("Should detect PID search with organisation.id parameter")
        void shouldDetectPidSearchWithOrganisationId() {
            // Given
            request.setRequestURI("/raid/test/handle");
            request.setParameter("organisation.id", "456");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted()); // Service point user should be denied for PID search
        }

        @Test
        @DisplayName("Should deny raid user with invalid handle")
        void shouldDenyRaidUserWithInvalidHandle() {
            // Given
            request.setRequestURI("/raid/invalid/handle");
            var permissions = new RaidPermissionsResponse(List.of(), List.of());
            when(keycloakService.getRaidPermissions(eq(TEST_USER_ID))).thenReturn(permissions);
            var auth = createJwtToken(RAID_USER_ROLE);
            var manager = raidAuthorizationService.createReadAccessManager();

            // When
            var decision = manager.check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }
    }

    @Nested
    @DisplayName("Scoped Service Point Admin Tests")
    class ScopedServicePointAdminTests {

        @Test
        @DisplayName("getAdministeredGroupIds extracts group ids from scoped roles")
        void getAdministeredGroupIdsExtractsGroupIds() {
            // Given
            var auth = createJwtToken(
                    SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + TEST_GROUP_ID,
                    SERVICE_POINT_USER_ROLE,
                    SERVICE_POINT_ADMIN_ROLE_PREFIX + ":other-group");

            // When
            var groupIds = raidAuthorizationService.getAdministeredGroupIds(auth);

            // Then
            assertEquals(Set.of(TEST_GROUP_ID, "other-group"), groupIds);
        }

        @Test
        @DisplayName("getAdministeredGroupIds returns empty set without scoped roles")
        void getAdministeredGroupIdsReturnsEmptyWithoutScopedRoles() {
            // Given
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE, OPERATOR_ROLE);

            // When
            var groupIds = raidAuthorizationService.getAdministeredGroupIds(auth);

            // Then
            assertTrue(groupIds.isEmpty());
        }

        @Test
        @DisplayName("getAdministeredGroupIds ignores scoped role with blank group id")
        void getAdministeredGroupIdsIgnoresBlankGroupId() {
            // Given
            var auth = createJwtToken(SERVICE_POINT_ADMIN_ROLE_PREFIX + ":");

            // When
            var groupIds = raidAuthorizationService.getAdministeredGroupIds(auth);

            // Then
            assertTrue(groupIds.isEmpty());
        }

        @Test
        @DisplayName("Should allow scoped admin of the raid's owning service point")
        void shouldAllowScopedAdminOfOwningServicePoint() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + TEST_GROUP_ID);
            var raid = createTestRaid(false);
            var servicePoint = createTestServicePoint();
            servicePoint.setGroupId(TEST_GROUP_ID);

            when(raidHistoryService.findByHandle(TEST_HANDLE)).thenReturn(Optional.of(raid));
            when(servicePointService.findById(TEST_SERVICE_POINT_ID)).thenReturn(Optional.of(servicePoint));

            // When
            var decision = raidAuthorizationService.isServicePointAdmin().check(() -> auth, context);

            // Then
            assertTrue(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny scoped admin of a different service point")
        void shouldDenyScopedAdminOfDifferentServicePoint() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_ADMIN_ROLE_PREFIX + ":other-group");
            var raid = createTestRaid(false);
            var servicePoint = createTestServicePoint();
            servicePoint.setGroupId(TEST_GROUP_ID);

            when(raidHistoryService.findByHandle(TEST_HANDLE)).thenReturn(Optional.of(raid));
            when(servicePointService.findById(TEST_SERVICE_POINT_ID)).thenReturn(Optional.of(servicePoint));

            // When
            var decision = raidAuthorizationService.isServicePointAdmin().check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny user without scoped admin roles")
        void shouldDenyUserWithoutScopedAdminRoles() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_USER_ROLE);

            // When
            var decision = raidAuthorizationService.isServicePointAdmin().check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny scoped admin for PID search requests")
        void shouldDenyScopedAdminForPidSearch() {
            // Given
            request.setRequestURI("/raid/test/handle");
            request.setParameter("contributor.id", "123");
            var auth = createJwtToken(SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + TEST_GROUP_ID);

            // When
            var decision = raidAuthorizationService.isServicePointAdmin().check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny scoped admin when handle missing from path")
        void shouldDenyScopedAdminWhenHandleMissing() {
            // Given
            request.setRequestURI("/raid");
            var auth = createJwtToken(SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + TEST_GROUP_ID);

            // When
            var decision = raidAuthorizationService.isServicePointAdmin().check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }

        @Test
        @DisplayName("Should deny scoped admin when raid not found")
        void shouldDenyScopedAdminWhenRaidNotFound() {
            // Given
            request.setRequestURI("/raid/test/handle");
            var auth = createJwtToken(SERVICE_POINT_ADMIN_ROLE_PREFIX + ":" + TEST_GROUP_ID);

            when(raidHistoryService.findByHandle(TEST_HANDLE)).thenReturn(Optional.empty());

            // When
            var decision = raidAuthorizationService.isServicePointAdmin().check(() -> auth, context);

            // Then
            assertFalse(decision.isGranted());
        }
    }
}