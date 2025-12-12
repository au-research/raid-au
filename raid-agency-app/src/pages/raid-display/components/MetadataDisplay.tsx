import { DisplayItem } from "@/components/display-item";
import { Card, CardContent, Grid } from "@mui/material";
import { formatDate } from "@/utils/date-utils/date-utils";

export const MetadataDisplay = ({
  metadata,
}: {
  metadata: { created?: number; updated?: number };
}) => {
  return (
    <Card>
      <CardContent>
        <Grid container spacing={2}>
          <DisplayItem
            label="RAiD created on"
            value={
              metadata.created && metadata.created !== null
                ? formatDate(metadata.created, true)
                : "Unknown"
            }
            width={3}
          />
          <DisplayItem
            label="RAiD updated on"
            value={
              metadata.updated && metadata.updated !== null
                ? formatDate(metadata.updated, true)
                : "Unknown"
            }
          />
        </Grid>
      </CardContent>
    </Card>
  );
};
