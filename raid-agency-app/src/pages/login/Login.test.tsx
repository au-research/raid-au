import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { ConfigIssues, ConfigIssuesProvider, NO_CONFIG_ISSUES } from "@/config";
import { Login } from "./Login";

const mockLogin = vi.fn();
const mockKeycloak = { isInitialized: true, authenticated: false, login: mockLogin };

vi.mock("@/contexts/keycloak-context", () => ({
  useKeycloak: () => mockKeycloak,
}));

vi.mock("@/components/app-nav-bar", () => ({
  AppNavBar: () => <div data-testid="app-nav-bar" />,
}));

const renderLogin = (issues: ConfigIssues, path = "/login?redirect=/raids") =>
  render(
    <ConfigIssuesProvider issues={issues}>
      <MemoryRouter initialEntries={[path]}>
        <Login />
      </MemoryRouter>
    </ConfigIssuesProvider>
  );

describe("Login", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockKeycloak.isInitialized = true;
    mockKeycloak.authenticated = false;
  });

  it("redirects to Keycloak straight away when the config has no issues", () => {
    renderLogin(NO_CONFIG_ISSUES);

    expect(mockLogin).toHaveBeenCalledWith({
      redirectUri: `${window.location.origin}/login?redirect=/raids`,
    });
    expect(screen.queryByRole("button", { name: "Continue to log in" })).not.toBeInTheDocument();
  });

  it("pauses with the header and a continue button when the config has warnings", () => {
    renderLogin({ missing: ["services.orcid"], invalid: [] });

    expect(mockLogin).not.toHaveBeenCalled();
    expect(screen.getByTestId("app-nav-bar")).toBeInTheDocument();
    expect(screen.getByText(/Some features may not work until the configuration is fixed/)).toBeInTheDocument();
  });

  it("continues to Keycloak with the same redirect when the button is clicked", () => {
    renderLogin({ missing: ["services.orcid"], invalid: [] });

    fireEvent.click(screen.getByRole("button", { name: "Continue to log in" }));

    expect(mockLogin).toHaveBeenCalledWith({
      redirectUri: `${window.location.origin}/login?redirect=/raids`,
    });
  });

  it("does not pause an already logged-in user", () => {
    mockKeycloak.authenticated = true;

    renderLogin({ missing: ["services.orcid"], invalid: [] });

    expect(screen.queryByRole("button", { name: "Continue to log in" })).not.toBeInTheDocument();
  });
});
