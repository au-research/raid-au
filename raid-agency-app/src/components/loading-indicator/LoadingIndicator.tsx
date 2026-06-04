import React, { memo } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

export const LoadingIndicator = memo(
({
    id,
    loadingStates,
    message,
    spinnerSize = 16,
    textVariant = "caption",
    centered = false,
}: {
    id?: string;
    loadingStates: Record<string, boolean>;
    message?: string;
    spinnerSize?: number;
    textVariant?: "caption" | "body2" | "body1";
    centered?: boolean;
}) => {
  if (!id || !loadingStates[id]) return null;

  if (centered) {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2, py: 2 }}>
        <CircularProgress size={spinnerSize} />
        <Typography variant={textVariant} color="text.primary">
          {message || 'Loading...'}
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
      <CircularProgress size={spinnerSize} />
      <Typography variant={textVariant} color="text.primary">
        {message || 'Loading...'}
      </Typography>
    </Box>
  );
});
