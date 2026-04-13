import { useState, useCallback, useMemo } from "react";
import { z } from "zod";

import {
  type BulkUploadVocabulary,
  type VocabularyEntry,
  buildLabelLookup,
  splitVocabularyKey,
} from "../types";

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

/**
 * Shape expected by the existing addRelatedObject() API handler.
 */
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

const DOI_SCHEMA_URI = "https://doi.org/";

// ------------------------------------------------------------------
// Validation schema (structural — vocab validity is checked in the mapper)
// ------------------------------------------------------------------

const doiRegex = /^https:\/\/doi\.org\/10\.\d{4,9}\/[^\s]+$/;
const webArchiveRegex =
  /^https:\/\/web\.archive\.org\/web\/\d{14}\/https:\/\/.*/;

const bulkRelatedObjectRowSchema = z.object({
  id: z
    .string()
    .trim()
    .min(1, "DOI URL is required")
    .refine((url) => doiRegex.test(url) || webArchiveRegex.test(url), {
      message:
        "Must be a valid DOI (https://doi.org/10.xxxx/...) or Web Archive URL",
    }),
  type: z.object({
    id: z.string().min(1, "Type is required"),
    schemaUri: z.string().min(1),
  }),
  category: z
    .array(
      z.object({
        id: z.string().min(1),
        schemaUri: z.string().min(1),
      })
    )
    .min(1, "At least one category is required"),
  schemaUri: z.string().min(1),
});

// ------------------------------------------------------------------
// CSV parsing
// ------------------------------------------------------------------

function parseCsvToRows(csvText: string): Record<string, string>[] {
  const lines = csvText.split(/\r?\n/).filter((line) => line.trim().length > 0);
  if (lines.length < 2) return [];

  const parseLine = (line: string): string[] => {
    const result: string[] = [];
    let current = "";
    let inQuotes = false;
    for (const char of line) {
      if (char === '"') {
        inQuotes = !inQuotes;
      } else if (char === "," && !inQuotes) {
        result.push(current.trim());
        current = "";
      } else {
        current += char;
      }
    }
    result.push(current.trim());
    return result;
  };

  const headers = parseLine(lines[0]);

  return lines.slice(1).map((line) => {
    const values = parseLine(line);
    const row: Record<string, string> = {};
    headers.forEach((header, i) => {
      row[header] = values[i] ?? "";
    });
    return row;
  });
}

// ------------------------------------------------------------------
// Row mapper — uses the runtime vocabulary lookups
// ------------------------------------------------------------------

function mapRowToRelatedObject(
  row: Record<string, string>,
  rowIndex: number,
  typeLookup: Map<string, VocabularyEntry>,
  categoryLookup: Map<string, VocabularyEntry>
): { data: ParsedRelatedObject | null; errors: ValidationError[] } {
  const errors: ValidationError[] = [];

  const doiUrl = (row["DOI URL"] ?? "").trim();
  const typeLabel = (row["Type"] ?? "").trim();
  const categoriesRaw = (row["Categories"] ?? "").trim();

  // ---- Look up type ----
  let typeUri = "";
  if (typeLabel) {
    const entry = typeLookup.get(typeLabel.toLowerCase());
    if (!entry) {
      errors.push({
        row: rowIndex,
        field: "Type",
        message: `Unknown type "${typeLabel}". Please use a value from the dropdown.`,
      });
    } else {
      typeUri = entry.key;
    }
  }

  // ---- Split and look up categories (multi-select support) ----
  const categoryLabels = categoriesRaw
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

  const categories: Array<{ id: string; schemaUri: string }> = [];
  for (const label of categoryLabels) {
    const entry = categoryLookup.get(label.toLowerCase());
    if (!entry) {
      const allowed = Array.from(categoryLookup.values())
        .map((e) => e.value)
        .join(", ");
      errors.push({
        row: rowIndex,
        field: "Categories",
        message: `Unknown category "${label}". Allowed values: ${allowed}.`,
      });
    } else {
      const { id, schemaUri } = splitVocabularyKey(entry.key);
      categories.push({ id, schemaUri });
    }
  }

  if (errors.length > 0) {
    return { data: null, errors };
  }

  const { id: typeId, schemaUri: typeSchemaUri } = splitVocabularyKey(typeUri);

  return {
    data: {
      id: doiUrl,
      schemaUri: DOI_SCHEMA_URI,
      type: { id: typeId, schemaUri: typeSchemaUri },
      category: categories,
    },
    errors: [],
  };
}

// ------------------------------------------------------------------
// Hook
// ------------------------------------------------------------------

