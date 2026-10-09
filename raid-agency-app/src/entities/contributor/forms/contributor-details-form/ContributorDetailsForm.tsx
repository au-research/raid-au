import { DisplayItem } from "@/components/display-item";
import { CheckboxField } from "@/components/fields/CheckboxField";
import ORCIDLookup from "@/containers/orcid-lookup/ORCID";
import { Contributor } from "@/generated/raid";
import { useResolvedContributorName } from "@/hooks/useResolvedContributorName";
import { ISNI_SCHEMA_URI } from "@/utils/contributor-utils/contributor-identifier";
import { Edit as EditIcon, IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import React from "react";
import { useState } from "react";
import { useFormContext } from "react-hook-form";

function FieldGrid({ index, data }: { index: number; data: Contributor[] }) {
  const { setValue, getValues, formState: { errors } } = useFormContext();
  const isIsni = data?.[index]?.schemaUri === ISNI_SCHEMA_URI;
  // A `status` field is only ever assigned server-side once a Contributor has
  // been saved, so its presence is how a saved row is told apart from one
  // still being drafted in this editing session.
  const isExisting = !!data?.[index] && Object.hasOwn(data[index], "status");
  // RAID-920: an existing Contributor's identifier (ORCID or ISNI) starts out
  // read-only (confirm-who-this-is, not edit-by-default) - the pencil icon
  // below opts into the same editable widget a brand-new row always shows.
  const [isEditingIdentifier, setIsEditingIdentifier] = useState(false);
  const showEditableWidget = !isExisting || isEditingIdentifier;
  const identifierId: string | undefined = getValues(`contributor.${index}.id`);

  // Only resolve while the read-only display is actually showing, not while
  // the editable widget is.
  const resolvedName = useResolvedContributorName(identifierId, isIsni, !showEditableWidget);

  return (
    <Grid container spacing={2}>
      {showEditableWidget ? (
        <Grid item xs={12}>
          <ORCIDLookup
            mode="validation-only"
            defaultValue={identifierId}
            path={{ name: `contributor.${index}.id` }}
            formMethods={{ setValue, formState: { errors } }}
          />
        </Grid>
      ) : (
        <Grid item xs={12}>
          <Stack direction="row" alignItems="center" gap={1}>
            <DisplayItem
              label={isIsni ? "ISNI" : "ORCID"}
              value={`${identifierId} — ${resolvedName}`}
              width={11}
              testid="contributor-identifier-display"
            />
            <Tooltip title="Edit identifier">
              <IconButton
                aria-label="edit identifier"
                size="small"
                onClick={() => setIsEditingIdentifier(true)}
              >
                <EditIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        </Grid>
      )}

      <CheckboxField
        name={`contributor.${index}.leader`}
        label="Leader?"
        width={6}
      />
      <CheckboxField
        name={`contributor.${index}.contact`}
        label="Contact?"
        width={6}
      />
    </Grid>
  );
}

export function ContributorDetailsForm({
  index,
  handleRemoveItem,
  data,
}: {
  index: number;
  handleRemoveItem: (index: number) => void;
  data: Contributor[];
}) {
  const key = "contributor";
  const label = "Contributor";

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const { getValues } = useFormContext();

  const handleMouseEnter = () => setIsRowHighlighted(true);
  const handleMouseLeave = () => setIsRowHighlighted(false);

  return (
    <Stack gap={2}>
      <Typography variant="body2">
        <span
          style={{ textDecoration: isRowHighlighted ? "line-through" : "" }}
        >
          {getValues(`${key}.${index}.text`)
            ? getValues(`${key}.${index}.text`)
            : `${label} # ${index + 1}`}
        </span>
        {isRowHighlighted && " (to be deleted)"}
      </Typography>

      <Stack direction="row" alignItems="flex-start" gap={1}>
        <FieldGrid data={data} index={index} />

        <Tooltip title={`Remove ${label}`} placement="right">
          <IconButton
            aria-label="delete"
            color="error"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            onClick={() => {
              if (
                window.confirm(
                  `Are you sure you want to delete ${label} # ${index + 1} ?`
                )//ShortTerm Fix: Display the title of the item and its corresponding sequence number in the confirmation dialog
              ) {
                handleRemoveItem(index);
              }
            }}
          >
            <IndeterminateCheckBox />
          </IconButton>
        </Tooltip>
      </Stack>
    </Stack>
  );
}
