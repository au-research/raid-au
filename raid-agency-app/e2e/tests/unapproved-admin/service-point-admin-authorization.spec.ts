// RAID-608 / HELP-2844: a user elevated to the flat `group-admin` role, but
// never actually approved (granted service-point-user) for a given service
// point, was able to approve access requests for that service point via the
// notification centre despite never being explicitly granted access. Fixed
// in iam/src/main/java/au/org/raid/iam/provider/group/GroupController.java
// (isGroupAdminOf's flat fallback now requires isApprovedGroupMember, not
// just group membership) - see GroupServicePointAdminIntegrationTest.java
// for the backend-level coverage this e2e test complements.
//
// isGroupAdminOf gates every group-scoped IAM endpoint uniformly, including
// the GET used to list a group's members - so the notification centre's own
// query is denied for this admin the exact same way grant() is, and it never
// surfaces the pending request to them in the first place. That means there
// is no UI path to click through to "Approve" here (which is itself part of
// what the fix guarantees), so this test verifies the fix directly against
// the API the UI's Approve button calls, using the unapproved admin's own
// session token, plus a UI-level check that the app honestly shows them as
// unapproved rather than silently offering a working approve action.
//
// Uses two dedicated fixture users (see iam/doc/keycloak-configuration.md):
//   - raid-au-unapproved-admin: group-admin role, raw self-joined member of
//     the raid-au group, never granted service-point-user for it.
//   - raid-au-pending-user: raw self-joined member of raid-au with no roles,
//     i.e. a colleague's outstanding access request.
//
// Runs against the dedicated unapproved-admin session (chromium-unapproved-
// -admin project).

import { test, expect, type APIRequestContext } from "@playwright/test";

const RAID_AU_GROUP_ID = "169bd3f3-dd42-4ac0-b89a-fb49648e5eff";
const PENDING_USERNAME = "raid-au-pending-user";

async function getAccessToken(
  request: APIRequestContext,
  { username, password }: { username: string; password: string }
): Promise<string> {
  const keycloakUrl = process.env.VITE_KEYCLOAK_URL!;
  const realm = process.env.VITE_KEYCLOAK_REALM!;
  const clientId = process.env.VITE_KEYCLOAK_CLIENT_ID!;

  const response = await request.post(
    `${keycloakUrl}/realms/${realm}/protocol/openid-connect/token`,
    {
      form: {
        grant_type: "password",
        client_id: clientId,
        username,
        password,
      },
    }
  );
  const body = await response.json();
  return body.access_token;
}

test.describe("Service point admin authorization: unapproved group-admin", () => {
  test(
    "cannot approve a pending access request for a service point they are not an approved member of",
    { tag: "@local" },
    async ({ page, request }) => {
      const keycloakUrl = process.env.VITE_KEYCLOAK_URL!;
      const realm = process.env.VITE_KEYCLOAK_REALM!;

      // UI-level: the app must honestly reflect that this admin has no
      // approved access, rather than silently offering a working approve
      // action it can't actually back up.
      await page.goto("/");
      await expect(
        page.getByText(/has not granted you access yet/i)
      ).toBeVisible({ timeout: 15000 });

      const bell = page.getByRole("button").filter({
        has: page.getByTestId("NotificationsIcon"),
      });
      await bell.click();
      await expect(page.getByText("0 pending requests")).toBeVisible({
        timeout: 15000,
      });
      await expect(page.getByText("No notifications")).toBeVisible();

      // Look up the pending colleague's Keycloak user id via the operator
      // account - test setup only. The security assertion below is made
      // with the unapproved admin's own token, not the operator's.
      const operatorToken = await getAccessToken(request, {
        username: process.env.VITE_KEYCLOAK_E2E_OPERATOR_USER!,
        password: process.env.VITE_KEYCLOAK_E2E_OPERATOR_PASSWORD!,
      });
      const groupResponse = await request.get(
        `${keycloakUrl}/realms/${realm}/group/?groupId=${RAID_AU_GROUP_ID}`,
        { headers: { Authorization: `Bearer ${operatorToken}` } }
      );
      expect(groupResponse.ok()).toBe(true);
      const group = await groupResponse.json();
      const pendingMember = (
        group.members as { id: string; attributes: { username: string[] } }[]
      ).find((member) => member.attributes.username?.[0] === PENDING_USERNAME);
      expect(pendingMember, "expected raid-au-pending-user to be a member of raid-au").toBeTruthy();

      // Attempt the exact request the UI's "Approve" button issues
      // (updateUserServicePointUserRole -> PUT /group/grant), using the
      // unapproved admin's own token.
      const adminToken = await getAccessToken(request, {
        username: process.env.VITE_KEYCLOAK_E2E_UNAPPROVED_ADMIN_USER!,
        password: process.env.VITE_KEYCLOAK_E2E_UNAPPROVED_ADMIN_PASSWORD!,
      });
      const grantResponse = await request.put(
        `${keycloakUrl}/realms/${realm}/group/grant`,
        {
          headers: {
            Authorization: `Bearer ${adminToken}`,
            "Content-Type": "application/json",
          },
          data: { userId: pendingMember!.id, groupId: RAID_AU_GROUP_ID },
        }
      );

      // The backend must reject the approval - the admin has no approved
      // membership of raid-au, only a flat group-admin role. JAX-RS's
      // NotAuthorizedException maps to 401 (see GroupController.grant()),
      // but assert loosely against 401/403 to match the existing backend
      // test convention (GroupServicePointAdminIntegrationTest#assertDenied).
      expect([401, 403]).toContain(grantResponse.status());
    }
  );
});
