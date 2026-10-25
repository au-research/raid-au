package au.org.raid.orcidupdater.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /invitations, sent by the app after the RAiD is saved. */
public record InvitationRequest(
        @NotBlank
        @Pattern(regexp = "^[^/\\s]+/[^/\\s]+$", message = "must be a RAiD handle as prefix/suffix")
        String handle,

        @NotBlank
        @Pattern(regexp = "^https://(sandbox\\.)?orcid\\.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$",
                message = "must be an ORCID iD as a URL")
        String contributorId,

        @NotBlank
        @Email
        @Size(max = 254)
        String email) {

    /** Never the email: records print every field by default, and this one must not be logged. */
    @Override
    public String toString() {
        return "InvitationRequest[handle=%s, contributorId=%s, email=redacted]".formatted(handle, contributorId);
    }
}
