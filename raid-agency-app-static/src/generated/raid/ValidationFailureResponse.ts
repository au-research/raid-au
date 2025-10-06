import type { ValidationFailure } from './ValidationFailure';

export interface ValidationFailureResponse {
    failures: Array<ValidationFailure>;
    type: string;
    title: string;
    status: number;
    detail: string;
    instance: string;
}
