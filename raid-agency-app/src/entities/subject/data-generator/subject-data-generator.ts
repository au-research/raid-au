import { subjectKeywordDataGenerator } from "@/entities/subject-keyword/data-generator/subject-keyword-data-generator";
import { Subject } from "@/generated/raid";
import subjectType from "@/references/subject_type.json";

export const subjectDataGenerator = (url: string): Subject => {
  const schemaUri = url.includes("for") ? `https://vocabs.ardc.edu.au/viewById/316` : `https://vocabs.ardc.edu.au/viewById/317`;
  return {
    id: url,
    schemaUri: schemaUri,
    keyword: [subjectKeywordDataGenerator()],
  };
};
