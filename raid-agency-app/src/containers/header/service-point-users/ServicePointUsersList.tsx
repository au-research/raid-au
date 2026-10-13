import { useSnackbar } from "@/components/snackbar";
import { ServicePointMember, ServicePointWithMembers } from "@/types";
import {
  Avatar,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Paper,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";

import {
  AdminPanelSettings as AdminPanelSettingsIcon,
  CheckCircleOutline as CheckCircleOutlineIcon,
  HighlightOff as HighlightOffIcon,
  MoreVert as MoreVertIcon,
  PersonRemove as PersonRemoveIcon,
  RemoveModerator as RemoveModeratorIcon,
  Schedule as ScheduleIcon,
} from "@mui/icons-material";
import { useMemo, useState } from "react";
import { useKeycloak } from "@/contexts/keycloak-context";
import { useAddUserToGroupAdmins, useModifyUserAccess, useRemoveUserFromGroupAdmins, useRemoveUserFromServicePoint } from "./useServicePointMutation";
import { SnackbarContextInterface } from "@/components/snackbar/SnackbarContext";
import {
  getMemberAccess,
  getMemberDetails,
  matchesFilter,
  MemberAccess,
  MemberDetails,
  MemberFilter,
} from "./memberAccess";

type MemberRow = {
  member: ServicePointMember;
  access: MemberAccess;
  details: MemberDetails;
};

const FILTERS: { value: MemberFilter; label: string; emptyText: string }[] = [
  { value: "all", label: "All", emptyText: "No people in this service point yet." },
  { value: "members", label: "Service Point Users", emptyText: "No approved Service Point Users yet." },
  { value: "pending", label: "Pending approval", emptyText: "No requests waiting for approval." },
  { value: "admins", label: "Service Point Admins", emptyText: "No Service Point Admins for this service point." },
];

// Where the list is not given a height to fill (the operator page stacks one
// list per service point), cap it so a large service point doesn't run on
// for pages.
const UNBOUNDED_LIST_MAX_HEIGHT = "max(240px, 60vh)";

// Lets an element shrink below its content in a flex column, so the rows -
// not the filters or header - give up height when space runs short.
const shrinkable = { display: "flex", flexDirection: "column", minHeight: 0 } as const;

const MemberChips = ({ access }: { access: MemberAccess }) => (
  <Stack direction="row" spacing={0.75} useFlexGap flexWrap="wrap" sx={{ mt: 0.5 }}>
    {access.isApproved ? (
      <Chip size="small" variant="outlined" color="success" icon={<CheckCircleOutlineIcon />} label="Service Point User" />
    ) : (
      <Chip size="small" variant="outlined" color="warning" icon={<ScheduleIcon />} label="Pending approval" />
    )}
    {access.isAdmin && (
      <Chip size="small" variant="outlined" color="primary" icon={<AdminPanelSettingsIcon />} label="Service Point Admin" />
    )}
  </Stack>
);

export const ServicePointUsersList = ({
  servicePointWithMembers,
  fillHeight = false,
}: {
  servicePointWithMembers?: ServicePointWithMembers;
  /**
   * Fill the height of a bounded flex-column parent: the list grows with its
   * content and only its rows scroll once they run out of room, keeping the
   * filters and the User / Actions header in view.
   */
  fillHeight?: boolean;
}) => {
  const { token } = useKeycloak();
  const snackbar: SnackbarContextInterface = useSnackbar();
  const IsnackBar = snackbar as { openSnackbar: (message: string, duration?: number, severity?: string) => void };
  // Define mutation hooks at the top of the component
  const modifyUserAccessMutation = useModifyUserAccess(IsnackBar);
  const removeUserFromServicePointMutation = useRemoveUserFromServicePoint(IsnackBar);
  const removeUserFromGroupAdminsMutation = useRemoveUserFromGroupAdmins(IsnackBar);
  const addUserToGroupAdminsMutation = useAddUserToGroupAdmins(IsnackBar);

  const groupId = servicePointWithMembers?.groupId;
  const [filter, setFilter] = useState<MemberFilter>("all");
  const [menu, setMenu] = useState<{ anchorEl: HTMLElement; row: MemberRow } | null>(null);
  const [rowToRemove, setRowToRemove] = useState<MemberRow | null>(null);

  // Operators manage every service point, so they are not listed (or counted)
  // here. Pending requests come first, then alphabetical by name.
  const rows = useMemo<MemberRow[]>(
    () =>
      (servicePointWithMembers?.members || [])
        .map((member) => ({
          member,
          access: getMemberAccess(member.roles, groupId),
          details: getMemberDetails(member),
        }))
        .filter((row) => !row.access.isOperator)
        .sort(
          (a, b) =>
            Number(a.access.isApproved) - Number(b.access.isApproved) ||
            a.details.name.localeCompare(b.details.name)
        ),
    [servicePointWithMembers?.members, groupId]
  );

  const counts = useMemo(
    () =>
      Object.fromEntries(
        FILTERS.map(({ value }) => [value, rows.filter((row) => matchesFilter(row.access, value)).length])
      ) as Record<MemberFilter, number>,
    [rows]
  );

  const visibleRows = rows.filter((row) => matchesFilter(row.access, filter));
  const activeFilter = FILTERS.find((f) => f.value === filter) ?? FILTERS[0];

  const closeMenu = () => setMenu(null);

  const setMembership = (row: MemberRow, operation: "grant" | "revoke") => {
    closeMenu();
    modifyUserAccessMutation.mutate({
      userId: row.member.id,
      userGroupId: groupId as string,
      operation,
      token: token as string,
    });
  };

  const setAdmin = (row: MemberRow, makeAdmin: boolean) => {
    closeMenu();
    const variables = { userId: row.member.id, groupId: groupId as string, token: token as string };
    if (makeAdmin) {
      addUserToGroupAdminsMutation.mutate(variables);
    } else {
      removeUserFromGroupAdminsMutation.mutate(variables);
    }
  };

  const confirmRemove = () => {
    if (!rowToRemove) return;
    removeUserFromServicePointMutation.mutate({
      userId: rowToRemove.member.id,
      groupId: groupId as string,
      token: token as string,
    });
    setRowToRemove(null);
  };

  const menuRow = menu?.row;

  return (
    <>
      <Paper variant="outlined" sx={{ p: 2, ...shrinkable }}>
        <Stack gap={2} sx={{ minHeight: 0 }}>
          <Typography variant="h6">{servicePointWithMembers?.name}</Typography>

          <ToggleButtonGroup
            size="small"
            exclusive
            value={filter}
            onChange={(_, value: MemberFilter | null) => value && setFilter(value)}
            aria-label="Show"
            sx={{ alignSelf: "flex-start", flexWrap: "wrap" }}
          >
            {FILTERS.map(({ value, label }) => {
              const needsAttention = value === "pending" && counts.pending > 0 && filter !== "pending";
              return (
                <ToggleButton key={value} value={value} sx={{ textTransform: "none", gap: 1 }}>
                  {label}
                  <Box
                    component="span"
                    sx={{
                      minWidth: 22,
                      px: 0.75,
                      borderRadius: 999,
                      fontSize: 12,
                      fontVariantNumeric: "tabular-nums",
                      bgcolor: needsAttention ? "warning.main" : "action.selected",
                      color: needsAttention ? "warning.contrastText" : "text.primary",
                    }}
                  >
                    {counts[value]}
                  </Box>
                </ToggleButton>
              );
            })}
          </ToggleButtonGroup>

          <Box sx={shrinkable}>
            <Box
              aria-hidden
              sx={{
                display: "flex",
                justifyContent: "space-between",
                flexShrink: 0,
                px: 2,
                py: 0.5,
                borderBottom: 1,
                borderColor: "divider",
              }}
            >
              <Typography variant="overline" color="text.secondary">User</Typography>
              <Typography variant="overline" color="text.secondary">Actions</Typography>
            </Box>

            <Box
              sx={{
                minHeight: 0,
                overflowY: "auto",
                maxHeight: fillHeight ? undefined : UNBOUNDED_LIST_MAX_HEIGHT,
              }}
            >
              {visibleRows.length === 0 ? (
                <Typography color="text.secondary" sx={{ px: 2, py: 2 }}>
                  {activeFilter.emptyText}
                </Typography>
              ) : (
                <List disablePadding aria-label={`${activeFilter.label} people`}>
                  {visibleRows.map((row) => {
                    const { member, access, details } = row;
                    const isMenuOpen = menuRow?.member.id === member.id;
                    return (
                      <ListItem
                        key={member.id}
                        divider
                        secondaryAction={
                          <IconButton
                            edge="end"
                            aria-label={`Actions for ${details.name}`}
                            aria-haspopup="menu"
                            aria-expanded={isMenuOpen}
                            aria-controls={isMenuOpen ? "service-point-member-actions" : undefined}
                            onClick={(event) => setMenu({ anchorEl: event.currentTarget, row })}
                          >
                            <MoreVertIcon />
                          </IconButton>
                        }
                      >
                        <ListItemAvatar>
                          <Avatar sx={{ bgcolor: "primary.main", width: 36, height: 36, fontSize: 14 }}>
                            {details.initials}
                          </Avatar>
                        </ListItemAvatar>
                        <ListItemText
                          primary={details.name}
                          primaryTypographyProps={{ fontWeight: 600 }}
                          secondaryTypographyProps={{ component: "div" }}
                          secondary={
                            <>
                              <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
                                {[details.username, details.email].filter(Boolean).join(" · ")}
                              </Typography>
                              <MemberChips access={access} />
                            </>
                          }
                        />
                      </ListItem>
                    );
                  })}
                </List>
              )}
            </Box>
          </Box>
        </Stack>
      </Paper>

      <Menu
        id="service-point-member-actions"
        anchorEl={menu?.anchorEl}
        open={!!menu}
        onClose={closeMenu}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
        transformOrigin={{ vertical: "top", horizontal: "right" }}
      >
        {menuRow &&
          (menuRow.access.isApproved ? (
            <MenuItem onClick={() => setMembership(menuRow, "revoke")}>
              <ListItemIcon><HighlightOffIcon fontSize="small" /></ListItemIcon>
              <ListItemText>Revoke Service Point User</ListItemText>
            </MenuItem>
          ) : (
            <MenuItem onClick={() => setMembership(menuRow, "grant")}>
              <ListItemIcon><CheckCircleOutlineIcon fontSize="small" /></ListItemIcon>
              <ListItemText>Approve as Service Point User</ListItemText>
            </MenuItem>
          ))}
        {menuRow?.access.isAdmin && (
          <MenuItem onClick={() => setAdmin(menuRow, false)}>
            <ListItemIcon><RemoveModeratorIcon fontSize="small" /></ListItemIcon>
            <ListItemText>Remove Service Point Admin</ListItemText>
          </MenuItem>
        )}
        {/* The backend allows this for anyone in the group, but only approved
            Service Point Users are offered it. */}
        {menuRow && !menuRow.access.isAdmin && menuRow.access.isApproved && (
          <MenuItem onClick={() => setAdmin(menuRow, true)}>
            <ListItemIcon><AdminPanelSettingsIcon fontSize="small" /></ListItemIcon>
            <ListItemText>Make Service Point Admin</ListItemText>
          </MenuItem>
        )}
        {menuRow && <Divider />}
        {menuRow && (
          <MenuItem
            onClick={() => {
              closeMenu();
              setRowToRemove(menuRow);
            }}
            sx={{ color: "error.main" }}
          >
            <ListItemIcon sx={{ color: "inherit" }}><PersonRemoveIcon fontSize="small" /></ListItemIcon>
            <ListItemText>Remove from service point…</ListItemText>
          </MenuItem>
        )}
      </Menu>

      <Dialog
        open={!!rowToRemove}
        onClose={() => setRowToRemove(null)}
        aria-labelledby="remove-member-title"
        aria-describedby="remove-member-description"
      >
        <DialogTitle id="remove-member-title">
          Remove {rowToRemove?.details.name} from this service point?
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="remove-member-description">
            They will lose access to {servicePointWithMembers?.name ?? "this service point"} and will
            need to request to join again.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRowToRemove(null)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={confirmRemove}>
            Remove
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};
