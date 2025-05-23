import { OrganisationLookupButton } from "@/containers/organisation-lookup/OrganisationLookupButton";
import { OrganisationLookupDialog } from "@/containers/organisation-lookup/OrganisationLookupDialog";
import { TextInputField } from "@/fields/TextInputField";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";

function FieldGrid({
  index,
  isRowHighlighted,
}: {
  index: number;
  isRowHighlighted: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [selectedValue, setSelectedValue] = useState<string | null>(null);
  const { setValue, getValues } = useFormContext();

  useEffect(() => {
    if (selectedValue) {
      setValue(`organisation.${index}.id`, selectedValue, {
        shouldValidate: true,
      });
    }
  }, [selectedValue, setValue, index]);

  const useOrganisationNames = () => {
    return useQuery({
      queryKey: ["organisationNames"],
      queryFn: () => {
        const stored = localStorage.getItem("organisationNames");
        return stored ? new Map(JSON.parse(stored)) : new Map();
      },
    });
  };

  const { data: organisationNames } = useOrganisationNames();

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <Grid item xs={12} sm={12}>
        <Typography variant="subtitle2" gutterBottom>
          Current value:{" "}
          {organisationNames?.size &&
            organisationNames?.get(getValues(`organisation.${index}.id`))
              ?.value}
        </Typography>
        <Stack direction="row" spacing={2} alignItems="center">
          <TextInputField
            name={`organisation.${index}.id`}
            label="ROR ID"
            placeholder="ROR ID"
            required={true}
            width={6}
          />
          <OrganisationLookupButton setOpen={setOpen} />
        </Stack>
      </Grid>
      <OrganisationLookupDialog
        open={open}
        setOpen={setOpen}
        setSelectedValue={setSelectedValue}
      />
    </Grid>
  );
}

export function OrganisationDetailsForm({
  index,
  handleRemoveItem,
}: {
  index: number;
  handleRemoveItem: (index: number) => void;
}) {
  const key = "organisation";
  const label = "Organisation";

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
