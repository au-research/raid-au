import type { Contributor, Organisation, RaidDto, RelatedRaid } from "@/generated/raid";

const PRIMARY_TITLE_TYPE = "https://vocabulary.raid.org/title.type.schema/5";
const PRIMARY_DESCRIPTION_TYPE = "https://vocabulary.raid.org/description.type.schema/318";
const FUNDER_ORGANISATION_ROLE = "https://vocabulary.raid.org/organisation.role.schema/186";

const RELATED_RAID_TYPE_IS_PART_OF = "https://vocabulary.raid.org/relatedRaid.type.schema/202";
const RELATED_RAID_TYPE_HAS_PART = "https://vocabulary.raid.org/relatedRaid.type.schema/201";
const RELATED_RAID_TYPE_IS_DERIVED_FROM = "https://vocabulary.raid.org/relatedRaid.type.schema/200";

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
  inDefinedTermSet: string;
}

interface RelatedResearchProject {
  "@type": "ResearchProject";
  "@id": string;
  identifier: string;
  relationshipType?: string;
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
    roleName: position.id,
    startDate: position.startDate,
    endDate: position.endDate,
    member: person,
  }));

  const creditRoles: Role[] = (contributor.role ?? []).map((role) => ({
    "@type": "Role",
    "@id": role.id,
    roleName: role.id,
    member: person,
  }));

  return [...positionRoles, ...creditRoles];
}

function buildOrganisationRole(organisation: Organisation, orgRole: { id: string; startDate: string; endDate?: string }): Role {
  return {
    "@type": "Role",
    "@id": orgRole.id,
    roleName: orgRole.id,
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

  const subjects: DefinedTerm[] = (raid.subject ?? []).map((subject) => ({
    "@type": "DefinedTerm",
    "@id": subject.id,
    inDefinedTermSet: subject.schemaUri,
  }));

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
    ...buildRelatedRaidProperties(raid.relatedRaid ?? []),
  };
}
