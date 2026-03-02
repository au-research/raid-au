// src/config/configLoader.ts

import { AppConfig } from "./Appconfig";
import { defaultConfig } from "./DefaultConfig";

/**
 * Deep merges source into target. Arrays are replaced, not merged.
 * This ensures partial configs in app-config.json override defaults
 * without requiring every field to be present.
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
      sourceVal !== null &&
      sourceVal !== undefined &&
      typeof sourceVal === "object" &&
      !Array.isArray(sourceVal) &&
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
 * Loads the app configuration.
 *
 * Behaviour:
 * - If VITE_APP_USE_CUSTOM_CONFIG is set to "true", fetches /app-config.json
 *   and deep-merges it with the default config (so partial overrides work).
 * - Otherwise, returns the default config immediately.
 *
 * The JSON file is served from the CRA `public/` folder, so it can be
 * replaced at deploy time without rebuilding the app.
 */
export async function loadAppConfig(): Promise<AppConfig> {
  const useCustomConfig =
    import.meta.env.VITE_APP_USE_CUSTOM_CONFIG === "true";

  if (!useCustomConfig) {
    console.info(
      "[AppConfig] VITE_APP_USE_CUSTOM_CONFIG is not enabled. Using default config."
    );
    return defaultConfig;
  }

  try {
    const configUrl =
      import.meta.env.REACT_APP_CONFIG_URL || "/app-config.json";

    console.info(`[AppConfig] Fetching custom config from ${configUrl}`);

    const response = await fetch(configUrl, {
      // Bust cache to always get the latest config
      headers: { "Cache-Control": "no-cache" },
    });

    if (!response.ok) {
      throw new Error(
        `Failed to fetch config: ${response.status} ${response.statusText}`
      );
    }

    const customConfig: Partial<AppConfig> = await response.json();

    // Deep merge so partial overrides work — you don't have to
    // duplicate the entire default config in app-config.json
    const mergedConfig = deepMerge(defaultConfig, customConfig);

    console.info("[AppConfig] Custom config loaded and merged successfully.");
    return mergedConfig;
  } catch (error) {
    console.error(
      "[AppConfig] Failed to load custom config. Falling back to defaults.",
      error
    );
    return defaultConfig;
  }
}
