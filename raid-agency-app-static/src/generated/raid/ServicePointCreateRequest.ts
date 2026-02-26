
export interface ServicePointCreateRequest {
    name: string;
    adminEmail?: string;
    techEmail?: string;
    identifierOwner: string;
    groupId: string;
    appWritesEnabled?: boolean;
    enabled?: boolean;
}
