// @ts-check
import { defineConfig } from "astro/config";

import tailwindcss from "@tailwindcss/vite";

import sitemap from "@astrojs/sitemap";

// https://astro.build/config
// todo: replace `prod` with actual value per environment
export default defineConfig({
  site: `https://static.prod.raid.org.au`,
  integrations: [
    sitemap({
      filter: (page) =>
        !page.endsWith(".json/") && !page.endsWith(".download/"),
    }),
  ],
  vite: {
    plugins: [tailwindcss()],
  },
});
