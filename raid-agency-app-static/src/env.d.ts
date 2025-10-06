/// <reference path="../.astro/types.d.ts" />

declare global {
  interface Window {
    dataLayer: any[];
    gtag: (...args: any[]) => void;
  }
}