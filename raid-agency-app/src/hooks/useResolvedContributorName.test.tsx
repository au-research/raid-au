import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode } from "react";
import { useResolvedContributorName } from "./useResolvedContributorName";

vi.mock("@/containers/orcid-lookup/ORCID", () => ({
  fetchFromOrcidPublicApi: vi.fn(),
}));
vi.mock("@/services/isni", () => ({
  fetchIsniName: vi.fn(),
}));

import { fetchFromOrcidPublicApi } from "@/containers/orcid-lookup/ORCID";
import { fetchIsniName } from "@/services/isni";

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe("useResolvedContributorName", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns "Not available" immediately (not stuck on "Resolving…") when the id has no recognisable body for its scheme', () => {
    const { result } = renderHook(
      () => useResolvedContributorName("not-a-real-identifier", false),
      { wrapper: createWrapper() }
    );

    expect(result.current).toBe("Not available");
    expect(fetchFromOrcidPublicApi).not.toHaveBeenCalled();
  });

  it("resolves and returns the ORCID name", async () => {
    vi.mocked(fetchFromOrcidPublicApi).mockResolvedValue({
      orcid: "0009-0002-5128-5184",
      creditName: "Jane Doe",
      givenName: "",
      lastName: "",
    });

    const { result } = renderHook(
      () =>
        useResolvedContributorName(
          "https://sandbox.orcid.org/0009-0002-5128-5184",
          false
        ),
      { wrapper: createWrapper() }
    );

    expect(result.current).toBe("Resolving…");
    await waitFor(() => expect(result.current).toBe("Jane Doe"));
  });

  it("resolves and returns the ISNI name", async () => {
    vi.mocked(fetchIsniName).mockResolvedValue("Taylor Swift");

    const { result } = renderHook(
      () =>
        useResolvedContributorName("https://isni.org/0000000078519858", true),
      { wrapper: createWrapper() }
    );

    expect(result.current).toBe("Resolving…");
    await waitFor(() => expect(result.current).toBe("Taylor Swift"));
  });

  it('returns "Not available" when the lookup fails, rather than a stale or fabricated name', async () => {
    vi.mocked(fetchIsniName).mockRejectedValue(new Error("404"));

    const { result } = renderHook(
      () =>
        useResolvedContributorName("https://isni.org/0000000078519858", true),
      { wrapper: createWrapper() }
    );

    await waitFor(() => expect(result.current).toBe("Not available"));
  });

  it("does not fetch at all when enabled is false", () => {
    renderHook(
      () =>
        useResolvedContributorName(
          "https://isni.org/0000000078519858",
          true,
          false
        ),
      { wrapper: createWrapper() }
    );

    expect(fetchIsniName).not.toHaveBeenCalled();
  });
});
