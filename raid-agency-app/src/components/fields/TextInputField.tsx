import { getErrorMessageForField } from "@/utils/data-utils";
import { Grid, TextField } from "@mui/material";
import { memo } from "react";
import { useController } from "react-hook-form";

interface TextInputFieldProps {
  name: string;
  label: string;
  placeholder?: string;
  helperText?: string;
  errorText?: string;
  required?: boolean;
  multiline?: boolean;
  width?: number;
  disabled?: boolean;
}

const TextInputField = memo(function TextInputField({
  name,
  label,
  helperText,
  errorText,
  required = false,
  multiline,
  width = 12,
  disabled,
}: TextInputFieldProps) {
  const {
    field,
    formState: { errors },
  } = useController({ name });

  const errorMessage = getErrorMessageForField(errors, field.name);

  const hasError = Boolean(errorMessage) || Boolean(errorText);

  const getDisplayHelperText = () => {
    if (errorText) return errorText;
    if (errorMessage) return errorMessage.message;

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
        size="small"
        error={hasError}
        fullWidth
        helperText={getDisplayHelperText()}
        label={`${label} ${multiline ? "(supports markdown syntax)" : ""}`}
        placeholder={`${label} ${
          multiline ? "(supports markdown syntax)" : ""
        }`}
        required={Boolean(required)}
        variant="filled"
        multiline={multiline}
        disabled={disabled}
        sx={{
          boxShadow: 0,
        }}
      />
    </Grid>
  );
});

TextInputField.displayName = "TextInputField";

export { TextInputField };
