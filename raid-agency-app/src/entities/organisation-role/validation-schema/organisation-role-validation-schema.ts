import organisationRole from "@/references/organisation_role.json";
import organisationRoleSchema from "@/references/organisation_role_schema.json";
import { combinedPattern } from "@/utils/date-utils/date-utils";
import { z } from "zod";

export const organisationRoleValidationSchema = z.array(
    z.object({
            id: z.enum(
                organisationRole.map((role) => role.uri) as [string, ...string[]]
            ),
            schemaUri: z.literal(organisationRoleSchema[0].uri),

            startDate: z.string().transform(val => val === '' ? undefined : val)
                .pipe(z.string().regex(combinedPattern)),
            endDate: z.string()
                .optional()
                .transform(val => {
                        if (val === undefined) return undefined;
                        if (val === '') return undefined;
                        return val;
                })
                .pipe(
                    z.string().regex(combinedPattern).optional()
                )
    })
);
