import { describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithConfig } from "@/test-utils/renderWithConfig";
import { BrandingProvider, neutralBranding, RuntimeConfig } from "@/config";
import { Privacy } from "./Privacy";

const full: RuntimeConfig["organisation"] = {
  name: "Example Agency",
  url: "https://agency.example.org/",
  privacyPolicyUrl: "https://agency.example.org/privacy",
  termsUrl: "https://agency.example.org/terms",
  dataDeletionDays: 14,
};

const render = (organisation: RuntimeConfig["organisation"]) =>
  renderWithConfig(<Privacy />, { organisation });

const pageText = () => document.body.textContent ?? "";

describe("Privacy page", () => {
  it("names the configured organisation and links to its policy", () => {
    render(full);

    expect(pageText()).toContain("privacy policy of Example Agency");
    expect(screen.getByRole("link", { name: "privacy policy" })).toHaveAttribute(
      "href",
      "https://agency.example.org/privacy"
    );
    expect(screen.getByRole("link", { name: "Example Agency" })).toHaveAttribute(
      "href",
      "https://agency.example.org/"
    );
  });

  it("names the organisation without a link when no policy address is set", () => {
    render({ name: "Example Agency" });

    expect(pageText()).toContain("privacy policy of Example Agency");
    expect(screen.queryByRole("link", { name: "privacy policy" })).not.toBeInTheDocument();
  });

  it("links the policy without a name when only the address is set", () => {
    render({ privacyPolicyUrl: "https://agency.example.org/privacy" });

    expect(pageText()).toContain("conforms to the privacy policy.");
    expect(pageText()).not.toContain("of the organisation");
    expect(screen.getByRole("link", { name: "privacy policy" })).toBeInTheDocument();
  });

  it("makes no claim about a policy when nothing is configured", () => {
    render({});

    expect(pageText()).not.toContain("fully conforms");
    expect(pageText()).not.toMatch(/ARDC|Australian Research Data Commons|ardc\.edu\.au/i);
    expect(screen.queryByRole("link", { name: "privacy policy" })).not.toBeInTheDocument();
  });

  it("states the configured deletion period", () => {
    render(full);

    expect(pageText()).toContain("will be deleted within 14 days");
  });

  it("promises no timeframe when none is configured", () => {
    render({});

    expect(pageText()).toContain("will be deleted.");
    expect(pageText()).not.toMatch(/within \d+ days/);
  });

  it("says 1 day, not 1 days", () => {
    render({ dataDeletionDays: 1 });

    expect(pageText()).toContain("within 1 day.");
  });

  it("links the helpdesk address, never mailto:undefined", () => {
    render({});

    const link = screen.getByRole("link", { name: "helpdesk@example.org" });
    expect(link).toHaveAttribute("href", "mailto:helpdesk@example.org");
    expect(document.body.innerHTML).not.toContain("undefined");
  });

  it("uses supportEmail even when a footer link is flagged as contact", () => {
    const branding = {
      ...neutralBranding,
      footer: {
        variant: "ardc" as const,
        links: [{ label: "other@example.org", path: "mailto:other@example.org", contact: true }],
      },
    };
    renderWithConfig(
      <BrandingProvider branding={branding}>
        <Privacy />
      </BrandingProvider>
    );

    expect(screen.getByRole("link", { name: "helpdesk@example.org" })).toBeInTheDocument();
    expect(screen.queryByText("other@example.org")).not.toBeInTheDocument();
  });
});
