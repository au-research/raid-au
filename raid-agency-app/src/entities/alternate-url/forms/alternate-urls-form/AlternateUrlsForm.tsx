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
import { useState, useContext } from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
} from "react-hook-form";
import { alternateUrlDataGenerator } from "@/entities/alternate-url/data-generator/alternate-url-data-generator";
import { AlternateUrlDetailsForm } from "@/entities/alternate-url/forms/alternate-url-details-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

export function AlternateUrlsForm({
  control,
  errors,
  trigger,
}: {
  control: Control<RaidDto>;
  errors: FieldErrors<RaidDto>;
  trigger: UseFormTrigger<RaidDto>;
}) {
  const key = "alternateUrl";
  const label = "Alternate URL";
  const labelPlural = "Alternate URLs";
  const generator = alternateUrlDataGenerator;
  const DetailsForm = AlternateUrlDetailsForm;

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
              <DetailsForm
                key={field.id}
                handleRemoveItem={() => remove(index)}
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
