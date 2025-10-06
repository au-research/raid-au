export function getRaidAppUrl(): string {
  const localWebAppPort = 7080;
  const raidEnv = import.meta.env.RAID_ENV;
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

  if (environment === "test" || "dev") {
    console.log("environment", environment);
  }

  return `https://app.${environment}.raid.org.au`;
}
