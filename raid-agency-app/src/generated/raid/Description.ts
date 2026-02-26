import type { DescriptionType } from './DescriptionType';
import type { Language } from './Language';

export interface Description {
    text: string;
    type: DescriptionType;
    language?: Language;
}
