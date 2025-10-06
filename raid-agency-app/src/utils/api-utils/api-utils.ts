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
export function getApiEndpoint() {
  const hostname = window.location.hostname;
  const baseDomain = "raid.org.au";
  
  // Determine environment based on hostname
  const environment = hostname.includes("test")
    ? "test"
    : hostname.includes("demo")
    ? "demo"
    : hostname.includes("prod")
    ? "prod"
    : hostname.includes("stage")
    ? "stage"
    : "dev";

  // Build the API endpoint URL
  return `${environment === "dev" ? "http" : "https"}://${
    environment === "dev"
      ? "localhost:8080"
      : `api.${environment}.${baseDomain}`
  }`.replace(/\/+$/, ""); // Remove any trailing slashes
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
    : hostname.includes("prod")
    ? "prod"
    : hostname.includes("stage")
    ? "stage"
    : "dev";

  return environment;
}
