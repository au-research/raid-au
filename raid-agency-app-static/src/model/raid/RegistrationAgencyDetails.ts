import type { RegistrationAgency } from "@/generated/raid";

/**
 * RegistrationAgency enriched at build time by scripts/fetch-ror.js with the
 * organisation details resolved from ROR. The shape of `rorDetails` mirrors the
 * output of getSimplifiedRorInfo in that script.
 */
export interface RegistrationAgencyDetails extends RegistrationAgency {
    rorDetails?: {
        rorId: string;
        name: string;
        type: string;
        rorUrl: string;
    };
}
