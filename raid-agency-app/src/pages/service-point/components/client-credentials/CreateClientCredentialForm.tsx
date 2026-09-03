import { useState } from "react";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { SquarePen } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createClientCredential, ClientCredentialApiError, ClientCredentialSecret, ClientCredential } from "@/services/client-credentials";
import { messages } from "@/constants/messages";

const CREDENTIAL_LIMIT = 10;

export function CreateClientCredentialForm({
  groupId,
  token,
  activeCount,
  onCreated,
}: {
  groupId: string;
  token: string | undefined;
  activeCount: number;
  onCreated: (secret: ClientCredentialSecret) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const [label, setLabel] = useState("");
  const atCap = activeCount >= CREDENTIAL_LIMIT;
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: createClientCredential,
    onSuccess: (secret) => {
      setLabel("");
      setExpanded(false);

      // The create response already has everything a list row needs, so the
      // new credential is inserted directly into the cache rather than
      // triggering a refetch - which would just be a slower round-trip to
      // fetch data we already have, and risks racing back over this update
      // with a response that started before the create did.
      const { secret: _secret, ...credential } = secret;
      const newCredential: ClientCredential = { ...credential, enabled: true };
      queryClient.setQueryData<ClientCredential[]>(
        ["clientCredentials", groupId],
        (existing) => [newCredential, ...(existing ?? [])]
      );

      onCreated(secret);
    },
  });

  const handleCreate = () => {
    if (!label.trim() || atCap) return;
    createMutation.mutate({ groupId, label: label.trim(), token });
  };

  const isLimitError = createMutation.error instanceof ClientCredentialApiError && createMutation.error.status === 409;

  return (
    <Accordion disableGutters expanded={expanded} onChange={(_, isExpanded) => setExpanded(isExpanded)}>
      <AccordionSummary expandIcon={<ExpandMoreIcon />} aria-controls="create-client-credential-content" id="create-client-credential-header">
        <SquarePen size={19} />
        <Typography sx={{ ml: 1 }} component="span">
          Create client credential
        </Typography>
      </AccordionSummary>
      <AccordionDetails>
        <Stack gap={2}>
          {atCap && (
            <Alert severity="warning">{messages.clientCredentialLimitReached(activeCount)}</Alert>
          )}
          {isLimitError && (
            // The server's message is authoritative here - the client's activeCount can be
            // stale when this 409 comes from a race (e.g. another tab creating concurrently).
            <Alert severity="error">{(createMutation.error as ClientCredentialApiError).message}</Alert>
          )}
          {!atCap && createMutation.isError && !isLimitError && (
            <Alert severity="error">Failed to create client credential. Please try again.</Alert>
          )}
          <Stack direction="row" gap={2} alignItems="flex-start">
            <TextField
              label="Label"
              placeholder="e.g. rspace-integration"
              variant="outlined"
              size="small"
              fullWidth
              value={label}
              onChange={(e) => setLabel(e.target.value)}
              disabled={atCap || createMutation.isPending}
            />
            <Box>
              <Button
                variant="outlined"
                onClick={handleCreate}
                disabled={atCap || !label.trim() || createMutation.isPending}
              >
                {createMutation.isPending ? <CircularProgress size={20} /> : "Create"}
              </Button>
            </Box>
          </Stack>
        </Stack>
      </AccordionDetails>
    </Accordion>
  );
}
