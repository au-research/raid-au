import { Alert, AlertTitle, Box, Typography } from "@mui/material";
import type { ValidationError } from "../hooks/useBulkUpload";

interface ValidationErrorDisplayProps {
  errors: ValidationError[];
}

export function ValidationErrorDisplay({
  errors,
}: ValidationErrorDisplayProps) {
  if (errors.length === 0) return null;

  // Group errors by row for cleaner display
  const errorsByRow = errors.reduce<Record<number, ValidationError[]>>(
    (acc, error) => {
      const key = error.row;
      if (!acc[key]) acc[key] = [];
      acc[key].push(error);
      return acc;
    },
    {}
  );

  // File-level errors (row === 0) are shown separately
  const fileErrors = errorsByRow[0] ?? [];
  const rowErrors = Object.entries(errorsByRow).filter(
    ([row]) => Number(row) !== 0
  );

  return (
    <Alert severity="error" sx={{ mt: 2 }}>
      <AlertTitle>
        Validation failed — please correct the file and re-upload
      </AlertTitle>

      {fileErrors.length > 0 && (
        <Box sx={{ mb: 1 }}>
          {fileErrors.map((err, i) => (
            <Typography key={i} variant="body2">
              {err.message}
            </Typography>
          ))}
        </Box>
      )}

      {rowErrors.length > 0 && (
        <Box
          component="ul"
          sx={{ m: 0, pl: 2, maxHeight: 200, overflow: "auto" }}
        >
          {rowErrors.map(([row, rowErrs]) => (
            <li key={row}>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                Row {row}:
              </Typography>
              <Box component="ul" sx={{ m: 0, pl: 2 }}>
                {rowErrs.map((err, i) => (
                  <li key={i}>
                    <Typography variant="body2" color="text.secondary">
                      <strong>{err.field}</strong>: {err.message}
                    </Typography>
                  </li>
                ))}
              </Box>
            </li>
          ))}
        </Box>
      )}
    </Alert>
  );
}
