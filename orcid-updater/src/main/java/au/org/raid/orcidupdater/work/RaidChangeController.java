package au.org.raid.orcidupdater.work;

import au.org.raid.orcidupdater.invitation.InvitationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RaidChangeController {
    private final WorkQueue workQueue;

    /** Contract schema RaidChangedNotification. */
    public record RaidChangedNotification(
            @NotBlank
            @Pattern(regexp = InvitationRequest.HANDLE, message = InvitationRequest.HANDLE_MESSAGE)
            String handle) {
    }

    /**
     * Contract operation notifyRaidChanged, called by the RAiD API after every mint, update and
     * patch. Only queued here, so the RAiD save never waits on ORCID.
     */
    @PostMapping("/raid-changes")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void notifyRaidChanged(@Valid @RequestBody final RaidChangedNotification notification) {
        workQueue.requestRaidSync(notification.handle());
        log.debug("RAiD {} changed; sync queued", notification.handle());
    }
}
