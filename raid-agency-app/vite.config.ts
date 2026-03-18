import react from "@vitejs/plugin-react";
import path from "path";
import { defineConfig } from "vite";

/**
 * Vite 8 (Rolldown) has a CJS interop issue where default imports from
 * @mui/icons-material deep paths (e.g. '@mui/icons-material/Close') resolve
 * to the CJS module object { __esModule: true, default: Component } instead
 * of the component itself. This plugin rewrites those import specifiers to
 * point at the ESM versions which are bundled correctly.
 */
function muiIconsEsmPlugin() {
  const iconImportRe = /(from\s+['"])@mui\/icons-material\/(?!esm)([\w]+)(['"])/g;
  return {
    name: "mui-icons-esm",
    enforce: "pre" as const,
    transform(code: string, id: string) {
      if (!code.includes("@mui/icons-material/")) return;
      if (id.includes("node_modules")) return;
      const transformed = code.replace(
        iconImportRe,
        "$1@mui/icons-material/esm/$2$3"
      );
      if (transformed !== code) return transformed;
    },
  };
}

export default defineConfig({
  plugins: [muiIconsEsmPlugin(), react()],
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
