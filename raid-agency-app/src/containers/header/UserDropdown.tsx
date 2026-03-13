import { ErrorAlertComponent } from "@/components/error-alert-component";
import { SnackbarContextInterface, useSnackbar } from "@/components/snackbar";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Loading } from "@/pages/loading";
import { fetchCurrentUserKeycloakGroups } from "@/services/keycloak-groups";
import { KeycloakGroup } from "@/types";
import { copyToClipboardWithNotification } from "@/utils/copy-utils/copyWithNotify";
import {
  AccountCircle as AccountCircleIcon,
  ExitToApp as ExitToAppIcon,
  ExpandMore as ExpandMoreIcon,
} from "@mui/icons-material";
import {
  Button,
  Divider,
  IconButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  MenuList,
  Typography,
} from "@mui/material";

import { useQuery } from "@tanstack/react-query";
import { KeycloakTokenParsed } from "keycloak-js";
import React from "react";

const keycloakInternalRoles = [
  "default-roles-raid",
  "offline_access",
  "uma_authorization",
];

function getRolesFromToken({
  tokenParsed,
}: {
  tokenParsed: KeycloakTokenParsed | undefined;
}): string[] | undefined {
  return tokenParsed?.realm_access?.roles.filter(
    (el) => !keycloakInternalRoles.includes(el)
  );
}

export function UserDropdown() {
  const { isInitialized, tokenParsed, token, authenticated, logout } =
    useKeycloak();
  const snackbar = useSnackbar();

  const [accountMenuAnchor, setAccountMenuAnchor] =
    React.useState<null | HTMLElement>(null);

  const handleAccountMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAccountMenuAnchor(event.currentTarget);
  };

  const handleAccountMenuClose = () => {
    setAccountMenuAnchor(null);
  };

  const roles = getRolesFromToken({ tokenParsed: tokenParsed });

  const keycloakGroupsQuery = useQuery<KeycloakGroup[]>({
    queryKey: ["keycloak-groups"],
    queryFn: async () => {
      const servicePoints = await fetchCurrentUserKeycloakGroups({
        token: token,
      });
      return servicePoints;
    },
  });

  if (keycloakGroupsQuery.isLoading) {
    return <Loading />;
  }

  if (keycloakGroupsQuery.isError) {
    return <ErrorAlertComponent error="Keycloak groups could not be fetched" />;
  }

  return (
    <>
      {authenticated && isInitialized && (
        <div>
          {(authenticated && tokenParsed?.email && (
            <Button
              variant="outlined"
              startIcon={<AccountCircleIcon />}
              endIcon={<ExpandMoreIcon />}
              color="primary"
              onClick={handleAccountMenuOpen}
              sx={{
                textTransform: "none",
              }}
            >
              <Typography
                sx={{
                  display: { xs: "none", md: "block" },
                }}
              >
                {tokenParsed?.email}
              </Typography>
            </Button>
          )) || (
            <IconButton
              size="large"
              aria-label="account of current user"
              aria-controls="menu-appbar"
              aria-haspopup="true"
              onClick={handleAccountMenuOpen}
            >
              <AccountCircleIcon />
            </IconButton>
          )}
          <Menu
            id="menu-appbar"
            anchorEl={accountMenuAnchor}
            anchorOrigin={{
              vertical: "top",
              horizontal: "right",
            }}
            keepMounted
            transformOrigin={{
              vertical: "top",
              horizontal: "right",
            }}
            open={Boolean(accountMenuAnchor)}
            onClose={handleAccountMenuClose}
          >
            <MenuList dense>
              {tokenParsed?.email && (
                <MenuItem
                  onClick={async () => {
                      await copyToClipboardWithNotification(
                        tokenParsed?.email || "",
                        "Copied email to clipboard",
                        snackbar as SnackbarContextInterface
                      );
                    handleAccountMenuClose();
                    }
                  }
                >
                  <ListItemText
                    primary="Email"
                    secondary={tokenParsed?.email || ""}
                  />
                </MenuItem>
              )}
              <MenuItem
                onClick={async () => {
                    await copyToClipboardWithNotification(
                      tokenParsed?.sub || "",
                      "Copied identity to clipboard",
                      snackbar as SnackbarContextInterface
                    );
                  handleAccountMenuClose();
                  }
                }
              >
                <ListItemText primary="Identity" secondary={tokenParsed?.sub} />
              </MenuItem>
              <MenuItem disabled>
                <ListItemText
                  primary="Signed in (24h format)"
                  secondary={
                    tokenParsed?.iat &&
                    Intl.DateTimeFormat("en-AU", {
                      timeStyle: "short",
                      dateStyle: "medium",
                      hour12: false,
                    }).format(+tokenParsed?.iat * 1000)
                  }
                />
              </MenuItem>
              <MenuItem disabled>
                <ListItemText
                  primary="Session expiry (24h format)"
                  secondary={
                    tokenParsed?.exp &&
                    Intl.DateTimeFormat("en-AU", {
                      timeStyle: "short",
                      dateStyle: "medium",
                      hour12: false,
                    }).format(+tokenParsed?.exp * 1000)
                  }
                />
              </MenuItem>
              <Divider />
              <MenuItem>
                <ListItemText
                  primary="Roles"
                  secondary={roles?.sort().join(" | ")}
                />
              </MenuItem>
              <Divider />
              <MenuItem
                onClick={() => {
                  localStorage.removeItem("client_id");
                  logout();
                }}
              >
                <ListItemIcon>
                  <ExitToAppIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Sign out</ListItemText>
              </MenuItem>
            </MenuList>
          </Menu>
        </div>
      )}
    </>
  );
}
