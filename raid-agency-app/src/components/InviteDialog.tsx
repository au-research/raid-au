import React, { useState } from "react";
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
import { useParams } from "react-router-dom";

async function sendInvite({
  email,
  handle,
}: {
  email: string;
  handle: string;
}) {
  const response = await fetch("https://orcid.test.raid.org.au/invite", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      email,
      handle,
    }),
  });
  return await response.json();
}

export default function InviteDialog({
  handle,
  open,
  setOpen,
}: {
  handle: string;
  open: boolean;
  setOpen: (open: boolean) => void;
}) {
  const { prefix, suffix } = useParams();
  const [email, setEmail] = useState("@ardc-raid.testinator.com ");
  const [role, setRole] = useState("raid-user");

  const sendInviteMutation = useMutation({
    mutationFn: sendInvite,
    onSuccess: (data) => {
      console.log("success", data);
      alert(`Success, invitation sent: ID ${data.stateUuid}`)
    },
    onError: (error) => {
      console.log(error);
    },
  });

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    console.log("Form submitted with values:", { email, role });
    sendInviteMutation.mutate({
      email,
      handle: `${prefix}/${suffix}`,
    });
    // Here you can add your logic to handle the form submission
    // For example, sending the data to an API
    // https://orcid.test.raid.org.au/invite
    setOpen(false);
  };

  return (
    <React.Fragment>
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">Invite user to RAiD</DialogTitle>
        <DialogContent>
          <form onSubmit={handleSubmit}>
            <Stack gap={2}>
              <TextField
                label="Invitee's Email"
                size="small"
                variant="filled"
                fullWidth
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <FormControl>
                <FormLabel id="demo-radio-buttons-group-label">
                  Invitee's role
                </FormLabel>
                <RadioGroup
                  aria-labelledby="demo-radio-buttons-group-label"
                  value={role}
                  onChange={(e) => setRole(e.target.value)}
                  name="radio-buttons-group"
                >
                  <FormControlLabel
                    value="raid-user"
                    control={<Radio />}
                    label="RAiD User"
                  />
                  <FormControlLabel
                    value="raid-admin"
                    control={<Radio />}
                    label="RAiD Admin"
                  />
                </RadioGroup>
              </FormControl>
            </Stack>
            <DialogActions>
              <Button onClick={() => setOpen(false)}>Cancel</Button>
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
