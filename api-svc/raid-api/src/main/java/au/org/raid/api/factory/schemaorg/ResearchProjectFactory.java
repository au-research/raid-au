package au.org.raid.api.factory.schemaorg;

import au.org.raid.idl.raidv2.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;

import static au.org.raid.idl.raidv2.model.DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318;

@Component
@RequiredArgsConstructor
public class ResearchProjectFactory {
    private static final DescriptionTypeIdEnum primaryDescriptionType = HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318;
    private static final OrganizationRoleIdEnum funderOrganizationRoleId =
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186;

    private final RoleFactory roleFactory;
    private final DefinedTermFactory definedTermFactory;


    public ResearchProject create(final RaidDto raid) {

        final var registrationAgencyId = raid.getIdentifier().getRegistrationAgency().getId();
        final var ownerId = raid.getIdentifier().getOwner().getId();
        final var dissolutionDate = raid.getDate().getEndDate();

        final var description = (raid.getDescription() != null) ? raid.getDescription().stream()
                .filter(d -> d.getType().getId() == primaryDescriptionType)
                .findFirst()
                .map(Description::getText)
                .orElse(null) : null;

        final var roles = new ArrayList<ResearchProjectRole>();

        for (final var contributor : raid.getContributor()) {
            roles.addAll(roleFactory.create(contributor));
        }

        final var funders = new ArrayList<ResearchProjectRole>();

        if (raid.getOrganisation() != null) {
            for (final var organisation : raid.getOrganisation()) {
                roles.addAll(roleFactory.create(organisation));
                funders.addAll(roleFactory.createFunder(organisation));
            }
        }

        final var subjects = new ArrayList<DefinedTerm>();

        if (raid.getSubject() != null) {
            for (final var subject: raid.getSubject()) {
                subjects.add(this.definedTermFactory.create(subject));
            }
        }

        return new ResearchProject()
                .atContext("https://schema.org")
                .atType("ResearchProject")
                .atId(raid.getIdentifier().getId())
                .identifier(new PropertyValue()
                        .atType("PropertyValue")
                        .propertyID("https://registry.identifiers.org/registry/raid")
                        .name("RAiD")
                        .value(raid.getIdentifier().getId())
                )
                .parentOrganization(new ResearchProjectParentOrganization()
                        .atType("Organization")
                        .atId(registrationAgencyId)
                        .identifier(new PropertyValue()
                                .atType("PropertyValue")
                                .value(registrationAgencyId)
                                .name("ROR")
                                .propertyID("https://registry.identifiers.org/registry/ror")
                        )
                )
                .foundingDate(LocalDate.parse(raid.getDate().getStartDate()))
                .dissolutionDate(dissolutionDate != null ? LocalDate.parse(dissolutionDate) : null)
                .description(description)
                .member(roles)
                .knowsAbout(subjects)
                .funder(funders);
    }
}
