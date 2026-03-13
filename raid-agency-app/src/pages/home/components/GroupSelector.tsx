import { ErrorAlertComponent } from "@/components/error-alert-component";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Loading } from "@/pages/loading";
import {
  fetchAllKeycloakGroups,
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

type KeycloakGroupSPI = {
  name: string;
  attributes: {
    groupId: string[];
  };
  id: string;
};

export const GroupSelector = memo(() => {
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

  return (
    <>
      <Card>
        <CardContent>
          <Stack gap={2}>
            <Alert severity="error">
              To use RAiD you must belong to a 'Service Point'; please request
              access to the appropriate Service Point in the list below.
              <br />
              If you haven't been assigned a Service Point yet, please use
              'raid-au.'
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
              <Link href="mailto:contact@raid.org?subject=Please approve my RAiD request">
                contact@raid.org
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
