import { getSiteConfig } from "@/config";
import { resolveAppUrl } from "@/utils/app-url";

export function getRaidAppUrl(): string {
  const { appUrl, raidEnv } = getSiteConfig();
  return resolveAppUrl({ appUrl, raidEnv });
}

export const getRAIDUrl: string = getSiteConfig().raidUrl ?? 'https://raid.org/';
