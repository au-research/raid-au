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
  loadAppConfig,
  AppConfigProvider,
  buildMuiTheme,
  loadRuntimeConfig,
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
  const [runtimeConfig, appConfig] = await Promise.all([
    loadRuntimeConfig(),
    loadAppConfig(),
  ]);

  setRuntimeConfig(runtimeConfig);
  initKeycloakInstance(runtimeConfig.keycloak);

  const theme = buildMuiTheme(appConfig.theme);

  const root = ReactDOM.createRoot(
    document.getElementById("root") as HTMLElement
  );

  root.render(
    <RuntimeConfigProvider config={runtimeConfig}>
      <AppConfigProvider config={appConfig}>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <RouterProvider router={router} />
        </ThemeProvider>
      </AppConfigProvider>
    </RuntimeConfigProvider>
  );
}

bootstrap();
