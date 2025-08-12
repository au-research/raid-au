import { contributorDataGenerator } from "@/entities/contributor/data-generator/contributor-data-generator";
import { Contributor, RaidDto } from "@/generated/raid";
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
import { Fragment, useContext, useState } from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
} from "react-hook-form";

import { ContributorPositionsForm } from "@/entities/contributor-position/forms/contributor-positions-form";
import { ContributorRolesForm } from "@/entities/contributor-role/forms/contributor-roles-form";
import { ContributorDetailsForm } from "@/entities/contributor/forms/contributor-details-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomTooltip } from "@/components/tooltips/ToolTip";

export function ContributorsForm({
  control,
  data,
  errors,
  trigger,
}: {
  control: Control<RaidDto>;
  data: Contributor[];
  errors: FieldErrors<RaidDto>;
  trigger: UseFormTrigger<RaidDto>;
}) {
  const key = "contributor";
  const label = "Contributor";
  const labelPlural = "Contributors";
  const generator = contributorDataGenerator;
  const DetailsForm = ContributorDetailsForm;

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const { fields, append, remove } = useFieldArray({ control, name: key });
  const errorMessage = errors[key]?.message;

  const handleAddItem = () => {
    append(generator());
    trigger(key);
  };
  const metadata = useContext(MetadataContext);
  const tooltip = metadata?.[key]?.tooltip;
  return (
    <Card
      sx={{
        borderLeft: errors[key] ? "3px solid" : "none",
        borderLeftColor: "error.main",
         overflow: "unset",
      }}
      id={key}
    >
       <Stack direction="row" alignItems="center">
          <CardHeader title={labelPlural} />
          <CustomTooltip
            title={label}
            content={tooltip}
            icon="info"
          />
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
                  data={data}
                />
                <ContributorPositionsForm
                  control={control}
                  errors={errors}
                  trigger={trigger}
                  parentIndex={index}
                />
                <ContributorRolesForm
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
