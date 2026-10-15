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
  ConfigIssuesProvider,
  defaultConfig,
  setRuntimeConfig,
  RuntimeConfigProvider,
} from "./config";
import { initKeycloakInstance } from "./auth/keycloak";
import { ConfigErrorScreen } from "./components/config-issue-banner";

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

const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement
);

async function bootstrap() {
  const { runtime, app, issues } = await loadConfig();

  setRuntimeConfig(runtime);
  initKeycloakInstance(runtime.keycloak);

  const theme = buildMuiTheme(app.theme);

  root.render(
    <RuntimeConfigProvider config={runtime}>
      <ConfigIssuesProvider issues={issues}>
        <AppConfigProvider config={app}>
          <ThemeProvider theme={theme}>
            <CssBaseline />
            <RouterProvider router={router} />
          </ThemeProvider>
        </AppConfigProvider>
      </ConfigIssuesProvider>
    </RuntimeConfigProvider>
  );
}

// RAID-941: never leave a blank page. Show what is wrong under a minimal
// header; the full error stays in the console for operators.
function renderStartupError(error: unknown) {
  console.error(error);
  root.render(
    <ThemeProvider theme={buildMuiTheme(defaultConfig.theme)}>
      <CssBaseline />
      <ConfigErrorScreen error={error} />
    </ThemeProvider>
  );
}

bootstrap().catch(renderStartupError);
