import { Home as HomeIcon, Menu as MenuIcon, OpenInNew as OpenInNewIcon } from "@mui/icons-material";
import {
  AppBar,
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Toolbar,
  Typography,
  useTheme,
} from "@mui/material";
import { Link, NavLink } from "react-router-dom";
import { UserDropdown } from "../../containers/header/UserDropdown";
import { NotificationBell } from '../alert-notifications/Notifications';
import { useServicePointPendingRequest } from "@/shared/service-point/service-point-pending-request";
import { useAuthHelper } from "@/auth/keycloak";
import { useKeycloak } from '@/contexts/keycloak-context';
import React from "react";
import { useNavigate } from 'react-router-dom';
import { useAppConfig } from "@/config/Appconfigcontext";
import { useRuntimeConfig } from "@/config";
import { MegaMenu } from "../mega-menu/mega-menu";
import Banner from "../alert-notifications/banner/Banner";
import { ROUTES } from "@/constants/routes";
import { EXTERNAL_LINKS } from "@/constants/external-links";

const ABOUT_URL = EXTERNAL_LINKS.ABOUT_RAID;
const DOCUMENTATION_URL = EXTERNAL_LINKS.DOCUMENTATION;
const RAID_ORG_URL = EXTERNAL_LINKS.RAID_ORG;

const HeaderNavLinks = () => {
  const theme = useTheme();
  // AppBar's default color="primary" sets text to its contrastText
  // (white) regardless of the AppBar's backgroundColor override, which
  // is invisible against the white/black background set there - same
  // issue as the header title text, fixed the same way.
  const textColor = theme.palette.mode === "dark" ? "#fff" : theme.palette.text.primary;
  const linkSx = { color: textColor, textDecoration: "none", fontWeight: 600, fontSize: "0.9rem" };
  const navLinkStyle = ({ isActive }: { isActive: boolean }): React.CSSProperties => ({
    textDecoration: isActive ? "underline" : "none",
    textUnderlineOffset: "6px",
    color: textColor,
    fontWeight: 600,
    fontSize: "0.9rem",
  });

  return (
    <Stack direction="row" alignItems="center" gap={3} sx={{ display: { xs: "none", md: "flex" } }}>
      <NavLink to={ROUTES.HOME} style={navLinkStyle} end>
        Home
      </NavLink>
      <NavLink to={ROUTES.RAIDS} style={navLinkStyle}>
        Your RAiDs
      </NavLink>
      <Stack component="a" href={ABOUT_URL} target="_blank" rel="noopener noreferrer" direction="row" alignItems="center" gap={0.4} sx={linkSx}>
        About <OpenInNewIcon sx={{ fontSize: 16 }} />
      </Stack>
      <Stack component="a" href={DOCUMENTATION_URL} target="_blank" rel="noopener noreferrer" direction="row" alignItems="center" gap={0.4} sx={linkSx}>
        Documentation <OpenInNewIcon sx={{ fontSize: 16 }} />
      </Stack>
      <Stack component="a" href={RAID_ORG_URL} target="_blank" rel="noopener noreferrer" direction="row" alignItems="center" gap={0.4} sx={linkSx}>
        raid.org <OpenInNewIcon sx={{ fontSize: 16 }} />
      </Stack>
    </Stack>
  );
};

// HeaderNavLinks hides below the "md" breakpoint with nowhere else to
// go, since the hamburger drawer that used to hold these was removed
// (it wasn't in the design and largely duplicated the new nav links /
// user menu) - this is the mobile-only replacement, a compact menu
// button that opens the same five links.
const MobileNavMenu = () => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  return (
    <Box sx={{ display: { xs: "flex", md: "none" } }}>
      <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} aria-label="navigation menu">
        <MenuIcon />
      </IconButton>
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        <MenuItem component={NavLink} to={ROUTES.HOME} onClick={() => setAnchorEl(null)}>
          Home
        </MenuItem>
        <MenuItem component={NavLink} to={ROUTES.RAIDS} onClick={() => setAnchorEl(null)}>
          Your RAiDs
        </MenuItem>
        <MenuItem component="a" href={ABOUT_URL} target="_blank" rel="noopener noreferrer" onClick={() => setAnchorEl(null)}>
          About
        </MenuItem>
        <MenuItem component="a" href={DOCUMENTATION_URL} target="_blank" rel="noopener noreferrer" onClick={() => setAnchorEl(null)}>
          Documentation
        </MenuItem>
        <MenuItem component="a" href={RAID_ORG_URL} target="_blank" rel="noopener noreferrer" onClick={() => setAnchorEl(null)}>
          raid.org
        </MenuItem>
      </Menu>
    </Box>
  );
};

const AuthenticatedNavbarContent = () => {
  const { isOperator, isGroupAdmin } = useAuthHelper();
  const { environment } = useRuntimeConfig();
  useServicePointPendingRequest();
  return (
    <Stack direction="row" alignItems="center" gap={2}>
      <HeaderNavLinks />
      <MobileNavMenu />
      {isOperator || isGroupAdmin ? <NotificationBell /> : null}
      <Chip
        label={environment.toUpperCase()}
        color="error"
        size="small"
      />
      <UserDropdown />
    </Stack>
  );
};

/**
 * Main application navigation bar
 * 
 * Provides consistent navigation across the application with authentication-aware
 * display of user controls and navigation options.
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
  const { environment } = useRuntimeConfig();
  const isProduction = environment === 'prod';
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
        zIndex: (theme) => theme.zIndex.drawer + 1,
      }}
      data-testid="app-nav-bar"
    >
       {!config.default && (<MegaMenu />)}
      <Toolbar variant={"dense"} sx={{ minHeight: "64px" }}>
        <Stack direction="row" alignItems="center">
          <Link
            to="/"
            style={{ lineHeight: 0, textDecoration: "none", color: "inherit" }}
          >
            <Stack direction="row" alignItems="center" gap={1.5}>
              <img
                src={
                  theme.palette.mode === "dark"
                    ? config.header.logo.src.replace('.svg', '-light.svg')
                    : config.header.logo.src
                }
                alt="logo"
                width={62}
                height={34}
              />
              <Typography
                variant="subtitle1"
                sx={{
                  fontWeight: 600,
                  // AppBar's default color="primary" sets text to its
                  // contrastText (white) regardless of the backgroundColor
                  // override above, which is invisible against white.
                  color: theme.palette.mode === "dark" ? "white" : "text.primary",
                  display: { xs: "none", sm: "block" },
                }}
              >
                ARDC Research Activity Identifier
              </Typography>
            </Stack>
          </Link>
        </Stack>
        <div style={{ flexGrow: 1 }} />
        {authenticated && <AuthenticatedNavbarContent /> }
      </Toolbar>
    </AppBar>
    {authenticated && !isProduction &&
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
