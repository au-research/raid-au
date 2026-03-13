package au.org.raid.api.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KeycloakGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String GROUPS = "groups";

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

        if (authorities.isEmpty()) {
            return mappedAuthorities;
        }

        var authority = authorities.iterator().next();

        if (authority instanceof OidcUserAuthority oidcUserAuthority) {
            mappedAuthorities.addAll(extractAuthoritiesFromOidc(oidcUserAuthority));
        } else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
            mappedAuthorities.addAll(extractAuthoritiesFromOAuth2(oauth2UserAuthority));
        }

        return mappedAuthorities;
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromOidc(OidcUserAuthority oidcUserAuthority) {
        var userInfo = oidcUserAuthority.getUserInfo();

        // Check realm_access claim first
        if (userInfo.hasClaim(REALM_ACCESS_CLAIM)) {
            var realmAccess = userInfo.getClaimAsMap(REALM_ACCESS_CLAIM);
            var roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);
            if (roles != null) {
                log.debug("Found roles in realm_access: {}", roles);
                return generateAuthoritiesFromClaim(roles);
            }
        }

        // Fallback to groups claim
        if (userInfo.hasClaim(GROUPS)) {
            Collection<String> roles = userInfo.getClaim(GROUPS);
            if (roles != null) {
                log.debug("Found roles in groups: {}", roles);
                return generateAuthoritiesFromClaim(roles);
            }
        }

        log.debug("No roles found in OIDC token");
        return Collections.emptySet();
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromOAuth2(OAuth2UserAuthority oauth2UserAuthority) {
        Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();

        if (userAttributes.containsKey(REALM_ACCESS_CLAIM)) {
            Map<String, Object> realmAccess = (Map<String, Object>) userAttributes.get(REALM_ACCESS_CLAIM);
            Collection<String> roles = (Collection<String>) realmAccess.get(ROLES_CLAIM);

            if (roles != null) {
                log.debug("Found roles in OAuth2 realm_access: {}", roles);
                return generateAuthoritiesFromClaim(roles);
            }
        }

        log.debug("No roles found in OAuth2 token");
        return Collections.emptySet();
    }

    private Collection<GrantedAuthority> generateAuthoritiesFromClaim(Collection<String> roles) {
        return roles.stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.trim().isEmpty())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                .collect(Collectors.toSet());
    }
}
