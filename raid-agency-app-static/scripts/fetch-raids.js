#!/usr/bin/env node

/**
 * RAID Data Collection Script with Citation Enhancement
 *
 * This script fetches RAID (Research Activity Identifier) data from a designated API
 * and enriches it with citation, ORCID, ROR, and service point information.
 *
 * Main Operations:
 * 1. Loads configuration from public/app-config.json and secrets from environment variables
 * 2. Validates required configuration
 * 3. Authenticates with IAM endpoint using OAuth 2.0 client credentials
 * 4. Fetches all public RAID data from the API
 * 5. Fetches citation data for all DOIs found in relatedObjects
 * 6. Enriches contributor data with ORCID display names
 * 7. Enriches organisation data with ROR names
 * 8. Adds service point names to each RAID record
 * 9. Saves enriched RAID data to raids.json
 * 10. Fetches and saves embargoed RAID summaries (separate dumper client)
 * 11. Extracts and saves unique handles
 *
 * Features:
 * - Automatic token refresh: tokens are re-fetched via client credentials before expiry,
 *   so long-running scripts (30+ min) never send an expired token
 * - Concurrent DOI processing for better performance
 * - Smart caching to avoid redundant API calls (citations, ORCID, ROR, service points)
 * - HTML response detection: doi.org HTML error pages are rejected and never cached
 * - Retry logic with exponential backoff
 * - Progress tracking with real-time updates
 * - Configurable rate limiting to respect API limits
 * - Cross-platform compatibility (Windows/Mac/Linux)
 * 
 * Output Files:
 * - ./src/raw-data/raids.json - Enhanced RAID data with citations
 * - ./src/raw-data/handles.json - Combined handles from all environments
 * - ./src/raw-data/.citation-cache.json - Citation cache (if caching enabled)
 * 
 * Environment Variables:
 * Required:
 * - IAM_ENDPOINT - IAM authentication endpoint
 * - API_ENDPOINT - RAID API endpoint
 * - IAM_CLIENT_ID - OAuth client ID
 * - IAM_CLIENT_SECRET - OAuth client secret
 * - RAID_ENV - RAID environment (prod/stage/demo/test)
 * 
 * Optional:
 * - DATA_DIR - Output directory (default: ./src/raw-data)
 * - CONCURRENT_DOI_REQUESTS - Number of parallel DOI requests (default: 5)
 * - DOI_REQUEST_DELAY - Delay between batches in ms (default: 100)
 * - REQUEST_TIMEOUT - HTTP request timeout in ms (default: 30000)
 * - MAX_RETRIES - Maximum retry attempts (default: 3)
 * - ENABLE_CACHING - Enable citation caching (default: false)
 * - CACHING_TIME - Cache validity in ms (default: 5 days)
 * - VERBOSE_LOGGING - Enable detailed logging (default: false)
 * 
 * Usage:
 * $ node fetch-raids.js
 * 
 */

import fs from 'fs/promises';
import path from 'path';
import https from 'https';
import http from 'http';
import {fetchCitation} from './fetch-citation.js'
import {extractHandles} from './fetch-handles.js';
import { addOrcidInfoToRaidData } from './fetch-orcidData.js';
import { addRorDetailsToRaidData } from './fetch-ror.js';
import { addServicePointNameToRaidData } from './fetch-sp.js';
import { fetchEmbargoedRaids } from './fetch-embargoed-raids.js';
import { loadAppConfig } from './loadAppConfig.js';
import { orcidCache, rorCache } from './apiCache.js';
import { TokenManager } from './tokenManager.js';

// Load config from public/app-config.json (falls back to env vars)
const config = loadAppConfig();

// Configure ORCID and ROR caches with the TTL from app-config
if (config.enableCaching) {
  orcidCache.configure({ ttlMs: config.cachingTime });
  rorCache.configure({ ttlMs: config.cachingTime });
}

// Simple in-memory cache for DOI citations
const citationCache = new Map();

// Statistics tracking
const stats = {
  totalRaids: 0,
  totalDois: 0,
  successfulCitations: 0,
  cachedCitations: 0,
  failedCitations: 0,
  startTime: Date.now(),
  totalOrcidIds: 0,
  successfulOrcidNames: 0,
  failedOrcidNames: 0,
  authenticatedOrcids: 0,
  notAuthenticatedOrcids: 0,
  authenticatedButPrivate: 0,
  totalRorIds: 0,
  successfulRorFetches: 0,
  failedRorFetches: 0,
  totalServicePoints: 0,
  successfulServicePointFetches: 0,
  failedServicePointFetches: 0,
};

