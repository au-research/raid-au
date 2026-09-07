import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import { ErrorAlertComponent } from "@/components/error-alert-component";
import { useAuthHelper } from "@/auth/keycloak";
import { Loading } from "@/pages/loading";
import { fetchServicePointWithMembers } from "@/services/service-points";
import { ServicePointWithMembers } from "@/types";
import { Group as GroupIcon, Home as HomeIcon, Hub as HubIcon, Key as KeyIcon } from "@mui/icons-material";
import {
  Alert,
  Box,
  Card,
  CardContent,
  Container,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Typography,
} from "@mui/material";

import { ServicePointUsersList } from "@/containers/header/service-point-users/ServicePointUsersList";
import { ClientCredentialsPanel } from "./components/client-credentials/ClientCredentialsPanel";
import { useKeycloak } from "@/contexts/keycloak-context";
import { useQuery } from "@tanstack/react-query";
import { useParams, useSearchParams } from "react-router-dom";
import { ServicePointUpdateForm } from "./";
import { RefreshCcw } from "lucide-react";
import type { ReactNode } from "react";

type SectionKey = "update" | "users" | "credentials";

interface Section {
  key: SectionKey;
  label: string;
  icon: ReactNode;
  content: ReactNode;
}

export const ServicePoint = () => {
  const { isOperator, isServicePointAdminOf } = useAuthHelper();
  const { isInitialized, authenticated, token, tokenParsed } = useKeycloak();
  const { servicePointId } = useParams() as { servicePointId: string };
  const [searchParams, setSearchParams] = useSearchParams();

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

  const sections: Section[] = [];
  if (isOperator) {
    sections.push({
      key: "update",
      label: "Update service point",
      icon: <RefreshCcw size={20} />,
      content: <ServicePointUpdateForm servicePoint={servicePointQuery.data!} />,
    });
  }
  if (canShowUsers) {
    sections.push({
      key: "users",
      label: "Users",
      icon: <GroupIcon />,
      content: <ServicePointUsersList servicePointWithMembers={servicePointQuery.data} />,
    });
  }
  if (canShowCredentials) {
    sections.push({
      key: "credentials",
      label: "Client credentials",
      icon: <KeyIcon />,
      content: <ClientCredentialsPanel groupId={groupId as string} />,
    });
  }

  const requestedKey = searchParams.get("tab");
  const activeSection = sections.find((section) => section.key === requestedKey) ?? sections[0];

  const handleSelectSection = (key: SectionKey) => {
    const next = new URLSearchParams(searchParams);
    next.set("tab", key);
    setSearchParams(next);
  };

  return (
    <Container sx={{ pb: 2 }}>
      <Stack direction="column" gap={2}>
        <BreadcrumbsBar breadcrumbs={breadcrumbs} />
        {sections.length === 0 ? (
          <Card>
            <CardContent>
              <Alert severity="warning">No group id set or access not allowed</Alert>
            </CardContent>
          </Card>
        ) : (
          <Card>
            <Box sx={{ display: "flex", alignItems: "stretch" }}>
              <Box sx={{ width: 240, flexShrink: 0, borderRight: 1, borderColor: "divider", py: 2 }}>
                <List component="nav">
                  {sections.map((section) => {
                    const selected = activeSection?.key === section.key;
                    return (
                      <ListItemButton
                        key={section.key}
                        selected={selected}
                        onClick={() => handleSelectSection(section.key)}
                        sx={{
                          borderLeft: 3,
                          borderColor: selected ? "primary.main" : "transparent",
                          "&.Mui-selected, &.Mui-selected:hover": {
                            backgroundColor: "action.selected",
                            color: "primary.main",
                          },
                        }}
                      >
                        <ListItemIcon sx={{ minWidth: 36, color: selected ? "primary.main" : "inherit" }}>
                          {section.icon}
                        </ListItemIcon>
                        <ListItemText primary={section.label} />
                      </ListItemButton>
                    );
                  })}
                </List>
              </Box>
              <Box sx={{ flex: 1, minWidth: 0, p: 3 }}>
                <Typography variant="h6" sx={{ mb: 2 }}>
                  {activeSection?.label}
                </Typography>
                {activeSection?.content}
              </Box>
            </Box>
          </Card>
        )}
      </Stack>
    </Container>
  );
};
