// src/config/AppConfigContext.tsx

import React, { createContext, useContext } from "react";
import { AppConfig } from "./Appconfig";
import { defaultConfig } from "./DefaultConfig";

export const AppConfigContext = createContext<AppConfig>(defaultConfig);

interface AppConfigProviderProps {
  config: AppConfig;
  children: React.ReactNode;
}

/**
 * Wraps the app and provides the loaded configuration to all children
 * via React context. The config is loaded BEFORE React mounts (in index.tsx)
 * so it's always available synchronously — no loading states needed.
 */
export const AppConfigProvider: React.FC<AppConfigProviderProps> = ({
  config,
  children,
}) => {
  return (
    <AppConfigContext.Provider value={config}>
      {children}
    </AppConfigContext.Provider>
  );
};

/**
 * Hook to access the app configuration anywhere in the component tree.
 *
 * Usage:
 *   const { header, footer, theme, content } = useAppConfig();
 */
export const useAppConfig = (): AppConfig => {
  return useContext(AppConfigContext);
};