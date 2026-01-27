/**
 * ORCID Name Fetching Service with Authentication Status
 * 
 * Fetches display names from ORCID, checks visibility settings,
 * and verifies authentication/account status.
 */

// ORCID API configuration
const getOrcidConfig = (env) => {
  const isSandbox = env === 'dev';
  return {
    publicApiUrl: isSandbox 
      ? 'https://pub.sandbox.orcid.org/v3.0'
      : 'https://pub.orcid.org/v3.0',
    baseUrl: isSandbox
      ? 'https://sandbox.orcid.org'
      : 'https://orcid.org'
  };
};

/**
 * Extract ORCID ID from various formats
 * Handles: https://orcid.org/0000-0001-2345-6789, 0000-0001-2345-6789, etc.
 */
function extractOrcidId(orcidString) {
  if (!orcidString) return null;
  
  // Match ORCID ID pattern (0000-0001-2345-6789)
  const match = orcidString.match(/(\d{4}-\d{4}-\d{4}-\d{3}[0-9X])/);
  return match ? match[1] : null;
}

/**
 * Check ORCID authentication and account status
 */
export async function checkOrcidAuthStatus(orcidId, makeRequestWithRetry, config) {
  const cleanOrcidId = extractOrcidId(orcidId);
  
  if (!cleanOrcidId) {
    return {
      exists: false,
      authenticated: false,
      error: 'Invalid ORCID ID format'
    };
  }
  const orcidAPIUrl = orcidId.includes('sandbox') ? 'https://pub.sandbox.orcid.org/v3.0' : 'https://pub.orcid.org/v3.0';
  //const orcidConfig = getOrcidConfig(config.orcidEnv || 'production');
  const url = `${orcidAPIUrl}/${cleanOrcidId}/record`;

  try {
    const response = await makeRequestWithRetry(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    });

    const data = JSON.parse(response.data);
    const history = data.history;

    return {
      exists: true,
      authenticated: true,
      statusCode: response.statusCode,
      claimed: history?.claimed ?? false,
      verified: history?.verified ?? false,
      deactivated: history?.['deactivation-date'] != null,
      creationMethod: history?.['creation-method'],
      lastModified: history?.['last-modified-date']?.value,
      source: history?.source
    };
  } catch (error) {
    // Handle different error statuses
    if (error.message.includes('HTTP 404')) {
      return {
        exists: false,
        authenticated: false,
        error: 'ORCID record not found'
      };
    } else if (error.message.includes('HTTP 409')) {
      return {
        exists: true,
        authenticated: false,
        deactivated: true,
        error: 'ORCID record is deactivated or private'
      };
    } else {
      if (config.verboseLogging) {
        console.warn(`Error checking ORCID status for ${cleanOrcidId}:`, error.message);
      }
      return {
        exists: false,
        authenticated: false,
        error: error.message
      };
    }
  }
}

/**
 * Fetch ORCID person data with retry logic
 */
export async function fetchOrcidPerson(orcidId, makeRequestWithRetry, config) {
  const cleanOrcidId = extractOrcidId(orcidId);
  
  if (!cleanOrcidId) {
    console.warn(`Invalid ORCID ID format: ${orcidId}`);
    return null;
  }

  const orcidConfig = getOrcidConfig(config.orcidEnv || 'production');
  const url = `${orcidConfig.publicApiUrl}/${cleanOrcidId}/person`;

  try {
    const response = await makeRequestWithRetry(url, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    });

    return JSON.parse(response.data);
  } catch (error) {
    if (config.verboseLogging) {
      console.warn(`Failed to fetch ORCID data for ${cleanOrcidId}:`, error.message);
    }
    return null;
  }
}

/**
 * Get display name from ORCID person data
 * Only returns name if visibility is public
 */
export function getPublicDisplayName(personData) {
  if (!personData || !personData.name) {
    return null;
  }

  const name = personData.name;
  // Check overall name visibility
  if (name.visibility !== 'public') {
    return {
      displayName: "Name withheld by Contributor",
      visibility: name.visibility,
      isPublic: false
    };
  }

  // Build from given/family names if public
  const creditName = name['credit-name']?.value;
  const givenName = name['given-names']?.value;
  const familyName = name['family-name']?.value;

  const constructedName = creditName ? `${creditName}` : `${givenName} ${familyName}`.trim();

  if (constructedName) {
    return {
      displayName: constructedName,
      type: 'constructed',
      visibility: 'public',
      isPublic: true,
      visibilityDetails: {
        overall: name.visibility,
        creditName: name['credit-name']?.value,
        givenNames: name['given-names']?.value,
        familyName: name['family-name']?.value
      }
    };
  }

  return {
    displayName: "Name withheld by Contributor",
    visibility: name.visibility,
    isPublic: false,
    visibilityDetails: {
      overall: name.visibility,
      creditName: name['credit-name']?.visibility,
      givenNames: name['given-names']?.visibility,
      familyName: name['family-name']?.visibility
    }
  };
}

/**
 * Fetch complete ORCID information with authentication status
 * Returns null if name is not public or account is not authenticated
 */
