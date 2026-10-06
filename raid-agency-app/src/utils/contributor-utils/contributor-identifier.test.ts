import { describe, it, expect } from "vitest";
import { detectContributorIdentifierType, looksLikeIsniAttempt } from "./contributor-identifier";

describe("detectContributorIdentifierType", () => {
  it("recognises bare ORCID digits", () => {
    expect(detectContributorIdentifierType("0000-0002-1825-0097")).toBe("orcid");
  });

  it("recognises a full orcid.org URL", () => {
    expect(detectContributorIdentifierType("https://orcid.org/0000-0002-1825-0097")).toBe("orcid");
  });

  it("recognises a sandbox.orcid.org URL", () => {
    expect(detectContributorIdentifierType("https://sandbox.orcid.org/0000-0002-1825-0097")).toBe("orcid");
  });

  it("recognises an ISNI URL", () => {
    expect(detectContributorIdentifierType("https://isni.org/0000000121032683")).toBe("isni");
  });

  it("returns unrecognised for an invalid string", () => {
    expect(detectContributorIdentifierType("not-an-identifier")).toBe("unrecognised");
  });

  it("returns unrecognised for an empty value", () => {
    expect(detectContributorIdentifierType("")).toBe("unrecognised");
    expect(detectContributorIdentifierType("   ")).toBe("unrecognised");
  });

  it("returns unrecognised for an ISNI URL with the wrong number of characters", () => {
    expect(detectContributorIdentifierType("https://isni.org/000000012103268")).toBe("unrecognised");
  });
});

describe("looksLikeIsniAttempt", () => {
  it("is false for a fully valid ISNI (it's not a bad attempt)", () => {
    expect(looksLikeIsniAttempt("https://isni.org/0000000121032683")).toBe(false);
  });

  it("is true for an isni.org URL with the wrong digit count", () => {
    expect(looksLikeIsniAttempt("https://isni.org/000000012103268")).toBe(true);
  });

  it("is true for an isni.org URL with an extra path segment", () => {
    expect(looksLikeIsniAttempt("https://isni.org/isni/000000012281955X")).toBe(true);
  });

  it("is false for an ORCID URL or unrelated text", () => {
    expect(looksLikeIsniAttempt("https://orcid.org/0000-0002-1825-0097")).toBe(false);
    expect(looksLikeIsniAttempt("not-an-identifier")).toBe(false);
    expect(looksLikeIsniAttempt("")).toBe(false);
  });
});
