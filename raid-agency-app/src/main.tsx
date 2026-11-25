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
  buildMuiTheme,
  ConfigIssuesProvider,
  defaultTheme,
  setRuntimeConfig,
  RuntimeConfigProvider,
  ThemeLookupFailedProvider,
  BrandingProvider,
  resolveStartupBranding,
} from "./config";
import { initKeycloakInstance } from "./auth/keycloak";
import { ConfigErrorScreen } from "./components/config-issue-banner";
import { StartupSpinner } from "./components/startup-spinner/StartupSpinner";

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
  // RAID-1065: nothing branded is shown until the config and the realm's login
  // theme are both known. There is no timeout.
  root.render(
    <ThemeProvider theme={buildMuiTheme(defaultTheme)}>
      <CssBaseline />
      <StartupSpinner />
    </ThemeProvider>
  );

  const { runtime, branding: rawBranding, issues } = await loadConfig();

  setRuntimeConfig(runtime);
  initKeycloakInstance(runtime.keycloak);

  const { branding, themeLookupFailed } = await resolveStartupBranding(
    rawBranding,
    runtime.keycloak
  );

  const theme = buildMuiTheme(branding.theme);

  root.render(
    <RuntimeConfigProvider config={runtime}>
      <ConfigIssuesProvider issues={issues}>
        <ThemeLookupFailedProvider failed={themeLookupFailed}>
          <BrandingProvider branding={branding}>
            <ThemeProvider theme={theme}>
              <CssBaseline />
              <RouterProvider router={router} />
            </ThemeProvider>
          </BrandingProvider>
        </ThemeLookupFailedProvider>
      </ConfigIssuesProvider>
    </RuntimeConfigProvider>
  );
}

// RAID-941: never leave a blank page. Show what is wrong under a minimal
// header; the full error stays in the console for operators.
function renderStartupError(error: unknown) {
  console.error(error);
  root.render(
    <ThemeProvider theme={buildMuiTheme(defaultTheme)}>
      <CssBaseline />
      <ConfigErrorScreen error={error} />
    </ThemeProvider>
  );
}

bootstrap().catch(renderStartupError);
