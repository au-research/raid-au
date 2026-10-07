import { AppNavBar } from "@/components/app-nav-bar";
import { ProtectedRoute } from "@/pages/protected-route";
import { AboutRaid } from "@/pages/about-raid";
import { ApiKey } from "@/pages/api-key";
import { CacheManager } from "@/pages/cache-manager";
import { Home } from "@/pages/home";
import { InviteRedirect } from "@/pages/invite-redirect";
import { Invites } from "@/pages/invites";
import { Login } from "@/pages/login";
import { Privacy } from "@/pages/privacy";
import { UsageTerms } from "@/pages/usage-terms";
import { Box } from "@mui/material";
import { RouteObject } from "react-router-dom";
import { ROUTES } from "@/constants/routes";
import OrcidSuccess from "@/pages/orcid-success/orcid-success";

export const otherRoutes: RouteObject[] = [
  {
    path: ROUTES.HOME,
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under HOME will render the Home component
        element: <Home />,
      },
    ],
  },
  {
    path: ROUTES.LOGIN,
    // No need for ProtectedRoute here since this is the login page
    element: (
      <Box sx={{ pt: 5 }}>
        <Login />
      </Box>
    ),
  },
  {
    path: ROUTES.PRIVACY,
    // No need for ProtectedRoute here since this is a public page
    element: (
      <>
        <AppNavBar />
        <Privacy />
      </>
    ),
  },
  {
    path: ROUTES.TERMS,
    // No need for ProtectedRoute here since this is a public page
    element: (
      <>
        <AppNavBar />
        <UsageTerms />
      </>
    ),
  },
  {
    path: ROUTES.ABOUT_RAID,
    // No need for ProtectedRoute here since this is a public page
    element: (
      <>
        <AppNavBar />
        <AboutRaid />
      </>
    ),
  },
   {
    path: ROUTES.ORCID_SUCCESS,
    // No need for ProtectedRoute here since this is a public page
    element: (
      <>
        <AppNavBar />
        <OrcidSuccess />
      </>
    ),
  },
  {
    path: ROUTES.API_KEY,
    // ProtectedRoute is used here to ensure the user is authenticated before accessing the API key page
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under API_KEY will render the ApiKey component
        element: <ApiKey />,
      },
    ],
  },
  /* {
    path: ROUTES.INVITES,
    // ProtectedRoute ensures that only authenticated users can access this page
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under INVITES will render the Invites component
        element: <Invites />,
      },
    ],
  }, */
  {
    path: ROUTES.CACHE_MANAGER,
    // ProtectedRoute ensures that only authenticated users can access this page
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under CACHE_MANAGER will render the CacheManager component
        element: <CacheManager />,
      },
    ],
  },
  {
    path: ROUTES.INVITE,
    // ProtectedRoute ensures that only authenticated users can access this page
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under INVITE will render the InviteRedirect component
        element: <InviteRedirect />,
      },
    ],
  },
];
