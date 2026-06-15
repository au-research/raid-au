export type {
  AppConfig,
  SiteConfig,
  LogoConfig,
  FooterLink,
  SocialLink,
  TopBarConfig,
  FooterConfig,
  AnalyticsConfig,
  CachingConfig,
} from "./AppConfig.types";

export { default as defaultConfig } from "./defaultConfig";
export { getSiteConfig, default as siteConfig } from "./configLoader";
