import { useState, useCallback, useMemo, useRef } from "react";
import { z } from "zod";

import {
  type BulkUploadVocabulary,
  type VocabularyEntry,
  buildLabelLookup,
  TYPE_SCHEMA_URI,
  CATEGORY_SCHEMA_URI,
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
 * One row in the editable preview table. Holds the raw user-facing
 * values (as the user sees and edits them) plus the per-field errors
 * for that row. The row is converted to one or more ParsedRelatedObject
 * records at submission time via `mapRowToRelatedObjects`.
 */
export interface EditableRow {
  /** Stable identifier so React can track rows across edits and reorders */
  id: string;
  /** Raw spreadsheet column values, edited in-place by the user */
  values: {
    URL: string;
    Type: string;
    Categories: string;
  };
  /** Field-keyed errors for this row. Empty object = row is valid. */
  errors: Partial<Record<"URL" | "Type" | "Categories", string>>;
}

export type EditableRowField = keyof EditableRow["values"];

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

/**
 * Coerces an `exceljs` cell value to a plain trimmed string.
 *
 * `cell.value` can be many shapes depending on what's in the cell:
 *   - string / number / boolean / Date / null / undefined  → primitive
 *   - { text, hyperlink, tooltip }                         → hyperlink (URLs)
 *   - { richText: [{ text, font }, ...] }                  → rich-text run
 *   - { formula, result }                                  → formula
 *   - { error: '#NAME?' }                                  → error cell
 *
 * Without this helper, `String(cell.value)` returns `"[object Object]"`
 * for hyperlink cells — which is what happens when a user pastes a URL
 * into Excel and Excel auto-converts it into a hyperlink object.
 */
function readCellAsString(cell: { value: unknown }): string {
  const v = cell.value;
  if (v === null || v === undefined) return "";

  // Primitive types — convert directly
  if (typeof v === "string") return v.trim();
  if (typeof v === "number" || typeof v === "boolean") return String(v).trim();
  if (v instanceof Date) return v.toISOString().trim();

  // Object types — pick the right field for each shape
  if (typeof v === "object") {
    const obj = v as Record<string, unknown>;

    // Hyperlink: prefer the visible text, fall back to the underlying URL
    if (typeof obj.text === "string") return obj.text.trim();
    if (typeof obj.hyperlink === "string") return obj.hyperlink.trim();

    // Rich text: concatenate all the runs
    if (Array.isArray(obj.richText)) {
      return obj.richText
        .map((run: unknown) => {
          if (typeof run === "object" && run !== null) {
            const r = run as Record<string, unknown>;
            return typeof r.text === "string" ? r.text : "";
          }
          return "";
        })
        .join("")
        .trim();
    }

    // Formula: prefer the calculated result
    if ("result" in obj) {
      return readCellAsString({ value: obj.result });
    }
  }

  return "";
}

const doiRegex = /^https:\/\/doi\.org\/10\.\d{4,9}\/[^\s]+$/;
const webArchiveRegex =
  /^https:\/\/web\.archive\.org\/web\/\d{14}\/https:\/\/.*/;

const bulkRelatedObjectRowSchema = z.object({
  id: z
    .string()
    .trim()
    .min(1, "URL is required")
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

  const doiUrl = (row["URL"] ?? "").trim();
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

/**
 * Validates a single editable row and returns:
 *   - field-keyed errors (for highlighting cells in the preview table)
 *   - the expanded ParsedRelatedObject[] if the row is valid (for submission)
 *
 * This runs the same mapper + Zod schema as `validateRows`, but produces
 * errors in the per-field shape the preview table needs.
 */
function validateEditableRow(
  values: EditableRow["values"],
  typeLookup: Map<string, VocabularyEntry>,
  categoryLookup: Map<string, VocabularyEntry>,
  generator?: () => Partial<ParsedRelatedObject>
): {
  errors: EditableRow["errors"];
  expanded: ParsedRelatedObject[];
} {
  const fieldErrors: EditableRow["errors"] = {};

  // Reuse the existing mapper, but ignore its rowIndex since the preview
  // table uses field names rather than row numbers for error placement.
  const rawRow: Record<string, string> = {
    URL: values["URL"],
    Type: values.Type,
    Categories: values.Categories,
  };

  const { data: expanded, errors: mapErrors } = mapRowToRelatedObjects(
    rawRow,
    0,
    typeLookup,
    categoryLookup,
    generator
  );

  // Convert mapper errors into the field-keyed shape
  for (const err of mapErrors) {
    const field = err.field as EditableRowField;
    if (!fieldErrors[field]) {
      fieldErrors[field] = err.message;
    }
  }

  // Run Zod schema on each expanded object — should pass or fail together
  for (const obj of expanded) {
    const result = bulkRelatedObjectRowSchema.safeParse(obj);
    if (!result.success) {
      result.error.errors.forEach((zodError) => {
        const pathStr = zodError.path.join(".");
        const fieldName: EditableRowField =
          pathStr === "id"
            ? "URL"
            : pathStr.startsWith("type")
              ? "Type"
              : pathStr.startsWith("category")
                ? "Categories"
                : ("URL" as EditableRowField);

        if (!fieldErrors[fieldName]) {
          fieldErrors[fieldName] = zodError.message;
        }
      });
    }
  }

  return {
    errors: fieldErrors,
    expanded: Object.keys(fieldErrors).length === 0 ? expanded : [],
  };
}

// ------------------------------------------------------------------
// Duplicate detection
// ------------------------------------------------------------------

const DUPLICATE_ERROR_PREFIX = "Duplicate:";
const MAX_ROWS = 100;

/**
 * Scans all rows for duplicate URL + Type combinations and stamps a
 * URL-field error onto each affected row, naming the other row numbers
 * involved (e.g. "Duplicate: same URL and Type as row 3").
 * Existing URL format errors take precedence — a duplicate error is only
 * added when the URL field has no other error. Stale duplicate errors are
 * cleared when the duplication is resolved.
 */
function applyDuplicateErrors(rows: EditableRow[]): EditableRow[] {
  // Map key -> list of 0-based row indices that share that key
  const keyToIndices = new Map<string, number[]>();

  rows.forEach((row, idx) => {
    const url = row.values.URL.trim().toLowerCase();
    if (!url) return;

    const types = row.values.Type.split(",")
      .map((t) => t.trim().toLowerCase())
      .filter((t) => t.length > 0);

    for (const type of types) {
      const key = `${url}|||${type}`;
      const existing = keyToIndices.get(key) ?? [];
      existing.push(idx);
      keyToIndices.set(key, existing);
    }
  });

  // Build a map from row index -> sorted set of other 1-based row numbers it duplicates
  const othersMap = new Map<number, Set<number>>();
  for (const indices of keyToIndices.values()) {
    if (indices.length < 2) continue;
    for (const idx of indices) {
      const others = othersMap.get(idx) ?? new Set<number>();
      for (const otherIdx of indices) {
        if (otherIdx !== idx) others.add(otherIdx + 1);
      }
      othersMap.set(idx, others);
    }
  }

  return rows.map((row, idx) => {
    const others = othersMap.get(idx);
    const isDuplicate = others !== undefined && others.size > 0;
    const hasDuplicateError = row.errors.URL?.startsWith(DUPLICATE_ERROR_PREFIX);

    if (isDuplicate && !row.errors.URL) {
      const sorted = Array.from(others).sort((a, b) => a - b);
      const othersText =
        sorted.length === 1
          ? `row ${sorted[0]}`
          : `rows ${sorted.slice(0, -1).join(", ")} and ${sorted[sorted.length - 1]}`;
      return {
        ...row,
        errors: {
          ...row.errors,
          URL: `${DUPLICATE_ERROR_PREFIX} same URL and Type as ${othersText}`,
        },
      };
    }
    if (!isDuplicate && hasDuplicateError) {
      const errors = { ...row.errors };
      delete errors.URL;
      return { ...row, errors };
    }
    return row;
  });
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
  /** Called once after all objects have been appended to the form. */
  onComplete?: () => void;
}

export function useBulkUpload(
  vocabulary: BulkUploadVocabulary | undefined,
  options: UseBulkUploadOptions = {}
) {
  const { generator, onComplete } = options;
  const rowCounter = useRef(0);
  const [status, setStatus] = useState<BulkUploadStatus>("idle");
  const [file, setFile] = useState<File | null>(null);
  const [editableRows, setEditableRows] = useState<EditableRow[]>([]);
  const [submissionError, setSubmissionError] = useState<string | null>(null);
  const [submissionProgress, setSubmissionProgress] = useState<{ current: number; total: number } | null>(null);

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
    setEditableRows([]);
    setSubmissionError(null);
    setSubmissionProgress(null);
  }, []);

  const parseFile = useCallback(
    async (file: File): Promise<Record<string, string>[]> => {
      const extension = file.name.split(".").pop()?.toLowerCase();

      if (extension === "csv") {
        const text = await file.text();
        return parseCsvToRows(text);
      }

      if (extension === "xlsx" || extension === "xls") {
        let ExcelJS: typeof import("exceljs");
        try {
          ExcelJS = await import("exceljs");
        } catch {
          throw new Error(
            "Unable to load Excel parser. Please try saving your file as CSV and uploading that instead."
          );
        }
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
          const colA = readCellAsString(row.getCell(1));
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
          headers[colNumber - 1] = readCellAsString(cell);
        });

        // ---- Read data rows ----
        const rows: Record<string, string>[] = [];
        for (let r = headerRowIdx + 1; r <= lastDataRowIdx; r++) {
          const row = sheet.getRow(r);
          const rowObj: Record<string, string> = {};
          row.eachCell((cell, colNumber) => {
            const header = headers[colNumber - 1];
            if (header) {
              rowObj[header] = readCellAsString(cell);
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

  /**
   * Builds an EditableRow from a raw spreadsheet row dict, running the
   * single-row validator to compute initial errors.
   */
  const buildEditableRow = useCallback(
    (rawRow: Record<string, string>): EditableRow => {
      const values: EditableRow["values"] = {
        URL: rawRow["URL"] ?? "",
        Type: rawRow["Type"] ?? "",
        Categories: rawRow["Categories"] ?? "",
      };

      const { errors } = validateEditableRow(
        values,
        typeLookup,
        categoryLookup,
        generator
      );

      return {
        id: `row-${rowCounter.current++}`,
        values,
        errors,
      };
    },
    [typeLookup, categoryLookup, generator]
  );

  const handleFileUpload = useCallback(
    async (uploadedFile: File) => {
      setFile(uploadedFile);
      setSubmissionError(null);

      setStatus("parsing");
      let rawRows: Record<string, string>[];
      try {
        rawRows = await parseFile(uploadedFile);
      } catch (err) {
        setEditableRows([]);
        setStatus("error");
        setSubmissionError(
          err instanceof Error
            ? err.message
            : "Failed to parse the uploaded file."
        );
        return;
      }

      if (rawRows.length === 0) {
        setEditableRows([]);
        setStatus("error");
        setSubmissionError("The uploaded file has no data rows.");
        return;
      }

      const REQUIRED_COLUMNS = ["URL", "Type", "Categories"] as const;
      const firstRow = rawRows[0] ?? {};
      const missingColumns = REQUIRED_COLUMNS.filter((col) => !(col in firstRow));
      if (missingColumns.length > 0) {
        setEditableRows([]);
        setStatus("error");
        setSubmissionError(
          `Missing required column(s): ${missingColumns.join(", ")}. Please use the downloaded template and ensure the header row contains URL, Type, and Categories.`
        );
        return;
      }

      if (rawRows.length > MAX_ROWS) {
        setEditableRows([]);
        setStatus("error");
        setSubmissionError(
          `Too many rows: the file contains ${rawRows.length} data rows but the maximum allowed is ${MAX_ROWS}. Please split your file into smaller batches.`
        );
        return;
      }

      setStatus("validating");

      const rows = applyDuplicateErrors(
        rawRows.map((rawRow) => buildEditableRow(rawRow))
      );

      setEditableRows(rows);
      setStatus(rows.some((r) => Object.keys(r.errors).length > 0) ? "invalid" : "valid");
    },
    [parseFile, buildEditableRow]
  );

  /**
   * Updates a single cell in the preview table and re-runs validation
   * for that row. Other rows are not touched.
   */
  const updateRow = useCallback(
    (rowIndex: number, field: EditableRowField, value: string) => {
      setEditableRows((prev) => {
        const next = [...prev];
        const target = next[rowIndex];
        if (!target) return prev;

        const newValues = { ...target.values, [field]: value };
        const { errors } = validateEditableRow(
          newValues,
          typeLookup,
          categoryLookup,
          generator
        );

        next[rowIndex] = { ...target, values: newValues, errors };

        // Re-run duplicate detection across all rows after each cell edit
        const withDups = applyDuplicateErrors(next);

        const stillHasErrors = withDups.some(
          (r) => Object.keys(r.errors).length > 0
        );
        setStatus(stillHasErrors ? "invalid" : "valid");

        return withDups;
      });
    },
    [typeLookup, categoryLookup, generator]
  );

  /**
   * Removes a row from the preview entirely.
   */
  const removeRow = useCallback((rowIndex: number) => {
    setEditableRows((prev) => {
      const next = prev.filter((_, i) => i !== rowIndex);

      if (next.length === 0) {
        setStatus("idle");
        return next;
      }

      // Re-run duplicate detection — removing a row may resolve a duplicate
      const withDups = applyDuplicateErrors(next);
      const stillHasErrors = withDups.some(
        (r) => Object.keys(r.errors).length > 0
      );
      setStatus(stillHasErrors ? "invalid" : "valid");

      return withDups;
    });
  }, []);

  const handleConfirm = useCallback(
    async (addRelatedObject: (obj: ParsedRelatedObject) => Promise<void>) => {
      if (editableRows.length > MAX_ROWS) {
        setSubmissionError(
          `Too many rows: ${editableRows.length} rows present but the maximum allowed is ${MAX_ROWS}. Please remove some rows before uploading.`
        );
        return;
      }

      // Re-validate every row and collect expanded objects in a single pass.
      type RowResult = { row: EditableRow; expanded: ParsedRelatedObject[] };
      const results: RowResult[] = editableRows.map((row) => {
        const { errors, expanded } = validateEditableRow(
          row.values,
          typeLookup,
          categoryLookup,
          generator
        );
        return { row: { ...row, errors }, expanded };
      });

      const revalidated = applyDuplicateErrors(results.map((r) => r.row));

      if (revalidated.some((r) => Object.keys(r.errors).length > 0)) {
        setSubmissionError(
          "Some rows still have validation errors. Please fix them before uploading."
        );
        return;
      }

      const allExpanded: ParsedRelatedObject[] = results.flatMap((r) => r.expanded);

      setStatus("submitting");
      setSubmissionError(null);
      setSubmissionProgress({ current: 0, total: allExpanded.length });

      try {
        for (let i = 0; i < allExpanded.length; i++) {
          await addRelatedObject(allExpanded[i]);
          setSubmissionProgress({ current: i + 1, total: allExpanded.length });
        }
        setSubmissionProgress(null);
        setStatus("done");
        onComplete?.();
      } catch (err) {
        setSubmissionError(
          err instanceof Error ? err.message : "Submission failed."
        );
        setStatus("error");
      }
    },
    [editableRows, typeLookup, categoryLookup, generator, onComplete]
  );

  // ---- Derived state ----

  const totalErrorCount = editableRows.reduce(
    (sum, row) => sum + Object.keys(row.errors).length,
    0
  );
  const hasAnyRows = editableRows.length > 0;
  const allRowsValid = hasAnyRows && totalErrorCount === 0;

  const isConfirmDisabled =
    !allRowsValid || status === "submitting" || status === "done";
  const isUploading = status === "parsing" || status === "validating";

  return {
    // Status
    status,
    file,
    submissionError,
    submissionProgress,
    isUploading,
    isVocabularyReady,

    // Preview data
    editableRows,
    totalErrorCount,
    hasAnyRows,
    allRowsValid,

    // Actions
    handleFileUpload,
    updateRow,
    removeRow,
    handleConfirm,
    reset,

    // Derived
    isConfirmDisabled,
  };
}
