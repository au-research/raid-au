// RAID-536: Test data fixtures for RAiD creation
// minimalValidRaid - satisfies validation with required fields only
// fullyPopulatedRaid - all sections populated for comprehensive testing

import {
  ACCESS_TYPE,
  TITLE_TYPE,
  DESCRIPTION_TYPE,
  CONTRIBUTOR_POSITION,
  CONTRIBUTOR_ROLE,
  ORGANISATION_ROLE,
  RELATED_OBJECT_TYPE,
  RELATED_OBJECT_CATEGORY,
  RELATED_RAID_TYPE,
  LANGUAGE_SCHEMA_URI,
  LANGUAGE,
} from "./vocabulary";

export const minimalValidRaid = {
  title: [
    {
      text: "E2E Test RAiD - Minimal",
      type: {
        id: TITLE_TYPE.PRIMARY,
        schemaUri:
          "https://vocabulary.raid.org/title.type.schema/",
      },
      language: {
        id: LANGUAGE.ENGLISH,
        schemaUri: LANGUAGE_SCHEMA_URI,
      },
      startDate: "2024-01-01",
    },
  ],
  date: {
    startDate: "2024-01-01",
  },
  access: {
    type: {
      id: ACCESS_TYPE.OPEN,
      schemaUri:
        "https://vocabularies.coar-repositories.org/access_rights/",
    },
  },
};

export const fullyPopulatedRaid = {
  title: [
    {
      text: "E2E Test RAiD - Fully Populated",
      type: {
        id: TITLE_TYPE.PRIMARY,
        schemaUri:
          "https://vocabulary.raid.org/title.type.schema/",
      },
      language: {
        id: LANGUAGE.ENGLISH,
        schemaUri: LANGUAGE_SCHEMA_URI,
      },
      startDate: "2024-01-01",
    },
    {
      text: "E2E Alt Title",
      type: {
        id: TITLE_TYPE.ALTERNATIVE,
        schemaUri:
          "https://vocabulary.raid.org/title.type.schema/",
      },
      language: {
        id: LANGUAGE.ENGLISH,
        schemaUri: LANGUAGE_SCHEMA_URI,
      },
      startDate: "2024-01-01",
    },
  ],
  date: {
    startDate: "2024-01-01",
    endDate: "2024-12-31",
  },
  access: {
    type: {
      id: ACCESS_TYPE.OPEN,
      schemaUri:
        "https://vocabularies.coar-repositories.org/access_rights/",
    },
  },
  description: [
    {
      text: "This is an E2E test RAiD created for automated testing purposes.",
      type: {
        id: DESCRIPTION_TYPE.PRIMARY,
        schemaUri:
          "https://vocabulary.raid.org/description.type.schema/",
      },
      language: {
        id: LANGUAGE.ENGLISH,
        schemaUri: LANGUAGE_SCHEMA_URI,
      },
    },
  ],
  contributor: [
    {
      id: "https://orcid.org/0000-0000-0000-0001",
      leader: true,
      contact: true,
      position: [
        {
          id: CONTRIBUTOR_POSITION.PRINCIPAL_INVESTIGATOR,
          schemaUri:
            "https://vocabulary.raid.org/contributor.position.schema/",
          startDate: "2024-01-01",
        },
      ],
      role: [
        {
          id: CONTRIBUTOR_ROLE.CONCEPTUALIZATION,
          schemaUri: "https://credit.niso.org/contributor-roles/",
        },
      ],
    },
  ],
  organisation: [
    {
      id: "https://ror.org/038sjwq14",
      schemaUri: "https://ror.org/",
      role: [
        {
          id: ORGANISATION_ROLE.LEAD_RESEARCH_ORGANISATION,
          schemaUri:
            "https://vocabulary.raid.org/organisation.role.schema/",
          startDate: "2024-01-01",
        },
      ],
    },
  ],
  alternateIdentifier: [
    {
      id: "e2e-test-alt-id-001",
      type: "local",
    },
  ],
  alternateUrl: [
    {
      url: "https://example.com/e2e-test-raid",
    },
  ],
};
