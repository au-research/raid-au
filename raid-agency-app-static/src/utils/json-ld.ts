import type { Contributor, Organisation, RaidDto, RelatedRaid } from "@/generated/raid";
import type { RelatedObjectWithCitation } from "@/model/raid";
import generalMapping from "@/mapping/data/general-mapping.json";
import subjectMapping from "@/mapping/data/subject-mapping.json";
import { kebabToTitle } from "@/utils";

const PRIMARY_TITLE_TYPE = "https://vocabulary.raid.org/title.type.schema/5";
const PRIMARY_DESCRIPTION_TYPE = "https://vocabulary.raid.org/description.type.schema/318";
const FUNDER_ORGANISATION_ROLE = "https://vocabulary.raid.org/organisation.role.schema/186";

const RELATED_RAID_TYPE_IS_PART_OF = "https://vocabulary.raid.org/relatedRaid.type.schema/202";
const RELATED_RAID_TYPE_HAS_PART = "https://vocabulary.raid.org/relatedRaid.type.schema/201";
const RELATED_RAID_TYPE_IS_DERIVED_FROM = "https://vocabulary.raid.org/relatedRaid.type.schema/200";

// Maps a related object's schemaUri to the identifier metadata used in the
// schema.org PropertyValue. Mirrors the backend DataCite export, which only
// treats ARK/DOI/ISBN as distinct identifier types; every other scheme
// (Handle, web.archive.org, SciCrunch, etc.) is a plain URL and falls through
// to the generic URL fallback below.
const RELATED_OBJECT_IDENTIFIER_TYPES: Record<string, { propertyID: string; name: string }> = {
  "https://doi.org/": { propertyID: "https://registry.identifiers.org/registry/doi", name: "DOI" },
  "https://arks.org/": { propertyID: "https://registry.identifiers.org/registry/ark", name: "ARK" },
  "https://www.isbn-international.org/": { propertyID: "https://registry.identifiers.org/registry/isbn", name: "ISBN" },
};

// Resolves a RAiD vocabulary URI (contributor.position, organisation.role,
// relatedRaid.type) to its human-readable label using the same static mapping
// the UI renders from. Returns undefined when the URI is not in the mapping so
// callers can decide whether to fall back to the raw URI or omit the label.
function lookupVocabLabel(uri: string): string | undefined {
  return generalMapping.find((entry) => entry.key === uri)?.value;
}

// CRediT contributor roles are not in general-mapping.json; the label is
// derived from the URI slug, mirroring contributor-roles.astro (e.g.
// https://credit.niso.org/contributor-roles/data-curation/ -> "Data curation").
function resolveCreditRoleLabel(uri: string): string {
  try {
    const slug = new URL(uri).pathname.split("/").slice(-2, -1)[0];
    return kebabToTitle(slug) || uri;
  } catch {
    return uri;
  }
}

// ANZSRC FoR subjects are keyed in subject-mapping.json by the numeric code,
// with the full linked.data.gov.au URI held on `definition`. The RaidDto
// subject id is that full URI, so it is matched against `definition`.
function lookupSubjectLabel(uri: string): string | undefined {
  return subjectMapping.find((entry) => entry.definition === uri)?.value;
}

interface PropertyValue {
  "@type": "PropertyValue";
  propertyID: string;
  name: string;
  value: string;
}

interface Role {
  "@type": "Role";
  "@id": string;
  roleName: string;
  startDate?: string;
  endDate?: string;
  member: {
    "@type": "Person" | "Organization";
    "@id": string;
    identifier: PropertyValue;
  };
}

interface DefinedTerm {
  "@type": "DefinedTerm";
  "@id": string;
  name?: string;
  inDefinedTermSet: string;
}

interface RelatedResearchProject {
  "@type": "ResearchProject";
  "@id": string;
  identifier: string;
  relationshipType?: string;
  relationshipTypeName?: string;
}

interface CreativeWorkReference {
  "@type": "CreativeWork";
  "@id": string;
  name?: string;
  identifier: PropertyValue;
  additionalType?: string | string[];
}

