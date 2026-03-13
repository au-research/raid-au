import { organisationRoleValidationSchema } from "@/entities/organisation-role/validation-schema/organisation-role-validation-schema";
import organisationRoles from "@/references/organisation_role.json";
import organisationRoleSchema from "@/references/organisation_role_schema.json";
import organisationSchemaReference from "@/references/organisation_schema.json";
import { z } from "zod";

// Ensure the arrays are not empty
if (
  organisationRoles.length === 0 ||
  organisationRoleSchema.length === 0 ||
  organisationSchemaReference.length === 0
) {
  throw new Error("One or more reference arrays are empty");
}

export const organisationValidationSchema = z
  .array(
    z.object({
      id: z.string().min(1, {
        message: "Organisation ID is required",
      }),
      schemaUri: z.literal(organisationSchemaReference[0].uri),
      role: organisationRoleValidationSchema,
    })
  )
