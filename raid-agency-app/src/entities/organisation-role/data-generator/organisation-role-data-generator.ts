import { OrganisationRole } from "@/generated/raid";
import organisationRole from "@/references/organisation_role.json";
import organisationRoleSchema from "@/references/organisation_role_schema.json";
import dayjs from "dayjs";

export const organisationRoleDataGenerator = (
  startDate?: string,
  endDate?: string
): OrganisationRole => {
  return {
    id: organisationRole[0].uri,
    schemaUri: organisationRoleSchema[0].uri,
    startDate: startDate ?? dayjs().format("YYYY-MM-DD"),
    endDate: endDate ?? dayjs().format("YYYY-MM-DD"),
  };
};
