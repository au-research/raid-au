export type {
  SiteConfig,
  LogoConfig,
  FooterLink,
  SocialLink,
  TopBarConfig,
  FooterConfig,
} from "./SiteConfig.types";

export { default as defaultConfig } from "./defaultConfig";
export { getSiteConfig, default as siteConfig } from "./configLoader";
