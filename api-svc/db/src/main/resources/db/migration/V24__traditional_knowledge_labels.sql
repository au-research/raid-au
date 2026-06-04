begin transaction;

drop table if exists raido.traditional_knowledge_label_schema;
create table raido.traditional_knowledge_label_schema (
    id serial primary key,
    uri varchar not null
);

drop table if exists raido.traditional_knowledge_label;
create table raido.traditional_knowledge_label
(
    schema_id int     not null,
    uri      varchar not null,
    primary key (schema_id, uri),
    constraint fk_traditional_knowledge_label_id foreign key (schema_id) references raido.traditional_knowledge_label_schema (id)
);

insert into raido.traditional_knowledge_label_schema (uri)
values ('https://localcontexts.org/labels/traditional-knowledge-labels/'),
       ('https://localcontexts.org/labels/biocultural-labels/');

insert into raido.traditional_knowledge_label (schema_id, uri)
values (1, 'https://localcontexts.org/label/tk-attribution/'),
       (1, 'https://localcontexts.org/label/tk-clan/'),
       (1, 'https://localcontexts.org/label/tk-family/'),
       (1, 'https://localcontexts.org/label/tk-multiple-communities/'),
       (1, 'https://localcontexts.org/label/tk-community-voice/'),
       (1, 'https://localcontexts.org/label/tk-creative/'),
       (1, 'https://localcontexts.org/label/tk-verified/'),
       (1, 'https://localcontexts.org/label/tk-non-verified/'),
       (1, 'https://localcontexts.org/label/tk-seasonal/'),
       (1, 'https://localcontexts.org/label/tk-women-general/'),
       (1, 'https://localcontexts.org/label/tk-men-general/'),
       (1, 'https://localcontexts.org/label/tk-men-restricted/'),
       (1, 'https://localcontexts.org/label/tk-women-restricted/'),
       (1, 'https://localcontexts.org/label/tk-culturally-sensitive/'),
       (1, 'https://localcontexts.org/label/tk-secret-sacred/'),
       (1, 'https://localcontexts.org/label/tk-commercial/'),
       (1, 'https://localcontexts.org/label/tk-non-commercial/'),
       (1, 'https://localcontexts.org/label/tk-community-use-only/'),
       (1, 'https://localcontexts.org/label/tk-outreach/'),
       (1, 'https://localcontexts.org/label/tk-open-to-collaboration/'),
       (2, 'https://localcontexts.org/label/bc-provenance/'),
       (2, 'https://localcontexts.org/label/bc-multiple-communities/'),
       (2, 'https://localcontexts.org/label/bc-clan/'),
       (2, 'https://localcontexts.org/label/bc-consent-verified/'),
       (2, 'https://localcontexts.org/label/bc-consent-non-verified/'),
       (2, 'https://localcontexts.org/label/bc-research-use/'),
       (2, 'https://localcontexts.org/label/bc-open-to-collaboration/'),
       (2, 'https://localcontexts.org/label/bc-open-to-commercialization/'),
       (2, 'https://localcontexts.org/label/bc-outreach/'),
       (2, 'https://localcontexts.org/label/bc-non-commercial/');

update raido.language_schema set uri = 'https://iso639-3.sil.org' where id = 1;

end transaction;
