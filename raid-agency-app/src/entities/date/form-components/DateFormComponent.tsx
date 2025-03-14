import { TextInputField } from "@/fields/TextInputField";
import { RaidDto } from "@/generated/raid";
import { Card, CardContent, CardHeader, Grid } from "@mui/material";
import { memo } from "react";
import { Control, FieldErrors, UseFormTrigger } from "react-hook-form";

const DateFormComponent = memo(
  ({
    errors,
  }: {
    control: Control<RaidDto>;
    errors: FieldErrors<RaidDto>;
    trigger: UseFormTrigger<RaidDto>;
  }) => {
    const key = "date";
    const labelPlural = "Dates";

    return (
      <Card
        sx={{
          borderLeft: errors[key] ? "3px solid" : "none",
          borderLeftColor: "error.main",
        }}
      >
        <CardHeader title={labelPlural} />
        <CardContent>
          <Grid container spacing={2}>
            <TextInputField
              name="date.startDate"
              label="Start Date"
              required={true}
              width={3}
            />
            <TextInputField
              name="date.endDate"
              label="End Date"
              required={false}
              width={3}
            />
          </Grid>
        </CardContent>
      </Card>
    );
  }
);

DateFormComponent.displayName = "DateFormComponent";
export { DateFormComponent };
