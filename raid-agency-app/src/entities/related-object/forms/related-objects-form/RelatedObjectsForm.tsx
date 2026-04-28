import { RelatedObjectCategoriesForm } from "@/entities/related-object-category/forms/related-object-categories-form";
import { relatedObjectDataGenerator } from "@/entities/related-object/data-generator/related-object-data-generator";
import { RaidDto } from "@/generated/raid";
import { AddBox, Close as CloseIcon, ExpandMore, ErrorOutline as ErrorIcon } from "@mui/icons-material";
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
  IconButton,
  Paper,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import { useState, useContext, useLayoutEffect, useRef, useCallback } from "react";
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

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const [entryMode, setEntryMode] = useState<EntryMode>("manual");
  const [highlightedFieldId, setHighlightedFieldId] = useState<string | null>(null);

  const { fields, append, remove } = useFieldArray({ control, name: key });
  const { formState } = useFormContext();

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
  const errorMessage = errors[key]?.message ?? (errors[key] as { root?: { message?: string } } | undefined)?.root?.message;

  // Track the set of field IDs seen in the previous render so we can
  // detect which fields are newly added after each append().
  const prevFieldIdsRef = useRef<Set<string>>(new Set(fields.map((f) => f.id)));

  // Bulk-added items start collapsed; manually added items start expanded.
  useLayoutEffect(() => {
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

  const handleBulkAddItems = async (objs: ParsedRelatedObject[]) => {
    // Single append call with the full array — one React re-render instead of N.
    // entryMode is "bulk" so the useEffect above will collapse all new items.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    append(objs as any);
  };

  const handleBulkComplete = useCallback(async () => {
    await trigger(key);
    // Read errors fresh from form context after trigger resolves — using the
    // prop here would be a stale closure from before trigger ran.
    const freshErrors = formState.errors.relatedObject as Record<number, unknown> | undefined;
    setCollapsedIds((prev) => {
      const next = new Set(prev);
      fields.forEach((field, index) => {
        if (freshErrors?.[index]) {
          next.delete(field.id);
        }
      });
      return next;
    });
  }, [trigger, key, fields, formState]);

  const handleRemoveItem = (fieldId: string, index: number) => {
    remove(index);
    trigger(key);
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
            {fields.map((field, index) => {
              const hasError = !!(errors.relatedObject?.[index]);
              return (
              <Accordion
                key={field.id}
                expanded={!collapsedIds.has(field.id)}
                onChange={() => handleToggle(field.id)}
                disableGutters
                sx={{
                  border: "1px solid",
                  borderColor: hasError ? "error.main" : "divider",
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
                    {hasError && (
                      <ErrorIcon color="error" fontSize="small" sx={{ mr: 0.5, flexShrink: 0 }} />
                    )}
                    <Typography
                      variant="body2"
                      fontWeight={500}
                      noWrap
                      sx={{
                        textDecoration: highlightedFieldId === field.id ? "line-through" : "none",
                        color: highlightedFieldId === field.id ? "error.main" : hasError ? "error.main" : "inherit",
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
                    <RelatedObjectDetailsForm
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
            );
            })}
          </Stack>

          {errorMessage && (
            <Typography variant="body2" color="error" textAlign="center">
              {errorMessage}
            </Typography>
          )}

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
                sx={{ p: 2, borderRadius: 2, bgcolor: "background.default", position: "relative" }}
              >
                <Tooltip title="Close bulk upload" placement="left">
                  <IconButton
                    size="small"
                    onClick={() => setEntryMode("manual")}
                    sx={{ position: "absolute", top: 8, right: 8 }}
                    aria-label="Close bulk upload"
                  >
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <BulkUploadComponent
                  addRelatedObjects={handleBulkAddItems}
                  onComplete={handleBulkComplete}
                />
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
          onClick={() => setEntryMode("bulk")}
        >
          Upload Bulk {labelPlural}
        </Button>
      </CardActions>
    </Card>
  );
}