// Validate required config values
function validateConfig() {
  const required = {
    apiEndpoint: config.apiEndpoint,
    iamEndpoint: config.iamEndpoint,
    iamClientId: config.iamClientId,
    iamClientSecret: config.iamClientSecret,
    raidEnv: config.raidEnv,
  };
  const missing = Object.entries(required).filter(([, v]) => !v).map(([k]) => k);
  if (missing.length > 0) {
    console.error('Error: The following required config values are not set:');
    missing.forEach(key => console.error(`  - ${key}`));
    console.error('Set them in public/app-config.json or as environment variables.');
    console.error('Secrets (iamClientSecret) must always be environment variables.');
    process.exit(1);
  }
}

// Enhanced HTTP request with retry logic
export async function makeRequestWithRetry(url, options = {}, retries = config.maxRetries) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const response = await makeRequest(url, {
        ...options,
        timeout: config.requestTimeout
      });
      return response;
    } catch (error) {
      if (attempt === retries) {
        throw error;
      }
      if (config.verboseLogging) {
        console.log(`  Retry ${attempt}/${retries} for ${url}`);
        console.error(` raids node module - Full error:`, error);
      }
      // Exponential backoff
      await new Promise(resolve => setTimeout(resolve, Math.pow(2, attempt) * 1000));
    }
  }
}

// Make HTTP request (supports both http and https)
function makeRequest(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(url);
    const protocol = parsedUrl.protocol === 'https:' ? https : http;
    
    const timeout = options.timeout || config.requestTimeout;
    const req = protocol.request(url, options, (res) => {
      let data = '';
      
      res.on('data', chunk => {
        data += chunk;
      });
      
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve({ data, headers: res.headers, statusCode: res.statusCode });
        } else if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          // Handle redirects manually
          makeRequest(res.headers.location, options).then(resolve).catch(reject);
        } else {
          reject(new Error(`HTTP ${res.statusCode}: ${data}`));
        }
      });
    });
    
    req.setTimeout(timeout, () => {
      req.destroy();
      reject(new Error(`Request timeout after ${timeout}ms`));
    });
    
    req.on('error', reject);
    
    if (options.body) {
      req.write(options.body);
    }
    
    req.end();
  });
}

// Get bearer token from IAM for a given client
async function getTokenForClient(clientId, clientSecret) {
  const tokenUrl = `${config.iamEndpoint}/realms/raid/protocol/openid-connect/token`;
  const bodyParams = `grant_type=client_credentials&client_id=${encodeURIComponent(clientId)}`;
  const body = clientSecret ? `${bodyParams}&client_secret=${encodeURIComponent(clientSecret)}` : bodyParams;

  const response = await makeRequestWithRetry(tokenUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': Buffer.byteLength(body)
    },
    body
  });

  const tokenData = JSON.parse(response.data);
  if (!tokenData.access_token) {
    throw new Error('No access token in response');
  }
  return tokenData.access_token;
}

// Get bearer token from IAM
async function getBearerToken() {
  console.log('Getting bearer token...');
  console.log(`  iamEndpoint : ${config.iamEndpoint}`);
  console.log(`  iamClientId : ${config.iamClientId}`);

  const tokenUrl = `${config.iamEndpoint}/realms/raid/protocol/openid-connect/token`;
  const body = `grant_type=client_credentials&client_id=${encodeURIComponent(config.iamClientId)}&client_secret=${encodeURIComponent(config.iamClientSecret)}`;
  
  try {
    const response = await makeRequestWithRetry(tokenUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(body)
      },
      body
    });
    
    const tokenData = JSON.parse(response.data);
    
    if (!tokenData.access_token) {
      throw new Error('No access token in response');
    }
    
    console.log('Token acquired successfully.');
    return tokenData.access_token;
  } catch (error) {
    console.error('Failed to get bearer token:', error.message);
    process.exit(1);
  }
}

