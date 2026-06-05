begin transaction;

create type schema_status as enum ('active', 'inactive');

alter table access_type_schema
    add column status schema_status;

update access_type_schema
set status = 'inactive'
where id = 1;

insert into access_type_schema (uri, status)
values ('https://vocabularies.coar-repositories.org/access_rights/', 'active');

insert into access_type (uri, schema_id)
values ('https://vocabularies.coar-repositories.org/access_rights/c_abf2/', 2),
       ('https://vocabularies.coar-repositories.org/access_rights/c_f1cf/', 2);

alter table contributor_position_schema
    add column status schema_status;

update contributor_position_schema
set status = 'inactive'
where id = 1;

insert into contributor_position_schema (uri, status)
values ('https://vocabulary.raid.org/contributor.position.schema/305', 'active');

insert into contributor_position (uri, schema_id)
values ('https://vocabulary.raid.org/contributor.position.schema/307', 2),
       ('https://vocabulary.raid.org/contributor.position.schema/308', 2),
       ('https://vocabulary.raid.org/contributor.position.schema/309', 2),
       ('https://vocabulary.raid.org/contributor.position.schema/310', 2),
       ('https://vocabulary.raid.org/contributor.position.schema/311', 2);

alter table contributor_role_schema
    add column status schema_status;

update contributor_role_schema
set status = 'active'
where id = 1;

alter table contributor_schema
    add column status schema_status;

update contributor_schema
set status = 'active'
where id = 1;

insert into contributor_schema (uri, status)
values ('https://isni.org/', 'active');

alter table description_type_schema
    add column status schema_status;

update description_type_schema
set status = 'inactive'
where id = 1;

insert into description_type_schema (uri, status)
values ('https://vocabulary.raid.org/description.type.schema/320', 'active');

insert into description_type (uri, schema_id)
values ('https://vocabulary.raid.org/description.type.schema/3', 2),
       ('https://vocabulary.raid.org/description.type.schema/318', 2),
       ('https://vocabulary.raid.org/description.type.schema/319', 2),
       ('https://vocabulary.raid.org/description.type.schema/6', 2),
       ('https://vocabulary.raid.org/description.type.schema/7', 2),
       ('https://vocabulary.raid.org/description.type.schema/8', 2),
       ('https://vocabulary.raid.org/description.type.schema/9', 2);

alter table organisation_schema
    add column status schema_status;

update organisation_schema
set status = 'active'
where id = 1;

alter table organisation_role_schema
    add column status schema_status;

update organisation_role_schema
set status = 'inactive'
where id = 1;

insert into organisation_role_schema (uri, status)
values ('https://vocabulary.raid.org/organisation.role.schema/359', 'active');

insert into organisation_role (uri, schema_id)
values ('https://vocabulary.raid.org/organisation.role.schema/182', 2),
       ('https://vocabulary.raid.org/organisation.role.schema/183', 2),
       ('https://vocabulary.raid.org/organisation.role.schema/184', 2),
       ('https://vocabulary.raid.org/organisation.role.schema/185', 2),
       ('https://vocabulary.raid.org/organisation.role.schema/186', 2),
       ('https://vocabulary.raid.org/organisation.role.schema/187', 2),
       ('https://vocabulary.raid.org/organisation.role.schema/188', 2);

alter table related_object_schema
    add column status schema_status;

update related_object_schema
set status = 'active'
where id = 1;

insert into related_object_schema (uri, status)
values ('http://hdl.handle.net/', 'active'),
       ('https://archive.org/', 'active'),
       ('https://arks.org/', 'active'),
       ('https://scicrunch.org/resolver/', 'active'),
       ('https://www.isbn-international.org/', 'active');

alter table related_object_type_schema
    add column status schema_status;

update related_object_type_schema
set status = 'inactive'
where id = 1;

insert into related_object_type_schema (uri, status)
values ('https://vocabulary.raid.org/relatedObject.type.schema/329', 'active');

insert into related_object_type (uri, schema_id)
values ('https://vocabulary.raid.org/relatedObject.type.schema/247', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/248', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/249', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/250', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/251', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/252', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/253', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/254', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/255', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/256', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/257', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/258', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/259', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/260', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/261', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/262', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/263', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/264', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/265', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/266', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/267', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/268', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/269', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/270', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/271', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/272', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/273', 2),
       ('https://vocabulary.raid.org/relatedObject.type.schema/274', 2);

alter table related_object_category_schema
    add column status schema_status;

