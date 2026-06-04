/**
 * Shared config loader for data-fetch scripts.
 *
 * Priority for non-secret values:
 *   1. public/app-config.json  (explicit runtime config)
 *   2. environment variables   (backwards-compat / CI overrides)
 *   3. built-in defaults
 *
 * Secrets (IAM_CLIENT_SECRET, RAID_DUMPER_CLIENT_SECRET) are ALWAYS
 * sourced from environment variables only — never from the JSON file.
 *
 * Usage:
 *   import { loadAppConfig } from './loadAppConfig.js';
 *   const config = loadAppConfig();
 */

import { readFileSync } from 'fs';
import { resolve } from 'path';
import { config as dotenvConfig } from 'dotenv';

dotenvConfig();

export function loadAppConfig() {
  let fileConfig = {};
  const configPath = resolve(process.cwd(), 'public', 'app-config.json');

  try {
    const raw = readFileSync(configPath, 'utf-8');
    fileConfig = JSON.parse(raw);
    console.log('[config] Loaded public/app-config.json');
  } catch {
    console.log('[config] public/app-config.json not found — using environment variables');
  }

  return {
    // Non-secret endpoints: file config > env var
    apiEndpoint:        fileConfig.apiEndpoint       ?? process.env.API_ENDPOINT,
    iamEndpoint:        fileConfig.iamEndpoint       ?? process.env.IAM_ENDPOINT,
    iamClientId:        fileConfig.iamClientId       ?? process.env.IAM_CLIENT_ID,
    raidDumperClientId: fileConfig.raidDumperClientId ?? process.env.RAID_DUMPER_CLIENT_ID,
    raidEnv:            fileConfig.raidEnv           ?? process.env.RAID_ENV,
    siteUrl:            fileConfig.siteUrl           ?? process.env.SITE_URL,

    // Secrets: env var ONLY — never stored in the JSON file
    iamClientSecret:        process.env.IAM_CLIENT_SECRET,
    raidDumperClientSecret: process.env.RAID_DUMPER_CLIENT_SECRET,

    // Output path: env var override only
    dataDir: process.env.DATA_DIR ?? './src/raw-data',

    // Performance tuning: env var overrides (useful for CI), then defaults
    concurrentDOIRequests: parseInt(process.env.CONCURRENT_DOI_REQUESTS) || 5,
    doiRequestDelay:       parseInt(process.env.DOI_REQUEST_DELAY)        || 100,
    requestTimeout:        parseInt(process.env.REQUEST_TIMEOUT)          || 30000,
    maxRetries:            parseInt(process.env.MAX_RETRIES)              || 3,
    concurrentRorRequests: parseInt(process.env.CONCURRENT_ROR_REQUESTS) || 5,
    rorRequestDelay:       parseInt(process.env.ROR_REQUEST_DELAY)        || 100,

    // Feature flags: file config > env var > default
    enableCaching: fileConfig.caching?.enabled
      ?? (process.env.ENABLE_CACHING === 'true'),
    cachingTime: fileConfig.caching?.ttlMs
      ?? (parseInt(process.env.CACHING_TIME) || 5 * 24 * 60 * 60 * 1000),

    verboseLogging: process.env.VERBOSE_LOGGING === 'true',

    orcidEnv: fileConfig.raidEnv ?? process.env.RAID_ENV ?? 'prod',
  };
}
