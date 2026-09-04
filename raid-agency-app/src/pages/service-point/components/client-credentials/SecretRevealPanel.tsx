import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import {
  ContentCopy as ContentCopyIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
} from "@mui/icons-material";
import { useSnackbar } from "@/components/snackbar";
import { copyToClipboardWithNotification } from "@/utils/copy-utils/copyWithNotify";
import type { ClientCredentialSecret } from "@/services/client-credentials";

const MASKED_VALUE = "•".repeat(24);

// Client ID is shown in plaintext here, not masked - the table already lists
// it in plaintext (RAID-826 comment thread), so masking it here would only
// add friction without hiding anything. Only the secret is sensitive.
function MaskableField({ label, value, maskable = true }: { label: string; value: string; maskable?: boolean }) {
  const [revealed, setRevealed] = useState(!maskable);
  const snackbar = useSnackbar();

  const handleCopy = () => copyToClipboardWithNotification(value, `${label} copied to clipboard`, snackbar);

  return (
    <TextField
      label={label}
      value={revealed ? value : MASKED_VALUE}
      fullWidth
      size="small"
      InputProps={{
        readOnly: true,
        sx: { fontFamily: "monospace" },
        endAdornment: (
          <InputAdornment position="end">
            {maskable && (
              <Tooltip title={revealed ? `Hide ${label.toLowerCase()}` : `Reveal ${label.toLowerCase()}`}>
                <IconButton
                  edge="end"
                  size="small"
                  aria-label={revealed ? `Hide ${label.toLowerCase()}` : `Reveal ${label.toLowerCase()}`}
                  onClick={() => setRevealed((prev) => !prev)}
                >
                  {revealed ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                </IconButton>
              </Tooltip>
            )}
            <Tooltip title="Copy to clipboard">
              <IconButton edge="end" size="small" aria-label={`Copy ${label.toLowerCase()}`} onClick={handleCopy}>
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </InputAdornment>
        ),
      }}
    />
  );
}

export function SecretRevealPanel({
  credential,
  heading,
  onClose,
}: {
  credential: ClientCredentialSecret;
  heading: string;
  onClose: () => void;
}) {
  return (
    <Alert severity="warning" variant="outlined">
      <Stack gap={1.5}>
        <Typography variant="subtitle2">{heading}</Typography>
        <MaskableField label="Client ID" value={credential.clientId} maskable={false} />
        <MaskableField label="Secret" value={credential.secret} />
        <Typography variant="body2">
          Store this secret securely — you won't be able to view it here again without revealing it.
        </Typography>
        <Box>
          <Button size="small" onClick={onClose}>
            Done
          </Button>
        </Box>
      </Stack>
    </Alert>
  );
}
