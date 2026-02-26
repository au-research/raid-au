import type { SubjectKeyword } from './SubjectKeyword';

export interface Subject {
    id: string;
    schemaUri: string;
    keyword?: Array<SubjectKeyword>;
}