update related_object_category_schema
set status = 'inactive'
where id = 1;

insert into related_object_category_schema (uri, status)
values ('https://vocabulary.raid.org/relatedObject.category.schemaUri/386', 'active');

insert into related_object_category (uri, schema_id)
values ('https://vocabulary.raid.org/relatedObject.category.id/190', 2),
       ('https://vocabulary.raid.org/relatedObject.category.id/191', 2),
       ('https://vocabulary.raid.org/relatedObject.category.id/192', 2);

alter table related_raid_type_schema
    add column status schema_status;

update related_raid_type_schema
set status = 'inactive'
where id = 1;

insert into related_raid_type_schema (uri, status)
values ('https://vocabulary.raid.org/relatedRaid.type.schema/367', 'active');

insert into related_raid_type (uri, schema_id)
values ('https://vocabulary.raid.org/relatedRaid.type.schema/198', 2),
       ('https://vocabulary.raid.org/relatedRaid.type.schema/199', 2),
       ('https://vocabulary.raid.org/relatedRaid.type.schema/200', 2),
       ('https://vocabulary.raid.org/relatedRaid.type.schema/201', 2),
       ('https://vocabulary.raid.org/relatedRaid.type.schema/202', 2),
       ('https://vocabulary.raid.org/relatedRaid.type.schema/203', 2),
       ('https://vocabulary.raid.org/relatedRaid.type.schema/204', 2),
       ('https://vocabulary.raid.org/relatedRaid.type.schema/205', 2);

alter table spatial_coverage_schema
    add column status schema_status;

update spatial_coverage_schema
set status = 'inactive'
where id = 1;

insert into spatial_coverage_schema (uri, status)
values ('https://nominatim.openstreetmap.org/', 'active');

alter table subject_type_schema
    add column status schema_status;

update subject_type_schema
set status = 'inactive'
where id = 1;

insert into subject_type_schema (uri, status)
values ('https://id.loc.gov/authorities/subject.html', 'active'),
       ('https://vocabs.ardc.edu.au/viewById/316', 'active');

alter table title_type_schema
    add column status schema_status;

update title_type_schema
set status = 'inactive'
where id = 1;

insert into title_type_schema (uri, status)
values ('https://vocabulary.raid.org/title.type.schema/376', 'active');

insert into title_type (uri, schema_id)
values ('https://vocabulary.raid.org/title.type.schema/156', 2),
       ('https://vocabulary.raid.org/title.type.schema/157', 2),
       ('https://vocabulary.raid.org/title.type.schema/4', 2),
       ('https://vocabulary.raid.org/title.type.schema/5', 2);

alter table traditional_knowledge_label_schema
    add column status schema_status;

update traditional_knowledge_label_schema
set status = 'active'
where id in (1,2);

create table traditional_knowledge_notice_schema
(
    id     serial primary key,
    uri    varchar not null,
    status schema_status
);

insert into traditional_knowledge_notice_schema (uri, status)
values ('https://localcontexts.org/notice/open-to-collaborate/', 'active'),
       ('https://localcontexts.org/notices/cc-notices/', 'active'),
       ('https://localcontexts.org/notices/disclosure-notices/', 'active');

create table traditional_knowledge_notice
(
    id        serial primary key,
    uri       varchar not null,
    schema_id int references traditional_knowledge_notice_schema (id)
);

insert into traditional_knowledge_notice (uri, schema_id)
values ('https://localcontexts.org/notice/open-to-collaborate/', 1),
       ('https://localcontexts.org/notice/authorization/', 2),
       ('https://localcontexts.org/notice/belonging/', 2),
       ('https://localcontexts.org/notice/caring/', 2),
       ('https://localcontexts.org/notice/gender-aware/', 2),
       ('https://localcontexts.org/notice/leave-undisturbed/', 2),
       ('https://localcontexts.org/notice/safety/', 2),
       ('https://localcontexts.org/notice/viewing/', 2),
       ('https://localcontexts.org/notice/withholding/', 2),
       ('https://localcontexts.org/notice/attribution-incomplete/', 3),
       ('https://localcontexts.org/notice/bc-notice/', 3),
       ('https://localcontexts.org/notice/tk-notice/', 3);

alter table language_schema
    add column status schema_status;

update language_schema
set status = 'inactive'
where id = 1;

insert into language_schema (uri, status)
values ('https://www.iso.org/standard/74575.html', 'active');

insert into language (code, name, schema_id)
select code, name, 2 as schema_id from language where schema_id = 1;

end transaction;