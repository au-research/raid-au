import { DisplayItem } from "@/components/display-item";
import { useMapping } from "@/mapping";
import { OrganisationRole } from "@/generated/raid";
import { dateDisplayFormatter } from "@/utils/date-utils/date-utils";
import { Grid } from "@mui/material";
import { memo, useMemo } from "react";

const OrganisationRoleItem = memo(({ item }: { item: OrganisationRole }) => {
  const { generalMap } = useMapping();

  const itemPositionMappedValue = useMemo(
    () => generalMap.get(String(item?.id)) ?? "",
    [generalMap, item?.id]
  );

  return (
    <Grid container spacing={2}>
      <DisplayItem label="Role" value={itemPositionMappedValue} width={6} />
      <DisplayItem
        label="Start"
        value={dateDisplayFormatter(item.startDate)}
        width={3}
      />
      <DisplayItem
        label="End"
        value={dateDisplayFormatter(item.endDate)}
        width={3}
      />
    </Grid>
  );
});

OrganisationRoleItem.displayName = "OrganisationRoleItem";
export { OrganisationRoleItem };
