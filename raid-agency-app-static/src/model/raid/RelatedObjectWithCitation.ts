import type { RelatedObject } from '@/generated/raid';
import type { Citation } from './Citation';

export interface RelatedObjectWithCitation extends RelatedObject {
    citation?: Citation;
}
