import { DisplayCard } from "@/components/display-card";
import { DisplayItem } from "@/components/display-item";
import { Access } from "@/generated/raid";
import { useMapping } from "@/mapping";
import { dateDisplayFormatter } from "@/utils/date-utils/date-utils";
import { Grid } from "@mui/material";
import dayjs from "dayjs";
import { memo } from "react";

const AccessView = memo(({ data }: { data: Access }) => {
  const { generalMap, languageMap } = useMapping();
  const hasEmbargoedAccess = data.type.id.includes("c_f1cf");
  const accessTypeMappedValue = generalMap.get(String(data.type?.id)) ?? "";
  const languageMappedValue =
    languageMap.get(String(data?.statement?.language?.id)) ?? "";

  return (
    <DisplayCard data={data} labelPlural="Access">
      <Grid container spacing={2}>
        <DisplayItem
          label="Access Type"
          value={accessTypeMappedValue}
          width={6}
        />
        {hasEmbargoedAccess && (
          <>
            <DisplayItem
              label="Statement"
              value={data?.statement?.text}
              width={12}
            />
            <DisplayItem
              label="Language"
              value={languageMappedValue}
              width={6}
            />
            <DisplayItem
              label="Embargo Expiry Date"
              value={dateDisplayFormatter(
                dayjs(data?.embargoExpiry).format("YYYY-MM-DD")
              )}
              width={6}
            />
          </>
        )}
      </Grid>
    </DisplayCard>
  );
});

AccessView.displayName = "AccessView";
export { AccessView };
