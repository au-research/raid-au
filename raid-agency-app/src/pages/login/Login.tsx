import { useKeycloak } from '@/contexts/keycloak-context';
import { useCallback, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, Button, Card, CardContent, CircularProgress, Typography } from '@mui/material';
import { AppNavBar } from '@/components/app-nav-bar';
import { hasConfigIssues, useConfigIssues } from '@/config';

export function Login() {
  const { isInitialized, authenticated, login } = useKeycloak();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  // RAID-941: with config warnings, pause here so the banner can be read
  // before the user leaves for Keycloak.
  const pauseForConfigIssues = hasConfigIssues(useConfigIssues());

  const redirectToKeycloak = useCallback(() => {
    const redirectUri = searchParams.get('redirect')
      ? `${window.location.origin}/login?redirect=${searchParams.get('redirect')}`
      : `${window.location.origin}/`;

    login({
      redirectUri: redirectUri,
    });
  }, [login, searchParams]);

  useEffect(() => {
    if (!isInitialized) {
      return; // Wait for Keycloak to initialize
    }

    if (authenticated) {
      // User is already logged in, redirect to intended destination or home
      const redirectTo = searchParams.get('redirect') || '/';
      navigate(redirectTo, { replace: true });
    } else if (!pauseForConfigIssues) {
      // User is not logged in, redirect to Keycloak login page
      redirectToKeycloak();
    }
  }, [isInitialized, authenticated, navigate, searchParams, pauseForConfigIssues, redirectToKeycloak]);

  if (isInitialized && !authenticated && pauseForConfigIssues) {
    return (
      <>
        <AppNavBar />
        <Box sx={{ display: 'flex', justifyContent: 'center', px: 2, py: 6 }}>
          <Card sx={{ maxWidth: 440, width: '100%', textAlign: 'center' }}>
            <CardContent sx={{ display: 'grid', gap: 1.5, p: 3.5 }}>
              <Typography variant="h5" component="h1" sx={{ fontWeight: 700 }}>
                Log in to RAiD
              </Typography>
              <Typography variant="body2" color="text.secondary">
                You can still log in. Some features may not work until the configuration is fixed.
              </Typography>
              <Box>
                <Button variant="contained" onClick={redirectToKeycloak}>
                  Continue to log in
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Box>
      </>
    );
  }

  // Show loading state while redirecting
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        gap: 2,
      }}
    >
      <CircularProgress size={48} />
      <Typography variant="body1" color="text.secondary">
        Redirecting to login...
      </Typography>
    </Box>
  );
}
