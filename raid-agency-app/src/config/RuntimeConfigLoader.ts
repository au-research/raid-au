import { RuntimeConfig } from "./RuntimeConfig";

export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
  const response = await fetch("/runtime-config.json", {
    headers: { "Cache-Control": "no-cache" },
  });

  if (!response.ok) {
    throw new Error(
      `[RuntimeConfig] Failed to load /runtime-config.json: ${response.status} ${response.statusText}. ` +
        "This file is required for every deployment."
    );
  }

  const config: RuntimeConfig = await response.json();
  console.info("[RuntimeConfig] Loaded successfully.", config.environment);
  return config;
}
