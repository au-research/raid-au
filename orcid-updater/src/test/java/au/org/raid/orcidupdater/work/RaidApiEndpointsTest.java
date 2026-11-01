package au.org.raid.orcidupdater.work;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.config.SecurityConfig;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.status.ContributorStatusController;
import au.org.raid.orcidupdater.status.ContributorStatusService;
import au.org.raid.orcidupdater.status.ContributorStatusService.Status;
import au.org.raid.orcidupdater.web.ProblemExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The two operations the RAiD API calls with the API key: notifyRaidChanged and getContributorStatus. */
@WebMvcTest({RaidChangeController.class, ContributorStatusController.class})
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
@DisplayName("RAiD API endpoints")
class RaidApiEndpointsTest {
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://orcid.org/0000-0001-5109-3700";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkQueue workQueue;

    @MockitoBean
    private ContributorStatusService statusService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("POST /raid-changes")
    class RaidChanges {
        @Test
        @DisplayName("Queues a sync and answers 202 straight away")
        void queues() throws Exception {
            mockMvc.perform(post("/raid-changes").header(API_KEY_HEADER, "test-api-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"handle\": \"10.82841/86bf1b6c\"}"))
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(""));

            verify(workQueue).requestRaidSync("10.82841/86bf1b6c");
        }

        @Test
        @DisplayName("Not a handle: 400, nothing queued")
        void invalidHandle() throws Exception {
            mockMvc.perform(post("/raid-changes").header(API_KEY_HEADER, "test-api-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"handle\": \"https://raid.org/10.82841/86bf1b6c\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.detail").value("handle must be a RAiD handle as prefix/suffix"));

            verifyNoInteractions(workQueue);
        }
    }

    @Nested
    @DisplayName("GET /contributors/status")
    class ContributorStatusLookup {
        @Test
        @DisplayName("Returns the status, with the iD the researcher signed in as")
        void found() throws Exception {
            when(statusService.lookup(ID)).thenReturn(new Status(ID, ContributorStatus.AUTHENTICATED, OTHER_ID));

            mockMvc.perform(get("/contributors/status").param("id", ID).header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID))
                    .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                    .andExpect(jsonPath("$.authenticatedId").value(OTHER_ID));
        }

        @Test
        @DisplayName("Not authenticated: authenticatedId is null")
        void notAuthenticated() throws Exception {
            when(statusService.lookup(ID)).thenReturn(new Status(ID, ContributorStatus.UNAUTHENTICATED, null));

            mockMvc.perform(get("/contributors/status").param("id", ID).header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UNAUTHENTICATED"))
                    .andExpect(jsonPath("$.authenticatedId").isEmpty());
        }

        @Test
        @DisplayName("Bare iD instead of the URL: 400 naming the parameter, not echoing the value")
        void bareId() throws Exception {
            mockMvc.perform(get("/contributors/status").param("id", "0000-0002-1825-0097")
                            .header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.detail").value("id must be an ORCID iD as a URL"));

            verifyNoInteractions(statusService);
        }

        @Test
        @DisplayName("No id: 400")
        void missing() throws Exception {
            mockMvc.perform(get("/contributors/status").header(API_KEY_HEADER, "test-api-key"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("id is required"));
        }
    }
}
