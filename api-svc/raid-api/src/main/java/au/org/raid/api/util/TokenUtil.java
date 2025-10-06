package au.org.raid.api.util;

import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.List;

public class TokenUtil {
    public static final String OPERATOR_ROLE = "operator";
    private static final String SUBJECT_CLAIM = "sub";
    private static final String USER_RAIDS_CLAIM = "user_raids";
    private static final String ADMIN_RAIDS_CLAIM = "admin_raids";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    public static Jwt getToken() {
        return ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();
    }

    public static String getUserId() {
        return (String) getToken().getClaims().get(SUBJECT_CLAIM);
    }

    public static List<String> getUserRaids() {
        if (getToken().getClaims().get(USER_RAIDS_CLAIM) != null) {
            return ((List<?>) getToken().getClaims().get(USER_RAIDS_CLAIM)).stream()
                    .filter(handle -> handle instanceof String)
                    .map(handle -> (String) handle)
                    .toList();
        }
        return Collections.emptyList();
    }

    public static List<String> getAdminRaids() {
        if (getToken().getClaims().get(ADMIN_RAIDS_CLAIM) != null) {
            return ((List<?>) getToken().getClaims().get(ADMIN_RAIDS_CLAIM)).stream()
                    .filter(handle -> handle instanceof String)
                    .map(handle -> (String) handle)
                    .toList();
        }
        return Collections.emptyList();
    }

    public static boolean hasRole(final String role) {
        final var token = getToken();
        if (token.getClaims().get(REALM_ACCESS_CLAIM) != null) {
            final var realmAccess = (LinkedTreeMap<?, ?>) token.getClaims().get(REALM_ACCESS_CLAIM);
            if (realmAccess.containsKey(ROLES_CLAIM) && realmAccess.get(ROLES_CLAIM) instanceof List) {
                return ((List<?>) realmAccess.get(ROLES_CLAIM)).contains(role);
            }
        }
        return false;
    }
}
