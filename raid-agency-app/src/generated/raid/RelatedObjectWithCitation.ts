import type { RelatedObject } from './RelatedObject';
import type { Citation } from './Citation';

export interface RelatedObjectWithCitation extends RelatedObject {
    citation?: Citation;
}
