import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import { ErrorAlertComponent } from "@/components/error-alert-component";
import { ServicePointUsers } from "@/components/service-point-users";
import { useAuthHelper } from "@/keycloak";
import { Loading } from "@/pages/loading";
import { fetchServicePointWithMembers } from "@/services/service-points";
import { ServicePointWithMembers } from "@/types";
import { Home as HomeIcon, Hub as HubIcon } from "@mui/icons-material";
import { Alert, Card, CardContent, CardHeader, Container } from "@mui/material";

import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { ServicePointUpdateForm } from "./";
import { useKeycloak } from "@/contexts/keycloak-context";

export const ServicePoint = () => {
  const { isOperator } = useAuthHelper();
  const { isInitialized, authenticated, token, tokenParsed } = useKeycloak();
  const { servicePointId } = useParams() as { servicePointId: string };

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
    return <ErrorAlertComponent error={servicePointQuery.error} />;
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

  return (
    <Container>
      <BreadcrumbsBar breadcrumbs={breadcrumbs} />
      <Card hidden={!isOperator} variant="outlined" sx={{ mt: 2 }}>
        <CardHeader title="Update service point" />
        <CardContent>
          <ServicePointUpdateForm servicePoint={servicePointQuery.data!} />
        </CardContent>
      </Card>
      <Card>
        <CardHeader title="Service point users" />
        <CardContent>
          {(servicePointQuery.data.groupId &&
            (isOperator ||
              tokenParsed?.service_point_group_id ===
                servicePointQuery.data.groupId) && (
              <ServicePointUsers
                servicePointWithMembers={servicePointQuery.data}
              />
            )) || (
            <Alert severity="warning">
              No group id set or access not allowed
            </Alert>
          )}
        </CardContent>
      </Card>
    </Container>
  );
};
