import { useCallback, useRef, useState } from "react";
import { Box, Typography } from "@mui/material";
import { CloudUpload as CloudUploadIcon } from "@mui/icons-material";

const ACCEPTED_TYPES = [
  "text/csv",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/vnd.ms-excel",
];

const ACCEPTED_EXTENSIONS = [".csv", ".xlsx", ".xls"];

interface FileDropZoneProps {
  onFileSelected: (file: File) => void;
  disabled?: boolean;
  currentFileName?: string | null;
}

export function FileDropZone({
  onFileSelected,
  disabled = false,
  currentFileName,
}: FileDropZoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isValidFile = (file: File): boolean => {
    const extension = `.${file.name.split(".").pop()?.toLowerCase()}`;
    return (
      ACCEPTED_TYPES.includes(file.type) ||
      ACCEPTED_EXTENSIONS.includes(extension)
    );
  };

  const handleFile = useCallback(
    (file: File) => {
      if (disabled) return;

      if (!isValidFile(file)) {
        // Could surface this via a snackbar or inline error
        console.warn("Invalid file type:", file.type, file.name);
        return;
      }

      onFileSelected(file);
    },
    [disabled, onFileSelected]
  );

  const handleDragOver = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      if (!disabled) setIsDragging(true);
    },
    [disabled]
  );

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragging(false);

      const file = e.dataTransfer.files?.[0];
      if (file) handleFile(file);
    },
    [handleFile]
  );

  const handleClick = useCallback(() => {
    if (!disabled) fileInputRef.current?.click();
  }, [disabled]);

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) handleFile(file);

      // Reset so the same file can be re-selected
      e.target.value = "";
    },
    [handleFile]
  );

  return (
    <Box
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      onClick={handleClick}
      sx={{
        border: "2px dashed",
        borderColor: isDragging
          ? "primary.main"
          : disabled
            ? "action.disabled"
            : "divider",
        borderRadius: 2,
        p: 3,
        textAlign: "center",
        cursor: disabled ? "not-allowed" : "pointer",
        backgroundColor: isDragging
          ? "action.hover"
          : "background.paper",
        transition: "all 0.2s ease",
        opacity: disabled ? 0.5 : 1,
        "&:hover": disabled
          ? {}
          : {
              borderColor: "primary.light",
              backgroundColor: "action.hover",
            },
      }}
    >
      <input
        ref={fileInputRef}
        type="file"
        accept={ACCEPTED_EXTENSIONS.join(",")}
        onChange={handleInputChange}
        style={{ display: "none" }}
      />

      <CloudUploadIcon
        sx={{
          fontSize: 40,
          color: isDragging ? "primary.main" : "action.active",
          mb: 1,
        }}
      />

      <Typography variant="body1" gutterBottom>
        {isDragging
          ? "Drop your file here"
          : "Drag & drop your file here, or click to browse"}
      </Typography>

      <Typography variant="caption" color="text.secondary">
        Accepted formats: .xlsx, .xls, .csv
      </Typography>

      {currentFileName && (
        <Typography
          variant="body2"
          color="primary"
          sx={{ mt: 1, fontWeight: 500 }}
        >
          Selected: {currentFileName}
        </Typography>
      )}
    </Box>
  );
}
