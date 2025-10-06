export interface ServicePointUpdateRequest {
    id: number;
    name: string;
    adminEmail?: string;
    techEmail?: string;
    identifierOwner: string;
    repositoryId?: string;
    groupId: string;
    prefix?: string;
    password?: string;
    appWritesEnabled?: boolean;
    enabled?: boolean;
}
