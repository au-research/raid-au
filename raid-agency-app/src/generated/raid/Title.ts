import type { Language } from './Language';
import type { TitleType } from './TitleType';

export interface Title {
    text: string;
    type: TitleType;
    startDate: string;
    endDate?: string;
    language?: Language;
}
