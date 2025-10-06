import { useKeycloak } from "@/contexts/keycloak-context";
import { organisationDataGenerator } from "@/entities/organisation/data-generator/organisation-data-generator";
import { RaidDto } from "@/generated/raid";
import { AddBox } from "@mui/icons-material";
import {
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  Divider,
  Stack,
  Typography,
} from "@mui/material";
import { Fragment, useState, useContext} from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
} from "react-hook-form";
import { OrganisationRolesForm } from "@/entities/organisation-role/forms/organisation-roles-form";
import { OrganisationDetailsForm } from "@/entities/organisation/forms/organisation-details-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

export function OrganisationsForm({
  control,
  errors,
  trigger,
}: {
  control: Control<RaidDto>;
  errors: FieldErrors<RaidDto>;
  trigger: UseFormTrigger<RaidDto>;
}) {
  const { tokenParsed, token } = useKeycloak();

  const key = "organisation";
  const label = "Organisation";
  const labelPlural = "Organisations";
  const generator = organisationDataGenerator;
  const DetailsForm = OrganisationDetailsForm;

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const { fields, append, remove } = useFieldArray({ control, name: key });
  const errorMessage = errors[key]?.message;

  const handleAddItem = async () => {
    if (!token || !tokenParsed) {
      console.error("Token or tokenParsed is undefined");
      return;
    }
    append(await generator({ token, tokenParsed }));
    trigger(key);
  };
  const metadata = useContext(MetadataContext);
  const tooltip = metadata?.[key]?.tooltip;
  return (
    <Card
      sx={{
        borderLeft: errors[key] ? "3px solid" : "none",
        borderLeftColor: "error.main",
      }}
      id={key}
    >
      <Stack direction="row" alignItems="center">
        <CardHeader sx={{padding: "16px 0 16px 16px"}} title={labelPlural} />
        <CustomStyledTooltip
          title={label}
          content={tooltip || ""}
          variant="info"
          placement="top"
          tooltipIcon={<InfoOutlinedIcon />}
        >
        </CustomStyledTooltip>
      </Stack>
      <CardContent>
        <Stack gap={2} className={isRowHighlighted ? "add" : ""}>
          {errorMessage && (
            <Typography variant="body2" color="error" textAlign="center">
              {errorMessage}
            </Typography>
          )}

          {fields.length === 0 && (
            <Typography
              variant="body2"
              color="text.secondary"
              textAlign="center"
            >
              No {labelPlural} defined
            </Typography>
          )}

          <Stack divider={<Divider />} gap={2} data-testid={`${key}-form`}>
            {fields.map((field, index) => (
              <Fragment key={field.id}>
                <DetailsForm
                  key={field.id}
                  handleRemoveItem={() => remove(index)}
                  index={index}
                />
                <OrganisationRolesForm
                  control={control}
                  errors={errors}
                  trigger={trigger}
                  parentIndex={index}
                />
              </Fragment>
            ))}
          </Stack>
        </Stack>
      </CardContent>

      <CardActions>
        <Button
          variant="outlined"
          color="success"
          size="small"
          startIcon={<AddBox />}
          sx={{ textTransform: "none", mt: 3 }}
          onClick={handleAddItem}
          onMouseEnter={() => setIsRowHighlighted(true)}
          onMouseLeave={() => setIsRowHighlighted(false)}
        >
          Add {label}
        </Button>
      </CardActions>
    </Card>
  );
}
