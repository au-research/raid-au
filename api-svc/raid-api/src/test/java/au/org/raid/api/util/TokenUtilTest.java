package au.org.raid.api.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RAID-877: TokenUtil.hasRole() must read Spring Security's derived authorities, not the raw
 * realm_access.roles claim - otherwise a scoped "service-point-user:<groupId>" role that
 * SecurityConfig#extractAuthorities has already normalised into a flat authority stays invisible
 * to it, and RaidIngestService silently truncates a service-point-user's own closed-access raids
 * out of GET /raid/ instead of erroring.
 *
 * <p>These tests deliberately make the raw claim and the authorities disagree, so a pass only
 * proves hasRole() is reading the authorities.
 */
@DisplayName("TokenUtil Tests")
class TokenUtilTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateWithDisagreement(final List<String> rawClaimRoles, final String... authorities) {
        final Collection<GrantedAuthority> granted = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        final var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("test-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", Map.of("roles", rawClaimRoles))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, granted));
    }

    @Test
    @DisplayName("hasRole() is true when the authority is present, even though the raw claim disagrees")
    void hasRoleTrueFromAuthorityDespiteDisagreeingClaim() {
        // Raw claim carries only the scoped role; the authority is what extractAuthorities would
        // have synthesised for a claim-matched scoped credential. If hasRole() read the raw claim,
        // this would incorrectly return false.
        authenticateWithDisagreement(List.of("service-point-user:group-a"), "ROLE_service-point-user");

        assertTrue(TokenUtil.hasRole(TokenUtil.SERVICE_POINT_USER_ROLE));
    }

    @Test
    @DisplayName("hasRole() is false when the authority is absent, even though the raw claim carries the flat role")
    void hasRoleFalseFromAuthorityDespiteAgreeingClaim() {
        // Raw claim carries the flat role directly, but the authority set (what Spring Security
        // actually authorizes against) does not. If hasRole() read the raw claim, this would
        // incorrectly return true.
        authenticateWithDisagreement(List.of("service-point-user"), "ROLE_operator");

        assertFalse(TokenUtil.hasRole(TokenUtil.SERVICE_POINT_USER_ROLE));
    }

    @Test
    @DisplayName("hasRole() matches the operator role from authorities")
    void hasRoleMatchesOperatorFromAuthorities() {
        authenticateWithDisagreement(List.of(), "ROLE_operator");

        assertTrue(TokenUtil.hasRole(TokenUtil.OPERATOR_ROLE));
    }

    @Test
    @DisplayName("hasRole() is false with no authentication in context")
    void hasRoleFalseWithNoAuthentication() {
        assertFalse(TokenUtil.hasRole(TokenUtil.SERVICE_POINT_USER_ROLE));
    }
}
