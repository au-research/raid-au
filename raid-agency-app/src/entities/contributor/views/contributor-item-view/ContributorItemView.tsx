import { DisplayItem } from "@/components/display-item";
import { ContributorPositionItemView } from "@/entities/contributor-position/views/contributor-position-item-view";
import { ContributorRoleItemView } from "@/entities/contributor-role/views/contributor-role-item-view";
import { Contributor } from "@/generated/raid";
import { Divider, Grid, Skeleton, Stack, Typography } from "@mui/material";
import { memo } from "react";
import { OrcidButton } from "@/components/orcid-button";

interface ContributorWithStatus extends Contributor {
  uuid: string;
  status: string;
}

const ContributorItemView = memo(
  ({
    contributor,
    orcidData,
    i,
  }: {
    contributor: ContributorWithStatus;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    orcidData?: any;
    i: number;
  }) => {
    return (
      <Stack gap={2}>
        <Typography variant="body1">Contributor #{i + 1}</Typography>

        <Stack direction="row" alignItems="center" gap={1}>
          <OrcidButton contributor={contributor} orcidData={orcidData} />
        </Stack>

        <Grid container spacing={2}>
          <DisplayItem label="ORCID" value={contributor.id} width={6} />

          <DisplayItem
            label="Leader"
            value={contributor.leader ? "Yes" : "No"}
            width={2}
          />
          <DisplayItem
            label="Contact"
            value={contributor.contact ? "Yes" : "No"}
            width={2}
          />
        </Grid>

        <Stack gap={2} sx={{ pl: 3 }}>
          <Stack direction="row" alignItems="baseline">
            <Typography variant="body1" sx={{ mr: 0.5 }}>
              Roles
            </Typography>
            <Typography variant="caption" color="text.disabled">
              Contributor #{i + 1}
            </Typography>
          </Stack>
          <Grid container spacing={1}>
            {contributor?.role ? (
              contributor.role
                .sort((a, b) => a.id.localeCompare(b.id))
                .map((role, index) => (
                  <ContributorRoleItemView
                    key={`contributor-${i}-role-${index}`}
                    contributorRole={role}
                  />
                ))
            ) : (
              <Grid item xs={12}>
                <Skeleton variant="rectangular" height={40} />
              </Grid>
            )}
          </Grid>

          <Stack direction="row" alignItems="baseline">
            <Typography variant="body1" sx={{ mr: 0.5 }}>
              Positions
            </Typography>
            <Typography variant="caption" color="text.disabled">
              Contributor #{i + 1}
            </Typography>
          </Stack>
          <Stack gap={2} divider={<Divider />}>
            {contributor?.position ? (
              contributor.position.map((position, index) => (
                <ContributorPositionItemView
                  key={`contributor-${i}-position-${index}`}
                  contributorPosition={position}
                />
              ))
            ) : (
              <Skeleton variant="rectangular" height={60} />
            )}
          </Stack>
        </Stack>
      </Stack>
    );
  }
);

ContributorItemView.displayName = "ContributorItemView";

export { ContributorItemView };
