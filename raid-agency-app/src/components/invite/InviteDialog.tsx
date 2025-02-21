import { useSnackbar } from "@/components/snackbar";
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  FormLabel,
  Radio,
  RadioGroup,
  Stack,
  TextField,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import { useCallback, useState, useMemo } from "react";
import { useParams } from "react-router-dom";
import { useKeycloak } from "@/contexts/keycloak-context";
import { sendInvite } from "@/services/invite";

export default function InviteDialog({
  title,
  open,
  setOpen,
}: {
  title: string;
  open: boolean;
  setOpen: (open: boolean) => void;
}) {
  const { prefix, suffix } = useParams();
  const { token } = useKeycloak();
  const snackbar = useSnackbar();

  const [inviteMethod, setInviteMethod] = useState("");
  const [email, setEmail] = useState("");
  const [orcid, setOrcid] = useState("");

  const isValidEmail = (email: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const isValidOrcid = (orcid: string) =>
    /^\d{4}-\d{4}-\d{4}-\d{4}$/.test(orcid);

  const isValidForm = useMemo(() => {
    if (inviteMethod === "email") {
      return isValidEmail(email) && !orcid;
    }
    if (inviteMethod === "orcid") {
      return isValidOrcid(orcid) && !email;
    }
    return false;
  }, [inviteMethod, email, orcid]);

  const sendInviteMutation = useMutation({
    mutationFn: sendInvite,
    onSuccess: () =>
      snackbar.openSnackbar("✅ Thank you, invite has been sent."),
    onError: () => snackbar.openSnackbar("❌ An error occurred."),
  });

  const handleClose = useCallback(() => {
    setOpen(false);
    setEmail("");
  }, [setOpen]);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    sendInviteMutation.mutate({
      title,
      email: inviteMethod === "email" ? email : "",
      orcid: inviteMethod === "orcid" ? orcid : "",
      handle: `${prefix}/${suffix}`,
      token: token!,
    });
    handleClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      aria-labelledby="invite-dialog-title"
      maxWidth="md"
      PaperProps={{ style: { width: "600px", maxWidth: "90vw" } }}
    >
      <DialogTitle id="invite-dialog-title">Invite user to RAiD</DialogTitle>
      <DialogContent>
        <FormControl>
          <FormLabel>Invite using</FormLabel>
          <RadioGroup
            row
            value={inviteMethod}
            onChange={(e) => {
              setInviteMethod(e.target.value);
              setEmail("");
              setOrcid("");
            }}
          >
            <FormControlLabel value="email" control={<Radio />} label="Email" />
            <FormControlLabel value="orcid" control={<Radio />} label="ORCID" />
          </RadioGroup>
        </FormControl>

        <form onSubmit={handleSubmit}>
          <Stack gap={2}>
            <TextField
              label="Invitee's Email"
              size="small"
              variant="filled"
              type="email"
              required={inviteMethod === "email"}
              fullWidth
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={inviteMethod !== "email"}
              error={inviteMethod === "email" && !isValidEmail(email)}
              helperText={
                inviteMethod === "email" && !isValidEmail(email)
                  ? "Please enter a valid email"
                  : ""
              }
            />
            <TextField
              label="Invitee's ORCID ID"
              size="small"
              variant="filled"
              type="text"
              required={inviteMethod === "orcid"}
              fullWidth
              value={orcid}
              onChange={(e) => setOrcid(e.target.value)}
              disabled={inviteMethod !== "orcid"}
              error={inviteMethod === "orcid" && !isValidOrcid(orcid)}
              helperText={inviteMethod === "orcid" ? "Format: 0000-0000-0000-0000" : ""}
            />
          </Stack>
          <DialogActions>
            <Button onClick={handleClose}>Cancel</Button>
            <Button type="submit" disabled={!isValidForm} autoFocus>
              Invite now
            </Button>
          </DialogActions>
        </form>
      </DialogContent>
    </Dialog>
  );
}
