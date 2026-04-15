import {
  Autocomplete,
  Box,
  Chip,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import {
  CheckCircleOutline as ValidIcon,
  ErrorOutline as ErrorIcon,
  DeleteOutline as DeleteIcon,
} from "@mui/icons-material";
import { useState, useEffect } from "react";

import type {
  EditableRow,
  EditableRowField,
} from "../hooks/useBulkUpload";
import type { BulkUploadVocabulary } from "../vocabulary/types";

interface BulkUploadPreviewTableProps {
  rows: EditableRow[];
  vocabulary: BulkUploadVocabulary;
  onUpdateRow: (
    rowIndex: number,
    field: EditableRowField,
    value: string
  ) => void;
  onRemoveRow: (rowIndex: number) => void;
  disabled?: boolean;
}

export function BulkUploadPreviewTable({
  rows,
  vocabulary,
  onUpdateRow,
  onRemoveRow,
  disabled = false,
}: BulkUploadPreviewTableProps) {
  if (rows.length === 0) return null;

  const errorRowCount = rows.filter(
    (r) => Object.keys(r.errors).length > 0
  ).length;

  const typeOptions = vocabulary.relatedObjectTypes.map((t) => t.value);
  const categoryOptions = vocabulary.relatedObjectCategories.map((c) => c.value);

  return (
    <Box>
      {/* ---- Header summary ---- */}
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ mb: 1 }}
      >
        <Typography variant="subtitle2">
          Preview ({rows.length} {rows.length === 1 ? "row" : "rows"})
        </Typography>
        {errorRowCount > 0 ? (
          <Chip
            size="small"
            color="error"
            icon={<ErrorIcon />}
            label={`${errorRowCount} ${errorRowCount === 1 ? "row has" : "rows have"} errors`}
          />
        ) : (
          <Chip
            size="small"
            color="success"
            icon={<ValidIcon />}
            label="All rows valid"
          />
        )}
      </Stack>

      {/* ---- Table ---- */}
      <TableContainer
        component={Paper}
        variant="outlined"
        sx={{ maxHeight: 480 }}
      >
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={{ width: 40 }} />
              <TableCell sx={{ minWidth: 280 }}>DOI URL</TableCell>
              <TableCell sx={{ minWidth: 220 }}>Type</TableCell>
              <TableCell sx={{ minWidth: 220 }}>Categories</TableCell>
              <TableCell sx={{ width: 60 }} align="center">
                Remove
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row, idx) => (
              <PreviewRow
                key={row.id}
                row={row}
                rowIndex={idx}
                typeOptions={typeOptions}
                categoryOptions={categoryOptions}
                onUpdate={onUpdateRow}
                onRemove={onRemoveRow}
                disabled={disabled}
              />
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

// ------------------------------------------------------------------
// Single row
// ------------------------------------------------------------------

interface PreviewRowProps {
  row: EditableRow;
  rowIndex: number;
  typeOptions: string[];
  categoryOptions: string[];
  onUpdate: (rowIndex: number, field: EditableRowField, value: string) => void;
  onRemove: (rowIndex: number) => void;
  disabled: boolean;
}

function PreviewRow({
  row,
  rowIndex,
  typeOptions,
  categoryOptions,
  onUpdate,
  onRemove,
  disabled,
}: PreviewRowProps) {
  const hasErrors = Object.keys(row.errors).length > 0;

  return (
    <TableRow
      hover
      sx={{
        backgroundColor: hasErrors
          ? "rgba(229, 75, 75, 0.04)"
          : "inherit",
      }}
    >
      {/* Status indicator */}
      <TableCell>
        {hasErrors ? (
          <Tooltip title="This row has validation errors">
            <ErrorIcon color="error" fontSize="small" />
          </Tooltip>
        ) : (
          <Tooltip title="Valid">
            <ValidIcon color="success" fontSize="small" />
          </Tooltip>
        )}
      </TableCell>

      {/* DOI URL — free text with debounced commit on blur */}
      <TableCell>
        <DebouncedTextCell
          value={row.values["DOI URL"]}
          onCommit={(value) => onUpdate(rowIndex, "DOI URL", value)}
          error={row.errors["DOI URL"]}
          disabled={disabled}
          placeholder="https://doi.org/10.xxxx/..."
        />
      </TableCell>

      {/* Type — single-select Autocomplete */}
      <TableCell>
        <SingleValueCell
          value={row.values.Type}
          onCommit={(value) => onUpdate(rowIndex, "Type", value)}
          error={row.errors.Type}
          options={typeOptions}
          disabled={disabled}
          placeholder="Select type..."
        />
      </TableCell>

      {/* Categories — multi-select Autocomplete */}
      <TableCell>
        <MultiValueCell
          value={row.values.Categories}
          onCommit={(value) => onUpdate(rowIndex, "Categories", value)}
          error={row.errors.Categories}
          options={categoryOptions}
          disabled={disabled}
          placeholder="Select categor(ies)..."
        />
      </TableCell>

      {/* Remove button */}
      <TableCell align="center">
        <Tooltip title="Remove row">
          <span>
            <IconButton
              size="small"
              onClick={() => onRemove(rowIndex)}
              disabled={disabled}
              aria-label="Remove row"
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
      </TableCell>
    </TableRow>
  );
}

// ------------------------------------------------------------------
// DebouncedTextCell — plain text input that commits on blur
// ------------------------------------------------------------------

interface DebouncedTextCellProps {
  value: string;
  onCommit: (value: string) => void;
  error?: string;
  disabled?: boolean;
  placeholder?: string;
}

function DebouncedTextCell({
  value,
  onCommit,
  error,
  disabled,
  placeholder,
}: DebouncedTextCellProps) {
  const [localValue, setLocalValue] = useState(value);

  // Sync from parent if it changes externally
  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  const commit = () => {
    if (localValue !== value) {
      onCommit(localValue);
    }
  };

  return (
    <Tooltip
      title={error ?? ""}
      placement="top"
      arrow
      disableHoverListener={!error}
    >
      <TextField
        value={localValue}
        onChange={(e) => setLocalValue(e.target.value)}
        onBlur={commit}
        error={!!error}
        disabled={disabled}
        placeholder={placeholder}
        size="small"
        fullWidth
        variant="outlined"
      />
    </Tooltip>
  );
}

// ------------------------------------------------------------------
// MultiValueCell — Autocomplete with chips for multi-select with freeSolo
// ------------------------------------------------------------------

interface MultiValueCellProps {
  value: string; // comma-separated string from the underlying state
  onCommit: (value: string) => void;
  error?: string;
  options: string[];
  disabled?: boolean;
  placeholder?: string;
}

/**
 * Splits a comma-separated string into an array of trimmed values.
 * Handles "Input, Output" and "Input,Output" the same way.
 */
function stringToArray(value: string): string[] {
  if (!value) return [];
  return value
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}

/**
 * Joins an array back into a comma-separated string for storage.
 */
function arrayToString(values: string[]): string {
  return values.join(", ");
}

function MultiValueCell({
  value,
  onCommit,
  error,
  options,
  disabled,
  placeholder,
}: MultiValueCellProps) {
  const arrayValue = stringToArray(value);

  return (
    <Tooltip
      title={error ?? ""}
      placement="top"
      arrow
      disableHoverListener={!error}
    >
      <Autocomplete
        multiple
        freeSolo
        size="small"
        options={options}
        value={arrayValue}
        disabled={disabled}
        onChange={(_, newValue) => {
          // newValue is string[] — convert each item to string in case
          // freeSolo passes a custom typed entry as an object
          const cleaned = newValue
            .map((v) => (typeof v === "string" ? v.trim() : String(v).trim()))
            .filter((v) => v.length > 0);
          onCommit(arrayToString(cleaned));
        }}
        renderTags={(tagValue, getTagProps) =>
          tagValue.map((option, index) => {
            const { key, ...chipProps } = getTagProps({ index });
            return (
              <Chip
                key={key}
                size="small"
                label={option}
                {...chipProps}
              />
            );
          })
        }
        renderInput={(params) => (
          <TextField
            {...params}
            error={!!error}
            placeholder={arrayValue.length === 0 ? placeholder : ""}
            variant="outlined"
            size="small"
          />
        )}
        sx={{
          "& .MuiOutlinedInput-root": {
            minHeight: 40,
          },
        }}
      />
    </Tooltip>
  );
}

// ------------------------------------------------------------------
// SingleValueCell — Autocomplete single-select for the Type column
// ------------------------------------------------------------------

interface SingleValueCellProps {
  value: string;
  onCommit: (value: string) => void;
  error?: string;
  options: string[];
  disabled?: boolean;
  placeholder?: string;
}

function SingleValueCell({
  value,
  onCommit,
  error,
  options,
  disabled,
  placeholder,
}: SingleValueCellProps) {
  return (
    <Tooltip
      title={error ?? ""}
      placement="top"
      arrow
      disableHoverListener={!error}
    >
      <Autocomplete
        size="small"
        options={options}
        value={value || null}
        disabled={disabled}
        onChange={(_, newValue) => {
          onCommit(typeof newValue === "string" ? newValue.trim() : "");
        }}
        renderInput={(params) => (
          <TextField
            {...params}
            error={!!error}
            placeholder={placeholder}
            variant="outlined"
            size="small"
          />
        )}
        sx={{
          "& .MuiOutlinedInput-root": {
            minHeight: 40,
          },
        }}
      />
    </Tooltip>
  );
}
