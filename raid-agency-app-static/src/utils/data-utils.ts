import { getSiteConfig } from "@/config";
import { buildRaidUrl } from "@/utils/raid-url";

export function getRaidAppUrl(): string {
  const localWebAppPort = 7080;
  const raidEnv = getSiteConfig().raidEnv;
  const environment =
    raidEnv === "test"
      ? "test"
      : raidEnv === "demo"
      ? "demo"
      : raidEnv === "prod"
      ? "prod"
      : raidEnv === "stage"
      ? "stage"
      : "dev";

  if (environment === "dev") {
    return `http://localhost:${localWebAppPort}`;
  }

  if (environment === "test") {
    console.log("environment", environment);
  }

  return `https://app.${environment}.raid.org.au`;
}

export function getRaidUrl(id: string): string {
  return buildRaidUrl(getSiteConfig().raidUrl, id);
}
