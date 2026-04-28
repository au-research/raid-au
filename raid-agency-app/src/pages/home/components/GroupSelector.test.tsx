import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { GroupSelector } from "./GroupSelector";

vi.mock("@/contexts/keycloak-context", () => ({
  useKeycloak: () => ({ token: "mock-token", isInitialized: true }),
}));

const mockFetchAllKeycloakGroups = vi.fn();
const mockFetchKeycloakLocalization = vi.fn();
const mockJoinKeycloakGroup = vi.fn();
const mockSetKeycloakUserAttribute = vi.fn();

vi.mock("@/services/keycloak-groups", () => ({
  fetchAllKeycloakGroups: (...args: unknown[]) => mockFetchAllKeycloakGroups(...args),
  fetchKeycloakLocalization: (...args: unknown[]) => mockFetchKeycloakLocalization(...args),
  joinKeycloakGroup: (...args: unknown[]) => mockJoinKeycloakGroup(...args),
  setKeycloakUserAttribute: (...args: unknown[]) => mockSetKeycloakUserAttribute(...args),
}));

const renderComponent = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <GroupSelector />
    </QueryClientProvider>
  );
};

describe("GroupSelector", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetchAllKeycloakGroups.mockResolvedValue({
      groups: [
        { id: "g1", name: "RAiD sandbox", attributes: { groupId: ["g1"] } },
      ],
    });
  });

  it("displays the localized message when fetch succeeds", async () => {
    mockFetchKeycloakLocalization.mockResolvedValue({
      key: "groupSelectorAccessMessage",
      value: "Custom access message from Keycloak",
      locale: "en",
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Custom access message from Keycloak/i)).toBeInTheDocument();
    });
  });

  it("displays the default message when localization fetch fails", async () => {
    mockFetchKeycloakLocalization.mockRejectedValue(new Error("Not found"));

    renderComponent();

    await waitFor(() => {
      expect(
        screen.getByText(/To use RAiD you must belong to a 'Service Point'/i)
      ).toBeInTheDocument();
    });
  });

  it("displays the default message when localization is still loading", async () => {
    // Return a promise that never resolves during this assertion window
    mockFetchKeycloakLocalization.mockReturnValue(new Promise(() => {}));

    renderComponent();

    await waitFor(() => {
      expect(
        screen.getByText(/To use RAiD you must belong to a 'Service Point'/i)
      ).toBeInTheDocument();
    });
  });

  it("calls fetchKeycloakLocalization with the expected key", async () => {
    mockFetchKeycloakLocalization.mockResolvedValue({
      key: "groupSelectorAccessMessage",
      value: "Message",
      locale: "en",
    });

    renderComponent();

    await waitFor(() => {
      expect(mockFetchKeycloakLocalization).toHaveBeenCalledWith(
        expect.objectContaining({
          token: "mock-token",
          key: "groupSelectorAccessMessage",
        })
      );
    });
  });

  it("sanitizes HTML in localization values", async () => {
    mockFetchKeycloakLocalization.mockResolvedValue({
      key: "groupSelectorAccessMessage",
      value: "Safe text<script>alert('xss')</script><br/>More text",
      locale: "en",
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Safe text/i)).toBeInTheDocument();
    });

    // Script tag should be stripped by DOMPurify
    expect(document.querySelector("script")).toBeNull();
  });

  it("renders the group list from fetchAllKeycloakGroups", async () => {
    mockFetchKeycloakLocalization.mockResolvedValue({
      key: "groupSelectorAccessMessage",
      value: "Message",
      locale: "en",
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText(/Institution/i)).toBeInTheDocument();
    });
  });
});
