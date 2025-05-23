import { ProtectedRoute } from "@/pages/protected-route";
import { MintRaid } from "@/pages/mint-raid";
import { RaidDisplay } from "@/pages/raid-display";
import { RaidEdit } from "@/pages/raid-edit";
import { RaidHistory } from "@/pages/raid-history";
import { Raids } from "@/pages/raids";
import { RouteObject } from "react-router-dom";

export const raidPageRoutes: RouteObject[] = [
  {
    path: "/raids",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <Raids />,
      },
    ],
  },
  {
    path: "/raids/new",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <MintRaid />,
      },
    ],
  },
  {
    path: "/raids/:prefix/:suffix",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <RaidDisplay />,
      },
    ],
  },
  {
    path: "/raids/:prefix/:suffix/edit",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <RaidEdit />,
      },
    ],
  },
  {
    path: "/raids/:prefix/:suffix/history",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <RaidHistory />,
      },
    ],
  },
];
