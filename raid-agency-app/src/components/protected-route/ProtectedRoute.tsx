import { AppNavBar } from "@/components/app-nav-bar";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Loading } from "@/pages/loading";
import { Box, Container } from "@mui/material";

import { memo } from "react";
import { Navigate, Outlet } from "react-router-dom";

export const ProtectedRoute = memo(() => {
  const { isInitialized, authenticated } = useKeycloak();

  if (!isInitialized) {
    return (
      <Container>
        <Loading />
      </Container>
    );
  }

  return authenticated ? (
    <>
      <AppNavBar authenticated={true} />
      <Box sx={{ height: "3em" }} />
      <Outlet />
    </>
  ) : (
    <Navigate to="/login" replace />
  );
});

ProtectedRoute.displayName = "ProtectedRoute";
