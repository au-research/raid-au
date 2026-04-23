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
  .superRefine((items, ctx) => {
    const seen = new Map<string, number>(); // "url|||typeId" -> first index

    items.forEach((item, index) => {
      const key = `${item.id ?? ""}|||${item.type?.id ?? ""}`;
      if (seen.has(key)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `Duplicate: this URL and type combination already exists (see item #${(seen.get(key) ?? 0) + 1})`,
          path: [index, "id"],
        });
      } else {
        seen.set(key, index);
      }
    });
  });
