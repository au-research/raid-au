
export interface ServicePointUpdateRequest {
    name: string;
    adminEmail?: string;
    techEmail?: string;
    identifierOwner: string;
    groupId: string;
    appWritesEnabled?: boolean;
    enabled?: boolean;
    id: number;
}
