import subjectMapping from "@/mapping/data/subject-mapping.json";

const LOWERCASE_WORDS = new Set(["and", "of", "the", "in", "for", "to", "on", "or", "excl.", "incl."]);

// The ANZSRC vocabulary publishes division labels in capitals
// (e.g. "HEALTH SCIENCES"). Group and field labels are already mixed case.
function toTitleCase(label: string): string {
  if (label !== label.toUpperCase()) {
    return label;
  }
  return label
    .toLowerCase()
    .split(" ")
    .map((word, index) => {
      if (index > 0 && LOWERCASE_WORDS.has(word)) {
        return word;
      }
      return word.replace(/[a-z]/, (letter) => letter.toUpperCase());
    })
    .join(" ");
}

export interface SubjectDisplay {
  label: string | undefined;
  code: string;
  text: string;
}

/**
 * Resolves a RAiD subject id (an ANZSRC FoR or SEO URI) to display text.
 * An id that is not in the mapping falls back to its code, so the page never
 * renders "undefined".
 */
export function getSubjectDisplay(subjectId: string | undefined): SubjectDisplay {
  const entry = subjectMapping.find((mapEl) => mapEl.definition === subjectId);
  if (entry) {
    const label = toTitleCase(entry.value);
    return { label, code: entry.key, text: `${label} (${entry.key})` };
  }
  const code = subjectId?.split("/").filter(Boolean).pop() ?? "";
  return { label: undefined, code, text: code || "---" };
}
