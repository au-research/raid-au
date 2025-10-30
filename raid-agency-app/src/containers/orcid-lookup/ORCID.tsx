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
import { ScanSearch } from 'lucide-react';
import { ClipLoader } from 'react-spinners';
import { useMutation } from '@tanstack/react-query';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import PulseLoader from "react-spinners/PulseLoader";

  // JSONP helper function
  const fetchJSONP = (url: string) => {
    return new Promise((resolve, reject) => {
      // Create a unique callback name
      const uniqueCallback = `jsonp_callback_${Date.now()}_${Math.floor(Math.random() * 100000)}`;
      
      // Create the script element
      const script = document.createElement('script');
      script.async = true;
      
      // Typed window helper for JSONP callbacks
      type JSONPCallback = (data: unknown) => void;
      const win = window as unknown as Window & Record<string, JSONPCallback | undefined>;
      
      // Set up the callback function
      win[uniqueCallback] = (data: unknown) => {
        // Clean up
        try {
          delete win[uniqueCallback];
          document.body.removeChild(script);
        } catch (e) {
          // Ignore cleanup errors
        }
        resolve(data);
      };
      
      // Handle errors
      script.onerror = () => {
        try {
          delete win[uniqueCallback];
          document.body.removeChild(script);
        } catch (e) {
          throw new Error('JSONP cleanup failed');
        }
        reject(new Error('JSONP request failed: ' + url));
      };
      
      // Add timeout
      const timeout = setTimeout(() => {
        try {
          delete win[uniqueCallback];
          document.body.removeChild(script);
        } catch (e) {
          // Ignore cleanup errors
        }
        reject(new Error('JSONP request timeout'));
      }, 10000);
      
      // Override cleanup to clear timeout
      const originalCallback = win[uniqueCallback];
      win[uniqueCallback] = (data: unknown) => {
        clearTimeout(timeout);
        if (typeof originalCallback === 'function') {
          originalCallback(data);
        }
      };
      
      // Replace callback=? with our callback name (jQuery style)
      const finalUrl = url.replace('callback=?', `callback=${uniqueCallback}`);
      script.src = finalUrl;
      
      // Append script to body to trigger the request
      document.body.appendChild(script);
    });
  };

const searchAPI = async (url: string): Promise<unknown> => {
  const response = await fetchJSONP(
    url
  );
/*   if (!response.ok) {
    throw new Error('Search failed');
  } */
  return response;
};

