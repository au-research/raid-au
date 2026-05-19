import type { Contributor, Organisation, RaidDto } from "@/generated/raid";

const PRIMARY_DESCRIPTION_TYPE = "https://vocabulary.raid.org/description.type.schema/318";
const FUNDER_ORGANISATION_ROLE = "https://vocabulary.raid.org/organisation.role.schema/186";

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

interface ResearchProjectJsonLd {
  "@context": "https://schema.org";
  "@type": "ResearchProject";
  "@id": string;
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

  return {
    "@context": "https://schema.org",
    "@type": "ResearchProject",
    "@id": raid.identifier?.id ?? "",
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
  };
}
