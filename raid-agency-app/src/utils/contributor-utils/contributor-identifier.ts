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
