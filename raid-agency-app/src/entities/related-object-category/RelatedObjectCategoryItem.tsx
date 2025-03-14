import { DisplayItem } from "@/components/display-item";
import { useMapping } from "@/mapping";
import { RelatedObjectCategory } from "@/generated/raid";
import { Grid } from "@mui/material";
import { memo, useMemo } from "react";

const RelatedObjectCategoryItem = memo(
  ({
    relatedObjectCategory,
  }: {
    relatedObjectCategory: RelatedObjectCategory;
  }) => {
    const { generalMap } = useMapping();

    const relatedObjectCategoryMappedValue = useMemo(
      () => generalMap.get(String(relatedObjectCategory.id)) ?? "",
      [generalMap, relatedObjectCategory.id]
    );
    return (
      <Grid container spacing={2}>
        <DisplayItem
          label="Position"
          value={relatedObjectCategoryMappedValue}
          width={6}
        />
      </Grid>
    );
  }
);

RelatedObjectCategoryItem.displayName = "RelatedObjectCategoryItem";
export default RelatedObjectCategoryItem;
