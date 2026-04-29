import { ContactSupport, Home, Refresh } from "@mui/icons-material";
import {
  Alert,
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  Stack,
} from "@mui/material";
import { memo } from "react";
import { useNavigate } from "react-router-dom";
import { useAppConfig } from "@/config/Appconfigcontext";
interface ErrorAlertProps {
  error?: Error | string;
  showButtons?: boolean;
}

export const ErrorAlertComponent = memo(
  ({ error, showButtons }: ErrorAlertProps) => {
    const navigate = useNavigate();
    const config = useAppConfig();
    const supportEmail =
      import.meta.env.VITE_SUPPORT_EMAIL || config.footer.links.find(link => link.contact)?.label;

    const handleContactSupport = () => {
      location.href = `mailto:${supportEmail}`;
    };

    const errorMessage =
      typeof error === "string"
        ? error
        : error
        ? JSON.stringify(error, null, 2)
        : "Something went wrong.";

    return (
      <Card sx={{ borderLeft: "solid 3px", borderLeftColor: "error.main" }}>
        <CardHeader title="Error" subheader="An error occured" />
        <CardContent>
          <Alert severity="error">{errorMessage}</Alert>
        </CardContent>
        {showButtons && (
          <CardActions sx={{ justifyContent: "space-between" }}>
            <Stack direction="row" spacing={1}>
              <Button
                color="error"
                size="small"
                variant="outlined"
                onClick={() => navigate("/home")}
                startIcon={<Home />}
              >
                Back to home
              </Button>
              <Button
                color="error"
                size="small"
                variant="outlined"
                onClick={() => window.location.reload()}
                startIcon={<Refresh />}
              >
                Reload
              </Button>
            </Stack>
            <Button
              color="error"
              size="small"
              variant="outlined"
              onClick={handleContactSupport}
              startIcon={<ContactSupport />}
            >
              Contact Support
            </Button>
          </CardActions>
        )}
      </Card>
    );
  }
);

ErrorAlertComponent.displayName = "ErrorAlertComponent";
