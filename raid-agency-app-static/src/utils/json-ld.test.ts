import { describe, expect, it } from "vitest";
import { buildResearchProjectJsonLd } from "./json-ld";
import type { RaidDto } from "@/generated/raid";

const PRIMARY_DESCRIPTION_TYPE = "https://vocabulary.raid.org/description.type.schema/318";
const FUNDER_ORGANISATION_ROLE = "https://vocabulary.raid.org/organisation.role.schema/186";

function minimalRaid(): Partial<RaidDto> {
  return {
    identifier: {
      id: "https://raid.org/10.26259/0d7f1865",
      schemaUri: "https://raid.org",
      registrationAgency: {
        id: "https://ror.org/038sjwq14",
        schemaUri: "https://ror.org",
      },
      owner: {
        id: "https://ror.org/038sjwq14",
        schemaUri: "https://ror.org",
        servicePoint: 1,
      },
      raidAgencyUrl: "",
      license: "",
      version: 1,
    },
    date: {
      startDate: "2025-05-26",
    },
  };
}

describe("buildResearchProjectJsonLd", () => {
  it("produces correct context and type", () => {
    const result = buildResearchProjectJsonLd(minimalRaid());
    expect(result["@context"]).toBe("https://schema.org");
    expect(result["@type"]).toBe("ResearchProject");
  });

  it("sets @id and identifier from raid identifier", () => {
    const result = buildResearchProjectJsonLd(minimalRaid());
    expect(result["@id"]).toBe("https://raid.org/10.26259/0d7f1865");
    expect(result.identifier).toEqual({
      "@type": "PropertyValue",
      propertyID: "https://registry.identifiers.org/registry/raid",
      name: "RAiD",
      value: "https://raid.org/10.26259/0d7f1865",
    });
  });

  it("sets parentOrganization from registration agency", () => {
    const result = buildResearchProjectJsonLd(minimalRaid());
    expect(result.parentOrganization).toEqual({
      "@type": "Organization",
      "@id": "https://ror.org/038sjwq14",
      identifier: {
        "@type": "PropertyValue",
        propertyID: "https://registry.identifiers.org/registry/ror",
        name: "ROR",
        value: "https://ror.org/038sjwq14",
      },
    });
  });

  it("extracts primary description", () => {
    const raid = minimalRaid();
    raid.description = [
      {
        text: "Secondary description",
        type: { id: "https://vocabulary.raid.org/description.type.schema/319", schemaUri: "" },
      },
      {
        text: "The aim of the study is to investigate...",
        type: { id: PRIMARY_DESCRIPTION_TYPE, schemaUri: "" },
      },
    ];

    const result = buildResearchProjectJsonLd(raid);
    expect(result.description).toBe("The aim of the study is to investigate...");
  });

  it("returns null description when no primary description exists", () => {
    const raid = minimalRaid();
    raid.description = [
      {
        text: "Not primary",
        type: { id: "https://vocabulary.raid.org/description.type.schema/319", schemaUri: "" },
      },
    ];

    const result = buildResearchProjectJsonLd(raid);
    expect(result.description).toBeNull();
  });

  it("sets foundingDate and dissolutionDate from raid dates", () => {
    const raid = minimalRaid();
    raid.date = { startDate: "2025-01-01", endDate: "2026-12-31" };

    const result = buildResearchProjectJsonLd(raid);
    expect(result.foundingDate).toBe("2025-01-01");
    expect(result.dissolutionDate).toBe("2026-12-31");
  });

  it("sets dissolutionDate to null when no end date", () => {
    const result = buildResearchProjectJsonLd(minimalRaid());
    expect(result.dissolutionDate).toBeNull();
  });

  it("maps contributors to member roles with Person and ORCID identifier", () => {
    const raid = minimalRaid();
    raid.contributor = [
      {
        id: "https://orcid.org/0000-0002-4582-7728",
        schemaUri: "https://orcid.org",
        position: [
          {
            schemaUri: "https://vocabulary.raid.org",
            id: "https://vocabulary.raid.org/contributor.position.schema/307",
            startDate: "2025-01-01",
            endDate: "2025-12-31",
          },
        ],
        role: [
          {
            schemaUri: "https://vocabulary.raid.org",
            id: "https://vocabulary.raid.org/contributor.role.schema/conceptualization",
          },
        ],
      },
    ];

    const result = buildResearchProjectJsonLd(raid);
    expect(result.member).toHaveLength(2);

    const positionRole = result.member[0];
    expect(positionRole["@type"]).toBe("Role");
    expect(positionRole["@id"]).toBe("https://vocabulary.raid.org/contributor.position.schema/307");
    expect(positionRole.roleName).toBe("https://vocabulary.raid.org/contributor.position.schema/307");
    expect(positionRole.startDate).toBe("2025-01-01");
    expect(positionRole.endDate).toBe("2025-12-31");
    expect(positionRole.member).toEqual({
      "@type": "Person",
      "@id": "https://orcid.org/0000-0002-4582-7728",
      identifier: {
        "@type": "PropertyValue",
        propertyID: "https://registry.identifiers.org/registry/orcid",
        name: "ORCID",
        value: "https://orcid.org/0000-0002-4582-7728",
      },
    });

    const creditRole = result.member[1];
    expect(creditRole["@type"]).toBe("Role");
    expect(creditRole.roleName).toBe("https://vocabulary.raid.org/contributor.role.schema/conceptualization");
    expect(creditRole.member["@type"]).toBe("Person");
  });

  it("maps non-funder organisations to member roles", () => {
    const raid = minimalRaid();
    raid.organisation = [
      {
        id: "https://ror.org/04yx6dh41",
        schemaUri: "https://ror.org",
        role: [
          {
            schemaUri: "https://vocabulary.raid.org",
            id: "https://vocabulary.raid.org/organisation.role.schema/185",
            startDate: "2025-01-01",
          },
        ],
      },
    ];

    const result = buildResearchProjectJsonLd(raid);
    expect(result.member).toHaveLength(1);
    expect(result.funder).toHaveLength(0);

    const orgRole = result.member[0];
    expect(orgRole["@type"]).toBe("Role");
    expect(orgRole["@id"]).toBe("https://vocabulary.raid.org/organisation.role.schema/185");
    expect(orgRole.member).toEqual({
      "@type": "Organization",
      "@id": "https://ror.org/04yx6dh41",
      identifier: {
        "@type": "PropertyValue",
        propertyID: "https://registry.identifiers.org/registry/ror",
        name: "ROR",
        value: "https://ror.org/04yx6dh41",
      },
    });
  });

  it("maps funder organisations to funder roles", () => {
    const raid = minimalRaid();
    raid.organisation = [
      {
        id: "https://ror.org/04yx6dh41",
        schemaUri: "https://ror.org",
        role: [
          {
            schemaUri: "https://vocabulary.raid.org",
            id: FUNDER_ORGANISATION_ROLE,
            startDate: "2025-01-01",
          },
        ],
      },
    ];

    const result = buildResearchProjectJsonLd(raid);
    expect(result.member).toHaveLength(0);
    expect(result.funder).toHaveLength(1);

    const funderRole = result.funder[0];
    expect(funderRole["@id"]).toBe(FUNDER_ORGANISATION_ROLE);
    expect(funderRole.member["@type"]).toBe("Organization");
  });

  it("splits organisation with both funder and non-funder roles", () => {
    const raid = minimalRaid();
    raid.organisation = [
      {
        id: "https://ror.org/04yx6dh41",
        schemaUri: "https://ror.org",
        role: [
          {
            schemaUri: "https://vocabulary.raid.org",
            id: "https://vocabulary.raid.org/organisation.role.schema/185",
            startDate: "2025-01-01",
          },
          {
            schemaUri: "https://vocabulary.raid.org",
            id: FUNDER_ORGANISATION_ROLE,
            startDate: "2025-02-01",
          },
        ],
      },
    ];

    const result = buildResearchProjectJsonLd(raid);
    expect(result.member).toHaveLength(1);
    expect(result.funder).toHaveLength(1);
    expect(result.member[0]["@id"]).toBe("https://vocabulary.raid.org/organisation.role.schema/185");
    expect(result.funder[0]["@id"]).toBe(FUNDER_ORGANISATION_ROLE);
  });

  it("maps subjects to knowsAbout DefinedTerms", () => {
    const raid = minimalRaid();
    raid.subject = [
      {
        id: "https://linked.data.gov.au/def/anzsrc-for/2020/420399",
        schemaUri: "https://vocabs.ardc.edu.au/viewById/316",
      },
    ];

    const result = buildResearchProjectJsonLd(raid);
    expect(result.knowsAbout).toEqual([
      {
        "@type": "DefinedTerm",
        "@id": "https://linked.data.gov.au/def/anzsrc-for/2020/420399",
        inDefinedTermSet: "https://vocabs.ardc.edu.au/viewById/316",
      },
    ]);
  });

  it("handles empty/missing optional fields", () => {
    const result = buildResearchProjectJsonLd({});
    expect(result["@id"]).toBe("");
    expect(result.identifier.value).toBe("");
    expect(result.parentOrganization["@id"]).toBe("");
    expect(result.description).toBeNull();
    expect(result.foundingDate).toBe("");
    expect(result.dissolutionDate).toBeNull();
    expect(result.member).toEqual([]);
    expect(result.funder).toEqual([]);
    expect(result.knowsAbout).toEqual([]);
  });
});
