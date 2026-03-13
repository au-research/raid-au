/**
 * API Utilities Module
 *
 * This module provides utility functions for determining API endpoints
 * and environment information based on the current hostname.
 */

/**
 * Determines the appropriate API endpoint URL based on the current hostname
 * 
 * This function examines the current window location to determine which
 * environment the application is running in (dev, test, demo, prod, or stage)
 * and returns the corresponding API endpoint URL.
 * 
 * - For development: http://localhost:8080
 * - For other environments: https://api.{environment}.raid.org.au
 * 
 * @returns The complete API endpoint URL without trailing slashes
 */
// @ts-expect-error: package exports map prevents TypeScript from resolving the bundled .mjs declarations
import psl from 'psl';

export function getApiEndpoint(hostname = window.location.hostname): string {
  // Special case for localhost
  if (hostname === 'localhost') {
    return 'http://localhost:8080';
  }

  const parts = hostname.split('.');

  // Replace the service (first part) with 'api'
  parts[0] = 'api';

  return `https://${parts.join('.')}`
}
/**
 * Determines the current environment based on the hostname
 * 
 * This function examines the current window location to identify which
 * environment the application is running in:
 * - test: Testing environment
 * - demo: Demonstration environment
 * - prod: Production environment
 * - stage: Staging environment
 * - dev: Development environment (default)
 * 
 * @returns A string representing the current environment
 */
export function getEnv() {
  const hostname = window.location.hostname;

  const environment = hostname.includes("test")
    ? "test"
    : hostname.includes("demo")
    ? "demo"
    : hostname.includes("stage")
    ? "stage"
    : hostname.includes("dev") || hostname.includes("localhost")
    ? "dev"
    : hostname.includes("prod")
    ? "prod"
    : ""; // Default to empty if no other keyword found

  return environment;
}

// Using psl to get the root domain from a URL
// @param {string} url - The full URL from which to extract the root domain
// @returns {string | null} The root domain or null if the URL is invalid
export const getRootDomain = (url: string) => {
  try {
    const urlObj = new URL(url);
    const parsed = psl.parse(urlObj.hostname);
    return parsed.domain; // Returns the registrable domain
  } catch (error) {
    console.error('Invalid URL:', error);
    return null;
  }
}
