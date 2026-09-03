import { useState } from "react";
import {
  Chip,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from "@mui/material";
import {
  Visibility as VisibilityIcon,
  Autorenew as AutorenewIcon,
  DeleteOutline as DeleteOutlineIcon,
} from "@mui/icons-material";
import CustomizedDialogs from "@/components/alert-dialog/alert-dialog";
import { useSnackbar } from "@/components/snackbar";
import type { ClientCredential, ClientCredentialSecret } from "@/services/client-credentials";
import {
  useRotateClientCredential,
  useRevokeClientCredential,
  useViewClientCredentialSecret,
} from "./useClientCredentialMutations";

const CLIENT_ID_VISIBLE_CHARS = 16;

function truncateClientId(clientId: string): string {
  return clientId.length > CLIENT_ID_VISIBLE_CHARS
    ? `${clientId.slice(0, CLIENT_ID_VISIBLE_CHARS)}…`
    : clientId;
}

export function ClientCredentialsTable({
  credentials,
  groupId,
  token,
  onSecretRevealed,
}: {
  credentials: ClientCredential[];
  groupId: string;
  token: string | undefined;
  onSecretRevealed: (secret: ClientCredentialSecret, action: "viewed" | "rotated") => void;
}) {
  const snackbar = useSnackbar();
  const [revokeTarget, setRevokeTarget] = useState<ClientCredential | null>(null);

  const viewSecretMutation = useViewClientCredentialSecret();
  const rotateMutation = useRotateClientCredential(groupId, snackbar);
  const revokeMutation = useRevokeClientCredential(groupId, snackbar);

  const handleView = (credential: ClientCredential) => {
    viewSecretMutation.mutate(
      { clientId: credential.clientId, token },
      { onSuccess: (secret) => onSecretRevealed(secret, "viewed") }
    );
  };

  const handleRotate = (credential: ClientCredential) => {
    rotateMutation.mutate(
      { clientId: credential.clientId, token },
      { onSuccess: (secret) => onSecretRevealed(secret, "rotated") }
    );
  };

  const handleConfirmRevoke = () => {
    if (!revokeTarget) return;
    revokeMutation.mutate({ clientId: revokeTarget.clientId, token });
    setRevokeTarget(null);
  };

  return (
    <TableContainer sx={{ overflowX: "auto" }}>
      <Table size="small" sx={{ minWidth: 640 }}>
        <TableHead>
          <TableRow>
            <TableCell>Label</TableCell>
            <TableCell>Client ID</TableCell>
            <TableCell>Created</TableCell>
            <TableCell>Last rotated</TableCell>
            <TableCell>Status</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {credentials.map((credential) => {
            const disabled = !credential.enabled;
            return (
              <TableRow key={credential.clientId} sx={{ opacity: disabled ? 0.55 : 1 }}>
                <TableCell sx={{ whiteSpace: "nowrap" }}>{credential.label}</TableCell>
                <TableCell sx={{ whiteSpace: "nowrap" }}>
                  <Tooltip title={credential.clientId}>
                    <Typography variant="body2" sx={{ fontFamily: "monospace" }}>
                      {truncateClientId(credential.clientId)}
                    </Typography>
                  </Tooltip>
                </TableCell>
                <TableCell sx={{ whiteSpace: "nowrap" }}>{new Date(credential.createdAt).toLocaleString("en-AU")}</TableCell>
                <TableCell sx={{ whiteSpace: "nowrap" }}>
                  {credential.lastRotatedAt ? new Date(credential.lastRotatedAt).toLocaleString("en-AU") : "—"}
                </TableCell>
                <TableCell sx={{ whiteSpace: "nowrap" }}>
                  <Chip
                    label={credential.enabled ? "Enabled" : "Revoked"}
                    size="small"
                    color={credential.enabled ? "success" : "default"}
                  />
                </TableCell>
                <TableCell align="right" sx={{ whiteSpace: "nowrap" }}>
                  <Tooltip title={disabled ? "Unavailable — revoked" : "View secret"}>
                    <span>
                      <IconButton
                        size="small"
                        aria-label="view secret"
                        disabled={disabled}
                        onClick={() => handleView(credential)}
                      >
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title={disabled ? "Unavailable — revoked" : "Rotate secret"}>
                    <span>
                      <IconButton
                        size="small"
                        aria-label="rotate secret"
                        disabled={disabled}
                        onClick={() => handleRotate(credential)}
                      >
                        <AutorenewIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                  <Tooltip title={disabled ? "Already revoked" : "Revoke"}>
                    <span>
                      <IconButton
                        size="small"
                        color="error"
                        aria-label="revoke"
                        disabled={disabled}
                        onClick={() => setRevokeTarget(credential)}
                      >
                        <DeleteOutlineIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>

      <CustomizedDialogs
        modalTitle="Revoke client credential"
        modalContent={`Revoke "${revokeTarget?.label ?? ""}"? Any system using it will lose access immediately. This can't be undone.`}
        alertOpen={!!revokeTarget}
        onClose={() => setRevokeTarget(null)}
        modalAction={true}
        modalActions={[
          {
            label: "Cancel",
            onClick: () => setRevokeTarget(null),
            icon: DeleteOutlineIcon,
            bgColor: "grey.500",
          },
          {
            label: "Revoke",
            onClick: handleConfirmRevoke,
            icon: DeleteOutlineIcon,
            bgColor: "error.main",
          },
        ]}
      />
    </TableContainer>
  );
}
