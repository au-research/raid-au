import * as React from 'react';
import Paper from '@mui/material/Paper';
import InputBase from '@mui/material/InputBase';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import TravelExploreIcon from '@mui/icons-material/TravelExplore';
import Stack from '@mui/material/Stack';
import List from '@mui/material/List';
import ListSubheader from '@mui/material/ListSubheader';
import ListItemText from '@mui/material/ListItemText';
import Box from '@mui/material/Box';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import { useFormContext } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { FormHelperText, ListItemButton, Popover, Typography } from '@mui/material';
import { Search, CheckCircle, AlertCircle } from 'lucide-react';
import { ClipLoader, PulseLoader } from "react-spinners";

// Type definitions for better type safety
interface RORLocation {
  geonames_details: {
    country_name: string;
  };
}

interface RORName {
  value: string;
  types: string[];
}

interface RORLink {
  type: string;
  value: string;
}

interface RORItem {
  id: string;
  names: RORName[];
  locations: RORLocation[];
  links: RORLink[];
}

interface RORResponse {
  items: RORItem[];
}

interface GroupedOrganization {
  id: string;
  displayName: string;
  links: RORLink[];
}

interface GroupedData {
  [countryName: string]: GroupedOrganization[];
}

const searchAPI = async (query: string): Promise<RORResponse> => {
  const response = await fetch(
    `https://api.ror.org/organizations?query=${encodeURIComponent(query)}`
  );
  if (!response.ok) {
    throw new Error('Search failed');
  }
  return response.json();
};

// Utility function to transform API response into grouped data
const transformToGroupedData = (items: RORItem[] = []): GroupedData => {
  const grouped: GroupedData = {};

  items.forEach((item) => {
    // Get the display name (prefer ror_display, fallback to label)
    const displayNameObj = item.names.find(name => 
      name.types.includes("ror_display") || name.types.includes("label")
    );
    
    if (!displayNameObj) return; // Skip items without display name

    const org: GroupedOrganization = {
      id: item.id,
      displayName: displayNameObj.value,
      links: item.links || []
    };

    // Add organization to each country it belongs to
    item.locations.forEach((location) => {
      const countryName = location.geonames_details.country_name;
      if (!grouped[countryName]) {
        grouped[countryName] = [];
      }
      grouped[countryName].push(org);
    });
  });

  return grouped;
};


interface CustomizedInputBaseProps {
  setSelectedValue: React.Dispatch<React.SetStateAction<{ id: string; name?: string } | null>>;
  name: string;
  defaultValue?: string;
  styles?: React.CSSProperties;
  resetValue?: { id: string; name?: string } | null;
}

