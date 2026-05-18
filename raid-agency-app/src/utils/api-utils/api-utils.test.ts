import { describe, it, expect } from "vitest";
import { getRootDomain } from "@/utils/api-utils/api-utils";

describe("getRootDomain", () => {
  it("extracts root domain from a plain domain URL", () => {
    expect(getRootDomain("http://www.raid.org.au")).toBe("raid.org.au");
  });

  it("extracts root domain from a subdomain URL", () => {
    expect(getRootDomain("https://app.test.raid.org.au")).toBe("raid.org.au");
  });

  it("extracts root domain regardless of subdomain depth", () => {
    expect(getRootDomain("https://static.prod.raid.org.au")).toBe("raid.org.au");
  });

  it("extracts root domain from a .com URL", () => {
    expect(getRootDomain("https://app.example.com")).toBe("example.com");
  });

  it("returns null for an invalid URL", () => {
    expect(getRootDomain("not-a-url")).toBeNull();
  });

  it("returns null for localhost (not in the public suffix list)", () => {
    expect(getRootDomain("http://localhost:8080")).toBeNull();
  });
});
