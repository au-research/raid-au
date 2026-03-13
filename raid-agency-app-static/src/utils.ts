export function kebabToTitle(str: string | undefined): string {
  if (!str) return "";

  const words = str.split("-");
  const firstWord = words[0].charAt(0).toUpperCase() + words[0].slice(1);
  const restWords = words.slice(1);

  return [firstWord, ...restWords].join(" ");
}

export function getLastTwoUrlSegments(url: string): string | null {
  const parts = url.split("/").filter((part) => part.length > 0);
  if (parts.length < 2) {
    return null;
  }
  return parts.slice(-2).join("/");
}
