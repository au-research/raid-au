import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeLookupBanner } from "./ThemeLookupBanner";

describe("ThemeLookupBanner", () => {
  it("says the branding could not load and names the support address", () => {
    render(<ThemeLookupBanner supportEmail="support@example.org" />);

    const banner = screen.getByRole("status");
    expect(banner.textContent).toContain("We could not load this site's branding");
    expect(banner.textContent).toContain("the default RAiD look is shown");
    expect(banner.textContent).toContain("Try reloading the page");
    expect(banner.textContent).toContain("support@example.org");
  });

  it("links the support address", () => {
    render(<ThemeLookupBanner supportEmail="support@example.org" />);
    expect(screen.getByRole("link", { name: "support@example.org" })).toHaveProperty(
      "href",
      "mailto:support@example.org"
    );
  });

  it("cannot be dismissed", () => {
    render(<ThemeLookupBanner supportEmail="support@example.org" />);
    expect(screen.queryByLabelText("Dismiss notification")).toBeNull();
  });

  it("reloads the page from the Reload button", () => {
    const reload = vi.fn();
    const original = window.location;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...original, reload },
    });

    render(<ThemeLookupBanner supportEmail="support@example.org" />);
    fireEvent.click(screen.getByRole("button", { name: "Reload" }));
    expect(reload).toHaveBeenCalledTimes(1);

    Object.defineProperty(window, "location", { configurable: true, value: original });
  });
});
