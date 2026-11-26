import { describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithConfig } from "@/test-utils/renderWithConfig";
import { RuntimeConfig } from "@/config";
import { UsageTerms } from "./UsageTerms";

const render = (organisation: RuntimeConfig["organisation"]) =>
  renderWithConfig(<UsageTerms />, { organisation });

const pageText = () => document.body.textContent ?? "";

describe("Usage Terms page", () => {
  it("names the organisation and links to its terms", () => {
    render({ name: "Example Agency", termsUrl: "https://agency.example.org/terms" });

    expect(pageText()).toContain("Example Agency");
    expect(screen.getByRole("link", { name: "Terms & conditions" })).toHaveAttribute(
      "href",
      "https://agency.example.org/terms"
    );
  });

  it("links the terms without a name when only the address is set", () => {
    render({ termsUrl: "https://agency.example.org/terms" });

    expect(screen.getByRole("link", { name: "Terms & conditions" })).toBeInTheDocument();
    expect(pageText()).toContain("Please refer to the Terms & conditions page");
  });

  it("points to the helpdesk when no terms address is configured", () => {
    render({ name: "Example Agency" });

    expect(screen.queryByRole("link", { name: "Terms & conditions" })).not.toBeInTheDocument();
    expect(pageText()).toContain("contact");
    expect(screen.getByRole("link", { name: "helpdesk@example.org" })).toHaveAttribute(
      "href",
      "mailto:helpdesk@example.org"
    );
  });

  it("has no ARDC wording or ardc.edu.au link by default", () => {
    render({});

    expect(pageText()).not.toMatch(/ARDC|Australian Research Data Commons/i);
    expect(document.body.innerHTML).not.toContain("ardc.edu.au");
  });
});
