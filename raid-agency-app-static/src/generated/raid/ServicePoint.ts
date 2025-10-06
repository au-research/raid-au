export interface ServicePoint {
    id: number;
    name: string;
    identifierOwner: string;
    repositoryId?: string;
    prefix?: string;
    groupId?: string;
    searchContent?: string;
    techEmail: string;
    adminEmail: string;
    enabled: boolean;
    appWritesEnabled?: boolean;
}
