import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import Ajv2019, { type ValidateFunction } from "ajv/dist/2019";
import addFormats from "ajv-formats";
import { describe, expect, it } from "vitest";
import { buildResearchProjectJsonLd } from "./json-ld";
import type { RaidDto } from "@/generated/raid";

// RAID-782: the static site's JSON-LD output (json-ld.ts) is hand-maintained,
// while the canonical shape of RAiD's schema.org ResearchProject lives in the
// LinkML schema (api-svc/datamodel/src/v2/researchproject.yaml), compiled to the
// JSON Schema below. This suite validates buildResearchProjectJsonLd() output
// against that schema so CI fails if the emitter drifts from the canonical
// definition (see RAID-759 Phase 3a; the decision rationale is recorded in
// doc/adr).
//
// The schema is read at runtime rather than statically imported so it stays out
// of the TypeScript type graph and `astro check`, and so the cross-module path to
// the datamodel module is resolved by Node at test time. CI checks out the whole
// monorepo, so this relative path resolves there too.
const SCHEMA_PATH = fileURLToPath(
  new URL(
    "../../../api-svc/datamodel/generated/v2/researchproject.json",
    import.meta.url,
  ),
);
const RESEARCH_PROJECT_REF =
  "https://example.org/raid-schema-org#/$defs/ResearchProject";

const schema = JSON.parse(readFileSync(SCHEMA_PATH, "utf-8"));

// strict:false — the LinkML output carries metaschema keywords (version:null,
// title, metamodel_version, additionalProperties:true at the root) that ajv's
// strict mode would reject. addFormats enables the `date` format assertion.
const ajv = new Ajv2019({ strict: false, allErrors: true });
addFormats(ajv);
ajv.addSchema(schema);

function researchProjectValidator(): ValidateFunction {
  const validate = ajv.getSchema(RESEARCH_PROJECT_REF);
  if (!validate) {
    throw new Error(
      `Could not resolve ${RESEARCH_PROJECT_REF}; the LinkML-generated schema may have changed shape (regenerate researchproject.json).`,
    );
  }
  return validate;
}

function expectValid(jsonLd: unknown): void {
  const validate = researchProjectValidator();
  const valid = validate(jsonLd);
  if (!valid) {
    // Surface the ajv errors so a divergence is diagnosable straight from CI output.
    // eslint-disable-next-line no-console
    console.error(JSON.stringify(validate.errors, null, 2));
  }
  expect(valid).toBe(true);
}

const PRIMARY_TITLE_TYPE = "https://vocabulary.raid.org/title.type.schema/5";
const PRIMARY_DESCRIPTION_TYPE =
  "https://vocabulary.raid.org/description.type.schema/318";
const FUNDER_ORGANISATION_ROLE =
  "https://vocabulary.raid.org/organisation.role.schema/186";

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

