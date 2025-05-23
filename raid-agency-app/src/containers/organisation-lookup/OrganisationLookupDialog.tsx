import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  ListSubheader,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import React, { useState } from "react";
import { Search as SearchIcon } from "@mui/icons-material";

const searchAPI = async (query: string) => {
  const response = await fetch(
    `https://api.ror.org/organizations?query=${query}`
  );
  return response.json();
};

export function OrganisationLookupDialog({
  open,
  setOpen,
  setSelectedValue,
}: {
  open: boolean;
  setOpen: (open: boolean) => void;
  setSelectedValue: (value: string | null) => void;
}) {
  const [query, setQuery] = useState("");
  const searchMutation = useMutation({
    mutationFn: searchAPI,
    onError: (error) => {
      console.error(error);
    },
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    searchMutation.mutate(query);
  };

  const handleListItemClick = (item: any) => {
    setOpen(false);
    setSelectedValue(item.id);
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
        },
      }}
    >
      <DialogTitle>Lookup ROR by name</DialogTitle>

      <DialogContent>
        <Stack>
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
              />
              <IconButton type="submit" disabled={searchMutation.isPending}>
                {searchMutation.isPending ? (
                  <CircularProgress size={24} />
                ) : (
                  <SearchIcon />
                )}
              </IconButton>
            </Stack>
          </form>
          {!searchMutation.data && !searchMutation.isPending && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="subtitle2">Examples: </Typography>
              <Stack direction="row" gap={1}>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => {
                    setQuery("ARDC");
                    searchMutation.mutate("ARDC");
                  }}
                >
                  ARDC
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => {
                    setQuery("Sydney");
                    searchMutation.mutate("Sydney");
                  }}
                >
                  Sydney
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => {
                    setQuery("Imperial");
                    searchMutation.mutate("Imperial");
                  }}
                >
                  Imperial
                </Button>
              </Stack>
            </Box>
          )}
        </Stack>

        {searchMutation.isPending ? (
          <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <List>
            {Object.entries(
              searchMutation.data?.items?.reduce(
                (acc: Record<string, any[]>, item: any) => {
                  const countryName = item.country.country_name;
                  if (!acc[countryName]) {
                    acc[countryName] = [];
                  }
                  acc[countryName].push(item);
                  return acc;
                },
                {}
              ) || {}
            )
              .sort(([countryA], [countryB]) =>
                countryA.localeCompare(countryB)
              )
              .map((value: [string, unknown]) => {
                const countryName = value[0];
                const items = value[1] as any[];
                return (
                  <div key={countryName}>
                    <ListSubheader
                      sx={{
                        backgroundColor: "white",
                        position: "sticky",
                        top: "0px",
                        zIndex: 1,
                        fontWeight: "bold",
                        bgcolor: "primary.light",
                        opacity: 1,
                      }}
                    >
                      {countryName}
                    </ListSubheader>
                    {items
                      .sort((a, b) => a.name.localeCompare(b.name))
                      .map((item, index) => (
                        <ListItemButton
                          key={`${countryName}-${index}`}
                          onClick={() => handleListItemClick(item)}
                        >
                          <ListItemText
                            primary={item.name}
                            secondary={
                              item?.links && item.links.length > 0
                                ? item.links[0]
                                : ""
                            }
                          />
                        </ListItemButton>
                      ))}
                  </div>
                );
              })}
          </List>
        )}
      </DialogContent>
    </Dialog>
  );
}
