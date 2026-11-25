package au.org.raid.orcidupdater.invitation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InvitationController {
    private final InvitationService invitationService;

    /** Contract operation registerInvitation. Errors are mapped by ProblemExceptionHandler. */
    @PostMapping("/invitations")
    public InvitationResponse registerInvitation(@Valid @RequestBody final InvitationRequest request,
                                                 @AuthenticationPrincipal final Jwt minter) {
        return invitationService.register(request, minter.getTokenValue());
    }
}
