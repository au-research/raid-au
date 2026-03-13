// getApiEndpoint.test.ts
import { describe, it, expect, vi, afterEach } from "vitest";
import { getApiEndpoint } from "@/utils/api-utils/api-utils";

describe("getApiEndpoint", () => {
  describe("localhost handling", () => {
    it("should return localhost endpoint when hostname is 'localhost'", () => {
      const endpoint = getApiEndpoint("localhost");
      expect(endpoint).toBe("http://localhost:8080");
    });
  });

  describe("hostname transformation", () => {
    it("should replace service name with 'api' for test environment", () => {
      const endpoint = getApiEndpoint("app.test.raid.org.au");
      expect(endpoint).toBe("https://api.test.raid.org.au");
    });

    it("should replace service name with 'api' for demo environment", () => {
      const endpoint = getApiEndpoint("app.demo.raid.org.au");
      expect(endpoint).toBe("https://api.demo.raid.org.au");
    });

    it("should replace service name with 'api' for staging environment", () => {
      const endpoint = getApiEndpoint("app.stage.raid.org.au");
      expect(endpoint).toBe("https://api.stage.raid.org.au");
    });

    it("should replace service name with 'api' for dev environment", () => {
      const endpoint = getApiEndpoint("app.dev.raid.org.au");
      expect(endpoint).toBe("https://api.dev.raid.org.au");
    });

    it("should replace service name with 'api' for prod environment", () => {
      const endpoint = getApiEndpoint("app.prod.raid.org.au");
      expect(endpoint).toBe("https://api.prod.raid.org.au");
    });

    it("should replace service name with 'api' for production (raid.org.au)", () => {
      const endpoint = getApiEndpoint("app.raid.org.au");
      expect(endpoint).toBe("https://api.raid.org.au");
    });
  });

  describe("edge cases", () => {
    it("should handle simple two-part domain", () => {
      const endpoint = getApiEndpoint("app.example.com");
      expect(endpoint).toBe("https://api.example.com");
    });

    it("should handle complex multi-part domain", () => {
      const endpoint = getApiEndpoint("service.sub.test.raid.org.au");
      expect(endpoint).toBe("https://api.sub.test.raid.org.au");
    });
  });

  describe("default parameter", () => {

    const originalLocation = window.location;

    afterEach(() => {
      // Reset the window.location.hostname to its original value after each test
      Object.defineProperty(window, 'location', {
        writable: true,
        value: originalLocation,
      });
    });
    it("should return the demo endpoint for demo hostnames", () => {
      vi.stubGlobal("window", {
        location: {
          hostname: "demo.hostname.com",
        },
      });

      const endpoint = getApiEndpoint();
      expect(endpoint).toBe("https://api.hostname.com");
    });
  });
});
