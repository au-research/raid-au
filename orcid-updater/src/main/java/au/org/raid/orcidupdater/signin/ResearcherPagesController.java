package au.org.raid.orcidupdater.signin;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.web.DisplayDates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Contract operations showInvitation, startAuthentication and orcidCallback: the pages a
 * researcher sees. No sign-in; the invitation token and OAuth state are the credentials.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ResearcherPagesController {
    /** ORCID's help article on removing a trusted organisation's access. */
    static final String ORCID_REVOKE_HELP =
            "https://support.orcid.org/hc/en-us/articles/360006973893-Trusted-organizations";

    private final SignInService signInService;
    private final OrcidUpdaterProperties properties;

    @ModelAttribute
    void common(final Model model) {
        model.addAttribute("clientName", properties.getOrcid().getClientName());
        model.addAttribute("contactEmail", properties.getContactEmail());
        model.addAttribute("orcidRevokeHelp", ORCID_REVOKE_HELP);
    }

    @GetMapping("/invitations/{token}")
    public String showInvitation(@PathVariable final String token, final Model model) {
        final var invitation = signInService.openInvitation(token);
        model.addAttribute("token", token);
        model.addAttribute("invitation", invitation);
        model.addAttribute("expires", DisplayDates.format(invitation.expiresAt()));
        return "invitation";
    }

    @PostMapping("/invitations/{token}")
    public ResponseEntity<Void> startAuthentication(@PathVariable final String token,
                                                    @RequestParam(defaultValue = "false") final boolean rememberMe) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(signInService.startSignIn(token, rememberMe))
                .build();
    }

    @GetMapping("/orcid/callback")
    public String orcidCallback(@RequestParam(required = false) final String state,
                                @RequestParam(required = false) final String code,
                                @RequestParam(required = false) final String error,
                                final Model model) {
        final var result = signInService.completeSignIn(state, code, error);
        model.addAttribute("result", result);
        model.addAttribute("expires", DisplayDates.format(result.invitationExpiresAt()));
        return result.authenticated() ? "authenticated" : "declined";
    }

    @ExceptionHandler(InvalidInvitationLinkException.class)
    ModelAndView invalidLink(final InvalidInvitationLinkException e) {
        final var status = e.getReason() == InvalidInvitationLinkException.Reason.UNKNOWN
                ? HttpStatus.NOT_FOUND : HttpStatus.GONE;
        return page("link-invalid", status).addObject("reason", e.getReason().name());
    }

    @ExceptionHandler(InvalidOAuthStateException.class)
    ModelAndView invalidState(final InvalidOAuthStateException e) {
        log.warn(e.getMessage());
        return page("sign-in-invalid", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrcidSignInFailedException.class)
    ModelAndView signInFailed(final OrcidSignInFailedException e) {
        log.warn(e.getMessage());
        return page("sign-in-failed", HttpStatus.BAD_GATEWAY);
    }

    // @ModelAttribute methods don't run for exception handlers, so the shared values are added here
    private ModelAndView page(final String view, final HttpStatus status) {
        final var page = new ModelAndView(view, status);
        page.addObject("clientName", properties.getOrcid().getClientName());
        page.addObject("contactEmail", properties.getContactEmail());
        return page;
    }
}
