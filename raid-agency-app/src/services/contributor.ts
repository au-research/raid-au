import { API_CONSTANTS } from "@/constants/apiConstants";

export async function fetchOrcidContributors({ handle }: { handle: string }) {
  try {
    const response = await fetch(API_CONSTANTS.ORCID.CONTRIBUTORS, {
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