// Fetch RAID data from API
async function fetchRaidData(bearerToken) {
  console.log('Fetching data from API...');
  
  const url = `${config.apiEndpoint}/raid/all-public`;
  
  try {
    const response = await makeRequestWithRetry(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${bearerToken}`,
        'Content-Type': 'application/json'
      }
    });
    
    return JSON.parse(response.data);
  } catch (error) {
    console.error('Failed to fetch RAID data:', error.message);
    process.exit(1);
  }
}

// Process a batch of DOIs concurrently
async function processDOIBatch(dois) {
  const results = await Promise.all(
    dois.map(async ({ doi, raidIndex, objectIndex }) => {
      const params = {doi, makeRequestWithRetry, stats, citationCache, config}
      const citation = await fetchCitation(params);
      return { citation, raidIndex, objectIndex };
    })
  );
  return results;
}

// Add citations to RAID data with concurrent processing
async function addCitationsToRaidData(raidData) {
  console.log('Fetching citation data for DOIs...');
  
  if (!Array.isArray(raidData)) {
    console.error('Error: RAID data is not an array');
    return raidData;
  }
  
  stats.totalRaids = raidData.length;
  console.log(`Found ${stats.totalRaids} RAID records to process`);
  
  // Collect all DOIs with their positions
  const allDois = [];
  
  raidData.forEach((raid, raidIndex) => {
    if (raid.relatedObject && Array.isArray(raid.relatedObject)) {
      raid.relatedObject.forEach((relatedObj, objectIndex) => {
        if (relatedObj.id && relatedObj.id.includes('10.')) {
          allDois.push({
            doi: relatedObj.id,
            raidIndex,
            objectIndex
          });
          stats.totalDois++;
        }
      });
    }
  });
  
  console.log(`Found ${stats.totalDois} DOIs to process`);
  
  // Process DOIs in batches
  const batchSize = config.concurrentDOIRequests;
  let processedCount = 0;
  
  for (let i = 0; i < allDois.length; i += batchSize) {
    const batch = allDois.slice(i, i + batchSize);
    if (config.verboseLogging) {
      console.log(`Processing DOI batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(allDois.length / batchSize)}`);
    }
    
    const results = await processDOIBatch(batch);

    // Apply results to RAID data
    results.forEach(({ citation, raidIndex, objectIndex }) => {
      if (citation) {
        raidData[raidIndex].relatedObject[objectIndex].citation = {
          text: citation
        };
      }
    });
    
    processedCount += batch.length;
    
    // Progress indicator
    const progress = Math.round((processedCount / allDois.length) * 100);
    process.stdout.write(`\rProgress: ${progress}% (${processedCount}/${allDois.length} DOIs)`);
    
    // Rate limiting delay between batches
    if (i + batchSize < allDois.length) {
      await new Promise(resolve => setTimeout(resolve, config.doiRequestDelay));
    }
  }
  
  console.log('\n'); // New line after progress indicator
  
  return raidData;
}

async function loadCache() {
  if (config.enableCaching) {
    const cacheFile = path.join(config.dataDir, '.citation-cache.json');
    try {
      const cacheData = await fs.readFile(cacheFile, 'utf8');
      const cache = JSON.parse(cacheData);
      let skipped = 0;
      Object.entries(cache).forEach(([doi, entry]) => {
        const text = (typeof entry === 'object' ? entry.citation : String(entry)).trimStart();
        if (text.startsWith('<!DOCTYPE') || text.startsWith('<html')) {
          skipped++;
        } else {
          citationCache.set(doi, entry);
        }
      });
      if (skipped > 0) {
        await fs.writeFile(cacheFile, JSON.stringify(Object.fromEntries(citationCache), null, 2));
      }
      console.log(`Citation cache loaded (${citationCache.size} entries${skipped > 0 ? `, ${skipped} bad HTML entries purged` : ''})`);
    } catch (error) {
      // Cache file doesn't exist or is invalid, start fresh
    }

    const orcidLoaded = await orcidCache.loadFromFile(path.join(config.dataDir, '.orcid-cache.json'));
    if (orcidLoaded > 0) console.log(`ORCID cache loaded (${orcidLoaded} entries)`);

    const rorLoaded = await rorCache.loadFromFile(path.join(config.dataDir, '.ror-cache.json'));
    if (rorLoaded > 0) console.log(`ROR cache loaded (${rorLoaded} entries)`);
  }
}

// Load cache from file (if enabled and exists)
// Save cache to file (if enabled)
async function saveCache() {
  if (config.enableCaching) {
    if (citationCache.size > 0) {
      const cacheFile = path.join(config.dataDir, '.citation-cache.json');
      const cacheData = Object.fromEntries(citationCache);
      await fs.writeFile(cacheFile, JSON.stringify(cacheData, null, 2));
      console.log(`Citation cache saved (${citationCache.size} entries)`);
    }

    if (orcidCache.size > 0) {
      await orcidCache.saveToFile(path.join(config.dataDir, '.orcid-cache.json'));
      console.log(`ORCID cache saved (${orcidCache.size} entries)`);
    }

    if (rorCache.size > 0) {
      await rorCache.saveToFile(path.join(config.dataDir, '.ror-cache.json'));
      console.log(`ROR cache saved (${rorCache.size} entries)`);
    }
  }
}
// Main execution
async function main() {
  try {
    // Validate configuration
    validateConfig();
    
    // Create data directory if it doesn't exist
    await fs.mkdir(config.dataDir, { recursive: true });
    console.log(`Data directory ready: ${config.dataDir}`);
    
    // Load cache if enabled
    await loadCache();
    
    // Step 1: Get bearer token
    const mainTokenManager = new TokenManager({
      iamEndpoint: config.iamEndpoint,
      clientId: config.iamClientId,
      clientSecret: config.iamClientSecret,
      makeRequestWithRetry,
    });
    config.bearerToken = await mainTokenManager.getValidToken();
    // Step 2: Fetch RAID data
    const raidData = await fetchRaidData(config.bearerToken);
    
    // Step 3: Add citations to RAID data
    const enrichedData = await addCitationsToRaidData(raidData);
    // Step 4: Add ORCID info to RAID data
    const enrichedWithOrcid = await addOrcidInfoToRaidData(
      enrichedData, 
      makeRequestWithRetry, 
      config, 
      stats
    );

    // Step 5: Add ROR details to RAID data (NEW)
    const enrichedWithRor = await addRorDetailsToRaidData(
      enrichedWithOrcid,
      makeRequestWithRetry,
      config,
      stats
    );

    // Step 6: Add service point names to RAID data  <-- NEW
    config.bearerToken = await mainTokenManager.getValidToken();
    const enrichedWithServicePoint = await addServicePointNameToRaidData(
      enrichedWithRor,
      makeRequestWithRetry,
      config,
      stats
    );

    // Step 7: Save enriched RAID data with ORCID info
    const outputFile = path.join(config.dataDir, 'raids.json');
    await fs.writeFile(outputFile, JSON.stringify(enrichedWithServicePoint, null, 2));
    console.log(`Data successfully saved to ${outputFile}`);

    // Step 7b: Fetch and save embargoed RAiD summaries
    const dumperTokenManager = new TokenManager({
      iamEndpoint: config.iamEndpoint,
      clientId: config.raidDumperClientId,
      clientSecret: config.raidDumperClientSecret,
      makeRequestWithRetry,
    });
    const dumperToken = await dumperTokenManager.getValidToken();
    const embargoedRaids = await fetchEmbargoedRaids(dumperToken, config, makeRequestWithRetry);
    const embargoedOutputFile = path.join(config.dataDir, 'embargoed-raids.json');
    await fs.writeFile(embargoedOutputFile, JSON.stringify(embargoedRaids, null, 2));
    console.log(`Embargoed RAiD data saved to ${embargoedOutputFile}`);
    
    // Step 8: Extract and save handles
    const handles = await extractHandles(raidData);
    const handlesFile = path.join(config.dataDir, 'handles.json');
    await fs.writeFile(handlesFile, JSON.stringify(handles, null, 2));
    console.log(`Unique handles saved to ${handlesFile}`);
    // Save cache for next run
    await saveCache();
    
    // Summary
    const elapsedTime = ((Date.now() - stats.startTime) / 1000).toFixed(2);
    console.log('\nSummary:');
    console.log(`- Total RAIDs processed: ${stats.totalRaids}`);
    console.log(`- Total DOIs found: ${stats.totalDois}`);
    console.log(`- Successful citations fetched: ${stats.successfulCitations}`);
    console.log(`- Failed citations: ${stats.failedCitations}`);
    if (config.enableCaching) {
      console.log(`- Cached citations used: ${stats.cachedCitations}`);
    }
    console.log(`- Total ORCID IDs found: ${stats.totalOrcidIds}`);
    console.log(`- Authenticated & Public ORCIDs: ${stats.authenticatedOrcids}`);
    console.log(`- Authenticated but Private: ${stats.authenticatedButPrivate}`);
    console.log(`- Not Authenticated: ${stats.notAuthenticatedOrcids}`);
    console.log(`- Failed ORCID lookups: ${stats.failedOrcidNames}`);
    console.log(`- Total unique service points: ${stats.totalServicePoints}`);
    console.log(`- Successful service point lookups: ${stats.successfulServicePointFetches}`);
    console.log(`- Failed service point lookups: ${stats.failedServicePointFetches}`);
    console.log(`- Total embargoed RAiDs: ${embargoedRaids.length}`);
    console.log(`- Total handles: ${handles.length}`);
    console.log(`- Execution time: ${elapsedTime} seconds`);
    
  } catch (error) {
    console.error('Error:', error.message);
    if (config.verboseLogging) {
      console.error(error.stack);
    }
    process.exit(1);
  }
}

// Run the script if executed directly
main();

// Export functions for use as a module
export {
  getBearerToken,
  fetchRaidData,
  addCitationsToRaidData,
  fetchCitation,
  addOrcidInfoToRaidData,
  addRorDetailsToRaidData,
  addServicePointNameToRaidData,
  fetchEmbargoedRaids,
};