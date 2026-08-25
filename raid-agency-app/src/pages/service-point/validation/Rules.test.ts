// RAID-480: unit tests for the Service Point Owner (identifierOwner/ROR)
// validation. Before this fix, a blank or whitespace ROR could be submitted
// and stored, which silently broke minting for that service point in Demo
// and Prod - see e2e/tests/operator/service-point-ror-validation.spec.ts for
// the corresponding UI-level coverage of the Create Service Point form.

import { describe, it, expect } from "vitest";
import { createServicePointRequestValidationSchema } from "./Rules";

const validServicePoint = {
  name: "Test Service Point",
  identifierOwner: "https://ror.org/038sjwq14",
  adminEmail: "admin@example.com",
  techEmail: "tech@example.com",
  enabled: true,
  appWritesEnabled: false,
};

const parse = (identifierOwner: string) =>
  createServicePointRequestValidationSchema.safeParse({
    servicePointCreateRequest: { ...validServicePoint, identifierOwner },
  });

describe("createServicePointRequestValidationSchema: identifierOwner (ROR)", () => {
  it("accepts a well-formed ROR URL", () => {
    const result = parse("https://ror.org/038sjwq14");

    expect(result.success).toBe(true);
  });

  it("rejects a blank identifierOwner", () => {
    const result = parse("");

    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((issue) => issue.message);
      expect(messages).toContain("Identifier owner is required");
    }
  });

  it("rejects a whitespace/tab-only identifierOwner (RAID-480: previously stored as-is)", () => {
    const result = parse("\t\t");

    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((issue) => issue.message);
      expect(messages).toContain(
        "Must be a valid ROR URL (e.g. https://ror.org/038sjwq14)"
      );
    }
  });

  it("rejects a non-ROR URL", () => {
    const result = parse("https://example.com/not-a-ror");

    expect(result.success).toBe(false);
    if (!result.success) {
      const messages = result.error.issues.map((issue) => issue.message);
      expect(messages).toContain(
        "Must be a valid ROR URL (e.g. https://ror.org/038sjwq14)"
      );
    }
  });

  it("rejects a ROR URL with an invalid ID length", () => {
    const result = parse("https://ror.org/abc");

    expect(result.success).toBe(false);
  });
});
