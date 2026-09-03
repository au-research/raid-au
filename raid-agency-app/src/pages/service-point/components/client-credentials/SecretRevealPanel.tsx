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

function MaskableField({ label, value }: { label: string; value: string }) {
  const [revealed, setRevealed] = useState(false);
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
        <MaskableField label="Client ID" value={credential.clientId} />
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
