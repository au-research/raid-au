package au.org.raid.orcidupdater.config;

import au.org.raid.orcidupdater.invitation.InvitationService;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.signin.InvalidInvitationLinkException;
import au.org.raid.orcidupdater.signin.InvalidInvitationLinkException.Reason;
import au.org.raid.orcidupdater.signin.InvalidOAuthStateException;
import au.org.raid.orcidupdater.signin.SignInService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caller authentication and CORS for each kind of caller in the updater's API contract. Where no
 * handler exists yet, a request that passes security reaches the dispatcher and gets 404; 401 and 403 come
 * from the security chains.
 */
@WebMvcTest
@Import({SecurityConfig.class, ProblemSecurityHandler.class})
@EnableConfigurationProperties(OrcidUpdaterProperties.class)
@TestPropertySource(properties = {
        "orcid-updater.orcid.client-id=APP-TEST",
        "orcid-updater.orcid.client-secret=test",
        "orcid-updater.orcid.client-name=ARDC RAiD Service",
        "orcid-updater.orcid.base-url=https://sandbox.orcid.org",
        "orcid-updater.orcid.api-url=https://api.sandbox.orcid.org/v3.0",
        "orcid-updater.raid-api.base-url=http://localhost:8080",
        "orcid-updater.keycloak.issuer-uri=http://localhost:8001/realms/raid",
        "orcid-updater.mail.host=localhost",
        "orcid-updater.mail.from=orcid-updater@example.org",
        "orcid-updater.email-hash-secret=test",
        "orcid-updater.token-encryption-key=test-token-encryption-key-32-chars",
        "orcid-updater.link-encryption-key=test-link-encryption-key-32-chars",
        "orcid-updater.api-key=test-api-key",
        "orcid-updater.app-base-url=http://localhost:7080",
        "orcid-updater.contact-email=privacy@example.org"})
@DisplayName("SecurityConfig")
class SecurityConfigTest {
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String ADMIN = "ROLE_orcid-updater-admin";
    private static final String NOT_BLOCKED_BY_SECURITY = "passes security (404: no handler yet)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private SignInService signInService;

    private static ResultMatcher unauthorizedProblem() {
        return result -> {
            status().isUnauthorized().match(result);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.status").value(401).match(result);
        };
    }

    private static ResultMatcher forbiddenProblem() {
        return result -> {
            status().isForbidden().match(result);
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.status").value(403).match(result);
        };
    }

    @Nested
    @DisplayName("RAiD API endpoints (API key)")
    class RaidApi {
        @Test
        @DisplayName("No key: 401 problem")
        void noKey() throws Exception {
            mockMvc.perform(post("/raid-changes")).andExpect(unauthorizedProblem());
            mockMvc.perform(get("/contributors/status")).andExpect(unauthorizedProblem());
        }

        @Test
        @DisplayName("Wrong key: 401 problem that does not echo the key")
        void wrongKey() throws Exception {
            final var body = mockMvc.perform(post("/raid-changes").header(API_KEY_HEADER, "wrong-key"))
                    .andExpect(unauthorizedProblem())
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("wrong-key").doesNotContain("test-api-key");
        }

        // A real Authorization header, not jwt(): jwt() sets the security context directly and so
        // skips the filters this test is about. This chain has no bearer-token filter, so the
        // header is ignored and the request stays anonymous.
        @Test
        @DisplayName("A bearer token is not accepted in place of the key")
        void bearerTokenInsteadOfKey() throws Exception {
            mockMvc.perform(post("/raid-changes").header("Authorization", "Bearer some.jwt.value"))
                    .andExpect(unauthorizedProblem());
        }

        @Test
        @DisplayName("Even an authenticated minter without the API role is refused: 403")
        void authenticatedMinterRefused() throws Exception {
            mockMvc.perform(post("/raid-changes").with(jwt())).andExpect(forbiddenProblem());
        }

