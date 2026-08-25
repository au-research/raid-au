import { describe, expect, it } from "vitest";
import { decodeHtmlEntities } from "./utils";

describe("decodeHtmlEntities", () => {
  it("leaves plain text with a raw ampersand untouched", () => {
    expect(decodeHtmlEntities("Stanley, F. J., & Landau, L. I.")).toBe(
      "Stanley, F. J., & Landau, L. I."
    );
  });

  it("decodes a single-encoded ampersand", () => {
    expect(decodeHtmlEntities("Stanley, F. J., &amp; Landau, L. I.")).toBe(
      "Stanley, F. J., & Landau, L. I."
    );
  });

  it("fully collapses a double-encoded ampersand", () => {
    expect(decodeHtmlEntities("Stanley, F. J., &amp;amp; Landau, L. I.")).toBe(
      "Stanley, F. J., & Landau, L. I."
    );
  });

  it("decodes a mis-cased entity as seen from some DOI registration agencies", () => {
    expect(decodeHtmlEntities("ACS Applied Materials &Amp; Interfaces")).toBe(
      "ACS Applied Materials & Interfaces"
    );
  });

  it("leaves real HTML tags in citation text untouched", () => {
    expect(decodeHtmlEntities("Wei, M. &amp; Zhang, J. <i>Some Title</i>")).toBe(
      "Wei, M. & Zhang, J. <i>Some Title</i>"
    );
  });

  it("decodes other common entities", () => {
    expect(decodeHtmlEntities("A &lt;title&gt; with &quot;quotes&quot; &amp; an apostrophe&#39;s mark")).toBe(
      "A <title> with \"quotes\" & an apostrophe's mark"
    );
  });
});
