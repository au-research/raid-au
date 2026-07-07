    
    
import React from "react";
import { useAuthHelper } from "@/auth/keycloak";
import { useQuery } from "@tanstack/react-query";
import { useKeycloak } from "@/contexts/keycloak-context";
import { fetchServicePointsWithMembers, fetchServicePointMembersWithGroupId } from "@/services/service-points";
import { useServicePointNotification } from "./servicePointNotificationService";
import { ServicePointWithMembers } from "@/types";

interface ServicePointMember {
    id: string;
    roles: string[];
    attributes: {
        firstName: string;
        lastName: string;
        username: string;
        email: string;
    };
    groupId?: string;
}

export const useServicePointPendingRequest = () => {
    const { isOperator, groupId, isGroupAdmin } = useAuthHelper();
    const { authenticated, isInitialized, token } = useKeycloak();
    const { transformMemberToNotification } = useServicePointNotification();

    const servicePointsQuery = useQuery({
        queryKey: ["service-point-request", groupId],
        queryFn: async () => {
            if (isOperator) {
                return await fetchServicePointsWithMembers({
                    token: token || ""
                });
            } else if (isGroupAdmin && groupId) {
                return await fetchServicePointMembersWithGroupId({
                    token: token || "",
                    id: groupId
                });
            }
            // This should never be reached due to the enabled condition
            throw new Error("Invalid query state");
        },
        enabled: isInitialized && authenticated && !!token && (isOperator || (isGroupAdmin && !!groupId)),
        refetchInterval: 30000, // Poll every 30 seconds
    });

    React.useEffect(() => {
        if (!servicePointsQuery.data) return;
        // Find the service point where the user is an admin or operator
        const servicePoints = Array.isArray(servicePointsQuery.data)
            ? servicePointsQuery.data
            : [servicePointsQuery.data];

        if (isOperator) {
            // One notification per service point so operators can see which SP each request belongs to
            servicePoints.forEach((sp) => {
                const spMembers = (sp.members ?? []).map(member => ({
                    ...member,
                    groupId: sp.groupId,
                })) as unknown as ServicePointMember[];
                transformMemberToNotification(
                    spMembers,
                    token as string,
                    sp.name || 'Service Point',
                    `servicePointRequests_${sp.groupId || sp.id}`,
                );
            });
        } else if (isGroupAdmin) {
            const adminGroup = servicePoints.find((sp) => {
                if (sp?.id.toString() === groupId) {
                    sp.members.forEach((member) => {
                        member.groupId = groupId;
                    });
                    return true;
                }
                return false;
            }) as ServicePointWithMembers | undefined;
            const spName = adminGroup?.name || (servicePointsQuery.data as ServicePointWithMembers)?.name || 'Service Point';
            transformMemberToNotification(
                adminGroup?.members as unknown as ServicePointMember[] ?? [],
                token as string,
                spName,
                `servicePointRequests_${groupId}`,
            );
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [servicePointsQuery.data, isOperator, isGroupAdmin]);

    const refetch = () => {
        servicePointsQuery.refetch();
    };

    return (
        refetch
    );
};
