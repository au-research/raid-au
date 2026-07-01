/**
 * API Response Cache Utility
 *
 * Provides in-memory caching for ORCID and ROR API responses
 * with optional TTL enforcement and file persistence between builds.
 */

import { readFile, writeFile } from 'fs/promises';

class ApiCache {
  constructor() {
    this.cache = new Map();
    this.ttlMs = 0; // 0 = no expiry (within-build dedup only)
    this.stats = {
      hits: 0,
      misses: 0,
      sets: 0
    };
  }

  /**
   * Set TTL and other runtime options from config.
   * Call this once after loadAppConfig(), before any get/set calls.
   */
  configure({ ttlMs }) {
    this.ttlMs = ttlMs;
  }

  get size() {
    return this.cache.size;
  }

  /**
   * Generate cache key from URL or ID
   */
  generateKey(prefix, id) {
    return `${prefix}:${id}`;
  }

  /**
   * Get cached value. Returns null on miss or if the entry has expired.
   */
  get(key) {
    if (this.cache.has(key)) {
      const entry = this.cache.get(key);
      if (this.ttlMs > 0 && (Date.now() - entry.timestamp) >= this.ttlMs) {
        this.cache.delete(key);
        this.stats.misses++;
        return null;
      }
      this.stats.hits++;
      return entry.value;
    }
    this.stats.misses++;
    return null;
  }

  /**
   * Store a value with a timestamp so TTL can be checked later.
   */
  set(key, value) {
    this.cache.set(key, { value, timestamp: Date.now() });
    this.stats.sets++;
  }

  /**
   * Check if a non-expired entry exists for the key.
   */
  has(key) {
    if (!this.cache.has(key)) return false;
    const entry = this.cache.get(key);
    if (this.ttlMs > 0 && (Date.now() - entry.timestamp) >= this.ttlMs) {
      this.cache.delete(key);
      return false;
    }
    return true;
  }

  /**
   * Clear all cache entries and reset stats.
   */
  clear() {
    this.cache.clear();
    this.stats = {
      hits: 0,
      misses: 0,
      sets: 0
    };
  }

  /**
   * Get cache statistics
   */
  getStats() {
    const total = this.stats.hits + this.stats.misses;
    const hitRate = total > 0 ? ((this.stats.hits / total) * 100).toFixed(2) : 0;

    return {
      ...this.stats,
      size: this.cache.size,
      hitRate: `${hitRate}%`
    };
  }

  /**
   * Print cache statistics
   */
  printStats(label = 'Cache') {
    const stats = this.getStats();
    console.log(`\n${label} Statistics:`);
    console.log(`  Entries: ${stats.size}`);
    console.log(`  Hits: ${stats.hits}`);
    console.log(`  Misses: ${stats.misses}`);
    console.log(`  Hit Rate: ${stats.hitRate}`);
  }

  /**
   * Populate the cache from a JSON file written by saveToFile().
   * Entries older than the configured TTL are skipped.
   * Returns the number of entries loaded.
   */
  async loadFromFile(filePath) {
    try {
      const raw = await readFile(filePath, 'utf-8');
      const data = JSON.parse(raw);
      let loaded = 0;
      for (const [key, entry] of Object.entries(data)) {
        if (!this.ttlMs || (Date.now() - entry.timestamp) < this.ttlMs) {
          this.cache.set(key, entry);
          loaded++;
        }
      }
      return loaded;
    } catch {
      return 0;
    }
  }

  /**
   * Persist the current cache to a JSON file so it can be reloaded
   * on the next build. Returns the number of entries written.
   */
  async saveToFile(filePath) {
    const data = Object.fromEntries(this.cache);
    await writeFile(filePath, JSON.stringify(data, null, 2));
    return this.cache.size;
  }
}

// Singleton instances shared across all fetcher modules
const orcidCache = new ApiCache();
const rorCache = new ApiCache();

export { orcidCache, rorCache };
