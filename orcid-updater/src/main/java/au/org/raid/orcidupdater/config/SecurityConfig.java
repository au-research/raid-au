package au.org.raid.orcidupdater.config;

import au.org.raid.orcidupdater.security.ApiKeyAuthenticationFilter;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.serviceauth.JwksController;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * One filter chain per kind of caller in the updater's API contract
 * (orcid-updater/idl/orcid-updater-openapi-3.1.yaml):
 * <ol>
 *   <li>RAiD API: shared API key.</li>
 *   <li>Researchers, through the RAiD app's pages: no credential. The invitation token and
 *       OAuth state are checked by the handlers themselves, and are secrets a forging site
 *       can't know.</li>
 *   <li>Minters and RA administrators: Keycloak access tokens. Everything not listed is denied.</li>
 * </ol>
 * All chains are stateless; there is no session or cookie to forge, so CSRF protection is off.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    public static final String ADMIN_ROLE = "orcid-updater-admin";
    static final String REALM_ACCESS_CLAIM = "realm_access";
    static final String ROLES_CLAIM = "roles";

    private final OrcidUpdaterProperties properties;
    private final ProblemSecurityHandler problemSecurityHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain raidApiChain(final HttpSecurity http) throws Exception {
        return stateless(http)
                .securityMatcher("/raid-changes", "/contributors/**")
                .addFilterBefore(new ApiKeyAuthenticationFilter(properties.getApiKey()),
                        AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(POST, "/raid-changes").hasRole(ApiKeyAuthenticationFilter.RAID_API_ROLE)
                        .requestMatchers(GET, "/contributors/status").hasRole(ApiKeyAuthenticationFilter.RAID_API_ROLE)
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain researcherChain(final HttpSecurity http) throws Exception {
        return stateless(http)
                .securityMatcher("/invitations/*", "/invitations/*/authentications", "/authentications",
                        JwksController.PATH, "/actuator/health", "/actuator/health/**", "/error")
                .cors(cors -> cors.configurationSource(appCorsConfiguration()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(GET, "/invitations/*").permitAll()
                        // the public half of the updater's signing key, read by Keycloak (RAID-1011)
                        .requestMatchers(GET, JwksController.PATH).permitAll()
                        .requestMatchers(POST, "/invitations/*/authentications").permitAll()
                        .requestMatchers(POST, "/authentications").permitAll()
                        .requestMatchers(GET, "/actuator/health", "/actuator/health/**").permitAll()
                        // the servlet container forwards every error here; without this, a 404 or
                        // 500 from an already-authorised request would be rewritten to 401
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain keycloakChain(final HttpSecurity http) throws Exception {
        return stateless(http)
                // the app registers invitations from the minter's browser
                .cors(cors -> cors.configurationSource(appCorsConfiguration()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemSecurityHandler)
                        .accessDeniedHandler(problemSecurityHandler))
                .authorizeHttpRequests(auth -> auth
                        // which RAiDs a minter may invite for is checked against the RAiD API (RAID-1002)
                        .requestMatchers(POST, "/invitations").authenticated()
                        .requestMatchers("/admin/**").hasRole(ADMIN_ROLE)
                        .anyRequest().denyAll())
                .build();
    }

    /**
     * Only the RAiD app may call the updater from a browser. No credentials mode: the app sends
     * the minter's token in the Authorization header, never a cookie.
     */
    CorsConfigurationSource appCorsConfiguration() {
        final var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(appOrigin()));
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    String appOrigin() {
        final var app = UriComponentsBuilder.fromUriString(properties.getAppBaseUrl()).build();
        return app.getScheme() + "://" + app.getHost() + (app.getPort() == -1 ? "" : ":" + app.getPort());
    }

    private HttpSecurity stateless(final HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemSecurityHandler)
                        .accessDeniedHandler(problemSecurityHandler));
    }

    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::realmRoles);
        return converter;
    }

    private static Collection<GrantedAuthority> realmRoles(final Jwt jwt) {
        return Optional.ofNullable(jwt.<Map<String, Collection<String>>>getClaim(REALM_ACCESS_CLAIM))
                .map(realmAccess -> realmAccess.get(ROLES_CLAIM))
                .orElse(Collections.emptyList())
                .stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
