import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ServicePointUpdateForm } from "./ServicePointUpdateForm";
import type { ServicePoint } from "@/generated/raid";

vi.mock("@/contexts/keycloak-context", () => ({
  useKeycloak: () => ({ token: "mock-token" }),
}));

vi.mock("@/components/snackbar", () => ({
  useSnackbar: () => ({ openSnackbar: vi.fn() }),
}));

vi.mock("@/components/error-dialog", () => ({
  useErrorDialog: () => ({ openErrorDialog: vi.fn() }),
}));

vi.mock("@/containers/organisation-lookup/RORCustomComponent", () => ({
  default: () => <input data-testid="ror-lookup" />,
}));

const mockUpdateServicePoint = vi.fn();
vi.mock("@/services/service-points", () => ({
  updateServicePoint: (...args: unknown[]) => mockUpdateServicePoint(...args),
}));

const makeServicePoint = (overrides: Partial<ServicePoint> = {}): ServicePoint => ({
  id: 1,
  name: "Test Service Point",
  identifierOwner: "https://ror.org/123",
  adminEmail: "admin@test.com",
  techEmail: "tech@test.com",
  enabled: true,
  appWritesEnabled: true,
  ...overrides,
});

const renderForm = (servicePoint: ServicePoint) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ServicePointUpdateForm servicePoint={servicePoint} />
    </QueryClientProvider>
  );
};

describe("ServicePointUpdateForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUpdateServicePoint.mockResolvedValue(makeServicePoint());
  });

  it("displays repositoryId and prefix as read-only fields", () => {
    const sp = makeServicePoint({ repositoryId: "ARDC.TEST", prefix: "10.99999" });
    renderForm(sp);

    const repoField = screen.getByLabelText("Repository ID");
    const prefixField = screen.getByLabelText("Prefix");

    expect(repoField).toBeDisabled();
    expect(repoField).toHaveValue("ARDC.TEST");
    expect(prefixField).toBeDisabled();
    expect(prefixField).toHaveValue("10.99999");
  });

  it("displays empty strings when repositoryId and prefix are undefined", () => {
    const sp = makeServicePoint({ repositoryId: undefined, prefix: undefined });
    renderForm(sp);

    const repoField = screen.getByLabelText("Repository ID");
    const prefixField = screen.getByLabelText("Prefix");

    expect(repoField).toHaveValue("");
    expect(prefixField).toHaveValue("");
  });

  it("submits successfully without repositoryId or prefix in payload", async () => {
    const sp = makeServicePoint({ repositoryId: "ARDC.TEST", prefix: "10.99999" });
    renderForm(sp);

    fireEvent.click(screen.getByRole("button", { name: /update/i }));

    await waitFor(() => {
      expect(mockUpdateServicePoint).toHaveBeenCalled();
    });

    const callArgs = mockUpdateServicePoint.mock.calls[0][0];
    expect(callArgs.data.servicePointUpdateRequest).not.toHaveProperty("repositoryId");
    expect(callArgs.data.servicePointUpdateRequest).not.toHaveProperty("prefix");
  });
});
