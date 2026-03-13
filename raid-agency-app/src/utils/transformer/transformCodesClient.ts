import {
  transformANZSRCCodes,
  transformSDGs,
  sortHierarchy,
  flattenHierarchy,
  findNodeById,
  toggleNodeSelection,
  getStatistics
} from "./codes-transformer"; // <-- shared logic from your TS file

// Use your existing CodeNode / interfaces from your TS conversion
import { CodeNode, TransformedData } from "./codes-transformer";

export function transformCodesClient(
  anzsrcFor: any[],
  anzsrcSeo: any[],
  //sdgs: any[]
): TransformedData {
  return {
    "ANZSRC FOR": transformANZSRCCodes(anzsrcFor, "FOR"),
    "ANZSRC SEO": transformANZSRCCodes(anzsrcSeo, "SEO"),
    //"SDGs": transformSDGs(sdgs)
  };
}

export {
  transformANZSRCCodes,
  transformSDGs,
  sortHierarchy,
  flattenHierarchy,
  findNodeById,
  toggleNodeSelection,
  getStatistics
};
