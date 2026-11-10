import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { ConfigError } from "@/config";
import { ConfigErrorScreen } from "./ConfigErrorScreen";

describe("ConfigErrorScreen", () => {
  it("shows the minimal header with the blocking banner under it", () => {
    const error = new ConfigError("invalid-fields", "[Config] ...", {
      missing: ["keycloak.url", "apiBaseUrl"],
      invalid: [],
    });

    render(<ConfigErrorScreen error={error} />);

    const header = screen.getByTestId("config-error-header");
    const banner = screen.getByRole("alert");
    expect(header).toHaveTextContent("ARDC Research Activity Identifier");
    expect(banner).toHaveTextContent("Missing: keycloak.url, apiBaseUrl.");
    // The banner sits directly under the header.
    expect(header.nextElementSibling).toBe(banner);
  });

  it("shows the load-failed message for a ConfigError that could not load the file", () => {
    render(<ConfigErrorScreen error={new ConfigError("load-failed", "[Config] 404")} />);

    expect(screen.getByRole("alert")).toHaveTextContent("configuration file could not be loaded");
  });

  it("shows a generic message for an unexpected error, without its details", () => {
    render(<ConfigErrorScreen error={new Error("secret internal detail")} />);

    const banner = screen.getByRole("alert");
    expect(banner).toHaveTextContent("The app could not start.");
    expect(banner).not.toHaveTextContent("secret internal detail");
  });
});
