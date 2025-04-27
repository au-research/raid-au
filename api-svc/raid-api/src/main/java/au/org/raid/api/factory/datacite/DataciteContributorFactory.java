package au.org.raid.api.factory.datacite;

import au.org.raid.api.model.datacite.DataciteContributor;
import au.org.raid.api.model.datacite.NameIdentifier;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.OrganizationRoleIdEnum;
import au.org.raid.idl.raidv2.model.RegistrationAgency;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataciteContributorFactory {
    private static final String NAME_IDENTIFIER_SCHEME = "ROR";
    private static final String ORGANISATIONAL_NAME_TYPE = "Organizational";

    public DataciteContributor create(final RegistrationAgency registrationAgency) {
        return new DataciteContributor()
                .setContributorType("RegistrationAgency")
                .setName("RAiD AU") // move to config
                .setNameType(ORGANISATIONAL_NAME_TYPE)
                .setNameIdentifiers(List.of(
                        new NameIdentifier()
                                .setNameIdentifier(registrationAgency.getId())
                                .setSchemeUri(registrationAgency.getSchemaUri().getValue())
                                .setNameIdentifierScheme(NAME_IDENTIFIER_SCHEME)
                ));
    }

    public DataciteContributor create(final Organisation organisation) {
        final var latestRole = organisation.getRole().stream()
                .filter(role -> !role.getId().equals(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186))
                .max((o1, o2) -> o2.getStartDate().compareTo(o1.getStartDate()))
                .orElse(null);

        final var contributor = new DataciteContributor()
                .setName(organisation.getId())
                .setNameType(ORGANISATIONAL_NAME_TYPE)

                .setNameIdentifiers(List.of(
                        new NameIdentifier()
                                .setNameIdentifier(organisation.getId())
                                .setNameIdentifierScheme(NAME_IDENTIFIER_SCHEME)
                ));

        if (latestRole != null) {
            contributor.setContributorType(ORGANISATION_ROLE_MAP.get(latestRole.getId().getValue()));
        }

        return contributor;
    }


    private static final Map<String, String> ORGANISATION_ROLE_MAP = Map.of(
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182.getValue(), "HostingInstitution",
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_183.getValue(), "Other",
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_184.getValue(), "Other",
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_185.getValue(), "Other",
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_187.getValue(), "Sponsor",
            OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_188.getValue(), "Other"
    );
}