        @Test
        @DisplayName("Correct key: " + NOT_BLOCKED_BY_SECURITY)
        void correctKey() throws Exception {
            mockMvc.perform(post("/raid-changes").header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/contributors/status").header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Correct key on an unlisted method or path under the API prefix: 403")
        void unlistedWithKey() throws Exception {
            mockMvc.perform(get("/raid-changes").header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(forbiddenProblem());
            mockMvc.perform(get("/contributors/other").header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(forbiddenProblem());
        }
    }

    @Nested
    @DisplayName("Researcher endpoints (no credential)")
    class Researcher {
        @Test
        @DisplayName("Get invitation, start and complete sign-in: pass security to the handler")
        void signInEndpoints() throws Exception {
            when(signInService.openInvitation(any())).thenThrow(new InvalidInvitationLinkException(Reason.UNKNOWN));
            when(signInService.startSignIn(any(), anyBoolean())).thenThrow(new InvalidInvitationLinkException(Reason.UNKNOWN));
            when(signInService.completeSignIn(any(), any(), any())).thenThrow(new InvalidOAuthStateException("test"));

            mockMvc.perform(get("/invitations/some-token")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
            mockMvc.perform(post("/invitations/some-token/authentications")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
            mockMvc.perform(post("/authentications").contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"s\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SIGN_IN_STATE"));
        }

        @Test
        @DisplayName("Other methods on researcher paths are denied")
        void otherMethodsDenied() throws Exception {
            mockMvc.perform(post("/invitations/some-token")).andExpect(unauthorizedProblem());
            mockMvc.perform(get("/authentications")).andExpect(unauthorizedProblem());
        }

        @Test
        @DisplayName("Responses are not cached, since they concern a single researcher")
        void noStore() throws Exception {
            when(signInService.openInvitation(any())).thenThrow(new InvalidInvitationLinkException(Reason.USED));

            mockMvc.perform(get("/invitations/some-token"))
                    .andExpect(header().string("Cache-Control", containsString("no-store")));
        }

        @Test
        @DisplayName("Health: " + NOT_BLOCKED_BY_SECURITY)
        void health() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("CORS: only the RAiD app's origin")
    class Cors {
        private static final String APP = "http://localhost:7080";

        @Test
        @DisplayName("Preflight from the app for the researcher and minter endpoints is allowed")
        void appPreflight() throws Exception {
            mockMvc.perform(options("/authentications").header("Origin", APP)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", APP));
            mockMvc.perform(options("/invitations").header("Origin", APP)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", APP));
        }

        @Test
        @DisplayName("Another origin is refused: 403, no Allow-Origin header")
        void otherOrigin() throws Exception {
            mockMvc.perform(options("/authentications").header("Origin", "https://evil.example")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("Actual requests from the app carry the Allow-Origin header, and no credentials flag")
        void actualRequest() throws Exception {
            when(signInService.openInvitation(any())).thenThrow(new InvalidInvitationLinkException(Reason.USED));

            mockMvc.perform(get("/invitations/some-token").header("Origin", APP))
                    .andExpect(header().string("Access-Control-Allow-Origin", APP))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }

        @Test
        @DisplayName("The RAiD API's endpoints have no CORS: browsers can't call them")
        void raidApiNotCors() throws Exception {
            mockMvc.perform(options("/raid-changes").header("Origin", APP)
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        @DisplayName("The origin is taken from the app base URL, without its path")
        void appOrigin() {
            assertThat(securityConfig.appOrigin()).isEqualTo(APP);
        }
    }

    @Nested
    @DisplayName("Minter and administrator endpoints (Keycloak token)")
    class Keycloak {
        @Test
        @DisplayName("Register invitation without a token: 401 problem")
        void invitationWithoutToken() throws Exception {
            mockMvc.perform(post("/invitations")).andExpect(unauthorizedProblem());
        }

        @Test
        @DisplayName("Register invitation with the API key instead of a token: 401 problem")
        void invitationWithApiKey() throws Exception {
            mockMvc.perform(post("/invitations").header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(unauthorizedProblem());
        }

        @Test
        @DisplayName("Register invitation with any valid token: passes security (400: no body)")
        void invitationWithToken() throws Exception {
            mockMvc.perform(post("/invitations").with(jwt())).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Admin endpoint without a token: 401 problem")
        void adminWithoutToken() throws Exception {
            mockMvc.perform(post("/admin/researcher-deletions")).andExpect(unauthorizedProblem());
        }

        @Test
        @DisplayName("Admin endpoint with a token lacking the admin role: 403 problem")
        void adminWithoutRole() throws Exception {
            mockMvc.perform(post("/admin/researcher-deletions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_service-point-user"))))
                    .andExpect(forbiddenProblem());
        }

        @Test
        @DisplayName("Admin endpoints with the admin role: " + NOT_BLOCKED_BY_SECURITY)
        void adminWithRole() throws Exception {
            mockMvc.perform(post("/admin/researcher-deletions").with(jwt().authorities(new SimpleGrantedAuthority(ADMIN))))
                    .andExpect(status().isNotFound());
            mockMvc.perform(post("/admin/put-code-imports").with(jwt().authorities(new SimpleGrantedAuthority(ADMIN))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Any other path is denied: 401 anonymous, 403 with a token")
        void everythingElseDenied() throws Exception {
            mockMvc.perform(get("/anything")).andExpect(unauthorizedProblem());
            mockMvc.perform(get("/anything").with(jwt().authorities(new SimpleGrantedAuthority(ADMIN))))
                    .andExpect(forbiddenProblem());
            mockMvc.perform(get("/actuator/env")).andExpect(unauthorizedProblem());
        }
    }

    @Nested
    @DisplayName("Keycloak realm roles")
    class RealmRoles {
        private List<String> authorities(final Map<String, Object> claims) {
            final var builder = Jwt.withTokenValue("token").header("alg", "none").subject("user")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
            claims.forEach(builder::claim);
            return securityConfig.jwtAuthenticationConverter().convert(builder.build()).getAuthorities()
                    .stream().map(GrantedAuthority::getAuthority).toList();
        }

        @Test
        @DisplayName("realm_access.roles become ROLE_ authorities")
        void mapsRealmRoles() {
            assertThat(authorities(Map.of("realm_access", Map.of("roles", List.of("orcid-updater-admin", "raid-user")))))
                    .containsExactlyInAnyOrder(ADMIN, "ROLE_raid-user");
        }

        @Test
        @DisplayName("No realm_access claim: no authorities")
        void noRealmAccess() {
            assertThat(authorities(Map.of("scope", "openid"))).isEmpty();
        }
    }
}
