import { getErrorMessageForField } from "@/utils/data-utils";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import { Grid, IconButton, InputAdornment, Popover, TextField } from "@mui/material";
import { DateCalendar, LocalizationProvider } from "@mui/x-date-pickers";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import dayjs, { Dayjs } from "dayjs";
import { memo, useState } from "react";
import { useController } from "react-hook-form";

interface DatePickerFieldProps {
  name: string;
  label: string;
  required?: boolean;
  width?: number;
  disabled?: boolean;
}

const DatePickerField = memo(function DatePickerField({
  name,
  label,
  required = false,
  width = 12,
  disabled,
}: DatePickerFieldProps) {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);

  const {
    field,
    formState: { errors },
  } = useController({ name });

  const errorMessage = getErrorMessageForField(errors, field.name);
  const hasError = Boolean(errorMessage);
  const open = Boolean(anchorEl);

  const handleCalendarOpen = (e: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(e.currentTarget);
  };

  const handleCalendarClose = () => {
    setAnchorEl(null);
  };

  // Parse the current field value for the calendar display.
  // Partial dates (YYYY or YYYY-MM) are expanded so dayjs can parse them.
  const calendarValue: Dayjs | null = (() => {
    if (!field.value || typeof field.value !== "string") return null;
    const v = field.value.trim();
    if (/^\d{4}$/.test(v)) return dayjs(`${v}-01-01`);
    if (/^\d{4}-\d{2}$/.test(v)) return dayjs(`${v}-01`);
    const parsed = dayjs(v);
    return parsed.isValid() ? parsed : null;
  })();

  return (
    <Grid item xs={width}>
      <LocalizationProvider
        dateAdapter={AdapterDayjs}
        adapterLocale={navigator.language}
      >
        <TextField
          {...field}
          id={field.name}
          size="small"
          error={hasError}
          fullWidth
          helperText={
            errorMessage ? errorMessage.message : "YYYY, YYYY-MM, or YYYY-MM-DD"
          }
          label={label}
          placeholder="YYYY-MM-DD"
          required={Boolean(required)}
          variant="filled"
          disabled={disabled}
          sx={{ boxShadow: 0 }}
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={handleCalendarOpen}
                  disabled={disabled}
                  size="small"
                  aria-label="open date picker"
                >
                  <CalendarTodayIcon fontSize="small" />
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
        <Popover
          open={open}
          anchorEl={anchorEl}
          onClose={handleCalendarClose}
          anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
          transformOrigin={{ vertical: "top", horizontal: "left" }}
        >
          <DateCalendar
            value={calendarValue}
            onChange={(date: Dayjs | null, selectionState) => {
              if (date?.isValid()) {
                field.onChange(date.format("YYYY-MM-DD"));
              }
              // Close only once a day has been fully selected (not while navigating year/month views)
              if (selectionState === "finish") {
                handleCalendarClose();
              }
            }}
          />
        </Popover>
      </LocalizationProvider>
    </Grid>
  );
});

DatePickerField.displayName = "DatePickerField";
export { DatePickerField };