export function useBulkUpload(vocabulary: BulkUploadVocabulary | undefined) {
  const [status, setStatus] = useState<BulkUploadStatus>("idle");
  const [file, setFile] = useState<File | null>(null);
  const [parsedRows, setParsedRows] = useState<ParsedRelatedObject[]>([]);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [submissionError, setSubmissionError] = useState<string | null>(null);

  // Build lookups once per vocabulary change. Safe against `undefined`
  // or partially-loaded vocabulary (e.g. async fetch still in flight).
  const typeLookup = useMemo(
    () => buildLabelLookup(vocabulary?.relatedObjectTypes ?? []),
    [vocabulary?.relatedObjectTypes]
  );
  const categoryLookup = useMemo(
    () => buildLabelLookup(vocabulary?.relatedObjectCategories ?? []),
    [vocabulary?.relatedObjectCategories]
  );

  const isVocabularyReady =
    !!vocabulary &&
    vocabulary.relatedObjectTypes.length > 0 &&
    vocabulary.relatedObjectCategories.length > 0;

  const reset = useCallback(() => {
    setStatus("idle");
    setFile(null);
    setParsedRows([]);
    setErrors([]);
    setSubmissionError(null);
  }, []);

  const parseFile = useCallback(
    async (file: File): Promise<Record<string, string>[]> => {
      const extension = file.name.split(".").pop()?.toLowerCase();

      if (extension === "csv") {
        const text = await file.text();
        return parseCsvToRows(text);
      }

      if (extension === "xlsx" || extension === "xls") {
        const ExcelJS = (await import("exceljs")).default;
        const workbook = new ExcelJS.Workbook();
        const buffer = await file.arrayBuffer();
        await workbook.xlsx.load(buffer);

        const sheet =
          workbook.getWorksheet("Related Objects") ?? workbook.worksheets[0];
        if (!sheet) return [];

        const rows: Record<string, string>[] = [];
        const headers: string[] = [];

        sheet.eachRow((row, rowNumber) => {
          if (rowNumber === 1) {
            row.eachCell((cell, colNumber) => {
              headers[colNumber - 1] = String(cell.value ?? "").trim();
            });
          } else {
            const rowObj: Record<string, string> = {};
            row.eachCell((cell, colNumber) => {
              const header = headers[colNumber - 1];
              if (header) {
                rowObj[header] = String(cell.value ?? "").trim();
              }
            });
            if (Object.values(rowObj).some((v) => v.length > 0)) {
              rows.push(rowObj);
            }
          }
        });

        return rows;
      }

      throw new Error(`Unsupported file type: .${extension}`);
    },
    []
  );

  const validateRows = useCallback(
    (
      rawRows: Record<string, string>[]
    ): { validRows: ParsedRelatedObject[]; errors: ValidationError[] } => {
      const allErrors: ValidationError[] = [];
      const validRows: ParsedRelatedObject[] = [];

      rawRows.forEach((rawRow, index) => {
        const rowNumber = index + 1;

        const { data, errors: mapErrors } = mapRowToRelatedObject(
          rawRow,
          rowNumber,
          typeLookup,
          categoryLookup
        );

        if (mapErrors.length > 0) {
          allErrors.push(...mapErrors);
          return;
        }

        if (!data) return;

        const result = bulkRelatedObjectRowSchema.safeParse(data);
        if (!result.success) {
          result.error.errors.forEach((zodError) => {
            const pathStr = zodError.path.join(".");
            const fieldName =
              pathStr === "id"
                ? "DOI URL"
                : pathStr.startsWith("type")
                  ? "Type"
                  : pathStr.startsWith("category")
                    ? "Categories"
                    : pathStr;

            allErrors.push({
              row: rowNumber,
              field: fieldName,
              message: zodError.message,
            });
          });
        } else {
          validRows.push(data);
        }
      });

      return { validRows, errors: allErrors };
    },
    [typeLookup, categoryLookup]
  );

  const handleFileUpload = useCallback(
    async (uploadedFile: File) => {
      setFile(uploadedFile);
      setErrors([]);
      setSubmissionError(null);

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
          {
            row: 0,
            field: "file",
            message: "The uploaded file has no data rows.",
          },
        ]);
        setStatus("invalid");
        return;
      }

      setStatus("validating");
      const { validRows, errors: validationErrors } = validateRows(rawRows);

      if (validationErrors.length > 0) {
        setParsedRows(validRows);
        setErrors(validationErrors);
        setStatus("invalid");
        return;
      }

      setParsedRows(validRows);
      setStatus("valid");
    },
    [parseFile, validateRows]
  );

  const handleConfirm = useCallback(
    async (addRelatedObject: (obj: ParsedRelatedObject) => Promise<void>) => {
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

  const isConfirmDisabled = status !== "valid" || parsedRows.length === 0;
  const isUploading = status === "parsing" || status === "validating";

  return {
    status,
    file,
    parsedRows,
    errors,
    submissionError,
    handleFileUpload,
    handleConfirm,
    reset,
    isConfirmDisabled,
    isUploading,
    isVocabularyReady,
  };
}