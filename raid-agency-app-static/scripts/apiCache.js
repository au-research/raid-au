/**
 * API Response Cache Utility
 * 
 * Provides in-memory caching for ORCID and ROR API responses
 * to reduce redundant API calls and improve performance.
 */

class ApiCache {
  constructor() {
    this.cache = new Map();
    this.stats = {
      hits: 0,
      misses: 0,
      sets: 0
    };
  }

  /**
   * Generate cache key from URL or ID
   */
  generateKey(prefix, id) {
    return `${prefix}:${id}`;
  }

  /**
   * Get cached value
   */
  get(key) {
    if (this.cache.has(key)) {
      this.stats.hits++;
      return this.cache.get(key);
    }
    this.stats.misses++;
    return null;
  }

  /**
   * Set cached value
   */
  set(key, value) {
    this.cache.set(key, value);
    this.stats.sets++;
  }

  /**
   * Check if key exists
   */
  has(key) {
    return this.cache.has(key);
  }

  /**
   * Clear all cache
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
}

// Singleton instances
const orcidCache = new ApiCache();
const rorCache = new ApiCache();

export { orcidCache, rorCache };
