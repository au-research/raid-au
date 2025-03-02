import { ErrorAlertComponent } from "@/components/error-alert-component";
import { ServicePointUsers } from "@/components/service-point-users";
import { ServicePointsTable } from "@/components/service-points-table";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Loading } from "@/pages/loading";
import { ServicePointCreateForm } from "@/pages/service-point";
import { fetchServicePointsWithMembers } from "@/services/service-points";
import { ServicePointWithMembers } from "@/types";
import { Card, CardContent, CardHeader, Stack } from "@mui/material";

import { useQuery } from "@tanstack/react-query";

export const ServicePointsOperatorView = () => {
  const { authenticated, isInitialized, token } = useKeycloak();

  const getServicePoints = async () => {
    return await fetchServicePointsWithMembers({
      token: token!,
    });
  };

  const query = useQuery<ServicePointWithMembers[]>({
    queryFn: getServicePoints,
    queryKey: ["servicePoints"],
    enabled: isInitialized && authenticated,
  });

  if (query.isPending) {
    return <Loading />;
  }

  if (query.isError) {
    return <ErrorAlertComponent error={query.error} />;
  }

  return (
    <Stack direction="column" gap={2}>
      <Card>
        <CardHeader title="Create new service point" />
        <CardContent>
          <ServicePointCreateForm />
        </CardContent>
      </Card>
      <Card>
        <CardHeader title="All service points" />
        <CardContent>
          <ServicePointsTable servicePoints={query.data} />
        </CardContent>
      </Card>
      <Card>
        <CardHeader title="Service points members" />
        <CardContent>
          <Stack direction="column" gap={2}>
            {query.data
              .filter((sp) => sp.members.length > 0)
              .map((servicePoint) => (
                <ServicePointUsers
                  key={servicePoint.id}
                  servicePointWithMembers={servicePoint}
                />
              ))}
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
};