interface ResearchProjectJsonLd {
  "@context": "https://schema.org";
  "@type": "ResearchProject";
  "@id": string;
  name: string;
  headline: string;
  identifier: PropertyValue;
  parentOrganization: {
    "@type": "Organization";
    "@id": string;
    identifier: PropertyValue;
  };
  description?: string;
  foundingDate: string;
  dissolutionDate?: string;
  member: Role[];
  funder: Role[];
  knowsAbout: DefinedTerm[];
  citation?: CreativeWorkReference[];
  isPartOf?: RelatedResearchProject[];
  hasPart?: RelatedResearchProject[];
  isBasedOn?: RelatedResearchProject[];
  isRelatedTo?: RelatedResearchProject[];
}

function buildContributorRoles(contributor: Contributor): Role[] {
  const person = {
    "@type": "Person" as const,
    "@id": contributor.id,
    identifier: {
      "@type": "PropertyValue" as const,
      propertyID: "https://registry.identifiers.org/registry/orcid",
      name: "ORCID",
      value: contributor.id,
    },
  };

  const positionRoles: Role[] = (contributor.position ?? []).map((position) => ({
    "@type": "Role",
    "@id": position.id,
    roleName: lookupVocabLabel(position.id) ?? position.id,
    startDate: position.startDate,
    endDate: position.endDate,
    member: person,
  }));

  const creditRoles: Role[] = (contributor.role ?? []).map((role) => ({
    "@type": "Role",
    "@id": role.id,
    roleName: resolveCreditRoleLabel(role.id),
    member: person,
  }));

  return [...positionRoles, ...creditRoles];
}

function buildOrganisationRole(organisation: Organisation, orgRole: { id: string; startDate: string; endDate?: string }): Role {
  return {
    "@type": "Role",
    "@id": orgRole.id,
    roleName: lookupVocabLabel(orgRole.id) ?? orgRole.id,
    startDate: orgRole.startDate,
    endDate: orgRole.endDate,
    member: {
      "@type": "Organization",
      "@id": organisation.id,
      identifier: {
        "@type": "PropertyValue",
        propertyID: "https://registry.identifiers.org/registry/ror",
        name: "ROR",
        value: organisation.id,
      },
    },
  };
}

function buildOrganisationRoles(organisation: Organisation): Role[] {
  return organisation.role
    .filter((r) => r.id !== FUNDER_ORGANISATION_ROLE)
    .map((r) => buildOrganisationRole(organisation, r));
}

function buildFunderRoles(organisation: Organisation): Role[] {
  return organisation.role
    .filter((r) => r.id === FUNDER_ORGANISATION_ROLE)
    .map((r) => buildOrganisationRole(organisation, r));
}

function schemaOrgPropertyForRelationType(typeId: string): "isPartOf" | "hasPart" | "isBasedOn" | "isRelatedTo" {
  switch (typeId) {
    case RELATED_RAID_TYPE_IS_PART_OF:
      return "isPartOf";
    case RELATED_RAID_TYPE_HAS_PART:
      return "hasPart";
    case RELATED_RAID_TYPE_IS_DERIVED_FROM:
      return "isBasedOn";
    default:
      return "isRelatedTo";
  }
}

function buildRelatedRaidProperties(relatedRaids: RelatedRaid[]): Pick<ResearchProjectJsonLd, "isPartOf" | "hasPart" | "isBasedOn" | "isRelatedTo"> {
  const groups: Record<string, RelatedResearchProject[]> = {};

  for (const related of relatedRaids) {
    if (!related.id) continue;
    const property = schemaOrgPropertyForRelationType(related.type?.id ?? "");
    const entry: RelatedResearchProject = {
      "@type": "ResearchProject",
      "@id": related.id,
      identifier: related.id,
    };
    if (property === "isRelatedTo" && related.type?.id) {
      entry.relationshipType = related.type.id;
      const label = lookupVocabLabel(related.type.id);
      if (label) {
        entry.relationshipTypeName = label;
      }
    }
    (groups[property] ??= []).push(entry);
  }

  return {
    ...(groups.isPartOf && { isPartOf: groups.isPartOf }),
    ...(groups.hasPart && { hasPart: groups.hasPart }),
    ...(groups.isBasedOn && { isBasedOn: groups.isBasedOn }),
    ...(groups.isRelatedTo && { isRelatedTo: groups.isRelatedTo }),
  };
}

