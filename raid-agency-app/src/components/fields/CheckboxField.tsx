import { Checkbox, FormControlLabel, FormGroup, Grid } from "@mui/material";
import { useController } from "react-hook-form";

export function CheckboxField({
  name,
  label,
  required,
  width = 12,
}: {
  name: string;
  label: string;
  required?: boolean;
  width?: number;
}) {
  const { field } = useController({ name });
  return (
    <Grid item xs={width}>
      <FormGroup>
        <FormControlLabel
          control={<Checkbox {...field} checked={Boolean(field.value)} />}
          label={label}
          required={Boolean(required)}
        />
      </FormGroup>
    </Grid>
  );
}
