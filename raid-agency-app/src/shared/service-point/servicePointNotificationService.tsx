import React from 'react';
import { IconButton, Tooltip } from '@mui/material';
import { 
  PersonAdd as PersonAddIcon,
  CheckCircleOutline as CheckCircleOutlineIcon,
  GroupRemove as GroupRemoveIcon,
} from '@mui/icons-material';
import { useNotificationContext } from '@/components/alert-notifications/notification-context/NotificationsContext';
import { useModifyUserAccess, useRemoveUserFromServicePoint } from '@/containers/header/service-point-users/useServicePointMutation';
import { useSnackbar } from '@/components/snackbar/hooks/useSnackbar';
import { SnackbarContextInterface } from '@/components/snackbar';

export interface ServicePointMember {
  id: string;
  roles: string[];
  attributes: {
    firstName: string;
    lastName: string;
    username: string;
    email: string;
  },
  groupId?: string;
}

export const useServicePointNotification = () => {
  const { addNotification, removeNotification } = useNotificationContext();
  const snackbar: SnackbarContextInterface = useSnackbar();
  const IsnackBar = snackbar as { openSnackbar: (message: string, duration?: number, severity?: string) => void };
  const modifyUserAccessMutation = useModifyUserAccess(IsnackBar);
  const removeUserFromServicePointMutation = useRemoveUserFromServicePoint(IsnackBar);
  const pendingMembers = [] as ServicePointMember[];
  // Transform service point members to notifications
  const transformMemberToNotification = (data: ServicePointMember[], token: string) => {
    // Filter members without 'service-point-user' role
    const requiredRoles = ["service-point-user", "group-admin", "operator"];
    pendingMembers.push(...data?.filter(
      member => !member.roles.some(role => requiredRoles.includes(role))
    ) || [] as ServicePointMember[]);
    // If no pending members, remove notification and return
    if (pendingMembers?.length === 0) {
      // Remove notification if no pending members
      removeNotification('servicePointRequests');
      return;
    }
    // Create notification object
    const notification = {
      title: 'Service Point Pending Requests',
      categories: pendingMembers?.map(member => ({
        titleIcon: <PersonAddIcon />,
        name: `${member.attributes.username || ''} (${member.attributes.firstName || ''} ${member.attributes.lastName || ''})`.replace(/\(\s*\)/g, '').trim(),
        actions: [
          <Tooltip title="Grant membership" placement="top" key="approve">
            <span>
            <IconButton
              key="approve"
              size="small"
              color="success"
              onClick={() => handleApprove({
                member: member as ServicePointMember,
                token: token as string
              })}
              aria-label="Approve"
              sx={{
                bgcolor: 'success.lighter',
                '&:hover': {
                  bgcolor: 'success.veryLighter',
                },
              }}
            >
              <CheckCircleOutlineIcon />
            </IconButton>
          </span>
          </Tooltip>,
          <Tooltip title="Permanently remove from SP" placement="top" key="reject">
            <span>
            <IconButton
              key="reject"
              size="small"
              color="error"
              onClick={() => handleReject({
                member: member as ServicePointMember,
                token: token as string,
              })} 
              aria-label="Reject"
              sx={{
                bgcolor: 'error.lighter',
                '&:hover': {
                  bgcolor: 'error.veryLighter',
                },
              }}
            >
              <GroupRemoveIcon />
            </IconButton>
          </span>
          </Tooltip>
        ],
        // Additional data for future use
        email: member.attributes.email[0],
        username: member.attributes.username[0],
      })),
    };

    addNotification('servicePointRequests', notification);
  };

  const handleApprove = ({ member, token }: {
    member: ServicePointMember,
    token: string,
  }) => modifyUserAccessMutation.mutate({
    userId: member.id,
    userGroupId: member.groupId as string,
    operation: "grant",
    token,
  });

  const handleReject =({ member, token }: {
    member: ServicePointMember,
    token: string,
  }) => removeUserFromServicePointMutation.mutate({
    userId: member.id,
    groupId: member.groupId as string,
    token: token as string,
  });

  return {
    transformMemberToNotification,
    handleApprove,
    handleReject,
  };
};
