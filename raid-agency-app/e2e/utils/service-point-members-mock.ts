// RAID-919: a stand-in for the Keycloak group endpoints behind a service
// point's Users list, so e2e tests can render a known set of people (pending
// requests, admins of other service points, operators) and exercise every
// action without changing real Keycloak users.
//
// It answers the members GET for any group with the people below, building
// each person's realm roles the way the real endpoint does (Keycloak
// defaults, the flat group-admin role and one scoped
// "service-point-admin:<groupId>" per service point administered). The
// grant / revoke / group-admin / active-group / leave calls are recorded and
// applied to that state, so the list refreshes just as it would for real.

import type { Page, Request, Route } from "@playwright/test";

export type MockPerson = {
  id: string;
  firstName: string;
  lastName: string;
  /** Holds service-point-user */
  approved: boolean;
  /** Holds service-point-admin:<this group> */
  adminHere?: boolean;
  /** Group ids of other service points this person administers */
  adminElsewhere?: string[];
  operator?: boolean;
};

export type RecordedCall = {
  method: string;
  path: string;
  body: Record<string, unknown> | null;
};

const KEYCLOAK_DEFAULT_ROLES = ["default-roles-raid", "offline_access", "uma_authorization"];

const rolesFor = (person: MockPerson, groupId: string): string[] => {
  const scoped = [
    ...(person.adminHere ? [groupId] : []),
    ...(person.adminElsewhere ?? []),
  ].map((id) => `service-point-admin:${id}`);
  return [
    ...KEYCLOAK_DEFAULT_ROLES,
    ...(person.approved ? ["service-point-user"] : []),
    ...(scoped.length > 0 ? ["group-admin"] : []),
    ...scoped,
    ...(person.operator ? ["operator"] : []),
  ];
};

const toMember = (person: MockPerson, groupId: string) => {
  const username = `${person.firstName}.${person.lastName}`.toLowerCase();
  return {
    id: person.id,
    roles: rolesFor(person, groupId),
    attributes: {
      username: [username],
      firstName: [person.firstName],
      lastName: [person.lastName],
      email: [`${username}@example.edu.au`],
      activeGroupId: [groupId],
    },
  };
};

// The app (on BASE_URL) calls Keycloak on another origin, with credentials.
const corsHeaders = (request: Request) => ({
  "access-control-allow-origin": request.headers()["origin"] ?? "*",
  "access-control-allow-credentials": "true",
  "access-control-allow-methods": "GET, PUT, DELETE, OPTIONS",
  "access-control-allow-headers": "authorization, content-type",
});

const isGroupEndpoint = (url: URL) => /\/realms\/[^/]+\/group(\/|$)/.test(url.pathname);

/** The group sub-path, e.g. "" for the members GET or "grant" for PUT /group/grant. */
const groupAction = (url: URL) => url.pathname.replace(/^.*?\/realms\/[^/]+\/group\/?/, "");

export async function mockServicePointMembers(page: Page, people: MockPerson[]) {
  const state = people.map((person) => ({ ...person }));
  const calls: RecordedCall[] = [];
  const find = (userId: unknown) => state.find((person) => person.id === userId);

  await page.route(
    (url) => isGroupEndpoint(url),
    async (route: Route) => {
      const request = route.request();
      const url = new URL(request.url());
      const method = request.method();
      const action = groupAction(url);

      if (method === "OPTIONS") {
        return route.fulfill({ status: 204, headers: corsHeaders(request) });
      }

      // Anything else on /group (e.g. /group/all, /group/user-groups) is
      // passed through to the real Keycloak.
      if (method === "GET" && action === "" && url.searchParams.has("groupId")) {
        const groupId = url.searchParams.get("groupId")!;
        return route.fulfill({
          status: 200,
          headers: { ...corsHeaders(request), "content-type": "application/json" },
          body: JSON.stringify({
            id: groupId,
            name: "e2e service point group",
            attributes: {},
            members: state.map((person) => toMember(person, groupId)),
          }),
        });
      }

      const handled: Record<string, (body: Record<string, unknown>) => void> = {
        "PUT grant": (body) => { const p = find(body.userId); if (p) p.approved = true; },
        "PUT revoke": (body) => { const p = find(body.userId); if (p) p.approved = false; },
        "PUT group-admin": (body) => { const p = find(body.userId); if (p) p.adminHere = true; },
        "DELETE group-admin": (body) => { const p = find(body.userId); if (p) p.adminHere = false; },
        "DELETE active-group": () => {},
        "PUT leave": (body) => {
          const index = state.findIndex((person) => person.id === body.userId);
          if (index >= 0) state.splice(index, 1);
        },
      };
      const handler = handled[`${method} ${action}`];
      if (!handler) {
        return route.continue();
      }

      const body = (request.postDataJSON() as Record<string, unknown> | null) ?? {};
      calls.push({ method, path: action, body });
      handler(body);
      return route.fulfill({
        status: 200,
        headers: { ...corsHeaders(request), "content-type": "application/json" },
        body: "{}",
      });
    }
  );

  return {
    /** Mutating calls made so far, in order. */
    calls,
    /** Mutating calls to one sub-path, e.g. callsTo("PUT", "grant"). */
    callsTo: (method: string, path: string) =>
      calls.filter((call) => call.method === method && call.path === path),
  };
}

/** A larger, fictional service point, for checking the list scrolls once it outgrows the screen. */
export function manyPeople(count: number): MockPerson[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `bulk-${i}`,
    firstName: `Person${String(i).padStart(2, "0")}`,
    lastName: "Example",
    approved: i % 5 !== 0,
  }));
}
