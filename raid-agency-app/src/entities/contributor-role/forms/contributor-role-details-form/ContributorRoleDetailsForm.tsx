import { TextSelectField } from "@/components/fields/TextSelectField";
import contributorRole from "@/references/contributor_role.json";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import { useMemo, useState } from "react";
import { useFormContext } from "react-hook-form";

function FieldGrid({
  parentIndex,
  index,
  isRowHighlighted,
}: {
  parentIndex: number;
  index: number;
  isRowHighlighted: boolean;
}) {
  const contributorRoleReference = useMemo(
    () =>
      contributorRole.map((el) => ({
        value: el.uri,
        label: el.uri,
      })),
    []
  );

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <TextSelectField
        options={contributorRoleReference}
        name={`contributor.${parentIndex}.role.${index}.id`}
        label="Role"
        placeholder="Role"
        required={true}
        width={12}
      />
    </Grid>
  );
}

export function ContributorRoleDetailsForm({
  parentIndex,
  index,
  handleRemoveItem,
}: {
  parentIndex: number;
  index: number;
  handleRemoveItem: (index: number) => void;
}) {
  const key = `contributor.${parentIndex}.role.${index}`;
  const label = "Contributor Role";
  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const { getValues } = useFormContext();

  const handleMouseEnter = () => setIsRowHighlighted(true);
  const handleMouseLeave = () => setIsRowHighlighted(false);

  const currentValue = getValues(`${key}.text`);

  return (
    <Stack gap={2}>
      <Typography variant="body2">
        <span
          style={{ textDecoration: isRowHighlighted ? "line-through" : "" }}
        >
          {currentValue ? currentValue : `${label} # ${index + 1}`}
        </span>
        {isRowHighlighted && " (to be deleted)"}
      </Typography>
      <Stack direction="row" alignItems="flex-start" gap={1}>
        <FieldGrid
          parentIndex={parentIndex}
          index={index}
          isRowHighlighted={isRowHighlighted}
        />
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