import language from "@/references/language.json";
import { getErrorMessageForField } from "@/utils/data-utils";
import { Autocomplete, Grid, TextField } from "@mui/material";
import { useController } from "react-hook-form";

interface FormFieldProps {
  name: string;
  helperText?: string;
  errorText?: string;
  required?: boolean;
  width?: number;
}
export default function LanguageSelector({
  name,
  helperText,
  errorText,
  required = false,
  width = 12,
}: FormFieldProps) {
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

  const { onChange, ...restField } = field;
  return (
    <Grid item xs={width}>
      <Autocomplete
        {...restField}
        options={language}
        getOptionLabel={(option) => `${option.code}: ${option.name}`}
        value={
          language.find(
            (lang) => lang.code.toString() === restField.value?.toString()
          ) || null
        }
        onChange={(_, newValue) => {
          onChange(newValue ? newValue.code : "");
        }}
        isOptionEqualToValue={(option, value) => {
          return option.code === value.code;
        }}
        renderInput={(params) => {
          return (
            <TextField
              error={Boolean(errorMessage)}
              helperText={getDisplayHelperText()}
              {...params}
              variant="filled"
              size="small"
              label="Language"
              required={required}
            />
          );
        }}
      />
    </Grid>
  );
}
