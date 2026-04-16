import { Button, Stack } from "@mui/material";
import { Download as DownloadIcon } from "@mui/icons-material";
import { useSnackbar } from "@/components/snackbar/hooks/useSnackbar";

/**
 * Both templates are static files stored in `public/templates/`.
 *
 *   - related-objects-template.xlsx  → 3-column sheet with native dropdowns
 *   - related-objects-template.csv   → 3-column table with instruction
 *                                      rows, sentinel markers, and an
 *                                      inline vocabulary reference
 *
 * Keeping the templates as static files means future tweaks (column
 * reordering, instructions, comments, examples) are file edits rather
 * than code changes. To regenerate the .xlsx, run the Python script in
 * scripts/generate-bulk-upload-template.py.
 */

const EXCEL_TEMPLATE_URL = "/templates/related-objects-template.xlsx";
const CSV_TEMPLATE_URL = "/templates/related-objects-template.csv";

const EXCEL_FILENAME = "related-objects-template.xlsx";
const CSV_FILENAME = "related-objects-template.csv";

export function TemplateDownloader() {
  const { openSnackbar } = useSnackbar();

  const downloadFromPublic = async (url: string, filename: string) => {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(
          `Failed to download template: ${response.status} ${response.statusText}`
        );
      }
      const blob = await response.blob();
      triggerDownload(blob, filename);
    } catch (err) {
      console.error(`Template download failed for ${filename}:`, err);
      openSnackbar(
        `Unable to download ${filename}. Please try again or contact support.`,
        6000,
        "error"
      );
    }
  };

  const downloadExcel = () => downloadFromPublic(EXCEL_TEMPLATE_URL, EXCEL_FILENAME);
  const downloadCsv = () => downloadFromPublic(CSV_TEMPLATE_URL, CSV_FILENAME);

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