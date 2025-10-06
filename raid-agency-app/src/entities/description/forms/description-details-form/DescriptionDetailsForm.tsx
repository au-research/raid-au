import LanguageSelector from "@/components/fields/LanguageSelector";
import { TextInputField } from "@/components/fields/TextInputField";
import { TextSelectField } from "@/components/fields/TextSelectField";
import generalMapping from "@/mapping/data/general-mapping.json";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import { useMemo, useState } from "react";
import { useFormContext } from "react-hook-form";

function FieldGrid({
  index,
  isRowHighlighted,
}: {
  index: number;
  isRowHighlighted: boolean;
}) {
  const descriptionTypeOptions = useMemo(
    () =>
      generalMapping
        .filter((el) => el.field === "description.type.id")
        .map((el) => ({
          value: el.key,
          label: el.value,
        })),
    []
  );

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <TextInputField
        name={`description.${index}.text`}
        label="Text"
        placeholder="Text"
        required={true}
        multiline={true}
        width={12}
      />
      <TextSelectField
        options={descriptionTypeOptions}
        name={`description.${index}.type.id`}
        label="Type"
        placeholder="Type"
        required={true}
        width={6}
      />
      <LanguageSelector name={`description.${index}.language.id`} width={6} />
    </Grid>
  );
}

export function DescriptionDetailsForm({
  index,
  handleRemoveItem,
}: {
  index: number;
  handleRemoveItem: (index: number) => void;
}) {
  const key = "description";
  const label = "Description";

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
          {label} #{index + 1}
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
                  `Are you sure you want to delete ${label} "${getValues(
                    `${key}.${index}.text`
                  )}"?`
                )
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
