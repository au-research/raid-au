import { REALM_ROLES } from "@/services/auth-service";
import { ServicePointMember } from "@/types";

export type MemberAccess = {
  /** Holds service-point-user, i.e. their membership has been approved */
  isApproved: boolean;
  /** Holds the scoped "service-point-admin:<groupId>" role for this service point */
  isAdmin: boolean;
  isOperator: boolean;
};

// RAID-919: since RAID-884 a member's roles include one scoped
// "service-point-admin:<groupId>" per service point they administer, plus the
// flat group-admin role and Keycloak's defaults. Only the scoped role for the
// service point being viewed says whether the member is an admin *here* - the
// flat group-admin role is held by anyone who administers any service point.
export const getMemberAccess = (
  roles: string[] | undefined,
  groupId: string | undefined
): MemberAccess => {
  const memberRoles = roles ?? [];
  return {
    isApproved: memberRoles.includes(REALM_ROLES.SERVICE_POINT_USER),
    isAdmin: !!groupId && memberRoles.includes(`service-point-admin:${groupId}`),
    isOperator: memberRoles.includes(REALM_ROLES.OPERATOR),
  };
};

export type MemberDetails = {
  username: string;
  email: string;
  /** Full name, falling back to the username when no name is set */
  name: string;
  initials: string;
};

export const getMemberDetails = (member: ServicePointMember): MemberDetails => {
  const attributes = member.attributes;
  const username = attributes?.username?.[0] ?? "";
  const firstName = attributes?.firstName?.[0] ?? "";
  const lastName = attributes?.lastName?.[0] ?? "";
  const email = attributes?.email?.[0] ?? "";
  const name = `${firstName} ${lastName}`.trim() || username;
  const initials =
    (firstName || lastName
      ? `${firstName.charAt(0)}${lastName.charAt(0)}`
      : username.charAt(0)
    ).toUpperCase();
  return { username, email, name, initials };
};

export type MemberFilter = "all" | "members" | "pending" | "admins";

export const matchesFilter = (access: MemberAccess, filter: MemberFilter): boolean => {
  switch (filter) {
    case "members":
      return access.isApproved;
    case "pending":
      return !access.isApproved;
    case "admins":
      return access.isAdmin;
    default:
      return true;
  }
};
