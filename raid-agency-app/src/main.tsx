import "@/index.css";
import "@fontsource/figtree";
import ReactDOM from "react-dom/client";
import { ThemeProvider } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import {
  createBrowserRouter,
  Navigate,
  RouterProvider,
} from "react-router-dom";
import { App } from "./App";
import { otherRoutes, raidPageRoutes, servicePointRoutes } from "./routes";
import { ErrorAlertComponent } from "./components/error-alert-component";
import {
  loadConfig,
  AppConfigProvider,
  buildMuiTheme,
  setRuntimeConfig,
  RuntimeConfigProvider,
} from "./config";
import { initKeycloakInstance } from "./auth/keycloak";

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
]);

async function bootstrap() {
  const { runtime, app } = await loadConfig();

  setRuntimeConfig(runtime);
  initKeycloakInstance(runtime.keycloak);

  const theme = buildMuiTheme(app.theme);

  const root = ReactDOM.createRoot(
    document.getElementById("root") as HTMLElement
  );

  root.render(
    <RuntimeConfigProvider config={runtime}>
      <AppConfigProvider config={app}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <RouterProvider router={router} />
        </ThemeProvider>
      </AppConfigProvider>
    </RuntimeConfigProvider>
  );
}

bootstrap();
