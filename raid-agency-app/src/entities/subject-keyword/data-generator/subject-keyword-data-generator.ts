import { SubjectKeyword } from "@/generated/raid";
import languageSchema from "@/references/language_schema.json";

export const subjectKeywordDataGenerator = (): SubjectKeyword => {
  return {
    text: "",
    language: {
      id: "eng",
      schemaUri: languageSchema[0].uri,
    },
  };
};
