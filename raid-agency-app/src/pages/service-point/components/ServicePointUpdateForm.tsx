import { useKeycloak } from "@/contexts/keycloak-context";
import type { ServicePoint } from "@/generated/raid";
import { updateServicePoint } from "@/services/service-points";
import { UpdateServicePointRequest } from "@/types";
import { zodResolver } from "@hookform/resolvers/zod";
import {
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
import { useMutation, useQueryClient } from "@tanstack/react-query";
import React from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { StyledPaper as Item } from "./StyledComponent";
import { useSnackbar } from "@/components/snackbar";
import { messages } from "@/constants/messages";
import { Building2, Settings, RefreshCcw, Database } from "lucide-react";
import CustomizedInputBase from "@/containers/organisation-lookup/RORCustomComponent";
import { useErrorDialog } from "@/components/error-dialog";
import { transformErrorMessage } from "@/components/raid-form-error-message/ErrorContentUtils";
import { ErrorItem } from '@/components/raid-form-error-message/types';
import { RaidFormErrorMessage } from "@/components/raid-form-error-message";
import { updateServicePointRequestValidationSchema } from "../validation/Rules";

export const ServicePointUpdateForm = ({
  servicePoint,
}: {
  servicePoint: ServicePoint;
}) => {
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

  const initalServicePointValues: UpdateServicePointRequest = {
    id: servicePoint.id,
    servicePointUpdateRequest: {
      ...servicePoint,
      groupId: servicePoint.groupId || "",
      identifierOwner: servicePoint.identifierOwner || "",
    },
  };

  const form = useForm<UpdateServicePointRequest>({
    resolver: zodResolver(updateServicePointRequestValidationSchema),
    mode: "onChange",
    reValidateMode: "onChange",
    defaultValues: { ...initalServicePointValues },
  });

  const handleUpdateSuccess = async() => {
    await queryClient.invalidateQueries({
      queryKey: ["servicePoints", servicePoint.id.toString()],
    });
    setAppState({...appState, loading: false, loaded: true });
    snackbar.openSnackbar(messages.servicePointUpdated, 3000, "success");
  };

  const handleCreateError = (error: Error) => {
    setAppState({...appState, loading: false, error: true });
    RaidFormErrorMessage(error, openErrorDialog);
  };

  const updateServicePointHandler = async (
    servicePoint: UpdateServicePointRequest
  ) => {
    return await updateServicePoint({
      id: servicePoint.id,
      data: {
        id: servicePoint.id,
        servicePointUpdateRequest: servicePoint.servicePointUpdateRequest,
      },
      token: token!,
    });
  };

  const updateServicePointMutation = useMutation({
    mutationFn: updateServicePointHandler,
    onError: handleCreateError,
    onSuccess: handleUpdateSuccess,
  });

  const onSubmit = (item: UpdateServicePointRequest) => {
    if (selectedValue) {
      item.servicePointUpdateRequest.identifierOwner = selectedValue.id;
    }
    setAppState({ ...appState, loading: true });
    updateServicePointMutation.mutate(item);
  };
  const { formState } = form;
  React.useEffect(() => {
    // This effect runs when the form is submitted
    // and there are validation errors
    if (formState.isSubmitted && Object.keys(formState.errors).length > 0) {
      openErrorDialog(transformErrorMessage(formState.errors.servicePointUpdateRequest as Record<string,  ErrorItem | ErrorItem[]>));
    }
    // This effect runs when there are validation errors
    // and opens an error dialog with the transformed error message
  }, [formState.errors, formState.isSubmitted, openErrorDialog]);

    return (
        <FormProvider {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} autoComplete="off">
                <Box sx={{mb: 2, pb: 2, borderBottom: 1, borderColor: 'divider'}}>
                    <Controller
                        name="servicePointUpdateRequest.name"
                        control={form.control}
                        render={({field}) => (
                            <TextField
                                error={
                                    !!form.formState.errors?.servicePointUpdateRequest?.name
                                }
                                helperText={
                                    form.formState.errors?.servicePointUpdateRequest?.name
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
                    direction={{xs: 'column', sm: 'row', md: 'row'}}
                    spacing={2}
                    sx={{
                        justifyContent: "flex-start",
                        alignItems: "flex-start",
                        mb: 3,
                        width: '100%'
                    }}
                    divider={<Divider orientation="vertical" flexItem/>}
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
                                icon={<Building2 fontSize="small"/>}
                                sx={{minHeight: "40px"}}
                            />
                        </Tabs>
                        <Box>
                            <Stack
                                spacing={{xs: 1, sm: 1, md: 2}}
                                direction="column"
                                useFlexGap
                                sx={{mt: 2}}
                            >
                                <div>
                                    <CustomizedInputBase
                                        setSelectedValue={setSelectedValue}
                                        name={`servicePointUpdateRequest.identifierOwner`}
                                        defaultValue={form.getValues("servicePointUpdateRequest.identifierOwner")}
                                        styles={{width: '100%'}}
                                    />
                                    <FormHelperText error>
                                        {form.formState.errors?.servicePointUpdateRequest?.identifierOwner?.message}
                                    </FormHelperText>
                                </div>
                                <div>
                                    <Controller
                                        name="servicePointUpdateRequest.adminEmail"
                                        control={form.control}
                                        render={({field}) => (
                                            <TextField
                                                label="Admin email *"
                                                variant="outlined"
                                                size="small"
                                                fullWidth
                                                {...field}
                                                value={field.value}
                                                error={
                                                    !!form.formState.errors?.servicePointUpdateRequest
                                                        ?.adminEmail
                                                }
                                                helperText={
                                                    form.formState.errors?.servicePointUpdateRequest?.adminEmail
                                                        ?.message
                                                }
                                            />
                                        )}
                                    />
                                </div>
                                <div>
                                    <Controller
                                        name="servicePointUpdateRequest.techEmail"
                                        control={form.control}
                                        render={({field}) => (
                                            <TextField
                                                label="Tech email *"
                                                variant="outlined"
                                                size="small"
                                                fullWidth
                                                {...field}
                                                value={field.value}
                                                error={
                                                    !!form.formState.errors?.servicePointUpdateRequest
                                                        ?.techEmail
                                                }
                                                helperText={
                                                    form.formState.errors?.servicePointUpdateRequest?.techEmail
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
                                icon={<Database fontSize="small"/>}
                                sx={{minHeight: "40px"}}
                            />
                        </Tabs>
                        <Box>
                            <Stack
                                spacing={{xs: 1, sm: 1, md: 2}}
                                direction="column"
                                useFlexGap
                                sx={{mt: 2}}
                            >
                                <div>
                                    <Controller
                                        name="servicePointUpdateRequest.repositoryId"
                                        control={form.control}
                                        render={({field}) => (
                                            <TextField
                                                disabled={true}
                                                label="Repository ID *"
                                                variant="outlined"
                                                size="small"
                                                fullWidth
                                                {...field}
                                                value={field.value}
                                                error={
                                                    !!form.formState.errors?.servicePointUpdateRequest
                                                        ?.repositoryId
                                                }
                                                helperText={
                                                    form.formState.errors?.servicePointUpdateRequest?.repositoryId
                                                        ?.message
                                                }
                                            />
                                        )}
                                    />
                                </div>
                                <div>
                                    <Controller
                                        name="servicePointUpdateRequest.prefix"
                                        control={form.control}
                                        render={({field}) => (
                                            <TextField
                                                disabled={true}
                                                label="Prefix *"
                                                variant="outlined"
                                                size="small"
                                                fullWidth
                                                {...field}
                                                value={field.value}
                                                error={
                                                    !!form.formState.errors?.servicePointUpdateRequest?.prefix
                                                }
                                                helperText={
                                                    form.formState.errors?.servicePointUpdateRequest?.prefix
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
                            icon={<Settings fontSize="small"/>}
                            sx={{minHeight: "40px"}}
                        />
                    </Tabs>
                    <Stack
                        direction="column"
                    >
                        <Item>
                            <Controller
                                name="servicePointUpdateRequest.enabled"
                                control={form.control}
                                render={({field}) => (
                                    <FormGroup sx={{display: 'inline-flex'}}>
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
                                name="servicePointUpdateRequest.appWritesEnabled"
                                control={form.control}
                                render={({field}) => (
                                    <FormGroup sx={{display: 'inline-flex'}}>
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
                    sx={{mt: 3, width: 120}}
                    disabled={appState.loading}
                >
                    {appState.loading ? (
                        <CircularProgress size={24}/>
                    ) : (
                        <>
                            <Box component="span" sx={{mr: 1, display: "inline-flex", verticalAlign: "middle"}}>
                                <RefreshCcw/>
                            </Box>
                            Update
                        </>
                    )}
                </Button>
            </form>
        </FormProvider>
    );
};
