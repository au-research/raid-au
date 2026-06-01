update raido.service_point set app_writes_enabled = true;

alter table raido.service_point alter column app_writes_enabled set not null;
