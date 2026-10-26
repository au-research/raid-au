package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.config.SecurityConfig;
import au.org.raid.orcidupdater.invitation.Invitation;
import au.org.raid.orcidupdater.security.ProblemSecurityHandler;
import au.org.raid.orcidupdater.signin.InvalidInvitationLinkException.Reason;
import au.org.raid.orcidupdater.signin.SignInService.SignInResult;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResearcherPagesController.class)
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
        "orcid-updater.public-base-url=http://localhost:8090",
        "orcid-updater.contact-email=privacy@example.org"})
@DisplayName("ResearcherPagesController")
class ResearcherPagesControllerTest {
    private static final String TOKEN = "random-part.sealed-part";
    private static final String TYPED_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String OTHER_ID = "https://orcid.org/0000-0001-5109-3700";
    private static final Instant EXPIRES = Instant.parse("2026-11-09T01:00:00Z");
    private static final Invitation INVITATION = new Invitation(UUID.randomUUID(), TYPED_ID, "10.82841/86bf1b6c",
            "Coral reef resilience", "Reef Research Institute", EXPIRES, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignInService signInService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private String page(final org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return result.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Landing page: explains RAiD, shows title, service point and client, unticked Remember me, ORCID button")
    void landingPage() throws Exception {
        when(signInService.openInvitation(TOKEN)).thenReturn(INVITATION);

        final var html = page(mockMvc.perform(get("/invitations/" + TOKEN)).andExpect(status().isOk()));

        assertThat(html).contains("What is a RAiD?", "Coral reef resilience", "Reef Research Institute",
                "ARDC RAiD Service", "Authenticate with ORCID", "Remember me for future RAiDs at this RA",
                "privacy@example.org", "9 November 2026", "action=\"/invitations/" + TOKEN + "\"");
        assertThat(html).containsPattern("<input type=\"checkbox\" id=\"rememberMe\" name=\"rememberMe\" value=\"true\">")
                .doesNotContain("checked");
    }

    @Test
    @DisplayName("Landing page escapes RAiD content, so a title can't inject markup")
    void escapesTitle() throws Exception {
        when(signInService.openInvitation(TOKEN)).thenReturn(new Invitation(UUID.randomUUID(), TYPED_ID, "h/s",
                "<script>alert(1)</script>", "<b>SP</b>", EXPIRES, null));

        final var html = page(mockMvc.perform(get("/invitations/" + TOKEN)));

        assertThat(html).doesNotContain("<script>alert(1)</script>", "<b>SP</b>").contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("Landing page for an invitation registered before titles were stored still renders")
    void noTitle() throws Exception {
        when(signInService.openInvitation(TOKEN)).thenReturn(new Invitation(UUID.randomUUID(), TYPED_ID, "h/s",
                null, null, EXPIRES, null));

        assertThat(page(mockMvc.perform(get("/invitations/" + TOKEN)).andExpect(status().isOk())))
                .contains("You have been named as a contributor on a research project.");
    }

    @Test
    @DisplayName("Authenticate with ORCID: 303 to ORCID, passing the Remember me choice")
    void start() throws Exception {
        final var orcid = URI.create("https://sandbox.orcid.org/oauth/authorize?state=abc");
        when(signInService.startSignIn(TOKEN, true)).thenReturn(orcid);
        when(signInService.startSignIn(TOKEN, false)).thenReturn(orcid);

        mockMvc.perform(post("/invitations/" + TOKEN).param("rememberMe", "true"))
                .andExpect(status().isSeeOther()).andExpect(header().string("Location", orcid.toString()));
        mockMvc.perform(post("/invitations/" + TOKEN))
                .andExpect(status().isSeeOther());

        verify(signInService).startSignIn(TOKEN, true);
        verify(signInService).startSignIn(TOKEN, false);
    }

    @Test
    @DisplayName("Unknown link: 404 page saying the link is no longer valid")
    void unknownLink() throws Exception {
        when(signInService.openInvitation(any())).thenThrow(new InvalidInvitationLinkException(Reason.UNKNOWN));

        assertThat(page(mockMvc.perform(get("/invitations/x")).andExpect(status().isNotFound())))
                .contains("This link is no longer valid", "We don't recognise it");
    }

    @Test
    @DisplayName("Used or expired link: 410 page, and the button never redirects to ORCID")
    void usedOrExpiredLink() throws Exception {
        when(signInService.openInvitation(any())).thenThrow(new InvalidInvitationLinkException(Reason.USED));
        assertThat(page(mockMvc.perform(get("/invitations/x")).andExpect(status().isGone())))
                .contains("This link is no longer valid", "already been used");

        when(signInService.startSignIn(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new InvalidInvitationLinkException(Reason.EXPIRED));
        assertThat(page(mockMvc.perform(post("/invitations/x")).andExpect(status().isGone())
                .andExpect(header().doesNotExist("Location")))).contains("It has expired.");
    }

    @Test
    @DisplayName("Confirmation page: success, what will be written, how to revoke, and the remembered choice")
    void authenticated() throws Exception {
        when(signInService.completeSignIn("s", "c", null)).thenReturn(
                new SignInResult(true, TYPED_ID, TYPED_ID, "Coral reef resilience", "Reef Research Institute", true, EXPIRES));

        final var html = page(mockMvc.perform(get("/orcid/callback").param("state", "s").param("code", "c"))
                .andExpect(status().isOk()));

        assertThat(html).contains("Your ORCID iD is confirmed", TYPED_ID, "Coral reef resilience",
                "as a membership", "revoke access at any time", ResearcherPagesController.ORCID_REVOKE_HELP,
                "one-way fingerprint", "privacy@example.org")
                .doesNotContain("different iD");
    }

    @Test
    @DisplayName("Confirmation page when ORCID returned a different iD says which one will be used")
    void authenticatedDifferentId() throws Exception {
        when(signInService.completeSignIn("s", "c", null)).thenReturn(
                new SignInResult(true, OTHER_ID, TYPED_ID, "Coral reef resilience", "SP", false, EXPIRES));

        assertThat(page(mockMvc.perform(get("/orcid/callback").param("state", "s").param("code", "c"))))
                .contains(OTHER_ID, "different iD from the one entered", TYPED_ID)
                .doesNotContain("one-way fingerprint");
    }

    @Test
    @DisplayName("Declined at ORCID: page explaining nothing was authorised and the link still works")
    void declined() throws Exception {
        when(signInService.completeSignIn("s", null, "access_denied")).thenReturn(
                new SignInResult(false, null, TYPED_ID, "Coral reef resilience", "SP", false, EXPIRES));

        assertThat(page(mockMvc.perform(get("/orcid/callback").param("state", "s").param("error", "access_denied"))
                .andExpect(status().isOk())))
                .contains("Nothing was authorised", "9 November 2026");
    }

    @Test
    @DisplayName("State mismatch: 400 page, nothing stored")
    void stateMismatch() throws Exception {
        when(signInService.completeSignIn(any(), any(), any())).thenThrow(new InvalidOAuthStateException("no match"));

        assertThat(page(mockMvc.perform(get("/orcid/callback").param("state", "forged").param("code", "c"))
                .andExpect(status().isBadRequest())))
                .contains("Sign-in could not be completed", "Nothing was\n        stored");
    }

    @Test
    @DisplayName("ORCID exchange fails: 502 page")
    void orcidFailed() throws Exception {
        when(signInService.completeSignIn(any(), any(), any())).thenThrow(new OrcidSignInFailedException("503"));

        assertThat(page(mockMvc.perform(get("/orcid/callback").param("state", "s").param("code", "c"))
                .andExpect(status().isBadGateway())))
                .contains("ORCID sign-in could not be completed", "privacy@example.org");
    }
}
