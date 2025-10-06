import languageSchema from "@/references/language_schema.json";
import { Access } from "@/generated/raid";
import accessType from "@/references/access_type.json";
import accessTypeSchema from "@/references/access_type_schema.json";

export function accessDataGenerator(): Access {
  return {
    type: {
      id: accessType[0].uri,
      schemaUri: accessTypeSchema[0].uri,
    },
    statement: undefined,
    language: {
      id: "eng",
      schemaUri: languageSchema[0].uri,
    },
    embargoExpiry: undefined,
  };
}
