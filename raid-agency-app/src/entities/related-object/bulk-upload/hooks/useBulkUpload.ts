import { useState, useCallback } from "react";
import { z } from "zod";

/**
 * Bulk upload status state machine:
 *
 *   idle → parsing → validating → valid → submitting → done
 *                                ↘ invalid → (user re-uploads) → parsing ...
 *                                           ↗
 */
export type BulkUploadStatus =
  | "idle"
  | "parsing"
  | "validating"
  | "valid"
  | "invalid"
  | "submitting"
  | "done"
  | "error";

export interface ValidationError {
  row: number;
  field: string;
  message: string;
}

export interface ParsedRelatedObject {
  id: string;
  schemaUri: string;
  type: {
    id: string;
    schemaUri: string;
  };
  category: Array<{
    id: string;
    schemaUri: string;
  }>;
}

// ------------------------------------------------------------------
// Validation schema — reuses the same regex rules as the manual form
// ------------------------------------------------------------------

const doiRegex = /^https:\/\/doi\.org\/10\.\d{4,9}\/[^\s]+$/;
const webArchiveRegex =
  /^https:\/\/web\.archive\.org\/web\/\d{14}\/https:\/\/.*/;

const bulkRelatedObjectRowSchema = z.object({
  id: z
    .string()
    .trim()
    .url()
    .refine((url) => doiRegex.test(url) || webArchiveRegex.test(url), {
      message:
        "Must be a valid DOI (https://doi.org/10.xxxx/...) or Web Archive URL (https://web.archive.org/web/{14-digit-timestamp}/https://...)",
    }),
  schemaUri: z.string().min(1, "Schema URI is required"),
  type: z.object({
    id: z.string().min(1, "Type ID is required"),
    schemaUri: z.string().min(1, "Type schema URI is required"),
  }),
  category: z
    .array(
      z.object({
        id: z.string().min(1, "Category ID is required"),
        schemaUri: z.string().min(1, "Category schema URI is required"),
      })
    )
    .min(1, "At least one category is required"),
});

// ------------------------------------------------------------------
// File parsing utilities
// ------------------------------------------------------------------

/**
 * Parses a CSV string into an array of row objects.
 * Assumes the first row is a header row.
 */
function parseCsvToRows(csvText: string): Record<string, string>[] {
  const lines = csvText
    .split(/\r?\n/)
    .filter((line) => line.trim().length > 0);

  if (lines.length < 2) return [];

  const headers = lines[0].split(",").map((h) => h.trim());

  return lines.slice(1).map((line) => {
    const values = line.split(",").map((v) => v.trim());
    const row: Record<string, string> = {};
    headers.forEach((header, i) => {
      row[header] = values[i] ?? "";
    });
    return row;
  });
}

/**
 * Maps a flat row from the spreadsheet/CSV into the nested shape
 * expected by the Zod schema and the API.
 *
 * Template columns:
 *   id | schemaUri | type.id | type.schemaUri | category.id | category.schemaUri
 */
function mapRowToRelatedObject(
  row: Record<string, string>
): ParsedRelatedObject {
  return {
    id: row["id"] ?? "",
    schemaUri: row["schemaUri"] ?? "",
    type: {
      id: row["type.id"] ?? "",
      schemaUri: row["type.schemaUri"] ?? "",
    },
    category: [
      {
        id: row["category.id"] ?? "",
        schemaUri: row["category.schemaUri"] ?? "",
      },
    ],
  };
}

// ------------------------------------------------------------------
// Hook
// ------------------------------------------------------------------

export function useBulkUpload() {
  const [status, setStatus] = useState<BulkUploadStatus>("idle");
  const [file, setFile] = useState<File | null>(null);
  const [parsedRows, setParsedRows] = useState<ParsedRelatedObject[]>([]);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [submissionError, setSubmissionError] = useState<string | null>(null);

  // ---- Reset everything back to idle ----
  const reset = useCallback(() => {
    setStatus("idle");
    setFile(null);
    setParsedRows([]);
    setErrors([]);
    setSubmissionError(null);
  }, []);

  // ---- Parse uploaded file ----
  const parseFile = useCallback(async (file: File): Promise<Record<string, string>[]> => {
    const extension = file.name.split(".").pop()?.toLowerCase();

    if (extension === "csv") {
      const text = await file.text();
      return parseCsvToRows(text);
    }

    if (extension === "xlsx" || extension === "xls") {
      // Dynamic import to keep bundle size down — xlsx is large
      const XLSX = await import("xlsx");
      const buffer = await file.arrayBuffer();
      const workbook = XLSX.read(buffer, { type: "array" });
      const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
      return XLSX.utils.sheet_to_json<Record<string, string>>(firstSheet, {
        defval: "",
      });
    }

    throw new Error(`Unsupported file type: .${extension}`);
  }, []);

  // ---- Validate parsed rows ----
  const validateRows = useCallback(
    (rows: ParsedRelatedObject[]): ValidationError[] => {
      const validationErrors: ValidationError[] = [];

      rows.forEach((row, index) => {
        const result = bulkRelatedObjectRowSchema.safeParse(row);
        if (!result.success) {
          result.error.errors.forEach((zodError) => {
            validationErrors.push({
              row: index + 1, // 1-indexed for user display
              field: zodError.path.join("."),
              message: zodError.message,
            });
          });
        }
      });

      return validationErrors;
    },
    []
  );

  // ---- Main handler: upload → parse → validate ----
  const handleFileUpload = useCallback(
    async (uploadedFile: File) => {
      setFile(uploadedFile);
      setErrors([]);
      setSubmissionError(null);

      // Parse
      setStatus("parsing");
      let rawRows: Record<string, string>[];
      try {
        rawRows = await parseFile(uploadedFile);
      } catch (err) {
        setErrors([
          {
            row: 0,
            field: "file",
            message:
              err instanceof Error
                ? err.message
                : "Failed to parse the uploaded file.",
          },
        ]);
        setStatus("invalid");
        return;
      }

      if (rawRows.length === 0) {
        setErrors([
          { row: 0, field: "file", message: "The uploaded file has no data rows." },
        ]);
        setStatus("invalid");
        return;
      }

      // Map flat rows → nested objects
      const mapped = rawRows.map(mapRowToRelatedObject);

      // Validate
      setStatus("validating");
      const validationErrors = validateRows(mapped);

      if (validationErrors.length > 0) {
        setParsedRows(mapped);
        setErrors(validationErrors);
        setStatus("invalid");
        return;
      }

      // All good
      setParsedRows(mapped);
      setStatus("valid");
    },
    [parseFile, validateRows]
  );

  // ---- Submit confirmed rows ----
  const handleConfirm = useCallback(
    async (
      addRelatedObject: (obj: ParsedRelatedObject) => Promise<void>
    ) => {
      setStatus("submitting");
      setSubmissionError(null);

      try {
        for (const row of parsedRows) {
          await addRelatedObject(row);
        }
        setStatus("done");
      } catch (err) {
        setSubmissionError(
          err instanceof Error ? err.message : "Submission failed."
        );
        setStatus("error");
      }
    },
    [parsedRows]
  );

  // ---- Derived state ----
  const isConfirmDisabled =
    status !== "valid" || parsedRows.length === 0;

  const isUploading = status === "parsing" || status === "validating";

  return {
    // State
    status,
    file,
    parsedRows,
    errors,
    submissionError,

    // Actions
    handleFileUpload,
    handleConfirm,
    reset,

    // Derived
    isConfirmDisabled,
    isUploading,
  };
}