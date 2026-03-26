/**
 * Config loader for the Astro static site.
 *
 * How it works:
 *   - If PUBLIC_USE_CUSTOM_CONFIG=true → reads public/site-config.json
 *     and deep-merges it with defaultConfig.
 *   - If PUBLIC_USE_CUSTOM_CONFIG is not set → uses defaultConfig as-is
 *     (no branding, top bar and footer hidden).
 *
 * This mirrors the React app's configLoader.ts pattern.
 * Teams can swap public/site-config.json at deploy time to
 * change branding without touching any code.
 *
 * Usage in .astro components:
 *   import { getSiteConfig } from "@/config/configLoader";
 *   const config = getSiteConfig();
 */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { SiteConfig } from "./SiteConfig.types";
import defaultConfig from "./defaultConfig";

/**
 * Deep merge two objects. Arrays are replaced, not appended.
 * Same behaviour as the React app's configLoader.
 */
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

/**
 * Load and return the site configuration.
 * Called once at build time — the result is baked into static HTML.
 */
function loadConfig(): SiteConfig {
  const useCustomConfig = import.meta.env.PUBLIC_USE_CUSTOM_CONFIG === "true";

  if (!useCustomConfig) {
    return defaultConfig;
  }

  try {
    const configPath = resolve("public", "site-config.json");
    const raw = readFileSync(configPath, "utf-8");
    const customConfig = JSON.parse(raw) as Partial<SiteConfig>;
    return deepMerge(defaultConfig, customConfig);
  } catch (error) {
    console.warn(
      "[siteConfig] Could not load public/site-config.json, falling back to defaults.",
      error
    );
    return defaultConfig;
  }
}

// Loaded once at build time
const siteConfig: SiteConfig = loadConfig();

export function getSiteConfig(): SiteConfig {
  return siteConfig;
}

export default siteConfig;
