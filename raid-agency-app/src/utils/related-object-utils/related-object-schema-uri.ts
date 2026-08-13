type SchemeMatcher = {
  schemaUri: string;
  test: (url: URL) => boolean;
};

function firstPathSegment(url: URL): string {
  return url.pathname.replace(/^\/+/, "").split("/")[0] ?? "";
}

// ARK isn't tied to one fixed host (RAID-793) — every publisher can run its own
// resolver — so it's matched structurally: "ark:" must be the first path segment
// after the host, not just present anywhere in the URL.
const schemeMatchers: SchemeMatcher[] = [
  { schemaUri: "https://doi.org/", test: (url) => url.hostname === "doi.org" },
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
  {
    schemaUri: "https://arks.org/",
    test: (url) => /^ark:/i.test(firstPathSegment(url)),
  },
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
