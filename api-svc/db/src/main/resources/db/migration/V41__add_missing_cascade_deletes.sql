-- Add ON DELETE CASCADE to foreign keys that are missing it.

-- raid_title: has FK to raid(handle) but without cascade
alter table api_svc.raid_title
    drop constraint raid_title_handle_fkey,
    add constraint raid_title_handle_fkey
        foreign key (handle) references api_svc.raid (handle) on delete cascade;

-- raid_organisation: has no FK to raid(handle) at all
alter table api_svc.raid_organisation
    add constraint raid_organisation_handle_fkey
        foreign key (handle) references api_svc.raid (handle) on delete cascade;

-- raid_subject_keyword: has FK to raid_subject(id) but without cascade
alter table api_svc.raid_subject_keyword
    drop constraint raid_subject_keyword_raid_subject_id_fkey,
    add constraint raid_subject_keyword_raid_subject_id_fkey
        foreign key (raid_subject_id) references api_svc.raid_subject (id) on delete cascade;

-- Archive and remove all raids with handles beginning with '102.100' or '10378'.
-- These are legacy handles from the old registration agency.

-- Create archive tables (same structure as originals, without foreign keys)
create table api_svc.raid_archive (
    handle              varchar(128) primary key not null,
    service_point_id    bigint not null,
    url                 varchar(512),
    url_index           integer,
    primary_title       varchar(256),
    confidential        boolean,
    metadata_schema     metaschema not null,
    metadata            jsonb,
    start_date          date,
    date_created        timestamp without time zone not null,
    version             int default 1,
    start_date_string   varchar,
    end_date            varchar,
    license             varchar,
    access_type_id      int,
    embargo_expiry      date,
    access_statement    text,
    access_statement_language_id int,
    schema_uri          varchar,
    registration_agency_organisation_id int,
    owner_organisation_id int
);

create table api_svc.raid_history_archive (
    handle      varchar(255) not null,
    revision    int not null,
    change_type varchar(8) not null,
    diff        text not null,
    created     timestamp not null,
    primary key (handle, revision, change_type)
);

-- Archive matching raids
insert into api_svc.raid_archive
select * from api_svc.raid
where handle like '102.100%' or handle like '10378%';

insert into api_svc.raid_history_archive
select * from api_svc.raid_history
where handle like '102.100%' or handle like '10378%';

-- Delete raid_history (no FK to raid)
delete from api_svc.raid_history
where handle like '102.100%' or handle like '10378%';

-- Delete raids (cascades to all related tables)
delete from api_svc.raid
where handle like '102.100%' or handle like '10378%';
