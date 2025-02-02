import OrganisationQueryButton from "@/components/organisation-query/OrganisationQueryButton";
import OrganisationQueryDialog from "@/components/organisation-query/OrganisationQueryDialog";
import { TextInputField } from "@/fields/TextInputField";
import { Button, Grid, Stack } from "@mui/material";
import { memo, useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";

const OrganisationForm = memo(({ index }: { index: number }) => {
  const [open, setOpen] = useState(false);
  const [selectedValue, setSelectedValue] = useState<string | null>(null);
  const { setValue } = useFormContext();

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
        <Stack direction="row" spacing={2} alignItems="center">
          <TextInputField
            name={`organisation.${index}.id`}
            label="ROR ID"
            placeholder="ROR ID"
            required={true}
            width={6}
          />
          <OrganisationQueryButton setOpen={setOpen} />
        </Stack>
      </Grid>
      <OrganisationQueryDialog
        open={open}
        setOpen={setOpen}
        setSelectedValue={setSelectedValue}
      />
    </Grid>
  );
});

OrganisationForm.displayName = "OrganisationDetailsFormComponent";
export default OrganisationForm;
