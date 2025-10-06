ALTER TABLE api_svc.subject_type
    DROP CONSTRAINT subject_pkey CASCADE;

ALTER TABLE api_svc.raid_subject
    DROP COLUMN subject_type_id;

ALTER TABLE api_svc.raid_subject
    ADD COLUMN subject_type_id INTEGER;

UPDATE api_svc.raid_subject SET subject_type_id = 1;

ALTER TABLE api_svc.subject_type
    RENAME COLUMN id TO subject_type_id;

ALTER TABLE api_svc.subject_type
    ADD COLUMN id SERIAL PRIMARY KEY;

ALTER TABLE api_svc.raid_subject
    ADD CONSTRAINT raid_subject_subject_type_id FOREIGN KEY (subject_type_id) REFERENCES subject_type (id);

INSERT INTO api_svc.subject_type
SELECT subject_type_id, name, description, note, 3 AS schema_id FROM api_svc.subject_type;