package au.org.raid.api.factory.schemaorg;

import au.org.raid.idl.raidv2.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class RoleFactory {
    private static final OrganizationRoleIdEnum funderOrganizationRoleId =
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186;

    public List<ResearchProjectRole> create(final Contributor contributor) {

        final var contributorPositions = contributor.getPosition().stream()
                .map(position -> new ResearchProjectRole()
                        .atType("Role")
                        .atId(position.getId().getValue())
                        .startDate(LocalDate.parse(position.getStartDate()))
                        .endDate((position.getEndDate() != null) ? LocalDate.parse(position.getEndDate()) : null)
                        .roleName(position.getId().getValue())
                        .member(new Member()
                                .atType("Person")
                                .atId(contributor.getId())
                                .identifier(new MemberIdentifier()
                                        .atType("PropertyValue")
                                        .value(contributor.getId())
                                        .name("ORCID")
                                        .propertyID("https://registry.identifiers.org/registry/orcid")
                                )
                        )
                ).toList();

        final var roles = new ArrayList<>(contributorPositions);

        if (contributor.getRole() != null) {
            final var contributorRoles = contributor.getRole().stream()
                    .map(role -> new ResearchProjectRole()
                            .atType("Role")
                            .atId(role.getId().getValue())
                            .roleName(role.getId().getValue())
                            .member(new Member()
                                    .atType("Person")
                                    .atId(contributor.getId())
                                    .identifier(new MemberIdentifier()
                                            .atType("PropertyValue")
                                            .value(contributor.getId())
                                            .name("ORCID")
                                            .propertyID("https://registry.identifiers.org/registry/orcid")
                                    )
                            )
                    ).toList();

            roles.addAll(contributorRoles);
        }

        return roles;
    }

    public List<ResearchProjectRole> create(final Organisation organisation) {

        final var organisationRoles = organisation.getRole().stream()
                .filter(organisationRole -> organisationRole.getId() != funderOrganizationRoleId)
                .map(organisationRoleMapper(organisation))
                .toList();

        return new ArrayList<>(organisationRoles);
    }

    public List<ResearchProjectRole> createFunder(final Organisation organisation) {
        final var organisationRoles = organisation.getRole().stream()
                .filter(organisationRole -> organisationRole.getId() == funderOrganizationRoleId)
                .map(organisationRoleMapper(organisation))
                .toList();

        return new ArrayList<>(organisationRoles);
    }

    private Function<OrganisationRole, ResearchProjectRole> organisationRoleMapper(Organisation organisation) {
        return organisationRole -> new ResearchProjectRole()
                .atType("Role")
                .atId(organisationRole.getId().getValue())
                .startDate(LocalDate.parse(organisationRole.getStartDate()))
                .endDate((organisationRole.getEndDate() != null) ? LocalDate.parse(organisationRole.getEndDate()) : null)
                .roleName(organisationRole.getId().getValue())
                .member(new Member()
                        .atType("Organization")
                        .atId(organisation.getId())
                        .identifier(new MemberIdentifier()
                                .atType("PropertyValue")
                                .value(organisation.getId())
                                .name("ROR")
                                .propertyID("https://registry.identifiers.org/registry/ror")
                        )
                );
    }
}
