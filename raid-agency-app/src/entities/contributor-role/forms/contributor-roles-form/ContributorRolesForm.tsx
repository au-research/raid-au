import { contributorRoleDataGenerator } from "@/entities/contributor-role/data-generator/contributor-role-data-generator";
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
import { useState } from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
} from "react-hook-form";
import { ContributorRoleDetailsForm } from "@/entities/contributor-role/forms/contributor-role-details-form";

export function ContributorRolesForm({
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
  const key = `role`;
  const label = "Role";
  const labelPlural = "Roles";
  const generator = contributorRoleDataGenerator;
  const DetailsForm = ContributorRoleDetailsForm;

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);

  const { fields, append, remove } = useFieldArray({
    control,
    name: `${parentKey}.${parentIndex}.${key}`,
  });

  const handleAddItem = () => {
    append(generator());
    trigger(`${parentKey}.${parentIndex}.${key}`);
  };
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
        >
          Add {label}
        </Button>
      </CardActions>
    </Card>
  );
}
