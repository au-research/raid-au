import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig, type Plugin } from "vite";

/**
 * Vite 8 (Rolldown) has a CJS interop issue where default imports from
 * @mui packages using deep paths (e.g. '@mui/icons-material/Close',
 * '@mui/system/createStyled') resolve to the CJS module object instead
 * of the actual export. This plugin rewrites import specifiers to point
 * at ESM versions which are bundled correctly.
 *
 * Affected packages have an esm/ subdirectory mirroring the CJS layout
 * but no per-subpath package.json with a "module" field:
 * - @mui/icons-material (icon components)
 * - @mui/system (createStyled, createTheme, etc.)
 *
 * Not affected: @mui/utils (each subpath has its own package.json with
 * a "module" field, so Vite resolves ESM automatically).
 */
function muiEsmPlugin(): Plugin {
  const muiDeepPathRe =
    /(from\s+['"])@mui\/(icons-material|system)\/(?!esm)([\w]+)(['"])/g;
  return {
    name: "mui-esm-redirect",
    enforce: "pre",
    transform(code, id) {
      if (!code.includes("@mui/")) return;
      const transformed = code.replace(
        muiDeepPathRe,
        "$1@mui/$2/esm/$3$4"
      );
      if (transformed !== code) return transformed;
    },
  };
}

export default defineConfig({
  plugins: [muiEsmPlugin(), react()],
  base: "/.",
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    sourcemap: true,
  },
  optimizeDeps: {
    include: [
      "@mui/icons-material/esm/AddBoxOutlined",
      "@mui/icons-material/esm/Close",
      "@mui/icons-material/esm/CloseRounded",
      "@mui/icons-material/esm/ContentCopy",
      "@mui/icons-material/esm/ExpandMore",
      "@mui/icons-material/esm/Fingerprint",
      "@mui/icons-material/esm/HelpOutline",
      "@mui/icons-material/esm/IndeterminateCheckBoxOutlined",
      "@mui/icons-material/esm/InfoOutlined",
      "@mui/icons-material/esm/OpenInNew",
      "@mui/icons-material/esm/Person",
      "@mui/icons-material/esm/Refresh",
      "@mui/icons-material/esm/TravelExplore",
    ],
  },
});
