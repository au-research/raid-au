begin transaction;

alter table contributor
    add column uuid varchar unique,
    add column status varchar,
    alter column pid drop not null;

end transaction;