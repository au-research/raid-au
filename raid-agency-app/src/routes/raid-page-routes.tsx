import { ProtectedRoute } from "@/pages/protected-route";
import { MintRaid } from "@/pages/mint-raid";
import { RaidDisplay } from "@/pages/raid-display";
import { RaidEdit } from "@/pages/raid-edit";
import { RaidHistory } from "@/pages/raid-history";
import { Raids } from "@/pages/raids";
import { RouteObject } from "react-router-dom";
import { ROUTES } from "@/constants/routes";

export const raidPageRoutes: RouteObject[] = [
  {
    path: ROUTES.RAIDS,
    // This route is protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under RAIDS will render the Raids component
        element: <Raids />,
      },
    ],
  },
  {
    path: ROUTES.MINT_RAID,
    // This route is also protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        element: <MintRaid />,
      },
    ],
  },
  {
    path: ROUTES.RAID_DISPLAY,
  // This route is protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under RAID_DISPLAY will render the RaidDisplay component
        element: <RaidDisplay />,
      },
    ],
  },
  {
    path: ROUTES.RAID_HISTORY_DETAIL,
  // This route is protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under RAID_DISPLAY will render the RaidDisplay component
        element: <RaidDisplay />,
      },
    ],
  },
  {
    path: ROUTES.RAID_EDIT,
  // This route is protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under RAID_EDIT will render the RaidEdit component
        element: <RaidEdit />,
      },
    ],
  },
  {
    path: ROUTES.RAID_HISTORY,
  // This route is protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under RAID_HISTORY will render the RaidHistory component
        element: <RaidHistory />,
      },
    ],
  },
];
