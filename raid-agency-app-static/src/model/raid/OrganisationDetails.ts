import type { Organisation } from "@/generated/raid";

export interface RORDetails extends Organisation {
    rorDetails: {
        rorId: string;
        name: string;
        country: string;
        types: Array<string>;
    };
}
