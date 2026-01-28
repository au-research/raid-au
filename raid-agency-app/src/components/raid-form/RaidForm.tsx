import { AnchorButtons } from "@/components/anchor-buttons";
import { RaidValidationSchema } from "@/entities/validation";
import { AccessForm } from "@/entities/access/forms/access-form";
import { AlternateIdentifiersForm } from "@/entities/alternate-identifier/forms/alternate-identifiers-form";
import { AlternateUrlsForm } from "@/entities/alternate-url/forms/alternate-urls-form";
import { ContributorsForm } from "@/entities/contributor/forms/contributors-form";
import { DateForm } from "@/entities/date/forms/date-form";
import { DescriptionsForm } from "@/entities/description/forms/descriptions-form";
import { OrganisationsForm } from "@/entities/organisation/forms/organisations-form";
import { RelatedObjectsForm } from "@/entities/related-object/forms/related-objects-form";
import { RelatedRaidsForm } from "@/entities/related-raid/forms/related-raids-form";
import { SpatialCoveragesForm } from "@/entities/spatial-coverage/forms/spatial-coverages-form";
import { SubjectsForm } from "@/entities/subject/forms/subjects-form";
import { TitlesForm } from "@/entities/title/forms/titles-form";
import { RaidCreateRequest, RaidDto } from "@/generated/raid";
import { zodResolver } from "@hookform/resolvers/zod";
import { Close as CloseIcon, Save as SaveIcon } from "@mui/icons-material";
import { Fab, Stack, Tooltip } from "@mui/material";
import { memo, useCallback, useEffect, useState, useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import {ServicePointForm} from "@/entities/service-point/forms/ServicePointForm.tsx";
import {useAuthHelper} from "@/auth/keycloak";
import { useErrorDialog } from "@/components/error-dialog";
import { transformErrorMessage } from "../raid-form-error-message/ErrorContentUtils";
import { formConfigService, transformFormData } from "@/services/form-service";
import { createContext } from "react";
import { useCodesContext } from "@/components/tree-view/context/CodesContext";
import { CodeItem } from "../tree-view/context/CodesProvider";

// Define JSON types locally since '@/types/json-types' is missing
type JSONValue = string | number | boolean | null | JSONObject | JSONArray;
type JSONObject = { [key: string]: JSONValue };
type JSONArray = JSONValue[];

// Create MetadataContext if not already defined elsewhere
export const MetadataContext = createContext<{ [key: string]: { tooltip?: string } }>({});

/**
 * Main form component for creating and editing RAIDs
 * 
 * Coordinates multiple form sections into a cohesive form with validation
 * across all fields. Uses React Hook Form for state management.
 * 
 * @param {RaidCreateRequest|RaidDto} raidData - Initial form data
 * @param {Function} onSubmit - Handler called with validated form data
 * @param {boolean} isSubmitting - Whether form submission is in progress
 * @param {string} prefix - Prefix part of RAID identifier for routing
 * @param {string} suffix - Suffix part of RAID identifier for routing
 * @returns {JSX.Element} Complete RAID form with validation and submission handling
 */
export const RaidForm = memo(
  ({
    raidData,
    onSubmit,
    isSubmitting,
    prefix,
    suffix,
  }: {
    raidData: RaidCreateRequest | RaidDto;
    onSubmit: (data: RaidDto) => void;
    isSubmitting: boolean;
    prefix: string;
    suffix: string;
  }) => {
    const { isOperator } = useAuthHelper();
    const [isInitialLoad, setIsInitialLoad] = useState(true);
    const hasLoadedInitialData = useRef(false);
    const { openErrorDialog } = useErrorDialog();
    const {
      setSelectedCodes,
      codesData,
      setSelectedCodesData,
      getCodeById,
      globalData,
      setSearchQueryState
    } = useCodesContext();

    const formConfig = formConfigService();
    const [formSchema, setFormSchema] = useState<JSONObject | null>(null);

    useEffect(() => {
      formConfig.getFormConfig().then((schema: JSONObject) => setFormSchema(schema));
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const { data: transformedData, metadata } = transformFormData(raidData, formSchema as JSONObject);

    const formMethods = useForm<RaidDto>({
      defaultValues: transformedData,
      resolver: zodResolver(RaidValidationSchema),
      mode: "onChange",
      reValidateMode: "onChange",
    });

    const { control, trigger, formState, setValue, clearErrors } = formMethods;

    const handleSubmit = useCallback(
      (data: RaidDto) => {
        // This function is called when the form is submitted
        // and all validations pass
        onSubmit(data);
      },
      [onSubmit]
    );

    useEffect(() => {
      if (isInitialLoad) {
        setIsInitialLoad(false);
      }
    }, [isInitialLoad]);

    useEffect(() => {
      // This effect runs when the form is submitted
      // and there are validation errors
      if (formState.isSubmitted && Object.keys(formState.errors).length > 0) {
        openErrorDialog(transformErrorMessage(formState.errors));
      }
      // This effect runs when there are validation errors
      // and opens an error dialog with the transformed error message
    }, [formState.errors, formState.isSubmitted, openErrorDialog]);

    useEffect(() => {
      if (!hasLoadedInitialData.current && Array.isArray(raidData?.subject) && raidData.subject.length > 0 && codesData) {
        const selectedSubjects = Array.isArray(raidData.subject)
          ? raidData.subject
          : [];

        const selectedIds = selectedSubjects.map((subject) =>
          subject.id.split("/").pop() || ""
        );

        if(selectedIds.length === 0) return;
        setSelectedCodes(selectedIds);
        const codesArray = selectedIds
          .map(codeId => getCodeById(codeId, codesData))
          .filter((item): item is CodeItem => item !== undefined);
        if (codesArray.length > 0) {
          setSelectedCodesData(codesArray);
          setSearchQueryState('')
          hasLoadedInitialData.current = true; // Mark as loaded
        }
      } else if ((!raidData?.subject || raidData.subject.length === 0) && isInitialLoad) {
        setSelectedCodes([]);
        setSelectedCodesData([]);
        setValue('subject', [])
        clearErrors('subject');
        setSearchQueryState('');
      }
    }, [raidData.subject, codesData, getCodeById, globalData]);

    return (
      <MetadataContext.Provider value={metadata}>
        <FormProvider {...formMethods}>
          <form
            onSubmit={formMethods.handleSubmit(handleSubmit)}
            autoComplete="off"
            noValidate
          >
            <Stack
              gap={2}
              sx={{
                position: "fixed",
                bottom: "16px",
                right: "16px",
                zIndex: 1000,
              }}
              alignItems="end"
            >
              <Tooltip title="Cancel" placement="left">
                <Fab
                  component={Link}
                  color="primary"
                  size="small"
                  to={
                    raidData?.identifier?.id ? `/raids/${prefix}/${suffix}` : "/"
                  }
                >
                  <CloseIcon/>
                </Fab>
              </Tooltip>
              <Tooltip title="Save changes" placement="left">
                <Fab
                  variant="extended"
                  color="primary"
                  component="button"
                  type="submit"
                  disabled={isSubmitting} //Removed isFormValid check to allow submission even with errors to avoid deadlock
                  data-testid="save-raid-button"
                >
                  <SaveIcon sx={{mr: 1}}/>
                  {isSubmitting ? "Saving..." : "Save"}
                </Fab>
              </Tooltip>
            </Stack>

            <Stack spacing={2} data-testid="raid-form">
              <AnchorButtons raidData={raidData} errors={formState.errors}/>
              <Stack spacing={2}>

                  {isOperator && raidData.identifier?.id && (
                    <ServicePointForm
                        errors={formState.errors}
                    />
                  )}

                <DateForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <TitlesForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <DescriptionsForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <ContributorsForm
                  control={control}
                  data={raidData.contributor ?? []}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <OrganisationsForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <RelatedObjectsForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <AlternateIdentifiersForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <AlternateUrlsForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <RelatedRaidsForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <AccessForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <SubjectsForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />

                <SpatialCoveragesForm
                  control={control}
                  errors={formState.errors}
                  trigger={trigger}
                />
              </Stack>
            </Stack>
            <pre>{JSON.stringify(
                Object.fromEntries(
                  Object.entries(formState.errors).map(([key, value]) => [
                    key,
                    { message: value?.message, type: value?.type }
                  ])
                ),
                null,
                2
            )}</pre>
          </form>
        </FormProvider>
      </MetadataContext.Provider>
    );
  }
);
