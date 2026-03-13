import { relatedObjectCategoryValidationSchema } from "@/entities/related-object-category/validation-schema/related-object-category-validation-schema";
import { z } from "zod";

const doiUrlSchema = z.string().trim().url().startsWith("https://doi.org", {
  message: "URL must be a DOI link starting with https://doi.org",
});
export const relatedObjectValidationSchema = z.array(
  z.object({
    id: doiUrlSchema,
    schemaUri: z.string().min(1),
    type: z.object({
      id: z.string(),
      schemaUri: z.string(),
    }),
    category: relatedObjectCategoryValidationSchema,
  })
);
