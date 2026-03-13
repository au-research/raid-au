import { useSnackbar } from "@/components/snackbar";
import { ServicePointWithMembers } from "@/types";
import {
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
import { useAddUserToGroupAdmins, useModifyUserAccess, useRemoveUserFromGroupAdmins, useRemoveUserFromServicePoint } from "./useServicePointMutation";
import { SnackbarContextInterface } from "@/components/snackbar/SnackbarContext";

export const ServicePointUsersList = ({
  servicePointWithMembers,
}: {
  servicePointWithMembers?: ServicePointWithMembers;
}) => {
  const { token } = useKeycloak();
  const snackbar: SnackbarContextInterface = useSnackbar();
  const IsnackBar = snackbar as { openSnackbar: (message: string, duration?: number, severity?: string) => void };
  // Define mutation hooks at the top of the component
  const modifyUserAccessMutation = useModifyUserAccess(IsnackBar);
  const removeUserFromServicePointMutation = useRemoveUserFromServicePoint(IsnackBar);
  const removeUserFromGroupAdminsMutation = useRemoveUserFromGroupAdmins(IsnackBar);
  const addUserToGroupAdminsMutation = useAddUserToGroupAdmins(IsnackBar);

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
                    <>
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
                    </>
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
