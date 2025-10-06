import {
  fetchCurrentUserKeycloakGroups,
  setKeycloakUserAttribute,
} from "@/services/keycloak-groups";
import { KeycloakGroup } from "@/types";
import { Circle as CircleIcon } from "@mui/icons-material";
import { Box, Tooltip, Typography } from "@mui/material";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import { useQuery } from "@tanstack/react-query";
import * as React from "react";

import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Loading } from "@/pages/loading";
import { ErrorAlertComponent } from "../../../components/error-alert-component";

export function ServicePointSwitcher() {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const open = Boolean(anchorEl);
  const handleClickListItem = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuItemClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(null);
    switchToNewServicePoint(event.currentTarget.id);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const { token, tokenParsed } = useKeycloak();

  const switchToNewServicePoint = async (groupId: string) => {
    await setKeycloakUserAttribute({
      token: token,
      groupId: groupId,
    });

    setTimeout(() => {
      window.location.reload();
    }, 250);
  };

  const keycloakGroupsQuery = useQuery<KeycloakGroup[]>({
    queryKey: ["keycloak-groups"],
    queryFn: async () => {
      const servicePoints = await fetchCurrentUserKeycloakGroups({
        token: token!,
      });
      return servicePoints;
    },
  });

  if (keycloakGroupsQuery.isLoading) {
    return <Loading />;
  }

  if (keycloakGroupsQuery.isError) {
    return <ErrorAlertComponent error="Keycloak groups could not be fetched" />
  }

  const servicePointGroups = keycloakGroupsQuery.data?.sort((a, b) =>
    a.name.localeCompare(b.name)
  );

  return (
    <Box>
      <Tooltip title="Select service point" arrow>
        <List
          component="nav"
          aria-label="service point switcher"
          sx={{ bgcolor: "background.paper", borderRadius: 1 }}
        >
          <ListItemButton
            id="lock-button"
            aria-haspopup="listbox"
            aria-controls="lock-menu"
            aria-label="select service point"
            aria-expanded={open ? "true" : undefined}
            onClick={handleClickListItem}
            sx={{
              color: "text.secondary",
            }}
          >
            <CircleIcon
              sx={{
                color: "success.main",
                fontSize: 8,
                mr: 1,
              }}
            />

            <Typography
              sx={{
                display: { xs: "none", md: "block" },
              }}
            >
              {
                servicePointGroups?.find(
                  (el) => el.id === tokenParsed?.service_point_group_id
                )?.name
              }
            </Typography>

            <Typography
              sx={{
                display: { xs: "block", md: "none" },
              }}
            >
              SP
            </Typography>
          </ListItemButton>
        </List>
      </Tooltip>
      {(servicePointGroups?.length ?? 0) > 1 && (
        <Menu
          id="sp-menu"
          anchorEl={anchorEl}
          open={open}
          onClose={handleClose}
          MenuListProps={{
            "aria-labelledby": "lock-button",
            role: "listbox",
          }}
        >
          {servicePointGroups?.map((el) => (
            <MenuItem
              key={el.id}
              id={el.id}
              disabled={el.id === tokenParsed?.service_point_group_id}
              selected={el.id === tokenParsed?.service_point_group_id}
              onClick={(event) => handleMenuItemClick(event)}
            >
              <CircleIcon
                sx={{
                  color: "success.main",
                  fontSize: 8,
                  mr: 1,
                }}
              />
              {el.name}
            </MenuItem>
          ))}
        </Menu>
      )}
    </Box>
  );
}
