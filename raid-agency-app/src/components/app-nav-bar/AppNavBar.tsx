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
import {
  NavLink as ConfiguredNavLink,
  hasConfigIssues,
  useBranding,
  useConfigIssues,
  useRuntimeConfig,
  useThemeLookupFailed,
} from "@/config";
import { MegaMenu } from "../mega-menu/mega-menu";
import Banner from "../alert-notifications/banner/Banner";
import { ConfigIssueBanner, ThemeLookupBanner } from "../config-issue-banner";
import { ROUTES } from "@/constants/routes";

// Links from header.navLinks in app-config.json. They are validated to be https
// or mailto addresses. A web link opens in a new tab; a mailto link does not.
const isWebLink = (link: ConfiguredNavLink) => link.path.startsWith("https:");
const linkTarget = (link: ConfiguredNavLink) =>
  isWebLink(link) ? { target: "_blank", rel: "noopener noreferrer" } : {};

const HeaderNavLinks = () => {
  const theme = useTheme();
  const { navLinks } = useBranding().header;
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
      {navLinks.map((link) => (
        <Stack
          key={`${link.label}|${link.path}`}
          component="a"
          href={link.path}
          {...linkTarget(link)}
          direction="row"
          alignItems="center"
          gap={0.4}
          sx={linkSx}
        >
          {link.label} {isWebLink(link) && <OpenInNewIcon sx={{ fontSize: 16 }} />}
        </Stack>
      ))}
    </Stack>
  );
};

// HeaderNavLinks hides below the "md" breakpoint with nowhere else to
// go, since the hamburger drawer that used to hold these was removed
// (it wasn't in the design and largely duplicated the new nav links /
// user menu) - this is the mobile-only replacement, a compact menu
// button that opens the same links.
const MobileNavMenu = () => {
  const { navLinks } = useBranding().header;
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
        {navLinks.map((link) => (
          <MenuItem
            key={`${link.label}|${link.path}`}
            component="a"
            href={link.path}
            {...linkTarget(link)}
            onClick={() => setAnchorEl(null)}
          >
            {link.label}
          </MenuItem>
        ))}
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
  const { header } = useBranding();
  const { environment, supportEmail } = useRuntimeConfig();
  const configIssues = useConfigIssues();
  const themeLookupFailed = useThemeLookupFailed();
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
       {header.menu === "ardc" && <MegaMenu />}
      <Toolbar variant={"dense"} sx={{ minHeight: "64px" }}>
        <Stack direction="row" alignItems="center">
          <Link
            to="/"
            style={{ lineHeight: 0, textDecoration: "none", color: "inherit" }}
          >
            <Stack direction="row" alignItems="center" gap={1.5}>
              <img
                src={theme.palette.mode === "dark" ? header.logo.dark : header.logo.light}
                alt={header.logo.alt}
                height={header.logo.height}
                style={{ width: "auto" }}
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
                {header.title}
              </Typography>
            </Stack>
          </Link>
        </Stack>
        <div style={{ flexGrow: 1 }} />
        {authenticated && <AuthenticatedNavbarContent /> }
      </Toolbar>
    </AppBar>
    <div className="notification-banner-stack">
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
      {themeLookupFailed && <ThemeLookupBanner supportEmail={supportEmail} />}
      {hasConfigIssues(configIssues) && (
        <ConfigIssueBanner level="warning" issues={configIssues} />
      )}
    </div>
    </>
  );
};
