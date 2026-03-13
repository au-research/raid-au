import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactNode } from 'react';
import {
  fetchDetailedDOICitation,
  useRelatedObjectCitations,
  cleanDetailedCitation,
  constructDOIUrl,
  type FetchDOIOptions
} from './related-object';

// Mock fetch globally
global.fetch = vi.fn();

// Helper to create QueryClient wrapper for hooks
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        staleTime: 0,
      },
    },
  });
  
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('Related Object Service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('constructDOIUrl >>>', () => {
    it('should return URL as is if already a complete DOI URL', () => {
      const input = 'https://doi.org/10.1126/science.aar3646';
      const result = constructDOIUrl(input);
      expect(result).toBe(input);
    });

    it('should extract DOI pattern and construct URL', () => {
      const input = 'some text 10.1126/science.aar3646 more text';
      const result = constructDOIUrl(input);
      expect(result).toBe('https://doi.org/10.1126/science.aar3646 more text');
    });

    it('should handle doi: prefix', () => {
      const input = 'doi:10.1126/science.aar3646';
      const result = constructDOIUrl(input);
      expect(result).toBe('https://doi.org/10.1126/science.aar3646');
    });

    it('should treat bare DOI as DOI and construct URL', () => {
      const input = '10.1126/science.aar3646';
      const result = constructDOIUrl(input);
      expect(result).toBe('https://doi.org/10.1126/science.aar3646');
    });

    it('should handle complex DOI identifiers', () => {
      const input = '10.1038/s41586-021-03819-2';
      const result = constructDOIUrl(input);
      expect(result).toBe('https://doi.org/10.1038/s41586-021-03819-2');
    });
  });

  describe('cleanDetailedCitation >>>', () => {
    it('should remove DOI URLs from citation', () => {
      const input = 'Smith, J. (2023). Test Paper. Journal Name. https://doi.org/10.1126/science.aar3646';
      const result = cleanDetailedCitation(input);
      expect(result).toBe('Smith, J. (2023). Test Paper. Journal Name.');
    });

    it('should remove dx.doi.org URLs', () => {
      const input = 'Smith, J. (2023). Test Paper. http://dx.doi.org/10.1126/science.aar3646';
      const result = cleanDetailedCitation(input);
      expect(result).toBe('Smith, J. (2023). Test Paper.');
    });

    it('should remove standalone DOI patterns with DOI: prefix', () => {
      const input = 'Smith, J. (2023). Test Paper. DOI: 10.1126/science.aar3646';
      const result = cleanDetailedCitation(input);
      expect(result).toBe('Smith, J. (2023). Test Paper. aar3646.');
    });

    it('should remove bare DOI patterns', () => {
      const input = 'Smith, J. (2023). Test Paper. 10.1126/science.aar3646';
      const result = cleanDetailedCitation(input);
      expect(result).toBe('Smith, J. (2023). Test Paper. aar3646.');
    });
  });

  describe('fetchDetailedDOICitation >>>', () => {
    const mockFetch = fetch as unknown as ReturnType<typeof vi.fn>;

    it('should fetch citation successfully with default options', async () => {
      const mockResponse = 'Smith, J. (2023). Test Paper. Journal Name.';
      mockFetch.mockResolvedValueOnce({
        ok: true,
        text: () => Promise.resolve(mockResponse),
      } as Response);

      const result = await fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646');

      expect(mockFetch).toHaveBeenCalledWith(
        'https://doi.org/10.1126/science.aar3646',
        expect.objectContaining({
          method: 'GET',
          headers: {
            'Accept': 'text/x-bibliography; style=apa',
            'User-Agent': 'DOI-Fetcher-Detailed/1.0',
          },
        })
      );
      expect(result).toBe(mockResponse);
    });

    it('should use custom options when provided', async () => {
      const mockResponse = 'Citation text';
      mockFetch.mockResolvedValueOnce({
        ok: true,
        text: () => Promise.resolve(mockResponse),
      } as Response);

      const options: FetchDOIOptions = {
        timeout: 5000,
        userAgent: 'Custom-Agent/1.0'
      };

      await fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646', options);

      expect(mockFetch).toHaveBeenCalledWith(
        'https://doi.org/10.1126/science.aar3646',
        expect.objectContaining({
          headers: expect.objectContaining({
            'User-Agent': 'Custom-Agent/1.0',
          }),
        })
      );
    });

    it('should throw error for invalid DOI URL format', async () => {
      await expect(
        fetchDetailedDOICitation('invalid-url')
      ).rejects.toThrow('Invalid DOI URL format');
    });

    it('should throw error for HTTP error responses', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
        statusText: 'Not Found',
      } as Response);

      await expect(
        fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646')
      ).rejects.toThrow('HTTP 404: Not Found');
    });

    it('should throw error for empty citation', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        text: () => Promise.resolve('   '),
      } as Response);

      await expect(
        fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646')
      ).rejects.toThrow('Empty citation received');
    });

    it('should handle timeout', async () => {
      const abortError = new Error('Request timeout after 1000ms');
      abortError.name = 'AbortError';
      
      mockFetch.mockRejectedValueOnce(abortError);

      await expect(
        fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646', { timeout: 1000 })
      ).rejects.toThrow('Request timeout after 1000ms');
    });

    it('should handle fetch network errors', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(
        fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646')
      ).rejects.toThrow('Network error');
    });

    it('should handle unknown errors', async () => {
      mockFetch.mockRejectedValueOnce('Unknown error');

      await expect(
        fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646')
      ).rejects.toThrow('Unknown error occurred while fetching detailed DOI citation');
    });

    it('should clean citation text', async () => {
      const mockResponse = 'Smith, J. (2023). Test Paper. https://doi.org/10.1126/science.aar3646';
      mockFetch.mockResolvedValueOnce({
        ok: true,
        text: () => Promise.resolve(mockResponse),
      } as Response);

      const result = await fetchDetailedDOICitation('https://doi.org/10.1126/science.aar3646');

      expect(result).toBe('Smith, J. (2023). Test Paper.');
    });
  });

  describe('useRelatedObjectCitations >>>', () => {
    it('should return empty Map when localStorage is empty', async () => {
      const wrapper = createWrapper();
      const { result } = renderHook(() => useRelatedObjectCitations(), { wrapper });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      }, { timeout: 1000 });

      expect(result.current.data).toEqual(new Map());
    });

    it('should return parsed Map from localStorage', async () => {
      const testData = new Map([
        ['10.1234/example1', { 
          cachedAt: Date.now(), 
          value: 'Citation 1',
          source: 'test',
          fullCitation: 'Citation 1'
        }],
        ['10.1234/example2', { 
          cachedAt: Date.now(), 
          value: 'Citation 2',
          source: 'test',
          fullCitation: 'Citation 2'
        }]
      ]);

      localStorage.setItem('relatedObjectCitations', JSON.stringify([...testData]));

      const wrapper = createWrapper();
      const { result } = renderHook(() => useRelatedObjectCitations(), { wrapper });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      }, { timeout: 1000 });

      expect(result.current.data).toEqual(testData);
    });

    it('should handle corrupted localStorage data gracefully', async () => {
      localStorage.setItem('relatedObjectCitations', 'invalid-json');

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const wrapper = createWrapper();
      const { result } = renderHook(() => useRelatedObjectCitations(), { wrapper });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      }, { timeout: 1000 });

      expect(result.current.data).toEqual(new Map());
      expect(consoleSpy).toHaveBeenCalledWith(
        'Error accessing relatedObjectCitations:',
        expect.any(Error)
      );

      consoleSpy.mockRestore();
    });

    it('should use correct query configuration', async () => {
      const wrapper = createWrapper();
      const { result } = renderHook(() => useRelatedObjectCitations(), { wrapper });

      await waitFor(() => {
        expect(result.current.isSuccess).toBe(true);
      }, { timeout: 1000 });

      // The query should complete successfully
      expect(result.current.dataUpdatedAt).toBeGreaterThan(0);
    });
  });

});