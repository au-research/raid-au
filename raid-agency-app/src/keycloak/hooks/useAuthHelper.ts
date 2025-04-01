import { useKeycloak } from "@/contexts/keycloak-context";
import { useCallback, useMemo } from "react";

// Define roles as readonly const to ensure type safety
const REALM_ROLES = {
  SERVICE_POINT_USER: "service-point-user",
  GROUP_ADMIN: "group-admin",
  OPERATOR: "operator",
} as const;

// Type for realm roles
type RealmRole = (typeof REALM_ROLES)[keyof typeof REALM_ROLES];

export function useAuthHelper() {
  const { user } = useKeycloak();
  const { tokenParsed } = useKeycloak();

  // Memoize the role checking function
  const hasRole = useCallback(
    (role: RealmRole): boolean => !!user?.roles?.includes(role),
    [user?.roles]
  );

  // Memoize the return object to prevent unnecessary re-renders
  return useMemo(
    () => ({
      hasServicePointGroup: Boolean(tokenParsed?.service_point_group_id),
      isServicePointUser: hasRole(REALM_ROLES.SERVICE_POINT_USER),
      isGroupAdmin: hasRole(REALM_ROLES.GROUP_ADMIN),
      isOperator: hasRole(REALM_ROLES.OPERATOR),
      groupId: tokenParsed?.service_point_group_id,
      hasRole, // Expose the hasRole function for additional role checks
    }),
    [hasRole, tokenParsed]
  );
}
