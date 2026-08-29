import { subjectKeywordDataGenerator } from "@/entities/subject-keyword/data-generator/subject-keyword-data-generator";
import { Subject } from "@/generated/raid";
import subjectType from "@/references/subject_type.json";

export const subjectDataGenerator = (): Subject => {
  const randomIndex = Math.floor(Math.random() * subjectType.length);
  return {
    id: `https://linked.data.gov.au/def/anzsrc-for/2020/${subjectType[randomIndex].id}`,
    schemaUri: `https://vocabs.ardc.edu.au/viewById/316`,
    keyword: [subjectKeywordDataGenerator()],
  };
};
