// The unapproved-admin e2e fixture (raid-au-unapproved-admin) lives in the
// shared test Keycloak realm, where anyone who administers the raid-au group
// can approve it. Once approved it holds service-point-user, and the RAID-884
// boot hook then also gives it the scoped service-point-admin role. Its spec
// then fails with an unexplained timeout (no "has not granted you access yet"
// alert), and the spec's "must be rejected" grant call would really approve
// raid-au-pending-user. This check runs in the setup project so that drift
// fails up front, with a message saying what to fix.

export const RAID_AU_GROUP_ID = "169bd3f3-dd42-4ac0-b89a-fb49648e5eff";

type AccessTokenClaims = {
  realm_access?: { roles?: string[] };
  service_point_group_id?: string;
};

function decodeClaims(accessToken: string): AccessTokenClaims {
  const payload = accessToken.split(".")[1];
  if (!payload) {
    throw new Error("Not a JWT: expected three dot-separated parts.");
  }
  return JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
}

/** What is wrong with the fixture user's state, in plain words. Empty when it is usable. */
export function unapprovedAdminProblems(
  accessToken: string,
  groupId: string = RAID_AU_GROUP_ID
): string[] {
  const claims = decodeClaims(accessToken);
  const roles = claims.realm_access?.roles ?? [];
  const problems: string[] = [];

  if (!roles.includes("group-admin")) {
    problems.push("it does not hold the flat group-admin role");
  }
  for (const approvalRole of ["service-point-user", `service-point-admin:${groupId}`]) {
    if (roles.includes(approvalRole)) {
      problems.push(`it holds ${approvalRole}, so it has been approved`);
    }
  }
  if (claims.service_point_group_id !== groupId) {
    problems.push(
      `its active group (service_point_group_id) is ${claims.service_point_group_id ?? "not set"}, not raid-au (${groupId})`
    );
  }
  return problems;
}

export function assertUnapprovedAdmin(
  accessToken: string,
  username: string,
  groupId: string = RAID_AU_GROUP_ID
): void {
  const problems = unapprovedAdminProblems(accessToken, groupId);
  if (problems.length > 0) {
    throw new Error(
      `The e2e fixture user ${username} is no longer an unapproved group-admin of raid-au: ` +
        `${problems.join("; ")}. ` +
        `Reset it in the Keycloak admin console (see iam/doc/keycloak-configuration.md): ` +
        `keep group-admin and raid-au membership, and remove service-point-user and ` +
        `service-point-admin:${groupId}.`
    );
  }
}
