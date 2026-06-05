-- access type
alter table raido.access_type
    rename column scheme_id to schema_id;

alter table raido.access_type
    rename constraint fk_access_type_scheme_id to fk_access_type_schema_id;

alter table raido.access_type_scheme
    rename constraint access_type_scheme_pkey to access_type_schema_pkey;

alter table raido.access_type_scheme rename to access_type_schema;

-- contributor position
alter table raido.contributor_position
    rename column scheme_id to schema_id;

alter table raido.contributor_position
    rename constraint fk_access_type_scheme_id to fk_contributor_position_schema_id;

alter table raido.contributor_position_scheme
    rename constraint contributor_position_scheme_pkey to contributor_position_schema_pkey;

alter table raido.contributor_position_scheme
    rename to contributor_position_schema;

-- contributor role
alter table raido.contributor_role
    rename column scheme_id to schema_id;

alter table raido.contributor_role
    rename constraint fk_contributor_role_type_scheme_id to fk_contributor_role_schema_id;

alter table raido.contributor_role_scheme
    rename constraint contributor_role_scheme_pkey to contributor_role_schema_pkey;

alter table raido.contributor_role_scheme
    rename to contributor_role_schema;

-- description type
alter table raido.description_type
    rename column scheme_id to schema_id;

alter table raido.description_type
    rename constraint fk_description_type_scheme_id to fk_description_type_schema_id;

alter table raido.description_type_scheme
    rename constraint description_type_scheme_pkey to dwscription_type_schema_pkey;

alter table raido.description_type_scheme
    rename to description_type_schema;

-- language
alter table raido.language
    rename column scheme_id to schema_id;

alter table raido.language
    rename constraint language_scheme_id to fk_language_schema_id;

alter table raido.language_scheme
    rename constraint language_scheme_pkey to language_schema_pkey;

alter table raido.language_scheme
    rename to language_schema;

-- organisation role
alter table raido.organisation_role
    rename column scheme_id to schema_id;

alter table raido.organisation_role
    rename constraint fk_organisation_role_type_scheme_id to fk_organisation_role_schema_id;

alter table raido.organisation_role_scheme
    rename constraint organisation_role_scheme_pkey to organisation_role_schema_pkey;

alter table raido.organisation_role_scheme
    rename to organisation_role_schema;

--related object category
alter table raido.related_object_category
    rename column scheme_id to schema_id;

alter table raido.related_object_category
    rename constraint fk_related_object_category_type_scheme_id to fk_related_object_category_schema_id;

alter table raido.related_object_category_scheme
    rename constraint related_object_category_scheme_pkey to related_object_category_schema_pkey;

alter table raido.related_object_category_scheme
    rename to related_object_category_schema;

-- related object type
alter table raido.related_object_type
    rename column scheme_id to schema_id;

alter table raido.related_object_type
    rename constraint fk_related_object_type_scheme_id to fk_related_object_type_schema_id;

alter table raido.related_object_type_scheme
    rename constraint related_object_type_scheme_pkey to related_object_type_schema_pkey;

alter table raido.related_object_type_scheme
    rename to related_object_type_schema;

-- related raid type
alter table raido.related_raid_type
    rename column scheme_id to schema_id;

alter table raido.related_raid_type
    rename constraint fk_related_raid_type_scheme_id to fk_related_raid_type_schema_id;

alter table raido.related_raid_type_scheme
    rename constraint related_raid_type_scheme_pkey to related_raid_type_schema_pkey;

alter table raido.related_raid_type_scheme
    rename to related_raid_type_schema;

-- subject type
 alter table raido.subject_type
    rename column scheme_id to schema_id;

alter table raido.subject_type
    add constraint fk_subject_type_schema_id foreign key (schema_id) references subject_type_scheme (id);

alter table raido.subject_type_scheme
    rename constraint subject_type_scheme_pkey to subject_type_schema_pkey;

alter table raido.subject_type_scheme
    rename to subject_type_schema;

-- title type
alter table raido.title_type
    rename column scheme_id to schema_id;

alter table raido.title_type
    rename constraint fk_title_type_scheme_id to fk_title_type_schema_id;

alter table raido.title_type_scheme
    rename constraint title_type_scheme_pkey to title_type_schema_pkey;

alter table raido.title_type_scheme
    rename to title_type_schema;





