import { TextInputField } from "@/components/fields/TextInputField";
import { TextSelectField } from "@/components/fields/TextSelectField";
import generalMapping from "@/mapping/data/general-mapping.json";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import { useState } from "react";
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
  const contributorPositionOptions = generalMapping
    .filter((el) => el.field === "contributor.position.id")
    .map((el) => ({
      value: el.key,
      label: el.value,
    }));

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <TextSelectField
        options={contributorPositionOptions}
        name={`contributor.${parentIndex}.position.${index}.id`}
        label="Position"
        placeholder="Position"
        required={true}
        width={6}
      />
      <TextInputField
        name={`contributor.${parentIndex}.position.${index}.startDate`}
        label="Start Date"
        placeholder="Start Date"
        required={true}
        width={3}
      />
      <TextInputField
        name={`contributor.${parentIndex}.position.${index}.endDate`}
        label="End Date"
        placeholder="End Date"
        width={3}
      />
    </Grid>
  );
}

export function ContributorPositionDetailsForm({
  parentIndex,
  index,
  handleRemoveItem,
}: {
  parentIndex: number;
  index: number;
  handleRemoveItem: (index: number) => void;
}) {
  const key = `contributor.${parentIndex}.position.${index}`;
  const label = "Contributor Position";
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
