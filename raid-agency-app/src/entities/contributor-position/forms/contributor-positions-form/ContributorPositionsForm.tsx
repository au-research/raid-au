import { RaidDto, ContributorPosition } from "@/generated/raid";
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
import { useState } from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
  useFormContext
} from "react-hook-form";
import { contributorPositionDataGenerator } from "@/entities/contributor-position/data-generator/contributor-position-data-generator";
import { ContributorPositionDetailsForm } from "@/entities/contributor-position/forms/contributor-position-details-form";
import { SubSections, useEnableAddFields } from "@/shared/hooks/business-rules/useEnableAddFields";

export function ContributorPositionsForm({
  control,
  errors,
  trigger,
  parentIndex,
}: {
  control: Control<RaidDto>;
  errors: FieldErrors<RaidDto>;
  trigger: UseFormTrigger<RaidDto>;
  parentIndex: number;
}) {
  const parentKey = `contributor`;
  const key = `position`;
  const label = "Position";
  const labelPlural = "Positions";
  const generator = contributorPositionDataGenerator;
  const DetailsForm = ContributorPositionDetailsForm;

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);

  const { fields, append, remove } = useFieldArray({
    control,
    name: `${parentKey}.${parentIndex}.${key}`,
  });

  const handleAddItem = () => {
    append(generator());
    trigger(`${parentKey}.${parentIndex}.${key}`);
  };
  const { formState, watch } = useFormContext<RaidDto>();
  const positions = watch([`${parentKey}.${parentIndex}.${key}`]) as ContributorPosition[][];
  const isDirty = formState.isDirty;

  const hasError = errors[parentKey]?.[parentIndex]?.[key];
  return (
    <Card
      sx={{
        borderLeft: hasError ? "3px solid" : "1px solid",
        borderLeftColor: hasError ? "error.main" : "divider",
      }}
      variant="outlined"
    >
      <CardHeader title={labelPlural} />
      <CardContent>
        <Stack gap={2} className={isRowHighlighted ? "add" : ""}>
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
              <DetailsForm
                key={field.id}
                handleRemoveItem={() => remove(index)}
                parentIndex={parentIndex}
                index={index}
              />
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
          disabled={useEnableAddFields(positions as unknown as SubSections, isDirty)}
        >
          Add {label}
        </Button>
      </CardActions>
    </Card>
  );
}
