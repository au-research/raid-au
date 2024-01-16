BEGIN TRANSACTION;

create table api_svc.language_new
(
    id        serial primary key,
    code      varchar not null,
    name      varchar not null,
    schema_id int     not null
);


insert into api_svc.language_new (code, name, schema_id)
select id, name, schema_id
from api_svc.language;

drop table api_svc.language;
alter table api_svc.language_new rename to language;

-- ACCESS
create table api_svc.access_type_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null
);

insert into api_svc.access_type_new (uri, schema_id)
select uri, schema_id
from api_svc.access_type;

drop table api_svc.access_type;
alter table api_svc.access_type_new rename to access_type;

-- /ACCESS

-- TITLE
create table api_svc.title_type_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null
);

insert into api_svc.title_type_new (uri, schema_id)
select uri, schema_id
from api_svc.title_type;

drop table api_svc.title_type;
alter table api_svc.title_type_new rename to title_type;

create table api_svc.raid_title
(
    id            serial primary key,
    handle        varchar not null references api_svc.raid (handle),
    title_type_id int     not null references api_svc.title_type (id),
    text          text    not null,
    language_id   int references api_svc.language (id),
    start_date    varchar not null,
    end_date      varchar
);

-- /TITLE

-- DESCRIPTION
create table api_svc.description_type_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null
);

insert into api_svc.description_type_new (uri, schema_id)
select uri, schema_id
from api_svc.description_type;

drop table api_svc.description_type;
alter table api_svc.description_type_new rename to description_type;

create table api_svc.raid_description
(
    id                  serial primary key,
    handle              varchar not null references api_svc.raid (handle) on delete cascade,
    description_type_id int     not null references api_svc.description_type (id),
    text                text    not null,
    language_id         int references api_svc.language (id)
);


-- /DESCRIPTION

-- CONTRIBUTOR

create table api_svc.contributor_position_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null
);

insert into api_svc.contributor_position_new (uri, schema_id)
select uri, schema_id
from api_svc.contributor_position;

drop table api_svc.contributor_position;
alter table api_svc.contributor_position_new rename to contributor_position;

create table api_svc.contributor_role_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null
);

insert into api_svc.contributor_role_new (uri, schema_id)
select uri, schema_id
from api_svc.contributor_role;

drop table api_svc.contributor_role;
alter table api_svc.contributor_role_new rename to contributor_role;

create table api_svc.contributor_schema
(
    id         serial primary key,
    uri varchar not null
);

insert into api_svc.contributor_schema (uri)
values ('https://orcid.org/');

create table api_svc.contributor
(
    id        serial primary key,
    pid       varchar not null,
    schema_id int     not null references contributor_schema (id),
    unique (pid, schema_id)
);

create table api_svc.raid_contributor
(
    id             serial primary key,
    handle         varchar not null references api_svc.raid (handle) on delete cascade,
    contributor_id int     not null references api_svc.contributor (id),
    leader         boolean,
    contact        boolean,
    unique (handle, contributor_id)
);

create table api_svc.raid_contributor_position
(
    id                      serial primary key,
    raid_contributor_id     int     not null references api_svc.raid_contributor (id) on delete cascade,
    contributor_position_id int     not null references api_svc.contributor_position (id),
    start_date              varchar not null,
    end_date                varchar,
    unique(raid_contributor_id, contributor_position_id, start_date, end_date)
);

create table api_svc.raid_contributor_role
(
    id                  serial primary key,
    raid_contributor_id int not null references api_svc.raid_contributor (id) on delete cascade,
    contributor_role_id int not null references api_svc.contributor_role (id),
    unique(raid_contributor_id, contributor_role_id)
);

-- /CONTRIBUTOR

-- ORGANISATION

create table api_svc.organisation_schema
(
    id  serial primary key,
    uri varchar not null,
    unique(uri)
);

insert into api_svc.organisation_schema (uri)
values ('https://ror.org/');


create table api_svc.organisation
(
    id        serial primary key,
    pid       varchar not null,
    schema_id int     not null references organisation_schema (id),
    unique (pid, schema_id)
);

create table api_svc.organisation_role_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null,
    unique (uri, schema_id)
);

insert into api_svc.organisation_role_new (uri, schema_id)
select uri, schema_id
from api_svc.organisation_role;

drop table api_svc.organisation_role;
alter table api_svc.organisation_role_new rename to organisation_role;

create table api_svc.raid_organisation
(
    id              serial primary key,
    handle       varchar not null,
    organisation_id int     not null,
    unique(handle, organisation_id)
);

create table api_svc.raid_organisation_role
(
    id                   serial primary key,
    raid_organisation_id int not null references api_svc.raid_organisation (id) on delete cascade,
    organisation_role_id int not null references api_svc.organisation_role (id),
    start_date           varchar,
    end_date             varchar,
    unique(raid_organisation_id, organisation_role_id, start_date, end_date)
);

