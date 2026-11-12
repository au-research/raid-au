import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { ConfigIssueBanner } from "./ConfigIssueBanner";

describe("ConfigIssueBanner", () => {
  describe("warning", () => {
    it("names missing fields and is announced politely", () => {
      render(
        <ConfigIssueBanner
          level="warning"
          issues={{ missing: ["services.orcid", "app.orcid.placeholder"], invalid: [] }}
        />
      );

      const banner = screen.getByRole("status");
      expect(banner).toHaveTextContent(
        "Some configuration is missing: services.orcid, app.orcid.placeholder."
      );
      expect(banner).toHaveTextContent(
        "Some features may not work as expected. Site administrators: update app-config.json and redeploy the release."
      );
    });

    it("fits on two lines: a headline naming the fields and a message", () => {
      const { container } = render(
        <ConfigIssueBanner level="warning" issues={{ missing: ["services.orcid"], invalid: [] }} />
      );

      const content = container.querySelector(".notification-banner__content");
      expect(content?.children).toHaveLength(2);
    });

    it("describes invalid URL fields", () => {
      render(
        <ConfigIssueBanner level="warning" issues={{ missing: [], invalid: ["services.staticProd"] }} />
      );

      expect(screen.getByRole("status")).toHaveTextContent(
        "Some configuration is invalid: services.staticProd (not a valid URL)."
      );
    });

    it("lists missing then invalid fields when there are both", () => {
      render(
        <ConfigIssueBanner
          level="warning"
          issues={{ missing: ["supportEmail"], invalid: ["services.orcid"] }}
        />
      );

      expect(screen.getByRole("status")).toHaveTextContent(
        "Some configuration is missing: supportEmail. Invalid: services.orcid (not a valid URL)."
      );
    });

    it("cannot be dismissed", () => {
      render(<ConfigIssueBanner level="warning" issues={{ missing: ["supportEmail"], invalid: [] }} />);

      expect(screen.queryByRole("button", { name: /dismiss/i })).not.toBeInTheDocument();
    });
  });

  describe("blocking", () => {
    it("names missing and invalid fields as an alert with a reload button", () => {
      render(
        <ConfigIssueBanner
          level="blocking"
          reason="invalid-fields"
          issues={{ missing: ["keycloak.url"], invalid: ["apiBaseUrl"] }}
        />
      );

      const banner = screen.getByRole("alert");
      expect(banner).toHaveTextContent("This site is not configured correctly");
      expect(banner).toHaveTextContent("The app cannot start. Missing: keycloak.url. Invalid: apiBaseUrl (not a valid URL).");
      expect(banner).toHaveTextContent("Site administrators: check app-config.json and redeploy the release.");
      expect(screen.getByRole("button", { name: "Reload" })).toBeInTheDocument();
    });

    it("says the configuration file could not be loaded", () => {
      render(<ConfigIssueBanner level="blocking" reason="load-failed" />);

      const banner = screen.getByRole("alert");
      expect(banner).toHaveTextContent("its configuration file could not be loaded");
      expect(banner).toHaveTextContent("check that app-config.json is deployed, then redeploy the release");
    });

    it("has a generic message for other startup failures", () => {
      render(<ConfigIssueBanner level="blocking" reason="startup-failed" />);

      expect(screen.getByRole("alert")).toHaveTextContent("The app could not start.");
    });
  });
});
