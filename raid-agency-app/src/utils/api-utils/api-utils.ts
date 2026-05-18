// @ts-expect-error: package exports map prevents TypeScript from resolving the bundled .mjs declarations
import psl from 'psl';

// Extracts the registrable domain from a URL (e.g. "https://app.test.raid.org.au" → "raid.org.au")
export const getRootDomain = (url: string) => {
  try {
    const urlObj = new URL(url);
    const parsed = psl.parse(urlObj.hostname);
    return parsed.domain;
  } catch (error) {
    console.error('Invalid URL:', error);
    return null;
  }
};
