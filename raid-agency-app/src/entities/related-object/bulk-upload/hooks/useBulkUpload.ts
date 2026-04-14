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

/** Sentinel markers that delimit the import region in the template. */
const IMPORT_START_MARKER = "[Import table starts here]";
const IMPORT_END_MARKER = "[Import table ends here]";

/**
 * Marker that Excel produces when it tries to evaluate a cell that
 * starts with '-', '=', '+', or '@' as a formula but can't.
 * If we see this in column A, the user almost certainly opened an
 * older version of the template (with '---' sentinels) in Excel and
 * Excel mangled them on save.
 */
const EXCEL_FORMULA_ERROR = "#NAME?";

/**
 * Splits a single CSV line into cells, respecting double-quoted values
 * that may contain commas.
 */
function parseCsvLine(line: string): string[] {
  const result: string[] = [];
  let current = "";
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const char = line[i];

    if (char === '"') {
      // Handle escaped quotes ("")
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === "," && !inQuotes) {
      result.push(current.trim());
      current = "";
    } else {
      current += char;
    }
  }

  result.push(current.trim());
  return result;
}

/**
 * Parses CSV content into row objects. The template uses sentinel rows
 * to mark the import region — only rows between IMPORT_START_MARKER and
 * IMPORT_END_MARKER are treated as data, with the first row inside the
 * region being the column headers.
 *
 * If no sentinels are found (e.g. user pasted into a fresh CSV), the
 * parser falls back to treating the whole file as a flat table.
 */
function parseCsvToRows(csvText: string): Record<string, string>[] {
  const allLines = csvText.split(/\r?\n/);

  // Detect Excel corruption: cells starting with `-`, `=`, `+`, or `@`
  // are treated as formulas by Excel and saved back as `#NAME?` if they
  // can't be evaluated. If we see this marker in column A, the user
  // saved the template through Excel after we used `---` sentinels.
  const hasCorruption = allLines.some((line) => {
    const firstCell = parseCsvLine(line)[0] ?? "";
    return firstCell === EXCEL_FORMULA_ERROR;
  });

  if (hasCorruption) {
    throw new Error(
      "This file appears to have been corrupted by Excel (some marker rows show #NAME?). Please re-download the template and try again."
    );
  }

  // Find the sentinel boundaries. We check column A only — the marker
  // text is the first cell on its row.
  let startIdx = -1;
  let endIdx = -1;

  for (let i = 0; i < allLines.length; i++) {
    const firstCell = parseCsvLine(allLines[i])[0] ?? "";
    if (firstCell === IMPORT_START_MARKER && startIdx === -1) {
      startIdx = i;
    } else if (firstCell === IMPORT_END_MARKER && startIdx !== -1) {
      endIdx = i;
      break;
    }
  }

  // Fallback: no sentinels found, treat the whole file as a flat table
  let dataLines: string[];
  if (startIdx === -1 || endIdx === -1) {
    dataLines = allLines.filter((line) => line.trim().length > 0);
  } else {
    // Lines strictly between the sentinels, blank lines stripped
    dataLines = allLines
      .slice(startIdx + 1, endIdx)
      .filter((line) => line.trim().length > 0);
  }

  if (dataLines.length < 2) return [];

  const headers = parseCsvLine(dataLines[0]);

  return dataLines
    .slice(1)
    .map((line) => {
      const values = parseCsvLine(line);
      const row: Record<string, string> = {};
      headers.forEach((header, i) => {
        row[header] = values[i] ?? "";
      });
      return row;
    })
    // Skip rows where every cell is empty. This catches blank lines
    // that snuck through the earlier filter (e.g. ",,") and rows the
    // user cleared but didn't delete.
    .filter((row) => Object.values(row).some((v) => v.length > 0));
}

// ------------------------------------------------------------------
// Row mapper — uses the runtime vocabulary lookups
// ------------------------------------------------------------------

/**
 * The schema URI *values* used inside the type and category sub-objects
 * are NOT the same as the ID's parent URI — they're separate schema
 * version URIs. Looking at actual RAiD payloads:
 *
 *   type.id        = https://vocabulary.raid.org/relatedObject.type.schema/250
 *   type.schemaUri = https://vocabulary.raid.org/relatedObject.type.schema/329
 *
 *   category[].id        = https://vocabulary.raid.org/relatedObject.category.id/190
 *   category[].schemaUri = https://vocabulary.raid.org/relatedObject.category.schemaUri/386
 *
 * The `.id` is the full vocabulary key (straight from the vocab JSON).
 * The `.schemaUri` is a version identifier that the backend expects.
 */
const TYPE_SCHEMA_URI =
  "https://vocabulary.raid.org/relatedObject.type.schema/329";
const CATEGORY_SCHEMA_URI =
  "https://vocabulary.raid.org/relatedObject.category.schemaUri/386";

/**
 * Maps a single spreadsheet row into one or more ParsedRelatedObject
 * records. When the Type column contains multiple comma-separated values,
 * the row is expanded: one ParsedRelatedObject per type, each sharing
 * the same DOI and categories.
 *
 * If you prefer different multi-type semantics, change the expansion
 * block below. Three common strategies:
 *
 *   1. Expand → one object per type (current behaviour)
 *   2. First wins → only use types[0], warn if more were listed
 *   3. Strict → reject the row with an error if more than one type is given
 */
