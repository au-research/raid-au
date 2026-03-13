import React, { memo } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

// Individual DOI Loading Indicator
export const LoadingIndicator = memo(
({
    id,
    loadingStates,
    message
}: {
    id?: string;
    loadingStates: Record<string, boolean>;
    message?: string;
}) => {
  if (!id || !loadingStates[id]) return null;

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
      <CircularProgress size={16} />
      <Typography variant="caption" color="text.secondary">
        {message || 'Loading...'}
      </Typography>
    </Box>
  );
});
