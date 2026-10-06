export type ContributorIdentifierType = "orcid" | "isni" | "unrecognised";

export const ISNI_SCHEMA_URI = "https://isni.org/";

// ISNI is 16 characters: 15 digits plus a checksum character that can be a
// digit or "X" - the same ISO 7064/27729 convention ORCID itself uses.
const isniRegex = /^https:\/\/isni\.org\/\d{15}[0-9X]$/;
const orcidBodyRegex = /^\d{4}-?\d{4}-?\d{4}-?\d{3}[0-9X]$/;

/**
 * Detects whether a pasted/typed contributor identifier value is an ORCID iD
 * (bare digits, or a full orcid.org/sandbox.orcid.org URL) or an ISNI URL, or
 * doesn't match either recognised scheme.
 */
export function detectContributorIdentifierType(value: string): ContributorIdentifierType {
  const trimmed = value.trim();
  if (!trimmed) return "unrecognised";

  if (isniRegex.test(trimmed)) return "isni";

  const orcidBody = trimmed.replace(/^https:\/\/(sandbox\.)?orcid\.org\//, "");
  if (orcidBodyRegex.test(orcidBody)) return "orcid";

  return "unrecognised";
}

// A looser check than isniRegex - matches anything clearly aimed at the ISNI
// scheme (the isni.org domain) even if the rest of the value is malformed
// (wrong digit count, an extra path segment, etc). Used to choose
// ISNI-specific guidance/error copy for a bad ISNI attempt, instead of
// silently falling back to ORCID's copy just because the strict format
// check failed.
const isniDomainRegex = /^https?:\/\/(www\.)?isni\.org\b/i;

export function looksLikeIsniAttempt(value: string): boolean {
  const trimmed = value.trim();
  return !!trimmed && isniDomainRegex.test(trimmed) && detectContributorIdentifierType(trimmed) !== "isni";
}

/**
 * Strips a recognised ORCID URL down to its bare id (for public-API calls),
 * regardless of the current runtime environment - unlike ORCID.tsx's own
 * normalizeOrcidId, which only strips the prefix matching the current
 * environment, this always recognises both orcid.org and sandbox.orcid.org.
 * Returns null when the value isn't a recognised ORCID.
 */
export function getOrcidBody(value: string): string | null {
  const trimmed = value.trim();
  const orcidBody = trimmed.replace(/^https:\/\/(sandbox\.)?orcid\.org\//, "");
  return orcidBodyRegex.test(orcidBody) ? orcidBody : null;
}
