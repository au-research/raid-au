import { ServicePoint } from "@/generated/raid";
import {
  CreateServicePointRequest,
  ServicePointMember,
  ServicePointWithMembers,
  UpdateServicePointRequest,
} from "@/types";
import { authService } from "@/services/auth-service.ts";
import { API_CONSTANTS } from "@/constants/apiConstants";
import { getRuntimeConfig } from "@/config";

const kcGroupBase = () => {
  const { keycloak } = getRuntimeConfig();
  return `${keycloak.url}/realms/${keycloak.realm}/group`;
};

export const fetchServicePoints = async ({
  token,
}: {
  token: string;
}): Promise<ServicePoint[]> => {
  const response = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.ALL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });
  return await response.json();
};

export const fetchServicePointsWithMembers = async ({
  token,
}: {
  token: string;
}): Promise<ServicePointWithMembers[]> => {
  const members = new Map<string, ServicePointMember[]>();
  const groupIdErrors = new Set<string>();

  const servicePointResponse = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.ALL, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });
  const servicePoints = await servicePointResponse.json();

  for (const servicePoint of servicePoints) {
    if (servicePoint.groupId && servicePoint.groupId.trim() !== "") {
      try {
        const servicePointMembersResponse = await authService.fetchWithAuth(
          `${kcGroupBase()}?groupId=${servicePoint.groupId}`,
          {
            method: "GET",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
          }
        );
        if (!servicePointMembersResponse.ok) {
          groupIdErrors.add(servicePoint.groupId);
          members.set(servicePoint.groupId, []);
        } else {
          const servicePointMembers = await servicePointMembersResponse.json();
          members.set(servicePoint.groupId, servicePointMembers.members as ServicePointMember[]);
        }
      } catch {
        groupIdErrors.add(servicePoint.groupId);
        members.set(servicePoint.groupId, []);
      }
    } else {
      members.set(servicePoint.groupId, []);
    }
  }

  return servicePoints.map((servicePoint: ServicePoint) => ({
    ...servicePoint,
    members: members.has(servicePoint?.groupId as string)
      ? members.get(servicePoint.groupId as string)
      : [],
    groupIdError: servicePoint.groupId ? groupIdErrors.has(servicePoint.groupId) : false,
  }));
};

export const fetchServicePointWithMembers = async ({
  id,
  token,
}: {
  id: number;
  token: string;
}): Promise<ServicePointWithMembers> => {
  const members = new Map<string, ServicePointMember[]>();

  const servicePointResponse = await authService.fetchWithAuth(
    API_CONSTANTS.SERVICE_POINT.BY_ID(id),
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    }
  );

  if (!servicePointResponse.ok) {
    throw new Error(`Failed to fetch service point: ${servicePointResponse.status}`);
  }

  const servicePoint = await servicePointResponse.json();

  if (servicePoint.groupId) {
    try {
      const servicePointMembersResponse = await authService.fetchWithAuth(
        `${kcGroupBase()}?groupId=${servicePoint.groupId}`,
        {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!servicePointMembersResponse.ok) {
        return { ...servicePoint, members: [], groupIdError: true };
      }

      const servicePointMembers = await servicePointMembersResponse.json();
      members.set(servicePoint.groupId, servicePointMembers.members as ServicePointMember[]);
    } catch {
      return { ...servicePoint, members: [], groupIdError: true };
    }
  } else {
    members.set(servicePoint.groupId, []);
  }

  return {
    ...servicePoint,
    members:
      servicePoint.groupId && members.has(servicePoint.groupId)
        ? members.get(servicePoint.groupId)
        : [],
  };
};

export const fetchServicePoint = async ({
  id,
  token,
}: {
  id: number;
  token: string;
}): Promise<ServicePoint> => {
  const response = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.BY_ID(id), {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });
  return await response.json();
};

