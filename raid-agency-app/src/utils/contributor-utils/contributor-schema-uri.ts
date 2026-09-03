import { getRuntimeConfig } from "@/config";

/**
 * Returns the environment-appropriate ORCID schemaUri for contributors.
 *
 * Production uses the live ORCID registry; every other environment uses the
 * ORCID sandbox. The backend enforces a sandbox-only schemaUri outside prod,
 * so the agency UI must send the matching value.
 *
 * IMPORTANT: this calls getRuntimeConfig(), which throws if the runtime config
 * has not been loaded yet. Only call it lazily (at render, validation or
 * generation time), never at module-load time.
 */
export const getContributorSchemaUri = (): string =>
  getRuntimeConfig().environment === "prod"
    ? "https://orcid.org/"
    : "https://sandbox.orcid.org/";
