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
  Branding,
  BrandingHeader,
  BrandingFooter,
  BrandingLogo,
  HeaderMenu,
  FooterVariant,
} from "./Appconfig";

export type { RuntimeConfig } from "./RuntimeConfig";

export { defaultConfig, neutralBranding } from "./DefaultConfig";
export {
  resolveBranding,
  isCustomLoginTheme,
  DEFAULT_LOGIN_THEME,
} from "./ResolveBranding";
export { loadConfig } from "./Configloader";
export { sanitiseBranding } from "./SanitiseBranding";
export type { RawBranding } from "./SanitiseBranding";
export { AppConfigProvider, useAppConfig } from "./Appconfigcontext";
export { buildMuiTheme } from "./Buildtheme";
export {
  setRuntimeConfig,
  getRuntimeConfig,
  RuntimeConfigProvider,
  useRuntimeConfig,
  ConfigIssuesProvider,
  useConfigIssues,
  ThemeLookupFailedProvider,
  useThemeLookupFailed,
  BrandingProvider,
  useBranding,
} from "./RuntimeConfigContext";
export { fetchLoginTheme, resolveStartupBranding } from "./KeycloakTheme";
export type { ThemeLookup } from "./KeycloakTheme";
export type { ConfigIssues, ConfigErrorReason } from "./ConfigValidation";
export {
  ConfigError,
  hasConfigIssues,
  isUrlField,
  NO_CONFIG_ISSUES,
} from "./ConfigValidation";
