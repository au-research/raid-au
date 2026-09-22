import { describe, it, expect } from "vitest";
import { relatedObjectIdSchema } from "./related-object-validation-schema";

describe("relatedObjectIdSchema DOI validation", () => {
  it.each([
    "https://doi.org/10.1234/xyz",
    "https://dx.doi.org/10.1234/xyz",
    "http://dx.doi.org/10.1234/xyz",
  ])("accepts %s", (url) => {
    expect(relatedObjectIdSchema.safeParse(url).success).toBe(true);
  });

  it("rejects a malformed dx.doi.org URL with the same error as other malformed DOIs", () => {
    const dxResult = relatedObjectIdSchema.safeParse(
      "https://dx.doi.org/not-a-doi"
    );
    const doiResult = relatedObjectIdSchema.safeParse(
      "https://doi.org/not-a-doi"
    );

    expect(dxResult.success).toBe(false);
    expect(doiResult.success).toBe(false);
    if (!dxResult.success && !doiResult.success) {
      expect(dxResult.error.issues[0].message).toBe(
        doiResult.error.issues[0].message
      );
    }
  });

  it("still accepts a valid web.archive.org snapshot URL", () => {
    expect(
      relatedObjectIdSchema.safeParse(
        "https://web.archive.org/web/20220101000000/https://example.com"
      ).success
    ).toBe(true);
  });

  // RAID-800: a URL that doesn't structurally match any recognised scheme's
  // host/path is a generic related-object identifier and is accepted as-is,
  // with no schemaUri inferred — it's only rejected once it *does* look like
  // an attempt at a known scheme (e.g. doi.org) but doesn't match that
  // scheme's shape.
  it("accepts a generic URL that doesn't match any recognised scheme", () => {
    expect(
      relatedObjectIdSchema.safeParse("https://example.com/10.1234/xyz").success
    ).toBe(true);
  });

  it.each([
    "not-a-url",
    "https://doi.org/not-a-doi",
  ])("rejects %s", (url) => {
    expect(relatedObjectIdSchema.safeParse(url).success).toBe(false);
  });
});
