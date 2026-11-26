// The organisation that runs this RAiD service (RAID-1066). Every field is
// optional and is read whatever login theme the realm uses, like supportEmail.
// The About, Privacy and Usage Terms pages use it, and leave out what is not set.

export interface Organisation {
  /** Shown in the privacy and terms wording, for example "Example Agency". */
  name?: string;
  /** The organisation's home page. Linked from the About page. https only. */
  url?: string;
  /** Linked from the Privacy page. https only. */
  privacyPolicyUrl?: string;
  /** Linked from the Usage Terms page. https only. */
  termsUrl?: string;
  /** How long a deletion request takes. The Privacy page promises no timeframe without it. */
  dataDeletionDays?: number;
}

export const ORGANISATION_LINK_FIELDS = ["url", "privacyPolicyUrl", "termsUrl"] as const;

export function isHttpsUrl(value: unknown): value is string {
  if (typeof value !== "string") return false;
  try {
    return new URL(value).protocol === "https:";
  } catch {
    return false;
  }
}

export function isPositiveWholeNumber(value: unknown): value is number {
  return typeof value === "number" && Number.isInteger(value) && value > 0;
}

/**
 * Reads the organisation section, keeping only usable values. A wrong value is
 * dropped (validateConfig names it in the warning banner), so a bad address can
 * never become a link.
 */
export function readOrganisation(value: unknown): Organisation {
  if (value === null || typeof value !== "object" || Array.isArray(value)) return {};
  const raw = value as Record<string, unknown>;
  const organisation: Organisation = {};

  if (typeof raw.name === "string" && raw.name.trim() !== "") {
    organisation.name = raw.name.trim();
  }
  for (const field of ORGANISATION_LINK_FIELDS) {
    // Trimmed, because validation trims before it checks, so a stray space must
    // not reach an href.
    const link = typeof raw[field] === "string" ? (raw[field] as string).trim() : raw[field];
    if (isHttpsUrl(link)) organisation[field] = link;
  }
  if (isPositiveWholeNumber(raw.dataDeletionDays)) {
    organisation.dataDeletionDays = raw.dataDeletionDays;
  }
  return organisation;
}
