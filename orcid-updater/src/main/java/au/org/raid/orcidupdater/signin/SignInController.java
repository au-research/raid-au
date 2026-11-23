package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.signin.SignInService.SignInResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

/**
 * Contract operations getInvitation, startAuthentication and completeAuthentication, called by
 * the RAiD app's researcher pages (RAID-977). No sign-in; the invitation token and OAuth state
 * are the credentials. Errors are mapped by ProblemExceptionHandler.
 */
@RestController
@RequiredArgsConstructor
public class SignInController {
    private final SignInService signInService;
    private final OrcidUpdaterProperties properties;

    @GetMapping("/invitations/{token}")
    public InvitationDetails getInvitation(@PathVariable final String token) {
        final var invitation = signInService.openInvitation(token);
        return new InvitationDetails(invitation.orcidId(), invitation.raidTitle(), invitation.servicePointName(),
                properties.getOrcid().getClientName(), properties.getContactEmail(), invitation.expiresAt());
    }

    @PostMapping("/invitations/{token}/authentications")
    public AuthenticationStart startAuthentication(@PathVariable final String token,
                                                   @RequestBody(required = false) final StartAuthenticationRequest request) {
        final var rememberMe = request != null && request.rememberMe();
        return new AuthenticationStart(signInService.startSignIn(token, rememberMe));
    }

    @PostMapping("/authentications")
    public SignInResult completeAuthentication(@RequestBody final CompleteAuthenticationRequest request) {
        return signInService.completeSignIn(request.state(), request.code(), request.error());
    }

    /** Contract schema Invitation: what the app's landing page shows. */
    public record InvitationDetails(String orcidId, String raidTitle, String servicePointName, String clientName,
                                    String contactEmail, Instant expiresAt) {
    }

    public record StartAuthenticationRequest(boolean rememberMe) {
    }

    /** The app sends the researcher's browser to this ORCID URL. */
    public record AuthenticationStart(URI authorisationUrl) {
    }

    /** The query parameters ORCID added to the app's redirect URI, passed on unchanged. */
    public record CompleteAuthenticationRequest(String state, String code, String error) {
    }
}