-- /ORGANISATION

-- RELATED OBJECT

create table api_svc.related_object_schema
(
    id  serial primary key,
    uri varchar not null,
    unique (uri)
);
insert into api_svc.related_object_schema (uri)
values ('https://doi.org/');


create table api_svc.related_object_category_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null,
    unique(uri, schema_id)
);

insert into api_svc.related_object_category_new (uri, schema_id)
select uri, schema_id
from api_svc.related_object_category;

drop table api_svc.related_object_category;
alter table api_svc.related_object_category_new rename to related_object_category;

create table api_svc.related_object_type_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null,
    unique(uri, schema_id)
);

insert into api_svc.related_object_type_new (uri, schema_id)
select uri, schema_id
from api_svc.related_object_type;

drop table api_svc.related_object_type;
alter table api_svc.related_object_type_new rename to related_object_type;

create table api_svc.related_object
(
    id        serial primary key,
    pid       varchar not null,
    schema_id int     not null references related_object_schema (id),
    unique (pid, schema_id)
);

create table api_svc.raid_related_object
(
    id                     serial primary key,
    handle                 varchar not null references api_svc.raid (handle) on delete cascade,
    related_object_id      int     not null references api_svc.related_object (id),
    related_object_type_id int     not null references api_svc.related_object_type (id),
    unique (handle, related_object_id, related_object_type_id)
);

create table api_svc.raid_related_object_category
(
    id                         serial primary key,
    raid_related_object_id     int not null references api_svc.raid_related_object (id) on delete cascade,
    related_object_category_id int not null references api_svc.related_object_category (id),
    unique(raid_related_object_id, related_object_category_id)
);

-- /RELATED OBJECT

-- ALTERNATE IDENTIFIER

create table api_svc.raid_alternate_identifier
(
    handle varchar not null references api_svc.raid (handle) on delete cascade,
    id     varchar not null,
    type   varchar not null,
    primary key (handle, id, type)
);

-- /ALTERNATE IDENTIFIER

-- ALTERNATE URL
create table api_svc.raid_alternate_url
(
    handle varchar not null references api_svc.raid (handle) on delete cascade,
    url    varchar not null,
    primary key (handle, url)
);

-- /ALTERNATE URL

-- RELATED RAID
create table api_svc.related_raid_type_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null,
    unique (uri, schema_id)
);

insert into api_svc.related_raid_type_new (uri, schema_id)
select uri, schema_id
from api_svc.related_raid_type;

drop table api_svc.related_raid_type;
alter table api_svc.related_raid_type_new rename to related_raid_type;

create table api_svc.related_raid
(
    handle               varchar not null references api_svc.raid (handle) on delete cascade,
    related_raid_handle  varchar not null,
    related_raid_type_id int     not null,
    primary key (handle, related_raid_handle)
);

-- /RELATED RAID

-- RAID

alter table api_svc.raid
    add column start_date_string varchar,
    add column end_date            varchar,
    add column license             varchar,
    add column access_type_id      int,
    add column embargo_expiry      date,
    add column access_statement    text,
    add column access_statement_language_id int references api_svc.language (id),
    add column schema_uri varchar,
    add column registration_agency_organisation_id int references api_svc.organisation (id),
    add column owner_organisation_id int references api_svc.organisation (id),
    alter column url drop not null,
    alter column url_index drop not null,
    alter column confidential drop not null,
    alter column start_date drop not null,
    alter column metadata drop not null,
    alter column primary_title drop not null;

-- /RAID

-- SUBJECT

create table api_svc.raid_subject
(
    id              serial primary key,
    handle          varchar not null references api_svc.raid (handle) on delete cascade,
    subject_type_id varchar not null references api_svc.subject_type (id),
    unique (handle, subject_type_id)
);

create table api_svc.raid_subject_keyword
(
    id              serial primary key,
    raid_subject_id int     not null references api_svc.raid_subject (id),
    keyword         varchar not null,
    language_id     int references api_svc.language (id),
    unique (raid_subject_id, keyword, language_id)
);

-- /SUBJECT

-- TRADITIONAL KNOWLEDGE LABEL

create table api_svc.traditional_knowledge_label_new
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int     not null,
    unique (uri, schema_id)
);

insert into api_svc.traditional_knowledge_label_new (uri, schema_id)
select uri, schema_id
from api_svc.traditional_knowledge_label;

drop table api_svc.traditional_knowledge_label;
alter table api_svc.traditional_knowledge_label_new rename to traditional_knowledge_label;

