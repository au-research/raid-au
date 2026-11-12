package au.org.raid.orcidupdater.invitation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /invitations, sent by the app after the RAiD is saved. */
public record InvitationRequest(
        @NotBlank
        @Pattern(regexp = HANDLE, message = HANDLE_MESSAGE)
        String handle,

        @NotBlank
        @Pattern(regexp = ORCID_ID, message = ORCID_ID_MESSAGE)
        String contributorId,

        @NotBlank
        @Email
        @Size(max = 254)
        String email) {

    public static final String HANDLE = "^[^/\\s]+/[^/\\s]+$";
    public static final String HANDLE_MESSAGE = "must be a RAiD handle as prefix/suffix";
    public static final String ORCID_ID = "^https://(sandbox\\.)?orcid\\.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$";
    public static final String ORCID_ID_MESSAGE = "must be an ORCID iD as a URL";

    /** Never the email: records print every field by default, and this one must not be logged. */
    @Override
    public String toString() {
        return "InvitationRequest[handle=%s, contributorId=%s, email=redacted]".formatted(handle, contributorId);
    }
}
