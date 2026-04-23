import { TextInputField } from "@/components/fields/TextInputField";
import { TextSelectField } from "@/components/fields/TextSelectField";
import generalMapping from "@/mapping/data/general-mapping.json";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip } from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useFormContext } from "react-hook-form";

function FieldGrid({
  index,
  isRowHighlighted,
}: {
  index: number;
  isRowHighlighted: boolean;
}) {
  const { setValue, watch, trigger } = useFormContext();
  const key = "relatedObject";
  const relatedObjectTypeOptions = useMemo(
    () =>
      generalMapping
        .filter((el) => el.field === "relatedObject.type.id")
        .map((el) => ({
          value: el.key,
          label: el.value,
        })),
    []
  );

  const idValue = watch(`relatedObject.${index}.id`);

  useEffect(() => {
    if (!idValue) return;

    if (idValue.includes("doi.org")) {
      setValue(`${key}.${index}.schemaUri`, "https://doi.org/");
      trigger(`${key}.${index}.schemaUri`);
    } else if (idValue.includes("web.archive.org")) {
      setValue(`${key}.${index}.schemaUri`, "https://web.archive.org/");
      trigger(`${key}.${index}.schemaUri`);
    }
  }, [idValue, index, setValue, trigger]);

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <TextInputField
        name={`relatedObject.${index}.id`}
        label="URL"
        helperText="Enter full DOI (https://doi.org/10.25955/abc-123) or web archive URL (https://web.archive.org/web/20220101000000/https://example.com)"
      />
      <TextSelectField
        options={relatedObjectTypeOptions}
        name={`relatedObject.${index}.type.id`}
        label="Type"
        placeholder="Type"
        required={true}
        width={4}
      />
    </Grid>
  );
}

export function RelatedObjectDetailsForm({
  index,
  handleRemoveItem,
  onHighlightChange,
}: {
  index: number;
  handleRemoveItem: (index: number) => void;
  onHighlightChange?: (highlighted: boolean) => void;
}) {
  const label = "Related Object";

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);

  const handleMouseEnter = () => {
    setIsRowHighlighted(true);
    onHighlightChange?.(true);
  };
  const handleMouseLeave = () => {
    setIsRowHighlighted(false);
    onHighlightChange?.(false);
  };

  return (
    <Stack gap={2}>
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

