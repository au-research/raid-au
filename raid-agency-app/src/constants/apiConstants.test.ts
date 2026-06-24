import { describe, it, expect, vi, beforeEach } from "vitest";
import { RuntimeConfig } from "@/config/RuntimeConfig";

vi.mock("@/config", () => ({
  getRuntimeConfig: vi.fn(),
}));

import { getRuntimeConfig } from "@/config";
import { API_CONSTANTS } from "./apiConstants";

const mockConfig: Pick<RuntimeConfig, "apiBaseUrl" | "services" | "app"> = {
  apiBaseUrl: "https://api.test.raid.org.au",
  services: {
    orcid: "https://orcid.test.raid.org.au",
    invite: "https://invite.test.raid.org.au",
    staticProd: "https://static.prod.raid.org.au",
    staticBase: "https://static.{env}.raid.org.au",
  },
  app: {
    orcid: {
      placeholder: "Enter ORCID iD (e.g., 0000-0002-1825-0097)",
      helpText: "Enter a valid ORCID iD",
    },
  },
};

describe("API_CONSTANTS", () => {
  beforeEach(() => {
    vi.mocked(getRuntimeConfig).mockReturnValue(mockConfig as RuntimeConfig);
  });

  describe("SERVICE_POINT", () => {
    it("ALL returns the correct URL", () => {
      expect(API_CONSTANTS.SERVICE_POINT.ALL).toBe("https://api.test.raid.org.au/service-point/");
    });

    it("BY_ID returns the correct URL", () => {
      expect(API_CONSTANTS.SERVICE_POINT.BY_ID(42)).toBe("https://api.test.raid.org.au/service-point/42");
    });
  });

  describe("RAID", () => {
    it("ALL returns the correct URL", () => {
      expect(API_CONSTANTS.RAID.ALL).toBe("https://api.test.raid.org.au/raid/");
    });

    it("BY_HANDLE returns the correct URL", () => {
      expect(API_CONSTANTS.RAID.BY_HANDLE("10.25.1/abc")).toBe("https://api.test.raid.org.au/raid/10.25.1/abc");
    });

    it("HISTORY returns the correct URL", () => {
      expect(API_CONSTANTS.RAID.HISTORY("10.25.1/abc")).toBe("https://api.test.raid.org.au/raid/10.25.1/abc/history");
    });

    it("HISTORY_DETAIL returns the correct URL", () => {
      expect(API_CONSTANTS.RAID.HISTORY_DETAIL("10.25.1/abc", "3")).toBe(
        "https://api.test.raid.org.au/raid/10.25.1/abc/3"
      );
    });

    it("GET_ENV_FOR_HANDLE always points to staticProd", () => {
      expect(API_CONSTANTS.RAID.GET_ENV_FOR_HANDLE).toBe(
        "https://static.prod.raid.org.au/api/all-handles.json"
      );
    });

    it("RELATED_RAID_TITLE substitutes {env} with the given environment", () => {
      expect(API_CONSTANTS.RAID.RELATED_RAID_TITLE("10.25.1/abc", "test")).toBe(
        "https://static.test.raid.org.au/raids/10.25.1/abc.json"
      );
    });

    it("RELATED_RAID_TITLE works with any environment value", () => {
      expect(API_CONSTANTS.RAID.RELATED_RAID_TITLE("10.25.1/abc", "prod")).toBe(
        "https://static.prod.raid.org.au/raids/10.25.1/abc.json"
      );
      expect(API_CONSTANTS.RAID.RELATED_RAID_TITLE("10.25.1/abc", "demo")).toBe(
        "https://static.demo.raid.org.au/raids/10.25.1/abc.json"
      );
    });
  });

  describe("ORCID", () => {
    it("CONTRIBUTORS returns the correct URL", () => {
      expect(API_CONSTANTS.ORCID.CONTRIBUTORS).toBe("https://orcid.test.raid.org.au/contributors");
    });
  });

  describe("INVITE", () => {
    it("SEND returns the correct URL when invite is configured", () => {
      expect(API_CONSTANTS.INVITE.SEND).toBe("https://invite.test.raid.org.au/invite");
    });

    it("FETCH returns the correct URL when invite is configured", () => {
      expect(API_CONSTANTS.INVITE.FETCH).toBe("https://invite.test.raid.org.au/invite/fetch");
    });

    it("ACCEPT returns the correct URL when invite is configured", () => {
      expect(API_CONSTANTS.INVITE.ACCEPT).toBe("https://invite.test.raid.org.au/invite/accept");
    });

    it("REJECT returns the correct URL when invite is configured", () => {
      expect(API_CONSTANTS.INVITE.REJECT).toBe("https://invite.test.raid.org.au/invite/reject");
    });

    it("returns undefined when invite is not configured", () => {
      vi.mocked(getRuntimeConfig).mockReturnValue({
        ...mockConfig,
        services: { ...mockConfig.services, invite: undefined },
      } as RuntimeConfig);

      expect(API_CONSTANTS.INVITE.SEND).toBeUndefined();
      expect(API_CONSTANTS.INVITE.FETCH).toBeUndefined();
      expect(API_CONSTANTS.INVITE.ACCEPT).toBeUndefined();
      expect(API_CONSTANTS.INVITE.REJECT).toBeUndefined();
    });
  });

  describe("DOI", () => {
    it("REGISTRATION returns the correct URL", () => {
      expect(API_CONSTANTS.DOI.REGISTRATION("10.25.1/abc")).toBe("https://doi.org/doiRA/10.25.1/abc");
    });

    it("CROSS_REF returns the correct URL", () => {
      expect(API_CONSTANTS.DOI.CROSS_REF("10.25.1/abc")).toBe("https://api.crossref.org/works/10.25.1/abc");
    });

    it("DATA_CITE returns the correct URL", () => {
      expect(API_CONSTANTS.DOI.DATA_CITE("10.25.1/abc")).toBe("https://api.datacite.org/dois/10.25.1/abc");
    });

    it("BY_HANDLE_URL returns the correct URL", () => {
      expect(API_CONSTANTS.DOI.BY_HANDLE_URL("10.25.1/abc")).toBe(
        "https://doi.org/api/handles/10.25.1/abc?type=url"
      );
    });
  });
});
