begin transaction;

drop table if exists team_user;
drop table if exists team;

alter table service_point
    add column group_id char(36);

end transaction;