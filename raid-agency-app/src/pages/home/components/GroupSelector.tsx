import DOMPurify from "dompurify";
import { ErrorAlertComponent } from "@/components/error-alert-component";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Loading } from "@/pages/loading";
import {
  fetchAllKeycloakGroups,
  fetchKeycloakLocalization,
  joinKeycloakGroup,
  setKeycloakUserAttribute,
} from "@/services/keycloak-groups";
import {
  Alert,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  InputLabel,
  Link,
  MenuItem,
  Select,
  SelectChangeEvent,
  Stack,
} from "@mui/material";

import { useMutation, useQuery } from "@tanstack/react-query";
import React, { memo, useState } from "react";
import { useAppConfig } from "@/config/Appconfigcontext";

type KeycloakGroupSPI = {
  name: string;
  attributes: {
    groupId: string[];
  };
  id: string;
};


export const GroupSelector = memo(() => {
  const config = useAppConfig();
  const { token, isInitialized } = useKeycloak();
  const [open, setOpen] = useState(false);
  const handleClose = () => {
    setOpen(false);
  };

  const [selectedServicePointId, setSelectedServicePointId] =
    useState<string>("");

  const fetchKeycloakGroupsQuery = useQuery({
    queryFn: () => fetchAllKeycloakGroups({ token: token }),
    queryKey: ["keycloakGroups"],
    enabled: isInitialized,
  });

  const handleGroupSelectorChange = (event: SelectChangeEvent) => {
    setSelectedServicePointId(event.target.value);
  };

  const joinKeycloakGroupMutation = useMutation({
    mutationFn: joinKeycloakGroup,
    onSuccess: () => {
      console.log("success");
    },
    onError: () => {
      console.log("error");
    },
  });

  const setKeycloakUserAttributeMutation = useMutation({
    mutationFn: setKeycloakUserAttribute,
    onSuccess: () => {
      console.log("success");
    },
    onError: () => {
      console.log("error");
    },
  });
  const localizationQuery = useQuery({
    queryFn: () =>
      fetchKeycloakLocalization({
        token: token,
        key: "groupSelectorAccessMessage",
      }),
    queryKey: ["localization", "groupSelectorAccessMessage"],
    enabled: isInitialized,
  });

  const handleKeycloakGroupJoinRequest = async () => {
    try {
      await Promise.all([
        new Promise((resolve, reject) => {
          setKeycloakUserAttributeMutation.mutate(
            {
              groupId: selectedServicePointId,
              token: token,
            },
            {
              onSuccess: resolve,
              onError: reject,
            }
          );
        }),
        new Promise((resolve, reject) => {
          joinKeycloakGroupMutation.mutate(
            {
              groupId: selectedServicePointId,
              token: token,
            },
            {
              onSuccess: resolve,
              onError: reject,
            }
          );
        }),
      ]);

      alert(
        "Request submitted and awaiting approval. Click to reload the page."
      );
      window.location.reload();
    } catch (error) {
      console.error("Error joining service point:", error);
      alert("Failed to join service point. Please try again.");
    }
  };

  if (fetchKeycloakGroupsQuery.isPending) {
    return <Loading />;
  }

  if (fetchKeycloakGroupsQuery.isError) {
    return <ErrorAlertComponent error="Keycloak groups could not be fetched" />;
  }

  if (localizationQuery.isError) {
    console.warn("Failed to fetch localization, using default text:", localizationQuery.error);
  }

  return (
    <>
      <Card>
        <CardContent>
          <Stack gap={2}>
           <Alert severity="error">
            {/*
            * Localization values are configured by admins in Keycloak's realm settings
            * and may contain HTML formatting (e.g. <br/>, <b>, <a>). DOMPurify
            * sanitizes the HTML to prevent XSS while preserving safe formatting tags.
            */}
            <span
              dangerouslySetInnerHTML={{
                __html: DOMPurify.sanitize(
                  localizationQuery.isError || localizationQuery.isPending
                  ? "To use RAiD you must belong to a 'Service Point'; please request access to the appropriate Service Point in the list below."
                  : localizationQuery.data?.value || "To use RAiD you must belong to a 'Service Point'; please request access to the appropriate Service Point in the list below."
                ),
              }}
            />
          </Alert>
            <>
              <FormControl>
                <InputLabel id="group-selector-label" size="small">
                  Institution
                </InputLabel>
                <Select
                  labelId="group-selector-label"
                  id="group-selector"
                  value={selectedServicePointId}
                  label="Institution"
                  onChange={handleGroupSelectorChange}
                  size="small"
                >
                  {fetchKeycloakGroupsQuery.data.groups.map(
                    (group: KeycloakGroupSPI) => (
                      <MenuItem key={group.id} value={group.id.toString()}>
                        {group.name}
                      </MenuItem>
                    )
                  )}
                </Select>
              </FormControl>
              <FormControl>
                <Button
                  variant="contained"
                  disabled={!selectedServicePointId}
                  onClick={handleKeycloakGroupJoinRequest}
                >
                  Submit request
                </Button>
              </FormControl>
            </>
          </Stack>
        </CardContent>
      </Card>
      <React.Fragment>
        <Dialog
          open={open}
          onClose={handleClose}
          aria-labelledby="alert-dialog-title"
          aria-describedby="alert-dialog-description"
        >
          <DialogTitle id="alert-dialog-title">
            {"Your authorisation request has been submitted."}
          </DialogTitle>
          <DialogContent>
            <DialogContentText id="alert-dialog-description">
              Our notification system is not yet implemented. Please send an
              email to{" "}
              <Link href={`mailto:${config.footer.links.find(link => link.contact)?.label}?subject=Please approve my RAiD request`}>
                {config.footer.links.find(link => link.contact)?.label}
              </Link>{" "}
              so we can approve your request.
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                handleClose();
                window.location.reload();
              }}
              autoFocus
            >
              Ok
            </Button>
          </DialogActions>
        </Dialog>
      </React.Fragment>
    </>
  );
});
