import { getErrorMessageForField } from "@/utils/data-utils";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import {
  Box,
  Grid,
  IconButton,
  InputAdornment,
  Popover,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import { DateCalendar, LocalizationProvider } from "@mui/x-date-pickers";
import { AdapterDayjs } from "@mui/x-date-pickers/AdapterDayjs";
import dayjs, { Dayjs } from "dayjs";
import { memo, useState } from "react";
import { useController } from "react-hook-form";

type DatePrecision = "year" | "month" | "day";

interface DatePickerFieldProps {
  name: string;
  label: string;
  required?: boolean;
  width?: number;
  disabled?: boolean;
  /** Show the Year / Month / Day precision selector in the picker. Defaults to true. */
  showPrecisionSelector?: boolean;
}

// Views shown in the calendar for each precision level
const PRECISION_VIEWS: Record<
  DatePrecision,
  readonly ("year" | "month" | "day")[]
> = {
  year: ["year"],
  month: ["year", "month"],
  day: ["year", "month", "day"],
};

// ISO output format for each precision level
const PRECISION_FORMAT: Record<DatePrecision, string> = {
  year: "YYYY",
  month: "YYYY-MM",
  day: "YYYY-MM-DD",
};

// Infer precision from an existing field value so the popover opens at the right level
function derivePrecision(value: unknown): DatePrecision {
  if (typeof value !== "string") return "day";
  if (/^\d{4}$/.test(value)) return "year";
  if (/^\d{4}-\d{2}$/.test(value)) return "month";
  return "day";
}

// Shared sx applied to every ToggleButton so the selected state uses the brand colour
const toggleButtonSx = {
  flexDirection: "column",
  py: 0.75,
  gap: 0.25,
  "&.Mui-selected": {
    bgcolor: "primary.main",
    color: "primary.contrastText",
    "&:hover": { bgcolor: "primary.dark" },
    // Keep the format hint readable against the coloured background
    "& .MuiTypography-root": { opacity: 1 },
  },
} as const;

const DatePickerField = memo(function DatePickerField({
  name,
  label,
  required = false,
  width = 12,
  disabled,
  showPrecisionSelector = true,
}: DatePickerFieldProps) {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);
  const [precision, setPrecision] = useState<DatePrecision>("day");

  const {
    field,
    formState: { errors },
  } = useController({ name });

  const errorMessage = getErrorMessageForField(errors, field.name);
  const hasError = Boolean(errorMessage);
  const open = Boolean(anchorEl);

  const handleCalendarOpen = (e: React.MouseEvent<HTMLButtonElement>) => {
    setPrecision(derivePrecision(field.value));
    setAnchorEl(e.currentTarget);
  };

  const handleCalendarClose = () => setAnchorEl(null);

  // Expand partial dates so dayjs can parse and highlight them in the calendar
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
          <Box sx={{ width: 320 }}>
            {showPrecisionSelector && (
              <Box sx={{ px: 1.5, pt: 1.5, pb: 0.5 }}>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block", mb: 0.75, fontWeight: 500 }}
                >
                  Select date precision
                </Typography>
                <ToggleButtonGroup
                  value={precision}
                  exclusive
                  fullWidth
                  size="small"
                  onChange={(_, next: DatePrecision | null) => {
                    if (next) setPrecision(next);
                  }}
                  sx={{
                    boxShadow: "0 2px 8px rgba(0,0,0,0.12)",
                    borderRadius: 1,
                  }}
                >
                  <ToggleButton value="year" sx={toggleButtonSx}>
                    <Typography
                      variant="caption"
                      fontWeight={600}
                      lineHeight={1}
                    >
                      Year
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{ fontSize: "0.62rem", opacity: 0.55, lineHeight: 1 }}
                    >
                      YYYY
                    </Typography>
                  </ToggleButton>
                  <ToggleButton value="month" sx={toggleButtonSx}>
                    <Typography
                      variant="caption"
                      fontWeight={600}
                      lineHeight={1}
                    >
                      Month
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{ fontSize: "0.62rem", opacity: 0.55, lineHeight: 1 }}
                    >
                      YYYY-MM
                    </Typography>
                  </ToggleButton>
                  <ToggleButton value="day" sx={toggleButtonSx}>
                    <Typography
                      variant="caption"
                      fontWeight={600}
                      lineHeight={1}
                    >
                      Day
                    </Typography>
                    <Typography
                      variant="caption"
                      sx={{ fontSize: "0.62rem", opacity: 0.55, lineHeight: 1 }}
                    >
                      YYYY-MM-DD
                    </Typography>
                  </ToggleButton>
                </ToggleButtonGroup>
              </Box>
            )}
            {/* key forces calendar to reinitialise when precision changes, resetting the active view */}
            <DateCalendar
              key={precision}
              value={calendarValue}
              views={PRECISION_VIEWS[precision]}
              openTo={precision}
              onChange={(date: Dayjs | null, selectionState) => {
                if (date?.isValid() && selectionState === "finish") {
                  field.onChange(date.format(PRECISION_FORMAT[precision]));
                  handleCalendarClose();
                }
              }}
            />
          </Box>
        </Popover>
      </LocalizationProvider>
    </Grid>
  );
});

DatePickerField.displayName = "DatePickerField";
export { DatePickerField };
