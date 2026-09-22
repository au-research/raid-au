type SchemeMatcher = {
  schemaUri: string;
  test: (url: URL) => boolean;
};

// RAID-801: ARK recognition is commented out for now — RAID-793 (the ARK
// backend validator) hasn't merged, so an ARK row would classify correctly
// here but the API would still reject it. Re-enable both this helper and
// the ARK matcher below once RAID-793 lands.
// function firstPathSegment(url: URL): string {
//   return url.pathname.replace(/^\/+/, "").split("/")[0] ?? "";
// }

const schemeMatchers: SchemeMatcher[] = [
  {
    schemaUri: "https://doi.org/",
    // dx.doi.org is a valid DOI proxy host alongside doi.org (RAID-804/RAID-798).
    test: (url) => url.hostname === "doi.org" || url.hostname === "dx.doi.org",
  },
  {
    schemaUri: "https://web.archive.org/",
    test: (url) => url.hostname === "web.archive.org",
  },
  {
    schemaUri: "https://hdl.handle.net/",
    test: (url) => url.hostname === "hdl.handle.net",
  },
  {
    schemaUri: "https://scicrunch.org/resolver/",
    test: (url) =>
      url.hostname === "scicrunch.org" && url.pathname.startsWith("/resolver/"),
  },
  // RAID-801: ARK isn't tied to one fixed host (RAID-793) — every publisher can
  // run its own resolver — so it would be matched structurally: "ark:" must be
  // the first path segment after the host, not just present anywhere in the
  // URL. Commented out until RAID-793's backend validator merges.
  // {
  //   schemaUri: "https://arks.org/",
  //   test: (url) => /^ark:/i.test(firstPathSegment(url)),
  // },
];

/**
 * Infers the relatedObject schemaUri from a pasted/typed URL, or null if it
 * doesn't match any recognised scheme.
 */
export function inferRelatedObjectSchemaUri(value: string): string | null {
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return null;
  }

  return schemeMatchers.find((matcher) => matcher.test(url))?.schemaUri ?? null;
}
