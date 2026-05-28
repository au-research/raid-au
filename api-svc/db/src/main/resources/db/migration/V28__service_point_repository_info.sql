begin transaction;

alter table service_point
    add column repository_id varchar,
    add column prefix        varchar,
    add column password      varchar;

end transaction;