export default function CustomizedInputBase({
  setSelectedValue,
  name,
  defaultValue,
  styles = { width: '400px' },
  resetValue
}: CustomizedInputBaseProps) {
  const [searchText, clearSearchText] = React.useState(false);
  const [inputText, setInputText] = React.useState(defaultValue || '');
  const [dropBox, setDropBox] = React.useState(false);
  const { watch, formState: { isDirty } } = useFormContext();
  const value = watch(`${name}`);
  const inputRef = React.useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    if (resetValue === null) {
      setInputText('');
    }
  }, [resetValue]);

  React.useEffect(() => {
    if (isDirty && value) {
      clearSearchText(true)
    } else {
      clearSearchText(false)
    }
  }, [isDirty, value, clearSearchText]);

  const searchMutation = useMutation({
      mutationFn: searchAPI,
      onError: (error) => {
        console.error('Search error:', error);
      },
    });
  
    // Transform data using useMemo for performance
    const groupedData = React.useMemo(() => {
      return transformToGroupedData(searchMutation.data?.items);
    }, [searchMutation.data]);
  
    // Get sorted country entries
    const sortedCountries = React.useMemo(() => {
      return Object.entries(groupedData).sort(([a], [b]) => a.localeCompare(b));
    }, [groupedData]);
  
    const handleSearch = (e: React.FormEvent) => {
      e.preventDefault();
      e.stopPropagation();
      if (inputText.trim()) {
        searchMutation.mutate(inputText.trim());
        setDropBox(true);
      }
    };
  
    const handleListItemClick = (org: GroupedOrganization) => {
      setDropBox(false);
      setSelectedValue({ id: org.id, name: org.displayName });
      clearSearchText(true);
      setInputText(org.id);
    };
  
    const formatSecondaryText = (links: RORLink[]) => {
      if (!links || links.length === 0) return "";
      // Format links as a string
      return (
        <>
          {links.slice(0, 2).map((link, idx) => (
            <React.Fragment key={link.type + link.value}>
              {idx > 0 && <br />}
              <strong>{link.type}:</strong> <a href={link.value} target="_blank" rel="noopener noreferrer">{link.value}</a>
            </React.Fragment>
          ))}
        </>
      );
    };
     const getStatusIcon = () => {
    switch (searchMutation.status) {
      case 'pending':
        return <ClipLoader color="#36a5dd" size={25}/>;
      case 'success':
        return <CheckCircle color="green" />;
      case 'error':
        return <AlertCircle color="red" />;
      case 'idle':
        return <Search color="gray" />;
      default:
        return <Search color="gray" />;
    }
  };

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
    <Stack spacing={2}>
      <Paper
        sx={{ p: '2px 4px', display: 'flex', alignItems: 'center', width: styles?.width || '400px', border: 1, borderColor: getStatusColor() }}
        className={getStatusColor()}
      >
        {<span style={{ height: "40px", margin:"-1px", marginRight:"8px" }} ref={inputRef}></span>}{getStatusIcon()}
        <InputBase
          sx={{ ml: 1, flex: 1 }}
          placeholder="Search Text or lookup ROR ID"
          inputProps={{ 'aria-label': 'Search Text or lookup ROR ID' }}
          value={inputText}
          onChange={(e) => {setInputText(e.target.value),clearSearchText(true)}}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              handleSearch(e);
            }
          }}
        />
        {searchText && <CloseRoundedIcon onClick={() => {clearSearchText(false), setInputText('')}} />}
        <Divider sx={{ height: 28, m: 0.5 }} orientation="vertical" />
        <IconButton  onClick={(e) => handleSearch(e)}  color="primary" sx={{ p: '10px' }} aria-label="directions">
          {searchMutation.status === 'pending' ? <ClipLoader color="#36a5dd" size={25}/> : <TravelExploreIcon />}
        </IconButton>
      </Paper>
      <FormHelperText>For e.g. "https://ror.org/123456" or "My Organization"</FormHelperText>
      <Popover
        open={dropBox}
        anchorEl={inputRef.current }
        onClose={() => setDropBox(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
        sx={{ mt: 1, left:0, width: styles?.width || '400px' }}
      >
        {searchMutation.data?.items?.length === 0 && (
          <Box sx={{ padding: 2, maxWidth: styles?.width || '400px'}}>
            <Typography variant="body2" className="text-orange-500">
              No organizations found for "{inputText}"
            </Typography>
          </Box>
        )}
        {searchMutation.status === 'error' && (
          <Box sx={{ padding: 2, maxWidth: styles?.width || '400px', color: getStatusColor() }}>
            <Typography variant="body2" className="text-red-500">
              Failed to fetch results for "{inputText}". Please try again.
            </Typography>
          </Box>
        )}
        <Box sx={{ maxHeight: "350px", overflow: "auto", width: styles?.width }} display={dropBox ? 'block' : 'none'}>
          {sortedCountries.length > 0 && (
            <List>
              {sortedCountries.map(([countryName, organizations]) => (
                <React.Fragment key={countryName}>
                  <ListSubheader
                    sx={{
                      backgroundColor: "primary.light",
                      color: "primary.contrastText",
                      position: "sticky",
                      top: 0,
                      zIndex: 1,
                      opacity: 1,
                    }}
                  >
                    {countryName} ({organizations.length})
                  </ListSubheader>
                  {organizations.map((org, index) => (
                    <ListItemButton
                      key={`${org.id}-${index}`}
                      onClick={() => handleListItemClick(org)}
                    >
                      <ListItemText
                        primary={org.displayName}
                        secondary={formatSecondaryText(org.links)}
                      />
                    </ListItemButton>
                  ))}
                </React.Fragment>
              ))}
            </List>
          )}
          {searchMutation.status === 'pending' && (
          <Box sx={{ padding: 2, minWidth: '350px', width: styles?.width || '400px'}}>
            <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: getStatusColor() }}>
              {`Searching `} <PulseLoader color="#36a5dd" size={5} />
            </Typography>
          </Box>
        )}
        </Box>
      </Popover>
    </Stack>
  );
}
