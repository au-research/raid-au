import { useSnackbar } from "@/components/snackbar";
import {
  addUserToGroupAdmins,
  removeUserFromGroupAdmins,
  removeUserFromServicePoint,
  updateUserServicePointUserRole,
} from "@/services/service-points";
import { ServicePointWithMembers } from "@/types";
import {
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Paper,
  Tooltip,
  Typography,
} from "@mui/material";

import {
  CheckCircleOutline as CheckCircleOutlineIcon,
  GroupRemove as GroupRemoveIcon,
  HighlightOff as HighlightOffIcon,
  ManageAccounts as ManageAccountsIcon
} from "@mui/icons-material";
import { Stack } from "@mui/material";

import { useKeycloak } from "@/contexts/keycloak-context";
import { useMutation, useQueryClient } from "@tanstack/react-query";

export const ServicePointUsersList = ({
  servicePointWithMembers,
}: {
  servicePointWithMembers?: ServicePointWithMembers;
}) => {
  const { token } = useKeycloak();

  const queryClient = useQueryClient();
  const snackbar = useSnackbar();

  const modifyUserAccessMutation = useMutation({
    mutationFn: updateUserServicePointUserRole,
    onError: (error) => {
      console.error(error);
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ["servicePoints"],
      });
      snackbar.openSnackbar(
        `✅ Success: ${variables.operation} role service-point-user `
      );
    },
  });

  const removeUserFromServicePointMutation = useMutation({
    mutationFn: removeUserFromServicePoint,
    onError: (error) => {
      console.error(error);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["servicePoints"],
      });
      snackbar.openSnackbar(`Success: Removed user from service point`, 3000, "success");
    },
  });

  const removeUserFromGroupAdminsMutation = useMutation({
    mutationFn: removeUserFromGroupAdmins,
    onError: (error) => {
      console.error(error);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["servicePoints"],
      });
      snackbar.openSnackbar(`Success: Removed user from group admins`, 3000, "success");
    },
  });

  const addUserToGroupAdminsMutation = useMutation({
    mutationFn: addUserToGroupAdmins,
    onError: (error) => {
      console.error(error);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["servicePoints"],
      });
      snackbar.openSnackbar(`Success: Added user to group admins`, 3000, "success");
    },
  });

  return (
    <>
      <Paper variant="outlined" sx={{ p: 2 }}>
        <Typography variant="h6">{servicePointWithMembers?.name}</Typography>
        <List dense={true}>
          {(servicePointWithMembers?.members || []).map((el) => {
            const username = el.attributes.username[0] || "";
            const firstName = el.attributes.firstName[0] || "";
            const lastName = el.attributes.lastName[0] || "";
            const email = el.attributes.email[0] || "";
            const roles = el.roles || [];
            const displayName =
              firstName || lastName
                ? `${username} (${`${firstName || ''} ${lastName || ''} ${email || ''}`.trim()})`
                : `${username} (${email || ''})`;
            return (
              <ListItem
                key={el.id}
                secondaryAction={
                  <Stack direction={"row"} spacing={1}>
                    <Tooltip title="Revoke membership" placement="top">
                      <span>
                        <IconButton
                          edge="end"
                          aria-label="revoke"
                          color="error"
                          disabled={!el?.roles?.includes("service-point-user")}
                          onClick={() => {
                            modifyUserAccessMutation.mutate({
                              userId: el.id,
                              userGroupId:
                                servicePointWithMembers?.groupId as string,
                              operation: "revoke",
                              token: token as string,
                            });
                          }}
                        >
                          <HighlightOffIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title="Grant membership" placement="top">
                      <span>
                        <IconButton
                          edge="end"
                          aria-label="grant"
                          color="success"
                          disabled={el?.roles?.includes("service-point-user")}
                          onClick={() => {
                            modifyUserAccessMutation.mutate({
                              userId: el.id,
                              userGroupId:
                                servicePointWithMembers?.groupId as string,
                              operation: "grant",
                              token: token as string,
                            });
                          }}
                        >
                          <CheckCircleOutlineIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Divider />
                    <Tooltip title="Permanently remove from SP" placement="top">
                      <span>
                        <IconButton
                          edge="end"
                          aria-label="remove from sp"
                          color="error"
                          onClick={() => {
                            removeUserFromServicePointMutation.mutate({
                              userId: el.id,
                              groupId:
                                servicePointWithMembers?.groupId as string,
                              token: token as string,
                            });
                          }}
                        >
                          <GroupRemoveIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Divider />
                    <Tooltip title="Revoke admin rights" placement="top">
                      <span>
                        <IconButton
                          edge="end"
                          aria-label="revoke admin rights"
                          color="error"
                          disabled={!el?.roles?.includes("group-admin")}
                          onClick={() => {
                            removeUserFromGroupAdminsMutation.mutate({
                              userId: el.id,
                              groupId:
                                servicePointWithMembers?.groupId as string,
                              token: token as string,
                            });
                          }}
                        >
                          <ManageAccountsIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <Tooltip title="Grant admin rights" placement="top">
                      <span>
                        <IconButton
                          edge="end"
                          aria-label="grant admin rights"
                          color="success"
                          disabled={el?.roles?.includes("group-admin")}
                          onClick={() => {
                            addUserToGroupAdminsMutation.mutate({
                              userId: el.id,
                              groupId:
                                servicePointWithMembers?.groupId as string,
                              token: token as string,
                            });
                          }}
                        >
                          <ManageAccountsIcon />
                        </IconButton>
                      </span>
                    </Tooltip>
                  </Stack>
                }
              >
                <ListItemText
                  primary={displayName}
                  secondary={roles.join(" • ")}
                />
              </ListItem>
            );
          })}
        </List>
      </Paper>
    </>
  );
};
