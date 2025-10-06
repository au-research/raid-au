/**
 * Core Type Definitions
 *
 * This module defines TypeScript interfaces and types used throughout the application.
 * It includes authentication, service point, and other shared types.
 */
import type {
  ServicePoint,
  ServicePointCreateRequest,
  ServicePointUpdateRequest,
} from "@/generated/raid";

/**
 * Represents a Keycloak group with identifier and name
 */
export type KeycloakGroup = {
  /** Unique identifier for the group */
  id: string;
  /** Display name of the group */
  name: string;
};

/**
 * User attributes associated with a service point member
 * Keycloak returns attributes as arrays even for single values
 */
type ServicePointMemberAttributes = {
  /** First name of the member (array with possibly null values) */
  firstName: (string | null)[];
  /** Last name of the member (array with possibly null values) */
  lastName: (string | null)[];
  /** ID of the active group the member belongs to */
  activeGroupId: string[];
  /** Email address of the member (array with possibly null values) */
  email: (string | null)[];
  /** Username for the member */
  username: string[];
};

/**
 * Represents a member of a service point with roles and attributes
 */
export type ServicePointMember = {
  /** Roles assigned to this member within the service point */
  roles: string[];
  /** User attributes containing personal information and settings */
  attributes: ServicePointMemberAttributes;
  /** Unique identifier for the member */
  id: string;
};

/**
 * Extended ServicePoint type that includes member information
 */
export type ServicePointWithMembers = ServicePoint & {
  /** List of members associated with this service point */
  members: ServicePointMember[];
};

/**
 * Request parameters for obtaining an API token
 */
export type ApiTokenRequest = {
  /** Refresh token used to obtain a new access token */
  refreshToken: string;
};

/**
 * Response from the token endpoint when requesting new tokens
 * Follows OAuth2/OpenID Connect token response format
 */
export type RequestTokenResponse = {
  /** Bearer token used for API authorization */
  access_token: string;
  /** Validity period of the access token in seconds */
  expires_in: number;
  /** OpenID Connect ID token containing user claims */
  id_token: string;
  /** Token policy timestamp */
  "not-before-policy": number;
  /** Validity period of the refresh token in seconds */
  refresh_expires_in: number;
  /** Token used to obtain new access tokens */
  refresh_token: string;
  /** OAuth scopes granted to this token */
  scope: string;
  /** Session identifier */
  session_state: string;
  /** Token type (typically "Bearer") */
  token_type: string;
};

export interface CreateServicePointRequest {
  servicePointCreateRequest: ServicePointCreateRequest;
}

export interface UpdateServicePointRequest {
  id: number;
  servicePointUpdateRequest: ServicePointUpdateRequest;
}
