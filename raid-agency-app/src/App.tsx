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
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode, useMemo } from "react";
import { Outlet } from "react-router-dom";
import { ErrorDialogProvider } from "./components/error-dialog";
import { KeycloakProvider } from "./contexts/keycloak-context";
import { useGoogleAnalytics } from "./shared/hooks/google-analytics/useGoogleAnalytics";

export function App() {
  useGoogleAnalytics();
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
          error: {
            main: "#d32f2f",
          },
          warning: {
            main: "#f57c00",
          },
          info: {
            main: "#1976d2",
          },
          success: {
            main: "#2e7d32",
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
    <>
      <KeycloakProvider>
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
    </KeycloakProvider>
    </>
  );
}
