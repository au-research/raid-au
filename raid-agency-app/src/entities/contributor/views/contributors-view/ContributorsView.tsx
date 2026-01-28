import { DisplayCard } from "@/components/display-card";
import { ErrorAlertComponent } from "@/components/error-alert-component";
import { NoItemsMessage } from "@/components/no-items-message";
import { ContributorItemView } from "@/entities/contributor/views/contributor-item-view";
import { Contributor } from "@/generated/raid";
import { fetchOrcidContributors } from "@/services/contributor";
import { Divider, Skeleton, Stack } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { memo } from "react";
import { useParams } from "react-router-dom";

interface ContributorWithStatus extends Contributor {
  uuid: string;
  status: string;
}

const ContributorsView = memo(
  ({ data }: { data: ContributorWithStatus[] }) => {
    const { prefix, suffix } = useParams<{ prefix: string; suffix: string }>();
    /* const orcidDataQuery = useQuery({
      queryFn: () => fetchOrcidContributors({ handle: `${prefix}/${suffix}` }),
      queryKey: ["orcid-contributors"],
    });

    if (orcidDataQuery.isPending) {
      return <Skeleton variant="rectangular" height={200} />;
    }

    if (orcidDataQuery.isError) {
      return <ErrorAlertComponent error="Error loading contributor details" />;
    } */

    const orcidData: any[] = [];
    const fetchCurrentOrcidData = (contributor: ContributorWithStatus) => {
      return "uuid" in contributor
        ? orcidData?.find(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (orcid: any) => orcid?.uuid === contributor.uuid && "name" in orcid
          )
        : null;
    };

    // Render contributors
    return (
      <DisplayCard
        data={data}
        labelPlural="Contributors"
        children={
          <>
          {data.length === 0 && <NoItemsMessage entity="contributor" />}
          <Stack gap={4} divider={<Divider />}>
            {data.map((contributor, index) => (
              <ContributorItemView
                key={contributor.uuid || contributor.id || index}
                contributor={contributor}
                orcidData={fetchCurrentOrcidData(contributor)}
                i={index}
              />
            ))}
          </Stack>
          </>
        }
      />
    );
  }
);

ContributorsView.displayName = "ContributorDisplay";

export { ContributorsView };
