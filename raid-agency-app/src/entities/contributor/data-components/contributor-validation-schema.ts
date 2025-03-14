import { contributorPositionValidationSchema } from "@/entities/contributor-position/data-components/contributor-position-validation-schema";
import { contributorRoleValidationSchema } from "@/entities/contributor-role/data-components/contributor-role-validation-schema";
import { z } from "zod";

// The ORCID regex pattern used in multiple places
const orcidPattern =
  "^https://(sandbox\\.)?orcid\\.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]$";
const orcidErrorMsg =
  "Invalid ORCID ID, must be full url, e.g. https://orcid.org/0000-0000-0000-0000";

// Base schema for contributors
const baseContributorSchema = z.object({
  contact: z.boolean(),
  email: z.string().optional(),
  id: z.string().regex(new RegExp(orcidPattern), { message: orcidErrorMsg }),
  leader: z.boolean(),
  position: contributorPositionValidationSchema,
  role: contributorRoleValidationSchema,
  schemaUri: z.literal("https://orcid.org/"),
  status: z.string().optional(),
  uuid: z.string().optional(),
});

// Single contributor validation with three potential formats
export const singleContributorValidationSchema = z.union([
  baseContributorSchema.extend({
    id: z
      .string()
      .regex(
        new RegExp("^https://orcid.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]$"),
        { message: orcidErrorMsg }
      ),
  }),
  baseContributorSchema.extend({
    uuid: z.string(),
  }),
  baseContributorSchema.extend({
    email: z.string().optional(),
  }),
]);

// Array of contributors with at least one element
export const contributorValidationSchema = z
  .array(singleContributorValidationSchema)
  .min(1);
