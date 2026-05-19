import React, { createContext, useContext } from "react";
import { RuntimeConfig } from "./RuntimeConfig";

// Module-level store for non-React consumers (services, utils)
let _runtimeConfig: RuntimeConfig | null = null;

export function setRuntimeConfig(config: RuntimeConfig): void {
  _runtimeConfig = config;
}

export function getRuntimeConfig(): RuntimeConfig {
  if (!_runtimeConfig) {
    throw new Error(
      "[RuntimeConfig] Not initialized. Ensure loadRuntimeConfig() completes before rendering."
    );
  }
  return _runtimeConfig;
}

// React context for component consumers
const RuntimeConfigContext = createContext<RuntimeConfig | null>(null);

interface RuntimeConfigProviderProps {
  config: RuntimeConfig;
  children: React.ReactNode;
}

export const RuntimeConfigProvider: React.FC<RuntimeConfigProviderProps> = ({
  config,
  children,
}) => (
  <RuntimeConfigContext.Provider value={config}>
    {children}
  </RuntimeConfigContext.Provider>
);

export function useRuntimeConfig(): RuntimeConfig {
  const config = useContext(RuntimeConfigContext);
  if (!config) {
    throw new Error("useRuntimeConfig must be used within a RuntimeConfigProvider");
  }
  return config;
}
