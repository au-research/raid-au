alter table api_svc.service_point
    add column created timestamp with time zone default current_timestamp,
    add column updated timestamp with time zone default current_timestamp;
