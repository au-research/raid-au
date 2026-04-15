import { relatedObjectCategoryDataGenerator } from "@/entities/related-object-category/data-generator/related-object-category-data-generator";
import { RelatedObject, RelatedObjectType } from "@/generated/raid";
import relatedObjectTypes from "@/references/related_object_type.json";
import relatedObjectTypesSchema from "@/references/related_object_type_schema.json";

const relatedObjectTypeGenerator = (): RelatedObjectType => {
  return {
    id: relatedObjectTypes[0].uri,
    schemaUri: relatedObjectTypesSchema[0].uri,
  };
};

export const relatedObjectDataGenerator = (): RelatedObject => {
  return {
    id: "",
    schemaUri: "",
    type: relatedObjectTypeGenerator(),
    category: [relatedObjectCategoryDataGenerator()],
  };
};
