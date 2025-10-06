import { DisplayItem } from "@/components/display-item";
import { RelatedObject } from "@/generated/raid";
import { useMapping } from "@/mapping";
import { Box, Divider, Grid, Stack, Typography, Link } from "@mui/material";
import { memo, useMemo } from "react";
import { LoadingIndicator } from "@/components/loading-indicator";
import { ErrorAlertWithAction } from "@/components/error-alert-component";
import ReactMarkdown from "react-markdown";
import rehypeRaw from 'rehype-raw';

const RelatedObjectItemView = memo(
  ({
    i,
    relatedObject,
    relatedObjectCitation,
    doiLoadingStates,
    doiErrors,
    retrySingleDOI,
  }: {
    i: number;
    relatedObject: RelatedObject;
    relatedObjectCitation: string | undefined;
    doiLoadingStates: Record<string, boolean>;
    doiErrors: Record<string, string | null>;
    retrySingleDOI: (relatedObject: RelatedObject) => void;
  }) => {
    const { generalMap } = useMapping();

    const relatedObjectMappedValue = useMemo(
      () => generalMap.get(String(relatedObject.type?.id)) ?? "",
      [generalMap, relatedObject.type?.id]
    );

    const isLoading = relatedObject.id ? doiLoadingStates[relatedObject.id] : false;
    const hasTitle = Boolean(relatedObjectCitation);
    return (
      <Stack gap={2}>
        <Typography variant="body2" color="text.primary" sx={{ mt: 1 }}>
            {`Related Object #${i + 1}`}
          </Typography>
        <LoadingIndicator
          id={relatedObject.id}
          loadingStates={doiLoadingStates}
          message={"Loading citation from DOI.org..."}
        />
        {hasTitle && !isLoading && (
          <Box sx={{ mt: 0, display: 'flex', gap: 0.5 }}>
            <Typography variant="body2" sx={{ color: 'text.primary' }}>
              Citation:
            </Typography>
            <Typography variant="body2" color="text.secondary">
              <ReactMarkdown
                rehypePlugins={[rehypeRaw]}
                components={{
                  p: ({ children }) => <span>{children}</span>, // Render as span instead of p
                  strong: ({ children }) => <strong>{children}</strong>,
                  em: ({ children }) => <em>{children}</em>,
                  a: ({ href, children }) => (
                    <Link href={href} color="primary">
                      {children}
                    </Link>
                  ),
                }}
              >
                {relatedObjectCitation}
              </ReactMarkdown>
            </Typography>
          </Box>
        )}
        {/* Individual error message with retry */}
        {relatedObject.id && doiErrors[relatedObject.id] && (
          <ErrorAlertWithAction
            message={"Unable to retrieve citation from DOI.org."}
            onRetry={() => retrySingleDOI(relatedObject)}
            args={relatedObject}
          />
        )}
        <Grid container spacing={2}>
          <DisplayItem
            label="Related Object Link"
            value={relatedObject.id}
            link={relatedObject.id}
            width={8}
          />
          <DisplayItem
            label="Type"
            value={relatedObjectMappedValue}
            width={4}
          />
        </Grid>

        <Stack gap={2} sx={{ pl: 3 }}>
          <Stack direction="row" alignItems="baseline">
            <Typography variant="body1" sx={{ mr: 0.5 }}>
              Categories
            </Typography>
          </Stack>
          <Stack gap={2} divider={<Divider />}>
            {(relatedObject?.category || []).map((category) => {
              const relatedObjectCategoryMappedValue = 
                generalMap.get(String(category.id)) ?? "";

              return (
                <Grid container spacing={2} key={category.id}>
                  <DisplayItem
                    label="Position"
                    value={relatedObjectCategoryMappedValue}
                    width={6}
                  />
                </Grid>
              );
            })}
          </Stack>
        </Stack>
      </Stack>
    );
  }
);

RelatedObjectItemView.displayName = "RelatedObjectItemView";

export { RelatedObjectItemView };
