import type { RelatedObjectType } from './RelatedObjectType';
import type { RelatedObjectCategory } from './RelatedObjectCategory';

export interface RelatedObject {
    id?: string;
    schemaUri?: string;
    type?: RelatedObjectType;
    category?: Array<RelatedObjectCategory>;
}
