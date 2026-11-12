import { describe, expect, it } from "vitest";
import type { ServicePointMember } from "@/types";
import { getMemberAccess, getMemberDetails, matchesFilter } from "./memberAccess";

const GROUP_ID = "group-a";

describe("getMemberAccess", () => {
  it("treats a member as admin via the scoped role for this service point", () => {
    expect(
      getMemberAccess(["service-point-user", `service-point-admin:${GROUP_ID}`], GROUP_ID).isAdmin
    ).toBe(true);
  });

  // RAID-919: the flat group-admin role is held by anyone who administers any
  // service point, so it must not make them look like an admin of this one.
  it("does not treat an admin of other service points as admin here", () => {
    const access = getMemberAccess(
      ["service-point-user", "group-admin", "service-point-admin:group-b", "service-point-admin:group-c"],
      GROUP_ID
    );
    expect(access.isAdmin).toBe(false);
    expect(access.isApproved).toBe(true);
  });

  it("reports a member without service-point-user as not approved", () => {
    expect(getMemberAccess(["default-roles-raid"], GROUP_ID)).toEqual({
      isApproved: false,
      isAdmin: false,
      isOperator: false,
    });
  });

  it("reports operators", () => {
    expect(getMemberAccess(["operator"], GROUP_ID).isOperator).toBe(true);
  });

  it("handles missing roles and group id", () => {
    expect(getMemberAccess(undefined, undefined)).toEqual({
      isApproved: false,
      isAdmin: false,
      isOperator: false,
    });
    expect(getMemberAccess(["service-point-admin:undefined"], undefined).isAdmin).toBe(false);
  });
});

describe("getMemberDetails", () => {
  const member = (attributes: Partial<ServicePointMember["attributes"]>) =>
    ({
      id: "1",
      groupId: GROUP_ID,
      roles: [],
      attributes: { username: ["sam.okafor"], firstName: [], lastName: [], email: [], activeGroupId: [], ...attributes },
    }) as ServicePointMember;

  it("uses the full name and its initials", () => {
    expect(
      getMemberDetails(member({ firstName: ["Sam"], lastName: ["Okafor"], email: ["sam@example.org"] }))
    ).toEqual({ username: "sam.okafor", email: "sam@example.org", name: "Sam Okafor", initials: "SO" });
  });

  it("falls back to the username when no name is set", () => {
    const details = getMemberDetails(member({ firstName: [null], lastName: [null] }));
    expect(details.name).toBe("sam.okafor");
    expect(details.initials).toBe("S");
  });
});

describe("matchesFilter", () => {
  const pending = { isApproved: false, isAdmin: false, isOperator: false };
  const admin = { isApproved: true, isAdmin: true, isOperator: false };

  it("matches everyone for all", () => {
    expect(matchesFilter(pending, "all")).toBe(true);
    expect(matchesFilter(admin, "all")).toBe(true);
  });

  it("matches only approved Service Point Users for members", () => {
    expect(matchesFilter(admin, "members")).toBe(true);
    expect(matchesFilter(pending, "members")).toBe(false);
  });

  it("matches only unapproved members for pending", () => {
    expect(matchesFilter(pending, "pending")).toBe(true);
    expect(matchesFilter(admin, "pending")).toBe(false);
  });

  it("matches only admins of this service point for admins", () => {
    expect(matchesFilter(admin, "admins")).toBe(true);
    expect(matchesFilter(pending, "admins")).toBe(false);
  });
});
