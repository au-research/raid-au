import { ProtectedRoute } from "@/components/protected-route";
import ServicePoint from "@/pages/service-point/ServicePoint";
import ServicePoints from "@/pages/service-points/ServicePoints";
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
