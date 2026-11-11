/**
 * Embargoed RAiD Fetcher
 *
 * Fetches embargoed RAiD records from /raid/all-embargoed and produces a
 * minimal summary for each record containing only the fields needed for
 * the public embargoed-raids listing.
 *
 * Per record we resolve:
 *   - RAiD handle (identifier.id)
 *   - Embargoed status label (derived from access.type.id)
 *   - Embargo expiry date (access.embargoExpiry)
 *   - Access statement (access.statement.text)
 *   - Registration agency ROR + name (identifier.registrationAgency.id → ROR API)
 *   - Service point name (identifier.owner.servicePoint → /service-point/)
 *   - Owning organisation ROR + name (identifier.owner.id → ROR API)
 *
 * Output: src/raw-data/embargoed-raids.json
 */

import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';
import https from 'https';
import http from 'http';
import { fetchRorDetails } from './fetch-ror.js';
import { fetchAllServicePoints } from './fetch-sp.js';
import { loadAppConfig } from './loadAppConfig.js';
import { TokenManager } from './tokenManager.js';

// Load config from public/app-config.json (falls back to env vars)
const config = loadAppConfig();

function makeRequest(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(url);
    const protocol = parsedUrl.protocol === 'https:' ? https : http;
    const timeout = options.timeout || config.requestTimeout;
    const req = protocol.request(url, options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve({ data, headers: res.headers, statusCode: res.statusCode });
        } else if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          makeRequest(res.headers.location, options).then(resolve).catch(reject);
        } else {
          reject(new Error(`HTTP ${res.statusCode}: ${data}`));
        }
      });
    });
    req.setTimeout(timeout, () => { req.destroy(); reject(new Error(`Request timeout after ${timeout}ms`)); });
    req.on('error', reject);
    if (options.body) req.write(options.body);
    req.end();
  });
}

async function makeRequestWithRetry(url, options = {}, retries = config.maxRetries) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      return await makeRequest(url, { ...options, timeout: config.requestTimeout });
    } catch (error) {
      if (attempt === retries) throw error;
      if (config.verboseLogging) {
        console.log(`  Retry ${attempt}/${retries} for ${url}`);
        console.error('Full error:', error);
      }
      await new Promise((resolve) => setTimeout(resolve, Math.pow(2, attempt) * 1000));
    }
  }
}

async function main() {
  const requiredFields = { iamEndpoint: config.iamEndpoint, apiEndpoint: config.apiEndpoint, raidDumperClientId: config.raidDumperClientId };
  const missing = Object.entries(requiredFields).filter(([, v]) => !v).map(([k]) => k);
  if (missing.length > 0) {
    console.error('Missing required config values:', missing.join(', '));
    console.error('Set them in public/app-config.json or as environment variables.');
    process.exit(1);
  }

  await fs.mkdir(config.dataDir, { recursive: true });

  const tokenManager = new TokenManager({
    iamEndpoint: config.iamEndpoint,
    clientId: config.raidDumperClientId,
    clientSecret: config.raidDumperClientSecret,
    makeRequestWithRetry,
  });
  const bearerToken = await tokenManager.getValidToken();
  config.bearerToken = bearerToken;


  const summaries = await fetchEmbargoedRaids(bearerToken, config, makeRequestWithRetry);

  const outputFile = path.join(config.dataDir, 'embargoed-raids.json');
  await fs.writeFile(outputFile, JSON.stringify(summaries, null, 2));
  console.log(`Saved ${summaries.length} embargoed RAiD(s) to ${outputFile}`);
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main().catch((err) => {
    console.error('Error:', err.message);
    process.exit(1);
  });
}

const EMBARGOED_ACCESS_LABEL = 'Embargoed Access';

/**
 * Extract the handle portion from a full RAiD identifier URL.
 * e.g. "https://raid.org/102.100.100/447187" → "102.100.100/447187"
 */
