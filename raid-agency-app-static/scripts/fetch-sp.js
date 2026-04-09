/**
 * Service Point Name Fetcher
 *
 * Fetches all service points in a single API call and adds the name
 * to the identifier.owner object of each RAID record.
 *
 * API: GET <apiEndpoint>/service-point
 */

/**
 * Fetch all service points and return a Map of id -> name.
 *
 * @param {Object} params
 * @param {Function} params.makeRequestWithRetry - Shared HTTP helper
 * @param {Object} params.config - Global config (apiEndpoint, bearerToken, etc.)
 * @returns {Promise<Map<string, string>>} Map of servicePointId -> name
 */
async function fetchAllServicePoints({ makeRequestWithRetry, config }) {
  const url = `${config.apiEndpoint}/service-point`;

  const response = await makeRequestWithRetry(url, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${config.bearerToken}`,
    },
  });

  const data = JSON.parse(response.data);
  const servicePointMap = new Map();

  for (const sp of data) {
    if (sp.id && sp.name) {
      servicePointMap.set(String(sp.id), sp.name);
    }
  }

  return servicePointMap;
}

/**
 * Enrich RAID data by adding the service point name to identifier.owner.
 *
 * Fetches all service points in one request, then applies names to
 * every RAID record.
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

  try {
    const servicePointMap = await fetchAllServicePoints({
      makeRequestWithRetry,
      config,
    });

    stats.totalServicePoints = servicePointMap.size;
    stats.successfulServicePointFetches = servicePointMap.size;
    console.log(`Fetched ${servicePointMap.size} service points`);

    // Apply names to every RAID record
    raidData.forEach((raid) => {
      const servicePointId = raid?.identifier?.owner?.servicePoint;
      if (servicePointId && servicePointMap.has(String(servicePointId))) {
        raid.identifier.owner.servicePointName = servicePointMap.get(
          String(servicePointId)
        );
      }
    });
  } catch (error) {
    stats.failedServicePointFetches++;
    console.error(`Failed to fetch service points: ${error.message}`);
  }

  console.log(
    `Service point enrichment complete: ${stats.successfulServicePointFetches} succeeded, ${stats.failedServicePointFetches} failed`
  );

  return raidData;
}
