import { useMemo } from "react";
import type {
  BulkUploadVocabulary,
  VocabularyEntry,
} from "../types";

/**
 * Raw entry as it appears in the shared RAiD vocabulary JSON file.
 * (e.g. src/vocab/vocabulary.json or wherever you host it in the app)
 */
export interface RawVocabularyEntry {
  field: string;
  key: string;
  value: string;
  definition?: string;
}

/**
 * Filters the full RAiD vocabulary down to just the fields the bulk
 * upload feature needs: relatedObject.type.id and relatedObject.category.id.
 *
 * In the real app, prefer fetching the vocabulary via React Query so it
 * stays fresh with the backend. This hook is a lightweight alternative
 * when the vocab is bundled as a static JSON file.
 *
 *   const vocabulary = useBulkUploadVocabulary(allVocabularyEntries);
 *   <BulkUploadComponent vocabulary={vocabulary} ... />
 */
export function useBulkUploadVocabulary(
  allEntries: RawVocabularyEntry[]
): BulkUploadVocabulary {
  return useMemo(() => {
    const relatedObjectTypes: VocabularyEntry[] = [];
    const relatedObjectCategories: VocabularyEntry[] = [];

    for (const entry of allEntries) {
      const mapped: VocabularyEntry = {
        key: entry.key,
        value: entry.value.trim(),
        definition: entry.definition,
      };

      if (entry.field === "relatedObject.type.id") {
        relatedObjectTypes.push(mapped);
      } else if (entry.field === "relatedObject.category.id") {
        relatedObjectCategories.push(mapped);
      }
    }

    // Sort alphabetically so the dropdowns are easy to scan
    relatedObjectTypes.sort((a, b) => a.value.localeCompare(b.value));
    relatedObjectCategories.sort((a, b) => a.value.localeCompare(b.value));

    return { relatedObjectTypes, relatedObjectCategories };
  }, [allEntries]);
}