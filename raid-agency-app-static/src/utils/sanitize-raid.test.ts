import { describe, expect, it } from "vitest";
import { sanitizeRaidForDownload } from "./sanitize-raid";
import type { RaidDto } from "@/generated/raid";

function enrichedRaid(): Partial<RaidDto> {
  return {
    identifier: {
      id: "https://raid.org/10.26259/0d7f1865",
      schemaUri: "https://raid.org",
      registrationAgency: {
        id: "https://ror.org/038sjwq14",
        schemaUri: "https://ror.org",
        // @ts-expect-error build-time embellishment, not part of the schema type
        rorDetails: { rorId: "038sjwq14", name: "ARDC", type: "Organization", rorUrl: "https://ror.org/038sjwq14" },
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
    contributor: [
      {
        id: "https://orcid.org/0000-0002-9382-744X",
        schemaUri: "https://orcid.org",
        leader: true,
        contact: true,
        position: [],
        role: [],
        // @ts-expect-error build-time embellishment, not part of the schema type
        orcidInfo: {
          orcidId: "0000-0002-9382-744X",
          authenticated: true,
          displayName: "Raoul Oehmen",
          profileUrl: "https://orcid.org/0000-0002-9382-744X",
          visibility: "public",
          style: "underline",
        },
      },
    ],
    organisation: [
      {
        id: "https://ror.org/02stey378",
        schemaUri: "https://ror.org",
        role: [],
        // @ts-expect-error build-time embellishment, not part of the schema type
        rorDetails: { rorId: "02stey378", name: "Notre Dame", country: "AU", types: ["Organization"] },
      },
    ],
    relatedObject: [
      {
        id: "https://doi.org/10.1234/abcd",
        schemaUri: "https://doi.org",
        type: { id: "", schemaUri: "" },
        category: [],
        // @ts-expect-error build-time embellishment, not part of the schema type
        citation: { text: "Some citation text." },
      },
    ],
  };
}

describe("sanitizeRaidForDownload", () => {
  it("strips orcidInfo from every contributor", () => {
    const result = sanitizeRaidForDownload(enrichedRaid());
    result.contributor?.forEach((c) => {
      expect(c).not.toHaveProperty("orcidInfo");
    });
  });

  it("strips rorDetails from every organisation", () => {
    const result = sanitizeRaidForDownload(enrichedRaid());
    result.organisation?.forEach((o) => {
      expect(o).not.toHaveProperty("rorDetails");
    });
  });

  it("strips rorDetails from identifier.registrationAgency", () => {
    const result = sanitizeRaidForDownload(enrichedRaid());
    expect(result.identifier?.registrationAgency).not.toHaveProperty(
      "rorDetails"
    );
  });

  it("strips citation from every relatedObject", () => {
    const result = sanitizeRaidForDownload(enrichedRaid());
    result.relatedObject?.forEach((ro) => {
      expect(ro).not.toHaveProperty("citation");
    });
  });

  it("preserves schema-defined fields", () => {
    const result = sanitizeRaidForDownload(enrichedRaid());
    expect(result.contributor?.[0].id).toBe(
      "https://orcid.org/0000-0002-9382-744X"
    );
    expect(result.organisation?.[0].id).toBe("https://ror.org/02stey378");
    expect(result.identifier?.registrationAgency?.id).toBe(
      "https://ror.org/038sjwq14"
    );
    expect(result.relatedObject?.[0].id).toBe("https://doi.org/10.1234/abcd");
  });

  it("does not mutate the input object", () => {
    const original = enrichedRaid();
    sanitizeRaidForDownload(original);
    expect(original.contributor?.[0]).toHaveProperty("orcidInfo");
  });
});
