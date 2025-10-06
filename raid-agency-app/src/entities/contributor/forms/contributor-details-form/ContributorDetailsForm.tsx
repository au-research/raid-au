import { DisplayItem } from "@/components/display-item";
import { CheckboxField } from "@/components/fields/CheckboxField";
import { TextInputField } from "@/components/fields/TextInputField";
import { Contributor } from "@/generated/raid";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import { useState } from "react";
import { useFormContext } from "react-hook-form";

function FieldGrid({ index, data }: { index: number; data: Contributor[] }) {
  return (
    <Grid container spacing={2}>
      {(!data || !data[index] || !Object.hasOwn(data[index], "status")) && (
        <TextInputField
          name={`contributor.${index}.id`}
          label="ORCID ID"
          placeholder="Full ORCID ID, e.g. https://orcid.org/0000-0000-0000-0000"
          width={12}
        />
      )}
      {data[index] && Object.hasOwn(data[index], "status") && (
        <DisplayItem
          label="Contributor Status"
          value={"status" in data[index] ? String((data[index] as any).status) : ""}
          width={12}
        />
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
        {/* <ContributorForm index={index} data={data} /> */}
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
