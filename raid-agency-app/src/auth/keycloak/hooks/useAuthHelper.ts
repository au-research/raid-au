import { useKeycloak } from "@/contexts/keycloak-context";
import {useCallback, useMemo, useState} from "react";

// Define roles as readonly const to ensure type safety
const REALM_ROLES = {
    SERVICE_POINT_USER: "service-point-user",
    GROUP_ADMIN: "group-admin",
    OPERATOR: "operator",
} as const;

// Type for realm roles
type RealmRole = (typeof REALM_ROLES)[keyof typeof REALM_ROLES];

export function useAuthHelper() {
    const { user, tokenParsed, updateToken, token } = useKeycloak();
    const [isRefreshing, setIsRefreshing] = useState(false);

    // Memoize the role checking function
    const hasRole = useCallback(
        (role: RealmRole): boolean => !!user?.roles?.includes(role),
        [tokenParsed]
    );

    const getValidToken = async () => {
        try {
            setIsRefreshing(true);
            await updateToken(30);
            return token || '';
        } catch (error) {
            console.error('Failed to refresh token:', error);
            throw error;
        } finally {
            setIsRefreshing(false);
        }
    };

    const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
        try {
            const validToken = await getValidToken();

            const authOptions = {
                ...options,
                headers: {
                    ...options.headers,
                    Authorization: `Bearer ${validToken}`
                }
            };

            return fetch(url, authOptions as RequestInit);
        } catch (error) {
            console.error('Error making authenticated request:', error);
            throw error;
        }
    }

    // Memoize the return object to prevent unnecessary re-renders
    return useMemo(
        () => ({
            hasServicePointGroup: Boolean(tokenParsed?.service_point_group_id),
            isServicePointUser: hasRole(REALM_ROLES.SERVICE_POINT_USER),
            isGroupAdmin: hasRole(REALM_ROLES.GROUP_ADMIN),
            isOperator: hasRole(REALM_ROLES.OPERATOR),
            groupId: tokenParsed?.service_point_group_id,
            hasRole,
            getValidToken,
            fetchWithAuth,
            isRefreshing,
            token,
            user
        }),
        [hasRole, tokenParsed, token, user, isRefreshing]
    );
}
