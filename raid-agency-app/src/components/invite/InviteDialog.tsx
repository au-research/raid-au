import { useSnackbar } from "@/components/snackbar";
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import React, { useCallback, useState } from "react";
import { useParams } from "react-router-dom";

import { sendInvite } from "@/services/invite";
import { useKeycloak } from "@/contexts/keycloak-context";

export default function InviteDialog({
  open,
  setOpen,
}: {
  open: boolean;
  setOpen: (open: boolean) => void;
}) {
  const { prefix, suffix } = useParams();
  const [email, setEmail] = useState("john.doe@ardc-raid.testinator.com");
  const snackbar = useSnackbar();
  const { token } = useKeycloak();

  const sendInviteMutation = useMutation({
    mutationFn: sendInvite,
    onSuccess: (data) => {
      snackbar.openSnackbar(`✅ Thank you, invite has been sent.`);
    },
    onError: (error) => {
      snackbar.openSnackbar(`❌ An error occurred.`);
    },
  });

  const resetForm = useCallback(() => {
    setEmail("@ardc-raid.testinator.com");
  }, []);

  const handleClose = useCallback(() => {
    setOpen(false);
    resetForm();
  }, [setOpen, resetForm]);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    sendInviteMutation.mutate({
      email,
      handle: `${prefix}/${suffix}`,
      token: token!,
    });
    handleClose();
  };

  return (
    <React.Fragment>
      <Dialog
        open={open}
        onClose={handleClose}
        aria-labelledby="invite-dialog-title"
        aria-describedby="invite-dialog-description"
        maxWidth="md"
        PaperProps={{
          style: {
            width: "600px",
            maxWidth: "90vw",
          },
        }}
      >
        <DialogTitle id="invite-dialog-title">Invite user to RAiD</DialogTitle>
        <DialogContent>
          <form onSubmit={handleSubmit}>
            <Stack gap={2}>
              <TextField
                label="Invitee's Email"
                size="small"
                variant="filled"
                type="email"
                required
                fullWidth
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </Stack>
            <DialogActions>
              <Button onClick={handleClose}>Cancel</Button>
              <Button type="submit" autoFocus>
                Invite now
              </Button>
            </DialogActions>
          </form>
        </DialogContent>
      </Dialog>
    </React.Fragment>
  );
}
