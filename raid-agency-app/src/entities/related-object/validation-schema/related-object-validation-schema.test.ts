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

  it.each([
    "not-a-url",
    "https://example.com/10.1234/xyz",
    "https://doi.org/not-a-doi",
  ])("rejects %s", (url) => {
    expect(relatedObjectIdSchema.safeParse(url).success).toBe(false);
  });
});
