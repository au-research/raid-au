import accessType from "@/references/access_type.json";
import accessTypeSchema from "@/references/access_type_schema.json";
import languageSchema from "@/references/language_schema.json";
import { z } from "zod";

export const yearMonthDayPattern =
  /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$/;

const yearMonthDayPatternSchema = z.string().regex(yearMonthDayPattern, {
  message: "YYYY-MM-DD",
});

const accessTypeValidationSchema = z.object({
  id: z.enum(accessType.map((type) => type.uri) as [string, ...string[]]),
  schemaUri: z.enum(
    accessTypeSchema.map((el) => el.uri) as [string, ...string[]]
  ),
});

const accessStatementLanguageValidationSchema = z.object({
  id: z.string().min(1),
  schemaUri: z.enum(
    languageSchema.map((el) => el.uri) as [string, ...string[]]
  ),
});

const accessStatementValidationSchema = z.object({
  text: z.string().optional(),
  language: accessStatementLanguageValidationSchema,
}).optional();

export const accessValidationSchema = z.object({
  type: accessTypeValidationSchema,
  statement: accessStatementValidationSchema,
  embargoExpiry: yearMonthDayPatternSchema.nullish(),
});
