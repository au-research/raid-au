import type { ValidationFailure } from './ValidationFailure';

export interface ValidationFailureResponse {
    type: string;
    title: string;
    status: number;
    detail: string;
    instance: string;
    failures: Array<ValidationFailure>;
}
