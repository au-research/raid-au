import { getSiteConfig } from "@/config";

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

  if (environment === "test" || environment === "dev") {
    console.log("environment", environment);
  }

  return `https://app.${environment}.raid.org.au`;
}

export const getRAIDUrl: string = getSiteConfig().raidUrl ?? 'https://raid.org/';
