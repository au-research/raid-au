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
        public static final String ADMIN_RAIDS_CLAIM = "admin_raids";
        public static final String USER_RAIDS_CLAIM = "user_raids";

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
                .requestMatchers(GET, RAID_API + "/count").hasRole(OPERATOR_ROLE)
                .requestMatchers(GET, RAID_API + "/all-embargoed").hasAnyRole(RAID_ACCESS_HANDLER_ROLE)
                .requestMatchers(GET, RAID_API + "/**").access(raidAuthorizationService.createReadAccessManager())
                .requestMatchers(POST, RAID_API + "/**").hasAnyRole(SERVICE_POINT_USER_ROLE, RAID_ADMIN_ROLE)
                .requestMatchers(PUT, RAID_API + "/**").access(raidAuthorizationService.createWriteAccessManager())
                .requestMatchers(PATCH, RAID_API + "/**").access(raidAuthorizationService.createPatchAccessManager())

                // Service Point API endpoints
                .requestMatchers(PUT, SERVICE_POINT_API + "/**").hasRole(OPERATOR_ROLE)
                .requestMatchers(POST, SERVICE_POINT_API + "/**").hasRole(OPERATOR_ROLE)
                .requestMatchers(GET, SERVICE_POINT_API + "/**").hasAnyRole(SERVICE_POINT_USER_ROLE, OPERATOR_ROLE)

                .anyRequest().denyAll();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return Optional.ofNullable(jwt.<Map<String, Collection<String>>>getClaim(REALM_ACCESS_CLAIM))
                .map(realmAccess -> realmAccess.get(ROLES_CLAIM))
                .orElse(Collections.emptyList())
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return new KeycloakGrantedAuthoritiesMapper();
    }
}