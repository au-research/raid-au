import fs from 'fs/promises';
import path from 'path';
import https from 'https';

/**
 * Make an HTTPS GET request and return the response
 * @param {string} url - The URL to fetch
 * @returns {Promise<any>} Parsed JSON response
 */
async function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      
      res.on('data', (chunk) => {
        data += chunk;
      });
      
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            const parsed = JSON.parse(data);
            resolve(parsed);
          } catch (error) {
            reject(new Error(`Invalid JSON from ${url}: ${error.message}`));
          }
        } else {
          reject(new Error(`HTTP ${res.statusCode} from ${url}`));
        }
      });
    }).on('error', reject);
  });
}

/**
 * Fetch handles from a specific backend
 * @param {string} backend - Backend name (prod, stage, demo, test)
 * @param {string} url - URL to fetch handles from
 * @returns {Promise<Array>} Array of [handle, backend] pairs
 */
async function fetchBackend(backend, url) {
  console.log(`Fetching data from ${backend}: ${url}`);
  
  try {
    const response = await fetchJson(url);
    
    // Verify we got an array
    if (!Array.isArray(response)) {
      console.warn(`Warning: Response from ${backend} is not an array`);
      return [];
    }
    
    // Transform handles into [handle, backend] pairs
    const pairs = response
      .filter(handle => handle && typeof handle === 'string')
      .map(handle => [handle, backend]);
    
    console.log(`Fetched ${pairs.length} handles from ${backend}`);
    return pairs;
    
  } catch (error) {
    console.warn(`Warning: Failed to fetch from ${backend}: ${error.message}`);
    return [];
  }
}

/**
 * Fetch handles from all backends and combine them
 * @param {Object} options - Configuration options
 * @param {string} options.outputFile - Path to save the combined handles
 * @param {Object} options.backends - Backend URLs (optional)
 * @returns {Promise<Array>} Combined array of [handle, backend] pairs
 */
async function fetchAllHandles(options = {}) {
  const {
    outputFile = 'src/raw-data/all-handles.json',
    backends = {
      prod: 'https://static.prod.raid.org.au/api/handles.json',
      stage: 'https://static.stage.raid.org.au/api/handles.json',
      demo: 'https://static.demo.raid.org.au/api/handles.json',
      test: 'https://static.test.raid.org.au/api/handles.json'
    }
  } = options;
  
  console.log('Starting to fetch handles from all backends...');
  
  // Fetch from all backends in parallel
  const backendPromises = Object.entries(backends).map(([backend, url]) => 
    fetchBackend(backend, url)
  );
  
  const results = await Promise.all(backendPromises);
  
  // Combine all results into a single array
  const combinedHandles = results.flat();
  
  // Ensure output directory exists
  const outputDir = path.dirname(outputFile);
  await fs.mkdir(outputDir, { recursive: true });
  
  // Save to file
  await fs.writeFile(outputFile, JSON.stringify(combinedHandles, null, 2));
  
  const fileStats = await fs.stat(outputFile);
  console.log(`Combined handles saved to ${outputFile}`);
  console.log(`Final file size: ${fileStats.size} bytes`);
  console.log(`Total handles: ${combinedHandles.length}`);
  
  // Log summary by backend
  const summary = combinedHandles.reduce((acc, [_, backend]) => {
    acc[backend] = (acc[backend] || 0) + 1;
    return acc;
  }, {});
  
  console.log('\nSummary by backend:');
  Object.entries(summary).forEach(([backend, count]) => {
    console.log(`  ${backend}: ${count} handles`);
  });
  
  return combinedHandles;
}

/**
 * Fetch handles from all backends with custom configuration
 * @param {Object} config - Custom configuration
 * @returns {Promise<Array>} Combined handles array
 */
async function fetchAllHandlesWithConfig(config) {
  return fetchAllHandles(config);
}

// If running directly, execute with defaults
if (import.meta.url === `file://${process.argv[1]}`) {
  fetchAllHandles().catch(error => {
    console.error('Error:', error.message);
    process.exit(1);
  });
}

async function extractHandles(raidData) {
  const handles = new Set();

  if (Array.isArray(raidData)) {
    raidData.forEach(raid => {
      if (raid.identifier?.id) {
        const id = raid.identifier.id;
        // Try to match the sed pattern exactly: http://[^/]*/([^/]+/[^/]+).*
        const match = id.match(/^http:\/\/[^\/]+\/([^\/]+\/[^\/]+)/);

        if (match && match[1]) {
          // If it matches the pattern, use the captured group
          handles.add(match[1]);
        } else {
          // If it doesn't match, use the original value (like sed does)
          handles.add(id);
        }
      }
    });
  }

  return Array.from(handles).sort();
}
// Export for use as a module
export { fetchAllHandles, fetchAllHandlesWithConfig, fetchBackend, extractHandles };

// Also export as default
export default fetchAllHandles;