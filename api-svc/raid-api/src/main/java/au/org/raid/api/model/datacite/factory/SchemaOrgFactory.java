package au.org.raid.api.model.datacite.factory;

import au.org.raid.api.model.schemaorg.*;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaOrgFactory {
    private final String CHIEF_INVESTIGATOR_POSITION_ID = "https://vocabulary.raid.org/contributor.position.schema/307";
    private final String FUNDER_ROLE_ID = "https://vocabulary.raid.org/organisation.role.schema/186";

    public SchemaOrg create(final RaidDto raidDto) {
        //TODO: Need to filter current principal investigator
        final var contributors = raidDto.getContributor().stream()
                .filter(c -> !c.getPosition().stream()
                        .map(ContributorPosition::getId).toList().contains(CHIEF_INVESTIGATOR_POSITION_ID))
                .map(c -> Contributor.builder()
                        .type("Person")
                        .id(c.getId())
                        .identifier(c.getId())
                        .contactPoint(c.getContact())
                        .leadOrSupervisor(c.getLeader())
                        .roleName(c.getRole().stream().map(ContributorRole::getId).toList())
                        .hasOccupation(c.getPosition().stream().map(p -> OrganizationRole.builder()
                                .roleName(p.getId())
                                .temporalCoverage(TemporalCoverage.builder()
                                        .startDate(p.getStartDate())
                                        .endDate(p.getEndDate())
                                        .build())
                                .build()).toList())
                        .build())
                .toList();

        //TODO: Need to get current principal investigator
        final var principalInvestigator = raidDto.getContributor().stream()
                .filter(c -> c.getPosition().stream()
                        .map(ContributorPosition::getId).toList().contains(CHIEF_INVESTIGATOR_POSITION_ID))
                .map(c -> Contributor.builder()
                        .type("Person")
                        .id(c.getId())
                        .identifier(c.getId())
                        .contactPoint(c.getContact())
                        .leadOrSupervisor(c.getLeader())
                        .roleName(c.getRole().stream().map(ContributorRole::getId).toList())
                        .hasOccupation(c.getPosition().stream().map(p -> OrganizationRole.builder()
                                .roleName(p.getId())
                                .temporalCoverage(TemporalCoverage.builder()
                                        .startDate(p.getStartDate())
                                        .endDate(p.getEndDate())
                                        .build())
                                .build()).toList())
                        .build())
                .toList();

        return SchemaOrg.builder()
                .context(Context.builder()
                        .vocab("https://schema.org")
                        .raid("https://raid,org")
                        .dcterms("http://purl.org/dcterms")
                        .foaf("http://xmlns.com/foaf/0.1/")
                        .alternateName("https://schema.org/alternateName")
                        .contributor("https://schema.org/contributor")
                        .funder("https://schema.org/funder")
                        .sponsor("https://schema.org/sponsor")
                        .principalInvestigator("https://schema.org/accountablePerson")
                        .leadOrSupervisor("https://schema.org/accountablePerson")
                        .isPartOf("https://schema.org/isPartOf")
                        .isRelatedTo("https://schema.org/isRelatedTo")
                        .keywords("https://schema.org/keywords")
                        .about("https://schema.org/about")
                        .spatialCoverage("https://schema.org/spatialCoverage")
                        .sameAs("https://schema.org/sameAs")
                        .identifier("https://schema.org/identifier")
                        .contentAccessMode("https://schema.org/accessMode")
                        .publisher("https://schema.org/publisher")
                        .build())
                .id(raidDto.getIdentifier().getId())
                .identifier(raidDto.getIdentifier().getId())
                .name(raidDto.getTitle().get(0).getText())
                .startDate(raidDto.getDate().getStartDate())
                .endDate(raidDto.getDate().getEndDate())
                .contentAccessMode(ContentAccessMode.builder()
                        .type("CreativeWork")
                        .conditionsOfAccess(raidDto.getAccess().getType().getId())
                        .accessibilitySummary(raidDto.getAccess().getStatement() != null ? raidDto.getAccess().getStatement().getText() : null)
                        .build())
                .contributor(contributors)
                .principalInvestigator(principalInvestigator)
                .sponsor(raidDto.getOrganisation().stream()
                        .filter(o -> o.getRole().stream().map(OrganisationRole::getId).toList().contains(FUNDER_ROLE_ID))
                        .map(o -> Sponsor.builder()
                                .type("Organization")
                                .id(o.getId())
                                .identifier(o.getId())
                                .roleOccupation(OrganizationRole.builder()
                                        .roleName(FUNDER_ROLE_ID)
                                        .build())
                                .build()).toList())
                .license(raidDto.getIdentifier().getLicense())
                .publisher(Publisher.builder()
                        .type("Organization")
                        .id(raidDto.getIdentifier().getRegistrationAgency().getId())
                        .build())
                .build();
    }
}
