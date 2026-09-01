// RAID-659: unit tests for the per-service-point group ID error handling in
// fetchServicePointsWithMembers/fetchServicePointWithMembers. Before this fix,
// a single service point whose Keycloak group could not be resolved (a
// non-ok response, or a thrown network error) caused the whole call to
// reject - see e2e/tests/operator/service-point-group-id-error.spec.ts for
// the corresponding UI-level coverage of GroupIdCell rendering the resulting
// `groupIdError` flag.

import { describe, it, expect, vi, beforeEach } from "vitest";
import type { RuntimeConfig } from "@/config/RuntimeConfig";

vi.mock("@/config", () => ({
  getRuntimeConfig: vi.fn(),
}));

vi.mock("@/services/auth-service.ts", () => ({
  authService: {
    fetchWithAuth: vi.fn(),
  },
}));

import { getRuntimeConfig } from "@/config";
import { authService } from "@/services/auth-service.ts";
import {
  fetchServicePointsWithMembers,
  fetchServicePointWithMembers,
} from "./index";

const mockConfig: Pick<RuntimeConfig, "apiBaseUrl" | "keycloak"> = {
  apiBaseUrl: "https://api.test.raid.org.au",
  keycloak: {
    url: "https://keycloak.test.raid.org.au",
    realm: "raid",
    clientId: "raid-api",
  },
};

const jsonResponse = (body: unknown, ok = true) => ({
  ok,
  json: async () => body,
});

const GOOD_GROUP_ID = "good-group-id";
const BAD_GROUP_ID = "bad-group-id";

describe("fetchServicePointsWithMembers", () => {
  beforeEach(() => {
    vi.mocked(getRuntimeConfig).mockReturnValue(mockConfig as RuntimeConfig);
    vi.mocked(authService.fetchWithAuth).mockReset();
  });

  it("flags only the service point whose group lookup fails, leaving others intact", async () => {
    vi.mocked(authService.fetchWithAuth).mockImplementation(async (url: string) => {
      if (url.includes("/service-point/")) {
        return jsonResponse([
          { id: 1, name: "Good SP", groupId: GOOD_GROUP_ID },
          { id: 2, name: "Bad SP", groupId: BAD_GROUP_ID },
        ]) as Response;
      }
      if (url.includes(`groupId=${GOOD_GROUP_ID}`)) {
        return jsonResponse({ members: [{ id: "member-1" }] }) as Response;
      }
      if (url.includes(`groupId=${BAD_GROUP_ID}`)) {
        return jsonResponse({}, false) as Response;
      }
      throw new Error(`Unexpected URL: ${url}`);
    });

    const result = await fetchServicePointsWithMembers({ token: "t" });

    const good = result.find((sp) => sp.id === 1)!;
    const bad = result.find((sp) => sp.id === 2)!;

    expect(good.groupIdError).toBe(false);
    expect(good.members).toEqual([{ id: "member-1" }]);

    expect(bad.groupIdError).toBe(true);
    expect(bad.members).toEqual([]);
  });

  it("flags a service point whose group lookup throws (network error) rather than rejecting the whole call", async () => {
    vi.mocked(authService.fetchWithAuth).mockImplementation(async (url: string) => {
      if (url.includes("/service-point/")) {
        return jsonResponse([{ id: 1, name: "SP", groupId: BAD_GROUP_ID }]) as Response;
      }
      throw new TypeError("Failed to fetch");
    });

    const result = await fetchServicePointsWithMembers({ token: "t" });

    expect(result).toHaveLength(1);
    expect(result[0].groupIdError).toBe(true);
    expect(result[0].members).toEqual([]);
  });

  it("does not attempt a group lookup, and does not flag an error, when groupId is blank", async () => {
    vi.mocked(authService.fetchWithAuth).mockImplementation(async (url: string) => {
      if (url.includes("/service-point/")) {
        return jsonResponse([{ id: 1, name: "SP", groupId: "  " }]) as Response;
      }
      throw new Error(`Unexpected group lookup for URL: ${url}`);
    });

    const result = await fetchServicePointsWithMembers({ token: "t" });

    expect(result[0].groupIdError).toBe(false);
    expect(result[0].members).toEqual([]);
  });
});

describe("fetchServicePointWithMembers", () => {
  beforeEach(() => {
    vi.mocked(getRuntimeConfig).mockReturnValue(mockConfig as RuntimeConfig);
    vi.mocked(authService.fetchWithAuth).mockReset();
  });

  it("returns groupIdError: true instead of throwing when the group lookup responds non-ok", async () => {
    vi.mocked(authService.fetchWithAuth).mockImplementation(async (url: string) => {
      if (url.includes("/service-point/")) {
        return jsonResponse({ id: 1, name: "SP", groupId: BAD_GROUP_ID }) as Response;
      }
      return jsonResponse({}, false) as Response;
    });

    const result = await fetchServicePointWithMembers({ id: 1, token: "t" });

    expect(result.groupIdError).toBe(true);
    expect(result.members).toEqual([]);
  });

  it("returns groupIdError: true instead of throwing when the group lookup throws", async () => {
    vi.mocked(authService.fetchWithAuth).mockImplementation(async (url: string) => {
      if (url.includes("/service-point/")) {
        return jsonResponse({ id: 1, name: "SP", groupId: BAD_GROUP_ID }) as Response;
      }
      throw new TypeError("Failed to fetch");
    });

    const result = await fetchServicePointWithMembers({ id: 1, token: "t" });

    expect(result.groupIdError).toBe(true);
    expect(result.members).toEqual([]);
  });

  it("still throws when the service point itself cannot be fetched", async () => {
    vi.mocked(authService.fetchWithAuth).mockResolvedValue(
      jsonResponse({}, false) as Response
    );

    await expect(fetchServicePointWithMembers({ id: 1, token: "t" })).rejects.toThrow(
      "Failed to fetch service point"
    );
  });
});
