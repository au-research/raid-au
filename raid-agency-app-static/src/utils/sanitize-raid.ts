import type { RaidDto } from "@/generated/raid";

/**
 * Strips build-time UI embellishments (orcidInfo, rorDetails, citation) that
 * are added by scripts/fetch-orcidData.js, scripts/fetch-ror.js and
 * scripts/fetch-citation.js for on-page rendering only, and are not part of
 * the RAiD metadata schema. See RAID-840.
 */
export function sanitizeRaidForDownload(
  raid: Partial<RaidDto>
): Partial<RaidDto> {
  const sanitized = structuredClone(raid) as Record<string, unknown>;

  const contributors = sanitized.contributor as
    | Array<Record<string, unknown>>
    | undefined;
  contributors?.forEach((contributor) => {
    delete contributor.orcidInfo;
  });

  const organisations = sanitized.organisation as
    | Array<Record<string, unknown>>
    | undefined;
  organisations?.forEach((organisation) => {
    delete organisation.rorDetails;
  });

  const registrationAgency = (sanitized.identifier as Record<string, unknown>)
    ?.registrationAgency as Record<string, unknown> | undefined;
  if (registrationAgency) {
    delete registrationAgency.rorDetails;
  }

  const relatedObjects = sanitized.relatedObject as
    | Array<Record<string, unknown>>
    | undefined;
  relatedObjects?.forEach((relatedObject) => {
    delete relatedObject.citation;
  });

  return sanitized as Partial<RaidDto>;
}
