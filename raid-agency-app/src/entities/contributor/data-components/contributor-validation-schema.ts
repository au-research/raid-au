import packageJson from "@/../package.json";
import { contributorPositionValidationSchema } from "@/entities/contributor/position/data-components/contributor-position-validation-schema";
import { contributorRoleValidationSchema } from "@/entities/contributor/role/data-components/contributor-role-validation-schema";
import { z } from "zod";

let baseContributorSchema = z.object({});

if (packageJson.apiVersion === "2") {
  baseContributorSchema = z.object({
    contact: z.boolean(),
    leader: z.boolean(),
    position: contributorPositionValidationSchema,
    role: contributorRoleValidationSchema,
    schemaUri: z.literal("https://orcid.org/"),
    status: z.string().optional(),
    uuid: z.string().optional(),
  });
}

if (packageJson.apiVersion === "3") {
  baseContributorSchema = z.object({
    contact: z.boolean(),
    email: z.string().optional(),
    id: z.string().optional(),
    leader: z.boolean(),
    position: contributorPositionValidationSchema,
    role: contributorRoleValidationSchema,
    schemaUri: z.literal("https://orcid.org/"),
    status: z.string().optional(),
    uuid: z.string().optional(),
  });
}

export const singleContributorValidationSchema = z.union([
  baseContributorSchema
    .extend({
      id: z
        .string()
        .regex(
          new RegExp("^https://orcid.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]$"),
          {
            message:
              "Invalid ORCID ID, must be full url, e.g. https://orcid.org/0000-0000-0000-0000",
          }
        ),
    })
    .strict(),
  baseContributorSchema
    .extend({
      uuid: z.string(),
    })
    .strict(),
  baseContributorSchema
    .extend({
      email: z.string().optional(),
    })
    .strict(),
]);

export const contributorValidationSchema = z
  .array(singleContributorValidationSchema)
  .min(1);
