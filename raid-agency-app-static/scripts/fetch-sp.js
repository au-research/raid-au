/**
 * Service Point Name Fetcher
 *
 * Fetches service point names from the Service Point API and adds them
 * to the identifier.owner object of each RAID record.
 *
 * API: GET <iamEndpoint>/service-point/<servicePointId>
 *
 * The servicePointId is sourced from identifier.owner.servicePoint in each RAID.
 */

// In-memory cache: servicePointId -> name
const servicePointCache = new Map();

/**
 * Fetch the service point name for a given ID.
 *
 * @param {Object} params
 * @param {number|string} params.servicePointId - The service point ID
 * @param {Function} params.makeRequestWithRetry - Shared HTTP helper
 * @param {Object} params.config - Global config (apiEndpoint, verboseLogging, etc.)
 * @param {Object} params.stats - Shared stats object
 * @returns {Promise<string|null>} The service point name, or null on failure
 */
export async function fetchServicePointName({
  servicePointId,
  makeRequestWithRetry,
  config,
  stats,
}) {
  if (!servicePointId) return null;

  // Check cache first
  if (servicePointCache.has(servicePointId)) {
    if (config.verboseLogging) {
      console.log(`  Cache hit for service point ${servicePointId}`);
    }
    return servicePointCache.get(servicePointId);
  }

  const url = `${config.apiEndpoint}/service-point/${servicePointId}`;
  console.log(`\n  SP request: ${url}`);
  console.log("Service point ID:", config.bearerToken);
  console.log(`  Token present: ${!!config.bearerToken}`);
  try {
    const response = await makeRequestWithRetry(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${config.bearerToken}`,
      },
    });

    const data = JSON.parse(response.data);
    const name = data.name || null;

    if (name) {
      servicePointCache.set(servicePointId, name);
      stats.successfulServicePointFetches++;
      if (config.verboseLogging) {
        console.log(`  Service point ${servicePointId}: ${name}`);
      }
    }

    return name;
  } catch (error) {
    stats.failedServicePointFetches++;
    console.error(`\n  Failed SP ${servicePointId}: ${error.message}`);
    console.error(`  Full error:`, error);
    console.error(
      `\n  Failed to fetch service point ${servicePointId}: ${error.message}`
    );
    return null;
  }
}

/**
 * Enrich RAID data by adding the service point name to identifier.owner.
 *
 * Processes unique service point IDs first (to populate cache), then
 * applies names to every RAID record.
 *
 * @param {Array} raidData - Array of RAID records
 * @param {Function} makeRequestWithRetry - Shared HTTP helper
 * @param {Object} config - Global config
 * @param {Object} stats - Shared stats object
 * @returns {Promise<Array>} The enriched RAID data
 */
export async function addServicePointNameToRaidData(
  raidData,
  makeRequestWithRetry,
  config,
  stats
) {
  console.log('Fetching service point names...');

  if (!Array.isArray(raidData)) {
    console.error('Error: RAID data is not an array');
    return raidData;
  }

  // Collect unique service point IDs
  const uniqueServicePointIds = new Set();

  raidData.forEach((raid) => {
    const servicePointId = raid?.identifier?.owner?.servicePoint;
    if (servicePointId) {
      uniqueServicePointIds.add(servicePointId);
    }
  });

  stats.totalServicePoints = uniqueServicePointIds.size;
  console.log(
    `Found ${uniqueServicePointIds.size} unique service point(s) to look up`
  );

  // Fetch all unique service points (with concurrency batching)
  const ids = [...uniqueServicePointIds];
  const batchSize = config.concurrentServicePointRequests || 5;
  let processedCount = 0;

  for (let i = 0; i < ids.length; i += batchSize) {
    const batch = ids.slice(i, i + batchSize);

    await Promise.all(
      batch.map((servicePointId) =>
        fetchServicePointName({
          servicePointId,
          makeRequestWithRetry,
          config,
          stats,
        })
      )
    );

    processedCount += batch.length;
    const progress = Math.round((processedCount / ids.length) * 100);
    process.stdout.write(
      `\rService points: ${progress}% (${processedCount}/${ids.length})`
    );

    // Rate limiting between batches
    if (i + batchSize < ids.length) {
      const delay = config.servicePointRequestDelay || 100;
      await new Promise((resolve) => setTimeout(resolve, delay));
    }
  }

  console.log(''); // newline after progress

  // Apply cached names to every RAID record
  raidData.forEach((raid) => {
    const servicePointId = raid?.identifier?.owner?.servicePoint;
    if (servicePointId && servicePointCache.has(servicePointId)) {
      raid.identifier.owner.servicePointName =
        servicePointCache.get(servicePointId);
    }
  });

  console.log(
    `Service point enrichment complete: ${stats.successfulServicePointFetches} succeeded, ${stats.failedServicePointFetches} failed`
  );

  return raidData;
}