import type { AccessStatement } from './AccessStatement';
import type { AccessType } from './AccessType';
import { Language } from './Language';

export interface Access {
    type: AccessType;
    statement?: AccessStatement;
    language?: Language
    embargoExpiry?: Date;
}