function mapRowToRelatedObjects(
  row: Record<string, string>,
  rowIndex: number,
  typeLookup: Map<string, VocabularyEntry>,
  categoryLookup: Map<string, VocabularyEntry>,
  generator?: () => Partial<ParsedRelatedObject>
): { data: ParsedRelatedObject[]; errors: ValidationError[] } {
  const errors: ValidationError[] = [];

  const doiUrl = (row["DOI URL"] ?? "").trim();
  const typesRaw = (row["Type"] ?? "").trim();
  const categoriesRaw = (row["Categories"] ?? "").trim();

  // ---- Split and look up types (multi-value support) ----
  const typeLabels = typesRaw
    .split(",")
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

  if (typeLabels.length === 0) {
    errors.push({
      row: rowIndex,
      field: "Type",
      message: "At least one type is required.",
    });
  }

  const typeEntries: Array<{ id: string; schemaUri: string }> = [];
  for (const label of typeLabels) {
    const entry = typeLookup.get(label.toLowerCase());
    if (!entry) {
      errors.push({
        row: rowIndex,
        field: "Type",
        message: `Unknown type "${label}". Please use a value from the dropdown.`,
      });
    } else {
      typeEntries.push({
        id: entry.key, // Full URI straight from vocab JSON
        schemaUri: TYPE_SCHEMA_URI,
      });
    }
  }

  // ---- Split and look up categories (multi-select) ----
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
      categories.push({
        id: entry.key, // Full URI from vocab
        schemaUri: CATEGORY_SCHEMA_URI,
      });
    }
  }

  if (errors.length > 0) {
    return { data: [], errors };
  }

  // ---- Expand row: one related object per selected type ----
  // Start each from the generator's default shape so react-hook-form gets
  // every field it expects (empty title arrays, default citation, etc.)
  // and then overwrite the bulk-upload fields.
  const expanded: ParsedRelatedObject[] = typeEntries.map((type) => {
    const base = generator ? generator() : {};
    return {
      ...base,
      id: doiUrl,
      schemaUri: DOI_SCHEMA_URI,
      type,
      category: categories,
    } as ParsedRelatedObject;
  });

  return { data: expanded, errors: [] };
}

// ------------------------------------------------------------------
// Hook
// ------------------------------------------------------------------

export interface UseBulkUploadOptions {
  /**
   * Optional: a function that returns the default shape for a new
   * related object (same one used by the manual add flow's
   * `relatedObjectDataGenerator`). The bulk upload will merge parsed
   * values on top of this base, ensuring react-hook-form gets every
   * field it expects.
   */
  generator?: () => Partial<ParsedRelatedObject>;
}

export function useBulkUpload(
  vocabulary: BulkUploadVocabulary | undefined,
  options: UseBulkUploadOptions = {}
) {
  const { generator } = options;
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

        // ---- Locate the sentinel rows in column A ----
        let startRowIdx = -1;
        let endRowIdx = -1;

        sheet.eachRow((row, rowNumber) => {
          const colA = String(row.getCell(1).value ?? "").trim();
          if (colA === IMPORT_START_MARKER && startRowIdx === -1) {
            startRowIdx = rowNumber;
          } else if (colA === IMPORT_END_MARKER && startRowIdx !== -1 && endRowIdx === -1) {
            endRowIdx = rowNumber;
          }
        });

        // Determine the data region. If sentinels are missing, fall back
        // to assuming row 1 = headers (legacy behaviour).
        const headerRowIdx = startRowIdx !== -1 ? startRowIdx + 1 : 1;
        const lastDataRowIdx =
          endRowIdx !== -1 ? endRowIdx - 1 : sheet.actualRowCount;

        if (headerRowIdx > lastDataRowIdx) return [];

        // ---- Read headers ----
        const headers: string[] = [];
        const headerRow = sheet.getRow(headerRowIdx);
        headerRow.eachCell((cell, colNumber) => {
          headers[colNumber - 1] = String(cell.value ?? "").trim();
        });

        // ---- Read data rows ----
        const rows: Record<string, string>[] = [];
        for (let r = headerRowIdx + 1; r <= lastDataRowIdx; r++) {
          const row = sheet.getRow(r);
          const rowObj: Record<string, string> = {};
          row.eachCell((cell, colNumber) => {
            const header = headers[colNumber - 1];
            if (header) {
              rowObj[header] = String(cell.value ?? "").trim();
            }
          });
          // Skip rows where every data column is empty
          if (Object.values(rowObj).some((v) => v.length > 0)) {
            rows.push(rowObj);
          }
        }

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

        // Mapper may return 0, 1, or N objects depending on how many
        // types were selected in the Type column.
        const { data: expandedObjects, errors: mapErrors } =
          mapRowToRelatedObjects(
            rawRow,
            rowNumber,
            typeLookup,
            categoryLookup,
            generator
          );

        if (mapErrors.length > 0) {
          allErrors.push(...mapErrors);
          return;
        }

        if (expandedObjects.length === 0) return;

        // Run schema validation on each expanded object. All of them
        // should pass or fail together since they share the same source
        // row, but we validate individually to catch any edge cases.
        for (const obj of expandedObjects) {
          const result = bulkRelatedObjectRowSchema.safeParse(obj);
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
            validRows.push(obj);
          }
        }
      });

      return { validRows, errors: allErrors };
    },
    [typeLookup, categoryLookup, generator]
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