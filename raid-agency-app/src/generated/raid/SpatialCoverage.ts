import type { SpatialCoveragePlace } from './SpatialCoveragePlace';

export interface SpatialCoverage {
    id?: string;
    schemaUri?: string;
    place?: Array<SpatialCoveragePlace>;
}
