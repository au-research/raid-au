import { useRuntimeConfig } from "@/config";
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

const linkButton = (href: string, label: string) => (
  <Button
    href={href}
    target="_blank"
    variant="text"
    size="small"
    sx={{ minWidth: 0 }}
  >
    {label}
  </Button>
);

export function Privacy() {
  const { supportEmail, organisation } = useRuntimeConfig();
  const { name, url, privacyPolicyUrl, dataDeletionDays } = organisation;

  const policy = privacyPolicyUrl
    ? linkButton(privacyPolicyUrl, "privacy policy")
    : "privacy policy";
  const owner = name ? (url ? linkButton(url, name) : name) : null;

  // With no name and no policy address there is nothing to say about a policy,
  // so the sentence is left out.
  const showPolicy = Boolean(name || privacyPolicyUrl);

  const deletionPeriod =
    dataDeletionDays === undefined
      ? "."
      : ` within ${dataDeletionDays} ${dataDeletionDays === 1 ? "day" : "days"}.`;

  return (
    <Container maxWidth="sm">
      <Card>
        <CardHeader title="Privacy Statement" />

        <CardContent>
          <Stack gap={2}>
            {showPolicy && (
              <Typography variant="body2" color="text.secondary">
                The RAiD Service fully conforms to the {policy}
                {owner && <> of {owner}</>}.
              </Typography>
            )}

            <Typography variant="body2" color="text.secondary">
              You can request that your own data be deleted by submitting a
              request to {linkButton(`mailto:${supportEmail}`, supportEmail)}. All data
              associated with your account (especially anything from Identity
              Providers like Google, etc.) will be deleted{deletionPeriod}
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
