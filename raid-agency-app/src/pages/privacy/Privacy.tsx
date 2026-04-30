import { useAppConfig } from "@/config/Appconfigcontext";
import {
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  Container,
  Stack,
  Typography,
} from "@mui/material";
import { NavLink } from "react-router-dom";

export function Privacy() {
  const config = useAppConfig();
  const privacyPolicyLinkButton = (
    <Button
      href="https://ardc.edu.au/privacy-policy/"
      target="_blank"
      variant="text"
      size="small"
      sx={{ minWidth: 0 }}
    >
      privacy policy
    </Button>
  );

  const ardcLinkButton = (
    <Button
      href="https://ardc.edu.au/"
      target="_blank"
      variant="text"
      size="small"
      sx={{ minWidth: 0 }}
    >
      ARDC
    </Button>
  );

  const supportMailLink = (
    <Button
      href={`mailto:${config.footer.links.find(link => link.contact)?.label}`}
      variant="text"
      size="small"
      sx={{ minWidth: 0 }}
    >
      {config.footer.links.find(link => link.contact)?.label}
    </Button>
  );

  return (
    <Container maxWidth="sm">
      <Card>
        <CardHeader title="Privacy Statement" />

        <CardContent>
          <Stack gap={2}>
            <Typography variant="body2" color="text.secondary">
              The RAiD Service fully conforms to the {privacyPolicyLinkButton}{" "}
              of the Australian Research Data Commons {ardcLinkButton}.
            </Typography>

            <Typography variant="body2" color="text.secondary">
              You can request that your own data be deleted by submitting a
              request to {supportMailLink}. All data associated with your
              account (especially anything from Identity Providers like Google,
              etc.) will be deleted within 7 days.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Data from Identity Providers (e.g. AAF, Google) is only used to
              enable sign-in functionality so that users can make use of the
              application without having to create a new set of email/password
              credentials.
            </Typography>
          </Stack>
        </CardContent>
        <CardActions>
          <NavLink to="/home">
            <Button>Back Home</Button>
          </NavLink>
        </CardActions>
      </Card>
    </Container>
  );
}
