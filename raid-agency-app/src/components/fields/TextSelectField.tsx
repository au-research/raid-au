import { getErrorMessageForField } from "@/utils/data-utils";
import { Grid, MenuItem, TextField } from "@mui/material";
import { useController } from "react-hook-form";

type Option = {
  value: string;
  label: string;
};

interface TextSelectFieldProps {
  options: Option[];
  name: string;
  label: string;
  placeholder?: string;
  helperText?: string;
  errorText?: string;
  required: boolean;
  multiline?: boolean;
  width?: number;
  id?: string;
}

export function TextSelectField({
  options,
  name,
  label,
  placeholder,
  helperText,
  errorText,
  required,
  width = 12,
}: TextSelectFieldProps) {
  const {
    field,
    formState: { errors },
  } = useController({ name });

  const errorMessage = getErrorMessageForField(errors, field.name);

  const getDisplayHelperText = () => {
    if (errorMessage) {
      return errorText || errorMessage.message;
    }

    if (required && helperText && helperText?.length > 0) {
      return `${helperText} *`;
    }

    return helperText || "";
  };

  return (
    <Grid item xs={width}>
      <TextField
        {...field}
        id={field.name || `field-${Date.now()}`}
        error={Boolean(errorMessage)}
        fullWidth
        helperText={getDisplayHelperText()}
        label={label}
        placeholder={placeholder || label}
        required={required}
        select
        size="small"
        variant="filled"
      >
        {options.map(({ value, label }) => (
          <MenuItem key={value} value={value}>
            {label}
          </MenuItem>
        ))}
      </TextField>
    </Grid>
  );
}
