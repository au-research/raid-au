begin transaction;

-- title

drop table if exists raido.title_type_scheme;
create table raido.title_type_scheme (
   id serial primary key,
   uri varchar not null
);

drop table if exists raido.title_type;
create table raido.title_type
(
    scheme_id int     not null,
    uri      varchar not null,
    primary key (scheme_id, uri),
    constraint fk_title_type_scheme_id foreign key (scheme_id) references raido.title_type_scheme (id)
);

insert into raido.title_type_scheme (uri)
values ('https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1');

insert into raido.title_type (scheme_id, uri) values
   (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/alternative.json'),
   (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json');

-- end title
-- description

drop table if exists raido.description_type_scheme;
create table raido.description_type_scheme (
    id serial primary key,
    uri varchar not null
);

drop table if exists raido.description_type;
create table raido.description_type
(
    scheme_id int     not null,
    uri      varchar not null,
    primary key (scheme_id, uri),
    constraint fk_description_type_scheme_id foreign key (scheme_id) references raido.description_type_scheme (id)
);

insert into raido.description_type_scheme (uri) values
    ('https://github.com/au-research/raid-metadata/tree/main/scheme/description/type/v1');

insert into raido.description_type (scheme_id, uri) values
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/alternative.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/primary.json');

-- end description
-- access

drop table if exists raido.access_type_scheme;
create table raido.access_type_scheme (
    id serial primary key,
    uri varchar not null
);

drop table if exists raido.access_type;
create table raido.access_type
(
    scheme_id int     not null,
    uri      varchar not null,
    primary key (scheme_id, uri),
    constraint fk_access_type_scheme_id foreign key (scheme_id) references raido.access_type_scheme (id)
);

insert into raido.access_type_scheme (uri) values
    ('https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1');

insert into raido.access_type (scheme_id, uri) values
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json');

-- end access
-- contributor

drop table if exists raido.contributor_position_scheme;
create table raido.contributor_position_scheme (
    id serial primary key,
    uri varchar not null
);

drop table if exists raido.contributor_position;
create table raido.contributor_position
(
    scheme_id int     not null,
    uri      varchar not null,
    primary key (scheme_id, uri),
    constraint fk_access_type_scheme_id foreign key (scheme_id) references raido.access_type_scheme (id)
);

insert into raido.contributor_position_scheme (uri) values
    ('https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1');

insert into raido.contributor_position (scheme_id, uri) values
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/co-investigator.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact-person.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/other-participant.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json');

drop table if exists raido.contributor_role_scheme;
create table raido.contributor_role_scheme (
    id serial primary key,
    uri varchar not null
);

drop table if exists raido.contributor_role;
create table raido.contributor_role (
    scheme_id int     not null,
    uri      varchar not null,
    primary key (scheme_id, uri),
    constraint fk_contributor_role_type_scheme_id foreign key (scheme_id) references raido.contributor_role_scheme (id)
);

insert into raido.contributor_role_scheme (uri) values
    ('https://credit.niso.org/');

insert into raido.contributor_role (scheme_id, uri) values
    (1, 'https://credit.niso.org/contributor-roles/conceptualization/'),
    (1, 'https://credit.niso.org/contributor-roles/data-curation/'),
    (1, 'https://credit.niso.org/contributor-roles/formal-analysis/'),
    (1, 'https://credit.niso.org/contributor-roles/funding-acquisition/'),
    (1, 'https://credit.niso.org/contributor-roles/investigation/'),
    (1, 'https://credit.niso.org/contributor-roles/methodology/'),
    (1, 'https://credit.niso.org/contributor-roles/project-administration/'),
    (1, 'https://credit.niso.org/contributor-roles/resources/'),
    (1, 'https://credit.niso.org/contributor-roles/software/'),
    (1, 'https://credit.niso.org/contributor-roles/supervision/'),
    (1, 'https://credit.niso.org/contributor-roles/validation/'),
    (1, 'https://credit.niso.org/contributor-roles/visualization/'),
    (1, 'https://credit.niso.org/contributor-roles/writing-original-draft/'),
    (1, 'https://credit.niso.org/contributor-roles/writing-review-editing/');

-- end contributor
-- organisation

drop table if exists raido.organisation_role_scheme;
create table raido.organisation_role_scheme (
    id serial primary key,
    uri varchar not null
);

drop table if exists raido.organisation_role;
create table raido.organisation_role
(
    scheme_id int     not null,
    uri      varchar not null,
    primary key (scheme_id, uri),
    constraint fk_organisation_role_type_scheme_id foreign key (scheme_id) references raido.organisation_role_scheme (id)
);

insert into raido.organisation_role_scheme (uri) values
    ('https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1');

insert into raido.organisation_role (scheme_id, uri) values
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/contractor.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-organisation.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-research-organisation.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/partner-organisation.json');

-- end organisation
-- related object

drop table if exists raido.related_object_category_scheme;
create table raido.related_object_category_scheme (
    id serial primary key,
    uri varchar not null
);

drop table if exists raido.related_object_category;
create table raido.related_object_category
(
    scheme_id int     not null,
    uri      varchar not null,
    primary key (scheme_id, uri),
    constraint fk_related_object_category_type_scheme_id foreign key (scheme_id) references raido.related_object_category_scheme (id)
);

insert into raido.related_object_category_scheme (uri) values
    ('https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1');

insert into raido.related_object_category (scheme_id, uri) values
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/internal.json'),
    (1, 'https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json');

drop table if exists raido.related_object_type_scheme;
create table raido.related_object_type_scheme (
    id serial primary key,
    uri varchar not null
);

create table raido.related_object_type_new
(
    scheme_id   int     not null,
    uri         varchar not null,
    name        varchar,
    description varchar,
    primary key (scheme_id, uri),
    constraint fk_related_object_type_scheme_id foreign key (scheme_id) references raido.related_object_type_scheme (id)
);

insert into raido.related_object_type_scheme (uri) values
    ('https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1');


insert into raido.related_object_type_new (scheme_id, uri, name, description)
select 1, url, name, description
from raido.related_object_type;

drop table raido.related_object_type;
alter table raido.related_object_type_new
    rename to related_object_type;

-- end related object
-- related raid

drop table if exists raido.related_raid_type_scheme;
create table raido.related_raid_type_scheme (
    id serial primary key,
    uri varchar not null
);

create table raido.related_raid_type_new
(
    scheme_id   int     not null,
    uri         varchar not null,
    name        varchar,
    description varchar,
    primary key (scheme_id, uri),
    constraint fk_related_raid_type_scheme_id foreign key (scheme_id) references raido.related_raid_type_scheme (id)
);

insert into raido.related_raid_type_scheme (uri) values
    ('https://github.com/au-research/raid-metadata/tree/main/scheme/related-raid/type/v1');

insert into raido.related_raid_type_new (scheme_id, uri, name, description)
select 1, url, name, description
from raido.related_raid_type;

drop table raido.related_raid_type;
alter table raido.related_raid_type_new
    rename to related_raid_type;

-- end related raid

end transaction;