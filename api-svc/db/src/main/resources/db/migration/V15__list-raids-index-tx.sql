drop index if exists idx_raid_service_point_id_date_created;

-- Flyway is currently broken for using "concurrently", it will hang if you try
-- https://github.com/flyway/flyway/issues/3508
-- https://github.com/flyway/flyway/issues/3684
create index idx_raid_service_point_id_date_created
  on raid(service_point_id, date_created desc);

comment on index idx_raid_service_point_id_date_created is
'created because load-testing showed the query for the experimental list-raid'
' endpoint was doing a full table scan';

analyze raid;

