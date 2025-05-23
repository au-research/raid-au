import { ProtectedRoute } from "@/pages/protected-route";
import { ServicePoint } from "@/pages/service-point";
import { ServicePoints } from "@/pages/service-points";
import { RouteObject } from "react-router-dom";

export const servicePointRoutes: RouteObject[] = [
  {
    path: "/service-points",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <ServicePoints />,
      },
    ],
  },
  {
    path: "/service-points/:servicePointId",
    element: <ProtectedRoute />,
    children: [
      {
        path: "",
        element: <ServicePoint />,
      },
    ],
  },
];
