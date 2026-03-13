/**
 * String Utilities Module
 *
 * This module provides utility functions for string manipulation and validation,
 * particularly for working with formatted strings in the RAID application.
 */

/**
 * Removes numbers enclosed in square brackets from a string
 * 
 * This function is useful for cleaning display text that may contain
 * automatically inserted index numbers.
 * 
 * @param str - The input string to clean
 * @returns The string with numbers in brackets removed
 * 
 * @example
 * // Returns "Item"
 * removeNumberInBrackets("Item[1]");
 */
export function removeNumberInBrackets(str: string) {
  return str.replace(/\[\d+\](?![^[]*\])/, "");
}

/**
 * Checks if a value is a valid number
 * 
 * This function performs a thorough validation to ensure a value is:
 * - Actually of type number
 * - Not null
 * - Not NaN (Not a Number)
 * 
 * @param value - The value to check, of any type
 * @returns True if the value is a valid number, false otherwise
 */
export function isValidNumber(value: unknown) {
  return typeof value === "number" && value !== null && !isNaN(value);
}

/**
 * Extracts the last two segments from a URL
 * 
 * This is particularly useful for working with DOIs and other identifiers
 * where the last two segments often represent the actual identifier.
 * 
 * @param url - The full URL to process
 * @returns The last two URL segments joined with a slash, or null if fewer than 2 segments exist
 * 
 * @example
 * // Returns "10.1234/abcd"
 * getLastTwoUrlSegments("https://doi.org/10.1234/abcd");
 */
export function getLastTwoUrlSegments(url: string): string | null {
  const parts = url.split("/").filter((part) => part.length > 0);
  if (parts.length < 2) {
    return null;
  }
  return parts.slice(-2).join("/");
}

/**
 * Extracts an ORCID identifier from a URL or string
 * 
 * This function can extract the ORCID ID from various formats, including:
 * - Full ORCID URL: https://orcid.org/0000-0000-0000-0000
 * - Sandbox ORCID URL: https://sandbox.orcid.org/0000-0000-0000-0000
 * - Plain ORCID ID: 0000-0000-0000-0000
 * 
 * @param url - The URL or string containing an ORCID ID
 * @returns The extracted ORCID ID, or null if no valid ORCID ID is found
 */
export function extractOrcidId(url: string): string | null {
  const regex =
    /(?:https?:\/\/(?:sandbox\.)?orcid\.org\/)?(\d{4}-\d{4}-\d{4}-\d{3}[\dX])/;
  const match = url.match(regex);
  return match ? match[1] : null;
}
