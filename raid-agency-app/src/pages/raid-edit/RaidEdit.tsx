import packageJson from "@/../package.json";
import type { Breadcrumb } from "@/components/breadcrumbs-bar";
import { BreadcrumbsBar } from "@/components/breadcrumbs-bar";
import { ErrorAlertComponent } from "@/components/error-alert-component";
import { useErrorDialog } from "@/components/error-dialog";
import { RaidForm } from "@/components/raid-form";
import { RaidFormErrorMessage } from "@/components/raid-form-error-message";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Contributor, RaidCreateRequest, RaidDto } from "@/generated/raid";
import { Loading } from "@/pages/loading";
import { fetchOneRaid, updateOneRaid } from "@/services/raid";
import { fetchServicePoints } from "@/services/service-points";
import { raidRequest } from "@/utils/data-utils";
import {
  DocumentScanner as DocumentScannerIcon,
  Edit as EditIcon,
  HistoryEdu as HistoryEduIcon,
  Home as HomeIcon,
} from "@mui/icons-material";
import { Container, Stack } from "@mui/material";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";

function createEditRaidPageBreadcrumbs({
  prefix,
  suffix,
}: {
  prefix: string;
  suffix: string;
}): Breadcrumb[] {
  return [
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
      label: `RAiD ${prefix}/${suffix}`,
      to: `/raids/${prefix}/${suffix}`,
      icon: <DocumentScannerIcon />,
    },
    {
      label: `Edit`,
      to: `/raids/${prefix}/${suffix}/edit`,
      icon: <EditIcon />,
    },
  ];
}

export const RaidEdit = () => {
  const { openErrorDialog } = useErrorDialog();
  const { authenticated, isInitialized, token, tokenParsed } = useKeycloak();
  const navigate = useNavigate();

  const { prefix, suffix } = useParams() as { prefix: string; suffix: string };
  if (!prefix || !suffix) {
    throw new Error("prefix and suffix are required");
  }

  const query = useQuery<RaidDto>({
    queryKey: useMemo(() => ["raids", prefix, suffix], [prefix, suffix]),
    queryFn: () =>
      fetchOneRaid({
        handle: `${prefix}/${suffix}`,
        token: token!,
      }),
    enabled: isInitialized && authenticated,
  });

  const servicePointsQuery = useQuery({
    queryKey: ["service-points"],
    queryFn: () =>
      fetchServicePoints({
        token: token!,
      }),
    enabled: isInitialized && authenticated,
  });

  useEffect(() => {
    if (
      !servicePointsQuery.isLoading &&
      servicePointsQuery.data &&
      query.data
    ) {
      const disabledServicePoints = servicePointsQuery.data
        .filter((servicePoint) => servicePoint.appWritesEnabled === false)
        .map((el) => el.id);

      const servicePoint = query.data.identifier.owner.servicePoint;
      if (servicePoint && disabledServicePoints.includes(servicePoint)) {
        alert(
          "Editing RAiDs on this service point is disabled. Redirecting to home page."
        );
        navigate(`/raids/${prefix}/${suffix}`);
      }
    }
  }, [
    servicePointsQuery.isLoading,
    servicePointsQuery.data,
    query.data,
    prefix,
    suffix,
    navigate,
  ]);

  const updateMutation = useMutation({
    mutationFn: updateOneRaid,
    onSuccess: () => {
      navigate(`/raids/${prefix}/${suffix}`);
    },
    onError: (error: Error) => {
      RaidFormErrorMessage(error, openErrorDialog);
    },
  });

  const handleSubmit = async (data: RaidDto) => {
    updateMutation.mutate({
      data: raidRequest(data),
      handle: `${prefix}/${suffix}`,
      token: token!,
    });
  };

  if (query.isPending) {
    return <Loading />;
  }

  if (query.isError) {
    return <ErrorAlertComponent error="RAiD could not be fetched" />;
  }

  if (servicePointsQuery.isPending) {
    return <Loading />;
  }

  if (servicePointsQuery.isError) {
    return <ErrorAlertComponent error="Service points could not be fetched" />;
  }

  let contributors: (Contributor & { email?: string | undefined })[] = [];

  if (packageJson.apiVersion === "3") {
    for (const contributor of query.data?.contributor ?? []) {
      const updatedContributor = {
        ...contributor,
        email: (contributor as { email?: string }).email || "",
      };
      contributors.push(updatedContributor);
    }
  }

  let raidData: RaidDto | RaidCreateRequest;

  if (packageJson.apiVersion === "3") {
    raidData = {
      ...query.data,
      contributor: contributors,
    };
  } else {
    raidData = query.data;
  }

  return (
    <Container
      maxWidth="lg"
      sx={{
        pb: 20,
      }}
    >
      <Stack gap={2}>
        <BreadcrumbsBar
          breadcrumbs={createEditRaidPageBreadcrumbs({
            prefix,
            suffix,
          })}
        />
        <RaidForm
          prefix={prefix}
          suffix={suffix}
          raidData={raidData}
          onSubmit={handleSubmit}
          isSubmitting={updateMutation.isPending}
        />
      </Stack>
    </Container>
  );
};
