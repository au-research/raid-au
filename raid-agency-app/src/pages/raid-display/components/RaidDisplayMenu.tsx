import { InviteButton } from "@/containers/invite/InviteButton";
import { InviteDialog } from "@/containers/invite/InviteDialog";
import {
  Edit as EditIcon,
  KeyboardArrowUp as KeyboardArrowUpIcon,
} from "@mui/icons-material";
import { Fab, Stack, Tooltip } from "@mui/material";
import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";

export const RaidDisplayMenu = ({
  prefix,
  suffix,
  title,
  version,
}: {
  prefix: string;
  suffix: string;
  title: string;
  version?: string;
}) => {
  const [isInviteDialogOpen, setIsInviteDialogOpen] = useState(false);
  return (
    <>
      <Stack
        gap={1}
        sx={{ position: "fixed", bottom: "16px", right: "16px", zIndex: 1000 }}
        alignItems="end"
      >
        <Tooltip title="Scroll to top" placement="left">
          <Fab
            color="primary"
            size="small"
            onClick={() => {
              window.scrollTo({
                top: 0,
                behavior: "smooth",
              });
            }}
          >
            <KeyboardArrowUpIcon />
          </Fab>
        </Tooltip>
        {version ? (
          <Tooltip title={`Back to RAiD history`} placement="left">
            <Fab
              variant="extended"
              color="primary"
              component={Link}
              to={`/raids/${prefix}/${suffix}/history`}
              data-testid="edit-raid-button"
            >
              <ArrowLeft style={{ marginRight: '8px' }} />
                Back
            </Fab>
          </Tooltip>
        ) : 
          <Tooltip title="Edit RAiD" placement="left">  
            <Fab
              variant="extended"
              color="primary"
              component={Link}
              to={`/raids/${prefix}/${suffix}/edit`}
              data-testid="edit-raid-button"
            >
              <EditIcon sx={{ mr: 1 }} />
              Edit
            </Fab>
          </Tooltip>
        }
        {/* <InviteButton setOpen={setIsInviteDialogOpen} /> */}
        <InviteDialog
          title={title}
          open={isInviteDialogOpen}
          setOpen={setIsInviteDialogOpen}
        />
      </Stack>
    </>
  );
};
