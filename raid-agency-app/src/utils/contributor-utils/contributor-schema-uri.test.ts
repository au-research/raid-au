import { describe, it, expect, vi, beforeEach } from "vitest";
import { RuntimeConfig } from "@/config/RuntimeConfig";

vi.mock("@/config", () => ({
  getRuntimeConfig: vi.fn(),
}));

import { getRuntimeConfig } from "@/config";
import { getContributorSchemaUri } from "./contributor-schema-uri";

describe("getContributorSchemaUri", () => {
  beforeEach(() => {
    vi.mocked(getRuntimeConfig).mockReset();
  });

  it("returns the live ORCID URL in prod", () => {
    vi.mocked(getRuntimeConfig).mockReturnValue({
      environment: "prod",
    } as RuntimeConfig);

    expect(getContributorSchemaUri()).toBe("https://orcid.org/");
  });

  it("returns the sandbox ORCID URL in non-prod environments", () => {
    for (const environment of ["test", "demo", "local"]) {
      vi.mocked(getRuntimeConfig).mockReturnValue({
        environment,
      } as RuntimeConfig);

      expect(getContributorSchemaUri()).toBe("https://sandbox.orcid.org/");
    }
  });
});
