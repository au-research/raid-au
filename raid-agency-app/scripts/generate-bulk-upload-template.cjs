/**
 * Regenerates public/templates/related-objects-template.xlsx
 *
 * Run from the raid-agency-app directory:
 *   node scripts/generate-bulk-upload-template.cjs
 *
 * Requires: exceljs (already a project dependency via the bulk-upload feature)
 */

const ExcelJS = require("exceljs");
const path = require("path");

const OUTPUT_PATH = path.join(
  __dirname,
  "..",
  "public",
  "templates",
  "related-objects-template.xlsx"
);

const INSTRUCTIONS_START = "--- Instructions for use start here";
const INSTRUCTIONS_END = "--- Instructions for use end here";
const IMPORT_START_MARKER = "Import table starts here";
const IMPORT_END_MARKER = "Import table ends here";

const TYPES = [
  "Audiovisual",
  "Book",
  "Book Chapter",
  "Computational Notebook",
  "Conference Paper",
  "Conference Poster",
  "Conference Proceeding",
  "Data Paper",
  "Dataset",
  "Dissertation",
  "Event",
  "Funding",
  "Image",
  "Instrument",
  "Journal Article",
  "Learning Object",
  "Model",
  "Output Management Plan",
  "Physical Object",
  "Preprint",
  "Prize",
  "Report",
  "Service",
  "Software",
  "Sound",
  "Standard",
  "Text",
  "Workflow",
];

const CATEGORIES = [
  "Input",
  "Internal process document or artefact",
  "Output",
  "Input,Internal process document or artefact",
  "Internal process document or artefact,Output",
  "Input,Internal process document or artefact,Output",
];

async function generate() {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = "RAiD";
  workbook.lastModifiedBy = "RAiD";

  // ---- Hidden vocabulary sheet (referenced by data validation) ----
  const vocabSheet = workbook.addWorksheet("Vocabulary");
  vocabSheet.state = "veryHidden";

  TYPES.forEach((t, i) => {
    vocabSheet.getCell(`A${i + 1}`).value = t;
  });
  CATEGORIES.forEach((c, i) => {
    vocabSheet.getCell(`B${i + 1}`).value = c;
  });

  // Define named ranges so validation formulae are portable
  workbook.definedNames.add(
    `Vocabulary!$A$1:$A$${TYPES.length}`,
    "TypeList"
  );
  workbook.definedNames.add(
    `Vocabulary!$B$1:$B$${CATEGORIES.length}`,
    "CategoryList"
  );

  // ---- Main data sheet ----
  const sheet = workbook.addWorksheet("Related Objects");

  // Column widths
  sheet.getColumn(1).width = 65; // Identifier
  sheet.getColumn(2).width = 30; // Type
  sheet.getColumn(3).width = 55; // Categories

  // ---- Instructions block ----
  const instructionLines = [
    "No more than 100 relatedObjects can be added at one time",
    `Add extra rows below the headers between the rows labelled "${IMPORT_START_MARKER}" and "${IMPORT_END_MARKER}"`,
    `Do not delete the rows labelled "${IMPORT_START_MARKER}" and "${IMPORT_END_MARKER}"`,
    "Identifier must be the full URL of one of the types supported by RAiD. For example, a DOI would look like https://doi.org/10.0001/01238",
    "Use the Excel drop-down lists to avoid typos",
  ];

  let currentRow = 1;

  // Instructions start marker
  const instrStartCell = sheet.getCell(`A${currentRow}`);
  instrStartCell.value = INSTRUCTIONS_START;
  instrStartCell.font = { bold: true, color: { argb: "FF8E489B" } };
  sheet.mergeCells(`A${currentRow}:C${currentRow}`);
  currentRow++;

  instructionLines.forEach((text) => {
    const cell = sheet.getCell(`A${currentRow}`);
    cell.value = text;
    cell.font = { italic: true, color: { argb: "FF444444" } };
    sheet.mergeCells(`A${currentRow}:C${currentRow}`);
    currentRow++;
  });

  // Instructions end marker
  const instrEndCell = sheet.getCell(`A${currentRow}`);
  instrEndCell.value = INSTRUCTIONS_END;
  instrEndCell.font = { bold: true, color: { argb: "FF8E489B" } };
  sheet.mergeCells(`A${currentRow}:C${currentRow}`);
  currentRow++;

  // Blank row
  currentRow++;

  // ---- Sentinel start ----
  const startRow = currentRow;
  sheet.getCell(`A${startRow}`).value = IMPORT_START_MARKER;
  styleMarkerRow(sheet, startRow);

  // ---- Header row ----
  const headerRow = startRow + 1;
  const headerFill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF4A4A8A" } };
  ["Identifier", "Type", "Categories"].forEach((label, col) => {
    const cell = sheet.getRow(headerRow).getCell(col + 1);
    cell.value = label;
    cell.font = { bold: true, color: { argb: "FFFFFFFF" } };
    cell.fill = headerFill;
    cell.alignment = { vertical: "middle", horizontal: "center" };
  });

  // ---- Data rows (1 example + 4 blank rows) ----
  const dataStartRow = headerRow + 1;
  const dataEndRow = dataStartRow + 4;

  // Example row
  sheet.getCell(`A${dataStartRow}`).value = "https://doi.org/10.0001/01238";
  sheet.getCell(`B${dataStartRow}`).value = "Book";
  sheet.getCell(`C${dataStartRow}`).value = "Output";

  for (let r = dataStartRow; r <= dataEndRow; r++) {
    // Alternate row shading
    if (r % 2 === 0) {
      for (let c = 1; c <= 3; c++) {
        sheet.getRow(r).getCell(c).fill = {
          type: "pattern",
          pattern: "solid",
          fgColor: { argb: "FFF5F5F5" },
        };
      }
    }

    // Type dropdown (column B)
    sheet.getCell(`B${r}`).dataValidation = {
      type: "list",
      allowBlank: true,
      formulae: [`=Vocabulary!$A$1:$A$${TYPES.length}`],
      showErrorMessage: true,
      errorStyle: "stop",
      errorTitle: "Invalid Type",
      error: `Please select a value from the dropdown list.`,
    };

    // Categories dropdown (column C)
    sheet.getCell(`C${r}`).dataValidation = {
      type: "list",
      allowBlank: true,
      formulae: [`=Vocabulary!$B$1:$B$${CATEGORIES.length}`],
      showErrorMessage: true,
      errorStyle: "stop",
      errorTitle: "Invalid Category",
      error: `Please select a value from the dropdown list.`,
    };
  }

  // ---- Sentinel end ----
  const endRow = dataEndRow + 1;
  sheet.getCell(`A${endRow}`).value = IMPORT_END_MARKER;
  styleMarkerRow(sheet, endRow);

  // Freeze the header row
  sheet.views = [{ state: "frozen", ySplit: headerRow }];

  // ---- Write file ----
  await workbook.xlsx.writeFile(OUTPUT_PATH);
  console.log(`Written: ${OUTPUT_PATH}`);
}

function styleMarkerRow(sheet, rowNumber) {
  const row = sheet.getRow(rowNumber);
  const cell = row.getCell(1);
  cell.font = { bold: true, color: { argb: "FF8E489B" } };
  cell.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FFF3E5F5" } };
  sheet.mergeCells(`A${rowNumber}:C${rowNumber}`);
}

generate().catch((err) => {
  console.error("Failed to generate template:", err);
  process.exit(1);
});
