import { ServicePoint } from "@/generated/raid";
import { authService } from "@/services/auth-service.ts";
import { CreateServicePointRequest, ServicePointMember, UpdateServicePointRequest } from "@/types.ts";
import { API_CONSTANTS } from "@/constants/apiConstants.ts";
import { getRuntimeConfig } from "@/config";

const kcGroupBase = () => {
  const { keycloak } = getRuntimeConfig();
  return `${keycloak.url}/realms/${keycloak.realm}/group`;
};

export const servicePointService = {
  fetchAll: async (): Promise<ServicePoint[]> => {
    const response = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.ALL);
    return await response.json();
  },

  fetchAllWithMembers: async () => {
    const members = new Map<string, ServicePointMember[]>();

    const servicePointResponse = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.ALL);
    const servicePoints = await servicePointResponse.json();

    for (const servicePoint of servicePoints) {
      if (servicePoint.groupId) {
        const servicePointMembersResponse = await authService.fetchWithAuth(
          `${kcGroupBase()}?groupId=${servicePoint.groupId}`
        );
        const servicePointMembers = await servicePointMembersResponse.json();
        members.set(servicePoint.groupId, servicePointMembers.members as ServicePointMember[]);
      }
    }

    return servicePoints.map((servicePoint: ServicePoint) => ({
      ...servicePoint,
      members: members.has(servicePoint?.groupId as string)
        ? members.get(servicePoint.groupId as string)
        : [],
    }));
  },

  fetch: async (id: number): Promise<ServicePoint> => {
    const response = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.BY_ID(id));
    return await response.json();
  },

  fetchWithMembers: async (id: number) => {
    const members = new Map<string, ServicePointMember[]>();

    const servicePointResponse = await authService.fetchWithAuth(
      API_CONSTANTS.SERVICE_POINT.BY_ID(id)
    );

    if (!servicePointResponse.ok) {
      throw new Error(`Failed to fetch service point: ${servicePointResponse.status}`);
    }

    const servicePoint = await servicePointResponse.json();

    if (servicePoint.groupId) {
      const servicePointMembersResponse = await authService.fetchWithAuth(
        `${kcGroupBase()}?groupId=${servicePoint.groupId}`
      );

      if (!servicePointMembersResponse.ok) {
        throw new Error(`Failed to fetch service point members: ${servicePointMembersResponse.status}`);
      }

      const servicePointMembers = await servicePointMembersResponse.json();
      members.set(servicePoint.groupId, servicePointMembers.members as ServicePointMember[]);
    }

    return {
      ...servicePoint,
      members:
        servicePoint.groupId && members.has(servicePoint.groupId)
          ? members.get(servicePoint.groupId)
          : [],
    };
  },

  create: async (data: CreateServicePointRequest) => {
    const response = await fetch(API_CONSTANTS.SERVICE_POINT.ALL, {
      method: "POST",
      body: JSON.stringify(data.servicePointCreateRequest),
    });
    return await response.json();
  },

  update: async (data: UpdateServicePointRequest, id: number) => {
    const response = await fetch(API_CONSTANTS.SERVICE_POINT.BY_ID(id), {
      method: "PUT",
      body: JSON.stringify(data.servicePointUpdateRequest),
    });
    return await response.json();
  },
};
