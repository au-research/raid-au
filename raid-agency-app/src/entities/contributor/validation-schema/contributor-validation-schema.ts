/**
 * Validation schema for RAID contributors
 *
 * This module defines validation rules for contributors in the RAID system.
 * Contributors are validated with specific rules for ORCID identifiers,
 * position, role, and contact information.
 *
 * The validation supports two contributor formats:
 * 1. Contributors with ORCID identifiers
 * 2. Contributors with UUIDs (typically system-generated)
 *
 * The schema ensures that at least one contributor exists in the RAID.
 */
import { contributorPositionValidationSchema } from "@/entities/contributor-position/validation-schema/contributor-position-validation-schema";
import { contributorRoleValidationSchema } from "@/entities/contributor-role/validation-schema/contributor-role-validation-schema";
import { getContributorSchemaUri } from "@/utils/contributor-utils/contributor-schema-uri";
import { ISNI_SCHEMA_URI } from "@/utils/contributor-utils/contributor-identifier";
import { z } from "zod";

// The ORCID or ISNI regex pattern used in multiple places (RAID-861 widened
// this from ORCID-only to also accept an ISNI URL). Exported for direct unit
// testing without needing to satisfy the full contributor schema's
// position/role requirements.
export const orcidPattern =
  "^(?:(?:https://(sandbox\\.)?orcid\\.org/)\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]|https://isni\\.org/\\d{15}[0-9X])$";
const orcidErrorMsg =
  "Invalid ORCID ID, must be full url, e.g. https://orcid.org/0000-0000-0000-0000, or a valid ISNI url, e.g. https://isni.org/0000000121032683";

// Base schema for contributors
const baseContributorSchema = z.object({
  contact: z.boolean(),
  id: z.string().optional(),
  leader: z.boolean(),
  position: contributorPositionValidationSchema,
  role: contributorRoleValidationSchema,
  schemaUri: z.string().refine((v) => v === getContributorSchemaUri() || v === ISNI_SCHEMA_URI, {
    message:
      "Invalid contributor schemaUri, expected the environment-appropriate ORCID URL or https://isni.org/",
  }),
  status: z.string().optional(),
  uuid: z.string().optional(),
});

// Single contributor validation with two potential formats
export const singleContributorValidationSchema = z.union([
  baseContributorSchema.extend({
    id: z
      .string()
      .trim()
      .regex(new RegExp(orcidPattern), { message: orcidErrorMsg })
      .optional()
  }),
  baseContributorSchema.extend({
    uuid: z.string(),
  }),
]);

// Array of contributors with at least one element
export const contributorValidationSchema = z
  .array(singleContributorValidationSchema)
  .min(1)
  .superRefine((contributors, ctx) => {
    contributors.forEach((contributor, index) => {
      // Check if ORCID field exists and is empty
      if ('id' in contributor && contributor.id !== undefined) {
        const trimmedId = contributor.id.trim();
       if (!new RegExp(orcidPattern).test(trimmedId)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: `#${index + 1} ${orcidErrorMsg}`,
            path: [index, 'id'],
          });
        }
      }
    });
  });
