import React, { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  IconButton,
  InputBase,
  Divider,
  FormHelperText,
  Popover,
  Card,
  CardContent,
  Avatar,
  Chip,
  Link
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import FingerprintIcon from '@mui/icons-material/Fingerprint';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { CircleCheckBig, ScanSearch } from 'lucide-react';
import { ClipLoader } from 'react-spinners';
import { useQueryClient } from '@tanstack/react-query';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import PulseLoader from "react-spinners/PulseLoader";
import { CustomStyledTooltip } from '@/components/tooltips/StyledTooltip';

// localStorage keys
const ORCID_CACHE_KEY = 'orcidCache';
const CACHE_EXPIRY_MS = 1000 * 60 * 60 * 24 * 7; // 7 days

// localStorage utility functions for ORCID lookups only
type OrcidCacheEntry = {
  data: unknown;
  value: string;
  cachedAt: number;
  timestamp: number;
  expiresAt: number;
  source: string;
};
const env = import.meta.env.VITE_RAIDO_ENV === 'prod' ? '' : 'demo.';
const replaceText = import.meta.env.VITE_RAIDO_ENV === 'prod' ? 'https://orcid.org/' : 'https://sandbox.orcid.org/';
const orcidLookupCache = {
  // Get data from localStorage
  get: <T,>(orcidId: string): T | null => {
    try {
      const cacheData = localStorage.getItem(ORCID_CACHE_KEY);
      if (!cacheData) return null;

      // Parse as Map structure (typed)
      const cache = new Map<string, OrcidCacheEntry>(JSON.parse(cacheData) as [string, OrcidCacheEntry][]);
      const normalizedId = normalizeOrcidId(orcidId);
      const entry = cache.get(normalizedId);

      if (!entry) return null;

      // Check if cache has expired
      if (Date.now() > entry.expiresAt) {
        // Remove expired entry
        cache.delete(normalizedId);
        localStorage.setItem(ORCID_CACHE_KEY, JSON.stringify(Array.from(cache)));
        return null;
      }

      return entry.data as T;
    } catch (error) {
      console.error('Error reading from localStorage:', error);
      return null;
    }
  },

  // Set data in localStorage
  set: <T,>(orcidId: string, data: T, displayName: string): void => {
    try {
      const cacheData = localStorage.getItem(ORCID_CACHE_KEY);
      const cache = cacheData
        ? new Map<string, OrcidCacheEntry>(JSON.parse(cacheData) as [string, OrcidCacheEntry][])
        : new Map<string, OrcidCacheEntry>();
      const normalizedId = normalizeOrcidId(orcidId);

      cache.set(normalizedId, {
        data,
        value: displayName, // Add display name for cache manager
        cachedAt: Date.now(),
        timestamp: Date.now(),
        expiresAt: Date.now() + CACHE_EXPIRY_MS,
        source: 'ORCID Lookup',
      });

      localStorage.setItem(ORCID_CACHE_KEY, JSON.stringify(Array.from(cache)));
    } catch (error) {
      console.error('Error writing to localStorage:', error);
    }
  },

  // Clear expired entries
  cleanup: (): void => {
    try {
      const cacheData = localStorage.getItem(ORCID_CACHE_KEY);
      if (!cacheData) return;

      const cache = new Map<string, OrcidCacheEntry>(JSON.parse(cacheData) as [string, OrcidCacheEntry][]);
      const now = Date.now();
      let hasChanges = false;

      cache.forEach((entry, orcidId) => {
        if (entry.expiresAt < now) {
          cache.delete(orcidId);
          hasChanges = true;
        }
      });

      if (hasChanges) {
        localStorage.setItem(ORCID_CACHE_KEY, JSON.stringify(Array.from(cache)));
      }
    } catch (error) {
      console.error('Error cleaning up localStorage:', error);
    }
  },
  // Clear all cache
  clear: (): void => {
    try {
      localStorage.removeItem(ORCID_CACHE_KEY);
    } catch (error) {
      console.error('Error clearing cache:', error);
    }
  }
};

// Helper to normalize ORCID ID
const normalizeOrcidId = (orcid: string): string => {
  return orcid.trim().replace(replaceText, '');
};

// JSONP helper function
const fetchJSONP = (url: string) => {
  return new Promise((resolve, reject) => {
    const uniqueCallback = `jsonp_callback_${Date.now()}_${Math.floor(Math.random() * 100000)}`;
    const script = document.createElement('script');
    script.async = true;
    
    type JSONPCallback = (data: unknown) => void;
    const win = window as unknown as Window & Record<string, JSONPCallback | undefined>;

    win[uniqueCallback] = (data: unknown) => {
      // Clean up
      try {
        delete win[uniqueCallback];
        document.body.removeChild(script);
      } catch (e) {
        // Ignore cleanup errors
      }
      
      // Check if data contains an error before resolving
      if (data && typeof data === 'object') {
        const errorResponse = data as { 'response-code'?: number };
        if (errorResponse['response-code'] && errorResponse['response-code'] >= 400) {
          reject(new Error(`API Error ${errorResponse['response-code']}`));
          return;
        }
      }
      if(!data) {
        reject(new Error('No data returned from ORCID API'));
        return;
      }

      resolve(data);
    };

    script.onerror = () => {
      try {
        delete win[uniqueCallback];
        document.body.removeChild(script);
      } catch (e) {
        // Ignore cleanup errors
      }
      reject(new Error('ORCID Search request failed: ' + url));
    };

    const timeout = setTimeout(() => {
      try {
        delete win[uniqueCallback];
        document.body.removeChild(script);
      } catch (e) {
        // Ignore cleanup errors
      }
      reject(new Error('ORCID Search request timeout'));
    }, 10000);

    const originalCallback = win[uniqueCallback];
    win[uniqueCallback] = (data: unknown) => {
      clearTimeout(timeout);
      if (typeof originalCallback === 'function') {
        originalCallback(data);
      }
    };

    const finalUrl = url.replace('callback=?', `callback=${uniqueCallback}`);
    script.src = finalUrl;
    document.body.appendChild(script);
  });
};

const getErrorMessage = (responseCode: number): string => {
  switch (responseCode) {
    case 404:
      return 'ORCID iD not found. Please verify the ID format (e.g., 0000-0002-1825-0097) and try again.';
    case 400:
      return 'Invalid ORCID iD format. Please check your entry and try again.';
    case 500:
    case 503:
      return 'ORCID service is temporarily unavailable. Please try again later.';
    default:
      return 'An error occurred while fetching ORCID data. Please try again.';
  }
};

const searchAPI = async (url: string): Promise<unknown> => {
  try {
    const response = await fetchJSONP(url);
    if (typeof response === 'string') {
      if (response.includes('404 Not Found')) {
        throw new Error('ORCID iD not found. Please verify the ID and try again.');
      }
      if (response.includes('500 Internal Server Error')) {
        throw new Error('ORCID service is temporarily unavailable. Please try again later.');
      }
      
      const jsonMatch = response.match(/(\{[^}]*"response-code"[^}]*\})/);
      if (jsonMatch) {
        const errorData = JSON.parse(jsonMatch[0]);
        if (errorData['response-code'] >= 400) {
          throw new Error(getErrorMessage(errorData['response-code']));
        }
      }
      throw new Error('An unknown error occurred while fetching ORCID data.');
    }
    return response;
  } catch (error) {
    console.error('JSONP error:', error);
    throw error;
  }
};

