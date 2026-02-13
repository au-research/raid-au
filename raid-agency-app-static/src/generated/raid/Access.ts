import type { AccessStatement } from './AccessStatement';
import type { AccessType } from './AccessType';

export interface Access {
    type: AccessType;
    statement?: AccessStatement;
    embargoExpiry?: Date;
}
