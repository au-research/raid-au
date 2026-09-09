package au.org.raid.api.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class TokenUtil {
    public static final String OPERATOR_ROLE = "operator";
    public static final String SERVICE_POINT_USER_ROLE = "service-point-user";
    private static final String SUBJECT_CLAIM = "sub";

    public static Jwt getToken() {
        return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();
    }

    public static String getUserId() {
        return (String) getToken().getClaims().get(SUBJECT_CLAIM);
    }

    /**
     * Does the current authentication hold the given flat role?
     *
     * <p>RAID-877: reads Spring Security's derived authorities rather than the raw {@code
     * realm_access.roles} claim. Authorities are always a superset of that claim's flat entries -
     * {@link au.org.raid.api.config.SecurityConfig#extractAuthorities} maps every role verbatim,
     * and additionally synthesises a flat {@code ROLE_service-point-user} authority for a scoped
     * {@code service-point-user:<groupId>} role that matches the token's own
     * {@code service_point_group_id} claim - so this is a drop-in equivalent for a flat role, and
     * now also correctly recognises a claim-matched scoped credential role.
     */
    public static boolean hasRole(final String role) {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        final var expectedAuthority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expectedAuthority::equals);
    }
}
