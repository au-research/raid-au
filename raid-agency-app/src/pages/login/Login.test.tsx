import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import {
  ConfigIssues,
  ConfigIssuesProvider,
  NO_CONFIG_ISSUES,
  ThemeLookupFailedProvider,
} from "@/config";
import { Login } from "./Login";

const mockKeycloak = {
  isInitialized: true,
  authenticated: false,
  login: vi.fn(),
};

vi.mock("@/contexts/keycloak-context", () => ({
  useKeycloak: () => mockKeycloak,
}));

vi.mock("@/components/app-nav-bar", () => ({
  AppNavBar: () => <div data-testid="app-nav-bar" />,
}));

const renderLogin = (issues: ConfigIssues = NO_CONFIG_ISSUES, themeLookupFailed = false) =>
  render(
    <ConfigIssuesProvider issues={issues}>
      <ThemeLookupFailedProvider failed={themeLookupFailed}>
        <MemoryRouter>
          <Login />
        </MemoryRouter>
      </ThemeLookupFailedProvider>
    </ConfigIssuesProvider>
  );

describe("Login", () => {
  beforeEach(() => {
    mockKeycloak.isInitialized = true;
    mockKeycloak.authenticated = false;
    mockKeycloak.login = vi.fn();
  });

  it("sends a signed-out user straight to Keycloak when nothing needs reading", () => {
    renderLogin();

    expect(mockKeycloak.login).toHaveBeenCalledTimes(1);
    expect(screen.getByText("Redirecting to login...")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Continue to log in" })).not.toBeInTheDocument();
  });

  it("pauses on a failed theme lookup so the banner can be read", () => {
    renderLogin(NO_CONFIG_ISSUES, true);

    expect(mockKeycloak.login).not.toHaveBeenCalled();
    expect(screen.getByTestId("app-nav-bar")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Continue to log in" })).toBeInTheDocument();
  });

  it("pauses on config warnings", () => {
    renderLogin({ missing: ["services.orcid"], invalid: [] });

    expect(mockKeycloak.login).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Continue to log in" })).toBeInTheDocument();
  });

  it("logs in when the user chooses to continue", () => {
    renderLogin(NO_CONFIG_ISSUES, true);

    fireEvent.click(screen.getByRole("button", { name: "Continue to log in" }));

    expect(mockKeycloak.login).toHaveBeenCalledTimes(1);
  });

  it("waits for Keycloak to initialise before doing anything", () => {
    mockKeycloak.isInitialized = false;

    renderLogin(NO_CONFIG_ISSUES, true);

    expect(mockKeycloak.login).not.toHaveBeenCalled();
    expect(screen.queryByRole("button", { name: "Continue to log in" })).not.toBeInTheDocument();
  });
});
