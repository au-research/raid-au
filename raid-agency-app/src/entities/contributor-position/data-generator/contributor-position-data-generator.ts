import contributorPosition from "@/references/contributor_position.json";
import contributorPositionSchema from "@/references/contributor_position_schema.json";
import dayjs from "dayjs";

import { ContributorPosition } from "@/generated/raid";

export const contributorPositionDataGenerator = (
  startDate?: string,
  endDate?: string
): ContributorPosition => {
  return {
    schemaUri: contributorPositionSchema[0].uri,
    id: contributorPosition[0].uri,
    startDate: startDate ?? dayjs().format("YYYY-MM-DD"),
    endDate: endDate ?? "",
  };
};
