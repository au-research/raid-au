package au.org.raid.api.config;

import au.org.raid.api.auth.KeycloakGrantedAuthoritiesMapper;
import au.org.raid.api.auth.RaidAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static au.org.raid.api.config.SecurityConfig.SecurityConstants.*;
import static org.springframework.http.HttpMethod.*;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Extract constants to a separate class
    public static class SecurityConstants {
        public static final String SERVICE_POINT_GROUP_ID_CLAIM = "service_point_group_id";
        public static final String REALM_ACCESS_CLAIM = "realm_access";
        public static final String ROLES_CLAIM = "roles";
        public static final String GROUPS = "groups";
        // Roles
        public static final String RAID_USER_ROLE = "raid-user";
        public static final String RAID_DUMPER_ROLE = "raid-dumper";
        public static final String RAID_ADMIN_ROLE = "raid-admin";
        public static final String PID_SEARCHER_ROLE = "pid-searcher";
        public static final String SERVICE_POINT_USER_ROLE = "service-point-user";
        public static final String OPERATOR_ROLE = "operator";
        public static final String CONTRIBUTOR_WRITER_ROLE = "contributor-writer";
        public static final String RAID_UPGRADER_ROLE = "raid-upgrader";
        public static final String RAID_ACCESS_HANDLER_ROLE = "raid-access-handler";
        // Prefix for scoped realm roles of the form "service-point-admin:<groupId>", where
        // groupId is the Keycloak group UUID (see RAID-712). Not yet wired into any endpoint's
        // authorization rules; consumed by later RAID-712 work.
        public static final String SERVICE_POINT_ADMIN_ROLE_PREFIX = "service-point-admin";
        // Prefix for scoped realm roles of the form "service-point-user:<groupId>", minted onto
        // client-credential tokens (see RAID-827). Unlike the scoped admin prefix above, this one
        // IS wired into authorization: extractAuthorities() below normalises a scoped role whose
        // groupId matches the token's own service_point_group_id claim into the flat
        // "ROLE_service-point-user" authority, so the existing flat-role checks throughout
        // SecurityConfig, RaidAuthorizationService and TokenUtil work unchanged for scoped
        // client-credential callers (RAID-877). Defined in terms of SERVICE_POINT_USER_ROLE
        // (rather than repeating the "service-point-user" literal) so the two constants can't drift,
        // and includes the trailing colon since every call site needs it.
        public static final String SERVICE_POINT_USER_ROLE_PREFIX = SERVICE_POINT_USER_ROLE + ":";

        // API paths
        public static final String RAID_API = "/raid";
        public static final String SERVICE_POINT_API = "/service-point";
    }

    private final CorsConfigurationSource corsConfigurationSource;
    private final KeycloakLogoutHandler keycloakLogoutHandler;
    private final RaidAuthorizationService raidAuthorizationService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(this::configureAuthorization)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        .addLogoutHandler(keycloakLogoutHandler)
                        .logoutSuccessUrl("/"))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }

    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Public endpoints
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/swagger-ui*/**", "/docs/**", "/actuator/**", "/error").permitAll()

                // Upgrade endpoints
                .requestMatchers(GET, "/legacy").hasRole(RAID_UPGRADER_ROLE)
                .requestMatchers(POST, "/legacy").hasRole(RAID_UPGRADER_ROLE)
                .requestMatchers(GET, "/upgrade").hasRole(RAID_UPGRADER_ROLE)
                .requestMatchers(POST, "/upgrade").hasRole(RAID_UPGRADER_ROLE)
                .requestMatchers(POST, RAID_API + "/post-to-datacite").hasRole(RAID_UPGRADER_ROLE)

                // RAID API endpoints
                .requestMatchers(GET, RAID_API + "/non-legacy").hasAnyRole(RAID_UPGRADER_ROLE)
                .requestMatchers(GET, RAID_API + "/all-public").hasAnyRole(RAID_DUMPER_ROLE, RAID_UPGRADER_ROLE)
                .requestMatchers(GET, RAID_API + "/count").hasAnyRole(OPERATOR_ROLE, SERVICE_POINT_USER_ROLE)
                .requestMatchers(GET, RAID_API + "/all-embargoed").hasAnyRole(RAID_ACCESS_HANDLER_ROLE)
                .requestMatchers(GET, RAID_API + "/**").access(raidAuthorizationService.createReadAccessManager())
                .requestMatchers(POST, RAID_API + "/**").hasAnyRole(SERVICE_POINT_USER_ROLE, RAID_ADMIN_ROLE)
                .requestMatchers(PUT, RAID_API + "/**").access(raidAuthorizationService.createWriteAccessManager())
                .requestMatchers(PATCH, RAID_API + "/**").access(raidAuthorizationService.createPatchAccessManager())

                // Service Point API endpoints
                .requestMatchers(PUT, SERVICE_POINT_API + "/**").hasRole(OPERATOR_ROLE)
                .requestMatchers(POST, SERVICE_POINT_API + "/**").hasRole(OPERATOR_ROLE)
                .requestMatchers(GET, SERVICE_POINT_API + "/**").hasAnyRole(SERVICE_POINT_USER_ROLE, OPERATOR_ROLE, RAID_DUMPER_ROLE)

                // Admin endpoints
                .requestMatchers(POST, "/admin/**").hasRole(OPERATOR_ROLE)

                .anyRequest().denyAll();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        var roles = Optional.ofNullable(jwt.<Map<String, Collection<String>>>getClaim(REALM_ACCESS_CLAIM))
                .map(realmAccess -> realmAccess.get(ROLES_CLAIM))
                .orElse(Collections.emptyList());

        var authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toCollection(ArrayList::new));

        // RAID-877: a client-credential token can carry a scoped "service-point-user:<groupId>"
        // realm role instead of (or as well as) the flat "service-point-user" role. Every existing
        // authorization check (SecurityConfig.hasAnyRole/hasRole, RaidAuthorizationService.hasRole,
        // TokenUtil.hasRole) tests for the flat authority, so a scoped-only role is otherwise
        // invisible to them. Synthesise the flat authority here - the one place with access to both
        // the roles and the claims - but only when a scoped role's groupId suffix matches this
        // token's own service_point_group_id claim, so a token can never be granted flat
        // service-point-user access for a group it does not belong to (fail closed).
        var claimGroupId = jwt.getClaimAsString(SERVICE_POINT_GROUP_ID_CLAIM);
        if (hasMatchingScopedServicePointUserRole(roles, claimGroupId)) {
            // A token can carry both the flat "service-point-user" role (mapped verbatim above) and
            // a matching scoped role at the same time; guard against adding the flat authority twice.
            var flatAuthority = new SimpleGrantedAuthority("ROLE_" + SERVICE_POINT_USER_ROLE);
            if (!authorities.contains(flatAuthority)) {
                authorities.add(flatAuthority);
            }
        } else if (roles.stream().anyMatch(role -> role.startsWith(SERVICE_POINT_USER_ROLE_PREFIX))) {
            // A scoped service-point-user role is present but did not translate into a usable
            // authority - either the claim is missing, or none of the scoped roles match it. Every
            // downstream denial in this case surfaces as an unhelpful "insufficient_scope", so flag
            // it here while we still know why. Demoted to debug (RAID-877 review): a misconfigured
            // credential polling the API would otherwise spam WARN-level logs indefinitely, and
            // nothing in the stack rate-limits requests.
            log.debug("JWT subject {} carries a scoped {} role that did not match its {} claim; " +
                            "no flat ROLE_{} authority was synthesised",
                    jwt.getSubject(), SERVICE_POINT_USER_ROLE_PREFIX, SERVICE_POINT_GROUP_ID_CLAIM,
                    SERVICE_POINT_USER_ROLE);
        }

        return authorities;
    }

    /**
     * Does {@code roles} contain a scoped {@code service-point-user:<groupId>} role whose groupId
     * suffix exactly equals {@code claimGroupId}? Uses startsWith/substring rather than split(":")
     * so a groupId containing a colon can't break parsing, and rejects a blank suffix (the bare
     * "service-point-user:" role) explicitly. Requires a non-blank claim - an absent or blank
     * service_point_group_id claim never matches, even against a role with a blank suffix.
     */
    private boolean hasMatchingScopedServicePointUserRole(Collection<String> roles, String claimGroupId) {
        if (claimGroupId == null || claimGroupId.isBlank()) {
            return false;
        }

        var prefix = SERVICE_POINT_USER_ROLE_PREFIX;
        return roles.stream().anyMatch(role -> {
            if (!role.startsWith(prefix)) {
                return false;
            }
            var suffix = role.substring(prefix.length());
            return !suffix.isBlank() && suffix.equals(claimGroupId);
        });
    }

    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return new KeycloakGrantedAuthoritiesMapper();
    }
}