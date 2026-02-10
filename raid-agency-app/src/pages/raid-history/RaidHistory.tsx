import type {Breadcrumb} from "@/components/breadcrumbs-bar";
import {BreadcrumbsBar} from "@/components/breadcrumbs-bar";
import {ErrorAlertComponent} from "@/components/error-alert-component";
import {useKeycloak} from "@/contexts/keycloak-context";
import {Loading} from "@/pages/loading";
import type {RaidHistoryElementType,} from "@/pages/raid-history";
import {
  DocumentScanner as DocumentScannerIcon,
  History as HistoryIcon,
  HistoryEdu as HistoryEduIcon,
  Home as HomeIcon,
  SettingsBackupRestore as SettingsBackupRestoreIcon,
} from "@mui/icons-material";
import {Box, Button, Card, CardContent, CardHeader, Container, Grid, Stack, Typography,} from "@mui/material";

import {useQuery} from "@tanstack/react-query";
import {Navigate, useNavigate, useParams} from "react-router-dom";
import {raidService} from "@/services/raid-service.ts";

export const RaidHistory = () => {
  const { authenticated, isInitialized, token } = useKeycloak();
  const navigate = useNavigate();

  const { prefix, suffix } = useParams() as { prefix: string; suffix: string };
  const handle = `${prefix}/${suffix}`;

  const raidHistoryQuery = useQuery({
    queryKey: ["raids", prefix, suffix],
    queryFn: () => raidService.fetchHistory(handle),
    enabled: isInitialized && authenticated,
  });

  if (raidHistoryQuery.isPending) {
    return <Loading />;
  }

  if (raidHistoryQuery.isError) {
    return <ErrorAlertComponent error="RAiD history could not be fetched" />;
  }

  const breadcrumbs: Breadcrumb[] = [
    {
      label: "Home",
      to: "/",
      icon: <HomeIcon />,
    },
    {
      label: "RAiDs",
      to: "/raids",
      icon: <HistoryEduIcon />,
    },
    {
      label: `RAiD ${handle}`,
      to: `/raids/${handle}`,
      icon: <DocumentScannerIcon />,
    },
    {
      label: `History`,
      to: `/raids/${handle}/history`,
      icon: <HistoryIcon />,
    },
  ];
  return (
    <Container maxWidth="lg">
      <Stack gap={4}>
        <BreadcrumbsBar breadcrumbs={breadcrumbs} />

        {raidHistoryQuery?.data &&
          raidHistoryQuery?.data.length > 0 &&
          raidHistoryQuery.data
            .sort((a, b) => (a.version > b.version ? -1 : 1))
            .map((el, i: number) => {
              return (
                <Card key={i}>
                  <CardHeader
                    title={`Version ${el.version}`}
                    subheader={`${new Date(el.timestamp).toLocaleString()} UTC`}
                    action={
                      <Button
                        variant="outlined"
                        size="small"
                        onClick={() => {
                          navigate(`/raids/${prefix}/${suffix}/${el.version}`);
                        }}
                      >
                        <SettingsBackupRestoreIcon sx={{ mr: 1 }} />
                        Restore
                      </Button>
                    }
                  />
                  <CardContent>
                    <Stack gap={2}>
                      {JSON.parse(atob(el.diff))
                        .filter(
                          (el: RaidHistoryElementType) =>
                            el.path !== "/identifier/version"
                        )
                        .filter(
                          (el: RaidHistoryElementType) =>
                            JSON.stringify(el.value) !== "[]"
                        )
                        .map((el: RaidHistoryElementType, i2: number) => (
                          <Box className="raid-card-well" key={i2}>
                            <Grid container spacing={2}>
                              <Grid item xs={12} sm={6} md={1}>
                                <Box>
                                  <Typography variant="body2">Op</Typography>
                                  <Typography
                                    color="text.secondary"
                                    variant="body1"
                                    data-testid="start-date-value"
                                  >
                                    {el.op}
                                  </Typography>
                                </Box>
                              </Grid>
                              <Grid item xs={12} sm={6} md={2}>
                                <Box>
                                  <Typography variant="body2">Path</Typography>
                                  <Typography
                                    color="text.secondary"
                                    variant="body1"
                                    data-testid="start-date-value"
                                  >
                                    {el.path}
                                  </Typography>
                                </Box>
                              </Grid>
                              <Grid item xs={12} sm={6} md={9}>
                                <Box sx={{ overflow: "auto" }}>
                                  <Typography variant="body2">Value</Typography>
                                  <Typography
                                    color="text.secondary"
                                    variant="body1"
                                    data-testid="start-date-value"
                                  >
                                    {JSON.stringify(el.value)}
                                  </Typography>
                                </Box>
                              </Grid>
                            </Grid>
                          </Box>
                        ))}
                    </Stack>
                  </CardContent>
                </Card>
              );
            })}
      </Stack>
    </Container>
  );
};
