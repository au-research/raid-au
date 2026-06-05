ALTER TYPE raido.metaschema ADD VALUE IF NOT EXISTS 'raido-metadata-schema-v2';

alter table raido.raid add column version int default 1;

-- Update existing metadata to add version to id block with value of 1
update raido.raid r set metadata = (select jsonb_set(to_jsonb(metadata), '{id, version}', '1', true) from raido.raid where handle = r.handle)

