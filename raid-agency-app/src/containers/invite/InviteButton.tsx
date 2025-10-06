import { useAuthHelper } from "@/auth/keycloak/hooks/useAuthHelper";
import { PersonAdd as PersonAddIcon } from "@mui/icons-material";
import { Fab, Tooltip } from "@mui/material";
import { Dispatch, SetStateAction } from "react";

export function InviteButton({ setOpen }: { setOpen: Dispatch<SetStateAction<boolean>> }) {
  const { hasServicePointGroup, isServicePointUser } = useAuthHelper();

  return (
    <Tooltip title="Invite users to RAiD" placement="left">
      <Fab
        variant="extended"
        color="primary"
        type="button"
        data-testid="invite-button"
        disabled={!hasServicePointGroup || !isServicePointUser}
        onClick={() => setOpen(true)}
      >
        <PersonAddIcon sx={{ mr: 1 }} />
        Invite
      </Fab>
    </Tooltip>
  );
}
