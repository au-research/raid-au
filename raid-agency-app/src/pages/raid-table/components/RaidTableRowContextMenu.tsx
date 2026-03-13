import { RaidDto } from "@/generated/raid";
import React from "react";

import {
  Edit as EditIcon,
  History as HistoryIcon,
  Menu as MenuIcon,
  Visibility as VisibilityIcon,
} from "@mui/icons-material";
import { SnackbarContextInterface, useSnackbar } from "@/components/snackbar";
import ContentCopy from "@mui/icons-material/ContentCopy";
import {
  IconButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
} from "@mui/material";
import Divider from "@mui/material/Divider";
import { Link } from "react-router-dom";
import { copyToClipboardWithNotification } from "@/utils/copy-utils/copyWithNotify";


export const RaidTableRowContextMenu = ({ row }: { row: RaidDto }) => {
  const [prefix, setPrefix] = React.useState<string>("");
  const [suffix, setSuffix] = React.useState<string>("");
  const snackbar = useSnackbar();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);
  const handleContextMenuClick = (
    event: React.MouseEvent<HTMLButtonElement>,
    rowData: RaidDto
  ) => {
    const identifierSplit = rowData?.identifier?.id.split("/") || [];
    const suffix = identifierSplit[identifierSplit.length - 1] || "";
    const prefix = identifierSplit[identifierSplit.length - 2] || "";

    setPrefix(prefix);
    setSuffix(suffix);
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
    setTimeout(() => {
      setPrefix("");
      setSuffix("");
    }, 500);
  };
       
  return (
    <>
      <Menu anchorEl={anchorEl} open={open} onClose={handleClose}>
        <MenuItem
          onClick={async () => {
            await copyToClipboardWithNotification(
              JSON.stringify(row, null, 2),
              "Copied raw JSON data to clipboard",
              snackbar as SnackbarContextInterface
            );
            handleClose();
            }
          }
        >
          <ListItemIcon>
            <ContentCopy fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Copy raw JSON" secondary={"RAW Data"}/>
        </MenuItem>
        <MenuItem
          onClick={async () => {
            await copyToClipboardWithNotification(
              `${suffix}`,
              "Copied Suffix to clipboard",
              snackbar as SnackbarContextInterface
            );
            handleClose();
          }}
        >
          <ListItemIcon>
            <ContentCopy fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Copy Suffix" secondary={`${suffix}`} />
        </MenuItem>
        <MenuItem
          onClick={async () => {
            await copyToClipboardWithNotification(
              `${prefix}/${suffix}`,
              "Copied Identifier to clipboard",
              snackbar as SnackbarContextInterface
            );
            handleClose();
          }}
        >
          <ListItemIcon>
            <ContentCopy fontSize="small" />
          </ListItemIcon>
          <ListItemText
            primary="Copy Identifier"
            secondary={`${prefix}/${suffix}`}
          />
        </MenuItem>
        <MenuItem
          onClick={async () => {
            await copyToClipboardWithNotification(
              `https://raid.org/${prefix}/${suffix}`,
              "Copied URL to clipboard",
              snackbar as SnackbarContextInterface
            );
            handleClose();
          }}
        >
          <ListItemIcon>
            <ContentCopy fontSize="small" />
          </ListItemIcon>
          <ListItemText
            primary="Copy URL"
            secondary={`https://raid.org/${prefix}/${suffix}`}
          />
        </MenuItem>

        <Divider />
        <MenuItem component={Link} to={`/raids/${prefix}/${suffix}`}>
          <ListItemIcon>
            <VisibilityIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Show RAiD</ListItemText>
        </MenuItem>
        <MenuItem component={Link} to={`/raids/${prefix}/${suffix}/edit`}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edit RAiD</ListItemText>
        </MenuItem>
        <Divider />
        {/* <MenuItem
          onClick={() => {
            handleClose();
            window.location.href = `https://doi.test.datacite.org/dois/${prefix}%2F${suffix}`;
          }}
        >
          <ListItemIcon>
            <OpenInNewIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText
            primary="Open in Datacite"
            secondary="Must be signed in to Fabrica to display"
          />
        </MenuItem> */}
        <MenuItem component={Link} to={`/raids/${prefix}/${suffix}/history`}>
          <ListItemIcon>
            <HistoryIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText
            primary="RAiD History"
            secondary="Show history of this RAiD"
          />
        </MenuItem>
      </Menu>
      <IconButton
        aria-label="more actions"
        onClick={(event) => handleContextMenuClick(event, row)}
      >
        <MenuIcon />
      </IconButton>
    </>
  );
};
