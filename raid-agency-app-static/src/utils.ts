export function kebabToTitle(str: string | undefined): string {
  if (!str) return "";

  const words = str.split("-");
  const firstWord = words[0].charAt(0).toUpperCase() + words[0].slice(1);
  const restWords = words.slice(1);

  return [firstWord, ...restWords].join(" ");
}

const HTML_ENTITIES: Record<string, string> = {
  amp: "&",
  lt: "<",
  gt: ">",
  quot: '"',
  apos: "'",
  "#39": "'",
  "#039": "'",
};

// Collapses any depth of HTML-entity over-encoding (e.g. "&amp;amp;", or the
// mis-cased "&Amp;" that DOI registration agencies occasionally emit) down to
// the real character, so text that already arrived HTML-safe from an
// upstream source doesn't get double-decoded (or left half-decoded) when
// injected as raw HTML.
export function decodeHtmlEntities(text: string): string {
  let decoded = text;
  let previous: string;
  do {
    previous = decoded;
    decoded = decoded.replace(
      /&(amp|lt|gt|quot|apos|#39|#039);/gi,
      (match, entity) => HTML_ENTITIES[entity.toLowerCase()] ?? match
    );
  } while (decoded !== previous);
  return decoded;
}

export function getLastTwoUrlSegments(url: string): string | null {
  const parts = url.split("/").filter((part) => part.length > 0);
  if (parts.length < 2) {
    return null;
  }
  return parts.slice(-2).join("/");
}
