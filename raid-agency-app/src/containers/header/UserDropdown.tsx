import { useAuthHelper } from "@/auth/keycloak";
import { useKeycloak } from "@/contexts/keycloak-context";
import { ROUTES } from "@/constants/routes";
import {
  AccountCircle as AccountCircleIcon,
  ExitToApp as ExitToAppIcon,
  ExpandMore as ExpandMoreIcon,
  Hub as HubIcon,
  Key as KeyIcon,
  Layers as LayersIcon,
} from "@mui/icons-material";
import {
  Avatar,
  Box,
  Button,
  Divider,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  MenuList,
  Tooltip,
  Typography,
} from "@mui/material";
import React from "react";
import { useNavigate } from "react-router-dom";

function getInitials(firstName?: string, lastName?: string, email?: string): string {
  if (firstName && lastName) {
    return `${firstName[0]}${lastName[0]}`.toUpperCase();
  }
  return email ? email[0].toUpperCase() : "?";
}

export function UserDropdown() {
  const { isInitialized, tokenParsed, authenticated, logout, user } = useKeycloak();
  const { isOperator, isGroupAdmin, hasServicePointGroup, isServicePointUser } = useAuthHelper();
  const navigate = useNavigate();

  const [accountMenuAnchor, setAccountMenuAnchor] = React.useState<null | HTMLElement>(null);

  const handleAccountMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAccountMenuAnchor(event.currentTarget);
  };

  const handleAccountMenuClose = () => {
    setAccountMenuAnchor(null);
  };

  const goTo = (path: string) => {
    handleAccountMenuClose();
    navigate(path);
  };

  if (!authenticated || !isInitialized) {
    return null;
  }

  const canManageServicePoints = isOperator || isGroupAdmin;

  const statusLabel = isOperator
    ? "Operator"
    : hasServicePointGroup && isServicePointUser
      ? "Active"
      : hasServicePointGroup && !isServicePointUser
        ? "Access Pending"
        : "No Service Point";

  const displayName =
    user?.firstName && user?.lastName
      ? `${user.firstName} ${user.lastName}`
      : tokenParsed?.email || "";

  // Federated SSO (SATOSA/eduGAIN) doesn't always release an email or
  // name claim - with neither, there's nothing meaningful to show as
  // initials or a label, so fall back to a plain account icon instead
  // of a bare "?" avatar with empty text next to it.
  const hasIdentity = Boolean((user?.firstName && user?.lastName) || tokenParsed?.email);

  return (
    <div>
      <Button
        variant="outlined"
        endIcon={<ExpandMoreIcon />}
        color="primary"
        onClick={handleAccountMenuOpen}
        sx={{
          textTransform: "none",
          pl: 0.5,
          borderRadius: "8px",
          // MUI's default outlined-button border is the theme colour at
          // 50% opacity, which read as too faint against the mock's
          // bolder purple outline - set explicitly to full opacity, with
          // a light purple tint behind it instead of a transparent fill.
          borderColor: "primary.main",
          backgroundColor: "rgba(142, 72, 155, 0.06)",
          "&:hover": {
            backgroundColor: "rgba(142, 72, 155, 0.12)",
            borderColor: "primary.main",
          },
        }}
      >
        <Avatar sx={{ width: 28, height: 28, mr: 1, fontSize: 13, bgcolor: "primary.main" }}>
          {hasIdentity ? (
            getInitials(user?.firstName, user?.lastName, tokenParsed?.email)
          ) : (
            <AccountCircleIcon fontSize="small" />
          )}
        </Avatar>
        {tokenParsed?.email && <Typography>{tokenParsed.email}</Typography>}
      </Button>
      <Menu
        id="menu-appbar"
        anchorEl={accountMenuAnchor}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
        transformOrigin={{ vertical: "top", horizontal: "right" }}
        open={Boolean(accountMenuAnchor)}
        onClose={handleAccountMenuClose}
        PaperProps={{ sx: { borderRadius: "8px", mt: 0.5 } }}
      >
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="subtitle2" fontWeight={700}>
            {displayName}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {statusLabel}
          </Typography>
        </Box>
        <Divider />
        <MenuList dense>
          <MenuItem onClick={() => goTo(ROUTES.API_KEY)}>
            <ListItemIcon>
              <KeyIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText
              primary="API Tokens"
              secondary="Create and manage integration tokens"
            />
          </MenuItem>
          <MenuItem onClick={() => goTo(ROUTES.CACHE_MANAGER)}>
            <ListItemIcon>
              <LayersIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText
              primary="Cache Manager"
              secondary="Refresh locally cached reference data"
            />
          </MenuItem>
          <Tooltip
            title={canManageServicePoints ? "" : "Requires service point administrator role"}
            placement="left"
          >
            <span>
              <MenuItem
                disabled={!canManageServicePoints}
                onClick={() => goTo(ROUTES.SERVICE_POINTS)}
              >
                <ListItemIcon>
                  <HubIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText
                  primary="Manage Service Points"
                  secondary={canManageServicePoints ? undefined : "Requires service point administrator role"}
                />
              </MenuItem>
            </span>
          </Tooltip>
        </MenuList>
        <Divider />
        <MenuList dense>
          <MenuItem
            onClick={() => {
              localStorage.removeItem("client_id");
              logout();
            }}
          >
            <ListItemIcon>
              <ExitToAppIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText>Sign Out</ListItemText>
          </MenuItem>
        </MenuList>
      </Menu>
    </div>
  );
}
