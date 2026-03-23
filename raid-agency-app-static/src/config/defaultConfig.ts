/**
 * Default site configuration.
 *
 * Used when USE_CUSTOM_CONFIG is not set to "true".
 * No branding — top bar and footer are hidden.
 * This is the baseline that any RA sees without a custom config.
 */

import type { SiteConfig } from "./SiteConfig.types";

const defaultConfig: SiteConfig = {
  header: {
    topBar: {
      show: false,
      label: "",
      href: "",
    },
  },
  footer: {
    show: false,
    main: {
      logos: [],
      text: [],
    },
    links: [],
    copyright: "",
    showSocialLinks: false,
    socialLinks: [],
  },
};

export default defaultConfig;
