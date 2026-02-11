import type { OrganisationRole } from './OrganisationRole';

export interface Organisation {
    id: string;
    schemaUri: string;
    role: Array<OrganisationRole>;
    rorDetails: {
        rorId: string;
        name: string;
        country: string;
        types: Array<string>;
    };
}
