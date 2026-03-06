// RAID-536: Thin HTTP client for API-based test data seeding
// Uses the RAiD API at localhost:8080 to create and retrieve RAiDs directly,
// which is faster than driving the UI for test setup.

const API_BASE = process.env.API_BASE_URL ?? "http://localhost:8080";

interface TokenProvider {
  (): Promise<string>;
}

export class RaidApiClient {
  constructor(private readonly getToken: TokenProvider) {}

  /**
   * Create a RAiD via the API. Returns the created RaidDto including the identifier.
   */
  async createRaid(raidData: Record<string, unknown>): Promise<Record<string, unknown>> {
    const token = await this.getToken();
    const response = await fetch(`${API_BASE}/raid/`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(raidData),
    });

    if (!response.ok) {
      const body = await response.text();
      throw new Error(
        `Failed to create RAiD: ${response.status} ${response.statusText}\n${body}`
      );
    }

    return response.json() as Promise<Record<string, unknown>>;
  }

  /**
   * Retrieve a RAiD by handle ({prefix}/{suffix}).
   */
  async getRaid(
    prefix: string,
    suffix: string
  ): Promise<Record<string, unknown>> {
    const token = await this.getToken();
    const response = await fetch(`${API_BASE}/raid/${prefix}/${suffix}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      const body = await response.text();
      throw new Error(
        `Failed to get RAiD ${prefix}/${suffix}: ${response.status} ${response.statusText}\n${body}`
      );
    }

    return response.json() as Promise<Record<string, unknown>>;
  }
}
