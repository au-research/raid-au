package au.org.raid.orcidupdater.deletion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static au.org.raid.orcidupdater.invitation.InvitationRequest.ORCID_ID;
import static au.org.raid.orcidupdater.invitation.InvitationRequest.ORCID_ID_MESSAGE;

/** Body of POST /admin/researcher-deletions: the researcher's email, ORCID iD or both. */
public record ResearcherDeletionRequest(
        @Email
        @Size(max = 254)
        String email,

        @Pattern(regexp = ORCID_ID, message = ORCID_ID_MESSAGE)
        String orcidId) {

    @JsonIgnore
    @AssertTrue(message = "must give the researcher's email, ORCID iD or both")
    public boolean isIdentified() {
        return !isBlank(email) || !isBlank(orcidId);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    /** Never the email: records print every field by default, and this one must not be logged. */
    @Override
    public String toString() {
        return "ResearcherDeletionRequest[email=%s, orcidId=%s]".formatted(isBlank(email) ? null : "redacted", orcidId);
    }
}
