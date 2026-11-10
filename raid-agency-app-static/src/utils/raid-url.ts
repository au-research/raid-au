export const DEFAULT_RAID_URL = "https://raid.org/";

/**
 * Builds the global resolver URL for a RAiD handle, e.g.
 * https://raid.org/10.83338/831b5565.
 *
 * The base URL may or may not end with a slash, and may be blank, so a
 * misconfigured `raidResolverUrl` cannot produce a malformed link.
 */
export function buildRaidUrl(baseUrl: string | undefined, id: string): string {
  const base = baseUrl?.trim() || DEFAULT_RAID_URL;
  const handle = id.replace(/^\/+/, "");
  return `${base.replace(/\/+$/, "")}/${handle}`;
}
