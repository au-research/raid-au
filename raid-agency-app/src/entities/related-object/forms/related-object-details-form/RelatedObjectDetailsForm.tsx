import { TextInputField } from "@/components/fields/TextInputField";
import { TextSelectField } from "@/components/fields/TextSelectField";
import generalMapping from "@/mapping/data/general-mapping.json";
import { inferRelatedObjectSchemaUri } from "@/utils/related-object-utils/related-object-schema-uri";
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
  const { setValue, watch, trigger, formState: { errors } } = useFormContext();
  const key = "relatedObject";

  // Read the id-field error directly — superRefine errors sometimes don't
  // propagate through useController's internal subscription.
  const idErrors = (errors?.relatedObject as Record<number, { id?: { message?: string } }> | undefined);
  const idErrorMessage = idErrors?.[index]?.id?.message;
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

    const schemaUri = inferRelatedObjectSchemaUri(idValue);
    if (schemaUri) {
      setValue(`${key}.${index}.schemaUri`, schemaUri);
      trigger(`${key}.${index}.schemaUri`);
    }
  }, [idValue, index, setValue, trigger]);

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <TextInputField
        name={`relatedObject.${index}.id`}
        label="URL"
        helperText="Enter full DOI, Handle, RRID, ARK, or web archive URL"
        errorText={idErrorMessage}
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

