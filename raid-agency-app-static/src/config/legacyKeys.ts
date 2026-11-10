/**
 * Config keys that were renamed and are no longer read. Maps each old key to
 * its replacement so the loader can tell agencies what to change.
 */
export const LEGACY_KEYS: Record<string, string> = {
  raidUrl: "raidResolverUrl",
};

/** Returns the old keys present in the given config, with their replacements. */
export function findLegacyKeys(
  config: Record<string, unknown>
): Array<{ oldKey: string; newKey: string }> {
  return Object.entries(LEGACY_KEYS)
    .filter(([oldKey]) => oldKey in config)
    .map(([oldKey, newKey]) => ({ oldKey, newKey }));
}
