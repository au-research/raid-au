import { describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithConfig } from "@/test-utils/renderWithConfig";
import { RuntimeConfig } from "@/config";
import { AboutRaid } from "./AboutRaid";

const render = (organisation: RuntimeConfig["organisation"]) =>
  renderWithConfig(<AboutRaid />, { organisation });

describe("About RAiD page", () => {
  it("has no ARDC wording, region claim or ardc.edu.au link by default", () => {
    render({});

    expect(document.body.textContent).not.toMatch(/ARDC|Australian Research Data Commons|Oceania/i);
    expect(document.body.innerHTML).not.toContain("ardc.edu.au");
    expect(screen.queryByRole("link", { name: "RAiD GitHub" })).not.toBeInTheDocument();
  });

  it("still links the RAiD project and the ISO standard", () => {
    render({});

    expect(screen.getByRole("link", { name: "RAiD project" })).toHaveAttribute(
      "href",
      "https://www.raid.org"
    );
    expect(screen.getByRole("link", { name: "RAiD ISO standard" })).toBeInTheDocument();
    expect(document.body.textContent).toContain(
      "This RAiD Service is an implementation of the RAiD ISO standard."
    );
  });

  it("adds a link to the configured organisation", () => {
    render({ name: "Example Agency", url: "https://agency.example.org/" });

    expect(screen.getByRole("link", { name: "Example Agency" })).toHaveAttribute(
      "href",
      "https://agency.example.org/"
    );
  });

  it("adds no organisation link without both a name and an address", () => {
    render({ name: "Example Agency" });
    expect(screen.queryByRole("link", { name: "Example Agency" })).not.toBeInTheDocument();
  });
});
