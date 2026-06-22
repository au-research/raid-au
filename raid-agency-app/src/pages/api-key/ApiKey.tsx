import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import { useSnackbar } from "@/components/snackbar";
import { useKeycloak } from "@/contexts/keycloak-context";
import { fetchApiTokenFromKeycloak } from "@/services/keycloak";
import {
  ContentCopy as ContentCopyIcon,
  Home as HomeIcon,
  Key as KeyIcon,
  Refresh as RefreshIcon,
  AutoAwesomeOutlined as AutoAwesomeIcon,
} from "@mui/icons-material";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  Container,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";

export const ApiKey = () => {
  const { token, refreshToken, updateToken } = useKeycloak();
  const { openSnackbar } = useSnackbar();
  const [apiToken, setApiToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  async function handleApiTokenCreate() {
    try {
      setIsLoading(true);
      if (!refreshToken) {
        throw new Error("No refresh token found");
      }

      // First try to update the token
      await updateToken(70);

      const data = await fetchApiTokenFromKeycloak({
        refreshToken: refreshToken,
      });

      setApiToken(data.access_token);
      openSnackbar("✅ New API key generated successfully", 2000);
    } catch (error) {
      console.error("Failed to create API token:", error);
      openSnackbar("❌ Failed to generate API token", 2000);
    } finally {
      setIsLoading(false);
    }
  }

  const handleCopyToken = async (tokenType: "api" | "refresh") => {
    const textToCopy = tokenType === "api" ? apiToken : refreshToken;
    try {
      await navigator.clipboard.writeText(textToCopy || "");
      openSnackbar(
        `✅ ${
          tokenType === "api" ? "API" : "Refresh"
        } token copied to clipboard`,
        1000
      );
    } catch (error) {
      openSnackbar("❌ Failed to copy to clipboard", 2000);
    }
  };

  const breadcrumbs: Breadcrumb[] = [
    {
      label: "Home",
      to: "/",
      icon: <HomeIcon />,
    },
    {
      label: "API Key",
      to: "/api-key",
      icon: <KeyIcon />,
    },
  ];

  return (
    <Container>
      <Stack gap={2}>
        <BreadcrumbsBar breadcrumbs={breadcrumbs} />

        <Card>
          <CardHeader
            title="API Tokens"
            subheader="Manage your API tokens"
          />
          <CardContent>
            <Stack direction="column" gap={4}>
              <Stack gap={2}>
                <Typography variant="h6">API Token</Typography>
                <Stack direction="row" alignItems="flex-start" gap={2}>
                  <Button
                    variant="outlined"
                    onClick={handleApiTokenCreate}
                    startIcon={<AutoAwesomeIcon />}
                    disabled={isLoading || !refreshToken}
                  >
                    Generate API Token
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => handleCopyToken("api")}
                    startIcon={<ContentCopyIcon />}
                    disabled={!apiToken}
                  >
                    Copy API Token
                  </Button>
                </Stack>
                <TextField
                  multiline
                  value={apiToken || ""}
                  fullWidth
                  size="small"
                  placeholder="No API token generated yet"
                  InputProps={{
                    readOnly: true,
                  }}
                />
              </Stack>
            </Stack>
          </CardContent>
        </Card>
      </Stack>
    </Container>
  );
};
