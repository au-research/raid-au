import { useKeycloak } from '@/contexts/keycloak-context';
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Box, CircularProgress, Typography } from '@mui/material';

export function Login() {
  const { isInitialized, authenticated, login } = useKeycloak();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    if (!isInitialized) {
      return; // Wait for Keycloak to initialize
    }

    if (authenticated) {
      // User is already logged in, redirect to intended destination or home
      const redirectTo = searchParams.get('redirect') || '/';
      navigate(redirectTo, { replace: true });
    } else {
      // User is not logged in, redirect to Keycloak login page
      const redirectUri = searchParams.get('redirect') 
        ? `${window.location.origin}/login?redirect=${searchParams.get('redirect')}`
        : `${window.location.origin}/`;
      
      login({
        redirectUri: redirectUri,
      });
    }
  }, [isInitialized, authenticated, login, navigate, searchParams]);

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
