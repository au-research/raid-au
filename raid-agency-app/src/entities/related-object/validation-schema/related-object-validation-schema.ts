import { relatedObjectCategoryValidationSchema } from "@/entities/related-object-category/validation-schema/related-object-category-validation-schema";
import { inferRelatedObjectSchemaUri } from "@/utils/related-object-utils/related-object-schema-uri";
import { z } from "zod";

const urlTypeLabels: Record<string, string> = {
  "https://doi.org/": "DOI",
  "https://web.archive.org/": "web.archive.org URL",
  "https://hdl.handle.net/": "Handle",
  "https://scicrunch.org/resolver/": "RRID",
  "https://arks.org/": "ARK",
};

const doiRegex = /^https:\/\/doi\.org\/10\.\d{4,9}\/[^\s]+$/;
const webArchiveRegex =
  /^https:\/\/web\.archive\.org\/web\/\d{14}\/https:\/\/.*/;
const handleRegex = /^https:\/\/hdl\.handle\.net\/\d+(?:\.\d+)*\/[^\s]+$/;
const rridRegex = /^https:\/\/scicrunch\.org\/resolver\/RRID:[^\s_]+_[^\s]+$/;
// NAAN must be exactly 5 or 9 digits; "ark:" must be the first path segment
// after the host (RAID-793) — non-numeric NAANs are a known, unsupported edge case.
const arkRegex = /^https:\/\/[^/\s]+\/ark:\/?(?:\d{5}|\d{9})\/[^\s]+$/i;

const schemaUriRegexes: Record<string, RegExp> = {
  "https://doi.org/": doiRegex,
  "https://web.archive.org/": webArchiveRegex,
  "https://hdl.handle.net/": handleRegex,
  "https://scicrunch.org/resolver/": rridRegex,
  "https://arks.org/": arkRegex,
};

const relatedObjectIdSchema = z
  .string()
  .trim()
  .url()
  .refine(
    (url) => {
      // Only enforce the strict per-scheme shape once the host/path already
      // looks like an attempt at a known scheme. A URL that doesn't match any
      // recognised scheme at all is a generic related-object identifier and
      // is left as-is, with no schemaUri inferred.
      const schemaUri = inferRelatedObjectSchemaUri(url);
      return !schemaUri || schemaUriRegexes[schemaUri].test(url);
    },
    {
      message:
        "URL must be a valid DOI, Handle, RRID, ARK, or Web Archive snapshot URL",
    }
  );

export const relatedObjectValidationSchema = z
  .array(
    z.object({
      id: relatedObjectIdSchema,
      // Left blank when the id doesn't match a recognised scheme (see
      // relatedObjectIdSchema above) — a generic related-object URL is still
      // a valid, submittable entry without an inferred schemaUri.
      schemaUri: z.string(),
      type: z.object({
        id: z.string(),
        schemaUri: z.string(),
      }),
      category: relatedObjectCategoryValidationSchema,
    })
  )
  .superRefine((items, ctx) => {
    // Group indices by URL only — one DOI/URL can only be linked to one type
    const keyToIndices = new Map<string, number[]>();
    items.forEach((item, index) => {
      const key = (item.id ?? "").trim().toLowerCase();
      if (!key) return;
      const existing = keyToIndices.get(key) ?? [];
      existing.push(index);
      keyToIndices.set(key, existing);
    });

    for (const indices of keyToIndices.values()) {
      if (indices.length < 2) continue;
      for (const index of indices) {
        const others = indices
          .filter((i) => i !== index)
          .map((i) => `#${i + 1}`)
          .join(", ");
        const schemaUri = inferRelatedObjectSchemaUri(items[index]?.id ?? "");
        const urlType = (schemaUri && urlTypeLabels[schemaUri]) || "DOI";
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Duplicate URL - One ${urlType} can only be linked to one type, see Related Object ${others}`,
          path: [index, "id"],
        });
      }
    }
  });
