import OrganisationLookupButton from "@/components/organisation-lookup/OrganisationLookupButton";
import OrganisationLookupDialog from "@/components/organisation-lookup/OrganisationLookupDialog";
import { TextInputField } from "@/fields/TextInputField";
import { Grid, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { memo, useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";

const OrganisationForm = memo(({ index }: { index: number }) => {
  const [open, setOpen] = useState(false);
  const [selectedValue, setSelectedValue] = useState<string | null>(null);
  const { setValue, getValues } = useFormContext();

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

  useEffect(() => {
    if (selectedValue) {
      setValue(`organisation.${index}.id`, selectedValue, {
        shouldValidate: true,
      });
    }
  }, [selectedValue, setValue, index]);
  return (
    <Grid container spacing={2}>
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
});

OrganisationForm.displayName = "OrganisationDetailsFormComponent";
export default OrganisationForm;
