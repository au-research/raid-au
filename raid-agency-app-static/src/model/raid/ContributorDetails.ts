import type { Contributor } from '@/generated/raid/Contributor';

export interface ContributorDetails extends Contributor {
    orcidInfo?: {
        orcidId: string;
        authenticated: boolean;
        displayName: string | null;
        profileUrl: string | null;
        visibility: 'public' | 'limited' | 'private';
        style: string;
    };
}