create table api_svc.raid_traditional_knowledge_label
(
    id                                    serial primary key,
    handle                                varchar not null references api_svc.raid (handle) on delete cascade,
    traditional_knowledge_label_id        int references api_svc.traditional_knowledge_label (id),
    traditional_knowledge_label_schema_id int references api_svc.traditional_knowledge_label_schema (id)
);

-- /TRADITIONAL KNOWLEDGE LABEL

-- SPATIAL COVERAGE

create table api_svc.spatial_coverage_schema
(
    id         serial primary key,
    uri varchar not null,
    unique (uri)
);

insert into api_svc.spatial_coverage_schema (uri)
values ('https://www.openstreetmap.org/'),
       ('https://www.geonames.org/');

create table api_svc.raid_spatial_coverage
(
    id        serial primary key,
    handle    varchar not null references api_svc.raid (handle) on delete cascade,
    uri       varchar not null,
    schema_id int     not null references api_svc.spatial_coverage_schema (id)
);

create table api_svc.raid_spatial_coverage_place
(
    raid_spatial_coverage_id int not null references api_svc.raid_spatial_coverage (id) on delete cascade,
    place                    varchar,
    language_id              int references api_svc.language (id)
);

-- /SPATIAL COVERAGE

-- INSERT TITLES
-- METADATA SCHEMA V2
insert into api_svc.raid_title (handle, title_type_id, text, language_id, start_date, end_date)
select r.handle as handle,
       (select id from api_svc.title_type where uri = r.type::json->> 'id') as title_type_id,
       r.text as text,
       (select id from api_svc.language where code = r.language::json->> 'id') as language_id,
       "startDate" as start_date,
       "endDate" as end_date
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{title}') as x(type text, "text" text, "startDate" text, "endDate" text, language text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') as r
on conflict do nothing;


-- METADATA SCHEMA V2 & LEGACY
insert into api_svc.raid_title (handle, title_type_id, text, start_date, end_date)
select r.handle as handle,
       case when r.type = 'Primary Title' then 2
            else 1 end as title_type_id,
       r.title as text,
       "startDate" as start_date,
       "endDate" as end_date
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{titles}') as x(type text, "title" text, "startDate" date, "endDate" date)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') as r
on conflict do nothing;

-- /INSERT TITLES

-- INSERT DESCRIPTIONS

insert into api_svc.raid_description (handle, description_type_id, text, language_id)
select r.handle as handle,
       (select id from api_svc.description_type where uri = r.type::json->> 'id') as description_type_id,
       r.text as text,
       (select id from api_svc.language where code = r.language::json->> 'id') as language_id
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{description}') as x(type text, "text" text,  language text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') as r
on conflict do nothing;

-- METADATA SCHEMA V2 & LEGACY
insert into api_svc.raid_description (handle, description_type_id, text)
select r.handle as handle,
       case when r.type = 'Primary Description' then 2
            else 1 end as description_type_id,
       r.description as text
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{descriptions}') as x(type text, "description" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') as r
on conflict do nothing;

-- /INSERT DESCRIPTIONS


-- INSERT CONTRIBUTORS

insert into api_svc.contributor (pid, schema_id)
select distinct id,
                (select id from api_svc.contributor_schema where uri = "schemaUri") as schema_id
from (select x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributor}') as x(id text, "schemaUri" text, leader boolean,
                                                                             contact boolean, "role" text,
                                                                             "position" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.contributor (pid, schema_id)
select distinct id,
                (select id from api_svc.contributor_schema where uri = "identifierSchemeUri") as schema_id
from (select x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributors}') as x(id text, "identifierSchemeUri" text, "roles" jsonb, "positions" jsonb)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;



insert into api_svc.raid_contributor (handle, contributor_id, leader, contact)
select handle,
       (select id from api_svc.contributor where pid = r.id) as contributor_id,
       leader,
       contact
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributor}') as x(id text, "schemaUri" text, leader boolean,
                                                                             contact boolean, "role" text,
                                                                             "position" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') as r
on conflict do nothing;

insert into api_svc.raid_contributor (handle, contributor_id)

select handle,
       (select id from api_svc.contributor where pid = r.id) as contributor_id
from (select handle,  x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributors}') as x(id text, "identifierSchemeUri" text, "roles" jsonb, "positions" jsonb)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.raid_contributor_position (raid_contributor_id, contributor_position_id, start_date, end_date)
select (select id
        from api_svc.raid_contributor
        where contributor_id = (select id from api_svc.contributor where pid = r.orcid)
          and handle = r.handle)                                     as raid_contributor_id,
       (select id from api_svc.contributor_position where uri = r.id) as contributor_position_id,
       "startDate"                                                    as start_date,
       "endDate"                                                      as end_date

