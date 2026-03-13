import React, { useEffect } from "react";
import { subjectDataGenerator } from "@/entities/subject/data-generator/subject-data-generator";
import { RaidDto } from "@/generated/raid";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  Stack,
  Typography,
} from "@mui/material";
import { Fragment, useState, useContext } from "react";
import {
  Control,
  FieldErrors,
  UseFormTrigger,
  useFieldArray,
  useFormContext,
} from "react-hook-form";
import { SubjectKeywordsForm } from "@/entities/subject-keyword/forms/subject-keywords-form/";
import { SubjectDetailsForm } from "@/entities/subject/forms/subject-details-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { useCodesContext } from "@/components/tree-view/context/CodesContext";
import { TransformCodes } from "@/utils/transformer/TransformCodes";
import CustomizedTreeViewWithSelection from "@/components/tree-view/TreeView";
import DropDown from "@/components/dropdown-select/DropDown";
import CustomisedInputBase from "@/components/custom-text-field/CustomisedInputBase";
import { RotateCcw, Search, Plus, Delete, Check } from "lucide-react";
import { CodeItem } from "@/components/tree-view/context/CodesProvider";
import CustomizedDialogs from "@/components/alert-dialog/alert-dialog";

export function SubjectsForm({
  control,
  errors,
  trigger,
}: {
  control: Control<RaidDto>;
  errors: FieldErrors<RaidDto>;
  trigger: UseFormTrigger<RaidDto>;
}) {
  const key = "subject";
  const label = "Subject";
  const labelPlural = "Subjects";
  const generator = subjectDataGenerator;
  const DetailsForm = SubjectDetailsForm;

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const { fields, prepend, remove } = useFieldArray({ control, name: key });
  const { clearErrors, getValues } = useFormContext();
  const errorMessage = errors[key]?.message;
  const {
    codesData,
    setCodesData,
    subjectType,
    setSubjectType,
    getSubjectTypes,
    setSearchQuery,
    searchQuery,
    filterCodesBySearch,
    resetState,
    getSelectedCodesData,
    selectedCodesData,
    selectedCodes,
    removeFromSubjects,
    confirmationNeeded,
    modifySubjectSelection,
    setConfirmationNeeded,
    restoreSubjectSelection,
    setGlobalData,
    setError,
    error
  } = useCodesContext();
  
  const metadata = useContext(MetadataContext);
  const tooltip = metadata?.[key]?.tooltip;
  const subjectTypes = getSubjectTypes();
  const preserveCodesData = React.useRef<{[key: string]: CodeItem[] | null} | null>(null);
  
  const hasLoadedCodesRef = React.useRef(false);

  React.useEffect(() => {
    // Prevent double-loading (especially in React Strict Mode)
    if (hasLoadedCodesRef.current) return;
    hasLoadedCodesRef.current = true;
    
    let isMounted = true;
    
    const loadCodes = async () => {
      try {
        const transformed = await TransformCodes();
        if (transformed) {
          // Batch state updates together to minimize re-renders
          setCodesData(transformed as any);
          setGlobalData(transformed as any);
          (preserveCodesData as React.MutableRefObject<{[key: string]: CodeItem[] | null} | null | any>).current = {...transformed};
        }
      } catch (error) {
        hasLoadedCodesRef.current = false; // Allow retry on error
        setError(`Failed to load Codes due to ${error}`)
      }
    };
    
    loadCodes();
    
    return () => {
      isMounted = false;
    };
  }, []); // Empty deps array - runs only once

  React.useEffect(() => {
    const filtered = filterCodesBySearch(preserveCodesData.current?.[subjectType] || [], searchQuery);
    preserveCodesData.current && setCodesData({...codesData, [subjectType]: filtered || [] });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery, filterCodesBySearch, subjectType]);

  const handleReset = () => {
    resetState();
    clearErrors(key);
  }

  const handleAddItem = () => {
    const selections = modifySubjectSelection();
    selections?.forEach((code, i) => {
      if(!code.id) return;
      if(code.url && !getValues(key)?.some((subject: any) => subject.id === code.url)) {
        prepend(generator(code?.url));
        trigger(key)
      }
    });
    clearErrors(key);
  }

  const handleRemoveTreeSelection = () => {
    if(selectedCodesData.length === 0) return;
    selectedCodesData.forEach(()=>{
      const index = getValues(key).findIndex((codeItem: { id: string; }, i: number) => selectedCodes[i] !== codeItem?.id?.split("/").pop());
      if(index === -1) return;
      remove(index);
    })
    getSelectedCodesData();
  }

  const hanldeRemoveIndividualSubjects = (codeId: string) => {
  // Find the index using the stable field ID
    const index = getValues(key)?.findIndex((subject: { id: string | string[]; }) => typeof subject.id === 'string' && subject.id.split("/").pop() === codeId);
    if (index !== -1) {
      remove(index);
      removeFromSubjects(codeId);
    }
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
      <CardContent sx={{ p: 3, m: 3, border: "1px solid rgba(0, 0, 0, 0.87)", borderRadius: 1 }}>
        <Stack gap={2} className={isRowHighlighted ? "add" : ""}>
          {errorMessage && (
            <Typography variant="body2" color="error" textAlign="center">
              {errorMessage}
            </Typography>
          )}
          <Stack gap={2} data-testid={`${key}-form`}>
            <Typography variant="body1" textAlign="left" marginBottom={2}>
              {"Subject Selection"}
            </Typography>
            <Stack
              gap={2}
              direction={{ xs: 'column', sm: 'row' }}
              alignItems="center"
              justifyContent="space-between"
              spacing={{ xs: 0, sm: 10, md: 20, lg: 30, xl: 40 }}
            >
              <DropDown
                label="Select Subject Type"
                options={subjectTypes || []}
                defaultValue={subjectType || ""}
                setValue={setSubjectType}
                currentValue={subjectType || ""}
              />
              <CustomisedInputBase
                placeholder="Type to filter subjects..."
                searchValue={(searchValue) => {
                  setSearchQuery(searchValue);
                }}
                value={searchQuery}
                endEdorment={<Search size={16} />}
              />
            </Stack>
            <CustomizedTreeViewWithSelection/>
            <Stack gap={2} direction="row" justifyContent="flex-end">
              <Button
                variant="outlined"
                color="info"
                size="small"
                startIcon={<RotateCcw />}
                sx={{ textTransform: "none", mt: 3, alignSelf: 'flex-end' }}
                onClick={handleReset}
              >
                Reset
              </Button>
              <Button
                variant="outlined"
                color="info"
                size="small"
                startIcon={<Plus />}
                sx={{ textTransform: "none", mt: 3, alignSelf: 'flex-end' }}
                onClick={handleAddItem}
                disabled={selectedCodes.length === 0 && selectedCodesData?.length === 0}
              >
                Save Selection
              </Button>
              </Stack>
            </Stack>
            <Stack gap={4}>
            {selectedCodesData?.map((field, index) => (
              <Fragment key={field.id}>
                <DetailsForm
                  key={field.id}
                  handleRemoveItem={(e) => hanldeRemoveIndividualSubjects(field.id)}
                  index={index}
                  selectedCode={field.label}
                  id={field.id}
                />
                <SubjectKeywordsForm
                  control={control}
                  errors={errors}
                  trigger={trigger}
                  parentIndex={index}
                />
              </Fragment>
            ))}
            {error && error}
          </Stack>
          {confirmationNeeded && (
            <CustomizedDialogs
              modalTitle="Confirm Removal"
              modalContent="Removing the subject will also delete its keywords. Are you sure you want to remove it?"
              alertOpen={confirmationNeeded}
              onClose={() => {}}
              modalAction={true}
              modalActions={[
            {
              label: "Cancel",
              onClick: () => {
                restoreSubjectSelection();
                setConfirmationNeeded(false);
              },
              icon: Delete,
              bgColor: "primary.main",
            },
            {
              label: "Yes",  
              onClick: () => {
                handleRemoveTreeSelection();
                setConfirmationNeeded(false);
              },
              icon: Check,
              bgColor: "error.main",
            }
          ]}
          />)}
        </Stack>
      </CardContent>
    </Card>
  );
}