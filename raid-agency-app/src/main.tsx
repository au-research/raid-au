import "@/index.css";
import "@fontsource/figtree";
import ReactDOM from "react-dom/client";
import {
  createBrowserRouter,
  Navigate,
  RouterProvider,
} from "react-router-dom";
import { App } from "./App";
import { otherRoutes, raidPageRoutes, servicePointRoutes } from "./routes";
import { ErrorAlertComponent } from "./components/error-alert-component";
import { OrcidSuccess } from "./pages/orcid-success";
import { AppNavBar } from "./components/app-nav-bar";
import { ROUTES } from "./constants/routes";

const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement
);

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    errorElement: <ErrorAlertComponent error="An error occurred" />,
    children: [
      ...servicePointRoutes,
      ...raidPageRoutes,
      ...otherRoutes,
      {
        path: "*",
        element: <Navigate to="/" />,
      },
    ],
  },
  {
    path: ROUTES.ORCID_SUCCESS,
    element: (
      <>
        <AppNavBar authenticated={false} />
        <OrcidSuccess />
      </>
    ),
  }
]);

root.render(<RouterProvider router={router} />);