function extractHandle(identifierId) {
  try {
    const url = new URL(identifierId);
    return url.pathname.replace(/^\//, '');
  } catch {
    return identifierId;
  }
}

/**
 * Fetch all embargoed RAiD records from the API.
 */
async function fetchAllEmbargoedRaids(bearerToken, config, makeRequestWithRetry) {
  const url = `${config.apiEndpoint}/raid/all-embargoed`;

  try {
    const response = await makeRequestWithRetry(url, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${bearerToken}`,
        'Content-Type': 'application/json',
      },
    });
    return JSON.parse(response.data);
  } catch (error) {
    console.error(`Failed to fetch embargoed raids: ${error.message}`);
    throw error;
  }
}

/**
 * Build a deduplicated map of ROR ID → name by querying the ROR API for
 * every unique ROR URL found in the embargoed raid records.
 */
async function buildRorNameMap(raids, makeRequestWithRetry, config) {
  const uniqueRorIds = new Set();

  for (const raid of raids) {
    const regAgencyId = raid?.identifier?.registrationAgency?.id;
    const ownerId = raid?.identifier?.owner?.id;
    if (regAgencyId) uniqueRorIds.add(regAgencyId);
    if (ownerId) uniqueRorIds.add(ownerId);
  }

  const rorNameMap = new Map();
  const batchSize = config.concurrentRorRequests || 5;
  const rorIds = [...uniqueRorIds];

  for (let i = 0; i < rorIds.length; i += batchSize) {
    const batch = rorIds.slice(i, i + batchSize);
    await Promise.all(
      batch.map(async (rorId) => {
        const details = await fetchRorDetails(rorId, makeRequestWithRetry, config);
        rorNameMap.set(rorId, details?.name ?? rorId);
      })
    );
    if (i + batchSize < rorIds.length) {
      await new Promise((resolve) =>
        setTimeout(resolve, config.rorRequestDelay || config.doiRequestDelay || 100)
      );
    }
  }

  return rorNameMap;
}

/**
 * Transform the full RaidDto array into minimal embargoed-raid summary objects.
 */
function buildSummaries(raids, rorNameMap, servicePointMap) {
  return raids.map((raid) => {
    const handle = extractHandle(raid?.identifier?.id ?? '');
    const registrationAgencyRor = raid?.identifier?.registrationAgency?.id ?? '';
    const ownerRor = raid?.identifier?.owner?.id ?? '';
    const spId = String(raid?.identifier?.owner?.servicePoint ?? '');

    return {
      handle,
      embargoedStatusLabel: EMBARGOED_ACCESS_LABEL,
      embargoExpiry: raid?.access?.embargoExpiry ?? null,
      accessStatement: raid?.access?.statement?.text ?? null,
      registrationAgencyRor,
      registrationAgencyName: rorNameMap.get(registrationAgencyRor) ?? registrationAgencyRor,
      servicePointName: servicePointMap.get(spId) ?? spId,
      ownerRor,
      ownerName: rorNameMap.get(ownerRor) ?? ownerRor,
    };
  });
}

/**
 * Main export: fetch embargoed raids and return minimal summaries.
 *
 * @param {string} bearerToken
 * @param {Object} config - shared config from fetch-raids.js
 * @param {Function} makeRequestWithRetry - shared HTTP helper
 * @returns {Promise<Array>} array of EmbargoedRaidSummary objects
 */
export async function fetchEmbargoedRaids(bearerToken, config, makeRequestWithRetry) {
  console.log('Fetching embargoed RAiD records...');

  const raids = await fetchAllEmbargoedRaids(bearerToken, config, makeRequestWithRetry);
  console.log(`Found ${raids.length} embargoed RAiD record(s)`);

  if (raids.length === 0) {
    return [];
  }

  const [rorNameMap, servicePointMap] = await Promise.all([
    buildRorNameMap(raids, makeRequestWithRetry, config),
    fetchAllServicePoints({ makeRequestWithRetry, config }),
  ]);

  const summaries = buildSummaries(raids, rorNameMap, servicePointMap);
  console.log(`Embargoed RAiD enrichment complete: ${summaries.length} records processed`);

  return summaries;
}
