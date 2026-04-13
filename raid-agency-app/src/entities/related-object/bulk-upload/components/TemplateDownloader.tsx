import { Button, Stack } from "@mui/material";
import { Download as DownloadIcon } from "@mui/icons-material";

import type { BulkUploadVocabulary } from "../types";

/**
 * Three-column template:
 *   - DOI URL        : free text (validated via regex on parse)
 *   - Type           : single-select dropdown from vocabulary
 *   - Categories     : dropdown + comma-separated text for multi-select
 *                      (Excel does not natively support multi-select)
 */

const TEMPLATE_HEADERS = ["DOI URL", "Type", "Categories"];

interface TemplateDownloaderProps {
  vocabulary: BulkUploadVocabulary;
}

export function TemplateDownloader({ vocabulary }: TemplateDownloaderProps) {
  const typeLabels = vocabulary.relatedObjectTypes.map((t) => t.value);
  const categoryLabels = vocabulary.relatedObjectCategories.map((c) => c.value);

  // Pick sensible example values if the vocabulary contains them
  const exampleType =
    typeLabels.find((t) => t === "Output Management Plan") ??
    typeLabels[0] ??
    "";
  const exampleCategories =
    [
      categoryLabels.find((c) => c === "Input"),
      categoryLabels.find((c) => c === "Output"),
    ]
      .filter(Boolean)
      .join(", ") || categoryLabels[0] || "";

  const exampleRow = [
    "https://doi.org/10.25955/abc-123",
    exampleType,
    exampleCategories,
  ];

  const downloadCsv = () => {
    const headerLine = TEMPLATE_HEADERS.join(",");
    const exampleLine = exampleRow
      .map((cell) => (cell.includes(",") ? `"${cell}"` : cell))
      .join(",");

    const csvContent = `${headerLine}\n${exampleLine}\n`;
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    triggerDownload(blob, "related-objects-template.csv");
  };

  const downloadExcel = async () => {
    const ExcelJS = (await import("exceljs")).default;

    const workbook = new ExcelJS.Workbook();
    workbook.creator = "RAiD Agency App";
    workbook.created = new Date();

    // ---- Sheet 1: the template the user fills in ----
    const sheet = workbook.addWorksheet("Related Objects");

    sheet.addRow(TEMPLATE_HEADERS);
    const headerRow = sheet.getRow(1);
    headerRow.font = { bold: true };
    headerRow.fill = {
      type: "pattern",
      pattern: "solid",
      fgColor: { argb: "FFE6F1FB" },
    };
    headerRow.alignment = { vertical: "middle", horizontal: "center" };

    sheet.addRow(exampleRow);

    sheet.getColumn(1).width = 45; // DOI URL
    sheet.getColumn(2).width = 28; // Type
    sheet.getColumn(3).width = 32; // Categories

    // ---- Sheet 2: hidden lookup sheet for dropdown sources ----
    const lookupSheet = workbook.addWorksheet("_lookups", { state: "hidden" });

    typeLabels.forEach((label, i) => {
      lookupSheet.getCell(`A${i + 1}`).value = label;
    });
    categoryLabels.forEach((label, i) => {
      lookupSheet.getCell(`B${i + 1}`).value = label;
    });

    workbook.definedNames.add(
      `_lookups!$A$1:$A$${typeLabels.length}`,
      "TypeList"
    );
    workbook.definedNames.add(
      `_lookups!$B$1:$B$${categoryLabels.length}`,
      "CategoryList"
    );

    // ---- Type column: strict single-select dropdown ----
    for (let row = 2; row <= 1000; row++) {
      sheet.getCell(`B${row}`).dataValidation = {
        type: "list",
        allowBlank: true,
        formulae: ["TypeList"],
        showErrorMessage: true,
        errorStyle: "error",
        errorTitle: "Invalid type",
        error: "Please select a type from the dropdown list.",
      };
    }

    // ---- Categories column: soft dropdown with multi-value support ----
    for (let row = 2; row <= 1000; row++) {
      sheet.getCell(`C${row}`).dataValidation = {
        type: "list",
        allowBlank: true,
        formulae: ["CategoryList"],
        showInputMessage: true,
        promptTitle: "Categories",
        prompt:
          "Select one value, or type multiple values separated by commas (e.g. Input, Output).",
        showErrorMessage: false,
      };
    }

    // ---- Sheet 3: instructions ----
    const instructions = workbook.addWorksheet("Instructions");
    instructions.getColumn(1).width = 90;

    const instructionLines: string[][] = [
      ["RAiD Related Objects — Bulk Upload Template"],
      [""],
      ["Columns:"],
      ["  1. DOI URL       — Full DOI link, e.g. https://doi.org/10.25955/abc-123"],
      ["                    Web Archive URLs are also accepted:"],
      ["                    https://web.archive.org/web/20240101120000/https://example.com"],
      ["  2. Type          — Select a value from the dropdown"],
      ["  3. Categories    — Select a single value from the dropdown, OR"],
      ["                    type multiple values separated by commas,"],
      ["                    e.g. Input, Output"],
      [""],
      ["Row 2 contains an example — delete it before uploading if not needed."],
      [""],
      ["Allowed types:"],
      ...typeLabels.map((t) => [`  • ${t}`]),
      [""],
      ["Allowed categories:"],
      ...categoryLabels.map((c) => [`  • ${c}`]),
    ];

    instructionLines.forEach((line) => instructions.addRow(line));
    instructions.getRow(1).font = { bold: true, size: 14 };

    const buffer = await workbook.xlsx.writeBuffer();
    const blob = new Blob([buffer], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });

    triggerDownload(blob, "related-objects-template.xlsx");
  };

  return (
    <Stack direction="row" spacing={1}>
      <Button
        variant="outlined"
        size="small"
        startIcon={<DownloadIcon />}
        onClick={downloadExcel}
      >
        Download Excel template
      </Button>
      <Button
        variant="outlined"
        size="small"
        startIcon={<DownloadIcon />}
        onClick={downloadCsv}
      >
        Download CSV template
      </Button>
    </Stack>
  );
}

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
