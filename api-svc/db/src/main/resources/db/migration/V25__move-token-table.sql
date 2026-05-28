CREATE TABLE token (
    name text NOT NULL,
    environment text NOT NULL,
    date_created timestamp without time zone NOT NULL,
    token text NOT NULL,
    s3_export jsonb NOT NULL
);


COMMENT ON TABLE token IS 'from arn:aws:dynamodb:ap-southeast-2:005299621378:table/RAiD-TokenTable-1P6MFZ0WFEETH';

ALTER TABLE ONLY token
    ADD CONSTRAINT token_pkey PRIMARY KEY (name, environment, date_created);


insert into token
select *
from raid_v1_import.token;