export async function fetchOrcidInfo(orcidId, makeRequestWithRetry, config) {
  const cleanOrcidId = extractOrcidId(orcidId);
  
  if (!cleanOrcidId) {
    return null;
  }

  // Check authentication status first
  const authStatus = await checkOrcidAuthStatus(orcidId, makeRequestWithRetry, config);
  
  // If account doesn't exist, is deactivated, or not authenticated, return early
  if (!authStatus.exists || authStatus.deactivated) {
    return {
      orcidId: cleanOrcidId,
      authenticated: false,
      authStatus,
      displayName: null,
      profileUrl: `${orcidId}`,
      reason: authStatus.deactivated ? 'deactivated' : 'not_found'
    };
  }

  // Fetch person data
  const personData = await fetchOrcidPerson(orcidId, makeRequestWithRetry, config);

  if (!personData) {
    return {
      orcidId: cleanOrcidId,
      authenticated: authStatus.authenticated,
      authStatus,
      displayName: null,
      profileUrl: `${orcidId}`,
      reason: 'fetch_failed'
    };
  }

  const nameInfo = getPublicDisplayName(personData);

  // Only return full info if name is public AND account is authenticated
  if (nameInfo && nameInfo.isPublic && authStatus.authenticated) {
    return {
      orcidId: cleanOrcidId,
      authenticated: true,
      authStatus,
      displayName: nameInfo.displayName,
      nameType: nameInfo.type,
      visibility: nameInfo.visibility,
      visibilityDetails: nameInfo.visibilityDetails,
      profileUrl: `${orcidId}`,
      isPublic: true
    };
  }

  // Account is authenticated but name is not public
  return {
    orcidId: cleanOrcidId,
    authenticated: authStatus.authenticated,
    authStatus,
    displayName: null,
    visibility: nameInfo?.visibility || 'unknown',
    visibilityDetails: nameInfo?.visibilityDetails,
    profileUrl: `${orcidId}`,
    isPublic: false,
    reason: 'name_not_public'
  };
}

/**
 * Process a batch of ORCID IDs concurrently
 */
export async function processBatchOrcidInfo(orcidIds, makeRequestWithRetry, config, stats) {
  const results = await Promise.all(
    orcidIds.map(async ({ orcidId, raidIndex, contributorIndex }) => {
      const orcidInfo = await fetchOrcidInfo(orcidId, makeRequestWithRetry, config);
      if (orcidInfo) {
        if (orcidInfo.authenticated && orcidInfo.visibility === 'public') {
          stats.successfulOrcidNames++;
          stats.authenticatedOrcids++;
        } else if (orcidInfo.authenticated && orcidInfo.visibility !== 'public') {
          stats.authenticatedButPrivate++;
        } else if (!orcidInfo.authenticated) {
          stats.notAuthenticatedOrcids++;
        }
      } else {
        stats.failedOrcidNames++;
      }
      
      return { orcidInfo, raidIndex, contributorIndex };
    })
  );
  
  return results;
}

/**
 * Add ORCID information to RAID data
 * Only adds complete info for authenticated accounts with public names
 */
export async function addOrcidInfoToRaidData(raidData, makeRequestWithRetry, config, stats) {

  if (!Array.isArray(raidData)) {
    console.error('Error: RAID data is not an array');
    return raidData;
  }

  // Collect all ORCID IDs from contributors
  const allOrcidIds = [];

  raidData.forEach((raid, raidIndex) => {
    if (raid.contributor && Array.isArray(raid.contributor)) {
      raid.contributor.forEach((contributor, contributorIndex) => {
        // Check for ORCID in contributor.id
        if (contributor.id && typeof contributor.id === 'string' && 
            contributor.id.includes('orcid.org')) {
          allOrcidIds.push({
            orcidId: contributor.id,
            raidIndex,
            contributorIndex
          });
          stats.totalOrcidIds++;
        }
      });
    }
  });

  console.log(`Found ${stats.totalOrcidIds} ORCID IDs to process`);

  if (allOrcidIds.length === 0) {
    console.log('No ORCID IDs found in RAID data');
    return raidData;
  }

  // Process ORCID IDs in batches
  const batchSize = config.concurrentOrcidRequests || config.concurrentDOIRequests;
  let processedCount = 0;

  for (let i = 0; i < allOrcidIds.length; i += batchSize) {
    const batch = allOrcidIds.slice(i, i + batchSize);
    
    if (config.verboseLogging) {
      console.log(`Processing ORCID batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(allOrcidIds.length / batchSize)}`);
    }

    const results = await processBatchOrcidInfo(batch, makeRequestWithRetry, config, stats);
    // Apply results to RAID data
    results.forEach(({ orcidInfo, raidIndex, contributorIndex }) => {
      if (orcidInfo) {
        // Only add full info if authenticated and public
        if (orcidInfo.authenticated && orcidInfo.isPublic) {
          raidData[raidIndex].contributor[contributorIndex].orcidInfo = {
            orcidId: orcidInfo.orcidId,
            displayName: orcidInfo.displayName,
            nameType: orcidInfo.nameType,
            visibility: orcidInfo.visibility,
            authenticated: orcidInfo.authenticated,
            claimed: orcidInfo.authStatus.claimed,
            verified: orcidInfo.authStatus.verified,
            profileUrl: orcidInfo.profileUrl
          };
        } else {
          // Add minimal info for non-public/non-authenticated accounts
          raidData[raidIndex].contributor[contributorIndex].orcidInfo = {
            orcidId: orcidInfo.orcidId,
            authenticated: orcidInfo.authenticated,
            isPublic: orcidInfo.isPublic || false,
            reason: orcidInfo.reason,
            profileUrl: orcidInfo.profileUrl
          };
        }
      }
    });

    processedCount += batch.length;

    // Progress indicator
    const progress = Math.round((processedCount / allOrcidIds.length) * 100);
    process.stdout.write(`\rORCID Progress: ${progress}% (${processedCount}/${allOrcidIds.length})`);

    // Rate limiting delay between batches
    if (i + batchSize < allOrcidIds.length) {
      await new Promise(resolve => setTimeout(resolve, config.orcidRequestDelay || config.doiRequestDelay));
    }
  }

  console.log('\n'); // New line after progress indicator

  return raidData;
}
