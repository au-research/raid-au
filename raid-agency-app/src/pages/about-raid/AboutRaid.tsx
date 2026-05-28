import {
  Button,
  Container,
  List,
  ListItem,
  Stack,
  Typography,
} from "@mui/material";
import { NavLink } from "react-router-dom";

const externalLinkButton = ({
  link,
  label,
}: {
  link: string;
  label: string;
}) => {
  return (
    <Button
      href={link}
      target="_blank"
      variant="text"
      size="small"
      sx={{ minWidth: 0 }}
    >
      {label}
    </Button>
  );
};

export function AboutRaid() {
  return (
    <Container maxWidth="sm">
      <Stack spacing={2}>
        <Typography paragraph>
          This RAiD Service is the Oceania region implementation of the RAiD ISO
          standard.
        </Typography>
        <Typography paragraph>
          RAiD is a unique and persistent identifier for research projects.
          <br />
          It acts as a container for research project activities by collecting
          identifiers for the people, publications, instruments and institutions
          that are involved.
        </Typography>
        <Typography paragraph>
          Anybody can use the application as long as they are approved by one of
          the research institutions that use the RAiD Service for publishing
          Research Activity Identifiers.
        </Typography>
        <Typography paragraph>
          A research project is an activity. It takes place over a period of
          time, has a set scope, is resourced by researchers, research support
          staff and uses and produces data.
        </Typography>

        <Typography>
          <List>
            You can find more information here:
            <ListItem>
              {externalLinkButton({
                label: "RAiD project",
                link: "https://www.raid.org",
              })}
            </ListItem>
            <ListItem>
              {externalLinkButton({
                label: "RAiD ISO standard",
                link: "https://www.iso.org/standard/75931.html",
              })}
            </ListItem>
            <ListItem>
              {externalLinkButton({
                label: "RAiD GitHub",
                link: "https://github.com/au-research/raid-au",
              })}
            </ListItem>
            <ListItem>
              {externalLinkButton({
                label: "Australian Research Data Commons",
                link: "https://ardc.edu.au/",
              })}
            </ListItem>
          </List>
        </Typography>
        <NavLink to="/home">
          <Button>Home</Button>
        </NavLink>
      </Stack>
    </Container>
  );
}
