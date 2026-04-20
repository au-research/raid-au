package au.org.raid.fixtures;


import au.org.raid.idl.raidv2.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static au.org.raid.fixtures.TestConstants.*;

public class APIFixtures {

    public static RaidUpdateRequest newUpdateRequest(){
        return newUpdateRequest(LocalDate.now());
    }

    public static RaidUpdateRequest newUpdateRequest(LocalDate today){
        String initialTitle = UUID.randomUUID().toString();
        final var descriptions = new ArrayList<Description>();
        descriptions.add(new Description()
                .language(new Language()
                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))
                        .id(LANGUAGE_ID))
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.fromValue(PRIMARY_DESCRIPTION_TYPE))
                        .schemaUri(DescriptionTypeSchemaURIEnum.fromValue(DESCRIPTION_TYPE_SCHEMA_URI)))
                .text("stuff about the int test raid")
                .language(new Language()
                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))
                        .id(LANGUAGE_ID)));


        final var identifier = new Id()
                .id("https://raid.org/xxx.yyy/zzz")
                .schemaUri(RaidIdentifierSchemaURIEnum.fromValue("https://raid.org/"))
                .registrationAgency(new RegistrationAgency()
                        .id("https://ror.org/02stey378")
                        .schemaUri(RegistrationAgencySchemaURIEnum.fromValue("https://ror.org/")))
                .owner(new Owner()
                        .id("https://ror.org/02stey378")
                        .schemaUri(RegistrationAgencySchemaURIEnum.fromValue("https://ror.org/")))
                .license("Creative Commons CC-0")
                .version(16);
        return new RaidUpdateRequest()
                .identifier(identifier)
                .title(List.of(new Title()
                        .language(new Language()
                                .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))
                                .id(LANGUAGE_ID)
                        )
                        .type(new TitleType()
                                .id(TitleTypeIdEnum.fromValue(PRIMARY_TITLE_TYPE))
                                .schemaUri(TitleTypeSchemaURIEnum.fromValue(TITLE_TYPE_SCHEMA_URI)))
                        .text(initialTitle)
                        .startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .date(new Date().startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .description(descriptions)

                .contributor(List.of(
                        orcidContributor(
                                REAL_TEST_ORCID, PRINCIPAL_INVESTIGATOR_POSITION, SOFTWARE_CONTRIBUTOR_ROLE, today, CONTRIBUTOR_EMAIL)
                ))
                .organisation(List.of(organisation(
                        REAL_TEST_ROR, LEAD_RESEARCH_ORGANISATION, today)))
                .access(new Access()
                        .statement(new AccessStatement()
                                .text("Embargoed")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))))
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.fromValue(EMBARGOED_ACCESS_TYPE))
                                .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI)))
                        .embargoExpiry(LocalDate.now().plusMonths(1)))
                .spatialCoverage(List.of(new SpatialCoverage()
                        .id(GEONAMES_MELBOURNE)
                        .place(List.of(new SpatialCoveragePlace()
                                .text("Melbourne")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI)))))
                        .schemaUri(SpatialCoverageSchemaUriEnum.fromValue(GEONAMES_SCHEMA_URI))))
                .subject(List.of(
                        new Subject()
                                .id("https://linked.data.gov.au/def/anzsrc-for/2020/3702")
                                .schemaUri(SubjectSchemaURIEnum.fromValue("https://vocabs.ardc.edu.au/viewById/316"))
                                .keyword(List.of(new SubjectKeyword()
                                        .language(new Language()
                                                .id("eng")
                                                .schemaUri(LanguageSchemaURIEnum.fromValue("https://www.iso.org/standard/74575.html")))
                                        .text("ENES")
                                ))));
    }

    public static RaidCreateRequest newCreateRequest() {
        return newCreateRequest(LocalDate.now());
    }

    public static RaidCreateRequest newCreateRequest(LocalDate today ) {
        String initialTitle = UUID.randomUUID().toString();
        final var descriptions = new ArrayList<Description>();
        descriptions.add(new Description()
                .language(new Language()
                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))
                        .id(LANGUAGE_ID))
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.fromValue(PRIMARY_DESCRIPTION_TYPE))
                        .schemaUri(DescriptionTypeSchemaURIEnum.fromValue(DESCRIPTION_TYPE_SCHEMA_URI)))
                .text("stuff about the int test raid")
                .language(new Language()
                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))
                        .id(LANGUAGE_ID)));


        return new RaidCreateRequest()
                .title(List.of(new Title()
                        .language(new Language()
                                .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))
                                .id(LANGUAGE_ID)
                        )
                        .type(new TitleType()
                                .id(TitleTypeIdEnum.fromValue(PRIMARY_TITLE_TYPE))
                                .schemaUri(TitleTypeSchemaURIEnum.fromValue(TITLE_TYPE_SCHEMA_URI)))
                        .text(initialTitle)
                        .startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .date(new Date().startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .description(descriptions)

                .contributor(List.of(
                        orcidContributor(
                                REAL_TEST_ORCID, PRINCIPAL_INVESTIGATOR_POSITION, SOFTWARE_CONTRIBUTOR_ROLE, today, CONTRIBUTOR_EMAIL)
                ))
                .organisation(List.of(organisation(
                        REAL_TEST_ROR, LEAD_RESEARCH_ORGANISATION, today)))
                .access(new Access()
                        .statement(new AccessStatement()
                                .text("Embargoed")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI))))
                        .type(new AccessType()
                                .id(AccessTypeIdEnum.fromValue(EMBARGOED_ACCESS_TYPE))
                                .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI)))
                        .embargoExpiry(LocalDate.now().plusMonths(1)))
                .spatialCoverage(List.of(new SpatialCoverage()
                        .id(GEONAMES_MELBOURNE)
                        .place(List.of(new SpatialCoveragePlace()
                                .text("Melbourne")
                                .language(new Language()
                                        .id(LANGUAGE_ID)
                                        .schemaUri(LanguageSchemaURIEnum.fromValue(LANGUAGE_SCHEMA_URI)))))
                        .schemaUri(SpatialCoverageSchemaUriEnum.fromValue(GEONAMES_SCHEMA_URI))))
                .subject(List.of(
                        new Subject()
                                .id("https://linked.data.gov.au/def/anzsrc-for/2020/3702")
                                .schemaUri(SubjectSchemaURIEnum.fromValue("https://vocabs.ardc.edu.au/viewById/316"))
                                .keyword(List.of(new SubjectKeyword()
                                        .language(new Language()
                                                .id("eng")
                                                .schemaUri(LanguageSchemaURIEnum.fromValue("https://www.iso.org/standard/74575.html")))
                                        .text("ENES")
                                ))));
    }
    public static Contributor orcidContributor(
            final String orcid,
            final String position,
            final String role,
            final LocalDate startDate,
            final String email
    ) {
        return new Contributor()
                .id(orcid)
                .contact(true)
                .leader(true)
                .schemaUri(ContributorSchemaUriEnum.fromValue(ORCID_SCHEMA_URI))
                .position(List.of(new ContributorPosition()
                        .schemaUri(ContributorPositionSchemaUriEnum.fromValue(CONTRIBUTOR_POSITION_SCHEMA_URI))
                        .id(ContributorPositionIdEnum.fromValue(position))
                        .startDate(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .role(List.of(
                        new ContributorRole()
                                .schemaUri(ContributorRoleSchemaUriEnum.fromValue(CONTRIBUTOR_ROLE_SCHEMA_URI))
                                .id(ContributorRoleIdEnum.fromValue(role))));
    }

    public static Organisation organisation(
            String ror,
            String role,
            LocalDate today
    ) {
        return new Organisation()
                .id(ror)
                .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                .role(List.of(
                        new OrganisationRole()
                                .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                                .id(OrganizationRoleIdEnum.fromValue(role))
                                .startDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))));
    }
}
