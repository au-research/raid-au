// RAID-1067: the config files the repository ships hold no ARDC addresses,
// hosts or analytics IDs, so copying one never points a deployment at ARDC's
// environment. app-config.ardc.example.json is left out on purpose: it shows
// what an ARDC deployment sets.

import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { validateConfig, hasConfigIssues } from "./ConfigValidation";

const ROOT = join(__dirname, "..", "..");

const SHIPPED_FILES = ["public/app-config.json", "app-config-examples/app-config.template.json"];

// Anything that would point at ARDC's environment or report to ARDC's analytics.
const ARDC_VALUES = [
  "raid.org.au",
  "ardc.edu.au",
  "au-research",
  "G-286MNGELG2",
  "G-RKPFHJG7Y7",
];

const read = (file: string): string => readFileSync(join(ROOT, file), "utf-8");

describe.each(SHIPPED_FILES)("%s (RAID-1067)", (file) => {
  const text = read(file);

  it.each(ARDC_VALUES)("holds no %s", (value) => {
    expect(text).not.toContain(value);
  });

  it("holds no branding, so copying it does not turn ARDC branding on", () => {
    const config = JSON.parse(text) as Record<string, unknown>;

    expect(config).not.toHaveProperty("branding");
    expect(config).not.toHaveProperty("header");
    expect(config).not.toHaveProperty("footer");
    expect(config).not.toHaveProperty("theme");
  });

  it("sets no analytics ids", () => {
    const config = JSON.parse(text) as { googleAnalytics?: Record<string, unknown> };

    expect(config.googleAnalytics?.measurementId).toBeUndefined();
    expect(config.googleAnalytics?.measurementIdDemo).toBeUndefined();
  });
});

describe("public/app-config.json as the local development file", () => {
  const config = JSON.parse(read("public/app-config.json")) as Record<string, unknown>;

  it("passes the app's own validation, so the app starts with it", () => {
    const { blocking, warning } = validateConfig(config);

    expect(hasConfigIssues(blocking)).toBe(false);
    expect(hasConfigIssues(warning)).toBe(false);
  });

  it("points Keycloak and the API at the local stack, which CI also uses", () => {
    expect(config.keycloak).toMatchObject({ url: "http://localhost:8001", realm: "raid", clientId: "raid-api" });
    expect(config.apiBaseUrl).toBe("http://localhost:8080");
  });
});
