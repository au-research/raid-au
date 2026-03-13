import { TextInputField } from "@/components/fields/TextInputField";
import { RaidDto } from "@/generated/raid";
import { Card, CardContent, CardHeader, Grid, Stack } from "@mui/material";
import { memo, useContext } from "react";
import { Control, FieldErrors, UseFormTrigger } from "react-hook-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

const DateForm = memo(
  ({
    errors,
  }: {
    control: Control<RaidDto>;
    errors: FieldErrors<RaidDto>;
    trigger: UseFormTrigger<RaidDto>;
  }) => {
    const key = "date";
    const label = "Date";
    const labelPlural = "Dates";
    const metadata = useContext(MetadataContext);
    const tooltip = metadata?.[key]?.tooltip;
    return (
      <Card
        sx={{
          borderLeft: errors[key] ? "3px solid" : "none",
          borderLeftColor: "error.main",
        }}
        id={key}
      >
        <Stack direction="row" alignItems="center">
          <CardHeader sx={{padding: "16px 0 16px 16px"}} title={labelPlural} />
          <CustomStyledTooltip
            title={label}
            content={tooltip || ""}
            variant="info"
            placement="top"
            tooltipIcon={<InfoOutlinedIcon />}
          >
          </CustomStyledTooltip>
        </Stack>
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

DateForm.displayName = "DateForm";
export { DateForm };
