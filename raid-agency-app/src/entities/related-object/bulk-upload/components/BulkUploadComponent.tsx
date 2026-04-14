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
import { useBulkUploadVocabulary } from "../hooks/useBulkuploadvocabulary";
import type { BulkUploadVocabulary } from "../types";
import { FileDropZone } from "./Filedropzone";
import { TemplateDownloader } from "./TemplateDownloader";
import { ValidationErrorDisplay } from "./Validationerrordisplay";

import rawVocabulary from "@/mapping/data/general-mapping.json";

interface BulkUploadComponentProps {
  /** Optional override — if not provided, the component loads the vocab itself. */
  vocabulary?: BulkUploadVocabulary;
  /** The same function used by the manual add form to persist one related object. */
  addRelatedObject: (obj: ParsedRelatedObject) => Promise<void>;
  /**
   * Optional: the same data generator the manual add flow uses (e.g.
   * `relatedObjectDataGenerator`). Parsed rows are merged on top of the
   * generator's output so react-hook-form gets every field it expects.
   */
  generator?: () => Partial<ParsedRelatedObject>;
}

export function BulkUploadComponent({
  vocabulary: vocabularyOverride,
  addRelatedObject,
  generator,
}: BulkUploadComponentProps) {
  // Load from the shared vocab file if no override is provided
  const loadedVocabulary = useBulkUploadVocabulary(rawVocabulary);
  const vocabulary = vocabularyOverride ?? loadedVocabulary;

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
    isVocabularyReady,
  } = useBulkUpload(vocabulary, { generator });

  // Vocabulary is still loading — show a spinner instead of the form
  if (!isVocabularyReady || !vocabulary) {
    return (
      <Box sx={{ mt: 2, display: "flex", alignItems: "center", gap: 1 }}>
        <CircularProgress size={18} />
        <Typography variant="body2" color="text.secondary">
          Loading vocabulary…
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ mt: 2 }}>
      <Stack spacing={2}>
        <Typography variant="body2" color="text.secondary">
          Download a template, fill in your related objects, then upload the
          completed file.
        </Typography>

        <TemplateDownloader />

        <FileDropZone
          onFileSelected={handleFileUpload}
          disabled={status === "submitting"}
          currentFileName={file?.name ?? null}
        />

        {isUploading && (
          <Stack direction="row" alignItems="center" spacing={1}>
            <CircularProgress size={18} />
            <Typography variant="body2" color="text.secondary">
              {status === "parsing" ? "Parsing file…" : "Validating rows…"}
            </Typography>
          </Stack>
        )}

        <ValidationErrorDisplay errors={errors} />

        {status === "valid" && (
          <Alert severity="success">
            {parsedRows.length} related object
            {parsedRows.length !== 1 ? "s" : ""} ready to upload.
          </Alert>
        )}

        {submissionError && <Alert severity="error">{submissionError}</Alert>}

        {status === "done" && (
          <Alert severity="success">
            All related objects have been added successfully.
          </Alert>
        )}

        <Stack direction="row" spacing={1} justifyContent="flex-end">
          {status !== "idle" && status !== "done" && (
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
            {status === "submitting" ? "Uploading…" : "Confirm upload"}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}