export default function ORCIDLookup(index: number) {
  const [searchMode, setSearchMode] = useState<'lookup' | 'search'>('lookup');
  const [searchValue, setSearchValue] = useState('');
  const [searchText, clearSearchText] = useState(false);
  const [dropBox, setDropBox] = React.useState(false);
  const inputRef = React.useRef<HTMLInputElement>(null);
  const [orcidName, setOrcidName] = useState<{
    creditName?: string;
    givenName?: string;
    lastName?: string;
  } | null>(null);

  // Typed shapes for results to allow safe access and narrowing
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

  const searchConfig = {
    lookup: {
      placeholder: 'Enter ORCID ID (e.g., 0000-0002-1825-0097)',
      endpoint: `https://researchdata.edu.au/api/v2.0/orcid.jsonp/lookup/${encodeURIComponent(searchValue)}/?api_key=public&callback=?`,
      label: 'ORCID ID',
      description: 'Search by unique ORCID identifier',
      icon: <FingerprintIcon />
    },
    search: {
      placeholder: 'Enter contributor name (e.g., John Smith)',
      endpoint: `https://researchdata.edu.au/api/v2.0/orcid.jsonp/search?api_key=public&q=${encodeURIComponent(searchValue)}&start=0&rows=10&wt=json&callback=?`,
      label: 'Custom Search',
      description: 'Search by name or keywords',
      icon: <PersonIcon />
    }
  };
  const currentConfig = searchConfig[searchMode];

  const handleSearch = async (e?: React.SyntheticEvent) => {
    e?.preventDefault();
    setResults(null);
    if (!searchValue.trim()) {
      return;
    }
    const orcid = searchValue.trim().replace('https://orcid.org/', '').match(/^\d{4}-?\d{4}-?\d{4}-?\d{3}[0-9X]$/);
    if (orcid) {
      setSearchMode('lookup');
      currentConfig.endpoint = `https://researchdata.edu.au/api/v2.0/orcid.jsonp/lookup/${encodeURIComponent(orcid[0])}/?api_key=public&callback=?`;
    } else {
      setSearchMode('search');
    }
    // Pass a single object matching the mutationFn signature
    searchMutation.mutate(currentConfig.endpoint);
    setDropBox(true);
  };

  const searchMutation = useMutation<unknown, Error, string>({
    mutationFn: searchAPI,
    onError: (error) => {
        console.error('Search error:', error);
    },
  });

  const onChangeMode = (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    event.preventDefault();
    const value = (event.target as HTMLInputElement).value || '';
    const orcid = value.trim().replace('https://orcid.org/', '').match(/^\d{4}-?\d{4}-?\d{4}-?\d{3}[0-9X]$/);
    if (orcid) {
      setSearchMode('lookup');
    } else {
      setSearchMode('search');
    }
  };

  React.useMemo(() => {
    if (searchMutation.data) {
        const data = searchMutation.data as unknown as LookupResponse | SearchResponse;
        try {
        if (searchMode === 'lookup') {
            // ORCID Lookup
            const lookup = data as LookupResponse;
            setResults({
                type: 'orcid',
                data: {
                    orcid: lookup.orcid,
                    creditName: lookup.person?.name?.['credit-name']?.value?.trim() || '',
                    givenName: lookup.person?.name?.['given-names']?.value?.trim() || '',
                    lastName: lookup.person?.name?.['family-name']?.value?.trim() || '',
                    country: lookup.person?.addresses?.address?.[0]?.country?.value || '',
                }
            });
        } else {
            // Narrow the union to SearchResponse before indexing
            const searchData = data as SearchResponse;
            if (searchData && Array.isArray(searchData['orcid-search-results']) && searchData['orcid-search-results'].length > 0) {
                const mappedResults = searchData['orcid-search-results'].map((item, index) => {
                const given = item.person?.name?.['given-names']?.value || '';
                const family = item.person?.name?.['family-name']?.value || '';
                const credit = item.person?.name?.['credit-name']?.value || '';
                const country = item.person?.addresses?.address?.[0]?.country?.value || '';
                return {
                        orcid: item.orcid,
                        creditName: credit.trim() || '',
                        givenName: given.trim() || '',
                        lastName: family.trim() || '',
                        country: country?.trim() || '',
                        relevance: 95 - (index * 5) // Mock relevance based on position
                    };
                });
                setResults({
                    type: 'search',
                    data: mappedResults
                });
            } else {
                setOrcidName(null);
            }
        }
    } catch (err) {
        console.error('Search error:', err);
    }
    }
    }, [searchMutation.data, searchMode]);

    const getStatusColor = () => {
        switch (searchMutation.status) {
        case 'pending':
            return '#36a5dd';
        case 'success':
            return '#4caf50';
        case 'error':
            return '#f44336';
        case 'idle':
            return '#e0e0e0';
        default:
            return '#e0e0e0';
        }
    };

  return (
    <Box sx={{ p: 1 }}>
      <Paper elevation={0} sx={{ p: 1, borderRadius: 2 }}>
            <Box sx={{ mb: 1 }}>
            <Typography variant="body1" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                Full Name: {orcidName && (`${orcidName.creditName ? orcidName.creditName : orcidName.givenName + ' ' + orcidName.lastName}`)}
            </Typography>
            </Box>
            <Paper
                sx={{ p: '2px 4px', display: 'flex', alignItems: 'center', width: '400px', border: 1, borderColor: getStatusColor() }}
                className={getStatusColor()}
            >
            {<span style={{ height: "40px", margin:"-1px", marginRight:"8px" }} ref={inputRef}></span>}{currentConfig.icon}
            <InputBase
                name={`contributor.${index}.id`}
                sx={{ ml: 1, flex: 1 }}
                placeholder={currentConfig?.placeholder}
                inputProps={{ 'aria-label': 'search orcid' }}
                value={searchValue}
                onChange={(e) => {e.preventDefault(); setSearchValue(e.target.value),clearSearchText(true), onChangeMode(e)}}
                onKeyDown={(e) => {
                if (e.key === 'Enter') {
                        handleSearch(e);
                    }
                }}
            />
            {searchText && <CloseRoundedIcon onClick={() => {clearSearchText(false), setSearchValue('')}} />}
            <Divider sx={{ height: 28, m: 0.5 }} orientation="vertical" />
            <IconButton  onClick={(e) => handleSearch(e)}  color="primary" sx={{ p: '10px' }} aria-label="directions">
                {searchMutation.status === 'pending' ? <ClipLoader color="#36a5dd" size={25}/> : <ScanSearch />}
            </IconButton>
            </Paper>
            <FormHelperText>{currentConfig?.description}</FormHelperText>
            <Popover
                open={dropBox}
                anchorEl={inputRef.current }
                onClose={() => setDropBox(false)}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                transformOrigin={{ vertical: 'top', horizontal: 'left' }}
                sx={{ mt: 1, left:0, width: '400px' }}
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
                {searchMutation.status === 'error' && (
                    <Box sx={{ padding: 2, maxWidth: '400px', color: getStatusColor() }}>
                    <Typography variant="body2" className="text-red-500">
                        Failed to fetch results for "{searchValue}". Please try again.
                    </Typography>
                    </Box>
                )}
                <Box sx={{ maxHeight: "350px", overflow: "auto", width: '400px' }} display={dropBox ? 'block' : 'none'}>
                            {/* Results Display */}
                    {results?.data && (
                    <Box>
                        <Typography variant="h6" sx={{ m: 2, fontWeight: 600 }}>
                        Search Results
                        </Typography>

                        {results?.type === 'orcid' ? (
                        // Single ORCID Result
                        <Card 
                            variant="outlined" 
                            sx={{ 
                            mb: 2,
                            cursor: 'pointer',
                            background: 'linear-gradient(to right, #eff6ff, #ffffff)',
                            '&:hover': { boxShadow: 2 }
                            }}
                            onClick={() => {
                                setSearchValue(`https://orcid.org/${results?.data.orcid}`);
                                setDropBox(false);
                                setOrcidName(results?.data);
                            }}
                        >
                            <CardContent>
                            <Box sx={{ display: 'flex', alignItems: 'start', gap: 2 }}>
                                <Avatar 
                                    sx={{ 
                                        bgcolor: 'secondary.main',
                                    }}
                                >
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
                        // Multiple Search Results
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            {results?.data.map((person, index) => (
                            <Card 
                                key={index} 
                                variant="outlined" 
                                sx={{ 
                                mb: 2,
                                cursor: 'pointer',
                                background: 'linear-gradient(to right, #eff6ff, #ffffff)',
                                '&:hover': { boxShadow: 2 }
                                }}
                                onClick={() => {
                                    setSearchValue(`https://orcid.org/${person.orcid}`);
                                    setDropBox(false);
                                    setOrcidName(person);
                                }}
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
                    {searchMutation.status === 'pending' && (
                        <Box sx={{ padding: 2, minWidth: '350px', width: '400px'}}>
                            <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: getStatusColor() }}>
                                {`Searching `} <PulseLoader color="#36a5dd" size={5} />
                            </Typography>
                        </Box>
                    )}
                </Box>
            </Popover>
      </Paper>
    </Box>
  );
}
