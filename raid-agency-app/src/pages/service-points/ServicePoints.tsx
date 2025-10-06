import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import { useAuthHelper } from "@/auth/keycloak";
import { Home as HomeIcon, Hub as HubIcon } from "@mui/icons-material";
import { Container, Stack } from "@mui/material";
import { ServicePointsGroupAdminView, ServicePointsOperatorView } from "./";

export const ServicePoints = () => {
  const { isOperator, isGroupAdmin } = useAuthHelper();

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
  ];

  return (
    <Container sx={{ pb: 2 }}>
      <Stack direction="column" gap={2}>
        <BreadcrumbsBar breadcrumbs={breadcrumbs} />
        {isOperator && <ServicePointsOperatorView />}
        {isGroupAdmin && !isOperator && <ServicePointsGroupAdminView />}
      </Stack>
    </Container>
  );
};
