import { describe, it, expect } from "vitest";
import { inferRelatedObjectSchemaUri } from "./related-object-schema-uri";

describe("inferRelatedObjectSchemaUri", () => {
  it("recognises a DOI URL", () => {
    expect(inferRelatedObjectSchemaUri("https://doi.org/10.25955/abc-123")).toBe(
      "https://doi.org/"
    );
  });

  it("recognises a Web Archive URL", () => {
    expect(
      inferRelatedObjectSchemaUri(
        "https://web.archive.org/web/20220101000000/https://example.com"
      )
    ).toBe("https://web.archive.org/");
  });

  it("recognises a Handle URL", () => {
    expect(
      inferRelatedObjectSchemaUri("https://hdl.handle.net/20.500.12345/abc123")
    ).toBe("https://hdl.handle.net/");
  });

  it("recognises an RRID URL", () => {
    expect(
      inferRelatedObjectSchemaUri("https://scicrunch.org/resolver/RRID:AB_2298772")
    ).toBe("https://scicrunch.org/resolver/");
  });

  it("recognises an ARK URL regardless of host", () => {
    expect(
      inferRelatedObjectSchemaUri(
        "https://example-repository.edu/ark:/13030/kt6f59n8z3"
      )
    ).toBe("https://arks.org/");
  });

  it("recognises an ARK URL without the optional slash after ark:", () => {
    expect(
      inferRelatedObjectSchemaUri("https://example-repository.edu/ark:13030/kt6f59n8z3")
    ).toBe("https://arks.org/");
  });

  it("does not match an ARK-shaped path that isn't the first segment", () => {
    expect(
      inferRelatedObjectSchemaUri("https://example.edu/not-ark/ark:/13030/kt6f59n8z3")
    ).toBeNull();
  });

  it("returns null for a bare ARK with no host", () => {
    // Not a valid absolute URL — RAiD stores every identifier fully qualified.
    expect(inferRelatedObjectSchemaUri("ark:/13030/kt6f59n8z3")).toBeNull();
  });

  it("returns null for an unrecognised URL", () => {
    expect(inferRelatedObjectSchemaUri("https://example.com/some-path")).toBeNull();
  });

  it("returns null for a non-URL string", () => {
    expect(inferRelatedObjectSchemaUri("not a url")).toBeNull();
  });
});
