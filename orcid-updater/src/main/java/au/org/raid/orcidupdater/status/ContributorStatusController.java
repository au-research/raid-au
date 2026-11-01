package au.org.raid.orcidupdater.status;

import au.org.raid.orcidupdater.invitation.InvitationRequest;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContributorStatusController {
    private final ContributorStatusService statusService;

    /** Contract operation getContributorStatus, called by the RAiD API with the API key. */
    @GetMapping("/contributors/status")
    public ContributorStatusService.Status getContributorStatus(
            @RequestParam("id") @Pattern(regexp = InvitationRequest.ORCID_ID,
                    message = InvitationRequest.ORCID_ID_MESSAGE) final String id) {
        return statusService.lookup(id);
    }
}
