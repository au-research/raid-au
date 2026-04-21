import { Box, ButtonBase, Stack, Typography } from "@mui/material";
import { Download as DownloadIcon, InfoOutlined as InfoIcon, TableChart as ExcelIcon, TextSnippet as CsvIcon } from "@mui/icons-material";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
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

interface TemplateButtonProps {
  label: string;
  extension: string;
  icon: React.ReactNode;
  accentColor: string;
  onClick: () => void;
}

function TemplateButton({ label, extension, icon, accentColor, onClick }: TemplateButtonProps) {
  return (
    <ButtonBase
      onClick={onClick}
      sx={{
        flex: 1,
        borderRadius: 2,
        border: "1px solid",
        borderColor: "divider",
        p: 2,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 1,
        transition: "all 0.15s ease",
        "&:hover": {
          borderColor: accentColor,
          backgroundColor: `${accentColor}10`,
          "& .template-icon": { transform: "scale(1.1)" },
        },
      }}
    >
      {/* File type icon */}
      <Box
        className="template-icon"
        sx={{
          color: accentColor,
          display: "flex",
          transition: "transform 0.15s ease",
          fontSize: 40,
          "& svg": { fontSize: "inherit" },
        }}
      >
        {icon}
      </Box>

      {/* Extension badge */}
      <Typography
        variant="caption"
        fontWeight={700}
        sx={{
          px: 1,
          py: 0.25,
          borderRadius: 1,
          backgroundColor: accentColor,
          color: "#fff",
          letterSpacing: 1,
          lineHeight: 1.6,
        }}
      >
        {extension}
      </Typography>

      {/* Label */}
      <Stack direction="row" alignItems="center" gap={0.5}>
        <DownloadIcon sx={{ fontSize: 14, color: "text.secondary" }} />
        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>
      </Stack>
    </ButtonBase>
  );
}

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
    <Stack spacing={1}>
      <Stack direction="row" spacing={2}>
        <TemplateButton
          label="Download Template"
          extension=".XLSX"
          icon={<ExcelIcon />}
          accentColor="#1D6F42"
          onClick={downloadExcel}
        />
        <TemplateButton
          label="Download Template"
          extension=".CSV"
          icon={<CsvIcon />}
          accentColor="#1565C0"
          onClick={downloadCsv}
        />
      </Stack>

      <Stack direction="row" alignItems="center" gap={0.5}>
        <CustomStyledTooltip
          title="Multiple Categories"
          content="To enter multiple categories for a single Related Object, separate them with commas in the CSV file"
          placement="top"
          tooltipIcon={<InfoIcon sx={{ fontSize: 18 }} />}
        />
        <Typography variant="body2" color="text.secondary">
          To enter multiple categories for a single Related Object, separate them with commas in the CSV file
        </Typography>
      </Stack>
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
