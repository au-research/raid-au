
/**
 * Citation Fetcher Module
 * 
 * This module handles fetching and processing citations from DOI.org
 * with caching support and fallback mechanisms.
 * 
 * Features:
 * - Fetches citations in APA format from DOI.org
 * - Cleans and formats citation text
 * - Implements caching with configurable TTL
 * - Provides retry logic with exponential backoff
 * - Handles various DOI formats and edge cases
 * 
 * @module fetch-citation
 */

// Simple in-memory cache for DOI citations

function cleanDetailedCitation(citation) {
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

// Fetch citation for a DOI with caching
async function fetchCitation(params) {
   const {doi, makeRequestWithRetry, stats, citationCache, config} = params; 
  // Check cache first
  if (config.enableCaching && citationCache.has(doi) && (Date.now() - citationCache.get(doi).timestamp < config.cachingTime)) {
    stats.cachedCitations++;
    return citationCache.get(doi).citation;
  }

  const url = `${doi}`;

  try {
    const response = await makeRequestWithRetry(url, {
      method: 'GET',
      headers: {
        'Accept': 'text/bibliography; style=apa, text/x-bibliography; style=apa, text/plain'
      }
    }, 2); // Fewer retries for DOI lookups
    
    // Check if we got a valid citation
    const citation = cleanDetailedCitation(response.data);

    if (citation && !citation.includes('DOI Not Found')) {
      if (config.enableCaching) {
        citationCache.set(doi, {
          citation: citation,
          timestamp: Date.now()
        });
      }
      stats.successfulCitations++;
      return citation;
    }
    return "DOI exists but metadata could not be retrieved - contact info@doi.org for help with this";
  } catch (error) {
    stats.failedCitations++;
    return "DOI exists but metadata could not be retrieved - contact info@doi.org for help with this";
  }
}


export {
    fetchCitation
}