export default function ORCIDLookup({
    path,
    setOrcidDetails,
    formMethods,
  }:{
    path: { name: string };
    setOrcidDetails: React.Dispatch<React.SetStateAction<unknown>>;
    formMethods?: Partial<{
      setValue: (name: string, value: unknown, options?: { shouldValidate?: boolean; shouldDirty?: boolean }) => void;
      formState?: { errors?: Record<string, unknown> };
    }>;
  }) {
  const [searchMode, setSearchMode] = useState<'lookup' | 'search'>('lookup');
  const [searchValue, setSearchValue] = useState('');
  const [searchText, clearSearchText] = useState(false);
  const [dropBox, setDropBox] = React.useState(false);
  const inputRef = React.useRef<HTMLInputElement>(null);
  const [verifiedORCID, setVerifiedORCID] = useState<boolean | null>(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cachedResult, setCachedResult] = useState<boolean>(false);
  const fieldName = path?.name;

  // Cleanup expired cache on mount
  React.useEffect(() => {
    orcidLookupCache.cleanup();
  }, []);

  // Typed shapes for results
  type OrcidData = {
    orcid: string;
    creditName: string;
    givenName: string;
    lastName: string;
    affiliation?: string;
    country?: string;
    keywords?: string;
  };

  interface LookupResponse {
    orcid: string;
    person: {
      name: {
        'credit-name': { value: string };
        'given-names': { value: string };
        'family-name': { value: string };
      };
      addresses?: {
        address?: {
            country: { value: string };
        }[];
      };
    };
  }

  interface SearchResponse {
    'orcid-search-results': {
      orcid: string;
      person: {
        name: {
          'credit-name': { value: string };
          'given-names': { value: string };
          'family-name': { value: string };
        };
        addresses?: {
          address?: {
            country: { value: string };
          }[];
        };
      };
    }[];
  }

  type SearchPerson = {
    orcid: string;
    creditName: string;
    givenName: string;
    lastName: string;
    affiliation?: string;
    country?: string;
    relevance?: number;
  };

  type ResultsState =
    | { type: 'orcid'; data: OrcidData }
    | { type: 'search'; data: SearchPerson[] };

  const [results, setResults] = useState<ResultsState | null>(null);
  const queryClient = useQueryClient();

  const searchConfig = {
    lookup: {
      placeholder: 'Type to search',
      endpoint: `https://${env}researchdata.ardc.edu.au/api/v2.0/orcid.jsonp/lookup/${encodeURIComponent(searchValue)}/?api_key=public&callback=?`,
      label: 'ORCID ID',
      description: 'Search by unique ORCID identifier',
      icon: <FingerprintIcon />
    },
    search: {
      placeholder: 'Type to search',
      endpoint: `https://${env}researchdata.ardc.edu.au/api/v2.0/orcid.jsonp/search?api_key=public&q=${encodeURIComponent(searchValue)}&start=0&rows=10&wt=json&callback=?`,
      label: 'Custom Search',
      description: 'Search by name or keywords',
      icon: <PersonIcon />
    },
    tooltipContent: (
      <>After selecting an ORCID record, the Credit Name will be displayed if available.
      If no Credit Name is provided, the Given Name and Family Name will be shown instead.
      Some details may not appear due to the researcher's visibility settings.
      For more information, see <a href="https://support.orcid.org/hc/en-us/articles/360006897614-Visibility-settings" target="_blank" rel="noopener noreferrer">[ORCID Visibility Settings]</a></>
    ),
    genericPlaceholder: `You can search by full ORCID iD or by contributor name (e.g., John Smith).`
  };

  const currentConfig = searchConfig[searchMode];

  const handleSearch = async (e?: React.SyntheticEvent) => {
  e?.preventDefault();
  setResults(null);
  setCachedResult(false);
  setError(null);

  const orcid = searchValue.trim().replace(replaceText, '').match(/^\d{4}-?\d{4}-?\d{4}-?\d{3}[0-9X]$/);

  if (orcid) {
    // LOOKUP MODE
    setSearchMode('lookup');
    const orcidParts = orcid[0].includes('-')
      ? orcid[0]
      : (orcid[0].match(/.{1,4}/g)?.join('-') || orcid[0]);

    // Check localStorage first
    const cachedData = orcidLookupCache.get<LookupResponse>(orcidParts);
    if (cachedData) {
      setCachedResult(true);
      processSearchData(cachedData, 'lookup');
      setDropBox(true);
      return;
    }

    // Fetch from API
    setIsLoading(true);
    setDropBox(true);
    try {
      const endpoint = `https://${env}researchdata.ardc.edu.au/api/v2.0/orcid.jsonp/lookup/${encodeURIComponent(orcidParts)}/?api_key=public&callback=?`;
      const data = await searchAPI(endpoint) as LookupResponse;

      // Create display name
      const creditName = data.person?.name?.['credit-name']?.value || '';
      const givenName = data.person?.name?.['given-names']?.value || '';
      const familyName = data.person?.name?.['family-name']?.value || '';
      const displayName = creditName || `${givenName} ${familyName}`.trim();

      // Save to cache with display name
      orcidLookupCache.set(orcidParts, data, displayName);
      processSearchData(data, 'lookup');
      setDropBox(true);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setIsLoading(false);
    }
  } else {
    // SEARCH MODE - No caching
    setSearchMode('search');
    setIsLoading(true);
    setDropBox(true);
    try {
      const endpoint = `https://${env}researchdata.ardc.edu.au/api/v2.0/orcid.jsonp/search?api_key=public&q=${encodeURIComponent(searchValue)}&start=0&rows=10&wt=json&callback=?`;
      const data = await searchAPI(endpoint) as SearchResponse;
      processSearchData(data, 'search');
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setIsLoading(false);
    }
  }
};

  // Update contributor names cache
  const updateContributorNamesCache = React.useCallback((searchTerm: string) => {
    try {
      const existing: string[] = queryClient.getQueryData(['contributorNames']) ||
                                 JSON.parse(localStorage.getItem("contributorNames") || "[]");
      const candidate = searchTerm.trim();

      if (candidate) {
        const newNames = Array.from(new Set([candidate, ...existing])).slice(0, 50);
        localStorage.setItem("contributorNames", JSON.stringify(newNames));
        queryClient.setQueryData(['contributorNames'], newNames);
      }
    } catch (err) {
      console.error('Error updating contributor names cache:', err);
    }
  }, [queryClient]);

  const onChangeMode = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    event.preventDefault();
    const value = (event.target as HTMLInputElement).value || '';
    setSearchValue(value);
    setVerifiedORCID(value === '' && false);
    formMethods?.setValue?.(fieldName, value);
    const orcid = value.trim().replace(replaceText, '').match(/^\d{4}-?\d{4}-?\d{4}-?\d{3}[0-9X]$/);
    if (orcid) {
      setSearchMode('lookup');
    } else {
      setSearchMode('search');
    }
  };

  type SearchResultItem = SearchResponse['orcid-search-results'][number];

  const getCountryName = (data: LookupResponse | SearchResultItem) => {
    const regionNamesInEnglish = new Intl.DisplayNames(["en"], { type: "region" });
    const countryCode =
      (data as LookupResponse).person?.addresses?.address?.[0]?.country?.value ||
      (data as SearchResultItem).person?.addresses?.address?.[0]?.country?.value;
    const countryName = countryCode?.length === 2 ? (regionNamesInEnglish.of(countryCode) || '') : countryCode;
    return countryName;
  }

  // Process search data
  const processSearchData = (data: LookupResponse | SearchResponse, mode: 'lookup' | 'search') => {
    try {
      if (mode === 'lookup') {
        const lookup = data as LookupResponse;
        setResults({
          type: 'orcid',
          data: {
            orcid: lookup.orcid,
            creditName: lookup.person?.name?.['credit-name']?.value?.trim() || '',
            givenName: lookup.person?.name?.['given-names']?.value?.trim() || '',
            lastName: lookup.person?.name?.['family-name']?.value?.trim() || '',
            country: getCountryName(lookup),
          }
        });
        updateContributorNamesCache(searchValue);
      } else {
        const searchData = data as SearchResponse;
        if (searchData && Array.isArray(searchData['orcid-search-results']) && searchData['orcid-search-results'].length > 0) {
          const mappedResults = searchData['orcid-search-results'].map((item, index) => {
            const given = item.person?.name?.['given-names']?.value || '';
            const family = item.person?.name?.['family-name']?.value || '';
            const credit = item.person?.name?.['credit-name']?.value || '';
            const country = getCountryName(item);
            return {
              orcid: item.orcid,
              creditName: credit.trim() || '',
              givenName: given.trim() || '',
              lastName: family.trim() || '',
              country: country?.trim() || '',
              relevance: 95 - (index * 5)
            };
          });
          setResults({
            type: 'search',
            data: mappedResults
          });

          updateContributorNamesCache(searchValue);
        } else {
          setVerifiedORCID(false);
        }
      }
    } catch (err) {
      console.error('Search error:', err);
    }
  };

  const getStatusColor = () => {
    if (isLoading && !cachedResult) return '#36a5dd';
    if (error) return '#f44336';
    if (results || cachedResult) return '#4caf50';
    return '#e0e0e0';
  };

