import languageSchema from "@/references/language_schema.json";
import titleType from "@/references/title_type.json";
import titleTypeSchema from "@/references/title_type_schema.json";
import { combinedPattern } from "@/utils/date-utils/date-utils";
import { z } from "zod";

const dateStringSchema = z.string()
    .transform(val => val === '' ? undefined : val).pipe(
        z.string().regex(combinedPattern, {
            message: "YYYY or YYYY-MM or YYYY-MM-DD",
        })
    );

const titleTypeValidationSchema = z.object({
  id: z.enum(titleType.map((type) => type.uri) as [string, ...string[]]),
  schemaUri: z.literal(titleTypeSchema[0].uri),
});

const titleLanguageValidationSchema = z.object({
  id: z.string().optional(),
  schemaUri: z.literal(languageSchema[0].uri).optional(),
});

export const singleTitleValidationSchema = z
  .object({
    text: z.string().min(1),
    type: titleTypeValidationSchema,
    language: titleLanguageValidationSchema.optional(),
    startDate: dateStringSchema,
    endDate: dateStringSchema.optional(),
  })
  .refine(
    ({ startDate, endDate }) => {
      if (!endDate) return true;
      return new Date(startDate) <= new Date(endDate);
    },
    {
      message: "Start date must be before or equal to end date",
      path: ["endDate"],
    }
  );

export const titleValidationSchema = z
  .array(singleTitleValidationSchema)
  .min(1);
