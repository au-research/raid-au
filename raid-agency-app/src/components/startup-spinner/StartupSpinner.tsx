import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";

/**
 * Shown while the app loads app-config.json and looks up the login theme, so
 * nothing branded appears before the branding is known (RAID-1065).
 */
export const StartupSpinner = () => (
  <Box
    sx={{
      display: "flex",
      justifyContent: "center",
      alignItems: "center",
      minHeight: "100vh",
    }}
  >
    <CircularProgress aria-label="Loading" />
  </Box>
);
