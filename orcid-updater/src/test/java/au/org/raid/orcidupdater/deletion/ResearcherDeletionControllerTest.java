package au.org.raid.orcidupdater.deletion;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.config.SecurityConfig;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.web.ProblemExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResearcherDeletionController.class)
@Import({SecurityConfig.class, ProblemSecurityHandler.class, ProblemExceptionHandler.class})
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
@DisplayName("ResearcherDeletionController")
class ResearcherDeletionControllerTest {
    private static final String ORCID_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String EMAIL = "researcher@example.edu";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResearcherDeletionService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private ResultActions postAsAdmin(final String json) throws Exception {
        return mockMvc.perform(post("/admin/researcher-deletions")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + SecurityConfig.ADMIN_ROLE)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    @DisplayName("Passes the request to the service and returns its answer")
    void deletes() throws Exception {
        when(service.delete(any())).thenReturn(new ResearcherDeletionResponse(true, 2, 1, true));

        postAsAdmin("{\"email\": \"%s\", \"orcidId\": \"%s\"}".formatted(EMAIL, ORCID_ID))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"found": true, "orcidEntriesDeleted": 2, "orcidEntriesLeft": 1, "tokenRevoked": true}
                        """, true));
        verify(service).delete(new ResearcherDeletionRequest(EMAIL, ORCID_ID));
    }

    @Test
    @DisplayName("Neither email nor iD: 400, and the service isn't called")
    void unidentified() throws Exception {
        postAsAdmin("{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("identified must give the researcher's email, ORCID iD or both"));
        postAsAdmin("{\"email\": \"  \"}").andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("A malformed email or iD: 400 naming the field, never echoing the value")
    void malformed() throws Exception {
        final var response = postAsAdmin("{\"email\": \"not-an-email\", \"orcidId\": \"0000-0002-1825-0097\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).contains("email", "orcidId").doesNotContain("not-an-email");
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Email and iD of different researchers: 400 RESEARCHER_MISMATCH, without naming the other iD")
    void mismatch() throws Exception {
        final var other = "https://orcid.org/0000-0001-5109-3700";
        when(service.delete(any())).thenThrow(new ResearcherMismatchException(ORCID_ID, other));

        final var response = postAsAdmin("{\"email\": \"%s\", \"orcidId\": \"%s\"}".formatted(EMAIL, ORCID_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESEARCHER_MISMATCH"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(other, EMAIL);
    }

    @Test
    @DisplayName("ORCID fails: 502 ORCID_UNAVAILABLE")
    void orcidUnavailable() throws Exception {
        when(service.delete(any())).thenThrow(new OrcidUnavailableException("ORCID returned 503"));

        postAsAdmin("{\"orcidId\": \"%s\"}".formatted(ORCID_ID))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ORCID_UNAVAILABLE"));
    }

    @Test
    @DisplayName("The request never prints the email")
    void requestToString() {
        assertThat(new ResearcherDeletionRequest(EMAIL, ORCID_ID).toString()).doesNotContain(EMAIL).contains(ORCID_ID);
    }
}
