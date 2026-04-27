import { relatedObjectCategoryValidationSchema } from "@/entities/related-object-category/validation-schema/related-object-category-validation-schema";
import { z } from "zod";

const doiRegex = /^https:\/\/doi\.org\/10\.\d{4,9}\/.+$/;
const webArchiveRegex =
  /^https:\/\/web\.archive\.org\/web\/\d{14}\/https:\/\/.*/;

const relatedObjectIdSchema = z
  .string()
  .trim()
  .url()
  .refine(
    (url) => doiRegex.test(url) || webArchiveRegex.test(url),
    {
      message:
        "URL must be a valid DOI (https://doi.org/10.xxxx/...) or a Web Archive snapshot (https://web.archive.org/web/{14-digit-timestamp}/https://...)",
    }
  );

export const relatedObjectValidationSchema = z
  .array(
    z.object({
      id: relatedObjectIdSchema,
      schemaUri: z.string().min(1),
      type: z.object({
        id: z.string(),
        schemaUri: z.string(),
      }),
      category: relatedObjectCategoryValidationSchema,
    })
  )
  .max(100, "Related Objects cannot exceed 100 items. Please remove some before saving.")
  .superRefine((items, ctx) => {
    // First pass: group indices by "url|||typeId" key
    const keyToIndices = new Map<string, number[]>();
    items.forEach((item, index) => {
      const key = `${item.id ?? ""}|||${item.type?.id ?? ""}`;
      const existing = keyToIndices.get(key) ?? [];
      existing.push(index);
      keyToIndices.set(key, existing);
    });

    // Second pass: flag every item in each duplicate group, naming the others
    for (const indices of keyToIndices.values()) {
      if (indices.length < 2) continue;
      for (const index of indices) {
        const others = indices
          .filter((i) => i !== index)
          .map((i) => `#${i + 1}`)
          .join(", ");
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Duplicate: same URL and Type as item ${others}`,
          path: [index, "id"],
        });
      }
    }
  });
