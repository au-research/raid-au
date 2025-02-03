import { SnackbarProvider } from "@/components/snackbar";
import { ReactErrorBoundary } from "@/error/ReactErrorBoundary";
import { MappingProvider } from "@/mapping";
import {
  Box,
  createTheme,
  CssBaseline,
  ThemeProvider,
  useMediaQuery,
} from "@mui/material";
import { grey } from "@mui/material/colors";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import Keycloak from "keycloak-js";
import { StrictMode, useMemo } from "react";
import { Outlet } from "react-router-dom";
import { ErrorDialogProvider } from "./components/error-dialog";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

export function App() {
  const prefersDarkMode = useMediaQuery("(prefers-color-scheme: dark)");

  const theme = useMemo(
    () =>
      createTheme({
        typography: {
          fontFamily: `Figtree, sans-serif`,
          fontSize: 14,
          fontWeightLight: 300,
          fontWeightRegular: 400,
          fontWeightMedium: 500,
        },
        palette: {
          mode: prefersDarkMode ? "dark" : "light",
          primary: {
            main: "#4183CE",
          },
          secondary: {
            main: "#DC8333",
          },
          background: {
            default: prefersDarkMode ? "#000" : grey[50],
          },
        },
      }),
    [prefersDarkMode]
  );

  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
      },
    },
  });

  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{
        pkceMethod: "S256",
      }}
    >
      <StrictMode>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <ErrorDialogProvider>
            <MappingProvider>
              <SnackbarProvider>
                <QueryClientProvider client={queryClient}>
                  <ReactErrorBoundary>
                    <Box sx={{ pt: 3 }}></Box>
                    <Outlet />
                  </ReactErrorBoundary>
                </QueryClientProvider>
              </SnackbarProvider>
            </MappingProvider>
          </ErrorDialogProvider>
        </ThemeProvider>
      </StrictMode>
    </ReactKeycloakProvider>
  );
}
