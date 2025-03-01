import packageJson from "@/../package.json";
import { DisplayItem } from "@/components/display-item";
import { CheckboxField } from "@/fields/CheckboxField";
import { TextInputField } from "@/fields/TextInputField";
import { Contributor } from "@/generated/raid";
import { Grid } from "@mui/material";

const ContributorForm = ({
  index,
  data,
}: {
  index: number;
  data:
    | (Contributor &
        {
          status?: string;
        }[])
    | Contributor[];
}) => {
  return (
    <Grid container spacing={2}>
      {packageJson.apiVersion === "2" && (
        <TextInputField
          name={`contributor.${index}.id`}
          label="ORCID URL"
          placeholder="ORCID URL"
          required={true}
          width={12}
        />
      )}
      {packageJson.apiVersion === "3" && (
        <>
          {(!data || !data[index] || !Object.hasOwn(data[index], "status")) && (
            <TextInputField
              name={`contributor.${index}.id`}
              label="ORCID ID"
              placeholder="ORCID ID"
              width={12}
            />
          )}
          {data[index] && Object.hasOwn(data[index], "status") && (
            <DisplayItem
              label="Contributor Status"
              value={"status" in data[index] ? data[index].status : ""}
              width={12}
            />
          )}
        </>
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
};

ContributorForm.displayName = "ContributorDetailsFormComponent";
export default ContributorForm;
