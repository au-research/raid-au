import { ProtectedRoute } from "@/pages/protected-route";
import { ServicePoint } from "@/pages/service-point";
import { ServicePoints } from "@/pages/service-points";
import { RouteObject } from "react-router-dom";
import { ROUTES } from "@/constants/routes";

export const servicePointRoutes: RouteObject[] = [
  {
    path: ROUTES.SERVICE_POINTS,
    // This route is protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under SERVICE_POINTS will render the ServicePoints component
        element: <ServicePoints />,
      },
    ],
  },
  {
    path: ROUTES.SERVICE_POINT,
    // This route is also protected, meaning it requires authentication
    element: <ProtectedRoute />,
    children: [
      {
        path: ROUTES.EMPTY_PATH,
        // The empty path under SERVICE_POINT will render the ServicePoint component
        element: <ServicePoint />,
      },
    ],
  },
];
