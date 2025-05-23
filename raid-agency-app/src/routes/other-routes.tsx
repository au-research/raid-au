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

export const otherRoutes: RouteObject[] = [
  {
    path: "/",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <Home />,
      },
    ],
  },
  {
    path: "/login",
    element: (
      <Box sx={{ pt: 5 }}>
        <AppNavBar authenticated={false} />
        <Login />
      </Box>
    ),
  },
  {
    path: "/privacy",
    element: (
      <>
        <AppNavBar authenticated={false} />
        <Privacy />
      </>
    ),
  },
  {
    path: "/terms",
    element: (
      <>
        <AppNavBar authenticated={false} />
        <UsageTerms />
      </>
    ),
  },
  {
    path: "/about-raid",
    element: (
      <>
        <AppNavBar authenticated={false} />
        <AboutRaid />
      </>
    ),
  },
  {
    path: "/api-key",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <ApiKey />,
      },
    ],
  },
  {
    path: "/invites",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <Invites />,
      },
    ],
  },
  {
    path: "/cache-manager",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <CacheManager />,
      },
    ],
  },
  {
    path: "/invite",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <InviteRedirect />,
      },
    ],
  },
];
