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

const renderForm = (servicePoint: ServicePoint, servicePoints: ServicePoint[] = []) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(["servicePoints"], servicePoints);

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

  it("submits successfully when repositoryId is undefined", async () => {
    const sp = makeServicePoint({ repositoryId: undefined });
    renderForm(sp, [sp, makeServicePoint({ id: 2, repositoryId: undefined })]);

    fireEvent.click(screen.getByRole("button", { name: /update/i }));

    await waitFor(() => {
      expect(mockUpdateServicePoint).toHaveBeenCalled();
    });
  });

  it("submits successfully when repositoryId is null", async () => {
    const sp = makeServicePoint({ repositoryId: null as unknown as string });
    renderForm(sp, [sp, makeServicePoint({ id: 2, repositoryId: null as unknown as string })]);

    fireEvent.click(screen.getByRole("button", { name: /update/i }));

    await waitFor(() => {
      expect(mockUpdateServicePoint).toHaveBeenCalled();
    });
  });

  it("submits successfully when repositoryId has a value", async () => {
    const sp = makeServicePoint({ repositoryId: "ARDC.TEST" });
    renderForm(sp, [sp, makeServicePoint({ id: 2, repositoryId: "ARDC.OTHER" })]);

    fireEvent.click(screen.getByRole("button", { name: /update/i }));

    await waitFor(() => {
      expect(mockUpdateServicePoint).toHaveBeenCalled();
    });
  });
});
