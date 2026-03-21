package au.org.raid.api.factory.datacite;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.config.properties.DataciteProperties;
import au.org.raid.api.model.datacite.doi.DataciteContributor;
import au.org.raid.api.model.datacite.doi.NameIdentifier;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.RegistrationAgency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataciteContributorFactory {
    private static final String NAME_IDENTIFIER_SCHEME = "ROR";
    private static final String ORGANISATIONAL_NAME_TYPE = "Organizational";

    private final RorClient rorClient;
    private final DataciteProperties properties;

    public DataciteContributor create(final RegistrationAgency registrationAgency) {
        return new DataciteContributor()
                .setContributorType("RegistrationAgency")
                .setName(properties.getRegistrationAgencyName())
                .setNameType(ORGANISATIONAL_NAME_TYPE)
                .setNameIdentifiers(List.of(
                        new NameIdentifier()
                                .setNameIdentifier(registrationAgency.getId())
                                .setSchemeUri(registrationAgency.getSchemaUri())
                                .setNameIdentifierScheme(NAME_IDENTIFIER_SCHEME)
                ));
    }

    public DataciteContributor create(final Organisation organisation) {
        final var organisationName = rorClient.getOrganisationName(organisation.getId());

        final var latestRole = organisation.getRole().stream()
                .filter(role -> !role.getId().equals(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri()))
                .max((o1, o2) -> o2.getStartDate().compareTo(o1.getStartDate()))
                .orElse(null);

        final var contributor = new DataciteContributor()
                .setName(organisationName)
                .setNameType(ORGANISATIONAL_NAME_TYPE)

                .setNameIdentifiers(List.of(
                        new NameIdentifier()
                                .setNameIdentifier(organisation.getId())
                                .setNameIdentifierScheme(NAME_IDENTIFIER_SCHEME)
                ));

        if (latestRole != null) {
            contributor.setContributorType(ORGANISATION_ROLE_MAP.get(latestRole.getId()));
        }

        return contributor;
    }


    private static final Map<String, String> ORGANISATION_ROLE_MAP = Map.of(
            SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri(), "HostingInstitution",
            SchemaValues.OTHER_RESEARCH_ORGANISATION_ROLE.getUri(), "Other",
            SchemaValues.PARTNER_ORGANISATION_ROLE.getUri(), "Other",
            SchemaValues.CONTRACTOR_ORGANISATION_ROLE.getUri(), "Other",
            SchemaValues.FACILITY_RESEARCH_ORGANISATION_ROLE.getUri(), "Sponsor",
            SchemaValues.OTHER_ORGANISATION_ROLE.getUri(), "Other"
    );
}