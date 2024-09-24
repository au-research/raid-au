/* tslint:disable */
/* eslint-disable */
/**
 * RAID v2 API
 * This file is where all the endpoint paths are defined, it\'s the \"top level\' of the OpenAPI definition that links all the different files together. The `3.0` in the filename refers to this file being based on OpenAPI 3.0  as opposed to OpenAPI 3.1, which the tooling doesn\'t support yet. The `2.0.0` in the version field refers to the fact that there\'s already  a `1.0.0` used for the legacy RAiD application. Note that swagger ui doesn\'t currently work with our spec,  see https://github.com/swagger-api/swagger-ui/issues/7724 But the spec works fine with openapi-generator tooling. 
 *
 * The version of the OpenAPI document: 2.0.0
 * Contact: contact@raid.org
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { ContributorPosition } from './ContributorPosition';
import {
    ContributorPositionFromJSON,
    ContributorPositionFromJSONTyped,
    ContributorPositionToJSON,
} from './ContributorPosition';
import type { ContributorRole } from './ContributorRole';
import {
    ContributorRoleFromJSON,
    ContributorRoleFromJSONTyped,
    ContributorRoleToJSON,
} from './ContributorRole';

/**
 * 
 * @export
 * @interface Contributor
 */
export interface Contributor {
    /**
     * 
     * @type {string}
     * @memberof Contributor
     */
    id: string;
    /**
     * 
     * @type {string}
     * @memberof Contributor
     */
    schemaUri: string;
    /**
     * 
     * @type {string}
     * @memberof Contributor
     */
    status?: string;
    /**
     * 
     * @type {string}
     * @memberof Contributor
     */
    email?: string;
    /**
     * 
     * @type {string}
     * @memberof Contributor
     */
    uuid?: string;
    /**
     * 
     * @type {Array<ContributorPosition>}
     * @memberof Contributor
     */
    position: Array<ContributorPosition>;
    /**
     * 
     * @type {Array<ContributorRole>}
     * @memberof Contributor
     */
    role: Array<ContributorRole>;
    /**
     * 
     * @type {boolean}
     * @memberof Contributor
     */
    leader?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof Contributor
     */
    contact?: boolean;
}

/**
 * Check if a given object implements the Contributor interface.
 */
export function instanceOfContributor(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "id" in value;
    isInstance = isInstance && "schemaUri" in value;
    isInstance = isInstance && "position" in value;
    isInstance = isInstance && "role" in value;

    return isInstance;
}

export function ContributorFromJSON(json: any): Contributor {
    return ContributorFromJSONTyped(json, false);
}

export function ContributorFromJSONTyped(json: any, ignoreDiscriminator: boolean): Contributor {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': json['id'],
        'schemaUri': json['schemaUri'],
        'status': !exists(json, 'status') ? undefined : json['status'],
        'email': !exists(json, 'email') ? undefined : json['email'],
        'uuid': !exists(json, 'uuid') ? undefined : json['uuid'],
        'position': ((json['position'] as Array<any>).map(ContributorPositionFromJSON)),
        'role': ((json['role'] as Array<any>).map(ContributorRoleFromJSON)),
        'leader': !exists(json, 'leader') ? undefined : json['leader'],
        'contact': !exists(json, 'contact') ? undefined : json['contact'],
    };
}

export function ContributorToJSON(value?: Contributor | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'id': value.id,
        'schemaUri': value.schemaUri,
        'status': value.status,
        'email': value.email,
        'uuid': value.uuid,
        'position': ((value.position as Array<any>).map(ContributorPositionToJSON)),
        'role': ((value.role as Array<any>).map(ContributorRoleToJSON)),
        'leader': value.leader,
        'contact': value.contact,
    };
}

