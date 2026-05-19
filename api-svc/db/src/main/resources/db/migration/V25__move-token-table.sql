CREATE TABLE api_svc.token (
    name text NOT NULL,
    environment text NOT NULL,
    date_created timestamp without time zone NOT NULL,
    token text NOT NULL,
    s3_export jsonb NOT NULL
);

COMMENT ON TABLE api_svc.token IS 'from arn:aws:dynamodb:ap-southeast-2:005299621378:table/RAiD-TokenTable-1P6MFZ0WFEETH';

ALTER TABLE ONLY api_svc.token
    ADD CONSTRAINT token_pkey PRIMARY KEY (name, environment, date_created);

insert into api_svc.token
select *
from raid_v1_import.token;
