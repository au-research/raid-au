import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ClientCredentialsPanel } from "./ClientCredentialsPanel";
import { ClientCredentialApiError } from "@/services/client-credentials";
import type { ClientCredential, ClientCredentialSecret } from "@/services/client-credentials";

vi.mock("@/contexts/keycloak-context", () => ({
  useKeycloak: () => ({ token: "mock-token" }),
}));

vi.mock("@/components/snackbar", () => ({
  useSnackbar: () => ({ openSnackbar: vi.fn() }),
}));

const mockFetchClientCredentials = vi.fn();
const mockCreateClientCredential = vi.fn();
const mockRotateClientCredential = vi.fn();
const mockRevokeClientCredential = vi.fn();
const mockGetClientCredentialSecret = vi.fn();

vi.mock("@/services/client-credentials", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/client-credentials")>();
  return {
    ...actual,
    fetchClientCredentials: (...args: unknown[]) => mockFetchClientCredentials(...args),
    createClientCredential: (...args: unknown[]) => mockCreateClientCredential(...args),
    rotateClientCredential: (...args: unknown[]) => mockRotateClientCredential(...args),
    revokeClientCredential: (...args: unknown[]) => mockRevokeClientCredential(...args),
    getClientCredentialSecret: (...args: unknown[]) => mockGetClientCredentialSecret(...args),
  };
});

const makeCredential = (overrides: Partial<ClientCredential> = {}): ClientCredential => ({
  clientId: "raid-cred-abc123",
  label: "rspace-integration",
  createdAt: "2026-06-02T00:00:00Z",
  lastRotatedAt: null,
  enabled: true,
  ...overrides,
});

const makeSecret = (overrides: Partial<ClientCredentialSecret> = {}): ClientCredentialSecret => ({
  clientId: "raid-cred-abc123",
  label: "rspace-integration",
  secret: "supersecretvalue",
  createdAt: "2026-06-02T00:00:00Z",
  lastRotatedAt: null,
  ...overrides,
});

const renderPanel = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ClientCredentialsPanel groupId="group-1" />
    </QueryClientProvider>
  );
};

describe("ClientCredentialsPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
      writable: true,
    });
  });

  it("shows the empty state and never renders a 'last used' column", async () => {
    mockFetchClientCredentials.mockResolvedValue([]);
    renderPanel();

    expect(await screen.findByText(/No client credentials yet/i)).toBeInTheDocument();
    expect(screen.queryByText(/last used/i)).not.toBeInTheDocument();
  });

  it("lists existing credentials with Created and Last rotated, not Last used", async () => {
    mockFetchClientCredentials.mockResolvedValue([makeCredential()]);
    renderPanel();

    expect(await screen.findByText("rspace-integration")).toBeInTheDocument();
    expect(screen.getByText("Created")).toBeInTheDocument();
    expect(screen.getByText("Last rotated")).toBeInTheDocument();
    expect(screen.queryByText(/last used/i)).not.toBeInTheDocument();
  });

  it("disables create and shows a limit message when 10 active credentials exist", async () => {
    mockFetchClientCredentials.mockResolvedValue(
      Array.from({ length: 10 }, (_, i) => makeCredential({ clientId: `cred-${i}`, label: `cred-${i}` }))
    );
    renderPanel();

    fireEvent.click(await screen.findByText("Create client credential"));
    expect(await screen.findByText(/Credential limit reached \(10\/10\)/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create" })).toBeDisabled();
  });

  it("creates a credential and shows the masked secret with working reveal and copy", async () => {
    mockFetchClientCredentials.mockResolvedValue([]);
    mockCreateClientCredential.mockResolvedValue(makeSecret());
    renderPanel();

    fireEvent.click(await screen.findByText("Create client credential"));
    fireEvent.change(screen.getByLabelText("Label"), { target: { value: "rspace-integration" } });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    await waitFor(() => expect(mockCreateClientCredential).toHaveBeenCalledWith(
      { groupId: "group-1", label: "rspace-integration", token: "mock-token" },
      expect.anything()
    ));

    const secretField = await screen.findByLabelText("Secret");
    expect(secretField).toHaveValue("•".repeat(24));

    fireEvent.click(screen.getByRole("button", { name: /Reveal secret/i }));
    expect(secretField).toHaveValue("supersecretvalue");

    fireEvent.click(screen.getByRole("button", { name: /Copy secret/i }));
    await waitFor(() => expect(navigator.clipboard.writeText).toHaveBeenCalledWith("supersecretvalue"));
  });

  it("shows the newly created credential in the list as soon as it's created, before Done is clicked", async () => {
    mockFetchClientCredentials.mockResolvedValue([]);
    mockCreateClientCredential.mockResolvedValue(makeSecret());
    renderPanel();

    expect(await screen.findByText(/No client credentials yet/i)).toBeInTheDocument();

    fireEvent.click(screen.getByText("Create client credential"));
    fireEvent.change(screen.getByLabelText("Label"), { target: { value: "rspace-integration" } });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    // The row shows up immediately, while the secret panel is still open -
    // not only after "Done" is clicked, and not only after a background refetch.
    expect(await screen.findByText("rspace-integration")).toBeInTheDocument();
    expect(screen.queryByText(/No client credentials yet/i)).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Done" }));
    expect(screen.getByText("rspace-integration")).toBeInTheDocument();
  });

  it("shows a specific message when create is rejected with a 409", async () => {
    mockFetchClientCredentials.mockResolvedValue([]);
    mockCreateClientCredential.mockRejectedValue(
      new ClientCredentialApiError("Credential limit reached (10/10)", 409)
    );
    renderPanel();

    fireEvent.click(await screen.findByText("Create client credential"));
    fireEvent.change(screen.getByLabelText("Label"), { target: { value: "one-too-many" } });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByText(/Credential limit reached \(10\/10\)/i)).toBeInTheDocument();
  });

  it("rotates a credential and reveals the new secret", async () => {
    mockFetchClientCredentials.mockResolvedValue([makeCredential()]);
    mockRotateClientCredential.mockResolvedValue(makeSecret({ secret: "rotatedsecretvalue" }));
    renderPanel();

    const row = (await screen.findByText("rspace-integration")).closest("tr") as HTMLElement;
    fireEvent.click(within(row).getByLabelText("rotate secret"));

    await waitFor(() => expect(mockRotateClientCredential).toHaveBeenCalledWith(
      { clientId: "raid-cred-abc123", token: "mock-token" },
      expect.anything()
    ));
    expect(await screen.findByText(/Secret rotated/i)).toBeInTheDocument();
  });

  it("revokes a credential after confirmation", async () => {
    mockFetchClientCredentials.mockResolvedValue([makeCredential()]);
    mockRevokeClientCredential.mockResolvedValue(undefined);
    renderPanel();

    const row = (await screen.findByText("rspace-integration")).closest("tr") as HTMLElement;
    fireEvent.click(within(row).getByLabelText("revoke"));

    fireEvent.click(await screen.findByRole("button", { name: "Revoke" }));

    await waitFor(() => expect(mockRevokeClientCredential).toHaveBeenCalledWith(
      { clientId: "raid-cred-abc123", token: "mock-token" },
      expect.anything()
    ));
  });
});
