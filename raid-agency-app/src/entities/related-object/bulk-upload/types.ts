/**
 * Shared vocabulary for the bulk upload feature.
 *
 * In production, these should be loaded from the API (the same source
 * that populates the manual add form's Type and Category dropdowns) via
 * a React Query hook and passed into TemplateDownloader and useBulkUpload
 * as props. Hardcoding leads to drift — see commit history where we had
 * wrong IDs for Dataset, Conference Paper, and Input/Output categories.
 *
 * The shapes here match the structure of your vocabulary JSON file,
 * filtered to just the fields the bulk upload feature needs.
 */

export interface VocabularyEntry {
  /** Full URI — used as `id` + `schemaUri` source */
  key: string;
  /** Human-readable label shown in dropdowns */
  value: string;
  /** Optional tooltip text */
  definition?: string;
}

export interface BulkUploadVocabulary {
  relatedObjectTypes: VocabularyEntry[];
  relatedObjectCategories: VocabularyEntry[];
}

// ------------------------------------------------------------------
// Helpers for extracting the numeric ID and schema URI from a full key
// ------------------------------------------------------------------

/**
 * Splits a vocabulary URI into its schema URI and numeric ID.
 *
 *   "https://vocabulary.raid.org/relatedObject.type.schema/269"
 *     → { schemaUri: "https://vocabulary.raid.org/relatedObject.type.schema/", id: "269" }
 */
export function splitVocabularyKey(key: string): {
  schemaUri: string;
  id: string;
} {
  const lastSlash = key.lastIndexOf("/");
  return {
    schemaUri: key.substring(0, lastSlash + 1),
    id: key.substring(lastSlash + 1),
  };
}

/**
 * Schema version URIs used in the type and category sub-objects of a
 * ParsedRelatedObject. These are distinct from the vocabulary entry
 * keys (which end with the item's own numeric ID) — they identify the
 * schema version the backend expects.
 *
 * Update these here if the backend vocabulary schema version changes.
 */
export const TYPE_SCHEMA_URI =
  "https://vocabulary.raid.org/relatedObject.type.schema/329";
export const CATEGORY_SCHEMA_URI =
  "https://vocabulary.raid.org/relatedObject.category.schemaUri/386";

/**
 * Builds a label-to-entry lookup map from a vocabulary array.
 * Used by the parser to convert human-readable labels from the
 * spreadsheet back into their URI form for API submission.
 *
 * The lookup is case-insensitive and trimmed to be forgiving of
 * minor user typos when they type values instead of using the dropdown.
 */
export function buildLabelLookup(
  entries: VocabularyEntry[]
): Map<string, VocabularyEntry> {
  const map = new Map<string, VocabularyEntry>();
  entries.forEach((entry) => {
    map.set(entry.value.toLowerCase().trim(), entry);
  });
  return map;
}