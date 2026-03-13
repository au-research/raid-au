import {
  LayersClear as LayersClearIcon,
  Layers as LayersIcon,
  Search as SearchIcon,
} from "@mui/icons-material";
import {
  Card,
  CardContent,
  CardHeader,
  Container,
  IconButton,
  InputAdornment,
  List,
  ListItem,
  ListItemText,
  Paper,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import { useMemo, useState } from "react";

dayjs.extend(relativeTime);

function CachedItemsList({
  cachedMap,
  handleDelete,
  storageKey,
}: {
  cachedMap: Map<string, { value: string; cachedAt: number; source?: string }>;
  handleDelete: ({
    key,
    storageKey,
  }: {
    key: string;
    storageKey: string;
  }) => void;
  storageKey: string;
}) {
  const [searchTerm, setSearchTerm] = useState("");

  const filteredEntries = useMemo(() => {
    return Array.from(cachedMap.entries()).filter(([key, entry]) => {
      const searchLower = searchTerm.toLowerCase();
      return (
        key.toLowerCase().includes(searchLower) ||
        entry.value.toLowerCase().includes(searchLower)
      );
    });
  }, [cachedMap, searchTerm]);

  const highlightMatch = (text: string) => {
    if (!searchTerm) return text;
    const regex = new RegExp(`(${searchTerm})`, "gi");
    const parts = text.split(regex);
    return (
      <>
        {parts.map((part, i) =>
          regex.test(part) ? (
            <strong
              key={i}
              style={{
                backgroundColor: "#ff0",
                borderRadius: "3px",
                padding: "0 2px",
              }}
            >
              {part}
            </strong>
          ) : (
            part
          )
        )}
      </>
    );
  };

  return (
    <>
      <TextField
        fullWidth
        margin="normal"
        variant="outlined"
        placeholder="Search entries..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon />
            </InputAdornment>
          ),
        }}
      />
      <List>
        {filteredEntries
          .sort(([, a], [, b]) => b.cachedAt - a.cachedAt)
          .map(([key, value]) => (
            <ListItem
              key={key}
              secondaryAction={
                <Tooltip title="Invalidate" placement="right">
                  <IconButton
                    edge="end"
                    aria-label="invalidate"
                    onClick={() =>
                      handleDelete({
                        key,
                        storageKey,
                      })
                    }
                  >
                    <LayersClearIcon />
                  </IconButton>
                </Tooltip>
              }
            >
              <ListItemText
                primary={highlightMatch(value.value)}
                secondary={`${key} | ${dayjs
                  .unix(value.cachedAt / 1000)
                  .fromNow()}  ${value.source ? ` | ${value.source}` : ""}`}
              />
            </ListItem>
          ))}
      </List>
    </>
  );
}

const useCache = ({ key }: { key: string }) => {
  const [cachedData, setCachedData] = useState(() => {
    const stored = localStorage.getItem(key);
    return stored ? new Map(JSON.parse(stored)) : new Map();
  });
  
  return { data: cachedData, refresh: setCachedData };
};

export function CacheManager() {

  const { data: relatedObjectCitations, refresh: refreshRelatedObjectCitations } = useCache({
    key: 'relatedObjectCitations'
  });

  const { data: organisationNames, refresh: refreshOrganisationNames  } = useCache({
    key: 'organisationNames'
  });

  const { data: contributorNames, refresh: refreshContributorNames } = useCache({
    key: 'orcidCache'
  });

    // Map storageKey to refresh function
  const refreshFunctions = {
    'relatedObjectCitations': refreshRelatedObjectCitations,
    'organisationNames': refreshOrganisationNames,
    'orcidCache': refreshContributorNames,
  };

const deleteItemMutation = useMutation({
  mutationFn: async ({ key, storageKey }: { key: string; storageKey: string }) => {
    const stored = localStorage.getItem(storageKey);
    if (stored) {
      const map = new Map(JSON.parse(stored));
      map.delete(key);
      localStorage.setItem(storageKey, JSON.stringify(Array.from(map)));
    }
    return Promise.resolve();
  },
  onSuccess: (_, { key, storageKey }) => {
    // Re-read from localStorage and update state
    const stored = localStorage.getItem(storageKey);
    const updatedMap = stored ? new Map(JSON.parse(stored)) : new Map();
      
    // Call the appropriate refresh function
    const refreshFn = refreshFunctions[storageKey as keyof typeof refreshFunctions];
    if (refreshFn) {
      refreshFn(updatedMap);
    }
  },
});


  const handleDelete = ({
    key,
    storageKey,
  }: {
    key: string;
    storageKey: string;
  }) => {
    deleteItemMutation.mutate({
      key,
      storageKey
    });
  };

  return (
    <Container>
      <Stack gap={2}>
        <Stack
          component={Paper}
          elevation={0}
          sx={{ p: 2 }}
          direction="row"
          alignItems="center"
          gap={2}
        >
          <LayersIcon />
          <Typography variant="h5">Cache Manager</Typography>
        </Stack>
        <Card>
          <CardHeader title="Related object citations" />
          <CardContent>
            {(relatedObjectCitations?.size && (
              <CachedItemsList
                cachedMap={relatedObjectCitations}
                handleDelete={handleDelete}
                storageKey="relatedObjectCitations"
              />
            )) || <Typography>No related objects in cache</Typography>}
          </CardContent>
        </Card>

        <Card>
          <CardHeader title="Organisation names" />
          <CardContent>
            {(organisationNames?.size && (
              <CachedItemsList
                cachedMap={organisationNames}
                handleDelete={handleDelete}
                storageKey="organisationNames"
              />
            )) || <Typography>No organisation names in cache</Typography>}
          </CardContent>
        </Card>

        <Card>
          <CardHeader title="ORCID names" />
          <CardContent>
            {(contributorNames?.size && (
              <CachedItemsList
                cachedMap={contributorNames}
                handleDelete={handleDelete}
                storageKey="orcidCache"
              />
            )) || <Typography>No ORCID names in cache</Typography>}
          </CardContent>
        </Card>
      </Stack>
    </Container>
  );
}
