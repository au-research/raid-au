begin transaction;

alter table raid_contributor
    drop constraint raid_contributor_handle_contributor_id_key;

end transaction;
