package au.org.raid.orcidupdater.orcid;

/** An organisation as ORCID wants it on an affiliation: a name, city, country and its ROR. */
public record OrcidOrganisation(String name, String city, String region, String country, String rorId) {
}
