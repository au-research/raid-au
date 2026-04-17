import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  Typography,
} from "@mui/material";
import { CloudUpload as UploadIcon } from "@mui/icons-material";

import { useBulkUpload } from "../hooks/useBulkUpload";
import type { ParsedRelatedObject } from "../hooks/useBulkUpload";
import { FileDropZone } from "./Filedropzone";
import { TemplateDownloader } from "./TemplateDownloader";
import { ValidationErrorDisplay } from "./Validationerrordisplay";

interface BulkUploadComponentProps {
  /** The same function used by the manual add form to persist a related object. */
  addRelatedObject: (obj: ParsedRelatedObject) => Promise<void>;
}

export function BulkUploadComponent({
  addRelatedObject,
}: BulkUploadComponentProps) {
  const {
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
  } = useBulkUpload();

  return (
    <Box sx={{ mt: 2 }}>
      {/* ---- Step 1: Template downloads ---- */}
      <Stack spacing={2}>
        <Typography variant="body2" color="text.secondary">
          Download a template, fill in your related objects, then upload the
          completed file.
        </Typography>

        <TemplateDownloader />

        {/* ---- Step 2: File drop zone ---- */}
        <FileDropZone
          onFileSelected={handleFileUpload}
          disabled={status === "submitting"}
          currentFileName={file?.name ?? null}
        />

        {/* ---- Loading indicator ---- */}
        {isUploading && (
          <Stack direction="row" alignItems="center" spacing={1}>
            <CircularProgress size={18} />
            <Typography variant="body2" color="text.secondary">
              {status === "parsing"
                ? "Parsing file…"
                : "Validating rows…"}
            </Typography>
          </Stack>
        )}

        {/* ---- Validation errors ---- */}
        <ValidationErrorDisplay errors={errors} />

        {/* ---- Success summary ---- */}
        {status === "valid" && (
          <Alert severity="success">
            {parsedRows.length} related object
            {parsedRows.length !== 1 ? "s" : ""} ready to upload.
          </Alert>
        )}

        {/* ---- Submission error ---- */}
        {submissionError && (
          <Alert severity="error">{submissionError}</Alert>
        )}

        {/* ---- Done ---- */}
        {status === "done" && (
          <Alert severity="success">
            All related objects have been added successfully.
          </Alert>
        )}

        {/* ---- Action buttons ---- */}
        <Stack direction="row" spacing={1} justifyContent="flex-end">
          {(status !== "idle" && status !== "done") && (
            <Button
              variant="outlined"
              size="small"
              onClick={reset}
              disabled={status === "submitting"}
            >
              Clear
            </Button>
          )}

          {status === "done" && (
            <Button variant="outlined" size="small" onClick={reset}>
              Upload more
            </Button>
          )}

          <Button
            variant="contained"
            size="small"
            startIcon={
              status === "submitting" ? (
                <CircularProgress size={16} color="inherit" />
              ) : (
                <UploadIcon />
              )
            }
            disabled={isConfirmDisabled || status === "submitting"}
            onClick={() => handleConfirm(addRelatedObject)}
          >
            {status === "submitting"
              ? "Uploading…"
              : "Confirm upload"}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}