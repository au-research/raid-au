import { RelatedObjectCategoriesForm } from "@/entities/related-object-category/forms/related-object-categories-form";
import { relatedObjectDataGenerator } from "@/entities/related-object/data-generator/related-object-data-generator";
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
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import { Fragment, useState, useContext } from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
} from "react-hook-form";
import { RelatedObjectDetailsForm } from "@/entities/related-object/forms/related-object-details-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { BulkUploadComponent, ParsedRelatedObject } from "../../bulk-upload/Index";

type EntryMode = "manual" | "bulk";

export function RelatedObjectsForm({
  control,
  errors,
  trigger,
}: {
  control: Control<RaidDto>;
  errors: FieldErrors<RaidDto>;
  trigger: UseFormTrigger<RaidDto>;
}) {
  const key = "relatedObject";
  const label = "Related Object";
  const labelPlural = "Related Objects";
  const generator = relatedObjectDataGenerator;
  const DetailsForm = RelatedObjectDetailsForm;

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const [entryMode, setEntryMode] = useState<EntryMode>("manual");
  const { fields, append, remove } = useFieldArray({ control, name: key });
  const errorMessage = errors[key]?.message;

  const handleAddItem = () => {
    setEntryMode("manual");
    append(generator());
    trigger(key);
  };

  const handleBulkAddItem = async (obj: ParsedRelatedObject) => {
    console.log("Adding from bulk upload:", obj);
    append(obj);
    trigger(key);
  };

  const metadata = useContext(MetadataContext);
  const tooltip = metadata?.[key]?.tooltip;

  const handleModeChange = (
    _event: React.MouseEvent<HTMLElement>,
    newMode: EntryMode | null
  ) => {
    // MUI ToggleButtonGroup can return null if the same button is clicked
    if (newMode !== null) {
      setEntryMode(newMode);
    }
  };

  const showBulkUploadSection = () => {
    setEntryMode("bulk");
  }

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
                <RelatedObjectCategoriesForm
                  control={control}
                  errors={errors}
                  trigger={trigger}
                  parentIndex={index}
                />
              </Fragment>
            ))}
            {entryMode === "bulk" && (
              <Typography
                variant="body2"
                color="text.secondary"
                textAlign="center"
              >
                <BulkUploadComponent addRelatedObject={handleBulkAddItem}/>
              </Typography>
            )}
            
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
        <Button
          variant="outlined"
          color="success"
          size="small"
          startIcon={<AddBox />}
          sx={{ textTransform: "none", mt: 3 }}
          onClick={showBulkUploadSection}
        >
          Upload Bulk {labelPlural}
        </Button>
      </CardActions>
    </Card>
  );
}