// A fixture that exercises every field buildResearchProjectJsonLd() can emit, so
// the validation covers every schema class (CreativeWork, RelatedResearchProject,
// Member with a resolved name, DefinedTerm, PropertyValue, Organization) and every
// property, not just the happy-path core.
function maximalRaid(): Partial<RaidDto> {
  const raid = minimalRaid();

  // Registration agency enriched with a resolved ROR name (RAID-794).
  raid.identifier = {
    ...raid.identifier!,
    registrationAgency: {
      id: "https://ror.org/038sjwq14",
      schemaUri: "https://ror.org",
      rorDetails: { name: "Australian Research Data Commons" },
    } as never,
  };

  raid.date = { startDate: "2024-01-01", endDate: "2025-12-31" };

  raid.title = [
    {
      text: "Primary project title",
      type: { id: PRIMARY_TITLE_TYPE, schemaUri: "" },
      startDate: "2024-01-01",
      language: {
        id: "eng",
        schemaUri: "https://www.iso.org/standard/74575.html",
      },
    },
    {
      text: "Alternative title",
      type: {
        id: "https://vocabulary.raid.org/title.type.schema/4",
        schemaUri: "",
      },
      startDate: "2024-01-01",
    },
  ];

  raid.description = [
    {
      text: "The primary description of the project.",
      type: { id: PRIMARY_DESCRIPTION_TYPE, schemaUri: "" },
      language: {
        id: "eng",
        schemaUri: "https://www.iso.org/standard/74575.html",
      },
    },
  ];

  raid.contributor = [
    {
      id: "https://orcid.org/0000-0002-4582-7728",
      schemaUri: "https://orcid.org",
      position: [
        {
          schemaUri: "https://vocabulary.raid.org",
          id: "https://vocabulary.raid.org/contributor.position.schema/307",
          startDate: "2024-01-01",
          endDate: "2025-12-31",
        },
      ],
      role: [
        {
          schemaUri: "https://credit.niso.org",
          id: "https://credit.niso.org/contributor-roles/data-curation/",
        },
      ],
    },
  ];

  raid.organisation = [
    // Non-funder organisation -> member role, with a resolved ROR name (Member.name).
    {
      id: "https://ror.org/03sd43014",
      schemaUri: "https://ror.org",
      rorDetails: { name: "QCIF Ltd." },
      role: [
        {
          schemaUri: "https://vocabulary.raid.org",
          id: "https://vocabulary.raid.org/organisation.role.schema/185",
          startDate: "2024-01-01",
        },
      ],
    } as never,
    // Funder organisation -> funder role.
    {
      id: "https://ror.org/04yx6dh41",
      schemaUri: "https://ror.org",
      rorDetails: { name: "Australian Research Council" },
      role: [
        {
          schemaUri: "https://vocabulary.raid.org",
          id: FUNDER_ORGANISATION_ROLE,
          startDate: "2024-01-01",
          endDate: "2025-12-31",
        },
      ],
    } as never,
  ];

  raid.subject = [
    {
      id: "https://linked.data.gov.au/def/anzsrc-for/2020/420399",
      schemaUri: "https://vocabs.ardc.edu.au/viewById/316",
    },
  ];

  // Related objects -> citation CreativeWork nodes: one with a single category
  // (additionalType as a one-element array) and formatted citation text, one with
  // multiple categories.
  raid.relatedObject = [
    {
      id: "https://doi.org/10.1007/s00442-014-2977-8",
      schemaUri: "https://doi.org/",
      type: {
        id: "https://vocabulary.raid.org/relatedObject.type.schema/250",
        schemaUri: "",
      },
      category: [
        {
          id: "https://vocabulary.raid.org/relatedObject.category.id/190",
          schemaUri: "",
        },
      ],
      citation: {
        text: "Smith, J. (2014). A study of things. Oecologia, 176(2), 1-10.",
      },
    },
    {
      id: "https://doi.org/10.4227/05/598bd8a2e9e76",
      schemaUri: "https://doi.org/",
      type: {
        id: "https://vocabulary.raid.org/relatedObject.type.schema/269",
        schemaUri: "",
      },
      category: [
        {
          id: "https://vocabulary.raid.org/relatedObject.category.id/190",
          schemaUri: "",
        },
        {
          id: "https://vocabulary.raid.org/relatedObject.category.id/192",
          schemaUri: "",
        },
      ],
    },
  ] as unknown as RaidDto["relatedObject"];

  // Related RAiDs covering all four schema.org relationship properties.
  raid.relatedRaid = [
    {
      id: "https://raid.org/10.71821/a945d761",
      type: {
        id: "https://vocabulary.raid.org/relatedRaid.type.schema/202",
        schemaUri: "https://vocabulary.raid.org/relatedRaid.type.schema",
      },
    },
    {
      id: "https://raid.org/10.71821/23fcbc6f",
      type: {
        id: "https://vocabulary.raid.org/relatedRaid.type.schema/201",
        schemaUri: "https://vocabulary.raid.org/relatedRaid.type.schema",
      },
    },
    {
      id: "https://raid.org/10.26259/abcd1234",
      type: {
        id: "https://vocabulary.raid.org/relatedRaid.type.schema/200",
        schemaUri: "https://vocabulary.raid.org/relatedRaid.type.schema",
      },
    },
    {
      id: "https://raid.org/10.26259/efgh5678",
      type: {
        id: "https://vocabulary.raid.org/relatedRaid.type.schema/204",
        schemaUri: "https://vocabulary.raid.org/relatedRaid.type.schema",
      },
    },
  ];

  return raid;
}

describe("buildResearchProjectJsonLd conforms to the LinkML researchproject schema", () => {
  it("resolves the ResearchProject subschema from the generated JSON Schema", () => {
    expect(researchProjectValidator()).toBeTypeOf("function");
  });

  it("validates the minimal RAiD output", () => {
    expectValid(buildResearchProjectJsonLd(minimalRaid()));
  });

  it("validates the maximal RAiD output covering every emitted field", () => {
    expectValid(buildResearchProjectJsonLd(maximalRaid()));
  });

  it("validates an organisation member carrying a resolved ROR name (RAID-794 -> Member.name)", () => {
    const raid = minimalRaid();
    raid.organisation = [
      {
        id: "https://ror.org/03sd43014",
        schemaUri: "https://ror.org",
        rorDetails: { name: "QCIF Ltd." },
        role: [
          {
            schemaUri: "https://vocabulary.raid.org",
            id: "https://vocabulary.raid.org/organisation.role.schema/185",
            startDate: "2024-01-01",
          },
        ],
      } as never,
    ];
    expectValid(buildResearchProjectJsonLd(raid));
  });

  it("validates a single-category related object (additionalType emitted as an array)", () => {
    const raid = minimalRaid();
    raid.relatedObject = [
      {
        id: "https://doi.org/10.1007/single-category",
        schemaUri: "https://doi.org/",
        type: {
          id: "https://vocabulary.raid.org/relatedObject.type.schema/250",
          schemaUri: "",
        },
        category: [
          {
            id: "https://vocabulary.raid.org/relatedObject.category.id/190",
            schemaUri: "",
          },
        ],
      },
    ];
    const output = buildResearchProjectJsonLd(raid);
    expect(output.citation?.[0].additionalType).toEqual([
      "https://vocabulary.raid.org/relatedObject.category.id/190",
    ]);
    expectValid(output);
  });

  // Negative control: proves the suite validates against the strict ResearchProject
  // subschema (additionalProperties:false) rather than the permissive schema root,
  // so a genuine divergence really would be rejected.
  it("rejects output carrying an unexpected top-level property", () => {
    const output = buildResearchProjectJsonLd(minimalRaid()) as Record<
      string,
      unknown
    >;
    const validate = researchProjectValidator();
    expect(validate({ ...output, unexpectedProperty: "should not validate" })).toBe(
      false,
    );
  });
});
