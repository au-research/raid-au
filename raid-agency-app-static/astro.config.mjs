// @ts-check
import { defineConfig } from "astro/config";
import { readFileSync } from "fs";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";

import tailwindcss from "@tailwindcss/vite";
import sitemap from "@astrojs/sitemap";

const __dirname = dirname(fileURLToPath(import.meta.url));
const embargoedRaids = JSON.parse(
  readFileSync(resolve(__dirname, "src/raw-data/embargoed-raids.json"), "utf-8")
);
const embargoedHandles = new Set(embargoedRaids.map((r) => r.handle));

// https://astro.build/config
// todo: replace `prod` with actual value per environment
export default defineConfig({
  site: `https://static.prod.raid.org.au`,
  integrations: [
    sitemap({
      filter: (page) => {
        if (page.endsWith(".json/") || page.endsWith(".download/")) return false;

        const { pathname } = new URL(page);

        // Exclude the embargoed listing page
        if (pathname.startsWith("/embargoed")) return false;

        // Exclude individual embargoed RAiD pages
        const match = pathname.match(/^\/raids\/(.+?)\/?$/);
        if (match && embargoedHandles.has(match[1])) return false;

        return true;
      },
    }),
  ],
  vite: {
    plugins: [tailwindcss()],
  },
});
