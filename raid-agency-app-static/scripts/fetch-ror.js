/**
 * ROR (Research Organization Registry) Details Fetching Service with Caching
 * 
 * Fetches organization information from ROR API and adds it to RAID data.
 * ROR provides standardized information about research organizations worldwide.
 */

import { rorCache } from './apiCache.js';

// ROR API configuration
const ROR_API_BASE = 'https://api.ror.org/organizations';

/**
 * Extract ROR ID from various formats
 * Handles: https://ror.org/05mmh0f86, ror.org/05mmh0f86, 05mmh0f86
 */
function extractRorId(rorString) {
  if (!rorString) return null;
  
  // Match ROR ID pattern (9 characters: alphanumeric)
  const match = rorString.match(/([0-9a-z]{9})/i);
  return match ? match[1] : null;
}

/**
 * Fetch organization details from ROR API
 */
export async function fetchRorDetails(rorId, makeRequestWithRetry, config) {
  const cleanRorId = extractRorId(rorId);
  
  if (!cleanRorId) {
    if (config.verboseLogging) {
      console.warn(`Invalid ROR ID format: ${rorId}`);
    }
    return null;
  }

  // Check cache first if enabled
  if (config.enableCaching) {
    const cacheKey = rorCache.generateKey('details', cleanRorId);
    const cached = rorCache.get(cacheKey);
    if (cached) {
      return cached;
    }
  }

  const url = `${ROR_API_BASE}/${cleanRorId}`;

  try {
    const response = await makeRequestWithRetry(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    });

    const data = JSON.parse(response.data);
    const result = {
      rorId: cleanRorId,
      name: data.names.find(name => 
        name.types.includes("ror_display")
      )?.value || data.name,
      rorUrl: `https://ror.org/${cleanRorId}`
    };

    // Cache the result if caching is enabled
    if (config.enableCaching) {
      const cacheKey = rorCache.generateKey('details', cleanRorId);
      rorCache.set(cacheKey, result);
    }

    return result;
  } catch (error) {
    if (config.verboseLogging) {
      console.warn(`Failed to fetch ROR data for ${cleanRorId}:`, error.message);
    }

    // Cache null result to avoid repeated failed requests
    if (config.enableCaching) {
      const cacheKey = rorCache.generateKey('details', cleanRorId);
      rorCache.set(cacheKey, null);
    }

    return null;
  }
}

/**
 * Get simplified organization info (for display)
 */
export function getSimplifiedRorInfo(rorDetails) {
  if (!rorDetails) return null;

  return {
    rorId: rorDetails.rorId,
    name: rorDetails.name,
    type: rorDetails.types?.[0] || 'Organization',
    rorUrl: rorDetails.rorUrl
  };
}

/**
 * Process a batch of ROR IDs concurrently
 */
export async function processBatchRorDetails(rorIds, makeRequestWithRetry, config, stats) {
  const results = await Promise.all(
    rorIds.map(async ({ rorId, raidIndex, orgIndex, field }) => {
      const rorDetails = await fetchRorDetails(rorId, makeRequestWithRetry, config);
      
      if (rorDetails) {
        stats.successfulRorFetches++;
      } else {
        stats.failedRorFetches++;
      }
      
      return { rorDetails, raidIndex, orgIndex, field };
    })
  );
  
  return results;
}

/**
 * Add ROR details to RAID data
 * Searches for ROR IDs in various fields (organizations, contributors, etc.)
 */
export async function addRorDetailsToRaidData(raidData, makeRequestWithRetry, config, stats) {
  console.log('Fetching ROR organization details...');

  if (!Array.isArray(raidData)) {
    console.error('Error: RAID data is not an array');
    return raidData;
  }

  // Collect all ROR IDs from various fields
  const allRorIds = [];

  raidData.forEach((raid, raidIndex) => {
    // Check in organization field (if exists)
    if (raid.organisation && Array.isArray(raid.organisation)) {
      raid.organisation.forEach((org, orgIndex) => {
        if (org.id && org.id.includes('ror.org')) {
          allRorIds.push({
            rorId: org.id,
            raidIndex,
            orgIndex,
            field: 'organization'
          });
          stats.totalRorIds++;
        }
      });
    }
  });
  
  console.log(`Found ${stats.totalRorIds} ROR IDs to process`);
  if (config.enableCaching) {
    console.log('ROR caching: ENABLED');
  }

  if (allRorIds.length === 0) {
    console.log('No ROR IDs found in RAID data');
    return raidData;
  }

  // Process ROR IDs in batches
  const batchSize = config.concurrentRorRequests || config.concurrentDOIRequests;
  let processedCount = 0;

  for (let i = 0; i < allRorIds.length; i += batchSize) {
    const batch = allRorIds.slice(i, i + batchSize);
    
    if (config.verboseLogging) {
      console.log(`Processing ROR batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(allRorIds.length / batchSize)}`);
    }

    const results = await processBatchRorDetails(batch, makeRequestWithRetry, config, stats);
    // Apply results to RAID data
    results.forEach(({ rorDetails, raidIndex, orgIndex, field, subIndex }) => {
      if (rorDetails) {
        const simplifiedInfo = getSimplifiedRorInfo(rorDetails);
        
        // Add to appropriate field
        raidData[raidIndex].organisation[orgIndex].rorDetails = simplifiedInfo;
      }
    });

    processedCount += batch.length;

    // Progress indicator
    const progress = Math.round((processedCount / allRorIds.length) * 100);
    process.stdout.write(`\rROR Progress: ${progress}% (${processedCount}/${allRorIds.length})`);

    // Rate limiting delay between batches
    if (i + batchSize < allRorIds.length) {
      await new Promise(resolve => setTimeout(resolve, config.rorRequestDelay || config.doiRequestDelay));
    }
  }

  console.log('\n'); // New line after progress indicator

  // Print cache statistics if caching is enabled
  if (config.enableCaching) {
    rorCache.printStats('ROR');
  }

  return raidData;
}