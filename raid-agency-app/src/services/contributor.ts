import { API_CONSTANTS } from "@/constants/apiConstants";
import { getRuntimeConfig } from "@/config";

export async function fetchOrcidContributors({ handle }: { handle: string }) {
  // The orcid service doesn't exist in dev, so dev maps to test.
  const env = getRuntimeConfig().environment;
  const environment = env === "dev" ? "test" : env;
  const subDomain = "orcid";
  try {
    const url = API_CONSTANTS.ORCID.CONTRIBUTORS(subDomain, environment);
    const response = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ handle }),
    });

    return response.json();
  } catch (error) {
    console.error("Error fetching contributors:", error);
    throw error;
  }
}