const selectOrcid = (item: OrcidData | SearchPerson) => {
  const orcidUrl = import.meta.env.VITE_RAIDO_ENV === 'prod' ? `https://orcid.org/${item?.orcid}` : `https://sandbox.orcid.org/${item?.orcid}`;
  const orcidName = item?.creditName ? item.creditName : `${item?.givenName || ''} ${item?.lastName || ''}`.trim();

  // Save selected ORCID to localStorage
  const lookupData: LookupResponse = {
    orcid: item.orcid,
    person: {
      name: {
        'credit-name': { value: item.creditName },
        'given-names': { value: item.givenName },
        'family-name': { value: item.lastName },
      },
      addresses: item.country ? {
        address: [{
          country: { value: item.country }
        }]
      } : undefined
    }
  };

  // Save with display name
  orcidLookupCache.set(item.orcid, lookupData, orcidName);

  formMethods?.setValue?.(fieldName, orcidUrl, { shouldValidate: true, shouldDirty: true });
  setSearchValue(orcidName);
  setVerifiedORCID(true);
  setDropBox(false);
  setOrcidDetails(item);
  updateContributorNamesCache(orcidName);
}
  const _errors = formMethods?.formState?.errors as Record<string, unknown> | undefined;
  const helperTextError = Array.isArray((_errors as Record<string, any>)?.contributor) && !!((_errors as Record<string, any>)[path.name]?.message) ?
  "Enter a valid ORCID iD e.g. 0000-0002-1825-0097 or free text to search" as string : '';

  return (
    <Box sx={{ p: 1 }}>
      <Paper elevation={0} sx={{ p: 1, borderRadius: 2 }}>
        <Paper
          sx={{
            p: '2px 4px',
            display: 'flex',
            alignItems: 'center',
            width: '400px',
            border: 1,
            borderColor: getStatusColor(),
            position: 'relative'
          }}
          className={getStatusColor()}
        >
          {cachedResult && searchMode === 'lookup' && (
            <Chip
              label="Cached"
              size="small"
              sx={{
                position: 'absolute',
                top: -10,
                right: 10,
                fontSize: '0.65rem',
                height: '18px',
                bgcolor: '#4caf50',
                color: 'white'
              }}
            />
          )}
          {<span style={{ height: "40px", margin:"-1px", marginRight:"8px" }} ref={inputRef}></span>}
          {currentConfig.icon}
          <InputBase
            name={fieldName}
            sx={{ ml: 1, flex: 1 }}
            placeholder={currentConfig?.placeholder}
            inputProps={{ 'aria-label': 'search orcid' }}
            value={searchValue}
            onChange={(e) => { e.preventDefault(); clearSearchText(true); onChangeMode(e); }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                handleSearch(e);
              }
            }}
          />
          {searchText && (
            <CloseRoundedIcon
              onClick={() => {
                clearSearchText(false);
                formMethods?.setValue?.(fieldName, '');
                setSearchValue('');
                setVerifiedORCID(false);
                setResults(null);
                setCachedResult(false);
                setError(null);
              }}
            />
          )}
          <Divider sx={{ height: 28, m: 0.5 }} orientation="vertical" />
          <IconButton onClick={(e) => handleSearch(e)} color="primary" sx={{ p: '10px' }} aria-label="directions">
            {(isLoading && !cachedResult) ? <ClipLoader color="#36a5dd" size={25}/> : verifiedORCID ? <CircleCheckBig color='green'/> : <ScanSearch />}
          </IconButton>
        </Paper>
        {searchMode === 'lookup' && <FormHelperText sx={{ fontSize: '0.875rem', color: 'error.main', mr: 1 }}>{helperTextError}</FormHelperText>}
        <Box sx={{mt: 1, mb: 1, display: 'flex', alignItems: 'center', width: '400px', justifyContent: 'space-between' }}>
          <FormHelperText sx={{ fontSize: '0.875rem', color: 'text.secondary' }}>{searchConfig?.genericPlaceholder}</FormHelperText>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <CustomStyledTooltip
              title={"ORCID Lookup Info"}
              content={searchConfig.tooltipContent}
              variant="info"
              placement="top"
              tooltipIcon={<InfoOutlinedIcon />}
            >
            </CustomStyledTooltip>
          </Box>
        </Box>
        <Popover
          open={dropBox}
          anchorEl={inputRef.current}
          onClose={() => setDropBox(false)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
          transformOrigin={{ vertical: 'top', horizontal: 'left' }}
          sx={{ mt: 1, left:0, width: '400px', backgroundColor: 'text.dark' }}
        >
          {(results?.type === 'search' && results.data.length === 0) && (
            <Box sx={{ padding: 2, maxWidth:'400px'}}>
              <Typography variant="body2" className="text-orange-500">
                No results found for "{searchValue}"
              </Typography>
            </Box>
          )}
          {(results?.type === 'orcid' && [results.data].length === 0) && (
            <Box sx={{ padding: 2, maxWidth:'400px'}}>
              <Typography variant="body2" className="text-orange-500">
                No results found for "{searchValue}"
              </Typography>
            </Box>
          )}
          {error && (
            <Box sx={{ padding: 2, maxWidth: '400px', color: getStatusColor() }}>
              <Typography variant="body2" className="text-red-500">
                {error}
              </Typography>
            </Box>
          )}
          <Box sx={{ maxHeight: "350px", overflow: "auto", width: '400px' }} display={dropBox ? 'block' : 'none'}>
            {results?.data && (
              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'start', alignItems: 'center', m: 2 }}>
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    Search Results
                  </Typography>
                  {cachedResult && searchMode === 'lookup' && (
                    <>
                      <Chip
                        label="Cached"
                        size="small"
                        sx={{
                          bgcolor: '#4caf50',
                          color: 'white',
                          fontSize: '0.7rem',
                          marginLeft: '10px'
                        }}
                      />
                    </>
                  )}
                </Box>
                {results?.type === 'orcid' ? (
                  <Card
                    variant="outlined"
                    sx={{
                      mb: 2,
                      cursor: 'pointer',
                      background: 'inherit',
                          '&:hover': { boxShadow: 2 }
                    }}
                    onClick={() => selectOrcid(results.data)}
                  >
                    <CardContent>
                      <Box sx={{ display: 'flex', alignItems: 'start', gap: 2 }}>
                        <Avatar sx={{ bgcolor: 'secondary.main' }}>
                          <PersonIcon sx={{ fontSize: 32 }} />
                        </Avatar>
                        <Box sx={{ flex: 1 }}>
                          <Typography variant="body2" fontWeight={600}>
                            {results?.data?.creditName && `Published Name: ${results?.data?.creditName}`}
                          </Typography>
                          <Typography variant="body2" fontWeight={600}>
                            Given Names: {results?.data?.givenName}
                          </Typography>
                          <Typography variant="body2" fontWeight={600}>
                            Family Name: {results?.data?.lastName}
                          </Typography>
                          <Typography variant="body2" fontWeight={600}>
                            {results?.data?.country}
                          </Typography>
                          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                            <Typography variant="body2" fontWeight={600}>
                              ORCID:
                            </Typography>
                            <Chip
                              label={results?.data.orcid}
                              size="small"
                              variant="outlined"
                              icon={
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v2h-2zm0 4h2v6h-2z"/>
                                </svg>
                              }
                            />
                            <Link
                              href={`https://orcid.org/${results?.data.orcid}`}
                              target="_blank"
                              rel="noopener"
                              sx={{ 
                                fontSize: '0.875rem',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: 0.5
                              }}
                              onClick={(e) => e.stopPropagation()}
                            >
                              View Profile
                              <OpenInNewIcon sx={{ fontSize: 16 }} />
                            </Link>
                          </Box>
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                ) : (
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    {results?.data.map((person, index) => (
                      <Card
                        key={index}
                        variant="outlined"
                        sx={{
                          mb: 1,
                          cursor: 'pointer',
                          background: 'inherit',
                          '&:hover': { boxShadow: 2 }
                        }}
                        onClick={() => selectOrcid(person)}
                      >
                        <CardContent>
                          <Box sx={{ display: 'flex', alignItems: 'start', gap: 2 }}>
                            <Avatar sx={{ bgcolor: 'secondary.main' }}>
                              <PersonIcon />
                            </Avatar>
                            <Box sx={{ flex: 1 }}>
                              <Box sx={{
                                display: 'block',
                                justifyContent: 'space-between',
                                alignItems: 'start',
                                flexWrap: 'wrap',
                                gap: 1,
                                mb: 0.5
                              }}>
                                <Typography variant="body2" fontWeight={600}>
                                  {person.creditName && `Published Name: ${person.creditName}`}
                                </Typography>
                                <Typography variant="body2" fontWeight={600}>
                                  Given Names: {person.givenName}
                                </Typography>
                                <Typography variant="body2" fontWeight={600}>
                                  Family Name: {person.lastName}
                                </Typography>
                                <Typography variant="body2" fontWeight={600}>
                                  {person.country}
                                </Typography>
                              </Box>
                              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                                <Typography variant="body2" fontWeight={600}>
                                  ORCID:
                                </Typography>
                                <Chip
                                  label={person.orcid}
                                  size="small"
                                  variant="outlined"
                                  icon={
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                                      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v2h-2zm0 4h2v6h-2z"/>
                                    </svg>
                                  }
                                />
                                <Link
                                  href={`https://orcid.org/${person.orcid}`}
                                  target="_blank"
                                  rel="noopener"
                                  sx={{
                                    fontSize: '0.875rem',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: 0.5
                                  }}
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  View Profile
                                  <OpenInNewIcon sx={{ fontSize: 16 }} />
                                </Link>
                              </Box>
                            </Box>
                          </Box>
                        </CardContent>
                      </Card>
                    ))}
                  </Box>
                )}
              </Box>
            )}
            {(isLoading) && (
              <Box sx={{ padding: 2, minWidth: '350px', width: '400px'}}>
                <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: getStatusColor() }}>
                  {`${searchMode.charAt(0).toUpperCase() + searchMode.slice(1)} `} <PulseLoader color="#36a5dd" size={5} />
                </Typography>
              </Box>
            )}
          </Box>
        </Popover>
      </Paper>
    </Box>
  );
}