export const createServicePoint = async ({
  data,
  token,
}: {
  data: CreateServicePointRequest;
  token: string;
}): Promise<ServicePoint> => {
  let groupId;

  try {
    const group = await authService.fetchWithAuth(`${kcGroupBase()}/create`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        name: data.servicePointCreateRequest.name,
        path: `/groups/${data.servicePointCreateRequest.name}`,
      }),
    });

    if (!group.ok) {
      throw new Error(`Failed to create group: ${group.status} ${group.statusText}`);
    }

    const groupResult = await group.json();
    groupId = groupResult.id;
    data.servicePointCreateRequest.groupId = groupId;

    const response = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.ALL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data.servicePointCreateRequest),
    });

    if (!response.ok) {
      throw new Error(`Failed to create service point: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Error in createServicePoint:", error);
    if (groupId) {
      await deleteServicePointGroup(groupId, token);
    }
    throw error;
  }
};

export const updateServicePoint = async ({
  id,
  data,
  token,
}: {
  id: number;
  data: UpdateServicePointRequest;
  token: string;
}): Promise<ServicePoint> => {
  const response = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.BY_ID(id), {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(data.servicePointUpdateRequest),
  });
  return await response.json();
};

export const updateUserServicePointUserRole = async ({
  userId,
  userGroupId,
  operation,
  token,
}: {
  userId: string;
  userGroupId: string;
  operation: "grant" | "revoke";
  token: string;
}): Promise<ServicePoint> => {
  const response = await authService.fetchWithAuth(`${kcGroupBase()}/${operation}`, {
    method: "PUT",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ userId, groupId: userGroupId }),
  });
  if (!response.ok) {
    throw new Error(`Failed to ${operation}`);
  }
  return response.json();
};

export const addUserToGroupAdmins = async ({
  userId,
  groupId,
  token,
}: {
  userId: string;
  groupId: string;
  token: string;
}): Promise<ServicePoint> => {
  const response = await authService.fetchWithAuth(`${kcGroupBase()}/group-admin`, {
    method: "PUT",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ userId, groupId }),
  });
  if (!response.ok) {
    throw new Error(`Failed to promote user to group admin`);
  }
  return response.json();
};

export const removeUserFromGroupAdmins = async ({
  userId,
  groupId,
  token,
}: {
  userId: string;
  groupId: string;
  token: string;
}): Promise<ServicePoint> => {
  const response = await authService.fetchWithAuth(`${kcGroupBase()}/group-admin`, {
    method: "DELETE",
    credentials: "include",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ userId, groupId }),
  });
  if (!response.ok) {
    throw new Error(`Failed to remove user from group admins`);
  }
  return response.json();
};

export const removeUserFromServicePoint = async ({
  userId,
  groupId,
  token,
}: {
  userId: string;
  groupId: string;
  token: string;
}): Promise<void> => {
  const activeGroupResponse = await authService.fetchWithAuth(
    `${kcGroupBase()}/active-group`,
    {
      method: "DELETE",
      credentials: "include",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ userId }),
    }
  );
  if (!activeGroupResponse.ok) {
    throw new Error(`Failed to remove active group`);
  }

  const removeFromGroupResponse = await authService.fetchWithAuth(
    `${kcGroupBase()}/leave`,
    {
      method: "PUT",
      credentials: "include",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ userId, groupId }),
    }
  );
  if (!removeFromGroupResponse.ok) {
    throw new Error(`Failed to remove user from SP`);
  }
};

export const fetchServicePointMembersWithGroupId = async ({
  id,
  token,
}: {
  id: string;
  token: string;
}): Promise<ServicePointWithMembers> => {
  const response = await authService.fetchWithAuth(
    `${kcGroupBase()}/?groupId=${id}`,
    {
      method: "GET",
      credentials: "include",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    }
  );
  if (!response.ok) {
    throw new Error(`Failed to fetch service point members`);
  }
  return response.json();
};

export const deleteServicePointGroup = async (
  groupId: string | undefined,
  token: string
): Promise<void> => {
  if (!groupId) return;

  const deleteUrl = `${kcGroupBase()}/delete?groupId=${encodeURIComponent(groupId)}`;

  try {
    const response = await authService.fetchWithAuth(deleteUrl, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
    const responseText = await response.text();
    console.log(`Delete group response: ${response.status} ${response.statusText} - ${responseText}`);
    if (!response.ok) {
      console.error(`Failed to delete group: ${response.status} ${response.statusText}`);
    }
  } catch (error) {
    console.error("Error deleting service point group:", error);
  }
};
