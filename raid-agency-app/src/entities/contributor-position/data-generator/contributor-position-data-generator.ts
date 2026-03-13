import contributorPosition from "@/references/contributor_position.json";
import contributorPositionSchema from "@/references/contributor_position_schema.json";
import dayjs from "dayjs";

import { ContributorPosition } from "@/generated/raid";

export const contributorPositionDataGenerator = (): ContributorPosition => {
  return {
    schemaUri: contributorPositionSchema[0].uri,
    id: contributorPosition[0].uri,
    startDate: dayjs().format("YYYY-MM-DD"),
    endDate: "",
  };
};
