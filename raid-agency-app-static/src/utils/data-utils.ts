import { getSiteConfig } from "@/config";
import { resolveAppUrl } from "@/utils/app-url";
import { buildRaidUrl } from "@/utils/raid-url";

export function getRaidAppUrl(): string {
  const { appUrl, raidEnv } = getSiteConfig();
  return resolveAppUrl({ appUrl, raidEnv });
}

export function getRaidUrl(id: string): string {
  return buildRaidUrl(getSiteConfig().raidResolverUrl, id);
}
