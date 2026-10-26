package au.org.raid.orcidupdater.config;

import au.org.raid.orcidupdater.security.ApiKeyAuthenticationFilter;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * One filter chain per kind of caller in the updater's API contract
 * (orcid-updater/idl/orcid-updater-openapi-3.1.yaml):
 * <ol>
 *   <li>RAiD API: shared API key.</li>
 *   <li>Researchers: no credential. The invitation token and OAuth state are checked by the
 *       handlers themselves. The landing page form needs no CSRF token because the invitation
 *       token in its URL is already a secret a forging site can't know.</li>
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
                .securityMatcher("/invitations/*", "/orcid/callback", "/css/**", "/actuator/health",
                        "/actuator/health/**", "/error")
                .headers(headers -> headers
                        // the landing page URL carries the invitation token; it must not reach ORCID
                        // or any linked site in a Referer header
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(researcherPagePolicy())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(GET, "/invitations/*").permitAll()
                        .requestMatchers(POST, "/invitations/*").permitAll()
                        .requestMatchers(GET, "/orcid/callback").permitAll()
                        .requestMatchers(GET, "/css/**").permitAll()
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
     * No scripts at all. The landing page form may only post here, and its redirect may only go
     * to the configured ORCID host (browsers apply form-action to the redirect too).
     */
    String researcherPagePolicy() {
        final var orcid = UriComponentsBuilder.fromUriString(properties.getOrcid().getBaseUrl()).build();
        final var orcidOrigin = orcid.getScheme() + "://" + orcid.getHost() + (orcid.getPort() == -1 ? "" : ":" + orcid.getPort());
        return "default-src 'none'; style-src 'self'; img-src 'self'; form-action 'self' " + orcidOrigin
                + "; frame-ancestors 'none'; base-uri 'none'";
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
