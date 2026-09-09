import { describe, it, expect } from "vitest";
import { orcidPattern } from "./contributor-validation-schema";

// RAID-861 widened this pattern from ORCID-only to also accept ISNI. Tested
// directly against the pattern rather than the full contributor schema,
// since the latter also requires satisfying position/role/contact fields
// unrelated to this change.
describe("contributor identifier pattern (ORCID or ISNI)", () => {
  const regex = new RegExp(orcidPattern);

  it("accepts a full orcid.org URL", () => {
    expect(regex.test("https://orcid.org/0000-0002-1825-0097")).toBe(true);
  });

  it("accepts a full sandbox.orcid.org URL", () => {
    expect(regex.test("https://sandbox.orcid.org/0000-0002-1825-0097")).toBe(true);
  });

  it("accepts a valid ISNI URL", () => {
    expect(regex.test("https://isni.org/0000000121032683")).toBe(true);
  });

  it("rejects a bare ORCID with no host (existing behaviour, unaffected)", () => {
    expect(regex.test("0000-0002-1825-0097")).toBe(false);
  });

  it("rejects an ISNI-shaped value on the wrong host", () => {
    expect(regex.test("https://example.com/0000000121032683")).toBe(false);
  });

  it("rejects an ISNI URL with the wrong number of characters", () => {
    expect(regex.test("https://isni.org/000000012103268")).toBe(false);
  });

  it("rejects an unrelated string", () => {
    expect(regex.test("not-an-identifier")).toBe(false);
  });
});
