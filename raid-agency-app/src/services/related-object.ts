/**
 * Related Object Service Module
 *
 * This module provides functionality for working with related objects in the RAID system,
 * particularly for resolving DOI information from external services like Crossref, Datacite and mEDRA.
 */

import { useQuery } from "@tanstack/react-query";

// Removed getTitleFromDOI function as it was not used in the original code

interface FetchDOIOptions {
  timeout?: number;
  userAgent?: string;
}

/**
 * Fetches detailed citation with publisher info (text/x-bibliography; style=apa) for a given DOI URL.
 * @param doiUrl - The DOI URL
 * @param options - Optional configuration
 * @returns Promise resolving to detailed citation string or null if not found
 */
async function fetchDetailedDOICitation(
  doiUrl: string, 
  options: FetchDOIOptions = {}
): Promise<string | null> {
  const { timeout = 8000, userAgent = 'DOI-Fetcher-Detailed/1.0' } = options;

  // Validate DOI URL format
  const doiPattern = /^https?:\/\/(?:dx\.)?doi\.org\/10\.\d+\/.+$/;
  if (!doiPattern.test(doiUrl)) {
    throw new Error('Invalid DOI URL format');
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  try {
    // First try text/x-bibliography for formatted citation
    const response = await fetch(doiUrl, {
      method: 'GET',
      headers: {
        'Accept': 'text/x-bibliography; style=apa',
        'User-Agent': userAgent,
      },
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      // If the response is not OK, throw an error with status
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const citation = await response.text();

    if (!citation.trim()) {
      throw new Error('Empty citation received');
    }

    // For detailed citation, we want to keep more information
    return cleanDetailedCitation(citation.trim());

  } catch (error) {
    clearTimeout(timeoutId);

    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        throw new Error(`Request timeout after ${timeout}ms`);
      }
      throw error;
    }

    throw new Error('Unknown error occurred while fetching detailed DOI citation');
  }
}

 /**
 * Enhanced citation cleaning that preserves publisher and detailed info
 * @param citation - Raw citation string
 * @returns Detailed citation with publisher info preserved
 */
function cleanDetailedCitation(citation: string): string {
  let cleaned = citation;

  // Remove DOI URLs (both http and https, with or without dx.)
  // This regex matches URLs that start with http(s)://doi.org or dx.doi.org
  // and continues until it hits whitespace or end of string
  cleaned = cleaned.replace(/https?:\/\/(?:dx\.)?doi\.org\/[^\s]+/gi, '');

  // Remove standalone DOI patterns (e.g., "DOI: 10.1234/..." or just "10.1234/...")
  // that might appear anywhere in the text
  cleaned = cleaned.replace(/\bDOI\s*:?\s*10\.\d+\/[^\s,.;]+/gi, '');
  
  // Remove bare DOI patterns that start with 10. and contain a forward slash
  cleaned = cleaned.replace(/\b10\.\d+\/[^\s,.;]+/g, '');

  // Clean up any trailing punctuation that might be left after DOI removal
  cleaned = cleaned.replace(/\s*[,;]\s*\./, '.');
  
  // Clean up extra spaces
  cleaned = cleaned.replace(/\s{2,}/g, ' ');
  
  // Clean up any double periods
  cleaned = cleaned.replace(/\.\s*\.\s*/g, '. ');
  
  // Remove any trailing whitespace before periods
  cleaned = cleaned.replace(/\s+\./g, '.');

  // Ensure proper ending
  cleaned = cleaned.trim();
  if (cleaned && !cleaned.endsWith('.') && !cleaned.endsWith('!') && !cleaned.endsWith('?')) {
    cleaned += '.';
  }

  return cleaned;
}

/**
 * Batch fetch detailed citations with publisher info
 * @param doiUrls - Array of DOI URLs
 * @param options - Optional configuration
 * @returns Promise resolving to array of detailed citations
 */
async function batchFetchDetailedCitations(
  doiUrls: string[], 
  options: FetchDOIOptions = {}
): Promise<(string | null)[]> {
  
  const promises = doiUrls.map(url => 
    fetchDetailedDOICitation(url, options).catch(() => null)
  );
  
  const results = await Promise.all(promises);
  
  return results;
}
/**
 * Utility to construct DOI URL from various formats
 * @param identifier - DOI string, URL, or other identifier
 * @returns Properly formatted DOI URL
 */
function constructDOIUrl(identifier: string): string {
  // If it's already a complete DOI URL, return as is
  if (identifier.startsWith('https://doi.org/')) {
    return identifier;
  }
  
  // If it contains a DOI pattern, extract and construct URL
  const doiMatch = identifier.match(/10\.\d+\/.+$/);
  if (doiMatch) {
    return `https://doi.org/${doiMatch[0]}`;
  }
  
  // If it starts with "doi:", remove prefix and construct URL
  if (identifier.startsWith('doi:')) {
    return `https://doi.org/${identifier.slice(4)}`;
  }
  
  // Assume it's a bare DOI and construct URL
  return `https://doi.org/${identifier}`;
}
// * Custom hook to fetch related object citations from localStorage
const useRelatedObjectCitations = () => {
  return useQuery({
    queryKey: ["relatedObjectCitations"],
    queryFn: () => {
      try {
        const stored = localStorage.getItem("relatedObjectCitations");
        return stored ? new Map(JSON.parse(stored)) : new Map();
      } catch (error) {
        console.error('Error accessing relatedObjectCitations:', error);
        return new Map();
      }
    },
  });
};
// Export all functions and types for external use

export {
  fetchDetailedDOICitation,
  batchFetchDetailedCitations,
  useRelatedObjectCitations,
  cleanDetailedCitation,
  constructDOIUrl,
  type FetchDOIOptions
};
