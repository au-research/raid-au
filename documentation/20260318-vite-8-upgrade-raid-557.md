# Vite 8 Upgrade - RAID-557

## What Changed

Upgraded `raid-agency-app` from Vite 5.4.21 to Vite 8.0.0 with Rolldown bundler.

### Key Changes

- **Vite 5 → 8**: Major upgrade including switch from esbuild to Rolldown bundler
- **@vitejs/plugin-react 4.3.1 → 6.0.1**: Updated React plugin (dropped `plugin-react-swc`)
- **Node.js 20 → 22**: Updated CI workflow and package.json engine requirement
- **MUI Icons CJS Interop Fix**: Added `muiIconsEsmPlugin` transform hook in `vite.config.ts`

### MUI Icons CJS Interop Issue

Rolldown (Vite 8's bundler) has a CJS interop bug where default imports from `@mui/icons-material` deep paths (e.g., `@mui/icons-material/Close`) resolve to the CJS module wrapper `{ __esModule: true, default: Component }` instead of unwrapping `exports.default`. esbuild (Vite 5) handled this correctly.

**Fix**: A Vite plugin (`muiIconsEsmPlugin`) that uses a `transform` hook to rewrite import specifiers from `@mui/icons-material/Close` to `@mui/icons-material/esm/Close`, which are native ESM and bundle correctly.

**Why `transform` instead of `resolveId`**: The `resolveId` hook approach caused E2E auth failures in CI — the Vite dev server's module graph was affected in a way that prevented the React app from initializing (Keycloak OAuth redirect never fired). The `transform` hook rewrites imports at the source level without interfering with module resolution.

Additionally, `optimizeDeps.include` pre-declares all ESM icon paths to prevent Vite's on-demand dependency optimization from triggering full-page reloads during the Keycloak OAuth flow.

## Why

- Vite 5 is approaching end-of-life
- Vite 8 with Rolldown provides faster builds
- Keeps the frontend toolchain current

## JIRA

- [RAID-557](https://ardc-jira.atlassian.net/browse/RAID-557)

## PR

- [#377](https://github.com/au-research/raid-au/pull/377)

## Related PRs (superseded)

- [#367](https://github.com/au-research/raid-au/pull/367) — manual dependency updates (can be closed)
- [#372](https://github.com/au-research/raid-au/pull/372) — Dependabot Vite upgrade (can be closed)
