import { Home as HomeIcon } from "@mui/icons-material";
import {
  AppBar,
  Box,
  Chip,
  IconButton,
  Stack,
  Toolbar,
  useTheme,
} from "@mui/material";
import { Link } from "react-router-dom";
import { ServicePointSwitcher } from "../../containers/header/service-point-switcher";
import { NavigationDrawer } from "../../containers/header/NavigationDrawer";
import { UserDropdown } from "../../containers/header/UserDropdown";
import { NotificationBell } from '../alert-notifications/Notifications';
import { useServicePointPendingRequest } from "@/shared/service-point/service-point-pending-request";
import { useAuthHelper } from "@/auth/keycloak";
import { useKeycloak } from '@/contexts/keycloak-context';
import React from "react";
import { useNavigate } from 'react-router-dom';
import { useAppConfig } from "@/config/Appconfigcontext";
import { MegaMenu } from "../mega-menu/mega-menu";
import Banner from "../alert-notifications/banner/Banner";
import { a } from "vitest/dist/chunks/suite.CcK46U-P.js";

const AuthenticatedNavbarContent = () => {
  const { isOperator, isGroupAdmin } = useAuthHelper();
  useServicePointPendingRequest();
  return (
    <Stack direction="row" alignItems="center" gap={1}>
      <ServicePointSwitcher />
      {isOperator || isGroupAdmin ? <NotificationBell /> : null}
      <UserDropdown />
      <Chip
        label={import.meta.env.VITE_RAIDO_ENV.toUpperCase()}
        color="error"
        size="small"
        sx={{ mr: 2 }}
      />
      <NavigationDrawer />
    </Stack>
  );
};

/**
 * Main application navigation bar
 * 
 * Provides consistent navigation across the application with authentication-aware
 * display of user controls, service point switcher, and navigation options.
 * 
 * @param {boolean} authenticated - Whether a user is currently authenticated
 * @returns {JSX.Element} Navigation bar with appropriate controls based on auth state
 */
export const AppNavBar = () => {
  const theme = useTheme();
  const { authenticated, tokenParsed, logout } = useKeycloak();
  const user = tokenParsed;
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const config = useAppConfig();
  const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogin = () => {
    navigate('/login');
  };

  const handleLogout = () => {
    handleClose();
    logout();
  };
  return (
    <>
    <AppBar
      position="sticky"
      elevation={1}
      sx={{
        backgroundColor: theme.palette.mode === "dark" ? "black" : "white",
        borderTop: "solid",
        zIndex: (theme) => theme.zIndex.drawer + 1,
      }}
      data-testid="app-nav-bar"
    >
       {!config.default && (<MegaMenu />)}
      <Toolbar variant={"dense"}>
        <Stack direction="row" alignItems="center">
          <Link to="/" style={{ lineHeight: 0 }}>
            <Box>
              <img
                src={
                  theme.palette.mode === "dark"
                    ? config.header.logo.src.replace('.svg', '-light.svg')
                    : config.header.logo.src
                }
                alt="logo"
                height={config.header.logo.height + "px"}
              />
            </Box>
          </Link>
        </Stack>
        <div style={{ flexGrow: 1 }} />
        {authenticated && <AuthenticatedNavbarContent /> }
      </Toolbar>
    </AppBar>
    {authenticated &&
      (
        <Banner
          variant="warning"
          message={
            <>
              This is not a production system. Research organisations can request access to the production system.
            </>
          }
          dismissible={false}
        />
      )}
    </>
  );
};
