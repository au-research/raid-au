package au.org.raid.api.config;

import au.org.raid.api.auth.RaidAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static au.org.raid.api.config.SecurityConfig.SecurityConstants.SERVICE_POINT_GROUP_ID_CLAIM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RAID-877: extractAuthorities() is the single place that has both a JWT's realm_access.roles and
 * its service_point_group_id claim, so it is the one place that can safely normalise a scoped
 * "service-point-user:<groupId>" role into the flat "ROLE_service-point-user" authority that every
 * existing authorization check (SecurityConfig.hasAnyRole/hasRole, RaidAuthorizationService.hasRole,
 * TokenUtil.hasRole) actually tests for.
 *
 * <p>extractAuthorities() is private, so these tests go through the public
 * jwtAuthenticationConverter() bean, mirroring how Spring Security invokes it in production.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private CorsConfigurationSource corsConfigurationSource;
    @Mock
    private KeycloakLogoutHandler keycloakLogoutHandler;
    @Mock
    private RaidAuthorizationService raidAuthorizationService;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(corsConfigurationSource, keycloakLogoutHandler, raidAuthorizationService);
    }

    private Collection<String> authorityNames(final Jwt jwt) {
        final var converter = securityConfig.jwtAuthenticationConverter();
        return extractAuthorities(converter, jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(final JwtAuthenticationConverter converter, final Jwt jwt) {
        return (Collection<GrantedAuthority>) converter.convert(jwt).getAuthorities();
    }

    private Jwt jwtWithRolesAndClaim(final List<String> roles, final String groupIdClaim) {
        var builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("test-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", Map.of("roles", roles));

        if (groupIdClaim != null) {
            builder = builder.claim(SERVICE_POINT_GROUP_ID_CLAIM, groupIdClaim);
        }

        return builder.build();
    }

    @Test
    @DisplayName("flat-only role is passed through unchanged")
    void flatOnlyRoleUnchanged() {
        var jwt = jwtWithRolesAndClaim(List.of("operator"), null);

        assertEquals(List.of("ROLE_operator"), authorityNames(jwt));
    }

    @Test
    @DisplayName("scoped role with matching claim synthesises the flat authority alongside the scoped one")
    void scopedRoleWithMatchingClaimSynthesisesFlatAuthority() {
        var jwt = jwtWithRolesAndClaim(List.of("service-point-user:group-a"), "group-a");

        var authorities = authorityNames(jwt);

        assertTrue(authorities.contains("ROLE_service-point-user:group-a"));
        assertTrue(authorities.contains("ROLE_service-point-user"));
        assertEquals(2, authorities.size());
    }

    @Test
    @DisplayName("scoped role with mismatched claim never synthesises the flat authority")
    void scopedRoleWithMismatchedClaimDoesNotSynthesiseFlatAuthority() {
        var jwt = jwtWithRolesAndClaim(List.of("service-point-user:group-a"), "group-b");

        var authorities = authorityNames(jwt);

        assertEquals(List.of("ROLE_service-point-user:group-a"), authorities);
    }

    @Test
    @DisplayName("scoped role with absent claim never synthesises the flat authority")
    void scopedRoleWithAbsentClaimDoesNotSynthesiseFlatAuthority() {
        var jwt = jwtWithRolesAndClaim(List.of("service-point-user:group-a"), null);

        var authorities = authorityNames(jwt);

        assertEquals(List.of("ROLE_service-point-user:group-a"), authorities);
    }

    @Test
    @DisplayName("blank-suffix scoped role never matches, even against a blank claim")
    void blankSuffixScopedRoleNeverMatches() {
        var jwt = jwtWithRolesAndClaim(List.of("service-point-user:"), "");

        var authorities = authorityNames(jwt);

        assertEquals(List.of("ROLE_service-point-user:"), authorities);
    }

    @Test
    @DisplayName("multiple scoped roles, one matching, synthesise the flat authority exactly once")
    void multipleScopedRolesOneMatchingSynthesisesFlatAuthorityOnce() {
        var jwt = jwtWithRolesAndClaim(
                List.of("service-point-user:group-a", "service-point-user:group-b", "service-point-user:group-c"),
                "group-b");

        var authorities = authorityNames(jwt);

        assertEquals(1, authorities.stream().filter("ROLE_service-point-user"::equals).count());
        assertEquals(4, authorities.size());
    }

    @Test
    @DisplayName("flat role plus a non-matching scoped role still carries the flat authority")
    void flatRolePlusNonMatchingScopedRoleStillCarriesFlatAuthority() {
        var jwt = jwtWithRolesAndClaim(
                List.of("service-point-user", "service-point-user:group-a"),
                "group-b");

        var authorities = authorityNames(jwt);

        assertTrue(authorities.contains("ROLE_service-point-user"));
        assertEquals(1, authorities.stream().filter("ROLE_service-point-user"::equals).count());
    }

    @Test
    @DisplayName("flat role plus a matching scoped role synthesises the flat authority only once")
    void flatRolePlusMatchingScopedRoleSynthesisesFlatAuthorityOnce() {
        var jwt = jwtWithRolesAndClaim(
                List.of("service-point-user", "service-point-user:group-a"),
                "group-a");

        var authorities = authorityNames(jwt);

        assertTrue(authorities.contains("ROLE_service-point-user"));
        assertTrue(authorities.contains("ROLE_service-point-user:group-a"));
        assertEquals(1, authorities.stream().filter("ROLE_service-point-user"::equals).count());
        assertEquals(2, authorities.size());
    }

    @Test
    @DisplayName("unrelated roles pass through untouched")
    void unrelatedRolesPassThroughUntouched() {
        var jwt = jwtWithRolesAndClaim(
                List.of("operator", "service-point-admin:group-a"),
                "group-a");

        var authorities = authorityNames(jwt);

        assertEquals(List.of("ROLE_operator", "ROLE_service-point-admin:group-a"), authorities);
    }

    @Test
    @DisplayName("empty realm_access.roles produces no authorities and no NPE")
    void emptyRealmAccessRolesProducesNoAuthorities() {
        var jwt = jwtWithRolesAndClaim(List.of(), "group-a");

        assertEquals(List.of(), authorityNames(jwt));
    }

    @Test
    @DisplayName("absent realm_access claim produces no authorities and no NPE")
    void absentRealmAccessClaimProducesNoAuthorities() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("test-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim(SERVICE_POINT_GROUP_ID_CLAIM, "group-a")
                .build();

        assertEquals(List.of(), authorityNames(jwt));
    }
}
