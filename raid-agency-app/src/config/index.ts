// src/config/index.ts

export type {
  AppConfig,
  HeaderConfig,
  FooterConfig,
  ContentConfig,
  ThemeConfig,
  NavLink,
  LogoConfig,
  SocialLink,
  ThemePaletteColor,
} from "./Appconfig";

export type { RuntimeConfig } from "./RuntimeConfig";

export { defaultConfig } from "./DefaultConfig";
export { loadAppConfig } from "./Configloader";
export { AppConfigProvider, useAppConfig } from "./Appconfigcontext";
export { buildMuiTheme } from "./Buildtheme";
export { loadRuntimeConfig } from "./RuntimeConfigLoader";
export {
  setRuntimeConfig,
  getRuntimeConfig,
  RuntimeConfigProvider,
  useRuntimeConfig,
} from "./RuntimeConfigContext";
