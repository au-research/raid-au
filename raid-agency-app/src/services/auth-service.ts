// src/services/authService.ts
import { getKeycloakInstance } from "@/auth/keycloak";

// Define roles as readonly const to ensure type safety
export const REALM_ROLES = {
    SERVICE_POINT_USER: "service-point-user",
    GROUP_ADMIN: "group-admin",
    OPERATOR: "operator",
} as const;

// Type for realm roles
export type RealmRole = (typeof REALM_ROLES)[keyof typeof REALM_ROLES];

export const authService = {
    // Token management
    async getValidToken(): Promise<string> {
        try {
            // Try to refresh when token has less than 30 seconds remaining
            const refreshed = await getKeycloakInstance().updateToken(30);
            if (refreshed) {
                console.log("Token was successfully refreshed");
            }
            return getKeycloakInstance().token || '';
        } catch (error) {
            console.error('Failed to refresh token:', error);
            throw error;
        }
    },

    // Fetch with authentication
    async fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
        try {
            const validToken = await this.getValidToken();

            const authOptions = {
                ...options,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers,
                    Authorization: `Bearer ${validToken}`,
                },
            };

            return fetch(url, authOptions as RequestInit);
        } catch (error) {
            console.error('Error making authenticated request:', error);
            throw error;
        }
    },

    // Role checking
    hasRole(role: RealmRole): boolean {
        return !!getKeycloakInstance().realmAccess?.roles.includes(role);
    },

    // Helper properties
    get hasServicePointGroup(): boolean {
        return Boolean(getKeycloakInstance().tokenParsed?.service_point_group_id);
    },

    get isServicePointUser(): boolean {
        return this.hasRole(REALM_ROLES.SERVICE_POINT_USER);
    },

    get isGroupAdmin(): boolean {
        return this.hasRole(REALM_ROLES.GROUP_ADMIN);
    },

    get isOperator(): boolean {
        return this.hasRole(REALM_ROLES.OPERATOR);
    },

    get groupId(): string | undefined {
        return getKeycloakInstance().tokenParsed?.service_point_group_id;
    },

    get token(): string | undefined {
        return getKeycloakInstance().token;
    },

    get user(): { id: string | undefined; roles: string[] } {
        return {
            id: getKeycloakInstance().subject,
            roles: getKeycloakInstance().realmAccess?.roles || [],
            // Add other user properties if needed
        };
    }
};