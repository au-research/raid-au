import { AppBar, Box, Stack, Toolbar, Typography } from "@mui/material";
import { ConfigError, defaultConfig, neutralBranding } from "@/config";
import { ConfigIssueBanner } from "./ConfigIssueBanner";

/**
 * Shown instead of the app when bootstrap fails (RAID-941). Deliberately uses
 * only the built-in neutral branding: the router, Keycloak and the loaded
 * config are all unavailable at this point.
 */
export const ConfigErrorScreen = ({ error }: { error: unknown }) => {
  const banner =
    error instanceof ConfigError ? (
      <ConfigIssueBanner level="blocking" reason={error.reason} issues={error.issues} />
    ) : (
      <ConfigIssueBanner level="blocking" reason="startup-failed" />
    );

  return (
    <Box
      sx={{
        minHeight: "100vh",
        backgroundColor: defaultConfig.theme.palette.background?.default,
      }}
    >
      <AppBar
        position="static"
        elevation={1}
        sx={{ backgroundColor: "white" }}
        data-testid="config-error-header"
      >
        <Toolbar variant="dense" sx={{ minHeight: "64px" }}>
          <Stack direction="row" alignItems="center" gap={1.5}>
            <img
              src={neutralBranding.header.logo.light}
              alt={neutralBranding.header.logo.alt}
              height={neutralBranding.header.logo.height}
              style={{ width: "auto" }}
            />
            <Typography
              variant="subtitle1"
              sx={{ fontWeight: 600, color: "text.primary", display: { xs: "none", sm: "block" } }}
            >
              {neutralBranding.header.title}
            </Typography>
          </Stack>
        </Toolbar>
      </AppBar>
      {banner}
      <Typography
        variant="body2"
        color="text.secondary"
        align="center"
        sx={{ pt: 5 }}
      >
        No other content is shown until the configuration is fixed.
      </Typography>
    </Box>
  );
};
