import {
  Alert,
  Box,
  Button,
  Stack,
  Typography,
  useTheme,
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

import { PulseLoader } from "react-spinners";
import rawVocabulary from "@/mapping/data/general-mapping.json";

interface BulkUploadComponentProps {
  /** Optional override — if not provided, the component loads the vocab itself. */
  vocabulary?: BulkUploadVocabulary;
  /** Called with all validated objects at once for a single batched append. */
  addRelatedObjects: (objs: ParsedRelatedObject[]) => Promise<void>;
  /**
   * Optional: the same data generator the manual add flow uses (e.g.
   * `relatedObjectDataGenerator`). Parsed rows are merged on top of the
   * generator's output so react-hook-form gets every field it expects.
   */
  generator?: () => Partial<ParsedRelatedObject>;
  /** Called once after all objects have been successfully appended. */
  onComplete?: () => void;
}

function BulkSpinner({ message }: { message: string }) {
  const theme = useTheme();
  return (
    <Stack spacing={1.5} alignItems="center" sx={{ py: 2 }}>
      <PulseLoader color={theme.palette.primary.main} />
      <Typography variant="body2" fontWeight={500}>
        {message}
      </Typography>
    </Stack>
  );
}

export function BulkUploadComponent({
  vocabulary: vocabularyOverride,
  addRelatedObjects,
  generator,
  onComplete,
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
  } = useBulkUpload(vocabulary, { generator, onComplete });

  // ---- Vocabulary still loading ----
  if (!isVocabularyReady || !vocabulary) {
    return <BulkSpinner message="Loading vocabulary…" />;
  }

  const isSubmitting = status === "submitting";
  const isDone = status === "done";

  return (
    <Box sx={{ mt: 2 }}>
      <Stack spacing={2}>
        {/* ---- Bulk upload UI (hidden once submission starts) ---- */}
        {!isSubmitting && !isDone && (
          <>
            {/* Intro + template download */}
            <Stack spacing={0.5}>
              {[
                "Download a template below (Excel or CSV)",
                "Fill in your Related Objects — one row per object",
                "Upload the completed file using the drop zone below",
              ].map((step, i) => (
                <Stack key={i} direction="row" alignItems="flex-start" spacing={1}>
                  <Typography variant="body2" color="primary" fontWeight={700} sx={{ minWidth: 18 }}>
                    {i + 1}.
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {step}
                  </Typography>
                </Stack>
              ))}
            </Stack>

            <TemplateDownloader />

            {/* File drop zone (shown until rows are loaded) */}
            {!hasAnyRows && (
              <FileDropZone
                onFileSelected={handleFileUpload}
                currentFileName={file?.name ?? null}
              />
            )}

            {/* Parsing / validating indicator */}
            {isUploading && (
              <BulkSpinner
                message={status === "parsing" ? "Parsing file…" : "Validating rows…"}
              />
            )}

            {/* File-level error (parse failure, empty file, missing columns, etc.) */}
            {submissionError && !hasAnyRows && (
              <Alert severity="error" onClose={reset}>
                {submissionError}
              </Alert>
            )}

            {/* Preview table */}
            {hasAnyRows && (
              <>
                <BulkUploadPreviewTable
                  rows={editableRows}
                  vocabulary={vocabulary}
                  onUpdateRow={updateRow}
                  onRemoveRow={removeRow}
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

            {/* Action buttons */}
            {hasAnyRows && (
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<RestartIcon />}
                  onClick={reset}
                >
                  Start over
                </Button>

                <Button
                  variant="contained"
                  size="small"
                  startIcon={<UploadIcon />}
                  disabled={isConfirmDisabled}
                  onClick={() => handleConfirm(addRelatedObjects)}
                >
                  {`Confirm upload (${editableRows.length})`}
                </Button>
              </Stack>
            )}
          </>
        )}

        {/* ---- Submission progress ---- */}
        {isSubmitting && (
          <BulkSpinner
            message={
              submissionProgress
                ? `Adding ${submissionProgress.total} related object${submissionProgress.total === 1 ? "" : "s"} to the form…`
                : "Uploading…"
            }
          />
        )}

        {/* ---- Success state ---- */}
        {isDone && (
          <Stack spacing={1}>
            <Alert severity="success">
              All related objects have been added successfully.
            </Alert>
            <Stack direction="row" justifyContent="flex-end">
              <Button
                variant="outlined"
                size="small"
                startIcon={<RestartIcon />}
                onClick={reset}
              >
                Upload more
              </Button>
            </Stack>
          </Stack>
        )}
      </Stack>
    </Box>
  );
}
