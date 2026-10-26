package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.config.SecurityConfig;
import au.org.raid.orcidupdater.invitation.Invitation;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.signin.InvalidInvitationLinkException.Reason;
import au.org.raid.orcidupdater.signin.SignInService.Outcome;
import au.org.raid.orcidupdater.signin.SignInService.SignInResult;
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

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SignInController.class)
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
@DisplayName("SignInController")
class SignInControllerTest {
    private static final String TOKEN = "random-part.sealed-part";
    private static final String TYPED_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://orcid.org/0000-0001-5109-3700";
    private static final Instant EXPIRES = Instant.parse("2026-11-09T01:00:00Z");
    private static final Invitation INVITATION = new Invitation(UUID.randomUUID(), TYPED_ID, "10.82841/86bf1b6c",
            "Coral reef resilience", "Reef Research Institute", EXPIRES, null);
    private static final URI ORCID = URI.create("https://sandbox.orcid.org/oauth/authorize?state=abc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignInService signInService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Get invitation: what the landing page shows, without the RAiD handle or link token")
    void getInvitation() throws Exception {
        when(signInService.openInvitation(TOKEN)).thenReturn(INVITATION);

        final var body = mockMvc.perform(get("/invitations/" + TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orcidId").value(TYPED_ID))
                .andExpect(jsonPath("$.raidTitle").value("Coral reef resilience"))
                .andExpect(jsonPath("$.servicePointName").value("Reef Research Institute"))
                .andExpect(jsonPath("$.clientName").value("ARDC RAiD Service"))
                .andExpect(jsonPath("$.contactEmail").value("privacy@example.org"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-09T01:00:00Z"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("10.82841", TOKEN);
    }

    @Test
    @DisplayName("Unknown link: 404 INVITATION_NOT_FOUND problem")
    void unknownLink() throws Exception {
        when(signInService.openInvitation(any())).thenThrow(new InvalidInvitationLinkException(Reason.UNKNOWN));

        mockMvc.perform(get("/invitations/x"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("Used or expired link: 410 with a code saying which")
    void usedOrExpiredLink() throws Exception {
        when(signInService.openInvitation("used")).thenThrow(new InvalidInvitationLinkException(Reason.USED));
        when(signInService.openInvitation("expired")).thenThrow(new InvalidInvitationLinkException(Reason.EXPIRED));

        mockMvc.perform(get("/invitations/used"))
                .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("INVITATION_USED"));
        mockMvc.perform(get("/invitations/expired"))
                .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"));
    }

    @Test
    @DisplayName("Start authentication: returns ORCID's authorisation URL, passing the Remember me choice")
    void start() throws Exception {
        when(signInService.startSignIn(TOKEN, true)).thenReturn(ORCID);

        mockMvc.perform(post("/invitations/" + TOKEN + "/authentications")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorisationUrl").value(ORCID.toString()));

        verify(signInService).startSignIn(TOKEN, true);
    }

    @Test
    @DisplayName("Start authentication without a body or the field: Remember me is off")
    void startDefaultsToNotRemembered() throws Exception {
        when(signInService.startSignIn(TOKEN, false)).thenReturn(ORCID);

        mockMvc.perform(post("/invitations/" + TOKEN + "/authentications")).andExpect(status().isOk());
        mockMvc.perform(post("/invitations/" + TOKEN + "/authentications")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(signInService, times(2)).startSignIn(TOKEN, false);
    }

    @Test
    @DisplayName("Start authentication on a used or expired link: 410, no ORCID URL")
    void startOnUsedLink() throws Exception {
        when(signInService.startSignIn(any(), anyBoolean())).thenThrow(new InvalidInvitationLinkException(Reason.EXPIRED));

        final var body = mockMvc.perform(post("/invitations/x/authentications"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("authorisationUrl");
    }

    @Test
    @DisplayName("Complete authentication: AUTHENTICATED with the signed-in and typed iDs")
    void complete() throws Exception {
        when(signInService.completeSignIn("s", "c", null)).thenReturn(
                new SignInResult(Outcome.AUTHENTICATED, OTHER_ID, TYPED_ID, "Coral reef resilience",
                        "Reef Research Institute", true, EXPIRES));

        mockMvc.perform(post("/authentications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"s\",\"code\":\"c\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.orcidId").value(OTHER_ID))
                .andExpect(jsonPath("$.typedOrcidId").value(TYPED_ID))
                .andExpect(jsonPath("$.raidTitle").value("Coral reef resilience"))
                .andExpect(jsonPath("$.servicePointName").value("Reef Research Institute"))
                .andExpect(jsonPath("$.remembered").value(true))
                .andExpect(jsonPath("$.invitationExpiresAt").value("2026-11-09T01:00:00Z"));
    }

    @Test
    @DisplayName("Complete authentication after declining at ORCID: DECLINED, no iD")
    void declined() throws Exception {
        when(signInService.completeSignIn("s", null, "access_denied")).thenReturn(
                new SignInResult(Outcome.DECLINED, null, TYPED_ID, "Coral reef resilience", "SP", false, EXPIRES));

        mockMvc.perform(post("/authentications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"s\",\"error\":\"access_denied\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("DECLINED"))
                .andExpect(jsonPath("$.orcidId").doesNotExist());
    }

    @Test
    @DisplayName("State mismatch: 400 INVALID_SIGN_IN_STATE problem")
    void stateMismatch() throws Exception {
        when(signInService.completeSignIn(any(), any(), any())).thenThrow(new InvalidOAuthStateException("no match"));

        mockMvc.perform(post("/authentications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"forged\",\"code\":\"c\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIGN_IN_STATE"));
    }

    @Test
    @DisplayName("Complete authentication without a body: 400 MALFORMED_REQUEST, service not called")
    void noBody() throws Exception {
        mockMvc.perform(post("/authentications"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(signInService);
    }

    @Test
    @DisplayName("ORCID exchange fails: 502 ORCID_SIGN_IN_FAILED problem that doesn't echo ORCID's reply")
    void orcidFailed() throws Exception {
        when(signInService.completeSignIn(any(), any(), any()))
                .thenThrow(new OrcidSignInFailedException("ORCID token exchange returned 503"));

        final var body = mockMvc.perform(post("/authentications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"s\",\"code\":\"c\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ORCID_SIGN_IN_FAILED"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("503");
    }
}
