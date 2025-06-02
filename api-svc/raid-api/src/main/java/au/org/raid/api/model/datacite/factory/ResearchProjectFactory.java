package au.org.raid.api.model.datacite.factory;

import au.org.raid.api.model.schemaorg.*;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ResearchProjectFactory {
    private final String CHIEF_INVESTIGATOR_POSITION_ID = "https://vocabulary.raid.org/contributor.position.schema/307";
    private final String FUNDER_ROLE_ID = "https://vocabulary.raid.org/organisation.role.schema/186";

    public ResearchProject create(final RaidDto raidDto) {
        final var members = new ArrayList<Member>();

        for (final var contributor : raidDto.getContributor()) {
            final var memberships = new ArrayList<OrganizationRole>();
            for (final var position : contributor.getPosition()) {
                memberships.add(OrganizationRole.builder()
                        .roleName(position.getId())
                        .startDate(position.getStartDate())
                        .endDate(position.getEndDate())
                        .build());
            }

            for (final var role : contributor.getRole()) {
                memberships.add(OrganizationRole.builder()
                        .roleName(role.getId())
                        .build());
            }

            final var member = Member.builder()
                    .identifier(contributor.getId())
                    .type("Person")
                    .memberOf(memberships)
                    .build();

            members.add(member);
        }

        for (final var organisation : raidDto.getOrganisation()) {
            final var memberships = new ArrayList<OrganizationRole>();
            for (final var role : organisation.getRole()) {
                memberships.add(OrganizationRole.builder()
                        .roleName(role.getId())
                        .startDate(role.getStartDate())
                        .endDate(role.getEndDate())
                        .build());
            }

            final var member = Member.builder()
                    .type("Organization")
                    .identifier(organisation.getId())
                    .memberOf(memberships)
                    .build();
            members.add(member);
        }

        return ResearchProject.builder()
                .id(raidDto.getIdentifier().getId())
                .identifier(
                        List.of(
                                Identifier.builder()
                                        .propertyId("https://registry.identifiers.org/registry/doi")
                                        .identifier(raidDto.getIdentifier().getId())
                                        .build(),
                                Identifier.builder()
                                        .propertyId("https://registry.identifiers.org/registry/ror")
                                        .identifier(raidDto.getIdentifier().getRegistrationAgency().getId())
                                        .valueReference("raid:registrationAgency")
                                        .build(),
                                Identifier.builder()
                                        .propertyId("https://registry.identifiers.org/registry/ror")
                                        .identifier(raidDto.getIdentifier().getOwner().getId())
                                        .valueReference("raid:owner")
                                        .build()
                        )
                )
                .name(raidDto.getTitle().get(0).getText())
                .foundingDate(raidDto.getDate().getStartDate())
                .dissolutionDate(raidDto.getDate().getEndDate())
                .member(members)
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
                .build();
    }
}
