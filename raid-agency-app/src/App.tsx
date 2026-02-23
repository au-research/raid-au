import { SnackbarProvider } from "@/components/snackbar";
import { ReactErrorBoundary } from "@/error/ReactErrorBoundary";
import { MappingProvider } from "@/mapping";
import {
  Box,
  createTheme,
  CssBaseline,
  ThemeProvider,
  useMediaQuery,
  Container,
} from "@mui/material";
import { grey } from "@mui/material/colors";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode, useMemo } from "react";
import { Outlet } from "react-router-dom";
import { ErrorDialogProvider } from "./components/error-dialog";
import { KeycloakProvider, useKeycloak } from "./contexts/keycloak-context";
import { useGoogleAnalytics } from "./shared/hooks/google-analytics/useGoogleAnalytics";
import { NotificationProvider } from "./components/alert-notifications/notification-context/NotificationsProvider";
import { CodesProvider } from "./components/tree-view/context/CodesProvider";
import { useAppConfig } from "./config/Appconfigcontext";

function AppContent() {
  useGoogleAnalytics();
  const prefersDarkMode = useMediaQuery("(prefers-color-scheme: dark)");
  const { theme: appTheme } = useAppConfig();
  const theme = useMemo(
    () =>
      createTheme({
        typography: appTheme.typography,
        shape: appTheme.shape,
        palette: {
          ...appTheme.palette,
          mode: prefersDarkMode ? "dark" : "light",
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
    <Container disableGutters maxWidth={false} sx={{backgroundColor: theme.palette.background.default, height: '100vh'}}>
      <NotificationProvider>
        <StrictMode>
          <CssBaseline />
          <ErrorDialogProvider>
            <MappingProvider>
              <SnackbarProvider>
                <QueryClientProvider client={queryClient}>
                  <ReactErrorBoundary>
                    <Box sx={{ pt: 7.4 }}></Box>
                    <CodesProvider>
                      <Outlet />
                    </CodesProvider>
                  </ReactErrorBoundary>
                </QueryClientProvider>
              </SnackbarProvider>
            </MappingProvider>
          </ErrorDialogProvider>
        </StrictMode>
    </NotificationProvider>
    </Container>
  );
}

export function App() {
  return (
    <KeycloakProvider>
      <AppContent />
    </KeycloakProvider>
  );
}
