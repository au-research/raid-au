import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  Typography,
} from "@mui/material";
import {
  CloudUpload as UploadIcon,
  RestartAlt as RestartIcon,
} from "@mui/icons-material";

import { useBulkUpload } from "../hooks/useBulkUpload";
import type { ParsedRelatedObject } from "../hooks/useBulkUpload";
import { useBulkUploadVocabulary } from "../hooks/useBulkuploadvocabulary";
import type { BulkUploadVocabulary } from "../types";
import { FileDropZone } from "./Filedropzone";
import { TemplateDownloader } from "./TemplateDownloader";
import { BulkUploadPreviewTable } from "./Bulkuploadpreviewtable";

// Import the RAiD vocabulary JSON. Adjust this path to wherever the
// shared vocabulary file lives in your app.
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
  const loadedVocabulary = useBulkUploadVocabulary(rawVocabulary);
  const vocabulary = vocabularyOverride ?? loadedVocabulary;

  const {
    status,
    file,
    submissionError,
    submissionProgress,
    isUploading,
    isVocabularyReady,
    editableRows,
    totalErrorCount,
    hasAnyRows,
    handleFileUpload,
    updateRow,
    removeRow,
    handleConfirm,
    reset,
    isConfirmDisabled,
  } = useBulkUpload(vocabulary, { generator });

  // ---- Vocabulary still loading ----
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

  const isSubmitting = status === "submitting";
  const isDone = status === "done";

  return (
    <Box sx={{ mt: 2 }}>
      <Stack spacing={2}>
        {/* ---- Intro + template download ---- */}
        <Typography variant="body2" color="text.secondary">
          Download a template, fill in your related objects, then upload the
          completed file.
        </Typography>

        <TemplateDownloader />

        {/* ---- Stage 1: file drop zone (shown until rows are loaded) ---- */}
        {!hasAnyRows && !isDone && (
          <FileDropZone
            onFileSelected={handleFileUpload}
            disabled={isSubmitting}
            currentFileName={file?.name ?? null}
          />
        )}

        {/* ---- Loading indicator ---- */}
        {isUploading && (
          <Stack direction="row" alignItems="center" spacing={1}>
            <CircularProgress size={18} />
            <Typography variant="body2" color="text.secondary">
              {status === "parsing" ? "Parsing file…" : "Validating rows…"}
            </Typography>
          </Stack>
        )}

        {/* ---- File-level error (parse failure, empty file, etc.) ---- */}
        {submissionError && !hasAnyRows && (
          <Alert severity="error" onClose={reset}>
            {submissionError}
          </Alert>
        )}

        {/* ---- Stage 2: preview table (shown once rows are loaded) ---- */}
        {hasAnyRows && !isDone && (
          <>
            <BulkUploadPreviewTable
              rows={editableRows}
              vocabulary={vocabulary}
              onUpdateRow={updateRow}
              onRemoveRow={removeRow}
              disabled={isSubmitting}
            />

            {totalErrorCount > 0 && (
              <Alert severity="warning">
                Fix the highlighted errors before uploading. You can edit
                cells directly in the table or remove rows you don't need.
              </Alert>
            )}

            {submissionError && (
              <Alert severity="error">{submissionError}</Alert>
            )}
          </>
        )}

        {/* ---- Stage 3: success state ---- */}
        {isDone && (
          <Alert severity="success">
            All related objects have been added successfully.
          </Alert>
        )}

        {/* ---- Action buttons ---- */}
        <Stack direction="row" spacing={1} justifyContent="flex-end">
          {hasAnyRows && !isDone && (
            <Button
              variant="outlined"
              size="small"
              startIcon={<RestartIcon />}
              onClick={reset}
              disabled={isSubmitting}
            >
              Start over
            </Button>
          )}

          {isDone && (
            <Button
              variant="outlined"
              size="small"
              startIcon={<RestartIcon />}
              onClick={reset}
            >
              Upload more
            </Button>
          )}

          {hasAnyRows && !isDone && (
            <Button
              variant="contained"
              size="small"
              startIcon={
                isSubmitting ? (
                  <CircularProgress size={16} color="inherit" />
                ) : (
                  <UploadIcon />
                )
              }
              disabled={isConfirmDisabled}
              onClick={() => handleConfirm(addRelatedObject)}
            >
              {isSubmitting && submissionProgress
                ? `Uploading ${submissionProgress.current} of ${submissionProgress.total}…`
                : isSubmitting
                  ? "Uploading…"
                  : `Confirm upload (${editableRows.length})`}
            </Button>
          )}
        </Stack>
      </Stack>
    </Box>
  );
}
