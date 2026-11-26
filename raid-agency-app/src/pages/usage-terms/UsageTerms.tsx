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

export function UsageTerms() {
  const { supportEmail, organisation } = useRuntimeConfig();
  const { name, termsUrl } = organisation;

  const termsButton = termsUrl && (
    <Button
      href={termsUrl}
      target="_blank"
      variant="text"
      size="small"
      sx={{ minWidth: 0 }}
    >
      Terms & conditions
    </Button>
  );

  return (
    <Container maxWidth="sm">
      <Card>
        <CardHeader title="Usage Terms" />
        <CardContent>
          <Stack gap={2}>
            <Typography variant="body2" color="text.secondary">
              {termsButton ? (
                <>
                  Please refer to the {name ? `${name} ` : ""}
                  {termsButton} page for the list of relevant Terms and
                  Conditions for usage of the RAiD Service.
                </>
              ) : (
                <>
                  For the terms and conditions that apply to your use of the
                  RAiD Service, please contact{" "}
                  <Button
                    href={`mailto:${supportEmail}`}
                    variant="text"
                    size="small"
                    sx={{ minWidth: 0 }}
                  >
                    {supportEmail}
                  </Button>
                  .
                </>
              )}
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
