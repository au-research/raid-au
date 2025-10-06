import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  List,
  ListItemButton,
  ListItemText,
  ListSubheader,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import React, { useState, useMemo } from "react";

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

export function OrganisationLookupDialog({
  open,
  setOpen,
  setSelectedValue,
}: {
  open: boolean;
  setOpen: (open: boolean) => void;
  setSelectedValue: (value: { id: string; name?: string }) => void;
}) {
  const [query, setQuery] = useState("");

  const searchMutation = useMutation({
    mutationFn: searchAPI,
    onError: (error) => {
      console.error('Search error:', error);
    },
  });

  // Transform data using useMemo for performance
  const groupedData = useMemo(() => {
    return transformToGroupedData(searchMutation.data?.items);
  }, [searchMutation.data]);

  // Get sorted country entries
  const sortedCountries = useMemo(() => {
    return Object.entries(groupedData).sort(([a], [b]) => a.localeCompare(b));
  }, [groupedData]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (query.trim()) {
      searchMutation.mutate(query.trim());
    }
  };

  const handleListItemClick = (org: GroupedOrganization) => {
    setOpen(false);
    setSelectedValue({ id: org.id, name: org.displayName });
  };

  const handleExampleClick = (exampleQuery: string) => {
    setQuery(exampleQuery);
    searchMutation.mutate(exampleQuery);
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

  return (
    <Dialog
      open={open}
      onClose={() => setOpen(false)}
      maxWidth="md"
      PaperProps={{
        style: {
          width: "600px",
          maxWidth: "90vw",
          maxHeight: "80vh", // Add max height for better UX
        },
      }}
    >
      <DialogTitle>Search for ROR by name and press enter</DialogTitle>

      <DialogContent>
        <Stack spacing={2}>
          <form onSubmit={handleSearch}>
            <Stack gap={2} alignItems="center" direction="row">
              <TextField
                label="Search"
                placeholder="Search for an organisation / city"
                size="small"
                variant="filled"
                fullWidth
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                disabled={searchMutation.isPending}
              />
            </Stack>
          </form>

          {/* Example buttons */}
          {!searchMutation.data && !searchMutation.isPending && (
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Examples:
              </Typography>
              <Stack direction="row" gap={1} flexWrap="wrap">
                {["ARDC", "Sydney", "Imperial"].map((example) => (
                  <Button
                    key={example}
                    size="small"
                    variant="outlined"
                    onClick={() => handleExampleClick(example)}
                  >
                    {example}
                  </Button>
                ))}
              </Stack>
            </Box>
          )}

          {/* Results */}
          {searchMutation.isPending ? (
            <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
              <CircularProgress />
            </Box>
          ) : searchMutation.data ? (
            <Box sx={{ maxHeight: "400px", overflow: "auto" }}>
              {sortedCountries.length > 0 ? (
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
              ) : (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  No organizations found for "{query}"
                </Typography>
              )}
            </Box>
          ) : searchMutation.error ? (
            <Typography variant="body2" color="error" sx={{ mt: 2 }}>
              Search failed. Please try again.
            </Typography>
          ) : null}
        </Stack>
      </DialogContent>
    </Dialog>
  );
}
