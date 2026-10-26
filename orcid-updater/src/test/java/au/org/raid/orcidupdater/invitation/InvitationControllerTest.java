package au.org.raid.orcidupdater.invitation;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.config.SecurityConfig;
import au.org.raid.orcidupdater.raidapi.MinterNotPermittedException;
import au.org.raid.orcidupdater.raidapi.RaidApiUnavailableException;
import au.org.raid.orcidupdater.raidapi.RaidNotFoundException;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.status.ContributorStatus;
import au.org.raid.orcidupdater.web.ProblemExceptionHandler;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
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
        "orcid-updater.token-encryption-key=test",
        "orcid-updater.link-encryption-key=test-link-encryption-key-32-chars",
        "orcid-updater.api-key=test-api-key",
        "orcid-updater.public-base-url=http://localhost:8090"})
@DisplayName("InvitationController")
class InvitationControllerTest {
    private static final String HANDLE = "10.82841/86bf1b6c";
    private static final String ORCID_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String EMAIL = "researcher@example.edu";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static String body(final String handle, final String contributorId, final String email) {
        return """
                {"handle": "%s", "contributorId": "%s", "email": "%s"}
                """.formatted(handle, contributorId, email);
    }

    private ResultActions postAsMinter(final String json) throws Exception {
        return mockMvc.perform(post("/invitations")
                .with(jwt().jwt(token -> token.tokenValue("minter-token")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions postValid() throws Exception {
        return postAsMinter(body(HANDLE, ORCID_ID, EMAIL));
    }

    @Test
    @DisplayName("Passes the request and the minter's own token to the service and returns its answer")
    void registers() throws Exception {
        when(invitationService.register(any(), eq("minter-token"))).thenReturn(new InvitationResponse(
                ORCID_ID, ContributorStatus.AWAITING_AUTHENTICATION, true, Instant.parse("2026-11-09T01:00:00Z")));

        postValid()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contributorId").value(ORCID_ID))
                .andExpect(jsonPath("$.status").value("AWAITING_AUTHENTICATION"))
                .andExpect(jsonPath("$.emailSent").value(true))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-09T01:00:00Z"));

        verify(invitationService).register(new InvitationRequest(HANDLE, ORCID_ID, EMAIL), "minter-token");
    }

    @Test
    @DisplayName("Invalid fields: 400 naming each field, never echoing the email")
    void validation() throws Exception {
        final var response = postAsMinter(body("no-slash", "0000-0002-1825-0097", "not-an-email-address"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).contains("handle", "contributorId", "email").doesNotContain("not-an-email-address");
        verifyNoInteractions(invitationService);
    }

    @Test
    @DisplayName("ORCID iD on another host is rejected")
    void foreignOrcidHost() throws Exception {
        postAsMinter(body(HANDLE, "https://evil.example/0000-0002-1825-0097", EMAIL))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(invitationService);
    }

    @Test
    @DisplayName("Malformed JSON: 400")
    void malformed() throws Exception {
        postAsMinter("{not json").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Minter can't edit the RAiD: 403 NOT_PERMITTED")
    void forbidden() throws Exception {
        when(invitationService.register(any(), any())).thenThrow(new MinterNotPermittedException(HANDLE));
        postValid().andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_PERMITTED"));
    }

    @Test
    @DisplayName("Unknown RAiD: 404 RAID_NOT_FOUND")
    void raidNotFound() throws Exception {
        when(invitationService.register(any(), any())).thenThrow(new RaidNotFoundException(HANDLE));
        postValid().andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RAID_NOT_FOUND"));
    }

    @Test
    @DisplayName("iD not on the RAiD: 422 NOT_A_CONTRIBUTOR")
    void notAContributor() throws Exception {
        when(invitationService.register(any(), any())).thenThrow(new ContributorNotOnRaidException(ORCID_ID, HANDLE));
        postValid().andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("NOT_A_CONTRIBUTOR"));
    }

    @Test
    @DisplayName("Email not sent: 502 EMAIL_DELIVERY_FAILED")
    void emailFailed() throws Exception {
        when(invitationService.register(any(), any())).thenThrow(new EmailDeliveryException("not sent", null));
        postValid().andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("EMAIL_DELIVERY_FAILED"));
    }

    @Test
    @DisplayName("RAiD API down: 502 RAID_API_UNAVAILABLE")
    void raidApiDown() throws Exception {
        when(invitationService.register(any(), any())).thenThrow(new RaidApiUnavailableException("down"));
        postValid().andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("RAID_API_UNAVAILABLE"));
    }
}
