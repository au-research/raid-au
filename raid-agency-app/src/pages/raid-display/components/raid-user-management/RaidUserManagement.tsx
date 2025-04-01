import { ErrorAlertComponent } from "@/components/error-alert-component";
import { useSnackbar } from "@/components/snackbar";
import { Loading } from "@/pages/loading";
import {
  fetchRaidUsers,
  promoteRaidUserToRaidAdmin,
} from "@/services/keycloak";
import { ManageAccounts as ManageAccountsIcon } from "@mui/icons-material";
import {
  Card,
  CardContent,
  CardHeader,
  IconButton,
  ListItem,
  ListItemText,
  Tooltip,
} from "@mui/material";
import List from "@mui/material/List";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";

export const RaidUserManagement = ({
  tokenParsed,
  token,
}: {
  tokenParsed: Record<string, string> | undefined;
  token: string | undefined;
}) => {
  const { prefix, suffix } = useParams() as { prefix: string; suffix: string };
  const adminRaids: string[] = Array.isArray(tokenParsed?.admin_raids)
    ? tokenParsed.admin_raids
    : [];
  const isAdmin = adminRaids.includes(`${prefix}/${suffix}`);
  const queryClient = useQueryClient();
  const snackbar = useSnackbar();

  const promoteRaidUserToRaidAdminMutation = useMutation({
    mutationFn: promoteRaidUserToRaidAdmin,
    onError: (error) => {
      console.error(error);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["servicePoints"],
      });
      snackbar.openSnackbar(`✅ Success: Added user to group admins`);
    },
  });

  const raidUsersQuery = useQuery({
    queryKey: ["raidUsers", prefix, suffix],
    queryFn: () =>
      fetchRaidUsers({
        handle: `${prefix}/${suffix}`,
        token: token,
      }),
    enabled: isAdmin,
  });

  if (raidUsersQuery.isPending) {
    return <Loading />;
  }

  if (raidUsersQuery.isError) {
    return <ErrorAlertComponent error="Failed to fetch RAiD users" />;
  }

  return (
    <Card>
      <CardHeader title="RAiD User Management" />
      <CardContent>
        <List>
          {raidUsersQuery.data?.map(
            (user: { id: string; firstName: string; lastName: string }) => (
              <ListItem
                key={user.id}
                disablePadding
                secondaryAction={
                  <Tooltip title="Promote to RAiD admin" placement="top">
                    <span>
                      <IconButton
                        edge="end"
                        aria-label="revoke"
                        color="success"
                        // disabled={!el?.roles?.includes("service-point-user")}
                        onClick={() => {
                          promoteRaidUserToRaidAdminMutation.mutate({
                            userId: user.id,
                            handle: `${prefix}/${suffix}`,
                            token: token as string,
                          });
                        }}
                      >
                        <ManageAccountsIcon />
                      </IconButton>
                    </span>
                  </Tooltip>
                }
              >
                <ListItemText
                  primary={`${user.firstName} ${user.lastName}`}
                  secondary={user.id}
                />
              </ListItem>
            )
          )}
        </List>
        {isAdmin ? (
          <ListItem disablePadding>
            <ListItemText
              primary="RAiD Admin"
              secondary="You are an admin of this RAiD"
            />
          </ListItem>
        ) : (
          <ListItem disablePadding>
            <ListItemText
              primary="RAiD User"
              secondary="You are a user of this RAiD"
            />
          </ListItem>
        )}
      </CardContent>
    </Card>
  );
};
