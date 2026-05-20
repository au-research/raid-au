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
import { useState, useContext, useLayoutEffect, useRef, useCallback, useMemo } from "react";
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
  const [isBulkVisible, setIsBulkVisible] = useState(false);
  const [highlightedFieldId, setHighlightedFieldId] = useState<string | null>(null);
  const [duplicateFormIndices, setDuplicateFormIndices] = useState<Set<number>>(new Set());
  const duplicateFormIndicesRef = useRef<Set<number>>(new Set());

  const { fields, append, remove } = useFieldArray({ control, name: key });
  const { formState, setError, clearErrors } = useFormContext();

  /**
   * IDs of accordions that are currently expanded.
   * - Initially loaded items are NOT in this set → collapsed by default.
   * - Bulk-added items are NOT added here → collapsed by default.
   * - Manually added items are added here via useLayoutEffect → start expanded.
   * - Error/duplicate items are added here explicitly → start expanded.
   * - User can toggle any item freely.
   */
  const [expandedIds, setExpandedIds] = useState<Set<string>>(() => new Set());
  const errorMessage = errors[key]?.message ?? (errors[key] as { root?: { message?: string } } | undefined)?.root?.message;

  // Always-current reference to fields — avoids stale closures in async callbacks.
  const fieldsRef = useRef(fields);
  fieldsRef.current = fields;

  // Tracks the field IDs seen in the previous render so useLayoutEffect can
  // detect which field was just manually added.
  const prevFieldIdsRef = useRef<Set<string>>(new Set(fields.map((f) => f.id)));

  // Set to true just before a manual append so the layout effect knows to expand it.
  const isManualAddRef = useRef(false);

  // Snapshot of field IDs taken just before each bulk append.
  // handleDuplicateIdentifiers uses this to avoid expanding newly-uploaded items —
  // only pre-existing items that match an uploaded DOI should be auto-expanded.
  const preBulkFieldIdsRef = useRef<Set<string>>(new Set(fields.map((f) => f.id)));

  // Field IDs that were auto-expanded by handleDuplicateIdentifiers (not by the user).
  // When the duplicate is resolved we collapse only these, not user-opened items.
  const duplicateExpandedIdsRef = useRef<Set<string>>(new Set());

  // Expand the single item that was just manually added (bulk items stay collapsed).
  useLayoutEffect(() => {
    const prevIds = prevFieldIdsRef.current;
    const newFields = fields.filter((f) => !prevIds.has(f.id));
    prevFieldIdsRef.current = new Set(fields.map((f) => f.id));

    if (newFields.length > 0 && isManualAddRef.current) {
      isManualAddRef.current = false;
      setExpandedIds((prev) => {
        const next = new Set(prev);
        newFields.forEach((f) => next.add(f.id));
        return next;
      });
    }
  }, [fields]);

  const { watch } = useFormContext();
  // Build a primitive key so useMemo only produces a new array when IDs actually
  // change. Using "\n" as separator — it cannot appear in valid DOI/URL values.
  const existingIdsKey = (watch("relatedObject") ?? [])
    .map((obj: { id?: string }) => obj.id ?? "")
    .filter((id: string) => id.length > 0)
    .join("\n");
  const existingIdentifiers: string[] = useMemo(
    () => (existingIdsKey.length === 0 ? [] : existingIdsKey.split("\n")),
    [existingIdsKey]
  );

  const handleDuplicateIdentifiers = useCallback((dois: string[]) => {
    const doiSet = new Set(dois.map((d) => d.trim().toLowerCase()));
    const indices = new Set<number>();
    existingIdentifiers.forEach((id, idx) => {
      if (doiSet.has(id.trim().toLowerCase())) indices.add(idx);
    });

    const prev = duplicateFormIndicesRef.current;

    // Clear stale duplicate errors from previously flagged indices
    prev.forEach((idx) => {
      if (!indices.has(idx)) {
        clearErrors(`relatedObject.${idx}.id` as `relatedObject.${number}.id`);
      }
    });

    // Set error on each newly duplicated field
    indices.forEach((idx) => {
      const doi = existingIdentifiers[idx];
      setError(`relatedObject.${idx}.id` as `relatedObject.${number}.id`, {
        type: "manual",
        message: `Duplicate URL - ${doi} is already included in the bulk upload.`,
      });
    });

    duplicateFormIndicesRef.current = indices;
    setDuplicateFormIndices(new Set(indices));

    // Collapse auto-expanded items that are no longer duplicates.
    const resolvedIndices = new Set([...prev].filter((idx) => !indices.has(idx)));
    const toCollapse = new Set<string>();
    fields.forEach((field, idx) => {
      if (resolvedIndices.has(idx) && duplicateExpandedIdsRef.current.has(field.id)) {
        toCollapse.add(field.id);
      }
    });
    if (toCollapse.size > 0) {
      duplicateExpandedIdsRef.current = new Set(
        [...duplicateExpandedIdsRef.current].filter((id) => !toCollapse.has(id))
      );
      setExpandedIds((prevExpanded) => {
        const next = new Set(prevExpanded);
        toCollapse.forEach((id) => next.delete(id));
        return next;
      });
    }

    // Only expand pre-existing items (those present before the bulk append)
    // that are newly detected as duplicates. Newly uploaded items stay collapsed.
    const newIndices = new Set([...indices].filter((idx) => !prev.has(idx)));
    const toExpand = new Set<string>();
    fields.forEach((field, idx) => {
      if (newIndices.has(idx) && preBulkFieldIdsRef.current.has(field.id)) {
        toExpand.add(field.id);
      }
    });
    if (toExpand.size > 0) {
      toExpand.forEach((id) => duplicateExpandedIdsRef.current.add(id));
      setExpandedIds((prevExpanded) => {
        const next = new Set(prevExpanded);
        toExpand.forEach((id) => next.add(id));
        return next;
      });
    }
  }, [existingIdentifiers, fields, setError, clearErrors]);

  const handleAddItem = () => {
    isManualAddRef.current = true;
    append(generator());
    trigger(key);
  };

  const handleBulkAddItems = async (objs: ParsedRelatedObject[]) => {
    // Snapshot current field IDs before appending so handleDuplicateIdentifiers
    // can distinguish pre-existing items from newly uploaded ones.
    preBulkFieldIdsRef.current = new Set(fields.map((f) => f.id));
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    append(objs as any);
  };

  const handleBulkComplete = useCallback(async () => {
    await trigger(key);
    // Expand any bulk-added fields that have validation errors.
    // fieldsRef.current is always the latest fields array (updated inline each render).
    const freshErrors = formState.errors.relatedObject as Record<number, unknown> | undefined;
    if (!freshErrors) return;
    setExpandedIds((prev) => {
      const next = new Set(prev);
      fieldsRef.current.forEach((field, index) => {
        if (freshErrors[index]) {
          next.add(field.id);
        }
      });
      return next;
    });
  }, [trigger, key, formState]);

  const handleRemoveItem = (fieldId: string, index: number) => {
    remove(index);
    trigger(key);
    setExpandedIds((prev) => {
      const next = new Set(prev);
      next.delete(fieldId);
      return next;
    });
  };

  const handleToggle = (fieldId: string) => {
    setExpandedIds((prev) => {
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
              const hasError = !!(errors.relatedObject?.[index]) || duplicateFormIndices.has(index);
              return (
              <Accordion
                key={field.id}
                expanded={expandedIds.has(field.id)}
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
                    {!expandedIds.has(field.id) && (
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

          {isBulkVisible && (
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
                    onClick={() => setIsBulkVisible(false)}
                    sx={{ position: "absolute", top: 8, right: 8 }}
                    aria-label="Close bulk upload"
                  >
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <BulkUploadComponent
                  addRelatedObjects={handleBulkAddItems}
                  onComplete={handleBulkComplete}
                  existingIdentifiers={existingIdentifiers}
                  onDuplicateIdentifiers={handleDuplicateIdentifiers}
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
          onClick={() => setIsBulkVisible(true)}
        >
          Upload Bulk {labelPlural}
        </Button>
      </CardActions>
    </Card>
  );
}
