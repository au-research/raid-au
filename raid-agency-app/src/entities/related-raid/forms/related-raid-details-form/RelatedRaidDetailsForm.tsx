import { TextInputField } from "@/components/fields/TextInputField";
import { TextSelectField } from "@/components/fields/TextSelectField";
import { RelatedRaid } from "@/generated/raid";
import generalMapping from "@/mapping/data/general-mapping.json";
import { fetchRelatedRaidTitle } from "@/services/related-raid";
import { getLastTwoUrlSegments } from "@/utils/string-utils/string-utils";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useFormContext } from "react-hook-form";

function FieldGrid({
  index,
  isRowHighlighted,
}: {
  index: number;
  isRowHighlighted: boolean;
}) {
  const relatedRaidTypeOptions = generalMapping
    .filter((el) => el.field === "relatedRaid.type.schema")
    .map((el) => ({
      value: el.key,
      label: el.value,
    }));

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <TextInputField
        name={`relatedRaid.${index}.id`}
        label="Related RAiD"
        placeholder="Related RAiD"
        required={true}
        width={8}
      />

      <TextSelectField
        options={relatedRaidTypeOptions}
        name={`relatedRaid.${index}.type.id`}
        label="Type"
        placeholder="Type"
        required={true}
        width={4}
      />
    </Grid>
  );
}

export function RelatedRaidDetailsForm({
  index,
  handleRemoveItem,
}: {
  index: number;
  handleRemoveItem: (index: number) => void;
}) {
  const { getValues } = useFormContext();
  const key = "relatedRaid";
  const label = "Related RAiD";

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);

  const handleMouseEnter = () => setIsRowHighlighted(true);
  const handleMouseLeave = () => setIsRowHighlighted(false);

  const relatedRaids: RelatedRaid[] = getValues("relatedRaid");

  const segments = relatedRaids[index].id
    ? getLastTwoUrlSegments(relatedRaids[index].id!)
    : null;
  const [prefix, suffix] = segments ? segments.split("/") : ["", ""];

  const handle = `${prefix}/${suffix}`;

  const raidQuery = useQuery({
    queryKey: ["related-raid", handle],
    queryFn: () =>
      fetchRelatedRaidTitle({
        handle: handle!,
      }),
    enabled: !!handle,
  });

  return (
    <Stack gap={2}>
      <Typography variant="body2">
        <span
          style={{ textDecoration: isRowHighlighted ? "line-through" : "" }}
        >
          {raidQuery.isPending
            ? "Loading..."
            : raidQuery.isError
            ? `${label} # ${index + 1}`
            : raidQuery.data}
        </span>
        {isRowHighlighted && " (to be deleted)"}
      </Typography>

      <Stack direction="row" alignItems="flex-start" gap={1}>
        <FieldGrid index={index} isRowHighlighted={isRowHighlighted} />

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
