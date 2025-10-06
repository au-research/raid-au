import type { RelatedObject } from './RelatedObject';
import type { Description } from './Description';
import type { Organisation } from './Organisation';
import type { ModelDate } from './ModelDate';
import type { Access } from './Access';
import type { Metadata } from './Metadata';
import type { Contributor } from './Contributor';
import type { Title } from './Title';
import type { RelatedRaid } from './RelatedRaid';
import type { AlternateIdentifier } from './AlternateIdentifier';
import type { Subject } from './Subject';
import type { Id } from './Id';
import type { AlternateUrl } from './AlternateUrl';
import type { TraditionalKnowledgeLabel } from './TraditionalKnowledgeLabel';
import type { SpatialCoverage } from './SpatialCoverage';

export interface RaidDto {
    traditionalKnowledgeLabel?: Array<TraditionalKnowledgeLabel>;
    metadata?: Metadata;
    identifier: Id;
    title?: Array<Title>;
    date?: ModelDate;
    description?: Array<Description>;
    access: Access;
    alternateUrl?: Array<AlternateUrl>;
    contributor?: Array<Contributor>;
    organisation?: Array<Organisation>;
    subject?: Array<Subject>;
    relatedRaid?: Array<RelatedRaid>;
    relatedObject?: Array<RelatedObject>;
    alternateIdentifier?: Array<AlternateIdentifier>;
    spatialCoverage?: Array<SpatialCoverage>;
}
