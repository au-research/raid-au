// src/config/buildTheme.ts

import { createTheme, Theme } from "@mui/material/styles";
import { ThemeConfig } from "./Appconfig";

/**
 * Converts the ThemeConfig from app-config.json into a fully formed
 * MUI theme object. This keeps the JSON config simple (just colours
 * and basic settings) while producing a complete MUI theme.
 */
export function buildMuiTheme(themeConfig: ThemeConfig): Theme {
  return createTheme({
    palette: {
      mode: themeConfig.mode || "light",
      primary: {
        main: themeConfig.palette.primary.main,
        ...(themeConfig.palette.primary.light && {
          light: themeConfig.palette.primary.light,
        }),
        ...(themeConfig.palette.primary.dark && {
          dark: themeConfig.palette.primary.dark,
        }),
        ...(themeConfig.palette.primary.contrastText && {
          contrastText: themeConfig.palette.primary.contrastText,
        }),
      },
      secondary: {
        main: themeConfig.palette.secondary.main,
        ...(themeConfig.palette.secondary.light && {
          light: themeConfig.palette.secondary.light,
        }),
        ...(themeConfig.palette.secondary.dark && {
          dark: themeConfig.palette.secondary.dark,
        }),
        ...(themeConfig.palette.secondary.contrastText && {
          contrastText: themeConfig.palette.secondary.contrastText,
        }),
      },
      background: {
        ...(themeConfig.palette.background?.default && {
          default: themeConfig.palette.background.default,
        }),
        ...(themeConfig.palette.background?.paper && {
          paper: themeConfig.palette.background.paper,
        }),
      },
    },
    typography: {
      ...(themeConfig.typography?.fontFamily && {
        fontFamily: themeConfig.typography.fontFamily,
      }),
      ...(themeConfig.typography?.fontSize && {
        fontSize: themeConfig.typography.fontSize,
      }),
      ...(themeConfig.typography?.h1FontFamily && {
        h1: { fontFamily: themeConfig.typography.h1FontFamily },
        h2: { fontFamily: themeConfig.typography.h1FontFamily },
        h3: { fontFamily: themeConfig.typography.h1FontFamily },
      }),
    },
    shape: {
      borderRadius: themeConfig.shape?.borderRadius ?? 8,
    },
  });
}
