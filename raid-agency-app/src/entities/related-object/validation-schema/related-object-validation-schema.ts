import { relatedObjectCategoryValidationSchema } from "@/entities/related-object-category/validation-schema/related-object-category-validation-schema";
import { z } from "zod";

const relatedObjectIdSchema = z
  .string()
  .trim()
  .url()
  .refine(
    (url) =>
      url.startsWith("https://doi.org") ||
      url.startsWith("https://web.archive.org"),
    {
      message:
        "URL must start with https://doi.org or https://web.archive.org",
    }
  );

export const relatedObjectValidationSchema = z.array(
  z.object({
    id: relatedObjectIdSchema,
    schemaUri: z.string().min(1),
    type: z.object({
      id: z.string(),
      schemaUri: z.string(),
    }),
    category: relatedObjectCategoryValidationSchema,
  })
);
