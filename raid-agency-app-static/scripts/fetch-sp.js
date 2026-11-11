/**
 * Service Point Name Fetcher
 *
 * Fetches all service points in a single API call and adds the name
 * to the identifier.owner object of each RAID record.
 *
 * API: GET <apiEndpoint>/service-point
 */

import { readFile, writeFile } from 'fs/promises';
import path from 'path';

/**
 * Fetch all service points and return a Map of id -> name.
 *
 * @param {Object} params
 * @param {Function} params.makeRequestWithRetry - Shared HTTP helper
 * @param {Object} params.config - Global config (apiEndpoint, bearerToken, etc.)
 * @returns {Promise<Map<string, string>>} Map of servicePointId -> name
 */
export async function fetchAllServicePoints({ makeRequestWithRetry, config }) {
  const url = `${config.apiEndpoint}/service-point/`;

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

const SP_CACHE_FILE = '.sp-cache.json';

async function loadServicePointCache(config) {
  if (!config.enableCaching) return null;
  try {
    const filePath = path.join(config.dataDir, SP_CACHE_FILE);
    const raw = await readFile(filePath, 'utf-8');
    const { timestamp, entries } = JSON.parse(raw);
    if ((Date.now() - timestamp) < config.cachingTime) {
      return new Map(entries);
    }
  } catch {
    // File missing or expired — fall through to live fetch
  }
  return null;
}

async function saveServicePointCache(servicePointMap, config) {
  if (!config.enableCaching) return;
  const filePath = path.join(config.dataDir, SP_CACHE_FILE);
  await writeFile(filePath, JSON.stringify({
    timestamp: Date.now(),
    entries: Array.from(servicePointMap.entries()),
  }, null, 2));
}

/**
 * Enrich RAID data by adding the service point name to identifier.owner.
 *
 * Loads from cache when caching is enabled and the cache is fresh;
 * otherwise fetches all service points in one request, then applies
 * names to every RAID record.
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
    let servicePointMap = await loadServicePointCache(config);

    if (servicePointMap) {
      console.log(`Service point cache hit (${servicePointMap.size} entries)`);
    } else {
      servicePointMap = await fetchAllServicePoints({ makeRequestWithRetry, config });
      await saveServicePointCache(servicePointMap, config);
    }

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
