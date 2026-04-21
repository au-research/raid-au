import { RelatedObjectCategoriesForm } from "@/entities/related-object-category/forms/related-object-categories-form";
import { relatedObjectDataGenerator } from "@/entities/related-object/data-generator/related-object-data-generator";
import { RaidDto } from "@/generated/raid";
import { AddBox, ExpandMore } from "@mui/icons-material";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  Chip,
  Divider,
  Paper,
  Stack,
  Typography,
} from "@mui/material";
import { useState, useContext, useEffect, useRef } from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
  useFormContext,
} from "react-hook-form";
import { RelatedObjectDetailsForm } from "@/entities/related-object/forms/related-object-details-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { BulkUploadComponent, ParsedRelatedObject } from "../../bulk-upload/Index";

type EntryMode = "manual" | "bulk";

/**
 * Reads the current DOI/URL value for a given index and displays it
 * in the accordion summary. Uses useFormContext so it must be rendered
 * inside a FormProvider.
 */
function RelatedObjectSummaryLabel({ index }: { index: number }) {
  const { watch } = useFormContext();
  const url: string = watch(`relatedObject.${index}.id`) ?? "";

  return (
    <Typography
      variant="body2"
      color="text.secondary"
      sx={{
        ml: 1.5,
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap",
        maxWidth: 480,
      }}
    >
      {url || "No URL set"}
    </Typography>
  );
}

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
  const [highlightedFieldId, setHighlightedFieldId] = useState<string | null>(null);

  const { fields, append, remove } = useFieldArray({ control, name: key });

  /**
   * IDs of accordions that are currently collapsed.
   * - Initially loaded items are collapsed (seeded from fields at mount).
   * - Bulk-added items are collapsed.
   * - Manually added items start expanded.
   * - User can toggle any item freely.
   */
  const [collapsedIds, setCollapsedIds] = useState<Set<string>>(
    () => new Set(fields.map((f) => f.id))
  );
  const errorMessage = errors[key]?.message;

  // Track the set of field IDs seen in the previous render so we can
  // detect which fields are newly added after each append().
  const prevFieldIdsRef = useRef<Set<string>>(new Set(fields.map((f) => f.id)));

  // Bulk-added items start collapsed; manually added items start expanded.
  useEffect(() => {
    const prevIds = prevFieldIdsRef.current;
    const newFields = fields.filter((f) => !prevIds.has(f.id));

    if (newFields.length > 0 && entryMode === "bulk") {
      setCollapsedIds((prev) => {
        const next = new Set(prev);
        newFields.forEach((f) => next.add(f.id));
        return next;
      });
    }

    prevFieldIdsRef.current = new Set(fields.map((f) => f.id));
  }, [fields, entryMode]);

  const handleAddItem = () => {
    setEntryMode("manual"); // hide bulk upload panel if open
    append(generator());
    trigger(key);
  };

  const handleBulkAddItem = async (obj: ParsedRelatedObject) => {
    // entryMode is already "bulk" — the useEffect above will collapse the new item
    append(obj);
    trigger(key);
  };

  const handleRemoveItem = (fieldId: string, index: number) => {
    remove(index);
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      next.delete(fieldId);
      return next;
    });
  };

  const handleToggle = (fieldId: string) => {
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      if (next.has(fieldId)) {
        next.delete(fieldId);
      } else {
        next.add(fieldId);
      }
      return next;
    });
  };

  const metadata = useContext(MetadataContext);
  const tooltip = metadata?.[key]?.tooltip;

  const showBulkUploadSection = () => {
    setEntryMode("bulk");
  };

  return (
    <Card
      sx={{
        borderLeft: errors[key] ? "3px solid" : "none",
        borderLeftColor: "error.main",
      }}
      id={key}
    >
      <Stack direction="row" alignItems="center">
        <CardHeader
          sx={{ padding: "16px 0 16px 16px" }}
          title={
            <Stack direction="row" alignItems="center" gap={1}>
              {labelPlural}
              {fields.length > 0 && (
                <Chip label={fields.length} size="small" />
              )}
            </Stack>
          }
        />
        <CustomStyledTooltip
          title={label}
          content={tooltip || ""}
          variant="info"
          placement="top"
          tooltipIcon={<InfoOutlinedIcon />}
        />
      </Stack>

      <CardContent>
        <Stack gap={2}>
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

          <Stack gap={1} data-testid={`${key}-form`}>
            {fields.map((field, index) => (
              <Accordion
                key={field.id}
                expanded={!collapsedIds.has(field.id)}
                onChange={() => handleToggle(field.id)}
                disableGutters
                sx={{
                  border: "1px solid",
                  borderColor: "divider",
                  borderRadius: 1,
                  "&:before": { display: "none" },
                }}
              >
                <AccordionSummary expandIcon={<ExpandMore />}>
                  <Stack
                    direction="row"
                    alignItems="center"
                    sx={{ width: "100%", overflow: "hidden", pr: 1 }}
                  >
                    <Typography
                      variant="body2"
                      fontWeight={500}
                      noWrap
                      sx={{
                        textDecoration: highlightedFieldId === field.id ? "line-through" : "none",
                        color: highlightedFieldId === field.id ? "error.main" : "inherit",
                      }}
                    >
                      {label} #{index + 1}
                    </Typography>
                    {collapsedIds.has(field.id) && (
                      <RelatedObjectSummaryLabel index={index} />
                    )}
                  </Stack>
                </AccordionSummary>

                <AccordionDetails>
                  <Stack gap={2}>
                    <DetailsForm
                      key={field.id}
                      handleRemoveItem={() => handleRemoveItem(field.id, index)}
                      index={index}
                      onHighlightChange={(highlighted) =>
                        setHighlightedFieldId(highlighted ? field.id : null)
                      }
                    />
                    <RelatedObjectCategoriesForm
                      control={control}
                      errors={errors}
                      trigger={trigger}
                      parentIndex={index}
                    />
                  </Stack>
                </AccordionDetails>
              </Accordion>
            ))}
          </Stack>

          <Box
            className={isRowHighlighted ? "add" : ""}
            sx={{
              minHeight: 40,
              borderRadius: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {isRowHighlighted && (
              <Typography variant="body2" color="success.dark">
                New {label} will be added here
              </Typography>
            )}
          </Box>

          {entryMode === "bulk" && (
            <>
              <Divider>
                <Chip label="Bulk Upload" size="small" variant="outlined" />
              </Divider>
              <Paper
                variant="outlined"
                sx={{ p: 2, borderRadius: 2, bgcolor: "background.default" }}
              >
                <BulkUploadComponent addRelatedObject={handleBulkAddItem} />
              </Paper>
            </>
          )}
        </Stack>
      </CardContent>

      <CardActions sx={{ pt: 0 }}>
        <Button
          variant="outlined"
          color="success"
          size="small"
          startIcon={<AddBox />}
          sx={{ textTransform: "none" }}
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
          sx={{ textTransform: "none" }}
          onClick={showBulkUploadSection}
        >
          Upload Bulk {labelPlural}
        </Button>
      </CardActions>
    </Card>
  );
}
