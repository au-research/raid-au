package au.org.raid.orcidupdater.deletion;

/**
 * Contract schema ResearcherDeletionResponse.
 *
 * @param orcidEntriesLeft entries still on the researcher's ORCID record because ORCID no longer
 *                         accepts the updater's token; the researcher can remove them on ORCID
 */
public record ResearcherDeletionResponse(boolean found, int orcidEntriesDeleted, int orcidEntriesLeft,
                                         boolean tokenRevoked) {
    static final ResearcherDeletionResponse NOTHING_HELD = new ResearcherDeletionResponse(false, 0, 0, false);
}
