import { getEnv } from "@/utils/api-utils/api-utils";
import { API_CONSTANTS } from "@/constants/apiConstants";

export async function fetchOrcidContributors({ handle }: { handle: string }) {
  let environment = getEnv();
  environment = environment === "dev" ? "test" : environment;
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
    // Handle any other errors
    console.error("Error fetching contributors:", error);
    throw error;
  }
}
