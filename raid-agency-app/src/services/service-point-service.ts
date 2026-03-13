import {ServicePoint} from "@/generated/raid";
import {authService} from "@/services/auth-service.ts";
import {CreateServicePointRequest, ServicePointMember, UpdateServicePointRequest} from "@/types.ts";
import { API_CONSTANTS } from "@/constants/apiConstants.ts";

const kcUrl = import.meta.env.VITE_KEYCLOAK_URL as string;
const kcRealm = import.meta.env.VITE_KEYCLOAK_REALM as string;

export const servicePointService = {
    fetchAll: async (): Promise<ServicePoint[]> => {
        const url = new URL(API_CONSTANTS.SERVICE_POINT.ALL);
        const response = await authService.fetchWithAuth(url.toString());
        return await response.json();
    },
    fetchAllWithMembers: async () => {
        const servicePointUrl = new URL(API_CONSTANTS.SERVICE_POINT.ALL);
        const servicePointMembersUrl = `${kcUrl}/realms/${kcRealm}/group`;

        const members = new Map<string, ServicePointMember[]>();

        const servicePointResponse = await authService.fetchWithAuth(servicePointUrl.toString());
        const servicePoints = await servicePointResponse.json();

        // Fetch members for each service point that has a groupId
        for (const servicePoint of servicePoints) {
            if (servicePoint.groupId) {
                const servicePointMembersResponse = await authService.fetchWithAuth(
                    `${servicePointMembersUrl}?groupId=${servicePoint.groupId}`
                );
                const servicePointMembers = await servicePointMembersResponse.json();
                members.set(
                    servicePoint.groupId,
                    servicePointMembers.members as ServicePointMember[]
                );
            }
        }

        // Combine service points with their members
        return servicePoints.map(
            (servicePoint: ServicePoint) => {
                return {
                    ...servicePoint,
                    members: members.has(servicePoint?.groupId as string)
                        ? members.get(servicePoint.groupId as string)
                        : [],
                };
            }
        );
    },
    fetch: async (id: number): Promise<ServicePoint> => {
        const url = new URL(API_CONSTANTS.SERVICE_POINT.BY_ID(id));
        const response = await authService.fetchWithAuth(url.toString());
        return await response.json();
    },
    fetchWithMembers: async (id: number) => {
        const servicePointUrl = new URL(API_CONSTANTS.SERVICE_POINT.BY_ID(id));
        const servicePointMembersUrl = `${kcUrl}/realms/${kcRealm}/group`;
        const members = new Map<string, ServicePointMember[]>();

        const servicePointResponse = await authService.fetchWithAuth(servicePointUrl.toString());

        if (!servicePointResponse.ok) {
            throw new Error(
                `Failed to fetch service point: ${servicePointResponse.status}`
            );
        }

        const servicePoint = await servicePointResponse.json();

        // Fetch members if the service point has a groupId
        if (servicePoint.groupId) {
            const servicePointMembersResponse = await authService.fetchWithAuth(
                `${servicePointMembersUrl}?groupId=${servicePoint.groupId}`
            );

            if (!servicePointMembersResponse.ok) {
                throw new Error(
                    `Failed to fetch service point members: ${servicePointMembersResponse.status}`
                );
            }

            const servicePointMembers = await servicePointMembersResponse.json();
            members.set(
                servicePoint.groupId,
                servicePointMembers.members as ServicePointMember[]
            );
        }

        // Combine service point with its members
        const servicePointWithMembers = {
            ...servicePoint,
            members:
                servicePoint.groupId && members.has(servicePoint.groupId)
                    ? members.get(servicePoint.groupId)
                    : [],
        };

        return servicePointWithMembers;
    },
    create: async (data: CreateServicePointRequest) => {
        const url = new URL(API_CONSTANTS.SERVICE_POINT.ALL);

        const response = await fetch(url, {
            method: "POST",
            body: JSON.stringify(data.servicePointCreateRequest),
        });
        return await response.json();
    },
    update: async (data: UpdateServicePointRequest, id: number) => {
        const url = new URL(API_CONSTANTS.SERVICE_POINT.BY_ID(id));
        const response = await fetch(url, {
            method: "PUT",
            body: JSON.stringify(data.servicePointUpdateRequest),
        });

        return await response.json();
    }
}