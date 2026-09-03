import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import { ErrorAlertComponent } from "@/components/error-alert-component";
import { useAuthHelper } from "@/auth/keycloak";
import { Loading } from "@/pages/loading";
import { fetchServicePointWithMembers } from "@/services/service-points";
import { ServicePointWithMembers } from "@/types";
import { Home as HomeIcon, Hub as HubIcon } from "@mui/icons-material";
import { Alert, Card, CardContent, CardHeader, Container, Stack, Tab, Tabs } from "@mui/material";

import { ServicePointUsersList } from "@/containers/header/service-point-users/ServicePointUsersList";
import { ClientCredentialsPanel } from "./components/client-credentials/ClientCredentialsPanel";
import { useKeycloak } from "@/contexts/keycloak-context";
import { useQuery } from "@tanstack/react-query";
import { useParams, useSearchParams } from "react-router-dom";
import { ServicePointUpdateForm } from "./";
import { RefreshCcw } from "lucide-react";
import type { SyntheticEvent } from "react";

type ServicePointTab = "users" | "credentials";

export const ServicePoint = () => {
  const { isOperator, isServicePointAdminOf } = useAuthHelper();
  const { isInitialized, authenticated, token, tokenParsed } = useKeycloak();
  const { servicePointId } = useParams() as { servicePointId: string };
  const [searchParams, setSearchParams] = useSearchParams();

  const activeTab: ServicePointTab = searchParams.get("tab") === "credentials" ? "credentials" : "users";

  const handleTabChange = (_: SyntheticEvent, value: ServicePointTab) => {
    const next = new URLSearchParams(searchParams);
    next.set("tab", value);
    setSearchParams(next);
  };

  const getServicePoint = async () => {
    return await fetchServicePointWithMembers({
      id: +servicePointId,
      token: token || "",
    });
  };

  const servicePointQuery = useQuery<ServicePointWithMembers>({
    queryKey: ["servicePoints", servicePointId.toString()],
    queryFn: getServicePoint,
    enabled: isInitialized && authenticated,
  });

  if (servicePointQuery.isPending) {
    return <Loading />;
  }

  if (servicePointQuery.isError) {
    return <ErrorAlertComponent error="Service point could not be fetched" />;
  }

  // Use spaces as decimal separator for thousands
  const servicePointIdFormatted = `Service point ${new Intl.NumberFormat(
    "en-AU",
    {}
  )
    .format(+servicePointId)
    .replace(/,/g, " ")}`;

  const breadcrumbs: Breadcrumb[] = [
    {
      label: "Home",
      to: "/",
      icon: <HomeIcon />,
    },
    {
      label: "Service points",
      to: "/service-points",
      icon: <HubIcon />,
    },
    {
      label: servicePointIdFormatted,
      to: `/service-points/${servicePointId}`,
      icon: <HubIcon />,
    },
  ];

  const groupId = servicePointQuery.data.groupId;
  const canShowUsers =
    !!groupId && (isOperator || tokenParsed?.service_point_group_id === groupId);
  const canShowCredentials = !!groupId && (isOperator || isServicePointAdminOf(groupId));

  return (
    <Container sx={{pb:2}}>
      <Stack direction="column" gap={2}>
        <BreadcrumbsBar breadcrumbs={breadcrumbs} />
        <Card hidden={!isOperator} variant="outlined" sx={{ mt: 2 }}>
          <CardHeader title={<><RefreshCcw /> Update Service Point</>} />
          <CardContent>
            <ServicePointUpdateForm servicePoint={servicePointQuery.data!} />
          </CardContent>
        </Card>
        {canShowUsers && canShowCredentials ? (
          <Card>
            <Tabs value={activeTab} onChange={handleTabChange} sx={{ borderBottom: 1, borderColor: "divider", px: 1 }}>
              <Tab label="Users" value="users" />
              <Tab label="Client credentials" value="credentials" />
            </Tabs>
            <CardContent>
              {activeTab === "users" ? (
                <ServicePointUsersList servicePointWithMembers={servicePointQuery.data} />
              ) : (
                <ClientCredentialsPanel groupId={groupId as string} />
              )}
            </CardContent>
          </Card>
        ) : (
          <Card>
            <CardHeader title="Service point users" />
            <CardContent>
              {canShowUsers ? (
                <ServicePointUsersList servicePointWithMembers={servicePointQuery.data} />
              ) : (
                <Alert severity="warning">
                  No group id set or access not allowed
                </Alert>
              )}
            </CardContent>
          </Card>
        )}
      </Stack>
    </Container>
  );
};
