import { Button, Stack } from "@mui/material";
import {
  Download as DownloadIcon,
} from "@mui/icons-material";

/**
 * Template column headers that match the flat structure expected
 * by the bulk upload parser (mapRowToRelatedObject).
 */
const TEMPLATE_HEADERS = [
  "id",
  "schemaUri",
  "type.id",
  "type.schemaUri",
  "category.id",
  "category.schemaUri",
];

const EXAMPLE_ROW = [
  "https://doi.org/10.25955/abc-123",
  "https://doi.org/",
  "https://vocabulary.raid.org/relatedObject.type.schema/247",
  "https://vocabulary.raid.org/relatedObject.type.schema/1",
  "https://vocabulary.raid.org/relatedObject.category.id/190",
  "https://vocabulary.raid.org/relatedObject.category.schema/1",
];

function downloadCsvTemplate() {
  const headerLine = TEMPLATE_HEADERS.join(",");
  const exampleLine = EXAMPLE_ROW.join(",");
  const csvContent = `${headerLine}\n${exampleLine}\n`;

  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  triggerDownload(blob, "related-objects-template.csv");
}

async function downloadExcelTemplate() {
  const XLSX = await import("xlsx");
  const worksheet = XLSX.utils.aoa_to_sheet([TEMPLATE_HEADERS, EXAMPLE_ROW]);

  // Set reasonable column widths
  worksheet["!cols"] = TEMPLATE_HEADERS.map(() => ({ wch: 50 }));

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, "Related Objects");

  const excelBuffer = XLSX.write(workbook, {
    bookType: "xlsx",
    type: "array",
  });

  const blob = new Blob([excelBuffer], {
    type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  });

  triggerDownload(blob, "related-objects-template.xlsx");
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

export function TemplateDownloader() {
  return (
    <Stack direction="row" spacing={1}>
      <Button
        variant="outlined"
        size="small"
        startIcon={<DownloadIcon />}
        onClick={downloadExcelTemplate}
      >
        Download Excel template
      </Button>
      <Button
        variant="outlined"
        size="small"
        startIcon={<DownloadIcon />}
        onClick={downloadCsvTemplate}
      >
        Download CSV template
      </Button>
    </Stack>
  );
}
