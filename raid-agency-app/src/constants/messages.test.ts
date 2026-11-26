import { afterEach, describe, expect, it, vi } from "vitest";
import { messages } from "./messages";
import { setRuntimeConfig } from "@/config";
import { baseRuntimeConfig } from "@/test-utils/renderWithConfig";

afterEach(() => setRuntimeConfig(baseRuntimeConfig));

describe("messages.requestFailedContent", () => {
  it("still gives a message, and does not throw, if the config is not loaded", async () => {
    vi.resetModules();
    const { messages: fresh } = await import("./messages");

    expect(() => fresh.requestFailedContent).not.toThrow();
    expect(fresh.requestFailedContent).toBe("Please try again.");
  });

  it("shows the configured supportEmail", () => {
    setRuntimeConfig({ ...baseRuntimeConfig, supportEmail: "helpdesk@example.org" });

    expect(messages.requestFailedContent).toBe("Please try again or email: helpdesk@example.org");
  });

  it("follows a later config, so it is read when shown and not when the app starts", () => {
    setRuntimeConfig({ ...baseRuntimeConfig, supportEmail: "first@example.org" });
    expect(messages.requestFailedContent).toContain("first@example.org");

    setRuntimeConfig({ ...baseRuntimeConfig, supportEmail: "second@example.org" });
    expect(messages.requestFailedContent).toContain("second@example.org");
  });

  it("never shows contactEmail, 'support team' or an ARDC address", () => {
    setRuntimeConfig({
      ...baseRuntimeConfig,
      supportEmail: "helpdesk@example.org",
      contactEmail: "contact@example.org",
    });

    expect(messages.requestFailedContent).not.toContain("contact@example.org");
    expect(messages.requestFailedContent).not.toMatch(/support team|ardc/i);
  });
});
