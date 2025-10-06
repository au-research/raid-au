import React, { useEffect } from "react";
import { useKeycloak } from "@/contexts/keycloak-context";
import { createServicePoint } from "@/services/service-points";
import { CreateServicePointRequest } from "@/types";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Button,
  FormControlLabel,
  FormGroup,
  Switch,
  Tabs,
  TextField,
  Typography,
  Tab,
  Stack,
  Divider,
  FormHelperText,
  CircularProgress
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { RaidFormErrorMessage } from "@/components/raid-form-error-message";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useSnackbar } from "@/components/snackbar";
import { messages } from "@/constants/messages";
import { Building2, Settings, SquarePen, Database } from "lucide-react";
import CustomizedInputBase from "@/containers/organisation-lookup/RORCustomComponent";
import { useErrorDialog } from "@/components/error-dialog";
import { transformErrorMessage } from "@/components/raid-form-error-message/ErrorContentUtils";
import { ErrorItem } from '@/components/raid-form-error-message/types';
import { ServicePoint } from "@/generated/raid";
import { createServicePointRequestValidationSchema } from "@/pages/service-point/validation/Rules";
import { StyledPaper as Item } from "./StyledComponent";

export const ServicePointCreateForm = () => {
  const queryClient = useQueryClient();
  const { token } = useKeycloak();
  const snackbar = useSnackbar();
  const [selectedValue, setSelectedValue] = React.useState<{ id: string; name?: string } | null>(null);
  const { openErrorDialog } = useErrorDialog();
  const [appState, setAppState] = React.useState({
    loading: false,
    loaded: false,
    error: false,
  });

  const initalServicePointValues: CreateServicePointRequest = {
    servicePointCreateRequest: {
      name: "",
      identifierOwner: "",
      adminEmail: "",
      techEmail: "",
      enabled: false,
      password: "",
      prefix: "",
      repositoryId: "",
      appWritesEnabled: false,
    },
  };

  const form = useForm<CreateServicePointRequest>({
    resolver: zodResolver(createServicePointRequestValidationSchema),
    mode: "onChange",
    reValidateMode: "onChange",
    defaultValues: { ...initalServicePointValues },
  });

  const handleCreateSuccess = async () => {
    queryClient.invalidateQueries({ queryKey: ["createServicePoint"] });
    // Show success snackbar
    setAppState({...appState, loading: false, loaded: true });
    form.reset();
    setSelectedValue(null);
    snackbar.openSnackbar(messages.servicePointCreated, 3000, "success");
    await queryClient.invalidateQueries({ queryKey: ["servicePoints"] });
  };

  const handleCreateError = (error: Error) => {
    setAppState({...appState, loading: false, error: true });
    RaidFormErrorMessage(error, openErrorDialog);
  };

  const createServicePointHandler = async (
    servicePoint: CreateServicePointRequest
  ) => {
    await createServicePoint({
      data: {
        servicePointCreateRequest: servicePoint.servicePointCreateRequest,
      },
      token: token!,
    });
  };

  const createServicePointMutation = useMutation({
    mutationFn: createServicePointHandler,
    onError: handleCreateError,
    onSuccess: handleCreateSuccess,
  });

const onSubmit = (item: CreateServicePointRequest) => {
  // Set identifier owner if selected
  if (selectedValue) {
    item.servicePointCreateRequest.identifierOwner = selectedValue.id;
  }

  // Check for duplicate repository ID
  const apiData = queryClient.getQueryData<ServicePoint[]>(["servicePoints"]);
  const isDuplicateRepositoryID = apiData?.some(
    (sp) => sp.repositoryId === item.servicePointCreateRequest.repositoryId
  );

  if (isDuplicateRepositoryID) {
    form.setError("servicePointCreateRequest.repositoryId", {
      type: "manual",
      message: messages.servicePointUniqueRepositoryID
    });
    return;
  }

  // Proceed with mutation
  createServicePointMutation.mutate(item);
  setAppState({ ...appState, loading: true });
};

  const { formState } = form;
  useEffect(() => {
    // This effect runs when the form is submitted
    // and there are validation errors
    if (formState.isSubmitted && Object.keys(formState.errors).length > 0) {
      openErrorDialog(transformErrorMessage(formState.errors.servicePointCreateRequest as Record<string,  ErrorItem | ErrorItem[]>));
    }
    // This effect runs when there are validation errors
    // and opens an error dialog with the transformed error message
  }, [formState.errors, formState.isSubmitted, openErrorDialog]);

  return (
    <>
      <Accordion
        disableGutters={true}
      >
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          aria-controls="panel1-content"
          id="panel1-header"
        >
          <SquarePen /><Typography sx={{ml: 1}}component="span">Create Service Point</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <FormProvider {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} autoComplete="off">
              <Box sx={{ mb: 2, pb: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Controller
                  name="servicePointCreateRequest.name"
                  control={form.control}
                  render={({ field }) => (
                    <TextField
                      error={
                        !!form.formState.errors?.servicePointCreateRequest?.name
                      }
                      helperText={
                        form.formState.errors?.servicePointCreateRequest?.name
                          ?.message
                      }
                      label="Service point name *"
                      variant="outlined"
                      size="small"
                      fullWidth
                      {...field}
                      value={field.value}
                    />
                  )}
                />
              </Box>
              <Stack
                direction={{ xs: 'column', sm: 'row', md: 'row' }}
                spacing={2}
                sx={{
                  justifyContent: "flex-start",
                  alignItems: "flex-start",
                  mb: 3,
                  width: '100%'
                }}
                divider={<Divider orientation="vertical" flexItem />}
              >
                <Item>
                  <Tabs
                    value={"one"}
                    textColor="primary"
                    indicatorColor="primary"
                    aria-label="secondary tabs example"
                  >
                    <Tab
                      value="one"
                      label={<Typography variant="body2">Service Point Owner</Typography>}
                      iconPosition="start"
                      icon={<Building2 fontSize="small" />}
                      sx={{ minHeight: "40px" }}
                    />
                  </Tabs>
                    <Box>
                    <Stack
                      spacing={{ xs: 1, sm: 1, md: 2 }}
                      direction="column"
                      useFlexGap
                      sx={{ mt: 2 }}
                    >
                      <div>
                        <CustomizedInputBase
                          setSelectedValue={setSelectedValue}
                          name={`servicePointCreateRequest.identifierOwner`}
                          defaultValue={selectedValue?.id || ""}
                          styles={{ width: '100%' }}
                          resetValue={selectedValue}
                        />
                        <FormHelperText error>
                          {form.formState.errors?.servicePointCreateRequest?.identifierOwner?.message}
                        </FormHelperText>
                      </div>
                      <div >
                        <Controller
                          name="servicePointCreateRequest.adminEmail"
                          control={form.control}
                          render={({ field }) => (
                            <TextField
                              label="Admin email *"
                              variant="outlined"
                              size="small"
                              fullWidth
                              {...field}
                              value={field.value}
                              error={
                                !!form.formState.errors?.servicePointCreateRequest
                                  ?.adminEmail
                              }
                              helperText={
                                form.formState.errors?.servicePointCreateRequest?.adminEmail
                                  ?.message
                              }
                            />
                          )}
                        />
                      </div>
                      <div >
                        <Controller
                          name="servicePointCreateRequest.techEmail"
                          control={form.control}
                          render={({ field }) => (
                            <TextField
                              label="Tech email *"
                              variant="outlined"
                              size="small"
                              fullWidth
                              {...field}
                              value={field.value}
                              error={
                                !!form.formState.errors?.servicePointCreateRequest
                                  ?.techEmail
                              }
                              helperText={
                                form.formState.errors?.servicePointCreateRequest?.techEmail
                                  ?.message
                              }
                            />
                          )}
                        />
                      </div>
                    </Stack>
                  </Box>
                </Item>
                <Item>
                  <Tabs
                    value={"two"}
                    textColor="primary"
                    indicatorColor="primary"
                    aria-label="secondary tabs example"
                  >
                    <Tab
                      value="two"
                      label={<Typography variant="body2">DataCite repository</Typography>}
                      iconPosition="start"
                      icon={<Database fontSize="small" />}
                      sx={{ minHeight: "40px" }}
                    />
                  </Tabs>
                  <Box>
                  <Stack
                    spacing={{ xs: 1, sm: 1, md: 2 }}
                    direction="column"
                    useFlexGap
                    sx={{ mt: 2 }}
                  >
                    <div>
                      <Controller
                        name="servicePointCreateRequest.repositoryId"
                        control={form.control}
                        render={({ field }) => (
                          <TextField
                            label="Repository ID *"
                            variant="outlined"
                            size="small"
                            fullWidth
                            {...field}
                            value={field.value}
                            error={
                              !!form.formState.errors?.servicePointCreateRequest
                                ?.repositoryId
                            }
                            helperText={
                              form.formState.errors?.servicePointCreateRequest?.repositoryId
                                ?.message
                            }
                          />
                        )}
                      />
                    </div>
                    <div >
                      <Controller
                        name="servicePointCreateRequest.prefix"
                        control={form.control}
                        render={({ field }) => (
                          <TextField
                            label="Prefix *"
                            variant="outlined"
                            size="small"
                            fullWidth
                            {...field}
                            value={field.value}
                            error={
                              !!form.formState.errors?.servicePointCreateRequest?.prefix
                            }
                            helperText={
                              form.formState.errors?.servicePointCreateRequest?.prefix
                                ?.message
                            }
                          />
                        )}
                      />
                    </div>
                    <div >
                      <Controller
                        name="servicePointCreateRequest.password"
                        control={form.control}
                        render={({ field }) => (
                          <TextField
                            label="Password *"
                            type="password"
                            variant="outlined"
                            size="small"
                            fullWidth
                            {...field}
                            value={field.value}
                            error={
                              !!form.formState.errors?.servicePointCreateRequest
                                ?.password
                            }
                            helperText={
                              form.formState.errors?.servicePointCreateRequest?.password
                                ?.message
                            }
                          />
                        )}
                      />
                    </div>
                  </Stack>
                  </Box>
                </Item>
              </Stack>
              <Item>
                <Tabs
                  value={"three"}
                  textColor="primary"
                  indicatorColor="primary"
                  aria-label="secondary tabs example"
                >
                  <Tab
                    value="three"
                    label={<Typography variant="body2">Settings</Typography>}
                    iconPosition="start"
                    icon={<Settings fontSize="small" />}
                    sx={{ minHeight: "40px" }}
                  />
                </Tabs>
                <Stack
                  direction="column"
                >
                  <Item>
                    <Controller
                      name="servicePointCreateRequest.enabled"
                      control={form.control}
                      render={({ field }) => (
                        <FormGroup sx={{ display: 'inline-flex' }}>
                          <FormControlLabel
                            control={
                              <Switch
                                {...field}
                                checked={Boolean(field.value)}
                              />
                            }
                            label="Enable service point?"
                          />
                        </FormGroup>
                      )}
                    />
                  </Item>
                  <Item>
                    <Controller
                      name="servicePointCreateRequest.appWritesEnabled"
                      control={form.control}
                      render={({ field }) => (
                        <FormGroup sx={{ display: 'inline-flex' }}>
                          <FormControlLabel
                            control={
                              <Switch
                                {...field}
                                checked={Boolean(field.value)}
                              />
                            }
                            label="Enable minting and editing RAiDs using web app?"
                          />
                        </FormGroup>
                      )}
                    />
                  </Item>
                </Stack>
              </Item>
              <Button
                variant="outlined"
                type="submit"
                sx={{ mt: 3, width: 120 }}
                disabled={appState.loading}
              >
                {appState.loading ? (
                  <CircularProgress size={24} />
                ) : (
                  <>
                    <Box component="span" sx={{ mr: 1, display: "inline-flex", verticalAlign: "middle" }}>
                      <SquarePen />
                    </Box>
                    Create
                  </>
                )}
              </Button>
            </form>
          </FormProvider>
        </AccordionDetails>
      </Accordion>
    </>
  );
};
