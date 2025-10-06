import type { Owner } from './Owner';
import type { RegistrationAgency } from './RegistrationAgency';

export interface Id {
    id: string;
    schemaUri: string;
    registrationAgency: RegistrationAgency;
    owner: Owner;
    raidAgencyUrl?: string;
    license: string;
    version: number;
}