from (select handle, x.id as orcid, xx.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributor}') as x(id text, "schemaUri" text, leader boolean,
                                                                             contact boolean, "role" text,
                                                                             "position" jsonb),
           lateral jsonb_to_recordset(x.position) as xx(id text, "schemaUri" text, "startDate" text, "endDate" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.raid_contributor_position (raid_contributor_id, contributor_position_id, start_date, end_date)
select (select id
        from api_svc.raid_contributor
        where contributor_id = (select id from api_svc.contributor where pid = r.orcid)
          and handle = r.handle) as raid_contributor_id,
       (select case
                   when position = 'Co-Investigator' then 1
                   when position = 'Contact Person' then 2
                   when position = 'Leader' then 3
                   when position = 'Other Participant' then 4
                   when position = 'Principal Investigator' then 5
                   end)           as contributor_position_id,
       "startDate"                as start_date,
       "endDate"                  as end_date
from (select handle, x.id as orcid, xx.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributors}') as x(id text, "identifierSchemeUri" text, "roles" jsonb, "positions" jsonb),
           lateral jsonb_to_recordset(x.positions) as xx(position text, "positionSchemaUri" text, "startDate" text, "endDate" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.raid_contributor_role (raid_contributor_id, contributor_role_id)
select (select id
        from api_svc.raid_contributor
        where contributor_id = (select id from api_svc.contributor where pid = r.orcid)
          and handle = r.handle)                                     as raid_contributor_id,
       (select id from api_svc.contributor_role where uri = r.id) as contributor_role_id
from (select handle, x.id as orcid, xx.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributor}') as x(id text, "schemaUri" text, leader boolean,
                                                                             contact boolean, "role" jsonb,
                                                                             "position" jsonb),
           lateral jsonb_to_recordset(x.role) as xx(id text, "schemaUri" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.raid_contributor_role (raid_contributor_id, contributor_role_id)
select (select id
        from api_svc.raid_contributor
        where contributor_id = (select id from api_svc.contributor where pid = r.orcid)
          and handle = r.handle) as raid_contributor_id,
       (select id from api_svc.contributor_role where uri like '%' || role || '%')           as contributor_role_id
from (select handle, x.id as orcid, xx.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{contributors}') as x(id text, "identifierSchemeUri" text, "roles" jsonb, "positions" jsonb),
           lateral jsonb_to_recordset(x.roles) as xx(role text, "roleSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

-- /INSERT CONTRIBUTORS

-- INSERT ORGANISATIONS

insert into api_svc.organisation (pid, schema_id)
select distinct id,
                (select id from api_svc.organisation_schema where uri = "schemaUri") as schema_id
from (select x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{organisation}') as x(id text, "schemaUri" text, "role" jsonb)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.organisation (pid, schema_id)
select distinct id, 1 as schema_id
from (select x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{organisations}') as x(id text, "roleSchemeUri" text, "roles" jsonb)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.raid_organisation (handle, organisation_id)
select handle,
       (select id from api_svc.organisation where pid = r.id) as organisation_id
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{organisation}') as x(id text, "schemaUri" text, "role" jsonb)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') as r
on conflict do nothing;

insert into api_svc.raid_organisation (handle, organisation_id)
select handle,
       (select id from api_svc.organisation where pid = r.id) as organisation_id
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{organisations}') as x(id text, "roleSchemeUri" text, "roles" jsonb)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.raid_organisation_role (raid_organisation_id, organisation_role_id, start_date, end_date)
select (select id
        from api_svc.raid_organisation
        where organisation_id = (select id from api_svc.organisation where pid = r.ror) and handle = r.handle) as raid_organisation_id,
       (select id from api_svc.organisation_role where uri = r.id) as organisation_role_id,
       "startDate" as start_date,
       "endDate" as end_date
from (select handle, x.id as ror, xx.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{organisation}') as x(id text, "schemaUri" text, "role" jsonb),
           lateral jsonb_to_recordset(x.role) as xx(id text, "schemaUri" text, "startDate" text, "endDate" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.raid_organisation_role (raid_organisation_id, organisation_role_id, start_date, end_date)
select (select id
        from api_svc.raid_organisation
        where organisation_id = (select id from api_svc.organisation where pid = r.ror) and handle = r.handle) as raid_organisation_id,
       (select case
                   when role = 'Contractor' then 1
                   when role = 'Lead Research Organisation' then 2
                   when role = 'Other Organisation' then 3
                   when role = 'Other Research Organisation' then 4
                   when role = 'Partner Organisation' then 5
                   end
       ) as organisation_role_id,
       "startDate" as start_date,
       "endDate" as end_date
from (select handle, x.id as ror, xx.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{organisations}') as x(id text, "identifierSchemeUri" text, "roles" jsonb),
           lateral jsonb_to_recordset(x.roles) as xx(role text, "roleSchemeUri" text, "startDate" text, "endDate" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

-- /INSERT ORGANISATIONS

-- INSERT RELATED OBJECTS
insert into api_svc.related_object (pid, schema_id)
select
    id as pid,
    (select id from api_svc.related_object_schema where uri = "schemaUri") as schema_id
from (select x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedObject}') as x(id text, "schemaUri" text, type jsonb, category jsonb)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2' and metadata ->> 'relatedObject' is not null) r
on conflict do nothing;

insert into api_svc.related_object (pid, schema_id)
select
    "relatedObject" as pid,
    (select id from api_svc.related_object_schema where uri = "relatedObjectSchemeUri") as schema_id
from (select x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedObjects}') as x("relatedObject" text, "relatedObjectType" text, "relatedObjectCategory" text, "relatedObjectSchemeUri" text, "relatedObjectTypeSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2' and metadata ->> 'relatedObjects' is not null) r
on conflict do nothing;

insert into api_svc.raid_related_object (handle, related_object_id, related_object_type_id)
select
    handle as handle,
    (select id from api_svc.related_object where pid = related_object_id) as related_object_id,
    (select id from api_svc.related_object_type where uri = r.related_object_type_id) as related_object_type_id
from (select handle, x.id as related_object_id, xx.id as related_object_type_id
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedObject}') as x(id text, "schemaUri" text, type jsonb, category jsonb),
           lateral jsonb_to_record(type) as xx(id text, "schemaUri" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2' and metadata ->> 'relatedObject' is not null) r
on conflict do nothing;

insert into api_svc.raid_related_object (handle, related_object_id, related_object_type_id)
select
    handle as handle,
    (select id from api_svc.related_object where pid = related_object_id) as related_object_id,
    (select id from api_svc.related_object_type where uri = related_object_type_id) as related_object_type_id
from (select handle, "relatedObject" as related_object_id, "relatedObjectType" as related_object_type_id
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedObjects}') as x("relatedObject" text, "relatedObjectType" text, "relatedObjectCategory" text, "relatedObjectSchemeUri" text, "relatedObjectTypeSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2' and metadata ->> 'relatedObjects' is not null) r
on conflict do nothing;

insert into api_svc.raid_related_object_category (raid_related_object_id, related_object_category_id)
select (select rro.id
        from api_svc.raid_related_object rro
                 join api_svc.related_object ro on rro.related_object_id = ro.id
                 join api_svc.related_object_type rot on rro.related_object_type_id = rot.id
        where rro.handle = r.handle
          and ro.pid = r.related_object_id
          and rot.uri = r.related_object_type_id) as raid_related_object_id,
       (select id
        from api_svc.related_object_category
        where uri = related_object_category_id)   as related_object_category_id
from (select handle,
             x.id        as related_object_id,
             category.id as related_object_category_id,
             type.id     as related_object_type_id
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedObject}') as x(id text, "schemaUri" text, type jsonb, category jsonb),
           lateral jsonb_to_record(category) as category(id text, "schemaUri" text),
           lateral jsonb_to_record(type) as type(id text, "schemaUri" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'relatedObject' is not null) r
on conflict do nothing;



insert into api_svc.raid_related_object_category (raid_related_object_id, related_object_category_id)
select (select rro.id
        from api_svc.raid_related_object rro
                 join api_svc.related_object ro on rro.related_object_id = ro.id
                 join api_svc.related_object_type rot on rro.related_object_type_id = rot.id
        where rro.handle = r.handle
          and ro.pid = r.related_object_id
          and rot.uri = r.related_object_type) as raid_related_object_id,
       (select case
                   when related_object_category = 'Input' then 1
                   when related_object_category = 'Output' then 3
                   else 2
                   end)                        as related_object_category_id
from (select handle,
             "relatedObject"         as related_object_id,
             "relatedObjectCategory" as related_object_category,
             x."relatedObjectType"   as related_object_type
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedObjects}') as x("relatedObject" text,
                                                                                "relatedObjectType" text,
                                                                                "relatedObjectCategory" text,
                                                                                "relatedObjectSchemeUri" text,
                                                                                "relatedObjectTypeSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'relatedObjects' is not null) r
on conflict do nothing;



-- /INSERT RELATED OBJECTS

-- INSERT ALTERNATE IDENTIFIERS
insert into api_svc.raid_alternate_identifier (handle, id, type)
select handle, id,     type
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{alternateIdentifier}') as x("id" text, "type" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2' and metadata ->> 'alternateIdentifier' is not null) r
on conflict do nothing;

insert into api_svc.raid_alternate_identifier (handle, id, type)
select handle, "alternateIdentifier", "alternateIdentifierType"
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{alternateIdentifiers}') as x("alternateIdentifier" text, "alternateIdentifierType" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'alternateIdentifiers' is not null) r
on conflict do nothing;

-- /INSERT ALTERNATE IDENTIFIERS
-- INSERT ALTERNATE URLS

insert into api_svc.raid_alternate_url (handle, url)
select handle, "url"
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{alternateUrl}') as x("url" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'alternateUrl' is not null) r
on conflict do nothing;

insert into api_svc.raid_alternate_url (handle, url)
select handle, "url"
from (select handle, x.*
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{alternateUrls}') as x("url" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'alternateUrls' is not null) r
on conflict do nothing;

-- /INSERT ALTERNATE URLS

-- INSERT RELATED RAIDS
insert into api_svc.related_raid (handle, related_raid_handle, related_raid_type_id)
select handle, id,
       (select id from api_svc.related_raid_type where uri = related_raid_type_id)
from (select handle, x.id, xx.id as related_raid_type_id
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedRaid}') as x(id text, type jsonb),
           lateral jsonb_to_record(x.type) as xx(id text, type text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'relatedRaid' is not null) r
on conflict do nothing;

insert into api_svc.related_raid (handle, related_raid_handle, related_raid_type_id)
select handle, id,
       (select id from api_svc.related_raid_type where uri = r.related_raid_type_id)
from (select handle, x."relatedRaid" as id, "relatedRaidType" as related_raid_type_id
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{relatedRaids}') as x("relatedRaid" text, "relatedRaidType" text, "relatedRaidTypeSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'relatedRaids' is not null) r
on conflict do nothing;

-- /INSERT RELATED RAIDS
-- INSERT SUBJECTS

insert into api_svc.raid_subject (handle, subject_type_id)
select handle,
       (select id from api_svc.subject_type where r.id like '%' || id)
from (select handle, x.id
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{subject}') as x(id text, "schemaUri" text, keyword jsonb)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'subject' is not null) r
on conflict do nothing;

insert into api_svc.raid_subject (handle, subject_type_id)
select handle,
       (select id from api_svc.subject_type where r.id like '%' || id)
from (select handle, x.subject as id
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{subjects}') as x(subject text, "subjectKeyword" text, "subjectSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'subjects' is not null) r
on conflict do nothing;

insert into api_svc.raid_subject_keyword (raid_subject_id, keyword, language_id)
select (select id
        from api_svc.raid_subject
        where handle = r.handle and r.subject_type_id like '%' || subject_type_id) as raid_subject_id,
       r.keyword_text                                                                 as keyword,
       (select id from api_svc.language where code = r.language_code)                 as language_id
from (select handle, x.id as subject_type_id, xx.text as keyword_text, xxx.id as language_code
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{subject}') as x(id text, "schemaUri" text, keyword jsonb),
           lateral jsonb_to_recordset(keyword) as xx("text" text, language jsonb),
           lateral jsonb_to_record(language) as xxx(id text, "schemaUri" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'subject' is not null
      group by handle, x.id, xx.text, xxx.id
      having xx.text is not null) r
on conflict do nothing;

insert into api_svc.raid_subject_keyword (raid_subject_id, keyword)
select (select id
        from api_svc.raid_subject
        where handle = r.handle and r.subject_type_id like '%' || subject_type_id) as raid_subject_id,
       r.keyword_text                                                                 as keyword
from (select handle, subject as subject_type_id, "subjectKeyword" as keyword_text
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{subjects}') as x(subject text, "subjectKeyword" text, "subjectSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'subjects' is not null) r
on conflict do nothing;

-- /INSERT SUBJECTS

-- INSERT TRADITIONAL KNOWLEDGE LABELS

insert into api_svc.raid_traditional_knowledge_label (handle, traditional_knowledge_label_id, traditional_knowledge_label_schema_id)
select handle as handle,
       case when r.id is null then null
            else (select id from api_svc.traditional_knowledge_label where uri = r.id)
           end as traditional_knowledge_label_id,
       (select id from api_svc.traditional_knowledge_label_schema where uri = r."schemaUri") as traditional_knowledge_label_schema_id
from (select handle, id, "schemaUri"
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{traditionalKnowledgeLabel}') as x(id text, "schemaUri" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'traditionalKnowledgeLabel' is not null) r
on conflict do nothing;

insert into api_svc.raid_traditional_knowledge_label (handle, traditional_knowledge_label_schema_id)
select handle as handle,
       (select id from api_svc.traditional_knowledge_label_schema where uri = r."traditionalKnowledgeLabelSchemeUri") as traditional_knowledge_label_schema_id
from (select handle, "traditionalKnowledgeLabelSchemeUri"
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{traditionalKnowledgeLabels}') as x("traditionalKnowledgeLabelSchemeUri" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'traditionalKnowledgeLabels' is not null) r
on conflict do nothing;

-- /INSERT TRADITIONAL KNOWLEDGE LABELS

-- INSERT SPATIAL COVERAGE

insert into api_svc.raid_spatial_coverage (handle, uri, schema_id)
select handle as handle,
       r.uri,
       (select id from api_svc.spatial_coverage_schema where uri = r."schemaUri") as schema_id
from (select handle, x.id as uri, x."schemaUri"
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{spatialCoverage}') as x(id text, "schemaUri" text, place text, language jsonb),
           lateral jsonb_to_record(x.language) as xx(id text, "schemaUri" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'spatialCoverage' is not null) r
on conflict do nothing;

insert into api_svc.raid_spatial_coverage_place (raid_spatial_coverage_id, place, language_id)
select (select rsc.id from api_svc.raid_spatial_coverage rsc join api_svc.spatial_coverage_schema scs
                  on rsc.schema_id = scs.id
                  where handle = r.handle and rsc.uri = r.uri and scs.uri = r."schemaUri") as raid_spatial_coverage_id,
       place,
       (select id from api_svc.language where code = r.language_code) as language_id
from (select handle, x.id as uri, x."schemaUri", place, xx.id as language_code
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{spatialCoverage}') as x(id text, "schemaUri" text, place text, language jsonb),
           lateral jsonb_to_record(x.language) as xx(id text, "schemaUri" text)
      where api_svc.raid.metadata_schema = 'raido-metadata-schema-v2'
        and metadata ->> 'spatialCoverage' is not null) r
on conflict do nothing;

insert into api_svc.raid_spatial_coverage (handle, uri, schema_id)
select handle as handle,
       "spatialCoverage",
       (select id from api_svc.spatial_coverage_schema where uri = r."spatialCoverageSchemeUri") as schema_id
from (select handle, x."spatialCoverage", x."spatialCoverageSchemeUri"
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{spatialCoverages}') as x("spatialCoverage" text, "spatialCoverageSchemeUri" text, "spatialCoveragePlace" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'spatialCoverages' is not null) r
on conflict do nothing;

insert into api_svc.raid_spatial_coverage_place (raid_spatial_coverage_id, place)
select (select rsc.id
        from api_svc.raid_spatial_coverage rsc
                 join api_svc.spatial_coverage_schema scs
                      on rsc.schema_id = scs.id
        where handle = r.handle
          and rsc.uri = r."spatialCoverage"
          and scs.uri = r."spatialCoverageSchemeUri") as raid_spatial_coverage_id,
       r."spatialCoveragePlace"
from (select handle, x."spatialCoverage", x."spatialCoverageSchemeUri", "spatialCoveragePlace"
      from api_svc.raid,
           jsonb_to_recordset(api_svc.raid.metadata #> '{spatialCoverages}') as x("spatialCoverage" text,
                                                                                  "spatialCoverageSchemeUri" text,
                                                                                  "spatialCoveragePlace" text)
      where api_svc.raid.metadata_schema <> 'raido-metadata-schema-v2'
        and metadata ->> 'spatialCoverages' is not null) r
on conflict do nothing;

-- /INSERT SPATIAL COVERAGE

-- UPDATE RAID

insert into api_svc.organisation (pid, schema_id)
select r.id, 1
from (select identifier_registration_agency.id as id
      from api_svc.raid,
           jsonb_to_record(metadata -> 'identifier') as identifier(id text, "schemaUri" text,
                                                                   "registrationAgency" jsonb,
                                                                   owner jsonb, license text, version int),
           lateral jsonb_to_record(identifier."registrationAgency") as identifier_registration_agency(id text, "schemaUri" text, "servicePoint" int)
      where metadata_schema = 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.organisation (pid, schema_id)
select r.id, 1 as schema_id
from (select identifier_owner.id as id
      from api_svc.raid,
           jsonb_to_record(metadata -> 'identifier') as identifier(id text, "schemaUri" text,
                                                                   "registrationAgency" jsonb,
                                                                   owner jsonb, license text, version int),
           lateral jsonb_to_record(identifier.owner) as identifier_owner(id text, "schemaUri" text)
      where metadata_schema = 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.organisation (pid, schema_id)
select distinct r.id, 1
from (select identifier."identifierRegistrationAgency" as id
      from api_svc.raid,
           jsonb_to_record(metadata -> 'id') as identifier(identifier text, "identifierSchemeUri" text, "identifierRegistrationAgency" text,
                                                           "identifierOwner" text, identifierServicePoint text, globalUrl text, raidAgencyUrl text, version int)
      where metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

insert into api_svc.organisation (pid, schema_id)
select distinct r.id, 1 as schema_id
from (select identifier."identifierOwner" as id
      from api_svc.raid,
           jsonb_to_record(metadata -> 'id') as identifier(identifier text, "identifierSchemeUri" text, "identifierRegistrationAgency" text,
                                                           "identifierOwner" text, identifierServicePoint text, globalUrl text, raidAgencyUrl text, version int)
      where metadata_schema <> 'raido-metadata-schema-v2') r
on conflict do nothing;

update api_svc.raid set
                        start_date_string = r.start_date,
                        end_date = r.end_date,
                        access_type_id = (select id from api_svc.access_type where uri = r.access_type_id),
                        embargo_expiry = r.embargo_expiry,
                        access_statement = r.access_statement_text,
                        access_statement_language_id = (select id from api_svc.language where code = r.access_statement_language_code),
                        schema_uri = r.schema_uri,
                        registration_agency_organisation_id = (select id from api_svc.organisation where pid = r.identifier_registration_agency_id),
                        owner_organisation_id = (select id from api_svc.organisation where pid = r.identifier_owner_id),
                        license = r.license
from
    (select handle,
            date."startDate" as start_date,
            date."endDate" as end_date,
            access_type.id as access_type_id,
            access."embargoExpiry" as embargo_expiry,
            access_statement.text as access_statement_text,
            access_statement_language.id as access_statement_language_code,
            identifier."schemaUri" as schema_uri,
            identifier_registration_agency.id as identifier_registration_agency_id,
            identifier_owner.id as identifier_owner_id,
            identifier.license as license
     from api_svc.raid,
          jsonb_to_record(metadata -> 'identifier') as identifier(id text, "schemaUri" text, "registrationAgency" jsonb,
                                                                  owner jsonb, license text, version int),
          lateral jsonb_to_record(identifier."registrationAgency") as identifier_registration_agency(id text, "schemaUri" text, "servicePoint" int),
          lateral jsonb_to_record(identifier.owner) as identifier_owner(id text, "schemaUri" text),
          lateral jsonb_to_record(metadata -> 'date') as date("startDate" text, "endDate" text),
          lateral jsonb_to_record(metadata -> 'access') as access(type jsonb, "embargoExpiry" date, "accessStatement" jsonb),
          lateral jsonb_to_record(access.type) as access_type(id text, "schemaUri" text),
          lateral jsonb_to_record(access."accessStatement") as access_statement(text text, language jsonb),
          lateral jsonb_to_record(access_statement.language) as access_statement_language(id text, "schemaUri" text)
     where metadata_schema = 'raido-metadata-schema-v2') r
where api_svc.raid.handle = r.handle;

update api_svc.raid
set start_date_string                   = r.start_date,
    end_date                            = r.end_date,
    access_type_id                      = case
                                              when r.access_type = 'Open' then 1
                                              else 3 end,
    access_statement                    = r.access_statement_text,
    schema_uri                          = r.schema_uri,
    registration_agency_organisation_id = (select id
                                           from api_svc.organisation
                                           where pid = r.identifier_registration_agency_id),
    owner_organisation_id               = (select id from api_svc.organisation where pid = r.identifier_owner_id),
    license                             = 'Creative Commons CC-0'
from (select handle,
             date."startDate"                          as start_date,
             date."endDate"                            as end_date,
             access.type                               as access_type,
             access."accessStatement"                  as access_statement_text,
             identifier."identifierSchemeUri"          as schema_uri,
             identifier."identifierRegistrationAgency" as identifier_registration_agency_id,
             identifier."identifierOwner"              as identifier_owner_id
      from api_svc.raid,
           jsonb_to_record(metadata -> 'id') as identifier(identifier text, "identifierSchemeUri" text,
                                                           "identifierRegistrationAgency" text,
                                                           "identifierOwner" text, identifierServicePoint text,
                                                           globalUrl text, raidAgencyUrl text, version int),
           lateral jsonb_to_record(metadata -> 'dates') as date("startDate" text, "endDate" text),
           lateral jsonb_to_record(metadata -> 'access') as access(type text, "accessStatement" text)
      where metadata_schema <> 'raido-metadata-schema-v2') r
where api_svc.raid.handle = r.handle;

create table api_svc.team
(
    id               varchar primary key,
    name             varchar not null,
    prefix           varchar not null,
    service_point_id bigint  not null references api_svc.service_point (id),
    unique(prefix));

create table api_svc.team_user
(
    app_user_id bigint  not null references api_svc.app_user (id) on delete cascade,
    team_id     varchar not null references api_svc.team (id) on delete cascade,
    unique (app_user_id, team_id)
);

-- /UPDATE RAID

END TRANSACTION;
