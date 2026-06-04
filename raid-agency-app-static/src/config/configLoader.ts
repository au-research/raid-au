/**
 * Config loader for the Astro static site.
 *
 * Reads public/app-config.json and deep-merges it with defaultConfig.
 * If the file is absent the site renders with default (no branding).
 *
 * The same file is served at /app-config.json by nginx, so agencies can
 * swap it on S3 / via a volume mount without rebuilding the Docker image
 * (branding + analytics update immediately; static HTML that bakes in
 * raidEnv or siteUrl still requires a rebuild).
 *
 * Usage in .astro components:
 *   import { getSiteConfig } from "@/config";
 *   const config = getSiteConfig();
 */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { AppConfig } from "./AppConfig.types";
import defaultConfig from "./defaultConfig";

function deepMerge<T extends Record<string, any>>(
  target: T,
  source: Partial<T>
): T {
  const result = { ...target };

  for (const key of Object.keys(source) as Array<keyof T>) {
    const sourceVal = source[key];
    const targetVal = target[key];

    if (
      sourceVal &&
      typeof sourceVal === "object" &&
      !Array.isArray(sourceVal) &&
      targetVal &&
      typeof targetVal === "object" &&
      !Array.isArray(targetVal)
    ) {
      result[key] = deepMerge(
        targetVal as Record<string, any>,
        sourceVal as Record<string, any>
      ) as T[keyof T];
    } else if (sourceVal !== undefined) {
      result[key] = sourceVal as T[keyof T];
    }
  }

  return result;
}

function loadConfig(): AppConfig {
  try {
    const configPath = resolve("public", "app-config.json");
    const raw = readFileSync(configPath, "utf-8");
    const customConfig = JSON.parse(raw) as Partial<AppConfig>;
    return deepMerge(defaultConfig, customConfig);
  } catch (error) {
    console.warn(
      "[appConfig] Could not load public/app-config.json, falling back to defaults.",
      error
    );
    return defaultConfig;
  }
}

const appConfig: AppConfig = loadConfig();

export function getSiteConfig(): AppConfig {
  return appConfig;
}

export default appConfig;