// Related objects (inputs/outputs) are emitted as a flat list of schema.org
// CreativeWork nodes under `citation`. `citation` is strictly a CreativeWork
// property whereas ResearchProject descends from Organization, but this file
// already uses CreativeWork properties (isPartOf/hasPart/isBasedOn) on the
// project loosely, consistent with how harvesters consume the output. The
// category (Input/Output/Internal) is preserved on `additionalType` rather
// than split across distinct schema.org properties (see RAID-757).
//
// The formatted citation text (APA string fetched from DOI.org by the
// fetch-raids build step, and shown on the landing page) is carried on `name`
// when present, so harvesters get the same human-readable reference the page
// displays rather than a bare identifier.
function buildRelatedObjectCitations(relatedObjects: RelatedObjectWithCitation[]): CreativeWorkReference[] {
  const citations: CreativeWorkReference[] = [];

  for (const related of relatedObjects) {
    if (!related.id) continue;

    const identifierType = RELATED_OBJECT_IDENTIFIER_TYPES[related.schemaUri ?? ""] ?? {
      propertyID: related.schemaUri ?? "",
      name: "URL",
    };

    const categoryIds = (related.category ?? [])
      .map((c) => c.id)
      .filter((id): id is string => Boolean(id));

    const citation: CreativeWorkReference = {
      "@type": "CreativeWork",
      "@id": related.id,
      identifier: {
        "@type": "PropertyValue",
        propertyID: identifierType.propertyID,
        name: identifierType.name,
        value: related.id,
      },
    };

    const citationText = related.citation?.text?.trim();
    if (citationText) {
      citation.name = citationText;
    }

    if (categoryIds.length === 1) {
      citation.additionalType = categoryIds[0];
    } else if (categoryIds.length > 1) {
      citation.additionalType = categoryIds;
    }

    citations.push(citation);
  }

  return citations;
}

export function buildResearchProjectJsonLd(raid: Partial<RaidDto>): ResearchProjectJsonLd {
  const registrationAgencyId = raid.identifier?.registrationAgency?.id ?? "";

  const description = raid.description
    ?.filter((d) => d.type.id === PRIMARY_DESCRIPTION_TYPE)
    ?.at(0)
    ?.text;

  const memberRoles: Role[] = [];
  const funderRoles: Role[] = [];

  for (const contributor of raid.contributor ?? []) {
    memberRoles.push(...buildContributorRoles(contributor));
  }

  for (const organisation of raid.organisation ?? []) {
    memberRoles.push(...buildOrganisationRoles(organisation));
    funderRoles.push(...buildFunderRoles(organisation));
  }

  const subjects: DefinedTerm[] = (raid.subject ?? []).map((subject) => {
    const label = lookupSubjectLabel(subject.id);
    return {
      "@type": "DefinedTerm",
      "@id": subject.id,
      ...(label && { name: label }),
      inDefinedTermSet: subject.schemaUri,
    };
  });

  const citations = buildRelatedObjectCitations(
    (raid.relatedObject ?? []) as RelatedObjectWithCitation[]
  );

  const primaryTitle = raid.title?.find((t) => t.type?.id === PRIMARY_TITLE_TYPE)?.text
    ?? raid.title?.at(0)?.text
    ?? "";

  const allTitles = raid.title?.map((t) => t.text).filter(Boolean) ?? [];
  const name = allTitles.length > 0 ? allTitles.join(" | ") : "";

  return {
    "@context": "https://schema.org",
    "@type": "ResearchProject",
    "@id": raid.identifier?.id ?? "",
    name,
    headline: primaryTitle,
    identifier: {
      "@type": "PropertyValue",
      propertyID: "https://registry.identifiers.org/registry/raid",
      name: "RAiD",
      value: raid.identifier?.id ?? "",
    },
    parentOrganization: {
      "@type": "Organization",
      "@id": registrationAgencyId,
      identifier: {
        "@type": "PropertyValue",
        propertyID: "https://registry.identifiers.org/registry/ror",
        name: "ROR",
        value: registrationAgencyId,
      },
    },
    ...(description !== undefined && { description }),
    foundingDate: raid.date?.startDate ?? "",
    ...(raid.date?.endDate !== undefined && { dissolutionDate: raid.date.endDate }),
    member: memberRoles,
    funder: funderRoles,
    knowsAbout: subjects,
    ...(citations.length > 0 && { citation: citations }),
    ...buildRelatedRaidProperties(raid.relatedRaid ?? []),
  };
}
