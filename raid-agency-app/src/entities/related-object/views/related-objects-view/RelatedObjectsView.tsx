import { DisplayCard } from "@/components/display-card";
import { NoItemsMessage } from "@/components/no-items-message";
import { RelatedObject } from "@/generated/raid";
import { constructDOIUrl, fetchDetailedDOICitation, useRelatedObjectCitations } from "@/services/related-object";
import { Divider, Stack } from "@mui/material";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { memo, useEffect, useState, useRef, useCallback } from "react";
import { RelatedObjectItemView } from "@/entities/related-object/views/related-object-item-view";


// New interface for tracking loading states
interface DOILoadingState {
  [doiId: string]: boolean;
}
// New interface for tracking errors
// This allows us to track errors for each DOI separately
interface DOIErrorState {
  [doiId: string]: string;
}
// Type for related object citation
type RelatedObjectCitation = {
  cachedAt: number;
  source: string;
  fullCitation: string;
  value?: string; // To display in cache manager
};

// Cache configuration
const CACHE_CONFIG = {
  EXPIRY_DAYS: 90,
  BACKGROUND_REFRESH_DAYS: 30, // Refresh in background if older than 30 days
  MS_PER_DAY: 1000 * 60 * 60 * 24,
  STORAGE_KEY: "relatedObjectCitations",
  VERSION: "v1" // For cache versioning
};
// Function to check if cache is expired
// This checks if the cached item is older than the configured expiry days
const isCacheExpired = (cachedAt: number): boolean => {
  const cacheAge = Date.now() - cachedAt;
  return cacheAge > CACHE_CONFIG.EXPIRY_DAYS * CACHE_CONFIG.MS_PER_DAY;
};

const RelatedObjectsView = memo(({ data }: { data: RelatedObject[] }) => {
  const { data: relatedObjectCitations } = useRelatedObjectCitations();
  // State to track loading and error states for DOIs
  // Using separate states for loading and errors to avoid unnecessary re-renders
  const [doiLoadingStates, setDoiLoadingStates] = useState<DOILoadingState>({});
  const [doiErrors, setDoiErrors] = useState<DOIErrorState>({});
  // Ref to track attempted DOIs to avoid re-fetching
  // This will persist across renders without causing re-renders
  const attemptedDOIsRef = useRef<Set<string>>(new Set());

  const queryClient = useQueryClient();

  // Updated mutation using batch fetch
  const downloadMutation = useMutation({
    mutationFn: async (relatedObjects: RelatedObject[]) => {
      // Filter valid objects and construct DOI URLs
      const validObjects = relatedObjects.filter(obj => obj.id);
      // Set loading states ONLY for DOIs being fetched now
      setDoiLoadingStates(prev => {
        const newStates = { ...prev };
        validObjects.forEach(obj => {
          if (obj.id) {
            newStates[obj.id] = true;
          }
        });
        return newStates;
      });

      // Clear errors ONLY for DOIs being fetched now
      setDoiErrors(prev => {
        const newErrors = { ...prev };
        validObjects.forEach(obj => {
          if (obj.id) delete newErrors[obj.id];
        });
        return newErrors;
      });
      // Use the fast batch fetch function
      // Fetch each DOI and track individual results
      const fetchPromises = validObjects.map(async (obj) => {
        const id = obj.id!;
        const url = constructDOIUrl(id);
        try {
          const citation = await fetchDetailedDOICitation(url, { 
            timeout: 8000,
            userAgent: 'RelatedObjects-Fetcher/1.0'
          });

          if (!citation) {
            throw new Error('Citation not found');
          }

          return { id, citation, error: null };
        } catch (error) {
          const errorMessage = error instanceof Error ? error.message : 'Unknown error';
          return { id, citation: null, error: errorMessage };
        }
      });

      const results = await Promise.all(fetchPromises);

      // Update ONLY the states for DOIs we just fetched
      setDoiLoadingStates(prev => {
        const newStates = { ...prev };
        results.forEach(result => {
          delete newStates[result.id];
        });
        return newStates;
      });

      // Add errors ONLY for failed DOIs we just fetched
      setDoiErrors(prev => {
        const newErrors = { ...prev };
        results.forEach(result => {
          if (result.error) {
            newErrors[result.id] = result.error;
          }
        });
        return newErrors;
      });

      // Process successful results
      const successfulResults = results.filter(r => r.citation !== null);

      return { results: successfulResults };
    },
    onSuccess: ({ results }) => {
      const currentNames =
        queryClient.getQueryData<Map<string, RelatedObjectCitation>>(["relatedObjectCitations"]) ||
        new Map<string, RelatedObjectCitation>();
      const newNames = new Map(currentNames);

      results.forEach(({ id, citation }) => {
        if (citation) {
          queryClient.setQueryData(["doi", id], {
            ra: 'doi-batch',
            fullCitation: citation,
            value: citation,// To display in cache manager
          });

          newNames.set(id, {
            cachedAt: Date.now(),
            source: 'doi-batch',
            fullCitation: citation,
            value: citation // To display in cache manager
          });
        }
      });

      localStorage.setItem(
        "relatedObjectCitations",
        JSON.stringify([...newNames])
      );
      queryClient.setQueryData(["relatedObjectCitations"], newNames);
    },
    onError: (error) => {
      console.error('❌ Batch DOI fetch failed:', error);
    }
  });

  // Retry individual DOI
  const retrySingleDOI = useCallback((relatedObject: RelatedObject) => {
    if (!relatedObject.id) return;
    // Call mutation directly
    downloadMutation.mutate([relatedObject]);
  }, [downloadMutation])

  useEffect(() => {
    if(!data.length || !relatedObjectCitations) return;
      // Filter out already attempted or cached objects
      // and those that are still fresh in cache
      const orgsToDownload = data.filter((org) => {
        if (!org.id || attemptedDOIsRef.current.has(org.id)) return false;
        const cached = relatedObjectCitations.get(org.id);
        if (!cached) return true;
        return isCacheExpired(cached.cachedAt);
      });

    if (orgsToDownload.length > 0) {
      // Mark as attempted IMMEDIATELY
      const newAttempted = new Set(attemptedDOIsRef.current);
      orgsToDownload.forEach(org => {
        if (org.id) newAttempted.add(org.id);
      });
      attemptedDOIsRef.current = newAttempted;
      downloadMutation.mutate(orgsToDownload);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, relatedObjectCitations]);

  return (
    <DisplayCard
      data={data}
      labelPlural="Related Objects"
      children={
        <>
          {data.length === 0 && <NoItemsMessage entity="related object" />}
          <Stack gap={2} divider={<Divider />}>
            {data?.map((relatedObject, i) => (
              <RelatedObjectItemView
                relatedObject={relatedObject}
                key={relatedObject.id || i}
                i={i}
                relatedObjectCitation={
                  relatedObjectCitations?.size &&
                  relatedObjectCitations?.get(relatedObject.id)?.fullCitation
                }
                doiLoadingStates={doiLoadingStates}
                doiErrors={doiErrors}
                retrySingleDOI={retrySingleDOI}
              />
            ))}
          </Stack>
        </>
      }
    />
  );
});

RelatedObjectsView.displayName = "RelatedObjectsView";

export { RelatedObjectsView };
