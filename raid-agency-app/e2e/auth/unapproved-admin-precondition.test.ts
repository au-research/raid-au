// Pure checks on a decoded token: no browser, no network.
import { test, expect } from "@playwright/test";
import {
  RAID_AU_GROUP_ID,
  assertUnapprovedAdmin,
  unapprovedAdminProblems,
} from "./unapproved-admin-precondition";

function tokenWith(claims: object): string {
  const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");
  return `${encode({ alg: "none" })}.${encode(claims)}.signature`;
}

const unapproved = {
  realm_access: { roles: ["default-roles-raid", "group-admin", "offline_access"] },
  service_point_group_id: RAID_AU_GROUP_ID,
};

test.describe("unapproved-admin fixture precondition", () => {
  test("accepts a flat group-admin with raid-au active and no approval", () => {
    expect(unapprovedAdminProblems(tokenWith(unapproved))).toEqual([]);
    expect(() => assertUnapprovedAdmin(tokenWith(unapproved), "u")).not.toThrow();
  });

  test("rejects a user who has been approved (service-point-user)", () => {
    const token = tokenWith({
      ...unapproved,
      realm_access: { roles: [...unapproved.realm_access.roles, "service-point-user"] },
    });
    expect(unapprovedAdminProblems(token)).toEqual([
      "it holds service-point-user, so it has been approved",
    ]);
  });

  test("rejects a user holding the scoped service-point-admin role for raid-au", () => {
    const token = tokenWith({
      ...unapproved,
      realm_access: {
        roles: [...unapproved.realm_access.roles, `service-point-admin:${RAID_AU_GROUP_ID}`],
      },
    });
    expect(unapprovedAdminProblems(token)).toEqual([
      `it holds service-point-admin:${RAID_AU_GROUP_ID}, so it has been approved`,
    ]);
  });

  test("ignores a scoped admin role for some other group", () => {
    const token = tokenWith({
      ...unapproved,
      realm_access: { roles: [...unapproved.realm_access.roles, "service-point-admin:other"] },
    });
    expect(unapprovedAdminProblems(token)).toEqual([]);
  });

  test("rejects a user without group-admin or without raid-au active", () => {
    const token = tokenWith({ realm_access: { roles: ["default-roles-raid"] } });
    expect(unapprovedAdminProblems(token)).toEqual([
      "it does not hold the flat group-admin role",
      `its active group (service_point_group_id) is not set, not raid-au (${RAID_AU_GROUP_ID})`,
    ]);
  });

  test("names the user, every problem and the fix in the error", () => {
    const token = tokenWith({
      ...unapproved,
      realm_access: { roles: [...unapproved.realm_access.roles, "service-point-user"] },
    });
    expect(() => assertUnapprovedAdmin(token, "raid-au-unapproved-admin")).toThrow(
      /raid-au-unapproved-admin is no longer an unapproved group-admin.*holds service-point-user.*remove service-point-user/
    );
  });
});
