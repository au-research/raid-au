--
-- PostgreSQL database dump
--

-- Dumped from database version 15.2 (Debian 15.2-1.pgdg110+1)
-- Dumped by pg_dump version 16.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


--
-- Name: auth_request_status; Type: TYPE; Schema: api_svc; Owner: postgres
--

CREATE TYPE api_svc.auth_request_status AS ENUM (
    'REQUESTED',
    'APPROVED',
    'REJECTED'
    );



--
-- Name: id_provider; Type: TYPE; Schema: api_svc; Owner: postgres
--

CREATE TYPE api_svc.id_provider AS ENUM (
    'GOOGLE',
    'AAF',
    'RAIDO_API',
    'ORCID'
    );



--
-- Name: metaschema; Type: TYPE; Schema: api_svc; Owner: postgres
--

CREATE TYPE api_svc.metaschema AS ENUM (
    'raido-metadata-schema-v1',
    'legacy-metadata-schema-v1',
    'raido-metadata-schema-v2'
    );



--
-- Name: user_role; Type: TYPE; Schema: api_svc; Owner: postgres
--

CREATE TYPE api_svc.user_role AS ENUM (
    'OPERATOR',
    'SP_ADMIN',
    'SP_USER',
    'API'
    );



SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: access_type; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.access_type (
                                     schema_id integer NOT NULL,
                                     uri character varying NOT NULL
);



--
-- Name: access_type_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.access_type_schema (
                                            id integer NOT NULL,
                                            uri character varying NOT NULL
);



--
-- Name: access_type_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.access_type_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: access_type_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.access_type_scheme_id_seq OWNED BY api_svc.access_type_schema.id;


--
-- Name: app_user; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.app_user (
                                  id bigint NOT NULL,
                                  service_point_id bigint NOT NULL,
                                  email character varying(256) NOT NULL,
                                  client_id character varying(256) NOT NULL,
                                  subject character varying(256) NOT NULL,
                                  id_provider api_svc.id_provider NOT NULL,
                                  role api_svc.user_role NOT NULL,
                                  enabled boolean DEFAULT true NOT NULL,
                                  token_cutoff timestamp without time zone,
                                  date_created timestamp without time zone DEFAULT transaction_timestamp() NOT NULL
);



--
-- Name: COLUMN app_user.email; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.app_user.email IS 'should be renamed to "description" or some such.  api-keys do not and orcid
  sign-ins might not have email address';


--
-- Name: COLUMN app_user.id_provider; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.app_user.id_provider IS 'not a real identity field, its just redundant info we figure it out from
  the clientId or issuer and store it for easy analysis';


--
-- Name: COLUMN app_user.token_cutoff; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.app_user.token_cutoff IS 'Any endpoint call with a bearer token issued after this point will be
  rejected. Any authentication attempt after this point will be rejected.';


--
-- Name: app_user_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

ALTER TABLE api_svc.app_user ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME api_svc.app_user_id_seq
    START WITH 1000000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    );


--
-- Name: contributor_position; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.contributor_position (
                                              schema_id integer NOT NULL,
                                              uri character varying NOT NULL
);



--
-- Name: contributor_position_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.contributor_position_schema (
                                                     id integer NOT NULL,
                                                     uri character varying NOT NULL
);



--
-- Name: contributor_position_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.contributor_position_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: contributor_position_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.contributor_position_scheme_id_seq OWNED BY api_svc.contributor_position_schema.id;


--
-- Name: contributor_role; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.contributor_role (
                                          schema_id integer NOT NULL,
                                          uri character varying NOT NULL
);



--
-- Name: contributor_role_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.contributor_role_schema (
                                                 id integer NOT NULL,
                                                 uri character varying NOT NULL
);



--
-- Name: contributor_role_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.contributor_role_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: contributor_role_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.contributor_role_scheme_id_seq OWNED BY api_svc.contributor_role_schema.id;


--
-- Name: description_type; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.description_type (
                                          schema_id integer NOT NULL,
                                          uri character varying NOT NULL
);



--
-- Name: description_type_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.description_type_schema (
                                                 id integer NOT NULL,
                                                 uri character varying NOT NULL
);



--
-- Name: description_type_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.description_type_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: description_type_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.description_type_scheme_id_seq OWNED BY api_svc.description_type_schema.id;


--
-- Name: language; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.language (
                                  id character varying NOT NULL,
                                  name character varying NOT NULL,
                                  schema_id integer NOT NULL
);



--
-- Name: language_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.language_schema (
                                         id integer NOT NULL,
                                         uri character varying NOT NULL
);



--
-- Name: language_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.language_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: language_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.language_scheme_id_seq OWNED BY api_svc.language_schema.id;


--
-- Name: organisation_role; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.organisation_role (
                                           schema_id integer NOT NULL,
                                           uri character varying NOT NULL
);



--
-- Name: organisation_role_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.organisation_role_schema (
                                                  id integer NOT NULL,
                                                  uri character varying NOT NULL
);



--
-- Name: organisation_role_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.organisation_role_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: organisation_role_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.organisation_role_scheme_id_seq OWNED BY api_svc.organisation_role_schema.id;


--
-- Name: raid; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.raid (
                              handle character varying(128) NOT NULL,
                              service_point_id bigint NOT NULL,
                              url character varying(512) NOT NULL,
                              url_index integer NOT NULL,
                              primary_title character varying(256) NOT NULL,
                              confidential boolean NOT NULL,
                              metadata_schema api_svc.metaschema NOT NULL,
                              metadata jsonb NOT NULL,
                              start_date date DEFAULT transaction_timestamp() NOT NULL,
                              date_created timestamp without time zone DEFAULT transaction_timestamp() NOT NULL,
                              version integer DEFAULT 1
);



--
-- Name: COLUMN raid.handle; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.raid.handle IS 'Holds the handle (i.e. just prefix/suffix) not the URL.  Usually quite  short in production, but the max length is set to accommodate int and load testing.';


--
-- Name: COLUMN raid.url; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.raid.url IS 'The value that we set as the `URL` property via ARDC APIDS.
  Example: `https://demo.raido-infra.com/raid/123.456/789`.
  The global handle regisrty url (e.g. `https://hdl.handle.net/123.456/789`)
  will redirect to this value.';


--
-- Name: COLUMN raid.url_index; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.raid.url_index IS 'The `index` of the URL property in APIDS. This can be different if we change
  how we mint URL values via APIDS.';


--
-- Name: COLUMN raid.metadata_schema; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.raid.metadata_schema IS 'Identifies the structure of the data in the metadata column';


--
-- Name: raido_operator; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.raido_operator (
    email character varying(256) NOT NULL
);



--
-- Name: TABLE raido_operator; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON TABLE api_svc.raido_operator IS 'any app_user with an email in this table will be considered an operator';


--
-- Name: related_object_category; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.related_object_category (
                                                 schema_id integer NOT NULL,
                                                 uri character varying NOT NULL
);



--
-- Name: related_object_category_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.related_object_category_schema (
                                                        id integer NOT NULL,
                                                        uri character varying NOT NULL
);



--
-- Name: related_object_category_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.related_object_category_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: related_object_category_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.related_object_category_scheme_id_seq OWNED BY api_svc.related_object_category_schema.id;


--
-- Name: related_object_type; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.related_object_type (
                                             schema_id integer NOT NULL,
                                             uri character varying NOT NULL,
                                             name character varying,
                                             description character varying
);



--
-- Name: related_object_type_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.related_object_type_schema (
                                                    id integer NOT NULL,
                                                    uri character varying NOT NULL
);



--
-- Name: related_object_type_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.related_object_type_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: related_object_type_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.related_object_type_scheme_id_seq OWNED BY api_svc.related_object_type_schema.id;


--
-- Name: related_raid_type; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.related_raid_type (
                                           schema_id integer NOT NULL,
                                           uri character varying NOT NULL,
                                           name character varying,
                                           description character varying
);



--
-- Name: related_raid_type_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.related_raid_type_schema (
                                                  id integer NOT NULL,
                                                  uri character varying NOT NULL
);



--
-- Name: related_raid_type_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.related_raid_type_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: related_raid_type_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.related_raid_type_scheme_id_seq OWNED BY api_svc.related_raid_type_schema.id;


--
-- Name: service_point; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.service_point (
                                       id bigint NOT NULL,
                                       name character varying(256) NOT NULL,
                                       search_content character varying(256),
                                       admin_email character varying(256) NOT NULL,
                                       tech_email character varying(256) NOT NULL,
                                       enabled boolean DEFAULT true NOT NULL,
                                       lower_name text GENERATED ALWAYS AS (TRIM(BOTH FROM lower((name)::text))) STORED,
                                       identifier_owner character(25) NOT NULL,
                                       app_writes_enabled boolean DEFAULT true NOT NULL,
                                       CONSTRAINT lowercase_search_content CHECK (((search_content)::text = lower(TRIM(BOTH FROM search_content))))
);



--
-- Name: COLUMN service_point.search_content; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.service_point.search_content IS 'Trimmed lowercase only';


--
-- Name: service_point_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

ALTER TABLE api_svc.service_point ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME api_svc.service_point_id_seq
    START WITH 20000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    );


--
-- Name: subject_type; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.subject_type (
                                      id character varying(6) NOT NULL,
                                      name text NOT NULL,
                                      description text,
                                      note text,
                                      schema_id integer
);



--
-- Name: subject_type_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.subject_type_schema (
                                             id integer NOT NULL,
                                             uri character varying NOT NULL
);



--
-- Name: subject_type_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.subject_type_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: subject_type_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.subject_type_scheme_id_seq OWNED BY api_svc.subject_type_schema.id;


--
-- Name: title_type; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.title_type (
                                    schema_id integer NOT NULL,
                                    uri character varying NOT NULL
);



--
-- Name: title_type_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.title_type_schema (
                                           id integer NOT NULL,
                                           uri character varying NOT NULL
);



--
-- Name: title_type_scheme_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.title_type_scheme_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: title_type_scheme_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.title_type_scheme_id_seq OWNED BY api_svc.title_type_schema.id;


--
-- Name: token; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.token (
                               name text NOT NULL,
                               environment text NOT NULL,
                               date_created timestamp without time zone NOT NULL,
                               token text NOT NULL,
                               s3_export jsonb NOT NULL
);



--
-- Name: TABLE token; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON TABLE api_svc.token IS 'from arn:aws:dynamodb:ap-southeast-2:005299621378:table/RAiD-TokenTable-1P6MFZ0WFEETH';


--
-- Name: traditional_knowledge_label; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.traditional_knowledge_label (
                                                     schema_id integer NOT NULL,
                                                     uri character varying NOT NULL
);



--
-- Name: traditional_knowledge_label_schema; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.traditional_knowledge_label_schema (
                                                            id integer NOT NULL,
                                                            uri character varying NOT NULL
);



--
-- Name: traditional_knowledge_label_schema_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

CREATE SEQUENCE api_svc.traditional_knowledge_label_schema_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: traditional_knowledge_label_schema_id_seq; Type: SEQUENCE OWNED BY; Schema: api_svc; Owner: postgres
--

ALTER SEQUENCE api_svc.traditional_knowledge_label_schema_id_seq OWNED BY api_svc.traditional_knowledge_label_schema.id;


--
-- Name: user_authz_request; Type: TABLE; Schema: api_svc; Owner: postgres
--

CREATE TABLE api_svc.user_authz_request (
                                            id bigint NOT NULL,
                                            status api_svc.auth_request_status NOT NULL,
                                            service_point_id bigint NOT NULL,
                                            email character varying(256) NOT NULL,
                                            client_id character varying(256) NOT NULL,
                                            id_provider api_svc.id_provider NOT NULL,
                                            subject character varying(256) NOT NULL,
                                            responding_user bigint,
                                            approved_user bigint,
                                            description character varying(1024) NOT NULL,
                                            date_requested timestamp without time zone DEFAULT transaction_timestamp(),
                                            date_responded timestamp without time zone,
                                            CONSTRAINT lowercase_email_constraint CHECK (((email)::text = lower((email)::text)))
);



--
-- Name: COLUMN user_authz_request.email; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.user_authz_request.email IS 'Lowercase chars only - db enforced';


--
-- Name: COLUMN user_authz_request.responding_user; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.user_authz_request.responding_user IS 'user that approved or rejected, not set until that happens';


--
-- Name: COLUMN user_authz_request.approved_user; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.user_authz_request.approved_user IS 'the user that was approved, set when request is approved and the
  user is created or updated';


--
-- Name: COLUMN user_authz_request.date_responded; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON COLUMN api_svc.user_authz_request.date_responded IS 'not set until approved or rejected';


--
-- Name: user_authz_request_id_seq; Type: SEQUENCE; Schema: api_svc; Owner: postgres
--

ALTER TABLE api_svc.user_authz_request ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME api_svc.user_authz_request_id_seq
    START WITH 30000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    );


--
-- Name: access_type_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.access_type_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.access_type_scheme_id_seq'::regclass);


--
-- Name: contributor_position_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_position_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.contributor_position_scheme_id_seq'::regclass);


--
-- Name: contributor_role_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_role_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.contributor_role_scheme_id_seq'::regclass);


--
-- Name: description_type_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.description_type_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.description_type_scheme_id_seq'::regclass);


--
-- Name: language_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.language_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.language_scheme_id_seq'::regclass);


--
-- Name: organisation_role_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.organisation_role_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.organisation_role_scheme_id_seq'::regclass);


--
-- Name: related_object_category_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_category_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.related_object_category_scheme_id_seq'::regclass);


--
-- Name: related_object_type_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_type_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.related_object_type_scheme_id_seq'::regclass);


--
-- Name: related_raid_type_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_raid_type_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.related_raid_type_scheme_id_seq'::regclass);


--
-- Name: subject_type_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.subject_type_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.subject_type_scheme_id_seq'::regclass);


--
-- Name: title_type_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.title_type_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.title_type_scheme_id_seq'::regclass);


--
-- Name: traditional_knowledge_label_schema id; Type: DEFAULT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.traditional_knowledge_label_schema ALTER COLUMN id SET DEFAULT nextval('api_svc.traditional_knowledge_label_schema_id_seq'::regclass);


--
-- Data for Name: access_type; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.access_type (schema_id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json
\.


--
-- Data for Name: access_type_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.access_type_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1/
\.


--
-- Data for Name: app_user; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.app_user (id, service_point_id, email, client_id, subject, id_provider, role, enabled, token_cutoff, date_created) FROM stdin;
\.


--
-- Data for Name: contributor_position; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.contributor_position (schema_id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/co-investigator.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact-person.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/other-participant.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json
\.


--
-- Data for Name: contributor_position_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.contributor_position_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/
\.


--
-- Data for Name: contributor_role; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.contributor_role (schema_id, uri) FROM stdin;
1	https://credit.niso.org/contributor-roles/conceptualization/
1	https://credit.niso.org/contributor-roles/data-curation/
1	https://credit.niso.org/contributor-roles/formal-analysis/
1	https://credit.niso.org/contributor-roles/funding-acquisition/
1	https://credit.niso.org/contributor-roles/investigation/
1	https://credit.niso.org/contributor-roles/methodology/
1	https://credit.niso.org/contributor-roles/project-administration/
1	https://credit.niso.org/contributor-roles/resources/
1	https://credit.niso.org/contributor-roles/software/
1	https://credit.niso.org/contributor-roles/supervision/
1	https://credit.niso.org/contributor-roles/validation/
1	https://credit.niso.org/contributor-roles/visualization/
1	https://credit.niso.org/contributor-roles/writing-original-draft/
1	https://credit.niso.org/contributor-roles/writing-review-editing/
\.


--
-- Data for Name: contributor_role_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.contributor_role_schema (id, uri) FROM stdin;
1	https://credit.niso.org/
\.


--
-- Data for Name: description_type; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.description_type (schema_id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/alternative.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/primary.json
\.


--
-- Data for Name: description_type_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.description_type_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/description/type/v1/
\.


--
-- Data for Name: language; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.language (id, name, schema_id) FROM stdin;
aaa	Ghotuo	1
aab	Alumu-Tesu	1
aac	Ari	1
aad	Amal	1
aae	Arbëreshë Albanian	1
aaf	Aranadan	1
aag	Ambrak	1
aah	Abu' Arapesh	1
aai	Arifama-Miniafia	1
aak	Ankave	1
aal	Afade	1
aan	Anambé	1
aao	Algerian Saharan Arabic	1
aap	Pará Arára	1
aaq	Eastern Abnaki	1
aar	Afar	1
aas	Aasáx	1
aat	Arvanitika Albanian	1
aau	Abau	1
aaw	Solong	1
aax	Mandobo Atas	1
aaz	Amarasi	1
aba	Abé	1
abb	Bankon	1
abc	Ambala Ayta	1
abd	Manide	1
abe	Western Abnaki	1
abf	Abai Sungai	1
abg	Abaga	1
abh	Tajiki Arabic	1
abi	Abidji	1
abj	Aka-Bea	1
abk	Abkhazian	1
abl	Lampung Nyo	1
abm	Abanyom	1
abn	Abua	1
abo	Abon	1
abp	Abellen Ayta	1
abq	Abaza	1
abr	Abron	1
abs	Ambonese Malay	1
abt	Ambulas	1
abu	Abure	1
abv	Baharna Arabic	1
abw	Pal	1
abx	Inabaknon	1
aby	Aneme Wake	1
abz	Abui	1
aca	Achagua	1
acb	Áncá	1
acd	Gikyode	1
ace	Achinese	1
acf	Saint Lucian Creole French	1
ach	Acoli	1
aci	Aka-Cari	1
ack	Aka-Kora	1
acl	Akar-Bale	1
acm	Mesopotamian Arabic	1
acn	Achang	1
acp	Eastern Acipa	1
acq	Ta'izzi-Adeni Arabic	1
acr	Achi	1
acs	Acroá	1
act	Achterhoeks	1
acu	Achuar-Shiwiar	1
acv	Achumawi	1
acw	Hijazi Arabic	1
acx	Omani Arabic	1
acy	Cypriot Arabic	1
acz	Acheron	1
ada	Adangme	1
adb	Atauran	1
add	Lidzonka	1
ade	Adele	1
adf	Dhofari Arabic	1
adg	Andegerebinha	1
adh	Adhola	1
adi	Adi	1
adj	Adioukrou	1
adl	Galo	1
adn	Adang	1
ado	Abu	1
adq	Adangbe	1
adr	Adonara	1
ads	Adamorobe Sign Language	1
adt	Adnyamathanha	1
adu	Aduge	1
adw	Amundava	1
adx	Amdo Tibetan	1
ady	Adyghe	1
adz	Adzera	1
aea	Areba	1
aeb	Tunisian Arabic	1
aec	Saidi Arabic	1
aed	Argentine Sign Language	1
aee	Northeast Pashai	1
aek	Haeke	1
ael	Ambele	1
aem	Arem	1
aen	Armenian Sign Language	1
aeq	Aer	1
aer	Eastern Arrernte	1
aes	Alsea	1
aeu	Akeu	1
aew	Ambakich	1
aey	Amele	1
aez	Aeka	1
afb	Gulf Arabic	1
afd	Andai	1
afe	Putukwam	1
afg	Afghan Sign Language	1
afh	Afrihili	1
afi	Akrukay	1
afk	Nanubae	1
afn	Defaka	1
afo	Eloyi	1
afp	Tapei	1
afr	Afrikaans	1
afs	Afro-Seminole Creole	1
aft	Afitti	1
afu	Awutu	1
afz	Obokuitai	1
aga	Aguano	1
agb	Legbo	1
agc	Agatu	1
agd	Agarabi	1
age	Angal	1
agf	Arguni	1
agg	Angor	1
agh	Ngelima	1
agi	Agariya	1
agj	Argobba	1
agk	Isarog Agta	1
agl	Fembe	1
agm	Angaataha	1
agn	Agutaynen	1
ago	Tainae	1
agq	Aghem	1
agr	Aguaruna	1
ags	Esimbi	1
agt	Central Cagayan Agta	1
agu	Aguacateco	1
agv	Remontado Dumagat	1
agw	Kahua	1
agx	Aghul	1
agy	Southern Alta	1
agz	Mt. Iriga Agta	1
aha	Ahanta	1
ahb	Axamb	1
ahg	Qimant	1
ahh	Aghu	1
ahi	Tiagbamrin Aizi	1
ahk	Akha	1
ahl	Igo	1
ahm	Mobumrin Aizi	1
ahn	Àhàn	1
aho	Ahom	1
ahp	Aproumu Aizi	1
ahr	Ahirani	1
ahs	Ashe	1
aht	Ahtena	1
aia	Arosi	1
aib	Ainu (China)	1
aic	Ainbai	1
aid	Alngith	1
aie	Amara	1
aif	Agi	1
aig	Antigua and Barbuda Creole English	1
aih	Ai-Cham	1
aii	Assyrian Neo-Aramaic	1
aij	Lishanid Noshan	1
aik	Ake	1
ail	Aimele	1
aim	Aimol	1
ain	Ainu (Japan)	1
aio	Aiton	1
aip	Burumakok	1
aiq	Aimaq	1
air	Airoran	1
ait	Arikem	1
aiw	Aari	1
aix	Aighon	1
aiy	Ali	1
aja	Aja (South Sudan)	1
ajg	Aja (Benin)	1
aji	Ajië	1
ajn	Andajin	1
ajs	Algerian Jewish Sign Language	1
aju	Judeo-Moroccan Arabic	1
ajw	Ajawa	1
ajz	Amri Karbi	1
aka	Akan	1
akb	Batak Angkola	1
akc	Mpur	1
akd	Ukpet-Ehom	1
ake	Akawaio	1
akf	Akpa	1
akg	Anakalangu	1
akh	Angal Heneng	1
aki	Aiome	1
akj	Aka-Jeru	1
akk	Akkadian	1
akl	Aklanon	1
akm	Aka-Bo	1
ako	Akurio	1
akp	Siwu	1
akq	Ak	1
akr	Araki	1
aks	Akaselem	1
akt	Akolet	1
aku	Akum	1
akv	Akhvakh	1
akw	Akwa	1
akx	Aka-Kede	1
aky	Aka-Kol	1
akz	Alabama	1
ala	Alago	1
alc	Qawasqar	1
ald	Alladian	1
ale	Aleut	1
alf	Alege	1
alh	Alawa	1
ali	Amaimon	1
alj	Alangan	1
alk	Alak	1
all	Allar	1
alm	Amblong	1
aln	Gheg Albanian	1
alo	Larike-Wakasihu	1
alp	Alune	1
alq	Algonquin	1
alr	Alutor	1
als	Tosk Albanian	1
alt	Southern Altai	1
alu	'Are'are	1
alw	Alaba-K’abeena	1
alx	Amol	1
aly	Alyawarr	1
alz	Alur	1
ama	Amanayé	1
amb	Ambo	1
amc	Amahuaca	1
ame	Yanesha'	1
amf	Hamer-Banna	1
amg	Amurdak	1
amh	Amharic	1
ami	Amis	1
amj	Amdang	1
amk	Ambai	1
aml	War-Jaintia	1
amm	Ama (Papua New Guinea)	1
amn	Amanab	1
amo	Amo	1
amp	Alamblak	1
amq	Amahai	1
amr	Amarakaeri	1
ams	Southern Amami-Oshima	1
amt	Amto	1
amu	Guerrero Amuzgo	1
amv	Ambelau	1
amw	Western Neo-Aramaic	1
amx	Anmatyerre	1
amy	Ami	1
amz	Atampaya	1
ana	Andaqui	1
anb	Andoa	1
anc	Ngas	1
and	Ansus	1
ane	Xârâcùù	1
anf	Animere	1
ang	Old English (ca. 450-1100)	1
anh	Nend	1
ani	Andi	1
anj	Anor	1
ank	Goemai	1
anl	Anu-Hkongso Chin	1
anm	Anal	1
ann	Obolo	1
ano	Andoque	1
anp	Angika	1
anq	Jarawa (India)	1
anr	Andh	1
ans	Anserma	1
ant	Antakarinya	1
anu	Anuak	1
anv	Denya	1
anw	Anaang	1
anx	Andra-Hus	1
any	Anyin	1
anz	Anem	1
aoa	Angolar	1
aob	Abom	1
aoc	Pemon	1
aod	Andarum	1
aoe	Angal Enen	1
aof	Bragat	1
aog	Angoram	1
aoi	Anindilyakwa	1
aoj	Mufian	1
aok	Arhö	1
aol	Alor	1
aom	Ömie	1
aon	Bumbita Arapesh	1
aor	Aore	1
aos	Taikat	1
aot	Atong (India)	1
aou	A'ou	1
aox	Atorada	1
aoz	Uab Meto	1
apb	Sa'a	1
apc	Levantine Arabic	1
apd	Sudanese Arabic	1
ape	Bukiyip	1
apf	Pahanan Agta	1
apg	Ampanang	1
aph	Athpariya	1
api	Apiaká	1
apj	Jicarilla Apache	1
apk	Kiowa Apache	1
apl	Lipan Apache	1
apm	Mescalero-Chiricahua Apache	1
apn	Apinayé	1
apo	Ambul	1
app	Apma	1
apq	A-Pucikwar	1
apr	Arop-Lokep	1
aps	Arop-Sissano	1
apt	Apatani	1
apu	Apurinã	1
apv	Alapmunte	1
apw	Western Apache	1
apx	Aputai	1
apy	Apalaí	1
apz	Safeyoka	1
aqc	Archi	1
aqd	Ampari Dogon	1
aqg	Arigidi	1
aqk	Aninka	1
aqm	Atohwaim	1
aqn	Northern Alta	1
aqp	Atakapa	1
aqr	Arhâ	1
aqt	Angaité	1
aqz	Akuntsu	1
ara	Arabic	1
arb	Standard Arabic	1
arc	Official Aramaic (700-300 BCE)	1
ard	Arabana	1
are	Western Arrarnta	1
arg	Aragonese	1
arh	Arhuaco	1
ari	Arikara	1
arj	Arapaso	1
ark	Arikapú	1
arl	Arabela	1
arn	Mapudungun	1
aro	Araona	1
arp	Arapaho	1
arq	Algerian Arabic	1
arr	Karo (Brazil)	1
ars	Najdi Arabic	1
aru	Aruá (Amazonas State)	1
arv	Arbore	1
arw	Arawak	1
arx	Aruá (Rodonia State)	1
ary	Moroccan Arabic	1
arz	Egyptian Arabic	1
asa	Asu (Tanzania)	1
asb	Assiniboine	1
asc	Casuarina Coast Asmat	1
ase	American Sign Language	1
asf	Auslan	1
asg	Cishingini	1
ash	Abishira	1
asi	Buruwai	1
asj	Sari	1
ask	Ashkun	1
asl	Asilulu	1
asm	Assamese	1
asn	Xingú Asuriní	1
aso	Dano	1
asp	Algerian Sign Language	1
asq	Austrian Sign Language	1
asr	Asuri	1
ass	Ipulo	1
ast	Asturian	1
asu	Tocantins Asurini	1
asv	Asoa	1
asw	Australian Aborigines Sign Language	1
asx	Muratayak	1
asy	Yaosakor Asmat	1
asz	As	1
ata	Pele-Ata	1
atb	Zaiwa	1
atc	Atsahuaca	1
atd	Ata Manobo	1
ate	Atemble	1
atg	Ivbie North-Okpela-Arhe	1
ati	Attié	1
atj	Atikamekw	1
atk	Ati	1
atl	Mt. Iraya Agta	1
atm	Ata	1
atn	Ashtiani	1
ato	Atong (Cameroon)	1
atp	Pudtol Atta	1
atq	Aralle-Tabulahan	1
atr	Waimiri-Atroari	1
ats	Gros Ventre	1
att	Pamplona Atta	1
atu	Reel	1
atv	Northern Altai	1
atw	Atsugewi	1
atx	Arutani	1
aty	Aneityum	1
atz	Arta	1
aua	Asumboa	1
aub	Alugu	1
auc	Waorani	1
aud	Anuta	1
aug	Aguna	1
auh	Aushi	1
aui	Anuki	1
auj	Awjilah	1
auk	Heyo	1
aul	Aulua	1
aum	Asu (Nigeria)	1
aun	Molmo One	1
auo	Auyokawa	1
aup	Makayam	1
auq	Anus	1
aur	Aruek	1
aut	Austral	1
auu	Auye	1
auw	Awyi	1
aux	Aurá	1
auy	Awiyaana	1
auz	Uzbeki Arabic	1
ava	Avaric	1
avb	Avau	1
avd	Alviri-Vidari	1
ave	Avestan	1
avi	Avikam	1
avk	Kotava	1
avl	Eastern Egyptian Bedawi Arabic	1
avm	Angkamuthi	1
avn	Avatime	1
avo	Agavotaguerra	1
avs	Aushiri	1
avt	Au	1
avu	Avokaya	1
avv	Avá-Canoeiro	1
awa	Awadhi	1
awb	Awa (Papua New Guinea)	1
awc	Cicipu	1
awe	Awetí	1
awg	Anguthimri	1
awh	Awbono	1
awi	Aekyom	1
awk	Awabakal	1
awm	Arawum	1
awn	Awngi	1
awo	Awak	1
awr	Awera	1
aws	South Awyu	1
awt	Araweté	1
awu	Central Awyu	1
awv	Jair Awyu	1
aww	Awun	1
awx	Awara	1
awy	Edera Awyu	1
axb	Abipon	1
axe	Ayerrerenge	1
axg	Mato Grosso Arára	1
axk	Yaka (Central African Republic)	1
axl	Lower Southern Aranda	1
axm	Middle Armenian	1
axx	Xârâgurè	1
aya	Awar	1
ayb	Ayizo Gbe	1
ayc	Southern Aymara	1
ayd	Ayabadhu	1
aye	Ayere	1
ayg	Ginyanga	1
ayh	Hadrami Arabic	1
ayi	Leyigha	1
ayk	Akuku	1
ayl	Libyan Arabic	1
aym	Aymara	1
ayn	Sanaani Arabic	1
ayo	Ayoreo	1
ayp	North Mesopotamian Arabic	1
ayq	Ayi (Papua New Guinea)	1
ayr	Central Aymara	1
ays	Sorsogon Ayta	1
ayt	Magbukun Ayta	1
ayu	Ayu	1
ayz	Mai Brat	1
aza	Azha	1
azb	South Azerbaijani	1
azd	Eastern Durango Nahuatl	1
aze	Azerbaijani	1
azg	San Pedro Amuzgos Amuzgo	1
azj	North Azerbaijani	1
azm	Ipalapa Amuzgo	1
azn	Western Durango Nahuatl	1
azo	Awing	1
azt	Faire Atta	1
azz	Highland Puebla Nahuatl	1
baa	Babatana	1
bab	Bainouk-Gunyuño	1
bac	Badui	1
bae	Baré	1
baf	Nubaca	1
bag	Tuki	1
bah	Bahamas Creole English	1
baj	Barakai	1
bak	Bashkir	1
bal	Baluchi	1
bam	Bambara	1
ban	Balinese	1
bao	Waimaha	1
bap	Bantawa	1
bar	Bavarian	1
bas	Basa (Cameroon)	1
bau	Bada (Nigeria)	1
bav	Vengo	1
baw	Bambili-Bambui	1
bax	Bamun	1
bay	Batuley	1
bba	Baatonum	1
bbb	Barai	1
bbc	Batak Toba	1
bbd	Bau	1
bbe	Bangba	1
bbf	Baibai	1
bbg	Barama	1
bbh	Bugan	1
bbi	Barombi	1
bbj	Ghomálá'	1
bbk	Babanki	1
bbl	Bats	1
bbm	Babango	1
bbn	Uneapa	1
bbo	Northern Bobo Madaré	1
bbp	West Central Banda	1
bbq	Bamali	1
bbr	Girawa	1
bbs	Bakpinka	1
bbt	Mburku	1
bbu	Kulung (Nigeria)	1
bbv	Karnai	1
bbw	Baba	1
bbx	Bubia	1
bby	Befang	1
bca	Central Bai	1
bcb	Bainouk-Samik	1
bcc	Southern Balochi	1
bcd	North Babar	1
bce	Bamenyam	1
bcf	Bamu	1
bcg	Baga Pokur	1
bch	Bariai	1
bci	Baoulé	1
bcj	Bardi	1
bck	Bunuba	1
bcl	Central Bikol	1
bcm	Bannoni	1
bcn	Bali (Nigeria)	1
bco	Kaluli	1
bcp	Bali (Democratic Republic of Congo)	1
bcq	Bench	1
bcr	Babine	1
bcs	Kohumono	1
bct	Bendi	1
bcu	Awad Bing	1
bcv	Shoo-Minda-Nye	1
bcw	Bana	1
bcy	Bacama	1
bcz	Bainouk-Gunyaamolo	1
bda	Bayot	1
bdb	Basap	1
bdc	Emberá-Baudó	1
bdd	Bunama	1
bde	Bade	1
bdf	Biage	1
bdg	Bonggi	1
bdh	Baka (South Sudan)	1
bdi	Burun	1
bdj	Bai (South Sudan)	1
bdk	Budukh	1
bdl	Indonesian Bajau	1
bdm	Buduma	1
bdn	Baldemu	1
bdo	Morom	1
bdp	Bende	1
bdq	Bahnar	1
bdr	West Coast Bajau	1
bds	Burunge	1
bdt	Bokoto	1
bdu	Oroko	1
bdv	Bodo Parja	1
bdw	Baham	1
bdx	Budong-Budong	1
bdy	Bandjalang	1
bdz	Badeshi	1
bea	Beaver	1
beb	Bebele	1
bec	Iceve-Maci	1
bed	Bedoanas	1
bee	Byangsi	1
bef	Benabena	1
beg	Belait	1
beh	Biali	1
bei	Bekati'	1
bej	Beja	1
bek	Bebeli	1
bel	Belarusian	1
bem	Bemba (Zambia)	1
ben	Bengali	1
beo	Beami	1
bep	Besoa	1
beq	Beembe	1
bes	Besme	1
bet	Guiberoua Béte	1
beu	Blagar	1
bev	Daloa Bété	1
bew	Betawi	1
bex	Jur Modo	1
bey	Beli (Papua New Guinea)	1
bez	Bena (Tanzania)	1
bfa	Bari	1
bfb	Pauri Bareli	1
bfc	Panyi Bai	1
bfd	Bafut	1
bfe	Betaf	1
bff	Bofi	1
bfg	Busang Kayan	1
bfh	Blafe	1
bfi	British Sign Language	1
bfj	Bafanji	1
bfk	Ban Khor Sign Language	1
bfl	Banda-Ndélé	1
bfm	Mmen	1
bfn	Bunak	1
bfo	Malba Birifor	1
bfp	Beba	1
bfq	Badaga	1
bfr	Bazigar	1
bfs	Southern Bai	1
bft	Balti	1
bfu	Gahri	1
bfw	Bondo	1
bfx	Bantayanon	1
bfy	Bagheli	1
bfz	Mahasu Pahari	1
bga	Gwamhi-Wuri	1
bgb	Bobongko	1
bgc	Haryanvi	1
bgd	Rathwi Bareli	1
bge	Bauria	1
bgf	Bangandu	1
bgg	Bugun	1
bgi	Giangan	1
bgj	Bangolan	1
bgk	Bit	1
bgl	Bo (Laos)	1
bgn	Western Balochi	1
bgo	Baga Koga	1
bgp	Eastern Balochi	1
bgq	Bagri	1
bgr	Bawm Chin	1
bgs	Tagabawa	1
bgt	Bughotu	1
bgu	Mbongno	1
bgv	Warkay-Bipim	1
bgw	Bhatri	1
bgx	Balkan Gagauz Turkish	1
bgy	Benggoi	1
bgz	Banggai	1
bha	Bharia	1
bhb	Bhili	1
bhc	Biga	1
bhd	Bhadrawahi	1
bhe	Bhaya	1
bhf	Odiai	1
bhg	Binandere	1
bhh	Bukharic	1
bhi	Bhilali	1
bhj	Bahing	1
bhl	Bimin	1
bhm	Bathari	1
bhn	Bohtan Neo-Aramaic	1
bho	Bhojpuri	1
bhp	Bima	1
bhq	Tukang Besi South	1
bhr	Bara Malagasy	1
bhs	Buwal	1
bht	Bhattiyali	1
bhu	Bhunjia	1
bhv	Bahau	1
bhw	Biak	1
bhx	Bhalay	1
bhy	Bhele	1
bhz	Bada (Indonesia)	1
bia	Badimaya	1
bib	Bissa	1
bid	Bidiyo	1
bie	Bepour	1
bif	Biafada	1
big	Biangai	1
bik	Bikol	1
bil	Bile	1
bim	Bimoba	1
bin	Bini	1
bio	Nai	1
bip	Bila	1
biq	Bipi	1
bir	Bisorio	1
bis	Bislama	1
bit	Berinomo	1
biu	Biete	1
biv	Southern Birifor	1
biw	Kol (Cameroon)	1
bix	Bijori	1
biy	Birhor	1
biz	Baloi	1
bja	Budza	1
bjb	Banggarla	1
bjc	Bariji	1
bje	Biao-Jiao Mien	1
bjf	Barzani Jewish Neo-Aramaic	1
bjg	Bidyogo	1
bjh	Bahinemo	1
bji	Burji	1
bjj	Kanauji	1
bjk	Barok	1
bjl	Bulu (Papua New Guinea)	1
bjm	Bajelani	1
bjn	Banjar	1
bjo	Mid-Southern Banda	1
bjp	Fanamaket	1
bjr	Binumarien	1
bjs	Bajan	1
bjt	Balanta-Ganja	1
bju	Busuu	1
bjv	Bedjond	1
bjw	Bakwé	1
bjx	Banao Itneg	1
bjy	Bayali	1
bjz	Baruga	1
bka	Kyak	1
bkc	Baka (Cameroon)	1
bkd	Binukid	1
bkf	Beeke	1
bkg	Buraka	1
bkh	Bakoko	1
bki	Baki	1
bkj	Pande	1
bkk	Brokskat	1
bkl	Berik	1
bkm	Kom (Cameroon)	1
bkn	Bukitan	1
bko	Kwa'	1
bkp	Boko (Democratic Republic of Congo)	1
bkq	Bakairí	1
bkr	Bakumpai	1
bks	Northern Sorsoganon	1
bkt	Boloki	1
bku	Buhid	1
bkv	Bekwarra	1
bkw	Bekwel	1
bkx	Baikeno	1
bky	Bokyi	1
bkz	Bungku	1
bla	Siksika	1
blb	Bilua	1
blc	Bella Coola	1
bld	Bolango	1
ble	Balanta-Kentohe	1
blf	Buol	1
blh	Kuwaa	1
bli	Bolia	1
blj	Bolongan	1
blk	Pa'o Karen	1
bll	Biloxi	1
blm	Beli (South Sudan)	1
bln	Southern Catanduanes Bikol	1
blo	Anii	1
blp	Blablanga	1
blq	Baluan-Pam	1
blr	Blang	1
bls	Balaesang	1
blt	Tai Dam	1
blv	Kibala	1
blw	Balangao	1
blx	Mag-Indi Ayta	1
bly	Notre	1
blz	Balantak	1
bma	Lame	1
bmb	Bembe	1
bmc	Biem	1
bmd	Baga Manduri	1
bme	Limassa	1
bmf	Bom-Kim	1
bmg	Bamwe	1
bmh	Kein	1
bmi	Bagirmi	1
bmj	Bote-Majhi	1
bmk	Ghayavi	1
bml	Bomboli	1
bmm	Northern Betsimisaraka Malagasy	1
bmn	Bina (Papua New Guinea)	1
bmo	Bambalang	1
bmp	Bulgebi	1
bmq	Bomu	1
bmr	Muinane	1
bms	Bilma Kanuri	1
bmt	Biao Mon	1
bmu	Somba-Siawari	1
bmv	Bum	1
bmw	Bomwali	1
bmx	Baimak	1
bmz	Baramu	1
bna	Bonerate	1
bnb	Bookan	1
bnc	Bontok	1
bnd	Banda (Indonesia)	1
bne	Bintauna	1
bnf	Masiwang	1
bng	Benga	1
bni	Bangi	1
bnj	Eastern Tawbuid	1
bnk	Bierebo	1
bnl	Boon	1
bnm	Batanga	1
bnn	Bunun	1
bno	Bantoanon	1
bnp	Bola	1
bnq	Bantik	1
bnr	Butmas-Tur	1
bns	Bundeli	1
bnu	Bentong	1
bnv	Bonerif	1
bnw	Bisis	1
bnx	Bangubangu	1
bny	Bintulu	1
bnz	Beezen	1
boa	Bora	1
bob	Aweer	1
bod	Tibetan	1
boe	Mundabli	1
bof	Bolon	1
bog	Bamako Sign Language	1
boh	Boma	1
boi	Barbareño	1
boj	Anjam	1
bok	Bonjo	1
bol	Bole	1
bom	Berom	1
bon	Bine	1
boo	Tiemacèwè Bozo	1
bop	Bonkiman	1
boq	Bogaya	1
bor	Borôro	1
bos	Bosnian	1
bot	Bongo	1
bou	Bondei	1
bov	Tuwuli	1
bow	Rema	1
box	Buamu	1
boy	Bodo (Central African Republic)	1
boz	Tiéyaxo Bozo	1
bpa	Daakaka	1
bpc	Mbuk	1
bpd	Banda-Banda	1
bpe	Bauni	1
bpg	Bonggo	1
bph	Botlikh	1
bpi	Bagupi	1
bpj	Binji	1
bpk	Orowe	1
bpl	Broome Pearling Lugger Pidgin	1
bpm	Biyom	1
bpn	Dzao Min	1
bpo	Anasi	1
bpp	Kaure	1
bpq	Banda Malay	1
bpr	Koronadal Blaan	1
bps	Sarangani Blaan	1
bpt	Barrow Point	1
bpu	Bongu	1
bpv	Bian Marind	1
bpw	Bo (Papua New Guinea)	1
bpx	Palya Bareli	1
bpy	Bishnupriya	1
bpz	Bilba	1
bqa	Tchumbuli	1
bqb	Bagusa	1
bqc	Boko (Benin)	1
bqd	Bung	1
bqf	Baga Kaloum	1
bqg	Bago-Kusuntu	1
bqh	Baima	1
bqi	Bakhtiari	1
bqj	Bandial	1
bqk	Banda-Mbrès	1
bql	Bilakura	1
bqm	Wumboko	1
bqn	Bulgarian Sign Language	1
bqo	Balo	1
bqp	Busa	1
bqq	Biritai	1
bqr	Burusu	1
bqs	Bosngun	1
bqt	Bamukumbit	1
bqu	Boguru	1
bqv	Koro Wachi	1
bqw	Buru (Nigeria)	1
bqx	Baangi	1
bqy	Bengkala Sign Language	1
bqz	Bakaka	1
bra	Braj	1
brb	Brao	1
brc	Berbice Creole Dutch	1
brd	Baraamu	1
bre	Breton	1
brf	Bira	1
brg	Baure	1
brh	Brahui	1
bri	Mokpwe	1
brj	Bieria	1
brk	Birked	1
brl	Birwa	1
brm	Barambu	1
brn	Boruca	1
bro	Brokkat	1
brp	Barapasi	1
brq	Breri	1
brr	Birao	1
brs	Baras	1
brt	Bitare	1
bru	Eastern Bru	1
brv	Western Bru	1
brw	Bellari	1
brx	Bodo (India)	1
bry	Burui	1
brz	Bilbil	1
bsa	Abinomn	1
bsb	Brunei Bisaya	1
bsc	Bassari	1
bse	Wushi	1
bsf	Bauchi	1
bsg	Bashkardi	1
bsh	Kati	1
bsi	Bassossi	1
bsj	Bangwinji	1
bsk	Burushaski	1
bsl	Basa-Gumna	1
bsm	Busami	1
bsn	Barasana-Eduria	1
bso	Buso	1
bsp	Baga Sitemu	1
bsq	Bassa	1
bsr	Bassa-Kontagora	1
bss	Akoose	1
bst	Basketo	1
bsu	Bahonsuai	1
bsv	Baga Sobané	1
bsw	Baiso	1
bsx	Yangkam	1
bsy	Sabah Bisaya	1
bta	Bata	1
btc	Bati (Cameroon)	1
btd	Batak Dairi	1
bte	Gamo-Ningi	1
btf	Birgit	1
btg	Gagnoa Bété	1
bth	Biatah Bidayuh	1
bti	Burate	1
btj	Bacanese Malay	1
btm	Batak Mandailing	1
btn	Ratagnon	1
bto	Rinconada Bikol	1
btp	Budibud	1
btq	Batek	1
btr	Baetora	1
bts	Batak Simalungun	1
btt	Bete-Bendi	1
btu	Batu	1
btv	Bateri	1
btw	Butuanon	1
btx	Batak Karo	1
bty	Bobot	1
btz	Batak Alas-Kluet	1
bua	Buriat	1
bub	Bua	1
buc	Bushi	1
bud	Ntcham	1
bue	Beothuk	1
buf	Bushoong	1
bug	Buginese	1
buh	Younuo Bunu	1
bui	Bongili	1
buj	Basa-Gurmana	1
buk	Bugawac	1
bul	Bulgarian	1
bum	Bulu (Cameroon)	1
bun	Sherbro	1
buo	Terei	1
bup	Busoa	1
buq	Brem	1
bus	Bokobaru	1
but	Bungain	1
buu	Budu	1
buv	Bun	1
buw	Bubi	1
bux	Boghom	1
buy	Bullom So	1
buz	Bukwen	1
bva	Barein	1
bvb	Bube	1
bvc	Baelelea	1
bvd	Baeggu	1
bve	Berau Malay	1
bvf	Boor	1
bvg	Bonkeng	1
bvh	Bure	1
bvi	Belanda Viri	1
bvj	Baan	1
bvk	Bukat	1
bvl	Bolivian Sign Language	1
bvm	Bamunka	1
bvn	Buna	1
bvo	Bolgo	1
bvp	Bumang	1
bvq	Birri	1
bvr	Burarra	1
bvt	Bati (Indonesia)	1
bvu	Bukit Malay	1
bvv	Baniva	1
bvw	Boga	1
bvx	Dibole	1
bvy	Baybayanon	1
bvz	Bauzi	1
bwa	Bwatoo	1
bwb	Namosi-Naitasiri-Serua	1
bwc	Bwile	1
bwd	Bwaidoka	1
bwe	Bwe Karen	1
bwf	Boselewa	1
bwg	Barwe	1
bwh	Bishuo	1
bwi	Baniwa	1
bwj	Láá Láá Bwamu	1
bwk	Bauwaki	1
bwl	Bwela	1
bwm	Biwat	1
bwn	Wunai Bunu	1
bwo	Boro (Ethiopia)	1
bwp	Mandobo Bawah	1
bwq	Southern Bobo Madaré	1
bwr	Bura-Pabir	1
bws	Bomboma	1
bwt	Bafaw-Balong	1
bwu	Buli (Ghana)	1
bww	Bwa	1
bwx	Bu-Nao Bunu	1
bwy	Cwi Bwamu	1
bwz	Bwisi	1
bxa	Tairaha	1
bxb	Belanda Bor	1
bxc	Molengue	1
bxd	Pela	1
bxe	Birale	1
bxf	Bilur	1
bxg	Bangala	1
bxh	Buhutu	1
bxi	Pirlatapa	1
bxj	Bayungu	1
bxk	Bukusu	1
bxl	Jalkunan	1
bxm	Mongolia Buriat	1
bxn	Burduna	1
bxo	Barikanchi	1
bxp	Bebil	1
bxq	Beele	1
bxr	Russia Buriat	1
bxs	Busam	1
bxu	China Buriat	1
bxv	Berakou	1
bxw	Bankagooma	1
bxz	Binahari	1
bya	Batak	1
byb	Bikya	1
byc	Ubaghara	1
byd	Benyadu'	1
bye	Pouye	1
byf	Bete	1
byg	Baygo	1
byh	Bhujel	1
byi	Buyu	1
byj	Bina (Nigeria)	1
byk	Biao	1
byl	Bayono	1
bym	Bidjara	1
byn	Bilin	1
byo	Biyo	1
byp	Bumaji	1
byq	Basay	1
byr	Baruya	1
bys	Burak	1
byt	Berti	1
byv	Medumba	1
byw	Belhariya	1
byx	Qaqet	1
byz	Banaro	1
bza	Bandi	1
bzb	Andio	1
bzc	Southern Betsimisaraka Malagasy	1
bzd	Bribri	1
bze	Jenaama Bozo	1
bzf	Boikin	1
bzg	Babuza	1
bzh	Mapos Buang	1
bzi	Bisu	1
bzj	Belize Kriol English	1
bzk	Nicaragua Creole English	1
bzl	Boano (Sulawesi)	1
bzm	Bolondo	1
bzn	Boano (Maluku)	1
bzo	Bozaba	1
bzp	Kemberano	1
bzq	Buli (Indonesia)	1
bzr	Biri	1
bzs	Brazilian Sign Language	1
bzt	Brithenig	1
bzu	Burmeso	1
bzv	Naami	1
bzw	Basa (Nigeria)	1
bzx	Kɛlɛngaxo Bozo	1
bzy	Obanliku	1
bzz	Evant	1
caa	Chortí	1
cab	Garifuna	1
cac	Chuj	1
cad	Caddo	1
cae	Lehar	1
caf	Southern Carrier	1
cag	Nivaclé	1
cah	Cahuarano	1
caj	Chané	1
cak	Kaqchikel	1
cal	Carolinian	1
cam	Cemuhî	1
can	Chambri	1
cao	Chácobo	1
cap	Chipaya	1
caq	Car Nicobarese	1
car	Galibi Carib	1
cas	Tsimané	1
cat	Catalan	1
cav	Cavineña	1
caw	Callawalla	1
cax	Chiquitano	1
cay	Cayuga	1
caz	Canichana	1
cbb	Cabiyarí	1
cbc	Carapana	1
cbd	Carijona	1
cbg	Chimila	1
cbi	Chachi	1
cbj	Ede Cabe	1
cbk	Chavacano	1
cbl	Bualkhaw Chin	1
cbn	Nyahkur	1
cbo	Izora	1
cbq	Tsucuba	1
cbr	Cashibo-Cacataibo	1
cbs	Cashinahua	1
cbt	Chayahuita	1
cbu	Candoshi-Shapra	1
cbv	Cacua	1
cbw	Kinabalian	1
cby	Carabayo	1
ccc	Chamicuro	1
ccd	Cafundo Creole	1
cce	Chopi	1
ccg	Samba Daka	1
cch	Atsam	1
ccj	Kasanga	1
ccl	Cutchi-Swahili	1
ccm	Malaccan Creole Malay	1
cco	Comaltepec Chinantec	1
ccp	Chakma	1
ccr	Cacaopera	1
cda	Choni	1
cde	Chenchu	1
cdf	Chiru	1
cdh	Chambeali	1
cdi	Chodri	1
cdj	Churahi	1
cdm	Chepang	1
cdn	Chaudangsi	1
cdo	Min Dong Chinese	1
cdr	Cinda-Regi-Tiyal	1
cds	Chadian Sign Language	1
cdy	Chadong	1
cdz	Koda	1
cea	Lower Chehalis	1
ceb	Cebuano	1
ceg	Chamacoco	1
cek	Eastern Khumi Chin	1
cen	Cen	1
ces	Czech	1
cet	Centúúm	1
cey	Ekai Chin	1
cfa	Dijim-Bwilim	1
cfd	Cara	1
cfg	Como Karim	1
cfm	Falam Chin	1
cga	Changriwa	1
cgc	Kagayanen	1
cgg	Chiga	1
cgk	Chocangacakha	1
cha	Chamorro	1
chb	Chibcha	1
chc	Catawba	1
chd	Highland Oaxaca Chontal	1
che	Chechen	1
chf	Tabasco Chontal	1
chg	Chagatai	1
chh	Chinook	1
chj	Ojitlán Chinantec	1
chk	Chuukese	1
chl	Cahuilla	1
chm	Mari (Russia)	1
chn	Chinook jargon	1
cho	Choctaw	1
chp	Chipewyan	1
chq	Quiotepec Chinantec	1
chr	Cherokee	1
cht	Cholón	1
chu	Church Slavic	1
chv	Chuvash	1
chw	Chuwabu	1
chx	Chantyal	1
chy	Cheyenne	1
chz	Ozumacín Chinantec	1
cia	Cia-Cia	1
cib	Ci Gbe	1
cic	Chickasaw	1
cid	Chimariko	1
cie	Cineni	1
cih	Chinali	1
cik	Chitkuli Kinnauri	1
cim	Cimbrian	1
cin	Cinta Larga	1
cip	Chiapanec	1
cir	Tiri	1
ciw	Chippewa	1
ciy	Chaima	1
cja	Western Cham	1
cje	Chru	1
cjh	Upper Chehalis	1
cji	Chamalal	1
cjk	Chokwe	1
cjm	Eastern Cham	1
cjn	Chenapian	1
cjo	Ashéninka Pajonal	1
cjp	Cabécar	1
cjs	Shor	1
cjv	Chuave	1
cjy	Jinyu Chinese	1
ckb	Central Kurdish	1
ckh	Chak	1
ckl	Cibak	1
ckm	Chakavian	1
ckn	Kaang Chin	1
cko	Anufo	1
ckq	Kajakse	1
ckr	Kairak	1
cks	Tayo	1
ckt	Chukot	1
cku	Koasati	1
ckv	Kavalan	1
ckx	Caka	1
cky	Cakfem-Mushere	1
ckz	Cakchiquel-Quiché Mixed Language	1
cla	Ron	1
clc	Chilcotin	1
cld	Chaldean Neo-Aramaic	1
cle	Lealao Chinantec	1
clh	Chilisso	1
cli	Chakali	1
clj	Laitu Chin	1
clk	Idu-Mishmi	1
cll	Chala	1
clm	Clallam	1
clo	Lowland Oaxaca Chontal	1
clt	Lautu Chin	1
clu	Caluyanun	1
clw	Chulym	1
cly	Eastern Highland Chatino	1
cma	Maa	1
cme	Cerma	1
cmg	Classical Mongolian	1
cmi	Emberá-Chamí	1
cml	Campalagian	1
cmm	Michigamea	1
cmn	Mandarin Chinese	1
cmo	Central Mnong	1
cmr	Mro-Khimi Chin	1
cms	Messapic	1
cmt	Camtho	1
cna	Changthang	1
cnb	Chinbon Chin	1
cnc	Côông	1
cng	Northern Qiang	1
cnh	Hakha Chin	1
cni	Asháninka	1
cnk	Khumi Chin	1
cnl	Lalana Chinantec	1
cno	Con	1
cnp	Northern Ping Chinese	1
cnq	Chung	1
cnr	Montenegrin	1
cns	Central Asmat	1
cnt	Tepetotutla Chinantec	1
cnu	Chenoua	1
cnw	Ngawn Chin	1
cnx	Middle Cornish	1
coa	Cocos Islands Malay	1
cob	Chicomuceltec	1
coc	Cocopa	1
cod	Cocama-Cocamilla	1
coe	Koreguaje	1
cof	Colorado	1
cog	Chong	1
coh	Chonyi-Dzihana-Kauma	1
coj	Cochimi	1
cok	Santa Teresa Cora	1
col	Columbia-Wenatchi	1
com	Comanche	1
con	Cofán	1
coo	Comox	1
cop	Coptic	1
coq	Coquille	1
cor	Cornish	1
cos	Corsican	1
cot	Caquinte	1
cou	Wamey	1
cov	Cao Miao	1
cow	Cowlitz	1
cox	Nanti	1
coz	Chochotec	1
cpa	Palantla Chinantec	1
cpb	Ucayali-Yurúa Ashéninka	1
cpc	Ajyíninka Apurucayali	1
cpg	Cappadocian Greek	1
cpi	Chinese Pidgin English	1
cpn	Cherepon	1
cpo	Kpeego	1
cps	Capiznon	1
cpu	Pichis Ashéninka	1
cpx	Pu-Xian Chinese	1
cpy	South Ucayali Ashéninka	1
cqd	Chuanqiandian Cluster Miao	1
   	L\tChara	1
crb	Island Carib	1
crc	Lonwolwol	1
crd	Coeur d'Alene	1
cre	Cree	1
crf	Caramanta	1
crg	Michif	1
crh	Crimean Tatar	1
cri	Sãotomense	1
crj	Southern East Cree	1
crk	Plains Cree	1
crl	Northern East Cree	1
crm	Moose Cree	1
crn	El Nayar Cora	1
cro	Crow	1
crq	Iyo'wujwa Chorote	1
crr	Carolina Algonquian	1
crs	Seselwa Creole French	1
crt	Iyojwa'ja Chorote	1
crv	Chaura	1
crw	Chrau	1
crx	Carrier	1
cry	Cori	1
crz	Cruzeño	1
csa	Chiltepec Chinantec	1
csb	Kashubian	1
csc	Catalan Sign Language	1
csd	Chiangmai Sign Language	1
cse	Czech Sign Language	1
csf	Cuba Sign Language	1
csg	Chilean Sign Language	1
csh	Asho Chin	1
csi	Coast Miwok	1
csj	Songlai Chin	1
csk	Jola-Kasa	1
csl	Chinese Sign Language	1
csm	Central Sierra Miwok	1
csn	Colombian Sign Language	1
cso	Sochiapam Chinantec	1
csp	Southern Ping Chinese	1
csq	Croatia Sign Language	1
csr	Costa Rican Sign Language	1
css	Southern Ohlone	1
cst	Northern Ohlone	1
csv	Sumtu Chin	1
csw	Swampy Cree	1
csx	Cambodian Sign Language	1
csy	Siyin Chin	1
csz	Coos	1
cta	Tataltepec Chatino	1
ctc	Chetco	1
ctd	Tedim Chin	1
cte	Tepinapa Chinantec	1
ctg	Chittagonian	1
cth	Thaiphum Chin	1
ctl	Tlacoatzintepec Chinantec	1
ctm	Chitimacha	1
ctn	Chhintange	1
cto	Emberá-Catío	1
ctp	Western Highland Chatino	1
cts	Northern Catanduanes Bikol	1
ctt	Wayanad Chetti	1
ctu	Chol	1
cty	Moundadan Chetty	1
ctz	Zacatepec Chatino	1
cua	Cua	1
cub	Cubeo	1
cuc	Usila Chinantec	1
cuh	Chuka	1
cui	Cuiba	1
cuj	Mashco Piro	1
cuk	San Blas Kuna	1
cul	Culina	1
cuo	Cumanagoto	1
cup	Cupeño	1
cuq	Cun	1
cur	Chhulung	1
cut	Teutila Cuicatec	1
cuu	Tai Ya	1
cuv	Cuvok	1
cuw	Chukwa	1
cux	Tepeuxila Cuicatec	1
cuy	Cuitlatec	1
cvg	Chug	1
cvn	Valle Nacional Chinantec	1
cwa	Kabwa	1
cwb	Maindo	1
cwd	Woods Cree	1
cwe	Kwere	1
cwg	Chewong	1
cwt	Kuwaataay	1
cxh	Cha'ari	1
cya	Nopala Chatino	1
cyb	Cayubaba	1
cym	Welsh	1
cyo	Cuyonon	1
czh	Huizhou Chinese	1
czk	Knaanic	1
czn	Zenzontepec Chatino	1
czo	Min Zhong Chinese	1
czt	Zotung Chin	1
daa	Dangaléat	1
dac	Dambi	1
dad	Marik	1
dae	Duupa	1
dag	Dagbani	1
dah	Gwahatike	1
dai	Day	1
daj	Dar Fur Daju	1
dak	Dakota	1
dal	Dahalo	1
dam	Damakawa	1
dan	Danish	1
dao	Daai Chin	1
daq	Dandami Maria	1
dar	Dargwa	1
das	Daho-Doo	1
dau	Dar Sila Daju	1
dav	Taita	1
daw	Davawenyo	1
dax	Dayi	1
daz	Dao	1
dba	Bangime	1
dbb	Deno	1
dbd	Dadiya	1
dbe	Dabe	1
dbf	Edopi	1
dbg	Dogul Dom Dogon	1
dbi	Doka	1
dbj	Ida'an	1
dbl	Dyirbal	1
dbm	Duguri	1
dbn	Duriankere	1
dbo	Dulbu	1
dbp	Duwai	1
dbq	Daba	1
dbr	Dabarre	1
dbt	Ben Tey Dogon	1
dbu	Bondum Dom Dogon	1
dbv	Dungu	1
dbw	Bankan Tey Dogon	1
dby	Dibiyaso	1
dcc	Deccan	1
dcr	Negerhollands	1
dda	Dadi Dadi	1
ddd	Dongotono	1
dde	Doondo	1
ddg	Fataluku	1
ddi	West Goodenough	1
ddj	Jaru	1
ddn	Dendi (Benin)	1
ddo	Dido	1
ddr	Dhudhuroa	1
dds	Donno So Dogon	1
ddw	Dawera-Daweloor	1
dec	Dagik	1
ded	Dedua	1
dee	Dewoin	1
def	Dezfuli	1
deg	Degema	1
deh	Dehwari	1
dei	Demisa	1
dek	Dek	1
del	Delaware	1
dem	Dem	1
den	Slave (Athapascan)	1
dep	Pidgin Delaware	1
deq	Dendi (Central African Republic)	1
der	Deori	1
des	Desano	1
deu	German	1
dev	Domung	1
dez	Dengese	1
dga	Southern Dagaare	1
dgb	Bunoge Dogon	1
dgc	Casiguran Dumagat Agta	1
dgd	Dagaari Dioula	1
dge	Degenan	1
dgg	Doga	1
dgh	Dghwede	1
dgi	Northern Dagara	1
dgk	Dagba	1
dgl	Andaandi	1
dgn	Dagoman	1
dgo	Dogri (individual language)	1
dgr	Dogrib	1
dgs	Dogoso	1
dgt	Ndra'ngith	1
dgw	Daungwurrung	1
dgx	Doghoro	1
dgz	Daga	1
dhd	Dhundari	1
dhg	Dhangu-Djangu	1
dhi	Dhimal	1
dhl	Dhalandji	1
dhm	Zemba	1
dhn	Dhanki	1
dho	Dhodia	1
dhr	Dhargari	1
dhs	Dhaiso	1
dhu	Dhurga	1
dhv	Dehu	1
dhw	Dhanwar (Nepal)	1
dhx	Dhungaloo	1
dia	Dia	1
dib	South Central Dinka	1
dic	Lakota Dida	1
did	Didinga	1
dif	Dieri	1
dig	Digo	1
dih	Kumiai	1
dii	Dimbong	1
dij	Dai	1
dik	Southwestern Dinka	1
dil	Dilling	1
dim	Dime	1
din	Dinka	1
dio	Dibo	1
dip	Northeastern Dinka	1
diq	Dimli (individual language)	1
dir	Dirim	1
dis	Dimasa	1
diu	Diriku	1
div	Dhivehi	1
diw	Northwestern Dinka	1
dix	Dixon Reef	1
diy	Diuwe	1
diz	Ding	1
dja	Djadjawurrung	1
djb	Djinba	1
djc	Dar Daju Daju	1
djd	Djamindjung	1
dje	Zarma	1
djf	Djangun	1
dji	Djinang	1
djj	Djeebbana	1
djk	Eastern Maroon Creole	1
djm	Jamsay Dogon	1
djn	Jawoyn	1
djo	Jangkang	1
djr	Djambarrpuyngu	1
dju	Kapriman	1
djw	Djawi	1
dka	Dakpakha	1
dkg	Kadung	1
dkk	Dakka	1
dkr	Kuijau	1
dks	Southeastern Dinka	1
dkx	Mazagway	1
dlg	Dolgan	1
dlk	Dahalik	1
dlm	Dalmatian	1
dln	Darlong	1
dma	Duma	1
dmb	Mombo Dogon	1
dmc	Gavak	1
dmd	Madhi Madhi	1
dme	Dugwor	1
dmf	Medefaidrin	1
dmg	Upper Kinabatangan	1
dmk	Domaaki	1
dml	Dameli	1
dmm	Dama	1
dmo	Kemedzung	1
dmr	East Damar	1
dms	Dampelas	1
dmu	Dubu	1
dmv	Dumpas	1
dmw	Mudburra	1
dmx	Dema	1
dmy	Demta	1
dna	Upper Grand Valley Dani	1
dnd	Daonda	1
dne	Ndendeule	1
dng	Dungan	1
dni	Lower Grand Valley Dani	1
dnj	Dan	1
dnk	Dengka	1
dnn	Dzùùngoo	1
dno	Ndrulo	1
dnr	Danaru	1
dnt	Mid Grand Valley Dani	1
dnu	Danau	1
dnv	Danu	1
dnw	Western Dani	1
dny	Dení	1
doa	Dom	1
dob	Dobu	1
doc	Northern Dong	1
doe	Doe	1
dof	Domu	1
doh	Dong	1
doi	Dogri (macrolanguage)	1
dok	Dondo	1
dol	Doso	1
don	Toura (Papua New Guinea)	1
doo	Dongo	1
dop	Lukpa	1
doq	Dominican Sign Language	1
dor	Dori'o	1
dos	Dogosé	1
dot	Dass	1
dov	Dombe	1
dow	Doyayo	1
dox	Bussa	1
doy	Dompo	1
doz	Dorze	1
dpp	Papar	1
drb	Dair	1
drc	Minderico	1
drd	Darmiya	1
dre	Dolpo	1
drg	Rungus	1
dri	C'Lela	1
drl	Paakantyi	1
drn	West Damar	1
dro	Daro-Matu Melanau	1
drq	Dura	1
drs	Gedeo	1
drt	Drents	1
dru	Rukai	1
dry	Darai	1
dsb	Lower Sorbian	1
dse	Dutch Sign Language	1
dsh	Daasanach	1
dsi	Disa	1
dsk	Dokshi	1
dsl	Danish Sign Language	1
dsn	Dusner	1
dso	Desiya	1
dsq	Tadaksahak	1
dsz	Mardin Sign Language	1
dta	Daur	1
dtb	Labuk-Kinabatangan Kadazan	1
dtd	Ditidaht	1
dth	Adithinngithigh	1
dti	Ana Tinga Dogon	1
dtk	Tene Kan Dogon	1
dtm	Tomo Kan Dogon	1
dtn	Daatsʼíin	1
dto	Tommo So Dogon	1
dtp	Kadazan Dusun	1
dtr	Lotud	1
dts	Toro So Dogon	1
dtt	Toro Tegu Dogon	1
dtu	Tebul Ure Dogon	1
dty	Dotyali	1
dua	Duala	1
dub	Dubli	1
duc	Duna	1
due	Umiray Dumaget Agta	1
duf	Dumbea	1
dug	Duruma	1
duh	Dungra Bhil	1
dui	Dumun	1
duk	Uyajitaya	1
dul	Alabat Island Agta	1
dum	Middle Dutch (ca. 1050-1350)	1
dun	Dusun Deyah	1
duo	Dupaninan Agta	1
dup	Duano	1
duq	Dusun Malang	1
dur	Dii	1
dus	Dumi	1
duu	Drung	1
duv	Duvle	1
duw	Dusun Witu	1
dux	Duungooma	1
duy	Dicamay Agta	1
duz	Duli-Gey	1
dva	Duau	1
dwa	Diri	1
dwk	Dawik Kui	1
dwr	Dawro	1
dws	Dutton World Speedwords	1
dwu	Dhuwal	1
dww	Dawawa	1
dwy	Dhuwaya	1
dwz	Dewas Rai	1
dya	Dyan	1
dyb	Dyaberdyaber	1
dyd	Dyugun	1
dyg	Villa Viciosa Agta	1
dyi	Djimini Senoufo	1
dym	Yanda Dom Dogon	1
dyn	Dyangadi	1
dyo	Jola-Fonyi	1
dyr	Dyarim	1
dyu	Dyula	1
dyy	Djabugay	1
dza	Tunzu	1
dzd	Daza	1
dze	Djiwarli	1
dzg	Dazaga	1
dzl	Dzalakha	1
dzn	Dzando	1
dzo	Dzongkha	1
eaa	Karenggapa	1
ebc	Beginci	1
ebg	Ebughu	1
ebk	Eastern Bontok	1
ebo	Teke-Ebo	1
ebr	Ebrié	1
ebu	Embu	1
ecr	Eteocretan	1
ecs	Ecuadorian Sign Language	1
ecy	Eteocypriot	1
eee	E	1
efa	Efai	1
efe	Efe	1
efi	Efik	1
ega	Ega	1
egl	Emilian	1
egm	Benamanga	1
ego	Eggon	1
egy	Egyptian (Ancient)	1
ehs	Miyakubo Sign Language	1
ehu	Ehueun	1
eip	Eipomek	1
eit	Eitiep	1
eiv	Askopan	1
eja	Ejamat	1
eka	Ekajuk	1
eke	Ekit	1
ekg	Ekari	1
eki	Eki	1
ekk	Standard Estonian	1
ekl	Kol (Bangladesh)	1
ekm	Elip	1
eko	Koti	1
ekp	Ekpeye	1
ekr	Yace	1
eky	Eastern Kayah	1
ele	Elepi	1
elh	El Hugeirat	1
eli	Nding	1
elk	Elkei	1
ell	Modern Greek (1453-)	1
elm	Eleme	1
elo	El Molo	1
elu	Elu	1
elx	Elamite	1
ema	Emai-Iuleha-Ora	1
emb	Embaloh	1
eme	Emerillon	1
emg	Eastern Meohang	1
emi	Mussau-Emira	1
emk	Eastern Maninkakan	1
emm	Mamulique	1
emn	Eman	1
emp	Northern Emberá	1
emq	Eastern Minyag	1
ems	Pacific Gulf Yupik	1
emu	Eastern Muria	1
emw	Emplawas	1
emx	Erromintxela	1
emy	Epigraphic Mayan	1
emz	Mbessa	1
ena	Apali	1
enb	Markweeta	1
enc	En	1
end	Ende	1
enf	Forest Enets	1
eng	English	1
enh	Tundra Enets	1
enl	Enlhet	1
enm	Middle English (1100-1500)	1
enn	Engenni	1
eno	Enggano	1
enq	Enga	1
enr	Emumu	1
enu	Enu	1
env	Enwan (Edo State)	1
enw	Enwan (Akwa Ibom State)	1
enx	Enxet	1
eot	Beti (Côte d'Ivoire)	1
epi	Epie	1
epo	Esperanto	1
era	Eravallan	1
erg	Sie	1
erh	Eruwa	1
eri	Ogea	1
erk	South Efate	1
ero	Horpa	1
err	Erre	1
ers	Ersu	1
ert	Eritai	1
erw	Erokwanas	1
ese	Ese Ejja	1
esg	Aheri Gondi	1
esh	Eshtehardi	1
esi	North Alaskan Inupiatun	1
esk	Northwest Alaska Inupiatun	1
esl	Egypt Sign Language	1
esm	Esuma	1
esn	Salvadoran Sign Language	1
eso	Estonian Sign Language	1
esq	Esselen	1
ess	Central Siberian Yupik	1
est	Estonian	1
esu	Central Yupik	1
esy	Eskayan	1
etb	Etebi	1
etc	Etchemin	1
eth	Ethiopian Sign Language	1
etn	Eton (Vanuatu)	1
eto	Eton (Cameroon)	1
etr	Edolo	1
ets	Yekhee	1
ett	Etruscan	1
etu	Ejagham	1
etx	Eten	1
etz	Semimi	1
eud	Eudeve	1
eus	Basque	1
eve	Even	1
evh	Uvbie	1
evn	Evenki	1
ewe	Ewe	1
ewo	Ewondo	1
ext	Extremaduran	1
eya	Eyak	1
eyo	Keiyo	1
eza	Ezaa	1
eze	Uzekwe	1
faa	Fasu	1
fab	Fa d'Ambu	1
fad	Wagi	1
faf	Fagani	1
fag	Finongan	1
fah	Baissa Fali	1
fai	Faiwol	1
faj	Faita	1
fak	Fang (Cameroon)	1
fal	South Fali	1
fam	Fam	1
fan	Fang (Equatorial Guinea)	1
fao	Faroese	1
fap	Paloor	1
far	Fataleka	1
fas	Persian	1
fat	Fanti	1
fau	Fayu	1
fax	Fala	1
fay	Southwestern Fars	1
faz	Northwestern Fars	1
fbl	West Albay Bikol	1
fcs	Quebec Sign Language	1
fer	Feroge	1
ffi	Foia Foia	1
ffm	Maasina Fulfulde	1
fgr	Fongoro	1
fia	Nobiin	1
fie	Fyer	1
fif	Faifi	1
fij	Fijian	1
fil	Filipino	1
fin	Finnish	1
fip	Fipa	1
fir	Firan	1
fit	Tornedalen Finnish	1
fiw	Fiwaga	1
fkk	Kirya-Konzəl	1
fkv	Kven Finnish	1
fla	Kalispel-Pend d'Oreille	1
flh	Foau	1
fli	Fali	1
fll	North Fali	1
fln	Flinders Island	1
flr	Fuliiru	1
fly	Flaaitaal	1
fmp	Fe'fe'	1
fmu	Far Western Muria	1
fnb	Fanbak	1
fng	Fanagalo	1
fni	Fania	1
fod	Foodo	1
foi	Foi	1
fom	Foma	1
fon	Fon	1
for	Fore	1
fos	Siraya	1
fpe	Fernando Po Creole English	1
fqs	Fas	1
fra	French	1
frc	Cajun French	1
frd	Fordata	1
frk	Frankish	1
frm	Middle French (ca. 1400-1600)	1
fro	Old French (842-ca. 1400)	1
frp	Arpitan	1
frq	Forak	1
frr	Northern Frisian	1
frs	Eastern Frisian	1
frt	Fortsenal	1
fry	Western Frisian	1
fse	Finnish Sign Language	1
fsl	French Sign Language	1
fss	Finland-Swedish Sign Language	1
fub	Adamawa Fulfulde	1
fuc	Pulaar	1
fud	East Futuna	1
fue	Borgu Fulfulde	1
fuf	Pular	1
fuh	Western Niger Fulfulde	1
fui	Bagirmi Fulfulde	1
fuj	Ko	1
ful	Fulah	1
fum	Fum	1
fun	Fulniô	1
fuq	Central-Eastern Niger Fulfulde	1
fur	Friulian	1
fut	Futuna-Aniwa	1
fuu	Furu	1
fuv	Nigerian Fulfulde	1
fuy	Fuyug	1
fvr	Fur	1
fwa	Fwâi	1
fwe	Fwe	1
gaa	Ga	1
gab	Gabri	1
gac	Mixed Great Andamanese	1
gad	Gaddang	1
gae	Guarequena	1
gaf	Gende	1
gag	Gagauz	1
gah	Alekano	1
gai	Borei	1
gaj	Gadsup	1
gak	Gamkonora	1
gal	Galolen	1
gam	Kandawo	1
gan	Gan Chinese	1
gao	Gants	1
gap	Gal	1
gaq	Gata'	1
gar	Galeya	1
gas	Adiwasi Garasia	1
gat	Kenati	1
gau	Mudhili Gadaba	1
gaw	Nobonob	1
gax	Borana-Arsi-Guji Oromo	1
gay	Gayo	1
gaz	West Central Oromo	1
gba	Gbaya (Central African Republic)	1
gbb	Kaytetye	1
gbd	Karajarri	1
gbe	Niksek	1
gbf	Gaikundi	1
gbg	Gbanziri	1
gbh	Defi Gbe	1
gbi	Galela	1
gbj	Bodo Gadaba	1
gbk	Gaddi	1
gbl	Gamit	1
gbm	Garhwali	1
gbn	Mo'da	1
gbo	Northern Grebo	1
gbp	Gbaya-Bossangoa	1
gbq	Gbaya-Bozoum	1
gbr	Gbagyi	1
gbs	Gbesi Gbe	1
gbu	Gagadu	1
gbv	Gbanu	1
gbw	Gabi-Gabi	1
gbx	Eastern Xwla Gbe	1
gby	Gbari	1
gbz	Zoroastrian Dari	1
gcc	Mali	1
gcd	Ganggalida	1
gce	Galice	1
gcf	Guadeloupean Creole French	1
gcl	Grenadian Creole English	1
gcn	Gaina	1
gcr	Guianese Creole French	1
gct	Colonia Tovar German	1
gda	Gade Lohar	1
gdb	Pottangi Ollar Gadaba	1
gdc	Gugu Badhun	1
gdd	Gedaged	1
gde	Gude	1
gdf	Guduf-Gava	1
gdg	Ga'dang	1
gdh	Gadjerawang	1
gdi	Gundi	1
gdj	Gurdjar	1
gdk	Gadang	1
gdl	Dirasha	1
gdm	Laal	1
gdn	Umanakaina	1
gdo	Ghodoberi	1
gdq	Mehri	1
gdr	Wipi	1
gds	Ghandruk Sign Language	1
gdt	Kungardutyi	1
gdu	Gudu	1
gdx	Godwari	1
gea	Geruma	1
geb	Kire	1
gec	Gboloo Grebo	1
ged	Gade	1
gef	Gerai	1
geg	Gengle	1
geh	Hutterite German	1
gei	Gebe	1
gej	Gen	1
gek	Ywom	1
gel	ut-Ma'in	1
geq	Geme	1
ges	Geser-Gorom	1
gev	Eviya	1
gew	Gera	1
gex	Garre	1
gey	Enya	1
gez	Geez	1
gfk	Patpatar	1
gft	Gafat	1
gga	Gao	1
ggb	Gbii	1
ggd	Gugadj	1
gge	Gurr-goni	1
ggg	Gurgula	1
ggk	Kungarakany	1
ggl	Ganglau	1
ggt	Gitua	1
ggu	Gagu	1
ggw	Gogodala	1
gha	Ghadamès	1
ghc	Hiberno-Scottish Gaelic	1
ghe	Southern Ghale	1
ghh	Northern Ghale	1
ghk	Geko Karen	1
ghl	Ghulfan	1
ghn	Ghanongga	1
gho	Ghomara	1
ghr	Ghera	1
ghs	Guhu-Samane	1
ght	Kuke	1
gia	Kija	1
gib	Gibanawa	1
gic	Gail	1
gid	Gidar	1
gie	Gaɓogbo	1
gig	Goaria	1
gih	Githabul	1
gii	Girirra	1
gil	Gilbertese	1
gim	Gimi (Eastern Highlands)	1
gin	Hinukh	1
gip	Gimi (West New Britain)	1
giq	Green Gelao	1
gir	Red Gelao	1
gis	North Giziga	1
git	Gitxsan	1
giu	Mulao	1
giw	White Gelao	1
gix	Gilima	1
giy	Giyug	1
giz	South Giziga	1
gjk	Kachi Koli	1
gjm	Gunditjmara	1
gjn	Gonja	1
gjr	Gurindji Kriol	1
gju	Gujari	1
gka	Guya	1
gkd	Magɨ (Madang Province)	1
gke	Ndai	1
gkn	Gokana	1
gko	Kok-Nar	1
gkp	Guinea Kpelle	1
gku	ǂUngkue	1
gla	Scottish Gaelic	1
glb	Belning	1
glc	Bon Gula	1
gld	Nanai	1
gle	Irish	1
glg	Galician	1
glh	Northwest Pashai	1
glj	Gula Iro	1
glk	Gilaki	1
gll	Garlali	1
glo	Galambu	1
glr	Glaro-Twabo	1
glu	Gula (Chad)	1
glv	Manx	1
glw	Glavda	1
gly	Gule	1
gma	Gambera	1
gmb	Gula'alaa	1
gmd	Mághdì	1
gmg	Magɨyi	1
gmh	Middle High German (ca. 1050-1500)	1
gml	Middle Low German	1
gmm	Gbaya-Mbodomo	1
gmn	Gimnime	1
gmr	Mirning	1
gmu	Gumalu	1
gmv	Gamo	1
gmx	Magoma	1
gmy	Mycenaean Greek	1
gmz	Mgbolizhia	1
gna	Kaansa	1
gnb	Gangte	1
gnc	Guanche	1
gnd	Zulgo-Gemzek	1
gne	Ganang	1
gng	Ngangam	1
gnh	Lere	1
gni	Gooniyandi	1
gnj	Ngen	1
gnk	ǁGana	1
gnl	Gangulu	1
gnm	Ginuman	1
gnn	Gumatj	1
gno	Northern Gondi	1
gnq	Gana	1
gnr	Gureng Gureng	1
gnt	Guntai	1
gnu	Gnau	1
gnw	Western Bolivian Guaraní	1
gnz	Ganzi	1
goa	Guro	1
gob	Playero	1
goc	Gorakor	1
god	Godié	1
goe	Gongduk	1
gof	Gofa	1
gog	Gogo	1
goh	Old High German (ca. 750-1050)	1
goi	Gobasi	1
goj	Gowlan	1
gok	Gowli	1
gol	Gola	1
gom	Goan Konkani	1
gon	Gondi	1
goo	Gone Dau	1
gop	Yeretuar	1
goq	Gorap	1
gor	Gorontalo	1
gos	Gronings	1
got	Gothic	1
gou	Gavar	1
gov	Goo	1
gow	Gorowa	1
gox	Gobu	1
goy	Goundo	1
goz	Gozarkhani	1
gpa	Gupa-Abawa	1
gpe	Ghanaian Pidgin English	1
gpn	Taiap	1
gqa	Ga'anda	1
gqi	Guiqiong	1
gqn	Guana (Brazil)	1
gqr	Gor	1
gqu	Qau	1
gra	Rajput Garasia	1
grb	Grebo	1
grc	Ancient Greek (to 1453)	1
grd	Guruntum-Mbaaru	1
grg	Madi	1
grh	Gbiri-Niragu	1
gri	Ghari	1
grj	Southern Grebo	1
grm	Kota Marudu Talantang	1
grn	Guarani	1
gro	Groma	1
grq	Gorovu	1
grr	Taznatit	1
grs	Gresi	1
grt	Garo	1
gru	Kistane	1
grv	Central Grebo	1
grw	Gweda	1
grx	Guriaso	1
gry	Barclayville Grebo	1
grz	Guramalum	1
gse	Ghanaian Sign Language	1
gsg	German Sign Language	1
gsl	Gusilay	1
gsm	Guatemalan Sign Language	1
gsn	Nema	1
gso	Southwest Gbaya	1
gsp	Wasembo	1
gss	Greek Sign Language	1
gsw	Swiss German	1
gta	Guató	1
gtu	Aghu-Tharnggala	1
gua	Shiki	1
gub	Guajajára	1
guc	Wayuu	1
gud	Yocoboué Dida	1
gue	Gurindji	1
guf	Gupapuyngu	1
gug	Paraguayan Guaraní	1
guh	Guahibo	1
gui	Eastern Bolivian Guaraní	1
guj	Gujarati	1
guk	Gumuz	1
gul	Sea Island Creole English	1
gum	Guambiano	1
gun	Mbyá Guaraní	1
guo	Guayabero	1
gup	Gunwinggu	1
guq	Aché	1
gur	Farefare	1
gus	Guinean Sign Language	1
gut	Maléku Jaíka	1
guu	Yanomamö	1
guw	Gun	1
gux	Gourmanchéma	1
guz	Gusii	1
gva	Guana (Paraguay)	1
gvc	Guanano	1
gve	Duwet	1
gvf	Golin	1
gvj	Guajá	1
gvl	Gulay	1
gvm	Gurmana	1
gvn	Kuku-Yalanji	1
gvo	Gavião Do Jiparaná	1
gvp	Pará Gavião	1
gvr	Gurung	1
gvs	Gumawana	1
gvy	Guyani	1
gwa	Mbato	1
gwb	Gwa	1
gwc	Gawri	1
gwd	Gawwada	1
gwe	Gweno	1
gwf	Gowro	1
gwg	Moo	1
gwi	Gwichʼin	1
gwj	ǀGwi	1
gwm	Awngthim	1
gwn	Gwandara	1
gwr	Gwere	1
gwt	Gawar-Bati	1
gwu	Guwamu	1
gww	Kwini	1
gwx	Gua	1
gxx	Wè Southern	1
gya	Northwest Gbaya	1
gyb	Garus	1
gyd	Kayardild	1
gye	Gyem	1
gyf	Gungabula	1
gyg	Gbayi	1
gyi	Gyele	1
gyl	Gayil	1
gym	Ngäbere	1
gyn	Guyanese Creole English	1
gyo	Gyalsumdo	1
gyr	Guarayu	1
gyy	Gunya	1
gyz	Geji	1
gza	Ganza	1
gzi	Gazi	1
gzn	Gane	1
haa	Han	1
hab	Hanoi Sign Language	1
hac	Gurani	1
had	Hatam	1
hae	Eastern Oromo	1
haf	Haiphong Sign Language	1
hag	Hanga	1
hah	Hahon	1
hai	Haida	1
haj	Hajong	1
hak	Hakka Chinese	1
hal	Halang	1
ham	Hewa	1
han	Hangaza	1
hao	Hakö	1
hap	Hupla	1
haq	Ha	1
har	Harari	1
has	Haisla	1
hat	Haitian	1
hau	Hausa	1
hav	Havu	1
haw	Hawaiian	1
hax	Southern Haida	1
hay	Haya	1
haz	Hazaragi	1
hba	Hamba	1
hbb	Huba	1
hbn	Heiban	1
hbo	Ancient Hebrew	1
hbs	Serbo-Croatian\tCode element for 639-1 has been deprecated	1
hbu	Habu	1
hca	Andaman Creole Hindi	1
hch	Huichol	1
hdn	Northern Haida	1
hds	Honduras Sign Language	1
hdy	Hadiyya	1
hea	Northern Qiandong Miao	1
heb	Hebrew	1
hed	Herdé	1
heg	Helong	1
heh	Hehe	1
hei	Heiltsuk	1
hem	Hemba	1
her	Herero	1
hgm	Haiǁom	1
hgw	Haigwai	1
hhi	Hoia Hoia	1
hhr	Kerak	1
hhy	Hoyahoya	1
hia	Lamang	1
hib	Hibito	1
hid	Hidatsa	1
hif	Fiji Hindi	1
hig	Kamwe	1
hih	Pamosu	1
hii	Hinduri	1
hij	Hijuk	1
hik	Seit-Kaitetu	1
hil	Hiligaynon	1
hin	Hindi	1
hio	Tsoa	1
hir	Himarimã	1
hit	Hittite	1
hiw	Hiw	1
hix	Hixkaryána	1
hji	Haji	1
hka	Kahe	1
hke	Hunde	1
hkh	Khah	1
hkk	Hunjara-Kaina Ke	1
hkn	Mel-Khaonh	1
hks	Hong Kong Sign Language	1
hla	Halia	1
hlb	Halbi	1
hld	Halang Doan	1
hle	Hlersu	1
hlt	Matu Chin	1
hlu	Hieroglyphic Luwian	1
hma	Southern Mashan Hmong	1
hmb	Humburi Senni Songhay	1
hmc	Central Huishui Hmong	1
hmd	Large Flowery Miao	1
hme	Eastern Huishui Hmong	1
hmf	Hmong Don	1
hmg	Southwestern Guiyang Hmong	1
hmh	Southwestern Huishui Hmong	1
hmi	Northern Huishui Hmong	1
hmj	Ge	1
hmk	Maek	1
hml	Luopohe Hmong	1
hmm	Central Mashan Hmong	1
hmn	Hmong	1
hmo	Hiri Motu	1
hmp	Northern Mashan Hmong	1
hmq	Eastern Qiandong Miao	1
hmr	Hmar	1
hms	Southern Qiandong Miao	1
hmt	Hamtai	1
hmu	Hamap	1
hmv	Hmong Dô	1
hmw	Western Mashan Hmong	1
hmy	Southern Guiyang Hmong	1
hmz	Hmong Shua	1
hna	Mina (Cameroon)	1
hnd	Southern Hindko	1
hne	Chhattisgarhi	1
hng	Hungu	1
hnh	ǁAni	1
hni	Hani	1
hnj	Hmong Njua	1
hnn	Hanunoo	1
hno	Northern Hindko	1
hns	Caribbean Hindustani	1
hnu	Hung	1
hoa	Hoava	1
hob	Mari (Madang Province)	1
hoc	Ho	1
hod	Holma	1
hoe	Horom	1
hoh	Hobyót	1
hoi	Holikachuk	1
hoj	Hadothi	1
hol	Holu	1
hom	Homa	1
hoo	Holoholo	1
hop	Hopi	1
hor	Horo	1
hos	Ho Chi Minh City Sign Language	1
hot	Hote	1
hov	Hovongan	1
how	Honi	1
hoy	Holiya	1
hoz	Hozo	1
hpo	Hpon	1
hps	Hawai'i Sign Language (HSL)	1
hra	Hrangkhol	1
hrc	Niwer Mil	1
hre	Hre	1
hrk	Haruku	1
hrm	Horned Miao	1
hro	Haroi	1
hrp	Nhirrpi	1
hrt	Hértevin	1
hru	Hruso	1
hrv	Croatian	1
hrw	Warwar Feni	1
hrx	Hunsrik	1
hrz	Harzani	1
hsb	Upper Sorbian	1
hsh	Hungarian Sign Language	1
hsl	Hausa Sign Language	1
hsn	Xiang Chinese	1
hss	Harsusi	1
hti	Hoti	1
hto	Minica Huitoto	1
hts	Hadza	1
htu	Hitu	1
htx	Middle Hittite	1
hub	Huambisa	1
huc	ǂHua	1
hud	Huaulu	1
hue	San Francisco Del Mar Huave	1
huf	Humene	1
hug	Huachipaeri	1
huh	Huilliche	1
hui	Huli	1
huj	Northern Guiyang Hmong	1
huk	Hulung	1
hul	Hula	1
hum	Hungana	1
hun	Hungarian	1
huo	Hu	1
hup	Hupa	1
huq	Tsat	1
hur	Halkomelem	1
hus	Huastec	1
hut	Humla	1
huu	Murui Huitoto	1
huv	San Mateo Del Mar Huave	1
huw	Hukumina	1
hux	Nüpode Huitoto	1
huy	Hulaulá	1
huz	Hunzib	1
hvc	Haitian Vodoun Culture Language	1
hve	San Dionisio Del Mar Huave	1
hvk	Haveke	1
hvn	Sabu	1
hvv	Santa María Del Mar Huave	1
hwa	Wané	1
hwc	Hawai'i Creole English	1
hwo	Hwana	1
hya	Hya	1
hye	Armenian	1
hyw	Western Armenian	1
iai	Iaai	1
ian	Iatmul	1
iar	Purari	1
iba	Iban	1
ibb	Ibibio	1
ibd	Iwaidja	1
ibe	Akpes	1
ibg	Ibanag	1
ibh	Bih	1
ibl	Ibaloi	1
ibm	Agoi	1
ibn	Ibino	1
ibo	Igbo	1
ibr	Ibuoro	1
ibu	Ibu	1
iby	Ibani	1
ica	Ede Ica	1
ich	Etkywan	1
icl	Icelandic Sign Language	1
icr	Islander Creole English	1
ida	Idakho-Isukha-Tiriki	1
idb	Indo-Portuguese	1
idc	Idon	1
idd	Ede Idaca	1
ide	Idere	1
idi	Idi	1
ido	Ido	1
idr	Indri	1
ids	Idesa	1
idt	Idaté	1
idu	Idoma	1
ifa	Amganad Ifugao	1
ifb	Batad Ifugao	1
ife	Ifè	1
iff	Ifo	1
ifk	Tuwali Ifugao	1
ifm	Teke-Fuumu	1
ifu	Mayoyao Ifugao	1
ify	Keley-I Kallahan	1
igb	Ebira	1
ige	Igede	1
igg	Igana	1
igl	Igala	1
igm	Kanggape	1
ign	Ignaciano	1
igo	Isebe	1
igs	Interglossa	1
igw	Igwe	1
ihb	Iha Based Pidgin	1
ihi	Ihievbe	1
ihp	Iha	1
ihw	Bidhawal	1
iii	Sichuan Yi	1
iin	Thiin	1
ijc	Izon	1
ije	Biseni	1
ijj	Ede Ije	1
ijn	Kalabari	1
ijs	Southeast Ijo	1
ike	Eastern Canadian Inuktitut	1
ikh	Ikhin-Arokho	1
iki	Iko	1
ikk	Ika	1
ikl	Ikulu	1
iko	Olulumo-Ikom	1
ikp	Ikpeshi	1
ikr	Ikaranggal	1
iks	Inuit Sign Language	1
ikt	Inuinnaqtun	1
iku	Inuktitut	1
ikv	Iku-Gora-Ankwa	1
ikw	Ikwere	1
ikx	Ik	1
ikz	Ikizu	1
ila	Ile Ape	1
ilb	Ila	1
ile	Interlingue	1
ilg	Garig-Ilgar	1
ili	Ili Turki	1
ilk	Ilongot	1
ilm	Iranun (Malaysia)	1
ilo	Iloko	1
ilp	Iranun (Philippines)	1
ils	International Sign	1
ilu	Ili'uun	1
ilv	Ilue	1
ima	Mala Malasar	1
imi	Anamgura	1
iml	Miluk	1
imn	Imonda	1
imo	Imbongu	1
imr	Imroing	1
ims	Marsian	1
imt	Imotong	1
imy	Milyan	1
ina	Interlingua (International Auxiliary Language Association)	1
inb	Inga	1
ind	Indonesian	1
ing	Degexit'an	1
inh	Ingush	1
inj	Jungle Inga	1
inl	Indonesian Sign Language	1
inm	Minaean	1
inn	Isinai	1
ino	Inoke-Yate	1
inp	Iñapari	1
ins	Indian Sign Language	1
int	Intha	1
inz	Ineseño	1
ior	Inor	1
iou	Tuma-Irumu	1
iow	Iowa-Oto	1
ipi	Ipili	1
ipk	Inupiaq	1
ipo	Ipiko	1
iqu	Iquito	1
iqw	Ikwo	1
ire	Iresim	1
irh	Irarutu	1
iri	Rigwe	1
irk	Iraqw	1
irn	Irántxe	1
irr	Ir	1
iru	Irula	1
irx	Kamberau	1
iry	Iraya	1
isa	Isabi	1
isc	Isconahua	1
isd	Isnag	1
ise	Italian Sign Language	1
isg	Irish Sign Language	1
ish	Esan	1
isi	Nkem-Nkum	1
isk	Ishkashimi	1
isl	Icelandic	1
ism	Masimasi	1
isn	Isanzu	1
iso	Isoko	1
isr	Israeli Sign Language	1
ist	Istriot	1
isu	Isu (Menchum Division)	1
ita	Italian	1
itb	Binongan Itneg	1
itd	Southern Tidung	1
ite	Itene	1
iti	Inlaod Itneg	1
itk	Judeo-Italian	1
itl	Itelmen	1
itm	Itu Mbon Uzo	1
ito	Itonama	1
itr	Iteri	1
its	Isekiri	1
itt	Maeng Itneg	1
itv	Itawit	1
itw	Ito	1
itx	Itik	1
ity	Moyadan Itneg	1
itz	Itzá	1
ium	Iu Mien	1
ivb	Ibatan	1
ivv	Ivatan	1
iwk	I-Wak	1
iwm	Iwam	1
iwo	Iwur	1
iws	Sepik Iwam	1
ixc	Ixcatec	1
ixl	Ixil	1
iya	Iyayu	1
iyo	Mesaka	1
iyx	Yaka (Congo)	1
izh	Ingrian	1
izm	Kizamani	1
izr	Izere	1
izz	Izii	1
jaa	Jamamadí	1
jab	Hyam	1
jac	Popti'	1
jad	Jahanka	1
jae	Yabem	1
jaf	Jara	1
jah	Jah Hut	1
jaj	Zazao	1
jak	Jakun	1
jal	Yalahatan	1
jam	Jamaican Creole English	1
jan	Jandai	1
jao	Yanyuwa	1
jaq	Yaqay	1
jas	New Caledonian Javanese	1
jat	Jakati	1
jau	Yaur	1
jav	Javanese	1
jax	Jambi Malay	1
jay	Yan-nhangu	1
jaz	Jawe	1
jbe	Judeo-Berber	1
jbi	Badjiri	1
jbj	Arandai	1
jbk	Barikewa	1
jbm	Bijim	1
jbn	Nafusi	1
jbo	Lojban	1
jbr	Jofotek-Bromnya	1
jbt	Jabutí	1
jbu	Jukun Takum	1
jbw	Yawijibaya	1
jcs	Jamaican Country Sign Language	1
jct	Krymchak	1
jda	Jad	1
jdg	Jadgali	1
jdt	Judeo-Tat	1
jeb	Jebero	1
jee	Jerung	1
jeh	Jeh	1
jei	Yei	1
jek	Jeri Kuo	1
jel	Yelmek	1
jen	Dza	1
jer	Jere	1
jet	Manem	1
jeu	Jonkor Bourmataguil	1
jgb	Ngbee	1
jge	Judeo-Georgian	1
jgk	Gwak	1
jgo	Ngomba	1
jhi	Jehai	1
jhs	Jhankot Sign Language	1
jia	Jina	1
jib	Jibu	1
jic	Tol	1
jid	Bu (Kaduna State)	1
jie	Jilbe	1
jig	Jingulu	1
jih	sTodsde	1
jii	Jiiddu	1
jil	Jilim	1
jim	Jimi (Cameroon)	1
jio	Jiamao	1
jiq	Guanyinqiao	1
jit	Jita	1
jiu	Youle Jinuo	1
jiv	Shuar	1
jiy	Buyuan Jinuo	1
jje	Jejueo	1
jjr	Bankal	1
jka	Kaera	1
jkm	Mobwa Karen	1
jko	Kubo	1
jkp	Paku Karen	1
jkr	Koro (India)	1
jks	Amami Koniya Sign Language	1
jku	Labir	1
jle	Ngile	1
jls	Jamaican Sign Language	1
jma	Dima	1
jmb	Zumbun	1
jmc	Machame	1
jmd	Yamdena	1
jmi	Jimi (Nigeria)	1
jml	Jumli	1
jmn	Makuri Naga	1
jmr	Kamara	1
jms	Mashi (Nigeria)	1
jmw	Mouwase	1
jmx	Western Juxtlahuaca Mixtec	1
jna	Jangshung	1
jnd	Jandavra	1
jng	Yangman	1
jni	Janji	1
jnj	Yemsa	1
jnl	Rawat	1
jns	Jaunsari	1
job	Joba	1
jod	Wojenaka	1
jog	Jogi	1
jor	Jorá	1
jos	Jordanian Sign Language	1
jow	Jowulu	1
jpa	Jewish Palestinian Aramaic	1
jpn	Japanese	1
jpr	Judeo-Persian	1
jqr	Jaqaru	1
jra	Jarai	1
jrb	Judeo-Arabic	1
jrr	Jiru	1
jrt	Jakattoe	1
jru	Japrería	1
jsl	Japanese Sign Language	1
jua	Júma	1
jub	Wannu	1
juc	Jurchen	1
jud	Worodougou	1
juh	Hõne	1
jui	Ngadjuri	1
juk	Wapan	1
jul	Jirel	1
jum	Jumjum	1
jun	Juang	1
juo	Jiba	1
jup	Hupdë	1
jur	Jurúna	1
jus	Jumla Sign Language	1
jut	Jutish	1
juu	Ju	1
juw	Wãpha	1
juy	Juray	1
jvd	Javindo	1
jvn	Caribbean Javanese	1
jwi	Jwira-Pepesa	1
jya	Jiarong	1
jye	Judeo-Yemeni Arabic	1
jyy	Jaya	1
kaa	Kara-Kalpak	1
kab	Kabyle	1
kac	Kachin	1
kad	Adara	1
kae	Ketangalan	1
kaf	Katso	1
kag	Kajaman	1
kah	Kara (Central African Republic)	1
kai	Karekare	1
kaj	Jju	1
kak	Kalanguya	1
kal	Kalaallisut	1
kam	Kamba (Kenya)	1
kan	Kannada	1
kao	Xaasongaxango	1
kap	Bezhta	1
kaq	Capanahua	1
kas	Kashmiri	1
kat	Georgian	1
kau	Kanuri	1
kav	Katukína	1
kaw	Kawi	1
kax	Kao	1
kay	Kamayurá	1
kaz	Kazakh	1
kba	Kalarko	1
kbb	Kaxuiâna	1
kbc	Kadiwéu	1
kbd	Kabardian	1
kbe	Kanju	1
kbg	Khamba	1
kbh	Camsá	1
kbi	Kaptiau	1
kbj	Kari	1
kbk	Grass Koiari	1
kbl	Kanembu	1
kbm	Iwal	1
kbn	Kare (Central African Republic)	1
kbo	Keliko	1
kbp	Kabiyè	1
kbq	Kamano	1
kbr	Kafa	1
kbs	Kande	1
kbt	Abadi	1
kbu	Kabutra	1
kbv	Dera (Indonesia)	1
kbw	Kaiep	1
kbx	Ap Ma	1
kby	Manga Kanuri	1
kbz	Duhwa	1
kca	Khanty	1
kcb	Kawacha	1
kcc	Lubila	1
kcd	Ngkâlmpw Kanum	1
kce	Kaivi	1
kcf	Ukaan	1
kcg	Tyap	1
kch	Vono	1
kci	Kamantan	1
kcj	Kobiana	1
kck	Kalanga	1
kcl	Kela (Papua New Guinea)	1
kcm	Gula (Central African Republic)	1
kcn	Nubi	1
kco	Kinalakna	1
kcp	Kanga	1
kcq	Kamo	1
kcr	Katla	1
kcs	Koenoem	1
kct	Kaian	1
kcu	Kami (Tanzania)	1
kcv	Kete	1
kcw	Kabwari	1
kcx	Kachama-Ganjule	1
kcy	Korandje	1
kcz	Konongo	1
kda	Worimi	1
kdc	Kutu	1
kdd	Yankunytjatjara	1
kde	Makonde	1
kdf	Mamusi	1
kdg	Seba	1
kdh	Tem	1
kdi	Kumam	1
kdj	Karamojong	1
kdk	Numèè	1
kdl	Tsikimba	1
kdm	Kagoma	1
kdn	Kunda	1
kdp	Kaningdon-Nindem	1
kdq	Koch	1
kdr	Karaim	1
kdt	Kuy	1
kdu	Kadaru	1
kdw	Koneraw	1
kdx	Kam	1
kdy	Keder	1
kdz	Kwaja	1
kea	Kabuverdianu	1
keb	Kélé	1
kec	Keiga	1
ked	Kerewe	1
kee	Eastern Keres	1
kef	Kpessi	1
keg	Tese	1
keh	Keak	1
kei	Kei	1
kej	Kadar	1
kek	Kekchí	1
kel	Kela (Democratic Republic of Congo)	1
kem	Kemak	1
ken	Kenyang	1
keo	Kakwa	1
kep	Kaikadi	1
keq	Kamar	1
ker	Kera	1
kes	Kugbo	1
ket	Ket	1
keu	Akebu	1
kev	Kanikkaran	1
kew	West Kewa	1
kex	Kukna	1
key	Kupia	1
kez	Kukele	1
kfa	Kodava	1
kfb	Northwestern Kolami	1
kfc	Konda-Dora	1
kfd	Korra Koraga	1
kfe	Kota (India)	1
kff	Koya	1
kfg	Kudiya	1
kfh	Kurichiya	1
kfi	Kannada Kurumba	1
kfj	Kemiehua	1
kfk	Kinnauri	1
kfl	Kung	1
kfm	Khunsari	1
kfn	Kuk	1
kfo	Koro (Côte d'Ivoire)	1
kfp	Korwa	1
kfq	Korku	1
kfr	Kachhi	1
kfs	Bilaspuri	1
kft	Kanjari	1
kfu	Katkari	1
kfv	Kurmukar	1
kfw	Kharam Naga	1
kfx	Kullu Pahari	1
kfy	Kumaoni	1
kfz	Koromfé	1
kga	Koyaga	1
kgb	Kawe	1
kge	Komering	1
kgf	Kube	1
kgg	Kusunda	1
kgi	Selangor Sign Language	1
kgj	Gamale Kham	1
kgk	Kaiwá	1
kgl	Kunggari	1
kgn	Karingani	1
kgo	Krongo	1
kgp	Kaingang	1
kgq	Kamoro	1
kgr	Abun	1
kgs	Kumbainggar	1
kgt	Somyev	1
kgu	Kobol	1
kgv	Karas	1
kgw	Karon Dori	1
kgx	Kamaru	1
kgy	Kyerung	1
kha	Khasi	1
khb	Lü	1
khc	Tukang Besi North	1
khd	Bädi Kanum	1
khe	Korowai	1
khf	Khuen	1
khg	Khams Tibetan	1
khh	Kehu	1
khj	Kuturmi	1
khk	Halh Mongolian	1
khl	Lusi	1
khm	Khmer	1
khn	Khandesi	1
kho	Khotanese	1
khp	Kapori	1
khq	Koyra Chiini Songhay	1
khr	Kharia	1
khs	Kasua	1
kht	Khamti	1
khu	Nkhumbi	1
khv	Khvarshi	1
khw	Khowar	1
khx	Kanu	1
khy	Kele (Democratic Republic of Congo)	1
khz	Keapara	1
kia	Kim	1
kib	Koalib	1
kic	Kickapoo	1
kid	Koshin	1
kie	Kibet	1
kif	Eastern Parbate Kham	1
kig	Kimaama	1
kih	Kilmeri	1
kii	Kitsai	1
kij	Kilivila	1
kik	Kikuyu	1
kil	Kariya	1
kim	Karagas	1
kin	Kinyarwanda	1
kio	Kiowa	1
kip	Sheshi Kham	1
kiq	Kosadle	1
kir	Kirghiz	1
kis	Kis	1
kit	Agob	1
kiu	Kirmanjki (individual language)	1
kiv	Kimbu	1
kiw	Northeast Kiwai	1
kix	Khiamniungan Naga	1
kiy	Kirikiri	1
kiz	Kisi	1
kja	Mlap	1
kjb	Q'anjob'al	1
kjc	Coastal Konjo	1
kjd	Southern Kiwai	1
kje	Kisar	1
kjg	Khmu	1
kjh	Khakas	1
kji	Zabana	1
kjj	Khinalugh	1
kjk	Highland Konjo	1
kjl	Western Parbate Kham	1
kjm	Kháng	1
kjn	Kunjen	1
kjo	Harijan Kinnauri	1
kjp	Pwo Eastern Karen	1
kjq	Western Keres	1
kjr	Kurudu	1
kjs	East Kewa	1
kjt	Phrae Pwo Karen	1
kju	Kashaya	1
kjv	Kaikavian Literary Language	1
kjx	Ramopa	1
kjy	Erave	1
kjz	Bumthangkha	1
kka	Kakanda	1
kkb	Kwerisa	1
kkc	Odoodee	1
kkd	Kinuku	1
kke	Kakabe	1
kkf	Kalaktang Monpa	1
kkg	Mabaka Valley Kalinga	1
kkh	Khün	1
kki	Kagulu	1
kkj	Kako	1
kkk	Kokota	1
kkl	Kosarek Yale	1
kkm	Kiong	1
kkn	Kon Keu	1
kko	Karko	1
kkp	Gugubera	1
kkq	Kaeku	1
kkr	Kir-Balar	1
kks	Giiwo	1
kkt	Koi	1
kku	Tumi	1
kkv	Kangean	1
kkw	Teke-Kukuya	1
kkx	Kohin	1
kky	Guugu Yimidhirr	1
kkz	Kaska	1
kla	Klamath-Modoc	1
klb	Kiliwa	1
klc	Kolbila	1
kld	Gamilaraay	1
kle	Kulung (Nepal)	1
klf	Kendeje	1
klg	Tagakaulo	1
klh	Weliki	1
kli	Kalumpang	1
klj	Khalaj	1
klk	Kono (Nigeria)	1
kll	Kagan Kalagan	1
klm	Migum	1
kln	Kalenjin	1
klo	Kapya	1
klp	Kamasa	1
klq	Rumu	1
klr	Khaling	1
kls	Kalasha	1
klt	Nukna	1
klu	Klao	1
klv	Maskelynes	1
klw	Tado	1
klx	Koluwawa	1
kly	Kalao	1
klz	Kabola	1
kma	Konni	1
kmb	Kimbundu	1
kmc	Southern Dong	1
kmd	Majukayang Kalinga	1
kme	Bakole	1
kmf	Kare (Papua New Guinea)	1
kmg	Kâte	1
kmh	Kalam	1
kmi	Kami (Nigeria)	1
kmj	Kumarbhag Paharia	1
kmk	Limos Kalinga	1
kml	Tanudan Kalinga	1
kmm	Kom (India)	1
kmn	Awtuw	1
kmo	Kwoma	1
kmp	Gimme	1
kmq	Kwama	1
kmr	Northern Kurdish	1
kms	Kamasau	1
kmt	Kemtuik	1
kmu	Kanite	1
kmv	Karipúna Creole French	1
kmw	Komo (Democratic Republic of Congo)	1
kmx	Waboda	1
kmy	Koma	1
kmz	Khorasani Turkish	1
kna	Dera (Nigeria)	1
knb	Lubuagan Kalinga	1
knc	Central Kanuri	1
knd	Konda	1
kne	Kankanaey	1
knf	Mankanya	1
kng	Koongo	1
kni	Kanufi	1
knj	Western Kanjobal	1
knk	Kuranko	1
knl	Keninjal	1
knm	Kanamarí	1
knn	Konkani (individual language)	1
kno	Kono (Sierra Leone)	1
knp	Kwanja	1
knq	Kintaq	1
knr	Kaningra	1
kns	Kensiu	1
knt	Panoan Katukína	1
knu	Kono (Guinea)	1
knv	Tabo	1
knw	Kung-Ekoka	1
knx	Kendayan	1
kny	Kanyok	1
knz	Kalamsé	1
koa	Konomala	1
koc	Kpati	1
kod	Kodi	1
koe	Kacipo-Bale Suri	1
kof	Kubi	1
kog	Cogui	1
koh	Koyo	1
koi	Komi-Permyak	1
kok	Konkani (macrolanguage)	1
kol	Kol (Papua New Guinea)	1
kom	Komi	1
kon	Kongo	1
koo	Konzo	1
kop	Waube	1
koq	Kota (Gabon)	1
kor	Korean	1
kos	Kosraean	1
kot	Lagwan	1
kou	Koke	1
kov	Kudu-Camo	1
kow	Kugama	1
koy	Koyukon	1
koz	Korak	1
kpa	Kutto	1
kpb	Mullu Kurumba	1
kpc	Curripaco	1
kpd	Koba	1
kpe	Kpelle	1
kpf	Komba	1
kpg	Kapingamarangi	1
kph	Kplang	1
kpi	Kofei	1
kpj	Karajá	1
kpk	Kpan	1
kpl	Kpala	1
kpm	Koho	1
kpn	Kepkiriwát	1
kpo	Ikposo	1
kpq	Korupun-Sela	1
kpr	Korafe-Yegha	1
kps	Tehit	1
kpt	Karata	1
kpu	Kafoa	1
kpv	Komi-Zyrian	1
kpw	Kobon	1
kpx	Mountain Koiali	1
kpy	Koryak	1
kpz	Kupsabiny	1
kqa	Mum	1
kqb	Kovai	1
kqc	Doromu-Koki	1
kqd	Koy Sanjaq Surat	1
kqe	Kalagan	1
kqf	Kakabai	1
kqg	Khe	1
kqh	Kisankasa	1
kqi	Koitabu	1
kqj	Koromira	1
kqk	Kotafon Gbe	1
kql	Kyenele	1
kqm	Khisa	1
kqn	Kaonde	1
kqo	Eastern Krahn	1
kqp	Kimré	1
kqq	Krenak	1
kqr	Kimaragang	1
kqs	Northern Kissi	1
kqt	Klias River Kadazan	1
kqu	Seroa	1
kqv	Okolod	1
kqw	Kandas	1
kqx	Mser	1
kqy	Koorete	1
kqz	Korana	1
kra	Kumhali	1
krb	Karkin	1
krc	Karachay-Balkar	1
krd	Kairui-Midiki	1
kre	Panará	1
krf	Koro (Vanuatu)	1
krh	Kurama	1
kri	Krio	1
krj	Kinaray-A	1
krk	Kerek	1
krl	Karelian	1
krn	Sapo	1
krp	Durop	1
krr	Krung	1
krs	Gbaya (Sudan)	1
krt	Tumari Kanuri	1
kru	Kurukh	1
krv	Kavet	1
krw	Western Krahn	1
krx	Karon	1
kry	Kryts	1
krz	Sota Kanum	1
ksb	Shambala	1
ksc	Southern Kalinga	1
ksd	Kuanua	1
kse	Kuni	1
ksf	Bafia	1
ksg	Kusaghe	1
ksh	Kölsch	1
ksi	Krisa	1
ksj	Uare	1
ksk	Kansa	1
ksl	Kumalu	1
ksm	Kumba	1
ksn	Kasiguranin	1
kso	Kofa	1
ksp	Kaba	1
ksq	Kwaami	1
ksr	Borong	1
kss	Southern Kisi	1
kst	Winyé	1
ksu	Khamyang	1
ksv	Kusu	1
ksw	S'gaw Karen	1
ksx	Kedang	1
ksy	Kharia Thar	1
ksz	Kodaku	1
kta	Katua	1
ktb	Kambaata	1
ktc	Kholok	1
ktd	Kokata	1
kte	Nubri	1
ktf	Kwami	1
ktg	Kalkutung	1
kth	Karanga	1
kti	North Muyu	1
ktj	Plapo Krumen	1
ktk	Kaniet	1
ktl	Koroshi	1
ktm	Kurti	1
ktn	Karitiâna	1
kto	Kuot	1
ktp	Kaduo	1
ktq	Katabaga	1
kts	South Muyu	1
ktt	Ketum	1
ktu	Kituba (Democratic Republic of Congo)	1
ktv	Eastern Katu	1
ktw	Kato	1
ktx	Kaxararí	1
kty	Kango (Bas-Uélé District)	1
ktz	Juǀʼhoan	1
kua	Kuanyama	1
kub	Kutep	1
kuc	Kwinsu	1
kud	'Auhelawa	1
kue	Kuman (Papua New Guinea)	1
kuf	Western Katu	1
kug	Kupa	1
kuh	Kushi	1
kui	Kuikúro-Kalapálo	1
kuj	Kuria	1
kuk	Kepo'	1
kul	Kulere	1
kum	Kumyk	1
kun	Kunama	1
kuo	Kumukio	1
kup	Kunimaipa	1
kuq	Karipuna	1
kur	Kurdish	1
kus	Kusaal	1
kut	Kutenai	1
kuu	Upper Kuskokwim	1
kuv	Kur	1
kuw	Kpagua	1
kux	Kukatja	1
kuy	Kuuku-Ya'u	1
kuz	Kunza	1
kva	Bagvalal	1
kvb	Kubu	1
kvc	Kove	1
kvd	Kui (Indonesia)	1
kve	Kalabakan	1
kvf	Kabalai	1
kvg	Kuni-Boazi	1
kvh	Komodo	1
kvi	Kwang	1
kvj	Psikye	1
kvk	Korean Sign Language	1
kvl	Kayaw	1
kvm	Kendem	1
kvn	Border Kuna	1
kvo	Dobel	1
kvp	Kompane	1
kvq	Geba Karen	1
kvr	Kerinci	1
kvt	Lahta Karen	1
kvu	Yinbaw Karen	1
kvv	Kola	1
kvw	Wersing	1
kvx	Parkari Koli	1
kvy	Yintale Karen	1
kvz	Tsakwambo	1
kwa	Dâw	1
kwb	Kwa	1
kwc	Likwala	1
kwd	Kwaio	1
kwe	Kwerba	1
kwf	Kwara'ae	1
kwg	Sara Kaba Deme	1
kwh	Kowiai	1
kwi	Awa-Cuaiquer	1
kwj	Kwanga	1
kwk	Kwakiutl	1
kwl	Kofyar	1
kwm	Kwambi	1
kwn	Kwangali	1
kwo	Kwomtari	1
kwp	Kodia	1
kwr	Kwer	1
kws	Kwese	1
kwt	Kwesten	1
kwu	Kwakum	1
kwv	Sara Kaba Náà	1
kww	Kwinti	1
kwx	Khirwar	1
kwy	San Salvador Kongo	1
kwz	Kwadi	1
kxa	Kairiru	1
kxb	Krobu	1
kxc	Konso	1
kxd	Brunei	1
kxf	Manumanaw Karen	1
kxh	Karo (Ethiopia)	1
kxi	Keningau Murut	1
kxj	Kulfa	1
kxk	Zayein Karen	1
kxm	Northern Khmer	1
kxn	Kanowit-Tanjong Melanau	1
kxo	Kanoé	1
kxp	Wadiyara Koli	1
kxq	Smärky Kanum	1
kxr	Koro (Papua New Guinea)	1
kxs	Kangjia	1
kxt	Koiwat	1
kxv	Kuvi	1
kxw	Konai	1
kxx	Likuba	1
kxy	Kayong	1
kxz	Kerewo	1
kya	Kwaya	1
kyb	Butbut Kalinga	1
kyc	Kyaka	1
kyd	Karey	1
kye	Krache	1
kyf	Kouya	1
kyg	Keyagana	1
kyh	Karok	1
kyi	Kiput	1
kyj	Karao	1
kyk	Kamayo	1
kyl	Kalapuya	1
kym	Kpatili	1
kyn	Northern Binukidnon	1
kyo	Kelon	1
kyp	Kang	1
kyq	Kenga	1
kyr	Kuruáya	1
kys	Baram Kayan	1
kyt	Kayagar	1
kyu	Western Kayah	1
kyv	Kayort	1
kyw	Kudmali	1
kyx	Rapoisi	1
kyy	Kambaira	1
kyz	Kayabí	1
kza	Western Karaboro	1
kzb	Kaibobo	1
kzc	Bondoukou Kulango	1
kzd	Kadai	1
kze	Kosena	1
kzf	Da'a Kaili	1
kzg	Kikai	1
kzi	Kelabit	1
kzk	Kazukuru	1
kzl	Kayeli	1
kzm	Kais	1
kzn	Kokola	1
kzo	Kaningi	1
kzp	Kaidipang	1
kzq	Kaike	1
kzr	Karang	1
kzs	Sugut Dusun	1
kzu	Kayupulau	1
kzv	Komyandaret	1
kzw	Karirí-Xocó	1
kzx	Kamarian	1
kzy	Kango (Tshopo District)	1
kzz	Kalabra	1
laa	Southern Subanen	1
lab	Linear A	1
lac	Lacandon	1
lad	Ladino	1
lae	Pattani	1
laf	Lafofa	1
lag	Rangi	1
lah	Lahnda	1
lai	Lambya	1
laj	Lango (Uganda)	1
lal	Lalia	1
lam	Lamba	1
lan	Laru	1
lao	Lao	1
lap	Laka (Chad)	1
laq	Qabiao	1
lar	Larteh	1
las	Lama (Togo)	1
lat	Latin	1
lau	Laba	1
lav	Latvian	1
law	Lauje	1
lax	Tiwa	1
lay	Lama Bai	1
laz	Aribwatsa	1
lbb	Label	1
lbc	Lakkia	1
lbe	Lak	1
lbf	Tinani	1
lbg	Laopang	1
lbi	La'bi	1
lbj	Ladakhi	1
lbk	Central Bontok	1
lbl	Libon Bikol	1
lbm	Lodhi	1
lbn	Rmeet	1
lbo	Laven	1
lbq	Wampar	1
lbr	Lohorung	1
lbs	Libyan Sign Language	1
lbt	Lachi	1
lbu	Labu	1
lbv	Lavatbura-Lamusong	1
lbw	Tolaki	1
lbx	Lawangan	1
lby	Lamalama	1
lbz	Lardil	1
lcc	Legenyem	1
lcd	Lola	1
lce	Loncong	1
lcf	Lubu	1
lch	Luchazi	1
lcl	Lisela	1
lcm	Tungag	1
lcp	Western Lawa	1
lcq	Luhu	1
lcs	Lisabata-Nuniali	1
lda	Kla-Dan	1
ldb	Dũya	1
ldd	Luri	1
ldg	Lenyima	1
ldh	Lamja-Dengsa-Tola	1
ldi	Laari	1
ldj	Lemoro	1
ldk	Leelau	1
ldl	Kaan	1
ldm	Landoma	1
ldn	Láadan	1
ldo	Loo	1
ldp	Tso	1
ldq	Lufu	1
lea	Lega-Shabunda	1
leb	Lala-Bisa	1
lec	Leco	1
led	Lendu	1
lee	Lyélé	1
lef	Lelemi	1
leh	Lenje	1
lei	Lemio	1
lej	Lengola	1
lek	Leipon	1
lel	Lele (Democratic Republic of Congo)	1
lem	Nomaande	1
len	Lenca	1
leo	Leti (Cameroon)	1
lep	Lepcha	1
leq	Lembena	1
ler	Lenkau	1
les	Lese	1
let	Lesing-Gelimi	1
leu	Kara (Papua New Guinea)	1
lev	Lamma	1
lew	Ledo Kaili	1
lex	Luang	1
ley	Lemolang	1
lez	Lezghian	1
lfa	Lefa	1
lfn	Lingua Franca Nova	1
lga	Lungga	1
lgb	Laghu	1
lgg	Lugbara	1
lgh	Laghuu	1
lgi	Lengilu	1
lgk	Lingarak	1
lgl	Wala	1
lgm	Lega-Mwenga	1
lgn	T'apo	1
lgo	Lango (South Sudan)	1
lgq	Logba	1
lgr	Lengo	1
lgs	Guinea-Bissau Sign Language	1
lgt	Pahi	1
lgu	Longgu	1
lgz	Ligenza	1
lha	Laha (Viet Nam)	1
lhh	Laha (Indonesia)	1
lhi	Lahu Shi	1
lhl	Lahul Lohar	1
lhm	Lhomi	1
lhn	Lahanan	1
lhp	Lhokpu	1
lhs	Mlahsö	1
lht	Lo-Toga	1
lhu	Lahu	1
lia	West-Central Limba	1
lib	Likum	1
lic	Hlai	1
lid	Nyindrou	1
lie	Likila	1
lif	Limbu	1
lig	Ligbi	1
lih	Lihir	1
lij	Ligurian	1
lik	Lika	1
lil	Lillooet	1
lim	Limburgan	1
lin	Lingala	1
lio	Liki	1
lip	Sekpele	1
liq	Libido	1
lir	Liberian English	1
lis	Lisu	1
lit	Lithuanian	1
liu	Logorik	1
liv	Liv	1
liw	Col	1
lix	Liabuku	1
liy	Banda-Bambari	1
liz	Libinza	1
lja	Golpa	1
lje	Rampi	1
lji	Laiyolo	1
ljl	Li'o	1
ljp	Lampung Api	1
ljw	Yirandali	1
ljx	Yuru	1
lka	Lakalei	1
lkb	Kabras	1
lkc	Kucong	1
lkd	Lakondê	1
lke	Kenyi	1
lkh	Lakha	1
lki	Laki	1
lkj	Remun	1
lkl	Laeko-Libuat	1
lkm	Kalaamaya	1
lkn	Lakon	1
lko	Khayo	1
lkr	Päri	1
lks	Kisa	1
lkt	Lakota	1
lku	Kungkari	1
lky	Lokoya	1
lla	Lala-Roba	1
llb	Lolo	1
llc	Lele (Guinea)	1
lld	Ladin	1
lle	Lele (Papua New Guinea)	1
llf	Hermit	1
llg	Lole	1
llh	Lamu	1
lli	Teke-Laali	1
llj	Ladji Ladji	1
llk	Lelak	1
lll	Lilau	1
llm	Lasalimu	1
lln	Lele (Chad)	1
llp	North Efate	1
llq	Lolak	1
lls	Lithuanian Sign Language	1
llu	Lau	1
llx	Lauan	1
lma	East Limba	1
lmb	Merei	1
lmc	Limilngan	1
lmd	Lumun	1
lme	Pévé	1
lmf	South Lembata	1
lmg	Lamogai	1
lmh	Lambichhong	1
lmi	Lombi	1
lmj	West Lembata	1
lmk	Lamkang	1
lml	Hano	1
lmn	Lambadi	1
lmo	Lombard	1
lmp	Limbum	1
lmq	Lamatuka	1
lmr	Lamalera	1
lmu	Lamenu	1
lmv	Lomaiviti	1
lmw	Lake Miwok	1
lmx	Laimbue	1
lmy	Lamboya	1
lna	Langbashe	1
lnb	Mbalanhu	1
lnd	Lundayeh	1
lng	Langobardic	1
lnh	Lanoh	1
lni	Daantanai'	1
lnj	Leningitij	1
lnl	South Central Banda	1
lnm	Langam	1
lnn	Lorediakarkar	1
lns	Lamnso'	1
lnu	Longuda	1
lnw	Lanima	1
lnz	Lonzo	1
loa	Loloda	1
lob	Lobi	1
loc	Inonhan	1
loe	Saluan	1
lof	Logol	1
log	Logo	1
loh	Laarim	1
loi	Loma (Côte d'Ivoire)	1
loj	Lou	1
lok	Loko	1
lol	Mongo	1
lom	Loma (Liberia)	1
lon	Malawi Lomwe	1
loo	Lombo	1
lop	Lopa	1
loq	Lobala	1
lor	Téén	1
los	Loniu	1
lot	Otuho	1
lou	Louisiana Creole	1
lov	Lopi	1
low	Tampias Lobu	1
lox	Loun	1
loy	Loke	1
loz	Lozi	1
lpa	Lelepa	1
lpe	Lepki	1
lpn	Long Phuri Naga	1
lpo	Lipo	1
lpx	Lopit	1
lqr	Logir	1
lra	Rara Bakati'	1
lrc	Northern Luri	1
lre	Laurentian	1
lrg	Laragia	1
lri	Marachi	1
lrk	Loarki	1
lrl	Lari	1
lrm	Marama	1
lrn	Lorang	1
lro	Laro	1
lrr	Southern Yamphu	1
lrt	Larantuka Malay	1
lrv	Larevat	1
lrz	Lemerig	1
lsa	Lasgerdi	1
lsb	Burundian Sign Language	1
lsc	Albarradas Sign Language	1
lsd	Lishana Deni	1
lse	Lusengo	1
lsh	Lish	1
lsi	Lashi	1
lsl	Latvian Sign Language	1
lsm	Saamia	1
lsn	Tibetan Sign Language	1
lso	Laos Sign Language	1
lsp	Panamanian Sign Language	1
lsr	Aruop	1
lss	Lasi	1
lst	Trinidad and Tobago Sign Language	1
lsv	Sivia Sign Language	1
lsw	Seychelles Sign Language	1
lsy	Mauritian Sign Language	1
ltc	Late Middle Chinese	1
ltg	Latgalian	1
lth	Thur	1
lti	Leti (Indonesia)	1
ltn	Latundê	1
lto	Tsotso	1
lts	Tachoni	1
ltu	Latu	1
ltz	Luxembourgish	1
lua	Luba-Lulua	1
lub	Luba-Katanga	1
luc	Aringa	1
lud	Ludian	1
lue	Luvale	1
luf	Laua	1
lug	Ganda	1
lui	Luiseno	1
luj	Luna	1
luk	Lunanakha	1
lul	Olu'bo	1
lum	Luimbi	1
lun	Lunda	1
luo	Luo (Kenya and Tanzania)	1
lup	Lumbu	1
luq	Lucumi	1
lur	Laura	1
lus	Lushai	1
lut	Lushootseed	1
luu	Lumba-Yakkha	1
luv	Luwati	1
luw	Luo (Cameroon)	1
luy	Luyia	1
luz	Southern Luri	1
lva	Maku'a	1
lvi	Lavi	1
lvk	Lavukaleve	1
lvl	Lwel	1
lvs	Standard Latvian	1
lvu	Levuka	1
lwa	Lwalu	1
lwe	Lewo Eleng	1
lwg	Wanga	1
lwh	White Lachi	1
lwl	Eastern Lawa	1
lwm	Laomian	1
lwo	Luwo	1
lws	Malawian Sign Language	1
lwt	Lewotobi	1
lwu	Lawu	1
lww	Lewo	1
lxm	Lakurumau	1
lya	Layakha	1
lyg	Lyngngam	1
lyn	Luyana	1
lzh	Literary Chinese	1
lzl	Litzlitz	1
lzn	Leinong Naga	1
lzz	Laz	1
maa	San Jerónimo Tecóatl Mazatec	1
mab	Yutanduchi Mixtec	1
mad	Madurese	1
mae	Bo-Rukul	1
maf	Mafa	1
mag	Magahi	1
mah	Marshallese	1
mai	Maithili	1
maj	Jalapa De Díaz Mazatec	1
mak	Makasar	1
mal	Malayalam	1
mam	Mam	1
man	Mandingo	1
maq	Chiquihuitlán Mazatec	1
mar	Marathi	1
mas	Masai	1
mat	San Francisco Matlatzinca	1
mau	Huautla Mazatec	1
mav	Sateré-Mawé	1
maw	Mampruli	1
max	North Moluccan Malay	1
maz	Central Mazahua	1
mba	Higaonon	1
mbb	Western Bukidnon Manobo	1
mbc	Macushi	1
mbd	Dibabawon Manobo	1
mbe	Molale	1
mbf	Baba Malay	1
mbh	Mangseng	1
mbi	Ilianen Manobo	1
mbj	Nadëb	1
mbk	Malol	1
mbl	Maxakalí	1
mbm	Ombamba	1
mbn	Macaguán	1
mbo	Mbo (Cameroon)	1
mbp	Malayo	1
mbq	Maisin	1
mbr	Nukak Makú	1
mbs	Sarangani Manobo	1
mbt	Matigsalug Manobo	1
mbu	Mbula-Bwazza	1
mbv	Mbulungish	1
mbw	Maring	1
mbx	Mari (East Sepik Province)	1
mby	Memoni	1
mbz	Amoltepec Mixtec	1
mca	Maca	1
mcb	Machiguenga	1
mcc	Bitur	1
mcd	Sharanahua	1
mce	Itundujia Mixtec	1
mcf	Matsés	1
mcg	Mapoyo	1
mch	Maquiritari	1
mci	Mese	1
mcj	Mvanip	1
mck	Mbunda	1
mcl	Macaguaje	1
mcm	Malaccan Creole Portuguese	1
mcn	Masana	1
mco	Coatlán Mixe	1
mcp	Makaa	1
mcq	Ese	1
mcr	Menya	1
mcs	Mambai	1
mct	Mengisa	1
mcu	Cameroon Mambila	1
mcv	Minanibai	1
mcw	Mawa (Chad)	1
mcx	Mpiemo	1
mcy	South Watut	1
mcz	Mawan	1
mda	Mada (Nigeria)	1
mdb	Morigi	1
mdc	Male (Papua New Guinea)	1
mdd	Mbum	1
mde	Maba (Chad)	1
mdf	Moksha	1
mdg	Massalat	1
mdh	Maguindanaon	1
mdi	Mamvu	1
mdj	Mangbetu	1
mdk	Mangbutu	1
mdl	Maltese Sign Language	1
mdm	Mayogo	1
mdn	Mbati	1
mdp	Mbala	1
mdq	Mbole	1
mdr	Mandar	1
mds	Maria (Papua New Guinea)	1
mdt	Mbere	1
mdu	Mboko	1
mdv	Santa Lucía Monteverde Mixtec	1
mdw	Mbosi	1
mdx	Dizin	1
mdy	Male (Ethiopia)	1
mdz	Suruí Do Pará	1
mea	Menka	1
meb	Ikobi	1
mec	Marra	1
med	Melpa	1
mee	Mengen	1
mef	Megam	1
meh	Southwestern Tlaxiaco Mixtec	1
mei	Midob	1
mej	Meyah	1
mek	Mekeo	1
mel	Central Melanau	1
mem	Mangala	1
men	Mende (Sierra Leone)	1
meo	Kedah Malay	1
mep	Miriwoong	1
meq	Merey	1
mer	Meru	1
mes	Masmaje	1
met	Mato	1
meu	Motu	1
mev	Mano	1
mew	Maaka	1
mey	Hassaniyya	1
mez	Menominee	1
mfa	Pattani Malay	1
mfb	Bangka	1
mfc	Mba	1
mfd	Mendankwe-Nkwen	1
mfe	Morisyen	1
mff	Naki	1
mfg	Mogofin	1
mfh	Matal	1
mfi	Wandala	1
mfj	Mefele	1
mfk	North Mofu	1
mfl	Putai	1
mfm	Marghi South	1
mfn	Cross River Mbembe	1
mfo	Mbe	1
mfp	Makassar Malay	1
mfq	Moba	1
mfr	Marrithiyel	1
mfs	Mexican Sign Language	1
mft	Mokerang	1
mfu	Mbwela	1
mfv	Mandjak	1
mfw	Mulaha	1
mfx	Melo	1
mfy	Mayo	1
mfz	Mabaan	1
mga	Middle Irish (900-1200)	1
mgb	Mararit	1
mgc	Morokodo	1
mgd	Moru	1
mge	Mango	1
mgf	Maklew	1
mgg	Mpumpong	1
mgh	Makhuwa-Meetto	1
mgi	Lijili	1
mgj	Abureni	1
mgk	Mawes	1
mgl	Maleu-Kilenge	1
mgm	Mambae	1
mgn	Mbangi	1
mgo	Meta'	1
mgp	Eastern Magar	1
mgq	Malila	1
mgr	Mambwe-Lungu	1
mgs	Manda (Tanzania)	1
mgt	Mongol	1
mgu	Mailu	1
mgv	Matengo	1
mgw	Matumbi	1
mgy	Mbunga	1
mgz	Mbugwe	1
mha	Manda (India)	1
mhb	Mahongwe	1
mhc	Mocho	1
mhd	Mbugu	1
mhe	Besisi	1
mhf	Mamaa	1
mhg	Margu	1
mhi	Ma'di	1
mhj	Mogholi	1
mhk	Mungaka	1
mhl	Mauwake	1
mhm	Makhuwa-Moniga	1
mhn	Mócheno	1
mho	Mashi (Zambia)	1
mhp	Balinese Malay	1
mhq	Mandan	1
mhr	Eastern Mari	1
mhs	Buru (Indonesia)	1
mht	Mandahuaca	1
mhu	Digaro-Mishmi	1
mhw	Mbukushu	1
mhx	Maru	1
mhy	Ma'anyan	1
mhz	Mor (Mor Islands)	1
mia	Miami	1
mib	Atatláhuca Mixtec	1
mic	Mi'kmaq	1
mid	Mandaic	1
mie	Ocotepec Mixtec	1
mif	Mofu-Gudur	1
mig	San Miguel El Grande Mixtec	1
mih	Chayuco Mixtec	1
mii	Chigmecatitlán Mixtec	1
mij	Abar	1
mik	Mikasuki	1
mil	Peñoles Mixtec	1
mim	Alacatlatzala Mixtec	1
min	Minangkabau	1
mio	Pinotepa Nacional Mixtec	1
mip	Apasco-Apoala Mixtec	1
miq	Mískito	1
mir	Isthmus Mixe	1
mis	Uncoded languages	1
mit	Southern Puebla Mixtec	1
miu	Cacaloxtepec Mixtec	1
miw	Akoye	1
mix	Mixtepec Mixtec	1
miy	Ayutla Mixtec	1
miz	Coatzospan Mixtec	1
mjb	Makalero	1
mjc	San Juan Colorado Mixtec	1
mjd	Northwest Maidu	1
mje	Muskum	1
mjg	Tu	1
mjh	Mwera (Nyasa)	1
mji	Kim Mun	1
mjj	Mawak	1
mjk	Matukar	1
mjl	Mandeali	1
mjm	Medebur	1
mjn	Ma (Papua New Guinea)	1
mjo	Malankuravan	1
mjp	Malapandaram	1
mjq	Malaryan	1
mjr	Malavedan	1
mjs	Miship	1
mjt	Sauria Paharia	1
mju	Manna-Dora	1
mjv	Mannan	1
mjw	Karbi	1
mjx	Mahali	1
mjy	Mahican	1
mjz	Majhi	1
mka	Mbre	1
mkb	Mal Paharia	1
mkc	Siliput	1
mkd	Macedonian	1
mke	Mawchi	1
mkf	Miya	1
mkg	Mak (China)	1
mki	Dhatki	1
mkj	Mokilese	1
mkk	Byep	1
mkl	Mokole	1
mkm	Moklen	1
mkn	Kupang Malay	1
mko	Mingang Doso	1
mkp	Moikodi	1
mkq	Bay Miwok	1
mkr	Malas	1
mks	Silacayoapan Mixtec	1
mkt	Vamale	1
mku	Konyanka Maninka	1
mkv	Mafea	1
mkw	Kituba (Congo)	1
mkx	Kinamiging Manobo	1
mky	East Makian	1
mkz	Makasae	1
mla	Malo	1
mlb	Mbule	1
mlc	Cao Lan	1
mle	Manambu	1
mlf	Mal	1
mlg	Malagasy	1
mlh	Mape	1
mli	Malimpung	1
mlj	Miltu	1
mlk	Ilwana	1
mll	Malua Bay	1
mlm	Mulam	1
mln	Malango	1
mlo	Mlomp	1
mlp	Bargam	1
mlq	Western Maninkakan	1
mlr	Vame	1
mls	Masalit	1
mlt	Maltese	1
mlu	To'abaita	1
mlv	Motlav	1
mlw	Moloko	1
mlx	Malfaxal	1
mlz	Malaynon	1
mma	Mama	1
mmb	Momina	1
mmc	Michoacán Mazahua	1
mmd	Maonan	1
mme	Mae	1
mmf	Mundat	1
mmg	North Ambrym	1
mmh	Mehináku	1
mmi	Musar	1
mmj	Majhwar	1
mmk	Mukha-Dora	1
mml	Man Met	1
mmm	Maii	1
mmn	Mamanwa	1
mmo	Mangga Buang	1
mmp	Siawi	1
mmq	Musak	1
mmr	Western Xiangxi Miao	1
mmt	Malalamai	1
mmu	Mmaala	1
mmv	Miriti	1
mmw	Emae	1
mmx	Madak	1
mmy	Migaama	1
mmz	Mabaale	1
mna	Mbula	1
mnb	Muna	1
mnc	Manchu	1
mnd	Mondé	1
mne	Naba	1
mnf	Mundani	1
mng	Eastern Mnong	1
mnh	Mono (Democratic Republic of Congo)	1
mni	Manipuri	1
mnj	Munji	1
mnk	Mandinka	1
mnl	Tiale	1
mnm	Mapena	1
mnn	Southern Mnong	1
mnp	Min Bei Chinese	1
mnq	Minriq	1
mnr	Mono (USA)	1
mns	Mansi	1
mnu	Mer	1
mnv	Rennell-Bellona	1
mnw	Mon	1
mnx	Manikion	1
mny	Manyawa	1
mnz	Moni	1
moa	Mwan	1
moc	Mocoví	1
mod	Mobilian	1
moe	Innu	1
mog	Mongondow	1
moh	Mohawk	1
moi	Mboi	1
moj	Monzombo	1
mok	Morori	1
mom	Mangue	1
mon	Mongolian	1
moo	Monom	1
mop	Mopán Maya	1
moq	Mor (Bomberai Peninsula)	1
mor	Moro	1
mos	Mossi	1
mot	Barí	1
mou	Mogum	1
mov	Mohave	1
mow	Moi (Congo)	1
mox	Molima	1
moy	Shekkacho	1
moz	Mukulu	1
mpa	Mpoto	1
mpb	Malak Malak	1
mpc	Mangarrayi	1
mpd	Machinere	1
mpe	Majang	1
mpg	Marba	1
mph	Maung	1
mpi	Mpade	1
mpj	Martu Wangka	1
mpk	Mbara (Chad)	1
mpl	Middle Watut	1
mpm	Yosondúa Mixtec	1
mpn	Mindiri	1
mpo	Miu	1
mpp	Migabac	1
mpq	Matís	1
mpr	Vangunu	1
mps	Dadibi	1
mpt	Mian	1
mpu	Makuráp	1
mpv	Mungkip	1
mpw	Mapidian	1
mpx	Misima-Panaeati	1
mpy	Mapia	1
mpz	Mpi	1
mqa	Maba (Indonesia)	1
mqb	Mbuko	1
mqc	Mangole	1
mqe	Matepi	1
mqf	Momuna	1
mqg	Kota Bangun Kutai Malay	1
mqh	Tlazoyaltepec Mixtec	1
mqi	Mariri	1
mqj	Mamasa	1
mqk	Rajah Kabunsuwan Manobo	1
mql	Mbelime	1
mqm	South Marquesan	1
mqn	Moronene	1
mqo	Modole	1
mqp	Manipa	1
mqq	Minokok	1
mqr	Mander	1
mqs	West Makian	1
mqt	Mok	1
mqu	Mandari	1
mqv	Mosimo	1
mqw	Murupi	1
mqx	Mamuju	1
mqy	Manggarai	1
mqz	Pano	1
mra	Mlabri	1
mrb	Marino	1
mrc	Maricopa	1
mrd	Western Magar	1
mre	Martha's Vineyard Sign Language	1
mrf	Elseng	1
mrg	Mising	1
mrh	Mara Chin	1
mri	Maori	1
mrj	Western Mari	1
mrk	Hmwaveke	1
mrl	Mortlockese	1
mrm	Merlav	1
mrn	Cheke Holo	1
mro	Mru	1
mrp	Morouas	1
mrq	North Marquesan	1
mrr	Maria (India)	1
mrs	Maragus	1
mrt	Marghi Central	1
mru	Mono (Cameroon)	1
mrv	Mangareva	1
mrw	Maranao	1
mrx	Maremgi	1
mry	Mandaya	1
mrz	Marind	1
msa	Malay (macrolanguage)	1
msb	Masbatenyo	1
msc	Sankaran Maninka	1
msd	Yucatec Maya Sign Language	1
mse	Musey	1
msf	Mekwei	1
msg	Moraid	1
msh	Masikoro Malagasy	1
msi	Sabah Malay	1
msj	Ma (Democratic Republic of Congo)	1
msk	Mansaka	1
msl	Molof	1
msm	Agusan Manobo	1
msn	Vurës	1
mso	Mombum	1
msp	Maritsauá	1
msq	Caac	1
msr	Mongolian Sign Language	1
mss	West Masela	1
msu	Musom	1
msv	Maslam	1
msw	Mansoanka	1
msx	Moresada	1
msy	Aruamu	1
msz	Momare	1
mta	Cotabato Manobo	1
mtb	Anyin Morofo	1
mtc	Munit	1
mtd	Mualang	1
mte	Mono (Solomon Islands)	1
mtf	Murik (Papua New Guinea)	1
mtg	Una	1
mth	Munggui	1
mti	Maiwa (Papua New Guinea)	1
mtj	Moskona	1
mtk	Mbe'	1
mtl	Montol	1
mtm	Mator	1
mtn	Matagalpa	1
mto	Totontepec Mixe	1
mtp	Wichí Lhamtés Nocten	1
mtq	Muong	1
mtr	Mewari	1
mts	Yora	1
mtt	Mota	1
mtu	Tututepec Mixtec	1
mtv	Asaro'o	1
mtw	Southern Binukidnon	1
mtx	Tidaá Mixtec	1
mty	Nabi	1
mua	Mundang	1
mub	Mubi	1
muc	Ajumbu	1
mud	Mednyj Aleut	1
mue	Media Lengua	1
mug	Musgu	1
muh	Mündü	1
mui	Musi	1
muj	Mabire	1
muk	Mugom	1
mul	Multiple languages	1
mum	Maiwala	1
muo	Nyong	1
mup	Malvi	1
muq	Eastern Xiangxi Miao	1
mur	Murle	1
mus	Creek	1
mut	Western Muria	1
muu	Yaaku	1
muv	Muthuvan	1
mux	Bo-Ung	1
muy	Muyang	1
muz	Mursi	1
mva	Manam	1
mvb	Mattole	1
mvd	Mamboru	1
mve	Marwari (Pakistan)	1
mvf	Peripheral Mongolian	1
mvg	Yucuañe Mixtec	1
mvh	Mulgi	1
mvi	Miyako	1
mvk	Mekmek	1
mvl	Mbara (Australia)	1
mvn	Minaveha	1
mvo	Marovo	1
mvp	Duri	1
mvq	Moere	1
mvr	Marau	1
mvs	Massep	1
mvt	Mpotovoro	1
mvu	Marfa	1
mvv	Tagal Murut	1
mvw	Machinga	1
mvx	Meoswar	1
mvy	Indus Kohistani	1
mvz	Mesqan	1
mwa	Mwatebu	1
mwb	Juwal	1
mwc	Are	1
mwe	Mwera (Chimwera)	1
mwf	Murrinh-Patha	1
mwg	Aiklep	1
mwh	Mouk-Aria	1
mwi	Labo	1
mwk	Kita Maninkakan	1
mwl	Mirandese	1
mwm	Sar	1
mwn	Nyamwanga	1
mwo	Central Maewo	1
mwp	Kala Lagaw Ya	1
mwq	Mün Chin	1
mwr	Marwari	1
mws	Mwimbi-Muthambi	1
mwt	Moken	1
mwu	Mittu	1
mwv	Mentawai	1
mww	Hmong Daw	1
mwz	Moingi	1
mxa	Northwest Oaxaca Mixtec	1
mxb	Tezoatlán Mixtec	1
mxc	Manyika	1
mxd	Modang	1
mxe	Mele-Fila	1
mxf	Malgbe	1
mxg	Mbangala	1
mxh	Mvuba	1
mxi	Mozarabic	1
mxj	Miju-Mishmi	1
mxk	Monumbo	1
mxl	Maxi Gbe	1
mxm	Meramera	1
mxn	Moi (Indonesia)	1
mxo	Mbowe	1
mxp	Tlahuitoltepec Mixe	1
mxq	Juquila Mixe	1
mxr	Murik (Malaysia)	1
mxs	Huitepec Mixtec	1
mxt	Jamiltepec Mixtec	1
mxu	Mada (Cameroon)	1
mxv	Metlatónoc Mixtec	1
mxw	Namo	1
mxx	Mahou	1
mxy	Southeastern Nochixtlán Mixtec	1
mxz	Central Masela	1
mya	Burmese	1
myb	Mbay	1
myc	Mayeka	1
mye	Myene	1
myf	Bambassi	1
myg	Manta	1
myh	Makah	1
myj	Mangayat	1
myk	Mamara Senoufo	1
myl	Moma	1
mym	Me'en	1
myo	Anfillo	1
myp	Pirahã	1
myr	Muniche	1
mys	Mesmes	1
myu	Mundurukú	1
myv	Erzya	1
myw	Muyuw	1
myx	Masaaba	1
myy	Macuna	1
myz	Classical Mandaic	1
mza	Santa María Zacatepec Mixtec	1
mzb	Tumzabt	1
mzc	Madagascar Sign Language	1
mzd	Malimba	1
mze	Morawa	1
mzg	Monastic Sign Language	1
mzh	Wichí Lhamtés Güisnay	1
mzi	Ixcatlán Mazatec	1
mzj	Manya	1
mzk	Nigeria Mambila	1
mzl	Mazatlán Mixe	1
mzm	Mumuye	1
mzn	Mazanderani	1
mzo	Matipuhy	1
mzp	Movima	1
mzq	Mori Atas	1
mzr	Marúbo	1
mzs	Macanese	1
mzt	Mintil	1
mzu	Inapang	1
mzv	Manza	1
mzw	Deg	1
mzx	Mawayana	1
mzy	Mozambican Sign Language	1
mzz	Maiadomu	1
naa	Namla	1
nab	Southern Nambikuára	1
nac	Narak	1
nae	Naka'ela	1
naf	Nabak	1
nag	Naga Pidgin	1
naj	Nalu	1
nak	Nakanai	1
nal	Nalik	1
nam	Ngan'gityemerri	1
nan	Min Nan Chinese	1
nao	Naaba	1
nap	Neapolitan	1
naq	Khoekhoe	1
nar	Iguta	1
nas	Naasioi	1
nat	Ca̱hungwa̱rya̱	1
nau	Nauru	1
nav	Navajo	1
naw	Nawuri	1
nax	Nakwi	1
nay	Ngarrindjeri	1
naz	Coatepec Nahuatl	1
nba	Nyemba	1
nbb	Ndoe	1
nbc	Chang Naga	1
nbd	Ngbinda	1
nbe	Konyak Naga	1
nbg	Nagarchal	1
nbh	Ngamo	1
nbi	Mao Naga	1
nbj	Ngarinyman	1
nbk	Nake	1
nbl	South Ndebele	1
nbm	Ngbaka Ma'bo	1
nbn	Kuri	1
nbo	Nkukoli	1
nbp	Nnam	1
nbq	Nggem	1
nbr	Numana	1
nbs	Namibian Sign Language	1
nbt	Na	1
nbu	Rongmei Naga	1
nbv	Ngamambo	1
nbw	Southern Ngbandi	1
nby	Ningera	1
nca	Iyo	1
ncb	Central Nicobarese	1
ncc	Ponam	1
ncd	Nachering	1
nce	Yale	1
ncf	Notsi	1
ncg	Nisga'a	1
nch	Central Huasteca Nahuatl	1
nci	Classical Nahuatl	1
ncj	Northern Puebla Nahuatl	1
nck	Na-kara	1
ncl	Michoacán Nahuatl	1
ncm	Nambo	1
ncn	Nauna	1
nco	Sibe	1
ncq	Northern Katang	1
ncr	Ncane	1
ncs	Nicaraguan Sign Language	1
nct	Chothe Naga	1
ncu	Chumburung	1
ncx	Central Puebla Nahuatl	1
ncz	Natchez	1
nda	Ndasa	1
ndb	Kenswei Nsei	1
ndc	Ndau	1
ndd	Nde-Nsele-Nta	1
nde	North Ndebele	1
ndf	Nadruvian	1
ndg	Ndengereko	1
ndh	Ndali	1
ndi	Samba Leko	1
ndj	Ndamba	1
ndk	Ndaka	1
ndl	Ndolo	1
ndm	Ndam	1
ndn	Ngundi	1
ndo	Ndonga	1
ndp	Ndo	1
ndq	Ndombe	1
ndr	Ndoola	1
nds	Low German	1
ndt	Ndunga	1
ndu	Dugun	1
ndv	Ndut	1
ndw	Ndobo	1
ndx	Nduga	1
ndy	Lutos	1
ndz	Ndogo	1
nea	Eastern Ngad'a	1
neb	Toura (Côte d'Ivoire)	1
nec	Nedebang	1
ned	Nde-Gbite	1
nee	Nêlêmwa-Nixumwak	1
nef	Nefamese	1
neg	Negidal	1
neh	Nyenkha	1
nei	Neo-Hittite	1
nej	Neko	1
nek	Neku	1
nem	Nemi	1
nen	Nengone	1
neo	Ná-Meo	1
nep	Nepali (macrolanguage)	1
neq	North Central Mixe	1
ner	Yahadian	1
nes	Bhoti Kinnauri	1
net	Nete	1
neu	Neo	1
nev	Nyaheun	1
new	Newari	1
nex	Neme	1
ney	Neyo	1
nez	Nez Perce	1
nfa	Dhao	1
nfd	Ahwai	1
nfl	Ayiwo	1
nfr	Nafaanra	1
nfu	Mfumte	1
nga	Ngbaka	1
ngb	Northern Ngbandi	1
ngc	Ngombe (Democratic Republic of Congo)	1
ngd	Ngando (Central African Republic)	1
nge	Ngemba	1
ngg	Ngbaka Manza	1
ngh	Nǁng	1
ngi	Ngizim	1
ngj	Ngie	1
ngk	Dalabon	1
ngl	Lomwe	1
ngm	Ngatik Men's Creole	1
ngn	Ngwo	1
ngp	Ngulu	1
ngq	Ngurimi	1
ngr	Engdewu	1
ngs	Gvoko	1
ngt	Kriang	1
ngu	Guerrero Nahuatl	1
ngv	Nagumi	1
ngw	Ngwaba	1
ngx	Nggwahyi	1
ngy	Tibea	1
ngz	Ngungwel	1
nha	Nhanda	1
nhb	Beng	1
nhc	Tabasco Nahuatl	1
nhd	Chiripá	1
nhe	Eastern Huasteca Nahuatl	1
nhf	Nhuwala	1
nhg	Tetelcingo Nahuatl	1
nhh	Nahari	1
nhi	Zacatlán-Ahuacatlán-Tepetzintla Nahuatl	1
nhk	Isthmus-Cosoleacaque Nahuatl	1
nhm	Morelos Nahuatl	1
nhn	Central Nahuatl	1
nho	Takuu	1
nhp	Isthmus-Pajapan Nahuatl	1
nhq	Huaxcaleca Nahuatl	1
nhr	Naro	1
nht	Ometepec Nahuatl	1
nhu	Noone	1
nhv	Temascaltepec Nahuatl	1
nhw	Western Huasteca Nahuatl	1
nhx	Isthmus-Mecayapan Nahuatl	1
nhy	Northern Oaxaca Nahuatl	1
nhz	Santa María La Alta Nahuatl	1
nia	Nias	1
nib	Nakame	1
nid	Ngandi	1
nie	Niellim	1
nif	Nek	1
nig	Ngalakgan	1
nih	Nyiha (Tanzania)	1
nii	Nii	1
nij	Ngaju	1
nik	Southern Nicobarese	1
nil	Nila	1
nim	Nilamba	1
nin	Ninzo	1
nio	Nganasan	1
niq	Nandi	1
nir	Nimboran	1
nis	Nimi	1
nit	Southeastern Kolami	1
niu	Niuean	1
niv	Gilyak	1
niw	Nimo	1
nix	Hema	1
niy	Ngiti	1
niz	Ningil	1
nja	Nzanyi	1
njb	Nocte Naga	1
njd	Ndonde Hamba	1
njh	Lotha Naga	1
nji	Gudanji	1
njj	Njen	1
njl	Njalgulgule	1
njm	Angami Naga	1
njn	Liangmai Naga	1
njo	Ao Naga	1
njr	Njerep	1
njs	Nisa	1
njt	Ndyuka-Trio Pidgin	1
nju	Ngadjunmaya	1
njx	Kunyi	1
njy	Njyem	1
njz	Nyishi	1
nka	Nkoya	1
nkb	Khoibu Naga	1
nkc	Nkongho	1
nkd	Koireng	1
nke	Duke	1
nkf	Inpui Naga	1
nkg	Nekgini	1
nkh	Khezha Naga	1
nki	Thangal Naga	1
nkj	Nakai	1
nkk	Nokuku	1
nkm	Namat	1
nkn	Nkangala	1
nko	Nkonya	1
nkp	Niuatoputapu	1
nkq	Nkami	1
nkr	Nukuoro	1
nks	North Asmat	1
nkt	Nyika (Tanzania)	1
nku	Bouna Kulango	1
nkv	Nyika (Malawi and Zambia)	1
nkw	Nkutu	1
nkx	Nkoroo	1
nkz	Nkari	1
nla	Ngombale	1
nlc	Nalca	1
nld	Dutch	1
nle	East Nyala	1
nlg	Gela	1
nli	Grangali	1
nlj	Nyali	1
nlk	Ninia Yali	1
nll	Nihali	1
nlm	Mankiyali	1
nlo	Ngul	1
nlq	Lao Naga	1
nlu	Nchumbulu	1
nlv	Orizaba Nahuatl	1
nlw	Walangama	1
nlx	Nahali	1
nly	Nyamal	1
nlz	Nalögo	1
nma	Maram Naga	1
nmb	Big Nambas	1
nmc	Ngam	1
nmd	Ndumu	1
nme	Mzieme Naga	1
nmf	Tangkhul Naga (India)	1
nmg	Kwasio	1
nmh	Monsang Naga	1
nmi	Nyam	1
nmj	Ngombe (Central African Republic)	1
nmk	Namakura	1
nml	Ndemli	1
nmm	Manangba	1
nmn	ǃXóõ	1
nmo	Moyon Naga	1
nmp	Nimanbur	1
nmq	Nambya	1
nmr	Nimbari	1
nms	Letemboi	1
nmt	Namonuito	1
nmu	Northeast Maidu	1
nmv	Ngamini	1
nmw	Nimoa	1
nmx	Nama (Papua New Guinea)	1
nmy	Namuyi	1
nmz	Nawdm	1
nna	Nyangumarta	1
nnb	Nande	1
nnc	Nancere	1
nnd	West Ambae	1
nne	Ngandyera	1
nnf	Ngaing	1
nng	Maring Naga	1
nnh	Ngiemboon	1
nni	North Nuaulu	1
nnj	Nyangatom	1
nnk	Nankina	1
nnl	Northern Rengma Naga	1
nnm	Namia	1
nnn	Ngete	1
nno	Norwegian Nynorsk	1
nnp	Wancho Naga	1
nnq	Ngindo	1
nnr	Narungga	1
nnt	Nanticoke	1
nnu	Dwang	1
nnv	Nugunu (Australia)	1
nnw	Southern Nuni	1
nny	Nyangga	1
nnz	Nda'nda'	1
noa	Woun Meu	1
nob	Norwegian Bokmål	1
noc	Nuk	1
nod	Northern Thai	1
noe	Nimadi	1
nof	Nomane	1
nog	Nogai	1
noh	Nomu	1
noi	Noiri	1
noj	Nonuya	1
nok	Nooksack	1
nol	Nomlaki	1
non	Old Norse	1
nop	Numanggang	1
noq	Ngongo	1
nor	Norwegian	1
nos	Eastern Nisu	1
not	Nomatsiguenga	1
nou	Ewage-Notu	1
nov	Novial	1
now	Nyambo	1
noy	Noy	1
noz	Nayi	1
npa	Nar Phu	1
npb	Nupbikha	1
npg	Ponyo-Gongwang Naga	1
nph	Phom Naga	1
npi	Nepali (individual language)	1
npl	Southeastern Puebla Nahuatl	1
npn	Mondropolon	1
npo	Pochuri Naga	1
nps	Nipsan	1
npu	Puimei Naga	1
npx	Noipx	1
npy	Napu	1
nqg	Southern Nago	1
nqk	Kura Ede Nago	1
nql	Ngendelengo	1
nqm	Ndom	1
nqn	Nen	1
nqo	N'Ko	1
nqq	Kyan-Karyaw Naga	1
nqt	Nteng	1
nqy	Akyaung Ari Naga	1
nra	Ngom	1
nrb	Nara	1
nrc	Noric	1
nre	Southern Rengma Naga	1
nrf	Jèrriais	1
nrg	Narango	1
nri	Chokri Naga	1
nrk	Ngarla	1
nrl	Ngarluma	1
nrm	Narom	1
nrn	Norn	1
nrp	North Picene	1
nrr	Norra	1
nrt	Northern Kalapuya	1
nru	Narua	1
nrx	Ngurmbur	1
nrz	Lala	1
nsa	Sangtam Naga	1
nsb	Lower Nossob	1
nsc	Nshi	1
nsd	Southern Nisu	1
nse	Nsenga	1
nsf	Northwestern Nisu	1
nsg	Ngasa	1
nsh	Ngoshie	1
nsi	Nigerian Sign Language	1
nsk	Naskapi	1
nsl	Norwegian Sign Language	1
nsm	Sumi Naga	1
nsn	Nehan	1
nso	Pedi	1
nsp	Nepalese Sign Language	1
nsq	Northern Sierra Miwok	1
nsr	Maritime Sign Language	1
nss	Nali	1
nst	Tase Naga	1
nsu	Sierra Negra Nahuatl	1
nsv	Southwestern Nisu	1
nsw	Navut	1
nsx	Nsongo	1
nsy	Nasal	1
nsz	Nisenan	1
ntd	Northern Tidung	1
nte	Nathembo	1
ntg	Ngantangarra	1
nti	Natioro	1
ntj	Ngaanyatjarra	1
ntk	Ikoma-Nata-Isenye	1
ntm	Nateni	1
nto	Ntomba	1
ntp	Northern Tepehuan	1
ntr	Delo	1
ntu	Natügu	1
ntw	Nottoway	1
ntx	Tangkhul Naga (Myanmar)	1
nty	Mantsi	1
ntz	Natanzi	1
nua	Yuanga	1
nuc	Nukuini	1
nud	Ngala	1
nue	Ngundu	1
nuf	Nusu	1
nug	Nungali	1
nuh	Ndunda	1
nui	Ngumbi	1
nuj	Nyole	1
nuk	Nuu-chah-nulth	1
nul	Nusa Laut	1
num	Niuafo'ou	1
nun	Anong	1
nuo	Nguôn	1
nup	Nupe-Nupe-Tako	1
nuq	Nukumanu	1
nur	Nukuria	1
nus	Nuer	1
nut	Nung (Viet Nam)	1
nuu	Ngbundu	1
nuv	Northern Nuni	1
nuw	Nguluwan	1
nux	Mehek	1
nuy	Nunggubuyu	1
nuz	Tlamacazapa Nahuatl	1
nvh	Nasarian	1
nvm	Namiae	1
nvo	Nyokon	1
nwa	Nawathinehena	1
nwb	Nyabwa	1
nwc	Classical Newari	1
nwe	Ngwe	1
nwg	Ngayawung	1
nwi	Southwest Tanna	1
nwm	Nyamusa-Molo	1
nwo	Nauo	1
nwr	Nawaru	1
nww	Ndwewe	1
nwx	Middle Newar	1
nwy	Nottoway-Meherrin	1
nxa	Nauete	1
nxd	Ngando (Democratic Republic of Congo)	1
nxe	Nage	1
nxg	Ngad'a	1
nxi	Nindi	1
nxk	Koki Naga	1
nxl	South Nuaulu	1
nxm	Numidian	1
nxn	Ngawun	1
nxo	Ndambomo	1
nxq	Naxi	1
nxr	Ninggerum	1
nxx	Nafri	1
nya	Nyanja	1
nyb	Nyangbo	1
nyc	Nyanga-li	1
nyd	Nyore	1
nye	Nyengo	1
nyf	Giryama	1
nyg	Nyindu	1
nyh	Nyikina	1
nyi	Ama (Sudan)	1
nyj	Nyanga	1
nyk	Nyaneka	1
nyl	Nyeu	1
nym	Nyamwezi	1
nyn	Nyankole	1
nyo	Nyoro	1
nyp	Nyang'i	1
nyq	Nayini	1
nyr	Nyiha (Malawi)	1
nys	Nyungar	1
nyt	Nyawaygi	1
nyu	Nyungwe	1
nyv	Nyulnyul	1
nyw	Nyaw	1
nyx	Nganyaywana	1
nyy	Nyakyusa-Ngonde	1
nza	Tigon Mbembe	1
nzb	Njebi	1
nzd	Nzadi	1
nzi	Nzima	1
nzk	Nzakara	1
nzm	Zeme Naga	1
nzr	Dir-Nyamzak-Mbarimi	1
nzs	New Zealand Sign Language	1
nzu	Teke-Nzikou	1
nzy	Nzakambay	1
nzz	Nanga Dama Dogon	1
oaa	Orok	1
oac	Oroch	1
oar	Old Aramaic (up to 700 BCE)	1
oav	Old Avar	1
obi	Obispeño	1
obk	Southern Bontok	1
obl	Oblo	1
obm	Moabite	1
obo	Obo Manobo	1
obr	Old Burmese	1
obt	Old Breton	1
obu	Obulom	1
oca	Ocaina	1
och	Old Chinese	1
oci	Occitan (post 1500)	1
ocm	Old Cham	1
oco	Old Cornish	1
ocu	Atzingo Matlatzinca	1
oda	Odut	1
odk	Od	1
odt	Old Dutch	1
odu	Odual	1
ofo	Ofo	1
ofs	Old Frisian	1
ofu	Efutop	1
ogb	Ogbia	1
ogc	Ogbah	1
oge	Old Georgian	1
ogg	Ogbogolo	1
ogo	Khana	1
ogu	Ogbronuagum	1
oht	Old Hittite	1
ohu	Old Hungarian	1
oia	Oirata	1
oie	Okolie	1
oin	Inebu One	1
ojb	Northwestern Ojibwa	1
ojc	Central Ojibwa	1
ojg	Eastern Ojibwa	1
oji	Ojibwa	1
ojp	Old Japanese	1
ojs	Severn Ojibwa	1
ojv	Ontong Java	1
ojw	Western Ojibwa	1
oka	Okanagan	1
okb	Okobo	1
okc	Kobo	1
okd	Okodia	1
oke	Okpe (Southwestern Edo)	1
okg	Koko Babangk	1
okh	Koresh-e Rostam	1
oki	Okiek	1
okj	Oko-Juwoi	1
okk	Kwamtim One	1
okl	Old Kentish Sign Language	1
okm	Middle Korean (10th-16th cent.)	1
okn	Oki-No-Erabu	1
oko	Old Korean (3rd-9th cent.)	1
okr	Kirike	1
oks	Oko-Eni-Osayen	1
oku	Oku	1
okv	Orokaiva	1
okx	Okpe (Northwestern Edo)	1
okz	Old Khmer	1
ola	Walungge	1
old	Mochi	1
ole	Olekha	1
olk	Olkol	1
olm	Oloma	1
olo	Livvi	1
olr	Olrat	1
olt	Old Lithuanian	1
olu	Kuvale	1
oma	Omaha-Ponca	1
omb	East Ambae	1
omc	Mochica	1
omg	Omagua	1
omi	Omi	1
omk	Omok	1
oml	Ombo	1
omn	Minoan	1
omo	Utarmbung	1
omp	Old Manipuri	1
omr	Old Marathi	1
omt	Omotik	1
omu	Omurano	1
omw	South Tairora	1
omx	Old Mon	1
omy	Old Malay	1
ona	Ona	1
onb	Lingao	1
one	Oneida	1
ong	Olo	1
oni	Onin	1
onj	Onjob	1
onk	Kabore One	1
onn	Onobasulu	1
ono	Onondaga	1
onp	Sartang	1
onr	Northern One	1
ons	Ono	1
ont	Ontenu	1
onu	Unua	1
onw	Old Nubian	1
onx	Onin Based Pidgin	1
ood	Tohono O'odham	1
oog	Ong	1
oon	Önge	1
oor	Oorlams	1
oos	Old Ossetic	1
opa	Okpamheri	1
opk	Kopkaka	1
opm	Oksapmin	1
opo	Opao	1
opt	Opata	1
opy	Ofayé	1
ora	Oroha	1
orc	Orma	1
ore	Orejón	1
org	Oring	1
orh	Oroqen	1
ori	Oriya (macrolanguage)	1
orm	Oromo	1
orn	Orang Kanaq	1
oro	Orokolo	1
orr	Oruma	1
ors	Orang Seletar	1
ort	Adivasi Oriya	1
oru	Ormuri	1
orv	Old Russian	1
orw	Oro Win	1
orx	Oro	1
ory	Odia	1
orz	Ormu	1
osa	Osage	1
osc	Oscan	1
osi	Osing	1
osn	Old Sundanese	1
oso	Ososo	1
osp	Old Spanish	1
oss	Ossetian	1
ost	Osatu	1
osu	Southern One	1
osx	Old Saxon	1
ota	Ottoman Turkish (1500-1928)	1
otb	Old Tibetan	1
otd	Ot Danum	1
ote	Mezquital Otomi	1
oti	Oti	1
otk	Old Turkish	1
otl	Tilapa Otomi	1
otm	Eastern Highland Otomi	1
otn	Tenango Otomi	1
otq	Querétaro Otomi	1
otr	Otoro	1
ots	Estado de México Otomi	1
ott	Temoaya Otomi	1
otu	Otuke	1
otw	Ottawa	1
otx	Texcatepec Otomi	1
oty	Old Tamil	1
otz	Ixtenco Otomi	1
oua	Tagargrent	1
oub	Glio-Oubi	1
oue	Oune	1
oui	Old Uighur	1
oum	Ouma	1
ovd	Elfdalian	1
owi	Owiniga	1
owl	Old Welsh	1
oyb	Oy	1
oyd	Oyda	1
oym	Wayampi	1
oyy	Oya'oya	1
ozm	Koonzime	1
pab	Parecís	1
pac	Pacoh	1
pad	Paumarí	1
pae	Pagibete	1
paf	Paranawát	1
pag	Pangasinan	1
pah	Tenharim	1
pai	Pe	1
pak	Parakanã	1
pal	Pahlavi	1
pam	Pampanga	1
pan	Panjabi	1
pao	Northern Paiute	1
pap	Papiamento	1
paq	Parya	1
par	Panamint	1
pas	Papasena	1
pau	Palauan	1
pav	Pakaásnovos	1
paw	Pawnee	1
pax	Pankararé	1
pay	Pech	1
paz	Pankararú	1
pbb	Páez	1
pbc	Patamona	1
pbe	Mezontla Popoloca	1
pbf	Coyotepec Popoloca	1
pbg	Paraujano	1
pbh	E'ñapa Woromaipu	1
pbi	Parkwa	1
pbl	Mak (Nigeria)	1
pbm	Puebla Mazatec	1
pbn	Kpasam	1
pbo	Papel	1
pbp	Badyara	1
pbr	Pangwa	1
pbs	Central Pame	1
pbt	Southern Pashto	1
pbu	Northern Pashto	1
pbv	Pnar	1
pby	Pyu (Papua New Guinea)	1
pca	Santa Inés Ahuatempan Popoloca	1
pcb	Pear	1
pcc	Bouyei	1
pcd	Picard	1
pce	Ruching Palaung	1
pcf	Paliyan	1
pcg	Paniya	1
pch	Pardhan	1
pci	Duruwa	1
pcj	Parenga	1
pck	Paite Chin	1
pcl	Pardhi	1
pcm	Nigerian Pidgin	1
pcn	Piti	1
pcp	Pacahuara	1
pcw	Pyapun	1
pda	Anam	1
pdc	Pennsylvania German	1
pdi	Pa Di	1
pdn	Podena	1
pdo	Padoe	1
pdt	Plautdietsch	1
pdu	Kayan	1
pea	Peranakan Indonesian	1
peb	Eastern Pomo	1
ped	Mala (Papua New Guinea)	1
pee	Taje	1
pef	Northeastern Pomo	1
peg	Pengo	1
peh	Bonan	1
pei	Chichimeca-Jonaz	1
pej	Northern Pomo	1
pek	Penchal	1
pel	Pekal	1
pem	Phende	1
peo	Old Persian (ca. 600-400 B.C.)	1
pep	Kunja	1
peq	Southern Pomo	1
pes	Iranian Persian	1
pev	Pémono	1
pex	Petats	1
pey	Petjo	1
pez	Eastern Penan	1
pfa	Pááfang	1
pfe	Pere	1
pfl	Pfaelzisch	1
pga	Sudanese Creole Arabic	1
pgd	Gāndhārī	1
pgg	Pangwali	1
pgi	Pagi	1
pgk	Rerep	1
pgl	Primitive Irish	1
pgn	Paelignian	1
pgs	Pangseng	1
pgu	Pagu	1
pgz	Papua New Guinean Sign Language	1
pha	Pa-Hng	1
phd	Phudagi	1
phg	Phuong	1
phh	Phukha	1
phj	Pahari	1
phk	Phake	1
phl	Phalura	1
phm	Phimbi	1
phn	Phoenician	1
pho	Phunoi	1
phq	Phana'	1
phr	Pahari-Potwari	1
pht	Phu Thai	1
phu	Phuan	1
phv	Pahlavani	1
phw	Phangduwali	1
pia	Pima Bajo	1
pib	Yine	1
pic	Pinji	1
pid	Piaroa	1
pie	Piro	1
pif	Pingelapese	1
pig	Pisabo	1
pih	Pitcairn-Norfolk	1
pij	Pijao	1
pil	Yom	1
pim	Powhatan	1
pin	Piame	1
pio	Piapoco	1
pip	Pero	1
pir	Piratapuyo	1
pis	Pijin	1
pit	Pitta Pitta	1
piu	Pintupi-Luritja	1
piv	Pileni	1
piw	Pimbwe	1
pix	Piu	1
piy	Piya-Kwonci	1
piz	Pije	1
pjt	Pitjantjatjara	1
pka	Ardhamāgadhī Prākrit	1
pkb	Pokomo	1
pkc	Paekche	1
pkg	Pak-Tong	1
pkh	Pankhu	1
pkn	Pakanha	1
pko	Pökoot	1
pkp	Pukapuka	1
pkr	Attapady Kurumba	1
pks	Pakistan Sign Language	1
pkt	Maleng	1
pku	Paku	1
pla	Miani	1
plb	Polonombauk	1
plc	Central Palawano	1
pld	Polari	1
ple	Palu'e	1
plg	Pilagá	1
plh	Paulohi	1
pli	Pali	1
plk	Kohistani Shina	1
pll	Shwe Palaung	1
pln	Palenquero	1
plo	Oluta Popoluca	1
plq	Palaic	1
plr	Palaka Senoufo	1
pls	San Marcos Tlacoyalco Popoloca	1
plt	Plateau Malagasy	1
plu	Palikúr	1
plv	Southwest Palawano	1
plw	Brooke's Point Palawano	1
ply	Bolyu	1
plz	Paluan	1
pma	Paama	1
pmb	Pambia	1
pmd	Pallanganmiddang	1
pme	Pwaamei	1
pmf	Pamona	1
pmh	Māhārāṣṭri Prākrit	1
pmi	Northern Pumi	1
pmj	Southern Pumi	1
pml	Lingua Franca	1
pmm	Pomo	1
pmn	Pam	1
pmo	Pom	1
pmq	Northern Pame	1
pmr	Paynamar	1
pms	Piemontese	1
pmt	Tuamotuan	1
pmw	Plains Miwok	1
pmx	Poumei Naga	1
pmy	Papuan Malay	1
pmz	Southern Pame	1
pna	Punan Bah-Biau	1
pnb	Western Panjabi	1
pnc	Pannei	1
pnd	Mpinda	1
pne	Western Penan	1
png	Pangu	1
pnh	Penrhyn	1
pni	Aoheng	1
pnj	Pinjarup	1
pnk	Paunaka	1
pnl	Paleni	1
pnm	Punan Batu 1	1
pnn	Pinai-Hagahai	1
pno	Panobo	1
pnp	Pancana	1
pnq	Pana (Burkina Faso)	1
pnr	Panim	1
pns	Ponosakan	1
pnt	Pontic	1
pnu	Jiongnai Bunu	1
pnv	Pinigura	1
pnw	Banyjima	1
pnx	Phong-Kniang	1
pny	Pinyin	1
pnz	Pana (Central African Republic)	1
poc	Poqomam	1
poe	San Juan Atzingo Popoloca	1
pof	Poke	1
pog	Potiguára	1
poh	Poqomchi'	1
poi	Highland Popoluca	1
pok	Pokangá	1
pol	Polish	1
pom	Southeastern Pomo	1
pon	Pohnpeian	1
poo	Central Pomo	1
pop	Pwapwâ	1
poq	Texistepec Popoluca	1
por	Portuguese	1
pos	Sayula Popoluca	1
pot	Potawatomi	1
pov	Upper Guinea Crioulo	1
pow	San Felipe Otlaltepec Popoloca	1
pox	Polabian	1
poy	Pogolo	1
ppe	Papi	1
ppi	Paipai	1
ppk	Uma	1
ppl	Pipil	1
ppm	Papuma	1
ppn	Papapana	1
ppo	Folopa	1
ppp	Pelende	1
ppq	Pei	1
pps	San Luís Temalacayuca Popoloca	1
ppt	Pare	1
ppu	Papora	1
pqa	Pa'a	1
pqm	Malecite-Passamaquoddy	1
prc	Parachi	1
prd	Parsi-Dari	1
pre	Principense	1
prf	Paranan	1
prg	Prussian	1
prh	Porohanon	1
pri	Paicî	1
prk	Parauk	1
prl	Peruvian Sign Language	1
prm	Kibiri	1
prn	Prasuni	1
pro	Old Provençal (to 1500)	1
prq	Ashéninka Perené	1
prr	Puri	1
prs	Dari	1
prt	Phai	1
pru	Puragi	1
prw	Parawen	1
prx	Purik	1
prz	Providencia Sign Language	1
psa	Asue Awyu	1
psc	Iranian Sign Language	1
psd	Plains Indian Sign Language	1
pse	Central Malay	1
psg	Penang Sign Language	1
psh	Southwest Pashai	1
psi	Southeast Pashai	1
psl	Puerto Rican Sign Language	1
psm	Pauserna	1
psn	Panasuan	1
pso	Polish Sign Language	1
psp	Philippine Sign Language	1
psq	Pasi	1
psr	Portuguese Sign Language	1
pss	Kaulong	1
pst	Central Pashto	1
psu	Sauraseni Prākrit	1
psw	Port Sandwich	1
psy	Piscataway	1
pta	Pai Tavytera	1
pth	Pataxó Hã-Ha-Hãe	1
pti	Pindiini	1
ptn	Patani	1
pto	Zo'é	1
ptp	Patep	1
ptq	Pattapu	1
ptr	Piamatsina	1
ptt	Enrekang	1
ptu	Bambam	1
ptv	Port Vato	1
ptw	Pentlatch	1
pty	Pathiya	1
pua	Western Highland Purepecha	1
pub	Purum	1
puc	Punan Merap	1
pud	Punan Aput	1
pue	Puelche	1
puf	Punan Merah	1
pug	Phuie	1
pui	Puinave	1
puj	Punan Tubu	1
pum	Puma	1
puo	Puoc	1
pup	Pulabu	1
puq	Puquina	1
pur	Puruborá	1
pus	Pushto	1
put	Putoh	1
puu	Punu	1
puw	Puluwatese	1
pux	Puare	1
puy	Purisimeño	1
pwa	Pawaia	1
pwb	Panawa	1
pwg	Gapapaiwa	1
pwi	Patwin	1
pwm	Molbog	1
pwn	Paiwan	1
pwo	Pwo Western Karen	1
pwr	Powari	1
pww	Pwo Northern Karen	1
pxm	Quetzaltepec Mixe	1
pye	Pye Krumen	1
pym	Fyam	1
pyn	Poyanáwa	1
pys	Paraguayan Sign Language	1
pyu	Puyuma	1
pyx	Pyu (Myanmar)	1
pyy	Pyen	1
pze	Pesse	1
pzh	Pazeh	1
pzn	Jejara Naga	1
qua	Quapaw	1
qub	Huallaga Huánuco Quechua	1
quc	K'iche'	1
qud	Calderón Highland Quichua	1
que	Quechua	1
quf	Lambayeque Quechua	1
qug	Chimborazo Highland Quichua	1
quh	South Bolivian Quechua	1
qui	Quileute	1
quk	Chachapoyas Quechua	1
qul	North Bolivian Quechua	1
qum	Sipacapense	1
qun	Quinault	1
qup	Southern Pastaza Quechua	1
quq	Quinqui	1
qur	Yanahuanca Pasco Quechua	1
qus	Santiago del Estero Quichua	1
quv	Sacapulteco	1
quw	Tena Lowland Quichua	1
qux	Yauyos Quechua	1
quy	Ayacucho Quechua	1
quz	Cusco Quechua	1
qva	Ambo-Pasco Quechua	1
qvc	Cajamarca Quechua	1
qve	Eastern Apurímac Quechua	1
qvh	Huamalíes-Dos de Mayo Huánuco Quechua	1
qvi	Imbabura Highland Quichua	1
qvj	Loja Highland Quichua	1
qvl	Cajatambo North Lima Quechua	1
qvm	Margos-Yarowilca-Lauricocha Quechua	1
qvn	North Junín Quechua	1
qvo	Napo Lowland Quechua	1
qvp	Pacaraos Quechua	1
qvs	San Martín Quechua	1
qvw	Huaylla Wanca Quechua	1
qvy	Queyu	1
qvz	Northern Pastaza Quichua	1
qwa	Corongo Ancash Quechua	1
qwc	Classical Quechua	1
qwh	Huaylas Ancash Quechua	1
qwm	Kuman (Russia)	1
qws	Sihuas Ancash Quechua	1
qwt	Kwalhioqua-Tlatskanai	1
qxa	Chiquián Ancash Quechua	1
qxc	Chincha Quechua	1
qxh	Panao Huánuco Quechua	1
qxl	Salasaca Highland Quichua	1
qxn	Northern Conchucos Ancash Quechua	1
qxo	Southern Conchucos Ancash Quechua	1
qxp	Puno Quechua	1
qxq	Qashqa'i	1
qxr	Cañar Highland Quichua	1
qxs	Southern Qiang	1
qxt	Santa Ana de Tusi Pasco Quechua	1
qxu	Arequipa-La Unión Quechua	1
qxw	Jauja Wanca Quechua	1
qya	Quenya	1
qyp	Quiripi	1
raa	Dungmali	1
rab	Camling	1
rac	Rasawa	1
rad	Rade	1
raf	Western Meohang	1
rag	Logooli	1
rah	Rabha	1
rai	Ramoaaina	1
raj	Rajasthani	1
rak	Tulu-Bohuai	1
ral	Ralte	1
ram	Canela	1
ran	Riantana	1
rao	Rao	1
rap	Rapanui	1
raq	Saam	1
rar	Rarotongan	1
ras	Tegali	1
rat	Razajerdi	1
rau	Raute	1
rav	Sampang	1
raw	Rawang	1
rax	Rang	1
ray	Rapa	1
raz	Rahambuu	1
rbb	Rumai Palaung	1
rbk	Northern Bontok	1
rbl	Miraya Bikol	1
rbp	Barababaraba	1
rcf	Réunion Creole French	1
rdb	Rudbari	1
rea	Rerau	1
reb	Rembong	1
ree	Rejang Kayan	1
reg	Kara (Tanzania)	1
rei	Reli	1
rej	Rejang	1
rel	Rendille	1
rem	Remo	1
ren	Rengao	1
rer	Rer Bare	1
res	Reshe	1
ret	Retta	1
rey	Reyesano	1
rga	Roria	1
rge	Romano-Greek	1
rgk	Rangkas	1
rgn	Romagnol	1
rgr	Resígaro	1
rgs	Southern Roglai	1
rgu	Ringgou	1
rhg	Rohingya	1
rhp	Yahang	1
ria	Riang (India)	1
rib	Bribri Sign Language	1
rif	Tarifit	1
ril	Riang Lang	1
rim	Nyaturu	1
rin	Nungu	1
rir	Ribun	1
rit	Ritharrngu	1
riu	Riung	1
rjg	Rajong	1
rji	Raji	1
rjs	Rajbanshi	1
rka	Kraol	1
rkb	Rikbaktsa	1
rkh	Rakahanga-Manihiki	1
rki	Rakhine	1
rkm	Marka	1
rkt	Rangpuri	1
rkw	Arakwal	1
rma	Rama	1
rmb	Rembarrnga	1
rmc	Carpathian Romani	1
rmd	Traveller Danish	1
rme	Angloromani	1
rmf	Kalo Finnish Romani	1
rmg	Traveller Norwegian	1
rmh	Murkim	1
rmi	Lomavren	1
rmk	Romkun	1
rml	Baltic Romani	1
rmm	Roma	1
rmn	Balkan Romani	1
rmo	Sinte Romani	1
rmp	Rempi	1
rmq	Caló	1
rms	Romanian Sign Language	1
rmt	Domari	1
rmu	Tavringer Romani	1
rmv	Romanova	1
rmw	Welsh Romani	1
rmx	Romam	1
rmy	Vlax Romani	1
rmz	Marma	1
rnb	Brunca Sign Language	1
rnd	Ruund	1
rng	Ronga	1
rnl	Ranglong	1
rnn	Roon	1
rnp	Rongpo	1
rnr	Nari Nari	1
rnw	Rungwa	1
rob	Tae'	1
roc	Cacgia Roglai	1
rod	Rogo	1
roe	Ronji	1
rof	Rombo	1
rog	Northern Roglai	1
roh	Romansh	1
rol	Romblomanon	1
rom	Romany	1
ron	Romanian	1
roo	Rotokas	1
rop	Kriol	1
ror	Rongga	1
rou	Runga	1
row	Dela-Oenale	1
rpn	Repanbitip	1
rpt	Rapting	1
rri	Ririo	1
rro	Waima	1
rrt	Arritinngithigh	1
rsb	Romano-Serbian	1
rsk	Ruthenian	1
rsl	Russian Sign Language	1
rsm	Miriwoong Sign Language	1
rsn	Rwandan Sign Language	1
rsw	Rishiwa	1
rtc	Rungtu Chin	1
rth	Ratahan	1
rtm	Rotuman	1
rts	Yurats	1
rtw	Rathawi	1
rub	Gungu	1
ruc	Ruuli	1
rue	Rusyn	1
ruf	Luguru	1
rug	Roviana	1
ruh	Ruga	1
rui	Rufiji	1
ruk	Che	1
run	Rundi	1
ruo	Istro Romanian	1
rup	Macedo-Romanian	1
ruq	Megleno Romanian	1
rus	Russian	1
rut	Rutul	1
ruu	Lanas Lobu	1
ruy	Mala (Nigeria)	1
ruz	Ruma	1
rwa	Rawo	1
rwk	Rwa	1
rwl	Ruwila	1
rwm	Amba (Uganda)	1
rwo	Rawa	1
rwr	Marwari (India)	1
rxd	Ngardi	1
rxw	Karuwali	1
ryn	Northern Amami-Oshima	1
rys	Yaeyama	1
ryu	Central Okinawan	1
rzh	Rāziḥī	1
saa	Saba	1
sab	Buglere	1
sac	Meskwaki	1
sad	Sandawe	1
sae	Sabanê	1
saf	Safaliba	1
sag	Sango	1
sah	Yakut	1
saj	Sahu	1
sak	Sake	1
sam	Samaritan Aramaic	1
san	Sanskrit	1
sao	Sause	1
saq	Samburu	1
sar	Saraveca	1
sas	Sasak	1
sat	Santali	1
sau	Saleman	1
sav	Saafi-Saafi	1
saw	Sawi	1
sax	Sa	1
say	Saya	1
saz	Saurashtra	1
sba	Ngambay	1
sbb	Simbo	1
sbc	Kele (Papua New Guinea)	1
sbd	Southern Samo	1
sbe	Saliba	1
sbf	Chabu	1
sbg	Seget	1
sbh	Sori-Harengan	1
sbi	Seti	1
sbj	Surbakhal	1
sbk	Safwa	1
sbl	Botolan Sambal	1
sbm	Sagala	1
sbn	Sindhi Bhil	1
sbo	Sabüm	1
sbp	Sangu (Tanzania)	1
sbq	Sileibi	1
sbr	Sembakung Murut	1
sbs	Subiya	1
sbt	Kimki	1
sbu	Stod Bhoti	1
sbv	Sabine	1
sbw	Simba	1
sbx	Seberuang	1
sby	Soli	1
sbz	Sara Kaba	1
scb	Chut	1
sce	Dongxiang	1
scf	San Miguel Creole French	1
scg	Sanggau	1
sch	Sakachep	1
sci	Sri Lankan Creole Malay	1
sck	Sadri	1
scl	Shina	1
scn	Sicilian	1
sco	Scots	1
scp	Hyolmo	1
scq	Sa'och	1
scs	North Slavey	1
sct	Southern Katang	1
scu	Shumcho	1
scv	Sheni	1
scw	Sha	1
scx	Sicel	1
sda	Toraja-Sa'dan	1
sdb	Shabak	1
sdc	Sassarese Sardinian	1
sde	Surubu	1
sdf	Sarli	1
sdg	Savi	1
sdh	Southern Kurdish	1
sdj	Suundi	1
sdk	Sos Kundi	1
sdl	Saudi Arabian Sign Language	1
sdn	Gallurese Sardinian	1
sdo	Bukar-Sadung Bidayuh	1
sdp	Sherdukpen	1
sdq	Semandang	1
sdr	Oraon Sadri	1
sds	Sened	1
sdt	Shuadit	1
sdu	Sarudu	1
sdx	Sibu Melanau	1
sdz	Sallands	1
sea	Semai	1
seb	Shempire Senoufo	1
sec	Sechelt	1
sed	Sedang	1
see	Seneca	1
sef	Cebaara Senoufo	1
seg	Segeju	1
seh	Sena	1
sei	Seri	1
sej	Sene	1
sek	Sekani	1
sel	Selkup	1
sen	Nanerigé Sénoufo	1
seo	Suarmin	1
sep	Sìcìté Sénoufo	1
seq	Senara Sénoufo	1
ser	Serrano	1
ses	Koyraboro Senni Songhai	1
set	Sentani	1
seu	Serui-Laut	1
sev	Nyarafolo Senoufo	1
sew	Sewa Bay	1
sey	Secoya	1
sez	Senthang Chin	1
sfb	Langue des signes de Belgique Francophone	1
sfe	Eastern Subanen	1
sfm	Small Flowery Miao	1
sfs	South African Sign Language	1
sfw	Sehwi	1
sga	Old Irish (to 900)	1
sgb	Mag-antsi Ayta	1
sgc	Kipsigis	1
sgd	Surigaonon	1
sge	Segai	1
sgg	Swiss-German Sign Language	1
sgh	Shughni	1
sgi	Suga	1
sgj	Surgujia	1
sgk	Sangkong	1
sgm	Singa	1
sgp	Singpho	1
sgr	Sangisari	1
sgs	Samogitian	1
sgt	Brokpake	1
sgu	Salas	1
sgw	Sebat Bet Gurage	1
sgx	Sierra Leone Sign Language	1
sgy	Sanglechi	1
sgz	Sursurunga	1
sha	Shall-Zwall	1
shb	Ninam	1
shc	Sonde	1
shd	Kundal Shahi	1
she	Sheko	1
shg	Shua	1
shh	Shoshoni	1
shi	Tachelhit	1
shj	Shatt	1
shk	Shilluk	1
shl	Shendu	1
shm	Shahrudi	1
shn	Shan	1
sho	Shanga	1
shp	Shipibo-Conibo	1
shq	Sala	1
shr	Shi	1
shs	Shuswap	1
sht	Shasta	1
shu	Chadian Arabic	1
shv	Shehri	1
shw	Shwai	1
shx	She	1
shy	Tachawit	1
shz	Syenara Senoufo	1
sia	Akkala Sami	1
sib	Sebop	1
sid	Sidamo	1
sie	Simaa	1
sif	Siamou	1
sig	Paasaal	1
sih	Zire	1
sii	Shom Peng	1
sij	Numbami	1
sik	Sikiana	1
sil	Tumulung Sisaala	1
sim	Mende (Papua New Guinea)	1
sin	Sinhala	1
sip	Sikkimese	1
siq	Sonia	1
sir	Siri	1
sis	Siuslaw	1
siu	Sinagen	1
siv	Sumariup	1
siw	Siwai	1
six	Sumau	1
siy	Sivandi	1
siz	Siwi	1
sja	Epena	1
sjb	Sajau Basap	1
sjd	Kildin Sami	1
sje	Pite Sami	1
sjg	Assangori	1
sjk	Kemi Sami	1
sjl	Sajalong	1
sjm	Mapun	1
sjn	Sindarin	1
sjo	Xibe	1
sjp	Surjapuri	1
sjr	Siar-Lak	1
sjs	Senhaja De Srair	1
sjt	Ter Sami	1
sju	Ume Sami	1
sjw	Shawnee	1
ska	Skagit	1
skb	Saek	1
skc	Ma Manda	1
skd	Southern Sierra Miwok	1
ske	Seke (Vanuatu)	1
skf	Sakirabiá	1
skg	Sakalava Malagasy	1
skh	Sikule	1
ski	Sika	1
skj	Seke (Nepal)	1
skm	Kutong	1
skn	Kolibugan Subanon	1
sko	Seko Tengah	1
skp	Sekapan	1
skq	Sininkere	1
skr	Saraiki	1
sks	Maia	1
skt	Sakata	1
sku	Sakao	1
skv	Skou	1
skw	Skepi Creole Dutch	1
skx	Seko Padang	1
sky	Sikaiana	1
skz	Sekar	1
slc	Sáliba	1
sld	Sissala	1
sle	Sholaga	1
slf	Swiss-Italian Sign Language	1
slg	Selungai Murut	1
slh	Southern Puget Sound Salish	1
sli	Lower Silesian	1
slj	Salumá	1
slk	Slovak	1
sll	Salt-Yui	1
slm	Pangutaran Sama	1
sln	Salinan	1
slp	Lamaholot	1
slr	Salar	1
sls	Singapore Sign Language	1
slt	Sila	1
slu	Selaru	1
slv	Slovenian	1
slw	Sialum	1
slx	Salampasu	1
sly	Selayar	1
slz	Ma'ya	1
sma	Southern Sami	1
smb	Simbari	1
smc	Som	1
sme	Northern Sami	1
smf	Auwe	1
smg	Simbali	1
smh	Samei	1
smj	Lule Sami	1
smk	Bolinao	1
sml	Central Sama	1
smm	Musasa	1
smn	Inari Sami	1
smo	Samoan	1
smp	Samaritan	1
smq	Samo	1
smr	Simeulue	1
sms	Skolt Sami	1
smt	Simte	1
smu	Somray	1
smv	Samvedi	1
smw	Sumbawa	1
smx	Samba	1
smy	Semnani	1
smz	Simeku	1
sna	Shona	1
snc	Sinaugoro	1
snd	Sindhi	1
sne	Bau Bidayuh	1
snf	Noon	1
sng	Sanga (Democratic Republic of Congo)	1
sni	Sensi	1
snj	Riverain Sango	1
snk	Soninke	1
snl	Sangil	1
snm	Southern Ma'di	1
snn	Siona	1
sno	Snohomish	1
snp	Siane	1
snq	Sangu (Gabon)	1
snr	Sihan	1
sns	South West Bay	1
snu	Senggi	1
snv	Sa'ban	1
snw	Selee	1
snx	Sam	1
sny	Saniyo-Hiyewe	1
snz	Kou	1
soa	Thai Song	1
sob	Sobei	1
soc	So (Democratic Republic of Congo)	1
sod	Songoora	1
soe	Songomeno	1
sog	Sogdian	1
soh	Aka	1
soi	Sonha	1
soj	Soi	1
sok	Sokoro	1
sol	Solos	1
som	Somali	1
soo	Songo	1
sop	Songe	1
soq	Kanasi	1
sor	Somrai	1
sos	Seeku	1
sot	Southern Sotho	1
sou	Southern Thai	1
sov	Sonsorol	1
sow	Sowanda	1
sox	Swo	1
soy	Miyobe	1
soz	Temi	1
spa	Spanish	1
spb	Sepa (Indonesia)	1
spc	Sapé	1
spd	Saep	1
spe	Sepa (Papua New Guinea)	1
spg	Sian	1
spi	Saponi	1
spk	Sengo	1
spl	Selepet	1
spm	Akukem	1
spn	Sanapaná	1
spo	Spokane	1
spp	Supyire Senoufo	1
spq	Loreto-Ucayali Spanish	1
spr	Saparua	1
sps	Saposa	1
spt	Spiti Bhoti	1
spu	Sapuan	1
spv	Sambalpuri	1
spx	South Picene	1
spy	Sabaot	1
sqa	Shama-Sambuga	1
sqh	Shau	1
sqi	Albanian	1
sqk	Albanian Sign Language	1
sqm	Suma	1
sqn	Susquehannock	1
sqo	Sorkhei	1
sqq	Sou	1
sqr	Siculo Arabic	1
sqs	Sri Lankan Sign Language	1
sqt	Soqotri	1
squ	Squamish	1
sqx	Kufr Qassem Sign Language (KQSL)	1
sra	Saruga	1
srb	Sora	1
src	Logudorese Sardinian	1
srd	Sardinian	1
sre	Sara	1
srf	Nafi	1
srg	Sulod	1
srh	Sarikoli	1
sri	Siriano	1
srk	Serudung Murut	1
srl	Isirawa	1
srm	Saramaccan	1
srn	Sranan Tongo	1
sro	Campidanese Sardinian	1
srp	Serbian	1
srq	Sirionó	1
srr	Serer	1
srs	Sarsi	1
srt	Sauri	1
sru	Suruí	1
srv	Southern Sorsoganon	1
srw	Serua	1
srx	Sirmauri	1
sry	Sera	1
srz	Shahmirzadi	1
ssb	Southern Sama	1
ssc	Suba-Simbiti	1
ssd	Siroi	1
sse	Balangingi	1
ssf	Thao	1
ssg	Seimat	1
ssh	Shihhi Arabic	1
ssi	Sansi	1
ssj	Sausi	1
ssk	Sunam	1
ssl	Western Sisaala	1
ssm	Semnam	1
ssn	Waata	1
sso	Sissano	1
ssp	Spanish Sign Language	1
ssq	So'a	1
ssr	Swiss-French Sign Language	1
sss	Sô	1
sst	Sinasina	1
ssu	Susuami	1
ssv	Shark Bay	1
ssw	Swati	1
ssx	Samberigi	1
ssy	Saho	1
ssz	Sengseng	1
sta	Settla	1
stb	Northern Subanen	1
std	Sentinel	1
ste	Liana-Seti	1
stf	Seta	1
stg	Trieng	1
sth	Shelta	1
sti	Bulo Stieng	1
stj	Matya Samo	1
stk	Arammba	1
stl	Stellingwerfs	1
stm	Setaman	1
stn	Owa	1
sto	Stoney	1
stp	Southeastern Tepehuan	1
stq	Saterfriesisch	1
str	Straits Salish	1
sts	Shumashti	1
stt	Budeh Stieng	1
stu	Samtao	1
stv	Silt'e	1
stw	Satawalese	1
sty	Siberian Tatar	1
sua	Sulka	1
sub	Suku	1
suc	Western Subanon	1
sue	Suena	1
sug	Suganga	1
sui	Suki	1
suj	Shubi	1
suk	Sukuma	1
sun	Sundanese	1
suo	Bouni	1
suq	Tirmaga-Chai Suri	1
sur	Mwaghavul	1
sus	Susu	1
sut	Subtiaba	1
suv	Puroik	1
suw	Sumbwa	1
sux	Sumerian	1
suy	Suyá	1
suz	Sunwar	1
sva	Svan	1
svb	Ulau-Suain	1
svc	Vincentian Creole English	1
sve	Serili	1
svk	Slovakian Sign Language	1
svm	Slavomolisano	1
svs	Savosavo	1
svx	Skalvian	1
swa	Swahili (macrolanguage)	1
swb	Maore Comorian	1
swc	Congo Swahili	1
swe	Swedish	1
swf	Sere	1
swg	Swabian	1
swh	Swahili (individual language)	1
swi	Sui	1
swj	Sira	1
swk	Malawi Sena	1
swl	Swedish Sign Language	1
swm	Samosa	1
swn	Sawknah	1
swo	Shanenawa	1
swp	Suau	1
swq	Sharwa	1
swr	Saweru	1
sws	Seluwasan	1
swt	Sawila	1
swu	Suwawa	1
swv	Shekhawati	1
sww	Sowa	1
swx	Suruahá	1
swy	Sarua	1
sxb	Suba	1
sxc	Sicanian	1
sxe	Sighu	1
sxg	Shuhi	1
sxk	Southern Kalapuya	1
sxl	Selian	1
sxm	Samre	1
sxn	Sangir	1
sxo	Sorothaptic	1
sxr	Saaroa	1
sxs	Sasaru	1
sxu	Upper Saxon	1
sxw	Saxwe Gbe	1
sya	Siang	1
syb	Central Subanen	1
syc	Classical Syriac	1
syi	Seki	1
syk	Sukur	1
syl	Sylheti	1
sym	Maya Samo	1
syn	Senaya	1
syo	Suoy	1
syr	Syriac	1
sys	Sinyar	1
syw	Kagate	1
syx	Samay	1
syy	Al-Sayyid Bedouin Sign Language	1
sza	Semelai	1
szb	Ngalum	1
szc	Semaq Beri	1
sze	Seze	1
szg	Sengele	1
szl	Silesian	1
szn	Sula	1
szp	Suabo	1
szs	Solomon Islands Sign Language	1
szv	Isu (Fako Division)	1
szw	Sawai	1
szy	Sakizaya	1
taa	Lower Tanana	1
tab	Tabassaran	1
tac	Lowland Tarahumara	1
tad	Tause	1
tae	Tariana	1
taf	Tapirapé	1
tag	Tagoi	1
tah	Tahitian	1
taj	Eastern Tamang	1
tak	Tala	1
tal	Tal	1
tam	Tamil	1
tan	Tangale	1
tao	Yami	1
tap	Taabwa	1
taq	Tamasheq	1
tar	Central Tarahumara	1
tas	Tay Boi	1
tat	Tatar	1
tau	Upper Tanana	1
tav	Tatuyo	1
taw	Tai	1
tax	Tamki	1
tay	Atayal	1
taz	Tocho	1
tba	Aikanã	1
tbc	Takia	1
tbd	Kaki Ae	1
tbe	Tanimbili	1
tbf	Mandara	1
tbg	North Tairora	1
tbh	Dharawal	1
tbi	Gaam	1
tbj	Tiang	1
tbk	Calamian Tagbanwa	1
tbl	Tboli	1
tbm	Tagbu	1
tbn	Barro Negro Tunebo	1
tbo	Tawala	1
tbp	Taworta	1
tbr	Tumtum	1
tbs	Tanguat	1
tbt	Tembo (Kitembo)	1
tbu	Tubar	1
tbv	Tobo	1
tbw	Tagbanwa	1
tbx	Kapin	1
tby	Tabaru	1
tbz	Ditammari	1
tca	Ticuna	1
tcb	Tanacross	1
tcc	Datooga	1
tcd	Tafi	1
tce	Southern Tutchone	1
tcf	Malinaltepec Me'phaa	1
tcg	Tamagario	1
tch	Turks And Caicos Creole English	1
tci	Wára	1
tck	Tchitchege	1
tcl	Taman (Myanmar)	1
tcm	Tanahmerah	1
tcn	Tichurong	1
tco	Taungyo	1
tcp	Tawr Chin	1
tcq	Kaiy	1
tcs	Torres Strait Creole	1
tct	T'en	1
tcu	Southeastern Tarahumara	1
tcw	Tecpatlán Totonac	1
tcx	Toda	1
tcy	Tulu	1
tcz	Thado Chin	1
tda	Tagdal	1
tdb	Panchpargania	1
tdc	Emberá-Tadó	1
tdd	Tai Nüa	1
tde	Tiranige Diga Dogon	1
tdf	Talieng	1
tdg	Western Tamang	1
tdh	Thulung	1
tdi	Tomadino	1
tdj	Tajio	1
tdk	Tambas	1
tdl	Sur	1
tdm	Taruma	1
tdn	Tondano	1
tdo	Teme	1
tdq	Tita	1
tdr	Todrah	1
tds	Doutai	1
tdt	Tetun Dili	1
tdv	Toro	1
tdx	Tandroy-Mahafaly Malagasy	1
tdy	Tadyawan	1
tea	Temiar	1
teb	Tetete	1
tec	Terik	1
ted	Tepo Krumen	1
tee	Huehuetla Tepehua	1
tef	Teressa	1
teg	Teke-Tege	1
teh	Tehuelche	1
tei	Torricelli	1
tek	Ibali Teke	1
tel	Telugu	1
tem	Timne	1
ten	Tama (Colombia)	1
teo	Teso	1
tep	Tepecano	1
teq	Temein	1
ter	Tereno	1
tes	Tengger	1
tet	Tetum	1
teu	Soo	1
tev	Teor	1
tew	Tewa (USA)	1
tex	Tennet	1
tey	Tulishi	1
tez	Tetserret	1
tfi	Tofin Gbe	1
tfn	Tanaina	1
tfo	Tefaro	1
tfr	Teribe	1
tft	Ternate	1
tga	Sagalla	1
tgb	Tobilung	1
tgc	Tigak	1
tgd	Ciwogai	1
tge	Eastern Gorkha Tamang	1
tgf	Chalikha	1
tgh	Tobagonian Creole English	1
tgi	Lawunuia	1
tgj	Tagin	1
tgk	Tajik	1
tgl	Tagalog	1
tgn	Tandaganon	1
tgo	Sudest	1
tgp	Tangoa	1
tgq	Tring	1
tgr	Tareng	1
tgs	Nume	1
tgt	Central Tagbanwa	1
tgu	Tanggu	1
tgv	Tingui-Boto	1
tgw	Tagwana Senoufo	1
tgx	Tagish	1
tgy	Togoyo	1
tgz	Tagalaka	1
tha	Thai	1
thd	Kuuk Thaayorre	1
the	Chitwania Tharu	1
thf	Thangmi	1
thh	Northern Tarahumara	1
thi	Tai Long	1
thk	Tharaka	1
thl	Dangaura Tharu	1
thm	Aheu	1
thn	Thachanadan	1
thp	Thompson	1
thq	Kochila Tharu	1
thr	Rana Tharu	1
ths	Thakali	1
tht	Tahltan	1
thu	Thuri	1
thv	Tahaggart Tamahaq	1
thy	Tha	1
thz	Tayart Tamajeq	1
tia	Tidikelt Tamazight	1
tic	Tira	1
tif	Tifal	1
tig	Tigre	1
tih	Timugon Murut	1
tii	Tiene	1
tij	Tilung	1
tik	Tikar	1
til	Tillamook	1
tim	Timbe	1
tin	Tindi	1
tio	Teop	1
tip	Trimuris	1
tiq	Tiéfo	1
tir	Tigrinya	1
tis	Masadiit Itneg	1
tit	Tinigua	1
tiu	Adasen	1
tiv	Tiv	1
tiw	Tiwi	1
tix	Southern Tiwa	1
tiy	Tiruray	1
tiz	Tai Hongjin	1
tja	Tajuasohn	1
tjg	Tunjung	1
tji	Northern Tujia	1
tjj	Tjungundji	1
tjl	Tai Laing	1
tjm	Timucua	1
tjn	Tonjon	1
tjo	Temacine Tamazight	1
tjp	Tjupany	1
tjs	Southern Tujia	1
tju	Tjurruru	1
tjw	Djabwurrung	1
tka	Truká	1
tkb	Buksa	1
tkd	Tukudede	1
tke	Takwane	1
tkf	Tukumanféd	1
tkg	Tesaka Malagasy	1
tkl	Tokelau	1
tkm	Takelma	1
tkn	Toku-No-Shima	1
tkp	Tikopia	1
tkq	Tee	1
tkr	Tsakhur	1
tks	Takestani	1
tkt	Kathoriya Tharu	1
tku	Upper Necaxa Totonac	1
tkv	Mur Pano	1
tkw	Teanu	1
tkx	Tangko	1
tkz	Takua	1
tla	Southwestern Tepehuan	1
tlb	Tobelo	1
tlc	Yecuatla Totonac	1
tld	Talaud	1
tlf	Telefol	1
tlg	Tofanma	1
tlh	Klingon	1
tli	Tlingit	1
tlj	Talinga-Bwisi	1
tlk	Taloki	1
tll	Tetela	1
tlm	Tolomako	1
tln	Talondo'	1
tlo	Talodi	1
tlp	Filomena Mata-Coahuitlán Totonac	1
tlq	Tai Loi	1
tlr	Talise	1
tls	Tambotalo	1
tlt	Sou Nama	1
tlu	Tulehu	1
tlv	Taliabu	1
tlx	Khehek	1
tly	Talysh	1
tma	Tama (Chad)	1
tmb	Katbol	1
tmc	Tumak	1
tmd	Haruai	1
tme	Tremembé	1
tmf	Toba-Maskoy	1
tmg	Ternateño	1
tmh	Tamashek	1
tmi	Tutuba	1
tmj	Samarokena	1
tml	Tamnim Citak	1
tmm	Tai Thanh	1
tmn	Taman (Indonesia)	1
tmo	Temoq	1
tmq	Tumleo	1
tmr	Jewish Babylonian Aramaic (ca. 200-1200 CE)	1
tms	Tima	1
tmt	Tasmate	1
tmu	Iau	1
tmv	Tembo (Motembo)	1
tmw	Temuan	1
tmy	Tami	1
tmz	Tamanaku	1
tna	Tacana	1
tnb	Western Tunebo	1
tnc	Tanimuca-Retuarã	1
tnd	Angosturas Tunebo	1
tng	Tobanga	1
tnh	Maiani	1
tni	Tandia	1
tnk	Kwamera	1
tnl	Lenakel	1
tnm	Tabla	1
tnn	North Tanna	1
tno	Toromono	1
tnp	Whitesands	1
tnq	Taino	1
tnr	Ménik	1
tns	Tenis	1
tnt	Tontemboan	1
tnu	Tay Khang	1
tnv	Tangchangya	1
tnw	Tonsawang	1
tnx	Tanema	1
tny	Tongwe	1
tnz	Ten'edn	1
tob	Toba	1
toc	Coyutla Totonac	1
tod	Toma	1
tof	Gizrra	1
tog	Tonga (Nyasa)	1
toh	Gitonga	1
toi	Tonga (Zambia)	1
toj	Tojolabal	1
tok	Toki Pona	1
tol	Tolowa	1
tom	Tombulu	1
ton	Tonga (Tonga Islands)	1
too	Xicotepec De Juárez Totonac	1
top	Papantla Totonac	1
toq	Toposa	1
tor	Togbo-Vara Banda	1
tos	Highland Totonac	1
tou	Tho	1
tov	Upper Taromi	1
tow	Jemez	1
tox	Tobian	1
toy	Topoiyo	1
toz	To	1
tpa	Taupota	1
tpc	Azoyú Me'phaa	1
tpe	Tippera	1
tpf	Tarpia	1
tpg	Kula	1
tpi	Tok Pisin	1
tpj	Tapieté	1
tpk	Tupinikin	1
tpl	Tlacoapa Me'phaa	1
tpm	Tampulma	1
tpn	Tupinambá	1
tpo	Tai Pao	1
tpp	Pisaflores Tepehua	1
tpq	Tukpa	1
tpr	Tuparí	1
tpt	Tlachichilco Tepehua	1
tpu	Tampuan	1
tpv	Tanapag	1
tpx	Acatepec Me'phaa	1
tpy	Trumai	1
tpz	Tinputz	1
tqb	Tembé	1
tql	Lehali	1
tqm	Turumsa	1
tqn	Tenino	1
tqo	Toaripi	1
tqp	Tomoip	1
tqq	Tunni	1
tqr	Torona	1
tqt	Western Totonac	1
tqu	Touo	1
tqw	Tonkawa	1
tra	Tirahi	1
trb	Terebu	1
trc	Copala Triqui	1
trd	Turi	1
tre	East Tarangan	1
trf	Trinidadian Creole English	1
trg	Lishán Didán	1
trh	Turaka	1
tri	Trió	1
trj	Toram	1
trl	Traveller Scottish	1
trm	Tregami	1
trn	Trinitario	1
tro	Tarao Naga	1
trp	Kok Borok	1
trq	San Martín Itunyoso Triqui	1
trr	Taushiro	1
trs	Chicahuaxtla Triqui	1
trt	Tunggare	1
tru	Turoyo	1
trv	Sediq	1
trw	Torwali	1
trx	Tringgus-Sembaan Bidayuh	1
try	Turung	1
trz	Torá	1
tsa	Tsaangi	1
tsb	Tsamai	1
tsc	Tswa	1
tsd	Tsakonian	1
tse	Tunisian Sign Language	1
tsg	Tausug	1
tsh	Tsuvan	1
tsi	Tsimshian	1
tsj	Tshangla	1
tsk	Tseku	1
tsl	Ts'ün-Lao	1
tsm	Turkish Sign Language	1
tsn	Tswana	1
tso	Tsonga	1
tsp	Northern Toussian	1
tsq	Thai Sign Language	1
tsr	Akei	1
tss	Taiwan Sign Language	1
tst	Tondi Songway Kiini	1
tsu	Tsou	1
tsv	Tsogo	1
tsw	Tsishingini	1
tsx	Mubami	1
tsy	Tebul Sign Language	1
tsz	Purepecha	1
tta	Tutelo	1
ttb	Gaa	1
ttc	Tektiteko	1
ttd	Tauade	1
tte	Bwanabwana	1
ttf	Tuotomb	1
ttg	Tutong	1
tth	Upper Ta'oih	1
tti	Tobati	1
ttj	Tooro	1
ttk	Totoro	1
ttl	Totela	1
ttm	Northern Tutchone	1
ttn	Towei	1
tto	Lower Ta'oih	1
ttp	Tombelala	1
ttq	Tawallammat Tamajaq	1
ttr	Tera	1
tts	Northeastern Thai	1
ttt	Muslim Tat	1
ttu	Torau	1
ttv	Titan	1
ttw	Long Wat	1
tty	Sikaritai	1
ttz	Tsum	1
tua	Wiarumus	1
tub	Tübatulabal	1
tuc	Mutu	1
tud	Tuxá	1
tue	Tuyuca	1
tuf	Central Tunebo	1
tug	Tunia	1
tuh	Taulil	1
tui	Tupuri	1
tuj	Tugutil	1
tuk	Turkmen	1
tul	Tula	1
tum	Tumbuka	1
tun	Tunica	1
tuo	Tucano	1
tuq	Tedaga	1
tur	Turkish	1
tus	Tuscarora	1
tuu	Tututni	1
tuv	Turkana	1
tux	Tuxináwa	1
tuy	Tugen	1
tuz	Turka	1
tva	Vaghua	1
tvd	Tsuvadi	1
tve	Te'un	1
tvi	Tulai	1
tvk	Southeast Ambrym	1
tvl	Tuvalu	1
tvm	Tela-Masbuar	1
tvn	Tavoyan	1
tvo	Tidore	1
tvs	Taveta	1
tvt	Tutsa Naga	1
tvu	Tunen	1
tvw	Sedoa	1
tvx	Taivoan	1
tvy	Timor Pidgin	1
twa	Twana	1
twb	Western Tawbuid	1
twc	Teshenawa	1
twd	Twents	1
twe	Tewa (Indonesia)	1
twf	Northern Tiwa	1
twg	Tereweng	1
twh	Tai Dón	1
twi	Twi	1
twl	Tawara	1
twm	Tawang Monpa	1
twn	Twendi	1
two	Tswapong	1
twp	Ere	1
twq	Tasawaq	1
twr	Southwestern Tarahumara	1
twt	Turiwára	1
twu	Termanu	1
tww	Tuwari	1
twx	Tewe	1
twy	Tawoyan	1
txa	Tombonuo	1
txb	Tokharian B	1
txc	Tsetsaut	1
txe	Totoli	1
txg	Tangut	1
txh	Thracian	1
txi	Ikpeng	1
txj	Tarjumo	1
txm	Tomini	1
txn	West Tarangan	1
txo	Toto	1
txq	Tii	1
txr	Tartessian	1
txs	Tonsea	1
txt	Citak	1
txu	Kayapó	1
txx	Tatana	1
txy	Tanosy Malagasy	1
tya	Tauya	1
tye	Kyanga	1
tyh	O'du	1
tyi	Teke-Tsaayi	1
tyj	Tai Do	1
tyl	Thu Lao	1
tyn	Kombai	1
typ	Thaypan	1
tyr	Tai Daeng	1
tys	Tày Sa Pa	1
tyt	Tày Tac	1
tyu	Kua	1
tyv	Tuvinian	1
tyx	Teke-Tyee	1
tyy	Tiyaa	1
tyz	Tày	1
tza	Tanzanian Sign Language	1
tzh	Tzeltal	1
tzj	Tz'utujil	1
tzl	Talossan	1
tzm	Central Atlas Tamazight	1
tzn	Tugun	1
tzo	Tzotzil	1
tzx	Tabriak	1
uam	Uamué	1
uan	Kuan	1
uar	Tairuma	1
uba	Ubang	1
ubi	Ubi	1
ubl	Buhi'non Bikol	1
ubr	Ubir	1
ubu	Umbu-Ungu	1
uby	Ubykh	1
uda	Uda	1
ude	Udihe	1
udg	Muduga	1
udi	Udi	1
udj	Ujir	1
udl	Wuzlam	1
udm	Udmurt	1
udu	Uduk	1
ues	Kioko	1
ufi	Ufim	1
uga	Ugaritic	1
ugb	Kuku-Ugbanh	1
uge	Ughele	1
ugh	Kubachi	1
ugn	Ugandan Sign Language	1
ugo	Ugong	1
ugy	Uruguayan Sign Language	1
uha	Uhami	1
uhn	Damal	1
uig	Uighur	1
uis	Uisai	1
uiv	Iyive	1
uji	Tanjijili	1
uka	Kaburi	1
ukg	Ukuriguma	1
ukh	Ukhwejo	1
uki	Kui (India)	1
ukk	Muak Sa-aak	1
ukl	Ukrainian Sign Language	1
ukp	Ukpe-Bayobiri	1
ukq	Ukwa	1
ukr	Ukrainian	1
uks	Urubú-Kaapor Sign Language	1
uku	Ukue	1
ukv	Kuku	1
ukw	Ukwuani-Aboh-Ndoni	1
uky	Kuuk-Yak	1
ula	Fungwa	1
ulb	Ulukwumi	1
ulc	Ulch	1
ule	Lule	1
ulf	Usku	1
uli	Ulithian	1
ulk	Meriam Mir	1
ull	Ullatan	1
ulm	Ulumanda'	1
uln	Unserdeutsch	1
ulu	Uma' Lung	1
ulw	Ulwa	1
uly	Buli	1
uma	Umatilla	1
umb	Umbundu	1
umc	Marrucinian	1
umd	Umbindhamu	1
umg	Morrobalama	1
umi	Ukit	1
umm	Umon	1
umn	Makyan Naga	1
umo	Umotína	1
ump	Umpila	1
umr	Umbugarla	1
ums	Pendau	1
umu	Munsee	1
una	North Watut	1
und	Undetermined	1
une	Uneme	1
ung	Ngarinyin	1
uni	Uni	1
unk	Enawené-Nawé	1
unm	Unami	1
unn	Kurnai	1
unr	Mundari	1
unu	Unubahe	1
unx	Munda	1
unz	Unde Kaili	1
uon	Kulon	1
upi	Umeda	1
upv	Uripiv-Wala-Rano-Atchin	1
ura	Urarina	1
urb	Urubú-Kaapor	1
urc	Urningangg	1
urd	Urdu	1
ure	Uru	1
urf	Uradhi	1
urg	Urigina	1
urh	Urhobo	1
uri	Urim	1
urk	Urak Lawoi'	1
url	Urali	1
urm	Urapmin	1
urn	Uruangnirin	1
uro	Ura (Papua New Guinea)	1
urp	Uru-Pa-In	1
urr	Lehalurup	1
urt	Urat	1
uru	Urumi	1
urv	Uruava	1
urw	Sop	1
urx	Urimo	1
ury	Orya	1
urz	Uru-Eu-Wau-Wau	1
usa	Usarufa	1
ush	Ushojo	1
usi	Usui	1
usk	Usaghade	1
usp	Uspanteco	1
uss	us-Saare	1
usu	Uya	1
uta	Otank	1
ute	Ute-Southern Paiute	1
uth	ut-Hun	1
utp	Amba (Solomon Islands)	1
utr	Etulo	1
utu	Utu	1
uum	Urum	1
uur	Ura (Vanuatu)	1
uuu	U	1
uve	West Uvean	1
uvh	Uri	1
uvl	Lote	1
uwa	Kuku-Uwanh	1
uya	Doko-Uyanga	1
uzb	Uzbek	1
uzn	Northern Uzbek	1
uzs	Southern Uzbek	1
vaa	Vaagri Booli	1
vae	Vale	1
vaf	Vafsi	1
vag	Vagla	1
vah	Varhadi-Nagpuri	1
vai	Vai	1
vaj	Sekele	1
val	Vehes	1
vam	Vanimo	1
van	Valman	1
vao	Vao	1
vap	Vaiphei	1
var	Huarijio	1
vas	Vasavi	1
vau	Vanuma	1
vav	Varli	1
vay	Wayu	1
vbb	Southeast Babar	1
vbk	Southwestern Bontok	1
vec	Venetian	1
ved	Veddah	1
vel	Veluws	1
vem	Vemgo-Mabas	1
ven	Venda	1
veo	Ventureño	1
vep	Veps	1
ver	Mom Jango	1
vgr	Vaghri	1
vgt	Vlaamse Gebarentaal	1
vic	Virgin Islands Creole English	1
vid	Vidunda	1
vie	Vietnamese	1
vif	Vili	1
vig	Viemo	1
vil	Vilela	1
vin	Vinza	1
vis	Vishavan	1
vit	Viti	1
viv	Iduna	1
vjk	Bajjika	1
vka	Kariyarra	1
vkj	Kujarge	1
vkk	Kaur	1
vkl	Kulisusu	1
vkm	Kamakan	1
vkn	Koro Nulu	1
vko	Kodeoha	1
vkp	Korlai Creole Portuguese	1
vkt	Tenggarong Kutai Malay	1
vku	Kurrama	1
vkz	Koro Zuba	1
vlp	Valpei	1
vls	Vlaams	1
vma	Martuyhunira	1
vmb	Barbaram	1
vmc	Juxtlahuaca Mixtec	1
vmd	Mudu Koraga	1
vme	East Masela	1
vmf	Mainfränkisch	1
vmg	Lungalunga	1
vmh	Maraghei	1
vmi	Miwa	1
vmj	Ixtayutla Mixtec	1
vmk	Makhuwa-Shirima	1
vml	Malgana	1
vmm	Mitlatongo Mixtec	1
vmp	Soyaltepec Mazatec	1
vmq	Soyaltepec Mixtec	1
vmr	Marenje	1
vms	Moksela	1
vmu	Muluridyi	1
vmv	Valley Maidu	1
vmw	Makhuwa	1
vmx	Tamazola Mixtec	1
vmy	Ayautla Mazatec	1
vmz	Mazatlán Mazatec	1
vnk	Vano	1
vnm	Vinmavis	1
vnp	Vunapu	1
vol	Volapük	1
vor	Voro	1
vot	Votic	1
vra	Vera'a	1
vro	Võro	1
vrs	Varisi	1
vrt	Burmbar	1
vsi	Moldova Sign Language	1
vsl	Venezuelan Sign Language	1
vsv	Valencian Sign Language	1
vto	Vitou	1
vum	Vumbu	1
vun	Vunjo	1
vut	Vute	1
vwa	Awa (China)	1
waa	Walla Walla	1
wab	Wab	1
wac	Wasco-Wishram	1
wad	Wamesa	1
wae	Walser	1
waf	Wakoná	1
wag	Wa'ema	1
wah	Watubela	1
wai	Wares	1
waj	Waffa	1
wal	Wolaytta	1
wam	Wampanoag	1
wan	Wan	1
wao	Wappo	1
wap	Wapishana	1
waq	Wagiman	1
war	Waray (Philippines)	1
was	Washo	1
wat	Kaninuwa	1
wau	Waurá	1
wav	Waka	1
waw	Waiwai	1
wax	Watam	1
way	Wayana	1
waz	Wampur	1
wba	Warao	1
wbb	Wabo	1
wbe	Waritai	1
wbf	Wara	1
wbh	Wanda	1
wbi	Vwanji	1
wbj	Alagwa	1
wbk	Waigali	1
wbl	Wakhi	1
wbm	Wa	1
wbp	Warlpiri	1
wbq	Waddar	1
wbr	Wagdi	1
wbs	West Bengal Sign Language	1
wbt	Warnman	1
wbv	Wajarri	1
wbw	Woi	1
wca	Yanomámi	1
wci	Waci Gbe	1
wdd	Wandji	1
wdg	Wadaginam	1
wdj	Wadjiginy	1
wdk	Wadikali	1
wdt	Wendat	1
wdu	Wadjigu	1
wdy	Wadjabangayi	1
wea	Wewaw	1
wec	Wè Western	1
wed	Wedau	1
weg	Wergaia	1
weh	Weh	1
wei	Kiunum	1
wem	Weme Gbe	1
weo	Wemale	1
wep	Westphalien	1
wer	Weri	1
wes	Cameroon Pidgin	1
wet	Perai	1
weu	Rawngtu Chin	1
wew	Wejewa	1
wfg	Yafi	1
wga	Wagaya	1
wgb	Wagawaga	1
wgg	Wangkangurru	1
wgi	Wahgi	1
wgo	Waigeo	1
wgu	Wirangu	1
wgy	Warrgamay	1
wha	Sou Upaa	1
whg	North Wahgi	1
whk	Wahau Kenyah	1
whu	Wahau Kayan	1
wib	Southern Toussian	1
wic	Wichita	1
wie	Wik-Epa	1
wif	Wik-Keyangan	1
wig	Wik Ngathan	1
wih	Wik-Me'anha	1
wii	Minidien	1
wij	Wik-Iiyanh	1
wik	Wikalkan	1
wil	Wilawila	1
wim	Wik-Mungkan	1
win	Ho-Chunk	1
wir	Wiraféd	1
wiu	Wiru	1
wiv	Vitu	1
wiy	Wiyot	1
wja	Waja	1
wji	Warji	1
wka	Kw'adza	1
wkb	Kumbaran	1
wkd	Wakde	1
wkl	Kalanadi	1
wkr	Keerray-Woorroong	1
wku	Kunduvadi	1
wkw	Wakawaka	1
wky	Wangkayutyuru	1
wla	Walio	1
wlc	Mwali Comorian	1
wle	Wolane	1
wlg	Kunbarlang	1
wlh	Welaun	1
wli	Waioli	1
wlk	Wailaki	1
wll	Wali (Sudan)	1
wlm	Middle Welsh	1
wln	Walloon	1
wlo	Wolio	1
wlr	Wailapa	1
wls	Wallisian	1
wlu	Wuliwuli	1
wlv	Wichí Lhamtés Vejoz	1
wlw	Walak	1
wlx	Wali (Ghana)	1
wly	Waling	1
wma	Mawa (Nigeria)	1
wmb	Wambaya	1
wmc	Wamas	1
wmd	Mamaindé	1
wme	Wambule	1
wmg	Western Minyag	1
wmh	Waima'a	1
wmi	Wamin	1
wmm	Maiwa (Indonesia)	1
wmn	Waamwang	1
wmo	Wom (Papua New Guinea)	1
wms	Wambon	1
wmt	Walmajarri	1
wmw	Mwani	1
wmx	Womo	1
wnb	Mokati	1
wnc	Wantoat	1
wnd	Wandarang	1
wne	Waneci	1
wng	Wanggom	1
wni	Ndzwani Comorian	1
wnk	Wanukaka	1
wnm	Wanggamala	1
wnn	Wunumara	1
wno	Wano	1
wnp	Wanap	1
wnu	Usan	1
wnw	Wintu	1
wny	Wanyi	1
woa	Kuwema	1
wob	Wè Northern	1
woc	Wogeo	1
wod	Wolani	1
woe	Woleaian	1
wof	Gambian Wolof	1
wog	Wogamusin	1
woi	Kamang	1
wok	Longto	1
wol	Wolof	1
wom	Wom (Nigeria)	1
won	Wongo	1
woo	Manombai	1
wor	Woria	1
wos	Hanga Hundi	1
wow	Wawonii	1
woy	Weyto	1
wpc	Maco	1
wrb	Waluwarra	1
wrg	Warungu	1
wrh	Wiradjuri	1
wri	Wariyangga	1
wrk	Garrwa	1
wrl	Warlmanpa	1
wrm	Warumungu	1
wrn	Warnang	1
wro	Worrorra	1
wrp	Waropen	1
wrr	Wardaman	1
wrs	Waris	1
wru	Waru	1
wrv	Waruna	1
wrw	Gugu Warra	1
wrx	Wae Rana	1
wry	Merwari	1
wrz	Waray (Australia)	1
wsa	Warembori	1
wsg	Adilabad Gondi	1
wsi	Wusi	1
wsk	Waskia	1
wsr	Owenia	1
wss	Wasa	1
wsu	Wasu	1
wsv	Wotapuri-Katarqalai	1
wtb	Matambwe	1
wtf	Watiwa	1
wth	Wathawurrung	1
wti	Berta	1
wtk	Watakataui	1
wtm	Mewati	1
wtw	Wotu	1
wua	Wikngenchera	1
wub	Wunambal	1
wud	Wudu	1
wuh	Wutunhua	1
wul	Silimo	1
wum	Wumbvu	1
wun	Bungu	1
wur	Wurrugu	1
wut	Wutung	1
wuu	Wu Chinese	1
wuv	Wuvulu-Aua	1
wux	Wulna	1
wuy	Wauyai	1
wwa	Waama	1
wwb	Wakabunga	1
wwo	Wetamut	1
wwr	Warrwa	1
www	Wawa	1
wxa	Waxianghua	1
wxw	Wardandi	1
wyb	Wangaaybuwan-Ngiyambaa	1
wyi	Woiwurrung	1
wym	Wymysorys	1
wyn	Wyandot	1
wyr	Wayoró	1
wyy	Western Fijian	1
xaa	Andalusian Arabic	1
xab	Sambe	1
xac	Kachari	1
xad	Adai	1
xae	Aequian	1
xag	Aghwan	1
xai	Kaimbé	1
xaj	Ararandewára	1
xak	Máku	1
xal	Kalmyk	1
xam	ǀXam	1
xan	Xamtanga	1
xao	Khao	1
xap	Apalachee	1
xaq	Aquitanian	1
xar	Karami	1
xas	Kamas	1
xat	Katawixi	1
xau	Kauwera	1
xav	Xavánte	1
xaw	Kawaiisu	1
xay	Kayan Mahakam	1
xbb	Lower Burdekin	1
xbc	Bactrian	1
xbd	Bindal	1
xbe	Bigambal	1
xbg	Bunganditj	1
xbi	Kombio	1
xbj	Birrpayi	1
xbm	Middle Breton	1
xbn	Kenaboi	1
xbo	Bolgarian	1
xbp	Bibbulman	1
xbr	Kambera	1
xbw	Kambiwá	1
xby	Batjala	1
xcb	Cumbric	1
xcc	Camunic	1
xce	Celtiberian	1
xcg	Cisalpine Gaulish	1
xch	Chemakum	1
xcl	Classical Armenian	1
xcm	Comecrudo	1
xcn	Cotoname	1
xco	Chorasmian	1
xcr	Carian	1
xct	Classical Tibetan	1
xcu	Curonian	1
xcv	Chuvantsy	1
xcw	Coahuilteco	1
xcy	Cayuse	1
xda	Darkinyung	1
xdc	Dacian	1
xdk	Dharuk	1
xdm	Edomite	1
xdo	Kwandu	1
xdq	Kaitag	1
xdy	Malayic Dayak	1
xeb	Eblan	1
xed	Hdi	1
xeg	ǁXegwi	1
xel	Kelo	1
xem	Kembayan	1
xep	Epi-Olmec	1
xer	Xerénte	1
xes	Kesawai	1
xet	Xetá	1
xeu	Keoru-Ahia	1
xfa	Faliscan	1
xga	Galatian	1
xgb	Gbin	1
xgd	Gudang	1
xgf	Gabrielino-Fernandeño	1
xgg	Goreng	1
xgi	Garingbal	1
xgl	Galindan	1
xgm	Dharumbal	1
xgr	Garza	1
xgu	Unggumi	1
xgw	Guwa	1
xha	Harami	1
xhc	Hunnic	1
xhd	Hadrami	1
xhe	Khetrani	1
xhm	Middle Khmer (1400 to 1850 CE)	1
xho	Xhosa	1
xhr	Hernican	1
xht	Hattic	1
xhu	Hurrian	1
xhv	Khua	1
xib	Iberian	1
xii	Xiri	1
xil	Illyrian	1
xin	Xinca	1
xir	Xiriâna	1
xis	Kisan	1
xiv	Indus Valley Language	1
xiy	Xipaya	1
xjb	Minjungbal	1
xjt	Jaitmatang	1
xka	Kalkoti	1
xkb	Northern Nago	1
xkc	Kho'ini	1
xkd	Mendalam Kayan	1
xke	Kereho	1
xkf	Khengkha	1
xkg	Kagoro	1
xki	Kenyan Sign Language	1
xkj	Kajali	1
xkk	Kachok	1
xkl	Mainstream Kenyah	1
xkn	Kayan River Kayan	1
xko	Kiorr	1
xkp	Kabatei	1
xkq	Koroni	1
xkr	Xakriabá	1
xks	Kumbewaha	1
xkt	Kantosi	1
xku	Kaamba	1
xkv	Kgalagadi	1
xkw	Kembra	1
xkx	Karore	1
xky	Uma' Lasan	1
xkz	Kurtokha	1
xla	Kamula	1
xlb	Loup B	1
xlc	Lycian	1
xld	Lydian	1
xle	Lemnian	1
xlg	Ligurian (Ancient)	1
xli	Liburnian	1
xln	Alanic	1
xlo	Loup A	1
xlp	Lepontic	1
xls	Lusitanian	1
xlu	Cuneiform Luwian	1
xly	Elymian	1
xma	Mushungulu	1
xmb	Mbonga	1
xmc	Makhuwa-Marrevone	1
xmd	Mbudum	1
xme	Median	1
xmf	Mingrelian	1
xmg	Mengaka	1
xmh	Kugu-Muminh	1
xmj	Majera	1
xmk	Ancient Macedonian	1
xml	Malaysian Sign Language	1
xmm	Manado Malay	1
xmn	Manichaean Middle Persian	1
xmo	Morerebi	1
xmp	Kuku-Mu'inh	1
xmq	Kuku-Mangk	1
xmr	Meroitic	1
xms	Moroccan Sign Language	1
xmt	Matbat	1
xmu	Kamu	1
xmv	Antankarana Malagasy	1
xmw	Tsimihety Malagasy	1
xmx	Salawati	1
xmy	Mayaguduna	1
xmz	Mori Bawah	1
xna	Ancient North Arabian	1
xnb	Kanakanabu	1
xng	Middle Mongolian	1
xnh	Kuanhua	1
xni	Ngarigu	1
xnj	Ngoni (Tanzania)	1
xnk	Nganakarti	1
xnm	Ngumbarl	1
xnn	Northern Kankanay	1
xno	Anglo-Norman	1
xnq	Ngoni (Mozambique)	1
xnr	Kangri	1
xns	Kanashi	1
xnt	Narragansett	1
xnu	Nukunul	1
xny	Nyiyaparli	1
xnz	Kenzi	1
xoc	O'chi'chi'	1
xod	Kokoda	1
xog	Soga	1
xoi	Kominimung	1
xok	Xokleng	1
xom	Komo (Sudan)	1
xon	Konkomba	1
xoo	Xukurú	1
xop	Kopar	1
xor	Korubo	1
xow	Kowaki	1
xpa	Pirriya	1
xpb	Northeastern Tasmanian	1
xpc	Pecheneg	1
xpd	Oyster Bay Tasmanian	1
xpe	Liberia Kpelle	1
xpf	Southeast Tasmanian	1
xpg	Phrygian	1
xph	North Midlands Tasmanian	1
xpi	Pictish	1
xpj	Mpalitjanh	1
xpk	Kulina Pano	1
xpl	Port Sorell Tasmanian	1
xpm	Pumpokol	1
xpn	Kapinawá	1
xpo	Pochutec	1
xpp	Puyo-Paekche	1
xpq	Mohegan-Pequot	1
xpr	Parthian	1
xps	Pisidian	1
xpt	Punthamara	1
xpu	Punic	1
xpv	Northern Tasmanian	1
xpw	Northwestern Tasmanian	1
xpx	Southwestern Tasmanian	1
xpy	Puyo	1
xpz	Bruny Island Tasmanian	1
xqa	Karakhanid	1
xqt	Qatabanian	1
xra	Krahô	1
xrb	Eastern Karaboro	1
xrd	Gundungurra	1
xre	Kreye	1
xrg	Minang	1
xri	Krikati-Timbira	1
xrm	Armazic	1
xrn	Arin	1
xrr	Raetic	1
xrt	Aranama-Tamique	1
xru	Marriammu	1
xrw	Karawa	1
xsa	Sabaean	1
xsb	Sambal	1
xsc	Scythian	1
xsd	Sidetic	1
xse	Sempan	1
xsh	Shamang	1
xsi	Sio	1
xsj	Subi	1
xsl	South Slavey	1
xsm	Kasem	1
xsn	Sanga (Nigeria)	1
xso	Solano	1
xsp	Silopi	1
xsq	Makhuwa-Saka	1
xsr	Sherpa	1
xsu	Sanumá	1
xsv	Sudovian	1
xsy	Saisiyat	1
xta	Alcozauca Mixtec	1
xtb	Chazumba Mixtec	1
xtc	Katcha-Kadugli-Miri	1
xtd	Diuxi-Tilantongo Mixtec	1
xte	Ketengban	1
xtg	Transalpine Gaulish	1
xth	Yitha Yitha	1
xti	Sinicahua Mixtec	1
xtj	San Juan Teita Mixtec	1
xtl	Tijaltepec Mixtec	1
xtm	Magdalena Peñasco Mixtec	1
xtn	Northern Tlaxiaco Mixtec	1
xto	Tokharian A	1
xtp	San Miguel Piedras Mixtec	1
xtq	Tumshuqese	1
xtr	Early Tripuri	1
xts	Sindihui Mixtec	1
xtt	Tacahua Mixtec	1
xtu	Cuyamecalco Mixtec	1
xtv	Thawa	1
xtw	Tawandê	1
xty	Yoloxochitl Mixtec	1
xua	Alu Kurumba	1
xub	Betta Kurumba	1
xud	Umiida	1
xug	Kunigami	1
xuj	Jennu Kurumba	1
xul	Ngunawal	1
xum	Umbrian	1
xun	Unggaranggu	1
xuo	Kuo	1
xup	Upper Umpqua	1
xur	Urartian	1
xut	Kuthant	1
xuu	Kxoe	1
xve	Venetic	1
xvi	Kamviri	1
xvn	Vandalic	1
xvo	Volscian	1
xvs	Vestinian	1
xwa	Kwaza	1
xwc	Woccon	1
xwd	Wadi Wadi	1
xwe	Xwela Gbe	1
xwg	Kwegu	1
xwj	Wajuk	1
xwk	Wangkumara	1
xwl	Western Xwla Gbe	1
xwo	Written Oirat	1
xwr	Kwerba Mamberamo	1
xwt	Wotjobaluk	1
xww	Wemba Wemba	1
xxb	Boro (Ghana)	1
xxk	Ke'o	1
xxm	Minkin	1
xxr	Koropó	1
xxt	Tambora	1
xya	Yaygir	1
xyb	Yandjibara	1
xyj	Mayi-Yapi	1
xyk	Mayi-Kulan	1
xyl	Yalakalore	1
xyt	Mayi-Thakurti	1
xyy	Yorta Yorta	1
xzh	Zhang-Zhung	1
xzm	Zemgalian	1
xzp	Ancient Zapotec	1
yaa	Yaminahua	1
yab	Yuhup	1
yac	Pass Valley Yali	1
yad	Yagua	1
yae	Pumé	1
yaf	Yaka (Democratic Republic of Congo)	1
yag	Yámana	1
yah	Yazgulyam	1
yai	Yagnobi	1
yaj	Banda-Yangere	1
yak	Yakama	1
yal	Yalunka	1
yam	Yamba	1
yan	Mayangna	1
yao	Yao	1
yap	Yapese	1
yaq	Yaqui	1
yar	Yabarana	1
yas	Nugunu (Cameroon)	1
yat	Yambeta	1
yau	Yuwana	1
yav	Yangben	1
yaw	Yawalapití	1
yax	Yauma	1
yay	Agwagwune	1
yaz	Lokaa	1
yba	Yala	1
ybb	Yemba	1
ybe	West Yugur	1
ybh	Yakha	1
ybi	Yamphu	1
ybj	Hasha	1
ybk	Bokha	1
ybl	Yukuben	1
ybm	Yaben	1
ybn	Yabaâna	1
ybo	Yabong	1
ybx	Yawiyo	1
yby	Yaweyuha	1
ych	Chesu	1
ycl	Lolopo	1
ycn	Yucuna	1
ycp	Chepya	1
ycr	Yilan Creole	1
yda	Yanda	1
ydd	Eastern Yiddish	1
yde	Yangum Dey	1
ydg	Yidgha	1
ydk	Yoidik	1
yea	Ravula	1
yec	Yeniche	1
yee	Yimas	1
yei	Yeni	1
yej	Yevanic	1
yel	Yela	1
yer	Tarok	1
yes	Nyankpa	1
yet	Yetfa	1
yeu	Yerukula	1
yev	Yapunda	1
yey	Yeyi	1
yga	Malyangapa	1
ygi	Yiningayi	1
ygl	Yangum Gel	1
ygm	Yagomi	1
ygp	Gepo	1
ygr	Yagaria	1
ygs	Yolŋu Sign Language	1
ygu	Yugul	1
ygw	Yagwoia	1
yha	Baha Buyang	1
yhd	Judeo-Iraqi Arabic	1
yhl	Hlepho Phowa	1
yhs	Yan-nhaŋu Sign Language	1
yia	Yinggarda	1
yid	Yiddish	1
yif	Ache	1
yig	Wusa Nasu	1
yih	Western Yiddish	1
yii	Yidiny	1
yij	Yindjibarndi	1
yik	Dongshanba Lalo	1
yil	Yindjilandji	1
yim	Yimchungru Naga	1
yin	Riang Lai	1
yip	Pholo	1
yiq	Miqie	1
yir	North Awyu	1
yis	Yis	1
yit	Eastern Lalu	1
yiu	Awu	1
yiv	Northern Nisu	1
yix	Axi Yi	1
yiz	Azhe	1
yka	Yakan	1
ykg	Northern Yukaghir	1
ykh	Khamnigan Mongol	1
yki	Yoke	1
ykk	Yakaikeke	1
ykl	Khlula	1
ykm	Kap	1
ykn	Kua-nsi	1
yko	Yasa	1
ykr	Yekora	1
ykt	Kathu	1
yku	Kuamasi	1
yky	Yakoma	1
yla	Yaul	1
ylb	Yaleba	1
yle	Yele	1
ylg	Yelogu	1
yli	Angguruk Yali	1
yll	Yil	1
ylm	Limi	1
yln	Langnian Buyang	1
ylo	Naluo Yi	1
ylr	Yalarnnga	1
ylu	Aribwaung	1
yly	Nyâlayu	1
ymb	Yambes	1
ymc	Southern Muji	1
ymd	Muda	1
yme	Yameo	1
ymg	Yamongeri	1
ymh	Mili	1
ymi	Moji	1
ymk	Makwe	1
yml	Iamalele	1
ymm	Maay	1
ymn	Yamna	1
ymo	Yangum Mon	1
ymp	Yamap	1
ymq	Qila Muji	1
ymr	Malasar	1
yms	Mysian	1
ymx	Northern Muji	1
ymz	Muzi	1
yna	Aluo	1
ynd	Yandruwandha	1
yne	Lang'e	1
yng	Yango	1
ynk	Naukan Yupik	1
ynl	Yangulam	1
ynn	Yana	1
yno	Yong	1
ynq	Yendang	1
yns	Yansi	1
ynu	Yahuna	1
yob	Yoba	1
yog	Yogad	1
yoi	Yonaguni	1
yok	Yokuts	1
yol	Yola	1
yom	Yombe	1
yon	Yongkom	1
yor	Yoruba	1
yot	Yotti	1
yox	Yoron	1
yoy	Yoy	1
ypa	Phala	1
ypb	Labo Phowa	1
ypg	Phola	1
yph	Phupha	1
ypm	Phuma	1
ypn	Ani Phowa	1
ypo	Alo Phola	1
ypp	Phupa	1
ypz	Phuza	1
yra	Yerakai	1
yrb	Yareba	1
yre	Yaouré	1
yrk	Nenets	1
yrl	Nhengatu	1
yrm	Yirrk-Mel	1
yrn	Yerong	1
yro	Yaroamë	1
yrs	Yarsun	1
yrw	Yarawata	1
yry	Yarluyandi	1
ysc	Yassic	1
ysd	Samatao	1
ysg	Sonaga	1
ysl	Yugoslavian Sign Language	1
ysm	Myanmar Sign Language	1
ysn	Sani	1
yso	Nisi (China)	1
ysp	Southern Lolopo	1
ysr	Sirenik Yupik	1
yss	Yessan-Mayo	1
ysy	Sanie	1
yta	Talu	1
ytl	Tanglang	1
ytp	Thopho	1
ytw	Yout Wam	1
yty	Yatay	1
yua	Yucateco	1
yub	Yugambal	1
yuc	Yuchi	1
yud	Judeo-Tripolitanian Arabic	1
yue	Yue Chinese	1
yuf	Havasupai-Walapai-Yavapai	1
yug	Yug	1
yui	Yurutí	1
yuj	Karkar-Yuri	1
yuk	Yuki	1
yul	Yulu	1
yum	Quechan	1
yun	Bena (Nigeria)	1
yup	Yukpa	1
yuq	Yuqui	1
yur	Yurok	1
yut	Yopno	1
yuw	Yau (Morobe Province)	1
yux	Southern Yukaghir	1
yuy	East Yugur	1
yuz	Yuracare	1
yva	Yawa	1
yvt	Yavitero	1
ywa	Kalou	1
ywg	Yinhawangka	1
ywl	Western Lalu	1
ywn	Yawanawa	1
ywq	Wuding-Luquan Yi	1
ywr	Yawuru	1
ywt	Xishanba Lalo	1
ywu	Wumeng Nasu	1
yww	Yawarawarga	1
yxa	Mayawali	1
yxg	Yagara	1
yxl	Yardliyawarra	1
yxm	Yinwum	1
yxu	Yuyu	1
yxy	Yabula Yabula	1
yyr	Yir Yoront	1
yyu	Yau (Sandaun Province)	1
yyz	Ayizi	1
yzg	E'ma Buyang	1
yzk	Zokhuo	1
zaa	Sierra de Juárez Zapotec	1
zab	Western Tlacolula Valley Zapotec	1
zac	Ocotlán Zapotec	1
zad	Cajonos Zapotec	1
zae	Yareni Zapotec	1
zaf	Ayoquesco Zapotec	1
zag	Zaghawa	1
zah	Zangwal	1
zai	Isthmus Zapotec	1
zaj	Zaramo	1
zak	Zanaki	1
zal	Zauzou	1
zam	Miahuatlán Zapotec	1
zao	Ozolotepec Zapotec	1
zap	Zapotec	1
zaq	Aloápam Zapotec	1
zar	Rincón Zapotec	1
zas	Santo Domingo Albarradas Zapotec	1
zat	Tabaa Zapotec	1
zau	Zangskari	1
zav	Yatzachi Zapotec	1
zaw	Mitla Zapotec	1
zax	Xadani Zapotec	1
zay	Zayse-Zergulla	1
zaz	Zari	1
zba	Balaibalan	1
zbc	Central Berawan	1
zbe	East Berawan	1
zbl	Blissymbols	1
zbt	Batui	1
zbu	Bu (Bauchi State)	1
zbw	West Berawan	1
zca	Coatecas Altas Zapotec	1
zcd	Las Delicias Zapotec	1
zch	Central Hongshuihe Zhuang	1
zdj	Ngazidja Comorian	1
zea	Zeeuws	1
zeg	Zenag	1
zeh	Eastern Hongshuihe Zhuang	1
zem	Zeem	1
zen	Zenaga	1
zga	Kinga	1
zgb	Guibei Zhuang	1
zgh	Standard Moroccan Tamazight	1
zgm	Minz Zhuang	1
zgn	Guibian Zhuang	1
zgr	Magori	1
zha	Zhuang	1
zhb	Zhaba	1
zhd	Dai Zhuang	1
zhi	Zhire	1
zhn	Nong Zhuang	1
zho	Chinese	1
zhw	Zhoa	1
zia	Zia	1
zib	Zimbabwe Sign Language	1
zik	Zimakani	1
zil	Zialo	1
zim	Mesme	1
zin	Zinza	1
ziw	Zigula	1
ziz	Zizilivakan	1
zka	Kaimbulawa	1
zkd	Kadu	1
zkg	Koguryo	1
zkh	Khorezmian	1
zkk	Karankawa	1
zkn	Kanan	1
zko	Kott	1
zkp	São Paulo Kaingáng	1
zkr	Zakhring	1
zkt	Kitan	1
zku	Kaurna	1
zkv	Krevinian	1
zkz	Khazar	1
zla	Zula	1
zlj	Liujiang Zhuang	1
zlm	Malay (individual language)	1
zln	Lianshan Zhuang	1
zlq	Liuqian Zhuang	1
zlu	Zul	1
zma	Manda (Australia)	1
zmb	Zimba	1
zmc	Margany	1
zmd	Maridan	1
zme	Mangerr	1
zmf	Mfinu	1
zmg	Marti Ke	1
zmh	Makolkol	1
zmi	Negeri Sembilan Malay	1
zmj	Maridjabin	1
zmk	Mandandanyi	1
zml	Matngala	1
zmm	Marimanindji	1
zmn	Mbangwe	1
zmo	Molo	1
zmp	Mpuono	1
zmq	Mituku	1
zmr	Maranunggu	1
zms	Mbesa	1
zmt	Maringarr	1
zmu	Muruwari	1
zmv	Mbariman-Gudhinma	1
zmw	Mbo (Democratic Republic of Congo)	1
zmx	Bomitaba	1
zmy	Mariyedi	1
zmz	Mbandja	1
zna	Zan Gula	1
zne	Zande (individual language)	1
zng	Mang	1
znk	Manangkari	1
zns	Mangas	1
zoc	Copainalá Zoque	1
zoh	Chimalapa Zoque	1
zom	Zou	1
zoo	Asunción Mixtepec Zapotec	1
zoq	Tabasco Zoque	1
zor	Rayón Zoque	1
zos	Francisco León Zoque	1
zpa	Lachiguiri Zapotec	1
zpb	Yautepec Zapotec	1
zpc	Choapan Zapotec	1
zpd	Southeastern Ixtlán Zapotec	1
zpe	Petapa Zapotec	1
zpf	San Pedro Quiatoni Zapotec	1
zpg	Guevea De Humboldt Zapotec	1
zph	Totomachapan Zapotec	1
zpi	Santa María Quiegolani Zapotec	1
zpj	Quiavicuzas Zapotec	1
zpk	Tlacolulita Zapotec	1
zpl	Lachixío Zapotec	1
zpm	Mixtepec Zapotec	1
zpn	Santa Inés Yatzechi Zapotec	1
zpo	Amatlán Zapotec	1
zpp	El Alto Zapotec	1
zpq	Zoogocho Zapotec	1
zpr	Santiago Xanica Zapotec	1
zps	Coatlán Zapotec	1
zpt	San Vicente Coatlán Zapotec	1
zpu	Yalálag Zapotec	1
zpv	Chichicapan Zapotec	1
zpw	Zaniza Zapotec	1
zpx	San Baltazar Loxicha Zapotec	1
zpy	Mazaltepec Zapotec	1
zpz	Texmelucan Zapotec	1
zqe	Qiubei Zhuang	1
zra	Kara (Korea)	1
zrg	Mirgan	1
zrn	Zerenkel	1
zro	Záparo	1
zrp	Zarphatic	1
zrs	Mairasi	1
zsa	Sarasira	1
zsk	Kaskean	1
zsl	Zambian Sign Language	1
zsm	Standard Malay	1
zsr	Southern Rincon Zapotec	1
zsu	Sukurum	1
zte	Elotepec Zapotec	1
ztg	Xanaguía Zapotec	1
ztl	Lapaguía-Guivini Zapotec	1
ztm	San Agustín Mixtepec Zapotec	1
ztn	Santa Catarina Albarradas Zapotec	1
ztp	Loxicha Zapotec	1
ztq	Quioquitani-Quierí Zapotec	1
zts	Tilquiapan Zapotec	1
ztt	Tejalapan Zapotec	1
ztu	Güilá Zapotec	1
ztx	Zaachila Zapotec	1
zty	Yatee Zapotec	1
zuh	Tokano	1
zul	Zulu	1
zum	Kumzari	1
zun	Zuni	1
zuy	Zumaya	1
zwa	Zay	1
zxx	No linguistic content	1
zyb	Yongbei Zhuang	1
zyg	Yang Zhuang	1
zyj	Youjiang Zhuang	1
zyn	Yongnan Zhuang	1
zyp	Zyphe Chin	1
zza	Zaza	1
zzj	Zuojiang Zhuang	1
\.


--
-- Data for Name: language_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.language_schema (id, uri) FROM stdin;
1	https://iso639-3.sil.org
\.


--
-- Data for Name: organisation_role; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.organisation_role (schema_id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/contractor.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-organisation.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-research-organisation.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/partner-organisation.json
\.


--
-- Data for Name: organisation_role_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.organisation_role_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1/
\.


--
-- Data for Name: raid; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.raid (handle, service_point_id, url, url_index, primary_title, confidential, metadata_schema, metadata, start_date, date_created, version) FROM stdin;
\.


--
-- Data for Name: raido_operator; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.raido_operator (email) FROM stdin;
matthias.liffers@ardc.edu.au
shawn.ross@ardc.edu.au
rob.leney@ardc.edu.au
steffen.weidenhaus@ardc.edu.au
\.


--
-- Data for Name: related_object_category; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.related_object_category (schema_id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/internal.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json
\.


--
-- Data for Name: related_object_category_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.related_object_category_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1/
\.


--
-- Data for Name: related_object_type; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.related_object_type (schema_id, uri, name, description) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/audiovisual.json	Audiovisual	An audiovisual related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book-chapter.json	Book Chapter	A book chapter related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book.json	Book	A book related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/computational-notebook.json	Computational Notebook	A computational notebook related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-paper.json	Conference Paper	A conference paper related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-poster.json	Conference Poster	A conference poster related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-proceeding.json	Conference Proceeding	A conference proceeding related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/data-paper.json	Data Paper	A data paper related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dataset.json	Dataset	A dataset related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dissertation.json	Dissertation	A dissertation related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/educational-material.json	Educational Material	An educational material related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/event.json	Event	An event related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/funding.json	Funding	A funding related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/image.json	Image	An image related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/instrument.json	Instrument	An instrument related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/journal-article.json	Journal Article	A journal article related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/model.json	Model	A model related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/output-management-plan.json	Output Management Plan	An output management plan related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/physical-object.json	Physical Object	A physical object related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/preprint.json	Preprint	A preprint related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/prize.json	Prize	A Prize related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/report.json	Report	A report related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/service.json	Service	A service related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/software.json	Software	A software related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/sound.json	Sound	A sound related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/standard.json	Standard	A standard related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/text.json	Text	A text related object.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/workflow.json	Workflow	A workflow related object.
\.


--
-- Data for Name: related_object_type_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.related_object_type_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1/
\.


--
-- Data for Name: related_raid_type; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.related_raid_type (schema_id, uri, name, description) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/continues.json	Continues	Continues the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/has-part.json	HasPart	Has part of the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-continued-by.json	IsContinuedBy	Is continued by the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-derived-from.json	IsDerivedFrom	Is derived from the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-identical-to.json	IsIdenticalTo	Is identical to the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-obsoleted-by.json	IsObsoletedBy	Is obsoleted by the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-part-of.json	IsPartOf	Is part of the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-source-of.json	IsSourceOf	Is the source of the related RAiD.
1	https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/obsoletes.json	Obsoletes	Obsoletes the related RAiD.
\.


--
-- Data for Name: related_raid_type_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.related_raid_type_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/related-raid/type/v1/
\.


--
-- Data for Name: service_point; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.service_point (id, name, search_content, admin_email, tech_email, enabled, identifier_owner, app_writes_enabled) FROM stdin;
20000000	raido	\N	web.services@ardc.edu.au	web.services@ardc.edu.au	t	https://ror.org/038sjwq14	t
20000001	Australian Research Data Commons	ardc	matthias.liffers@ardc.edu.au	joel.benn@ardc.edu.au	t	https://ror.org/038sjwq14	t
20000002	RDM@UQ				t	https://ror.org/00rqy9422	t
20000003	University of Notre Dame Library				t	https://ror.org/02stey378	t
20000004	UQ Centre for Advanced Imaging				t	https://ror.org/00rqy9422	t
\.


--
-- Data for Name: subject_type; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.subject_type (id, name, description, note, schema_id) FROM stdin;
4502	Aboriginal and Torres Strait Islander education	This group covers all aspects of education related to Aboriginal and Torres Strait Islander.		1
4503	Aboriginal and Torres Strait Islander environmental knowledges and management	This group covers earth sciences, environmental sciences, knowledges and management (caring for country), agricultural sciences and veterinary sciences related to Aboriginal and Torres Strait Islander.		1
4504	Aboriginal and Torres Strait Islander health and wellbeing	This group covers health and wellbeing (including physical, psychological and spiritual wellbeing), health services and biomedical and clinical sciences related to Aboriginal and Torres Strait Islander.		1
451308	Pacific Peoples history			1
451307	Pacific Peoples ethics			1
4506	Aboriginal and Torres Strait Islander sciences	This group covers mathematical sciences, physical sciences, chemical sciences, biology, engineering, information systems and computing sciences related to Aboriginal and Torres Strait Islander.	a) Broader Indigenous data and data technologies is included in Group 4519.	1
4507	Te ahurea, reo me te hītori o te Māori (Māori culture, language and history)	This group covers culture, languages, history, creative and performing arts, archaeology, built environment and design, philosophy and religion related to Māori.		1
451309	Pacific Peoples land, culture and identity			1
4508	Mātauranga Māori (Māori education)	This group covers all aspects of education related to Māori.		1
510601	Nuclear physics			1
4509	Ngā mātauranga taiao o te Māori (Māori environmental knowledges)	This group covers earth sciences, environmental sciences and knowledges, agricultural sciences and veterinary sciences related to Māori.		1
451304	Pacific Peoples cultural history			1
451303	Pacific Peoples artefacts			1
451306	Pacific Peoples curatorial, archives and museum studies			1
510602	Plasma physics; fusion plasmas; electrical discharges			1
451305	Pacific Peoples culture			1
451302	Conservation of Pacific Peoples heritage			1
451301	Archaeology of New Guinea and Pacific Islands (excl. New Zealand)			1
321299	Ophthalmology and optometry not elsewhere classified			1
350602	Consumer-oriented product or service development			1
350603	Industrial marketing			1
350604	Marketing communications			1
520406	Sensory processes, perception and performance			1
520405	Psycholinguistics (incl. speech production and comprehension)			1
350605	Marketing management (incl. strategy and customer relations)			1
520404	Memory and attention			1
350606	Marketing research methodology			1
520403	Learning, motivation and emotion			1
350607	Marketing technology			1
350608	Marketing theory			1
520402	Decision making			1
350609	Not-for-profit marketing			1
520401	Cognition			1
461103	Deep learning			1
461102	Context learning			1
461101	Adversarial machine learning			1
370699	Geophysics not elsewhere classified			1
461106	Semi- and unsupervised learning			1
4510	Te hauora me te oranga o te Māori (Māori health and wellbeing)	This group covers health and wellbeing (including physical, psychological and spiritual wellbeing), health services and biomedical and clinical sciences related to Māori.		1
461105	Reinforcement learning			1
4511	Ngā tāngata, te porihanga me ngā hapori o te Māori (Māori peoples, society and community)	This group covers human society, economics, commerce, tourism, community governance and decision making, anthropology, human geography, community-based research, architecture and design, and urban planning related to Māori.	a) Māori health policy is included in Group 4512.	1
350601	Consumer behaviour			1
461104	Neural networks			1
360401	Applied theatre			1
360402	Dance and dance studies			1
451319	Pacific Peoples ways of knowing, being and doing			1
451318	Pacific Peoples visual arts and crafts			1
451315	Pacific Peoples religion and religious studies			1
451799	Pacific Peoples sciences not elsewhere classified			1
451314	Pacific Peoples philosophy			1
451317	Pacific Peoples research methods			1
451316	Pacific Peoples repatriation			1
451311	Pacific Peoples literature, journalism and professional writing			1
451310	Pacific Peoples linguistics and languages			1
451313	Pacific Peoples music and performing arts			1
360403	Drama, theatre and performance studies			1
451312	Pacific Peoples media, film, animation and photography			1
370201	Climate change processes			1
370202	Climatology			1
370203	Greenhouse gas inventories and fluxes			1
350610	Pricing (incl. consumer value estimation)			1
350611	Service marketing			1
350612	Social marketing			1
390299	Education policy, sociology and philosophy not elsewhere classified			1
440210	Organised crime			1
3210	Nutrition and dietetics	This group covers nutrition and dietetics.	a) Nutritional content and balance of foods is included in Group 3006 Food sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n b) Metabolism is included in Group 3205 Medical biochemistry and metabolomics.	1
480302	Comparative law			1
480301	Asian and Pacific law			1
480309	Ocean law and governance			1
480307	International humanitarian and human rights law			1
480306	International criminal law			1
480305	International arbitration			1
480304	European Union law			1
470528	Print culture			1
3214	Pharmacology and pharmaceutical sciences	This group covers the preparation, properties, uses and actions of drugs for medical uses. It includes pharmacogenomics.	a) Medicinal chemistry is included in Group 3404 Medicinal and biomolecular chemistry.                                                                                                                                                                                                                                                                                                                                      +\n b) Pharmacology for veterinary use is included in Group 3009 Veterinary sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n c) Dental therapeutics, pharmacology and toxicology are included in Group 3203 Dentistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n d) Nanomedicine is included in Group 3206 Medical biotechnology.	1
420601	Community child health			1
4999	Other mathematical sciences	This group covers any mathematical sciences not elsewhere classified.		1
450418	Aboriginal and Torres Strait Islander remote health			1
470509	Ecocriticism			1
3699	Other creative arts and writing	This group covers studies in creative arts and writing not elsewhere classified.		1
310199	Biochemistry and cell biology not elsewhere classified			1
420604	Injury prevention			1
420605	Preventative health care			1
420602	Health equity			1
420603	Health promotion			1
420606	Social determinants of health			1
450499	Aboriginal and Torres Strait Islander health and wellbeing not elsewhere classified			1
300399	Animal production not elsewhere classified			1
440209	Gender and crime			1
440208	Environmental crime			1
440207	Cybercrime			1
440206	Critical approaches to crime			1
460299	Artificial intelligence not elsewhere classified			1
440205	Criminological theories			1
440204	Crime and social justice			1
440203	Courts and sentencing			1
440202	Correctional theory, offender treatment and rehabilitation			1
440201	Causes and prevention of crime			1
401099	Engineering practice and education not elsewhere classified			1
3203	Dentistry	This group covers dentistry.	a) Biomaterials not solely restricted to dental use are included in Group 4003 Biomedical engineering.	1
3204	Immunology	This group covers the processes and reactions of the human immune system.	a) Specific human disorders or diseases are included in Group 3202 Clinical sciences.	1
3206	Medical biotechnology	This group covers medical biotechnology.	a) Biomedical imaging, medical devices, neural engineering and tissue engineering are included in Group 4003 Biomedical engineering.	1
3207	Medical microbiology	This group covers microbiology associated with human health and disease, other than clinical microbiology.	a) Microbiology unrelated to human medicine is included in Group 3107 Microbiology.                                                                                                                                                                                                                                                                                                                                                                           +\n b) Microbiology related to veterinary practice is included in Group 3009 Veterinary sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n c) Clinical microbiology is included in Group 3202 Clinical sciences.	1
3208	Medical physiology	This group covers the processes and function of the human body or the physical and chemical processes involved in the functioning of the human body and its component parts.	a) Medical physics and biophysics unrelated to humans are included in Group 5199 Other physical sciences.                                                                                                                                                                                                                                                                                     +\n b) Basic animal physiology and comparative physiology are included in Group 3109 Zoology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n c) Physiology related to specific aspects of plant or animal production or veterinary practice is included in the appropriate groups in Division 30 Agricultural, veterinary and food sciences.                                                                                                                                                                                                                                                                                                                                                                                                            +\n d) Exercise physiology is included in Group 4207 Sports science and exercise.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n e) Nutritional physiology is included in Group 3210 Nutrition and dietetics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n f) Physiological psychology is included in Division 52 Psychology.	1
3209	Neurosciences	This group covers processes and reactions of the human nervous system. It includes neurology and neuromuscular disease.	a) Psychiatry and psychotherapy are included in Group 3202 Clinical sciences.                                                                                                                                                                                                                                                                                                                                                                           +\n b) Biological psychology is included in Group 5202 Biological psychology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n c) Motor control is included in Group 4207 Sports science and exercise.	1
440611	Transport geography			1
440610	Social geography			1
460205	Intelligent robotics			1
460204	Fuzzy computation			1
400199	Aerospace engineering not elsewhere classified			1
460203	Evolutionary computation			1
3211	Oncology and carcinogenesis	This group covers oncology and carcinogenesis.	a) Accelerators and other equipment for the production of radiation or radioisotopes for radiation therapy are included in Group 5199 Other physical sciences.                                                                                                                                                                                                                                                                                                                                                     +\n b) Medical genetics other than cancer genetics is included in Group 3202 Clinical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n c) Tumour immunology is included in Group 3204 Immunology.	1
3212	Ophthalmology and optometry	This group covers ophthalmology and optometry.		1
3213	Paediatrics	This group covers paediatrics.	a) Stem cells and tissue engineering are included in Group 3206 Medical biotechnology.	1
3201	Cardiovascular medicine and haematology	This group covers cardiovascular medicine and haematology.	a) Pathology is included in Group 3202 Clinical sciences.	1
4512	Ngā pūtaiao Māori (Māori sciences)	This group covers mathematical, biological, physical, chemical, engineering, and information systems and computing sciences related to Māori.	a) Broader Indigenous data and data technologies is included in Group 4519.	1
4513	Pacific Peoples culture, language and history	This group covers culture, languages, history, creative and performing arts, archaeology, built environment and design, philosophy and religion related to Pacific Peoples.		1
4514	Pacific Peoples education	This group covers all aspects of education related to Pacific Peoples.		1
4515	Pacific Peoples environmental knowledges	This group covers earth sciences, environmental sciences, knowledges and management, agricultural sciences and veterinary sciences related to Pacific Peoples.		1
4518	Pacific Peoples society and community	This group covers mathematical, biological, physical, chemical, engineering, and information systems and computing sciences related to Pacific Peoples.	a) Broader Indigenous data and data technologies is included in Group 4519.	1
4519	Other Indigenous data, methodologies and global Indigenous studies	This group covers Indigenous data, data technologies, Indigenous methodologies and global Indigenous studies.	a) Research methods of Aboriginal and Torres Strait Islander are included in Group 4501, Māori in Group 4511 and Pacific Peoples in Group 4513.                                                                                                                                                                                                                                                              +\n d) Māori data sovereignty is in 4509.	1
440219	White collar crime			1
440218	Victims			1
440217	Terrorism			1
440216	Technology, crime and surveillance			1
440215	State crime			1
440699	Human geography not elsewhere classified			1
440214	Sociological studies of crime			1
440213	Race/ethnicity and crime			1
440212	Private policing and security services			1
440211	Police administration, procedures and practice			1
451708	Pacific Peoples knowledge management methods			1
451707	Pacific Peoples innovation			1
451709	Pacific Peoples mathematical, physical and chemical sciences (excl. astronomy and cosmology)			1
500402	Comparative religious studies			1
451704	Pacific Peoples engineering			1
451703	Pacific Peoples computing technology use and design			1
4902	Mathematical physics	This group covers the mathematical aspects of physics.	a) Combinatorics other than physical combinatorics is included in Group 4904 Pure mathematics.                                                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Development of statistical techniques is included in Group 4905 Statistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n c) Statistical physics is included in Group 5103 Classical physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n d) Statistical mechanics associated with chemical processes is included in 3407 Theoretical and computational chemistry.	1
500401	Christian studies			1
4903	Numerical and computational mathematics	This group covers numerical and computational mathematics.	a) Computational analysis and mathematical software are included in Group 4613 Theory of computation.	1
451706	Pacific Peoples information and knowledge management systems			1
451705	Pacific Peoples genomics			1
4904	Pure mathematics	This group covers pure mathematics.	a) Physical combinatorics is included in Group 4902 Mathematical physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n b) Mathematics associated with computer sciences is included in Division 46 Information and computing sciences.	1
460202	Autonomous agents and multiagent systems			1
4905	Statistics	This group covers statistics. It includes the development of statistical techniques.	a) Statistical mechanics is included in Group 4902 Mathematical physics.                                                                                                                                                                                                                                                                                                                                                                                                                      +\n b) Statistical physics is included in Group 5103 Classical physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n c) Bioinformatics is included in Group 3101 Biochemistry and cell biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Applications of statistical techniques are included in the appropriate division.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n e) Econometrics, economic time series analysis and fields are included in Division 38 Economics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n f) Criminology is included in Group 4402 Criminology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n g) Forensic intelligence and forensic science and management is covered in Group 3503 Business systems in context.	1
520499	Cognitive and computational psychology not elsewhere classified			1
510205	Terahertz physics			1
510204	Photonics, optoelectronics and optical communications			1
451702	Pacific Peoples biological sciences			1
451701	Pacific Peoples astronomy and cosmology			1
321204	Vision science			1
321202	Optical technology			1
321203	Optometry			1
370608	Petrophysics and rock mechanics			1
370609	Seismology and seismic exploration			1
510699	Nuclear and plasma physics not elsewhere classified			1
321201	Ophthalmology			1
370604	Geodynamics			1
370605	Geothermics and radiometrics			1
370606	Gravimetrics			1
370607	Magnetism and palaeomagnetism			1
370601	Applied geophysics			1
370602	Electrical and electromagnetic methods in geophysics			1
370603	Geodesy			1
390201	Education policy			1
390202	History and philosophy of education			1
390203	Sociology of education			1
510201	Atomic and molecular physics			1
510203	Nonlinear optics and spectroscopy			1
510202	Lasers and quantum electronics			1
500406	Studies in eastern religious traditions			1
401006	Systems engineering			1
401005	Risk engineering			1
500405	Religion, society and culture			1
401004	Humanitarian engineering			1
500404	Jewish studies			1
500403	Islamic studies			1
401003	Engineering practice			1
401002	Engineering education			1
401001	Engineering design			1
500407	Studies in religious traditions (excl. Eastern, Jewish, Christian and Islamic traditions)			1
310113	Synthetic biology			1
310112	Structural biology (incl. macromolecular modelling)			1
310599	Genetics not elsewhere classified			1
310111	Signal transduction			1
310110	Receptors and membrane biology			1
450417	Aboriginal and Torres Strait Islander public health and wellbeing			1
450416	Aboriginal and Torres Strait Islander psychology			1
450419	Aboriginal and Torres Strait Islander social determinants of health			1
450412	Aboriginal and Torres Strait Islander men’s health and wellbeing			1
450415	Aboriginal and Torres Strait Islander nursing			1
450414	Aboriginal and Torres Strait Islander mothers and babies health and wellbeing			1
450411	Aboriginal and Torres Strait Islander medicine and treatments			1
320399	Dentistry not elsewhere classified			1
450410	Aboriginal and Torres Strait Islander lifecourse			1
300303	Animal nutrition			1
300302	Animal management			1
440609	Rural and regional geography			1
300305	Animal reproduction and breeding			1
300304	Animal protection (incl. pests and pathogens)			1
440608	Recreation, leisure and tourism geography			1
440607	Population geography			1
300307	Environmental studies in animal production			1
440606	Political geography			1
300306	Animal welfare			1
440605	Health geography			1
440604	Environmental geography			1
460212	Speech recognition			1
440603	Economic geography			1
440602	Development geography			1
310109	Proteomics and intermolecular interactions (excl. medical proteomics)			1
460211	Speech production			1
440601	Cultural geography			1
310108	Protein trafficking			1
460210	Satisfiability and optimisation			1
401499	Manufacturing engineering not elsewhere classified			1
310107	Glycobiology			1
310106	Enzymes			1
310105	Cellular interactions (incl. adhesion, matrix, cell wall)			1
460699	Distributed computing and systems software not elsewhere classified			1
310104	Cell neurochemistry			1
310103	Cell metabolism			1
330199	Architecture not elsewhere classified			1
3602	Creative and professional writing	This group covers professional and creative writing.	a) Journalism is included in 4701 Communication and media studies.	1
3603	Music	This group covers music.	a) Song lyrics and libretti is included in Group 3602 Creative and professional writing.                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n b) Music therapy is included in Group 4201 Allied health and rehabilitation science.	1
470503	Book history			1
470502	Australian literature (excl. Aboriginal and Torres Strait Islander literature)			1
470505	Central and Eastern European literature (incl. Russian)			1
470504	British and Irish literature			1
470507	Comparative and transnational literature			1
470506	Children's literature			1
490599	Statistics not elsewhere classified			1
470501	African literature			1
3604	Performing arts	This group covers performing arts.	a) Film, television and digital media are included in Group 3605 Screen and digital media.                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Performance and installation art are included in Group 3606 Visual arts.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n c) Literary studies are included in Group 4705 Literary studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n d) Music therapy is included in Group 4201 Allied health and rehabilitation science.	1
3605	Screen and digital media	This group covers screen and digital media.	a) Computer software is included in Group 4612 Software engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n b) Communications technologies are included in Group 4006 Communications engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n c) Lens-based practice, including photography, is included in Group 3606 Visual arts.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n d) Creative writing including scriptwriting and screen writing is included in Group 3602 Creative and professional writing.	1
3606	Visual arts	This group covers visual arts.	a) Film, television and digital media are included in Group 3605 Screen and digital media.                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n b) Drama theatre and performance studies are included in Group 3604 Performing Arts.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n c) Art history, theory and criticism are included in Group 3601 Art history, theory and criticism.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n d) Museum Studies are included in Group 4302 Heritage, archive and museum studies.	1
450423	Aboriginal and Torres Strait Islander youth and family social and emotional wellbeing			1
450420	Aboriginal and Torres Strait Islander social, emotional, cultural and spiritual wellbeing			1
470499	Linguistics not elsewhere classified			1
450422	Aboriginal and Torres Strait Islander theory of change models for health			1
450421	Aboriginal and Torres Strait Islander sport and physical activity			1
480299	Environmental and resources law not elsewhere classified			1
300799	Forestry sciences not elsewhere classified			1
310114	Systems biology			1
310102	Cell development, proliferation and death			1
450409	Aboriginal and Torres Strait Islander health services			1
310101	Analytical biochemistry			1
450406	Aboriginal and Torres Strait Islander epidemiology			1
450405	Aboriginal and Torres Strait Islander diet and nutrition			1
450408	Aboriginal and Torres Strait Islander health promotion			1
450407	Aboriginal and Torres Strait Islander health policy			1
450402	Aboriginal and Torres Strait Islander biomedical and clinical sciences			1
450401	Aboriginal and Torres Strait Islander and disability			1
450404	Aboriginal and Torres Strait Islander cultural determinants of health			1
450403	Aboriginal and Torres Strait Islander child health and wellbeing			1
300301	Animal growth and development			1
460209	Planning and decision making			1
460208	Natural language processing			1
460207	Modelling and simulation			1
460206	Knowledge representation and reasoning			1
460201	Artificial life and complex adaptive systems			1
440612	Urban geography			1
480303	Conflict of laws (incl. private international law)			1
450904	Ngā mātauranga taiao o te Māori (Māori environmental knowledges)			1
450907	Te pūtaiao taiao moana o te Māori (Māori marine environment science)			1
450906	Te whakahaere whenua me te wai o te Māori (Māori land and water management)			1
450901	Te ahuwhenua me ngā mahi ngahere o te Māori (Māori agriculture and forestry)			1
450903	Te whāomoomo taiao o te Māori (Māori environmental conservation)			1
450902	Ngā pūtaiao-ā-nuku o te Māori (Māori earth sciences)			1
460704	Interactive narrative			1
460703	Entertainment and gaming			1
460702	Computer graphics			1
460701	Computer aided design			1
460708	Virtual and mixed reality			1
460707	Sound and music computing			1
460706	Serious games			1
460705	Procedural content generation			1
401501	Marine engineering			1
469999	Other information and computing sciences not elsewhere classified			1
401505	Special vehicles			1
401504	Ship and platform structures (incl. maritime hydrodynamics)			1
401503	Ocean engineering			1
401502	Naval architecture			1
511001	Accelerators			1
511002	Instruments and techniques			1
511003	Synchrotrons			1
401999	Resources engineering and extractive metallurgy not elsewhere classified			1
340399	Macromolecular and materials chemistry not elsewhere classified			1
350199	Accounting, auditing and accountability not elsewhere classified			1
449901	Studies of Asian society			1
500321	Social and political philosophy			1
310510	Molecular evolution			1
310511	Neurogenetics			1
500320	Psychoanalytic philosophy			1
310999	Zoology not elsewhere classified			1
320799	Medical microbiology not elsewhere classified			1
320311	Prosthodontics			1
320312	Special needs dentistry			1
300701	Agroforestry			1
300703	Forest ecosystems			1
300702	Forest biodiversity			1
300705	Forestry biomass and bioproducts			1
300704	Forest health and pathology			1
400202	Automotive engineering materials			1
500318	Philosophy of specific cultures (incl. comparative philosophy)			1
300707	Forestry management and environment			1
400203	Automotive mechatronics and autonomous systems			1
500317	Philosophy of science (excl. history and philosophy of specific fields)			1
300706	Forestry fire management			1
300709	Tree improvement (incl. selection and breeding)			1
500316	Philosophy of religion			1
500315	Philosophy of mind (excl. cognition)			1
300708	Forestry product quality assessment			1
400201	Automotive combustion and fuel engineering			1
420299	Epidemiology not elsewhere classified			1
500319	Poststructuralism			1
500310	Phenomenology			1
330110	Sustainable architecture			1
310501	Anthropological genetics			1
310502	Cell and nuclear division			1
500314	Philosophy of language			1
310503	Developmental genetics (incl. sex determination)			1
310504	Epigenetics (incl. genome methylation and epigenomics)			1
500313	Philosophy of gender			1
310505	Gene expression (incl. microarray and other genome-wide approaches)			1
320308	Orthodontics and dentofacial orthopaedics			1
500312	Philosophy of cognition			1
400204	Automotive safety engineering			1
320309	Paedodontics			1
400205	Hybrid and electric vehicles and powertrains			1
310506	Gene mapping			1
500311	Philosophical psychology (incl. moral psychology and philosophy of action)			1
310507	Genetic immunology			1
320306	Oral implantology			1
310508	Genome structure and regulation			1
320307	Oral medicine and pathology			1
470530	Stylistics and textual analysis			1
310509	Genomics			1
320304	Endodontics			1
410499	Environmental management not elsewhere classified			1
320305	Oral and maxillofacial surgery			1
320302	Dental materials and equipment			1
320303	Dental therapeutics, pharmacology and toxicology			1
470531	Young adult literature			1
320301	Craniofacial biology			1
300710	Tree nutrition and physiology			1
320310	Periodontics			1
300712	Wood processing			1
300711	Wood fibre processing			1
330109	Landscape architecture			1
500307	Hermeneutics			1
500306	Ethical theory			1
330106	Architecture for disaster relief			1
330107	Architecture management			1
500305	Epistemology			1
500304	Environmental philosophy			1
330108	Interior design			1
330101	Architectural computing and visualisation methods			1
330102	Architectural design			1
500309	Metaphysics			1
330103	Architectural heritage and conservation			1
500308	Logic			1
470525	Other European literature			1
470524	Other Asian literature (excl. South-East Asian)			1
470527	Popular and genre literature			1
470526	Other literatures in English			1
470529	South-East Asian literature (excl. Indonesian)			1
510106	High energy astrophysics and galactic cosmic rays			1
520399	Clinical and health psychology not elsewhere classified			1
510105	General relativity and gravitational waves			1
510108	Solar physics			1
510107	Planetary science (excl. solar system and planetary geology)			1
470521	Middle Eastern literature			1
470520	Literature in Spanish and Portuguese			1
510109	Stellar astronomy and planetary systems			1
470523	North American literature			1
470522	New Zealand literature (excl. Māori literature)			1
510599	Medical and biological physics not elsewhere classified			1
470514	Literary theory			1
490101	Approximation theory and asymptotic methods			1
470513	Latin and classical Greek literature			1
490103	Calculus of variations, mathematical aspects of systems theory and control theory			1
470516	Literature in French			1
490102	Biological mathematics			1
470515	Literature in Chinese			1
470518	Literature in Italian			1
470517	Literature in German			1
470519	Literature in Japanese			1
490109	Theoretical and applied mechanics			1
490108	Operations research			1
490105	Dynamical systems in applications			1
470510	Indian literature			1
490104	Complex systems			1
309999	Other agricultural, veterinary and food sciences not elsewhere classified			1
490107	Mathematical methods and special functions			1
470512	Korean literature			1
490106	Financial mathematics			1
470511	Indonesian literature			1
480311	Space, maritime and aviation law			1
480310	Public international law			1
480799	Public law not elsewhere classified			1
490510	Stochastic analysis and modelling			1
480707	Welfare, insurance, disability and social security law			1
490511	Time series and spatial modelling			1
480706	Privacy and data rights			1
480705	Military law and justice			1
489999	Other law and legal studies not elsewhere classified			1
480704	Migration, asylum and refugee law			1
480703	Domestic human rights law			1
480702	Constitutional law			1
480701	Administrative law			1
451399	Pacific Peoples culture, language and history not elsewhere classified			1
461199	Machine learning not elsewhere classified			1
340308	Supramolecular chemistry			1
3599	Other commerce, management, tourism and services	This group covers commerce, management, tourism and services not elsewhere classified.		1
401905	Mining engineering			1
401904	Mineral processing/beneficiation			1
340309	Theory and design of materials			1
340306	Polymerisation mechanisms			1
401903	Hydrometallurgy			1
360399	Music not elsewhere classified			1
401902	Geomechanics and resources geotechnical engineering			1
340307	Structure and dynamics of materials			1
401901	Electrometallurgy			1
340301	Inorganic materials (incl. nanomaterials)			1
300299	Agriculture, land and farm management not elsewhere classified			1
340304	Optical properties of materials			1
340305	Physical properties of materials			1
401908	Pyrometallurgy			1
401907	Petroleum and reservoir engineering			1
340302	Macromolecular materials			1
340303	Nanochemistry			1
401906	Nuclear engineering (incl. fuel enrichment and waste processing and storage)			1
350107	Sustainability accounting and reporting			1
350108	Taxation accounting			1
370199	Atmospheric sciences not elsewhere classified			1
350101	Accounting theory and standards			1
350102	Auditing and accountability			1
350103	Financial accounting			1
350104	International accounting			1
350105	Management accounting			1
420201	Behavioural epidemiology			1
4006	Communications engineering	This group covers communications engineering.	a) Electronic devices and digital hardware for communication engineering is included in Group 4009 Electronics, sensors and digital hardware.                                                                                                                                                                                                                                                                                                                                                                        +\n b) Molecular and biological materials are included in Group 4016 Materials engineering.	1
3107	Microbiology	This group covers microbiology, including bacteriology, parasitology and virology.	a) Microbial systematics, taxonomy and phylogeny are included in Group 3104 Evolutionary biology.                                                                                                                                                                                                                                                                                                                                                                                             +\n b) Veterinary microbiology is included in Group 3009 Veterinary sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n c) Medical and clinical microbiology are included in Division 32 Biomedical and Clinical Sciences.	1
470319	Other European languages			1
450721	Ngā toi ataata ngā mahi ā-rehe o te Māori (Māori visual arts and crafts)			1
3109	Zoology	This group covers zoology.	a) Palaeontology other than that associated with palaeoecological studies is included in Group 3705 Geology.                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n b) Cell and molecular biology not specific to plants or animals is included in Group 3101 Biochemistry and cell biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Animal ecology and marine ichthyology are included in Group 3103 Ecology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n d) Animal systematics, taxonomy and phylogeny are included in Group 3104 Evolutionary biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n e) The use of live animals for purposes of primary production is included in Group 3003 Animal production.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n f) Fisheries and aquaculture for primary production is included in Group 3005 Fisheries sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n g) The human immune system is included in Group 3204 Immunology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n h) The human nervous system is included in Group 3209 Neurosciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n i) The use of live animals or material from live animals for medical research is included in Group 3299 Other biomedical and clinical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n j) Animal bioethics is included in Group 5001 Applied ethics.	1
340799	Theoretical and computational chemistry not elsewhere classified			1
511099	Synchrotrons and accelerators not elsewhere classified			1
350599	Human resources and industrial relations not elsewhere classified			1
420204	Epidemiological methods			1
420205	Epidemiological modelling			1
420202	Disease surveillance			1
420203	Environmental epidemiology			1
420208	Nutritional epidemiology			1
310910	Animal physiology - systems			1
420209	Occupational epidemiology			1
310911	Animal structure and function			1
420206	Forensic epidemiology			1
310912	Comparative physiology			1
310913	Invertebrate biology			1
420207	Major global burdens of disease			1
310914	Vertebrate biology			1
420210	Social epidemiology			1
4017	Mechanical engineering	This group covers mechanical engineering.	a) Control theory is included in Group 4901 Applied mathematics.                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n b) Energy generation from photovoltaic devices (solar cells) is included in Group 4009 Electronics, sensors and digital hardware.                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n c) Heat and mass transfer operations, fluidisation and fluid mechanics and turbulent flows are included in Group 4012 Fluid mechanics and thermal engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n d) Nanoelectromechanical Systems (NEMS) are included in Group 4018 Nanotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n e) Architectural acoustics is included in Group 3301 Architecture.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n f) Automation, control engineering and autonomous vehicles are included in Group 4007 Control engineering, mechatronics and robotics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n g) Electrical energy generation and storage is included in Group 4008 Electrical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n h) Chemical energy generation and storage is included in Group 4004 Chemical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n i) Automotive combustion and fuel engineering is included in Group 4002 Automotive engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n j) Control engineering, mechatronics and robotics is included in Group 4007 Control engineering, mechatronics and robotics.	1
400606	Satellite communications			1
4018	Nanotechnology	This group covers nanotechnology.	a) Nanobiotechnology is included in Group 3106 Industrial biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n b) Medical biotechnology is included in Group 3206 Medical biotechnology.	1
400607	Signal processing			1
400604	Network engineering			1
4019	Resources engineering and extractive metallurgy	This group covers resources engineering and extractive metallurgy.	a) Earth sciences are included in Division 37 Earth sciences.                                                                                                                                                                                                                                                                                                                                                                                                              +\n b) Geotechnical engineering associated with civil engineering is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Physical metallurgy is included in Group 4016 Materials engineering.	1
400605	Optical fibre communication systems and technologies			1
400602	Data communications			1
400603	Molecular, biological, and multi-scale communications			1
320704	Medical parasitology			1
310901	Animal behaviour			1
320705	Medical virology			1
500399	Philosophy not elsewhere classified			1
400601	Antennas and propagation			1
310902	Animal cell and molecular biology			1
310903	Animal developmental and reproductive biology			1
320702	Medical infection agents (incl. prions)			1
310904	Animal diet and nutrition			1
320703	Medical mycology			1
310905	Animal immunology			1
310906	Animal neurobiology			1
320701	Medical bacteriology			1
310907	Animal physiological ecology			1
310908	Animal physiology - biophysics			1
400608	Wireless communication systems and technologies (incl. microwave and millimetrewave)			1
310909	Animal physiology - cell			1
410405	Environmental rehabilitation and restoration			1
410406	Natural resource management			1
410407	Wildlife and habitat management			1
410401	Conservation and biodiversity			1
410402	Environmental assessment and monitoring			1
410403	Environmental education and extension			1
410404	Environmental management			1
4009	Electronics, sensors and digital hardware	This group covers electronics, sensors and digital hardware.	a) Semiconductor materials are included in Group 4016 Materials engineering.                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Quantum device science is included in Group 5108 Quantum physics.	1
329999	Other biomedical and clinical sciences not elsewhere classified			1
510199	Astronomical sciences not elsewhere classified			1
450720	Ngā tikanga rangahau o te Māori (Māori research methods)			1
470310	Iberian languages			1
470312	Indonesian languages			1
4011	Environmental engineering	This group covers environmental engineering.	a) Hydrology other than agricultural hydrology is included in Group 3707 Hydrology.                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Environmental sciences is included in Division 41 Environmental sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n c) Agricultural hydrology is included in Group 3002 Agriculture, land and farm management.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Protection of infrastructure from climate change and other environmental factors is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n e) Environmental biotechnology, including bioremediation, is included in Group 4103 Environmental biotechnology.	1
4012	Fluid mechanics and thermal engineering	This group covers fluid mechanics and thermal engineering.	a) Hypersonic aerothermodynamics is included in Group 4001 Aerospace engineering.                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Hydrodynamics in maritime engineering applications is included in Group 4015 Maritime engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n c) Thermal processes in chemical energy transformation and combustion is included in Group 4004 Chemical engineering.	1
4013	Geomatic engineering	This group covers geomatic engineering.	a) Geophysics and Geodesy are included in Group 3706 Geophysics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Antennas for use in remote sensing are included in Group 4006 Communication engineering.	1
4014	Manufacturing engineering	This group covers manufacturing engineering.	a) Post-harvest packaging, transportation and storage of agricultural products are included in Division 30 Agricultural, veterinary and food sciences.                                                                                                                                                                                                                                                                                                                                                                 +\n b) Automotive engineering and automotive mechatronics are included in Group 4002 Automotive engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Membrane and separation technologies are included in Group 4004 Chemical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n d) Manufacturing robotics is included in Group 4007 Control engineering, mechatronics and robotics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n e) Food packaging, preservation and safety are included in Group 3006 Food sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n f) Heat and mass transfer operations, fluidisation and fluid mechanics and turbulent flows are included in Group 4012 Fluid mechanics and thermal engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n g) Applications of biosensors to manufacturing are included in Group 3106 Industrial biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n h) Manufacturing robotics and mechatronics, other than their automotive applications are included in Group 4007 Control engineering, mechatronics and robotics.	1
4499	Other human society	This group covers human society not elsewhere classified.		1
4015	Maritime engineering	This group covers maritime engineering.	a) Transport engineering other than aerospace engineering is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Fluidisation, fluid mechanics and turbulent flows are included in Group 4012 Fluid mechanics and thermal engineering.	1
420699	Public health not elsewhere classified			1
490505	Large and complex data theory			1
490504	Forensic evaluation, inference and statistics			1
490507	Spatial statistics			1
490506	Probability theory			1
490501	Applied statistics			1
490503	Computational statistics			1
490502	Biostatistics			1
490509	Statistical theory			1
490508	Statistical data science			1
4001	Aerospace engineering	This group covers aerospace engineering including avionics and aeronautical and astronomical engineering.	a) Space sciences are included in Group 5109 Space sciences.                                                                                                                                                                                                                                                                                                                                                                                                  +\n b) Transport engineering other than aerospace engineering is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) Remote sensing is included in Group 4013 Geomatic engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Heat and mass transfer operations, fluidisation, fluid mechanics and turbulent flows are included in Group 4012 Fluid mechanics and thermal engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n e) Satellite communications are included in Group 4006 Communications engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n f) Aerodynamics is included in 4012 Fluid mechanics and thermal engineering.	1
37	EARTH SCIENCES	This division covers the earth sciences.	a) Indigenous earth sciences are included in Division 45 Indigenous studies.	1
451201	Te mātai arorangi me te mātai tuarangi o te Māori (Māori astronomy and cosmology)			1
510902	Heliophysics and space weather			1
510901	Astrodynamics and space situational awareness			1
38	ECONOMICS	This division covers economics.	a) Indigenous economics is included in Division 45 Indigenous studies.	1
4003	Biomedical engineering	This group covers biomedical engineering.	a) Regenerative medicine and medical biotechnology is included in Group 3206 Medical biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Other material sciences are included in Groups 5104 Condensed matter physics, 3402 Inorganic chemistry, 3403 Macromolecular and materials chemistry, 4016 Materials engineering, and 4019 Resources engineering and extractive metallurgy.                                                                                                                                                                                                                                                                                                                                                              +\n c) Materials used in dentistry are included in Group 3203 Dentistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n d) Medical robotics and biomechatronics are included in Group 4007 Control engineering, mechatronics and robotics.	1
440299	Criminology not elsewhere classified			1
470311	Indian languages			1
470314	Japanese language			1
470313	Italian language			1
300914	Veterinary virology			1
4005	Civil engineering	This group covers civil engineering.	a) Hydrology other than agricultural hydrology is included in Group 3707 Hydrology.                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Agricultural hydrology is included in Group 3002 Agriculture, land and farm management.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Aerospace engineering is included in Group 4001 Aerospace engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n d) Automotive engineering is included in Group 4002 Automotive engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n e) Water treatment processes are included in Group 4004 Chemical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n f) Surveying is included in Group 4013 Geomatic engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n g) Maritime engineering is included in Group 4015 Maritime engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n h) Materials engineering without civil engineering applications is included in Group 4016 Materials engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n i) Geotechnical engineering associated with mining and mineral extraction is included in Group 4019 Resources engineering and extractive metallurgy.                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n j) Agricultural engineering is included in Group 4099 Other engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n k) Building science and techniques including acoustics, lighting, structure, thermal and moisture are included in Group 3302 Building.                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n l) Building science, construction management and quantity surveying are included in Group 3302 Building.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n m) Transport planning is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n n) Mechanical engineering asset management is included in Group 4017 Mechanical engineering.	1
3301	Architecture	This group covers architecture.	a) Construction and structural engineering are included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n b) Building is included in Group 3302 Building.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n c) History and theory of the built environment other than architectural history and theory is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Data visualisation and computational (parametric and generative) design is included in Group 3303 Design.	1
4611	Machine learning	This group covers machine learning and the techniques used by machine learning.	a) Machine learning that is specific to a particular application domain is included in the corresponding code for that domain.                                                                                                                                                                                                                                                                                                                                                               +\n b) Systems software to support efficient execution of machine learning is included in Group 4606 Distributed computing and systems software.	1
4612	Software engineering	This group covers software engineering and technologies and processes associated with the development of computer software.	a) Descriptions of software applications that are not focussed on the structure or development of the software are included in the appropriate codes for the application domain or type of software.	1
4613	Theory of computation	This group covers the theory of computation.	a) Logic not relating to computation is included in Group 4904 Pure mathematics.                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Engineering of quantum computing systems is included in Group 4009 Electronics, sensors and digital hardware.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n c) Physics of quantum computing is included in Group 5108 Quantum physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Numerical computation and mathematical software that is not in a particular domain and not relating to computer algorithms is included in Division 49 Mathematical Sciences.                                                                                                                                                                                                                                                                                                                                                                                                                            +\n e) Numerical computation relating to a particular domain is covered in that domain (e.g. computational chemistry is included in Group 3407 Theoretical and computational chemistry).	1
520301	Clinical neuropsychology			1
440999	Social work not elsewhere classified			1
4601	Applied computing	This group covers computing techniques specific to particular application domains and also topics which combine techniques from different sub-disciplines of computing.	a) Hardware oriented applications in Health or life sciences are included in Group 4003 Biomedical engineering.                                                                                                                                                                                                                                                                                     +\n b) Applications where primary focus is the field of application, rather than the application and evaluation of computing technology are included in the appropriate application codes.	1
4602	Artificial intelligence	This group covers artificial intelligence other than Machine learning and computer vision.	a) Machine learning is included in Group 4611 Machine learning.                                                                                                                                                                                                                                                                                                                                                                                                            +\n b) Vision and imaging are included in Group 4603 Computer vision and multimedia computation.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n c) Robotics hardware is included in Group 4007 Control engineering, mechatronics and robotics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n d) Signal processing is included in Group 4006 Communications engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n e) Fairness, accountability, transparency, trust and ethics of AI systems is included in Group 4608 Human-centred computing.	1
4603	Computer vision and multimedia computation	This group covers computer vision, and methods, systems and technologies for dealing with images, video data, audio and combinations of these.	a) Sound and music processing is included in Group 4607 Graphics, augmented reality and games.                                                                                                                                                                                                                                                                                                      +\n b) Signal processing is included in Group 4006 Communications engineering.	1
4604	Cybersecurity and privacy	This group covers the technology that supports cybersecurity and privacy.	a) Legal aspects of cybersecurity and privacy are included in Group 4604 Cybersecurity and privacy.                                                                                                                                                                                                                                                                                                                                                                                       +\n b) Cybercrime is included in Group 4402 Criminology.	1
300499	Crop and pasture production not elsewhere classified			1
49	MATHEMATICAL SCIENCES	This division covers mathematics, statistics, and mathematical aspects of the physical sciences.	a) Indigenous mathematical sciences are included in Division 45 Indigenous studies.	1
4606	Distributed computing and systems software	This group covers methods and systems for supporting the efficient execution of application software, including those which use multiple computation units.	a) The engineering processes that produce these software components are included in Group 4612 Software engineering.	1
509999	Other philosophy and religious studies not elsewhere classified			1
4607	Graphics, augmented reality and games	This group covers algorithms, methods and systems that generate convincing synthetic imagery and sound, amongst other things to allow user interaction in virtual, augmented and simulated environments (including, but not limited to games, online virtual worlds, real-time military, industrial or medical simulators).	a) CAD/CAM systems for manufacturing are included in Group 4014 Manufacturing engineering.                                                                                                                                  +\n b) Storage and querying of graphics data is included in Group 4605 Data management and data science.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n c) Human interaction focus in media or games is included in Group 4608 Human-centred computing.	1
4609	Information systems	This group covers systems that manage information for organisational purposes.	a) Organisation of information and knowledge resources is included in Group 4610 Library and information studies.                                                                                                                                                                                                                                                                                                                                                                          +\n b) Information retrieval and web search/querying are included in Group 4605 Data management and data science.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n b) Geospatial information systems are included in Group 4013 Geomatic engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n c) Development of computer hardware is included in Group 4009 Electronics, sensors and digital hardware.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n d) Health information systems, including surveillance, where the focus is not on computer science are included in Group 4203 Health services and systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n e) Health information systems with a focus on computer sciences are included in Group 4601 Applied computing.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n f) Business information systems are included in Group 3503 Business systems in context.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n g) Data science for earth science and geospatial data visualisation is included in Group 3704 Geoinformatics and geospatial information systems and geospatial data modelling is included in 4013 Geomatic engineering.	1
450799	Te ahurea, reo, me te hītori o te Māori kāore anō kia whakarōpūtia i wāhi kē (Māori culture, language and history not elsewhere classified)			1
321399	Paediatrics not elsewhere classified			1
460599	Data management and data science not elsewhere classified			1
4610	Library and information studies	This group covers library and information studies.	a) Cheminformatics is included in Group 3404 Medicinal and biomolecular chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                         +\n b) Bioinformatics is included in Group 4601 Applied computing and Group 3102 Bioinformatics and computational biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) Information systems organisation and management is included in Group 4609 Information systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Archival, repository and related studies is included in Group 4302 Heritage, archive and museum studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n e) Information Retrieval and Web Search is included in Group 4605 Data management and data science.	1
470399	Language studies not elsewhere classified			1
480199	Commercial law not elsewhere classified			1
380102	Behavioural economics			1
380103	Economic history			1
380101	Agricultural economics			1
380106	Experimental economics			1
380107	Financial economics			1
380104	Economics of education			1
380105	Environment and resource economics			1
380108	Health economics			1
380109	Industry economics and industrial organisation			1
380110	International economics			1
380113	Public economics - public choice			1
380114	Public economics - publicly provided goods			1
380111	Labour economics			1
380112	Macroeconomics (incl. monetary and fiscal theory)			1
380117	Transport economics			1
380118	Urban and regional economics			1
380115	Public economics - taxation and revenue			1
380116	Tourism economics			1
380119	Welfare economics			1
390399	Education systems not elsewhere classified			1
510906	Space instrumentation			1
420501	Acute care			1
310299	Bioinformatics and computational biology not elsewhere classified			1
510905	Solar system planetary science (excl. planetary geology)			1
420502	Aged care nursing			1
419999	Other environmental sciences not elsewhere classified			1
3799	Other earth sciences	This group covers earth sciences not elsewhere classified.		1
420505	Nursing workforce			1
451209	Ngā tikanga whakahaere mōhiotanga Māori (Māori knowledge management methods)			1
461010	Social and community informatics			1
451208	Te wairua auaha o te Māori (Māori innovation)			1
420506	Sub-acute care			1
420503	Community and primary care			1
420504	Mental health nursing			1
451205	Te mātauranga pūkaha o te Māori (Māori engineering)			1
350709	Organisation and management theory			1
451204	Te mana motuhake o ngā raraunga Māori (Māori data sovereignty)			1
39	EDUCATION	This division covers education.	a) Indigenous education is included in Division 45 Indigenous studies.	1
451207	Ngā pūnaha whakahaere mōhiohio me te mātauranga o te Māori (Māori information and knowledge management systems)			1
451206	Te mātai huingaira o te Māori (Māori genomics)			1
360505	Screen media			1
451203	Te whakamahi me te hoahoa o te hangarau rorohiko o te Māori (Māori computing technology use and design)			1
430310	Global and world history			1
510904	Solar system energetic particles			1
510903	Mesospheric, thermospheric, ionospheric and magnetospheric physics			1
36	CREATIVE ARTS AND WRITING	This division covers the creative arts and writing.	a) Indigenous creative arts and writing are included in Division 45 Indigenous studies.	1
451202	Ngā pūtaiao koiora o te Māori (Māori biological sciences)			1
350701	Corporate governance			1
44	HUMAN SOCIETY	This division covers human society.	a) Indigenous human society is included in Division 45 Indigenous studies.	1
430301	Asian history			1
480599	Legal systems not elsewhere classified			1
5099	Other philosophy and religious studies	This group covers philosophy and religious studies not elsewhere classified.		1
490399	Numerical and computational mathematics not elsewhere classified			1
451620	Pacific Peoples youth and family			1
390301	Continuing and community education			1
390302	Early childhood education			1
390303	Higher education			1
390304	Primary education			1
45	INDIGENOUS STUDIES	This division covers research that significantly:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n . relates to Aboriginal and Torres Strait Islander, Māori, Pacific, and other Indigenous peoples, nations, communities, languages, places, cultures or knowledges and/or                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n . incorporates or utilises Indigenous methodologies/ways of knowing, theories, practice and/or                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n . is undertaken with or by these peoples, nations or communities.		1
350702	Corporate social responsibility			1
430303	Biography			1
350703	Disaster and emergency management			1
42	HEALTH SCIENCES	This division covers health sciences.	a) Indigenous health sciences and services are included in Division 45 Indigenous studies.	1
451210	Ngā pūtaiao pāngarau, ōkiko, matū hoki - hāunga te mātai arorangi me te mātai tuarangi o te Māori (Māori mathematical, physical and chemical sciences - excl. astronomy and cosmology)			1
350704	Entrepreneurship			1
43	HISTORY, HERITAGE AND ARCHAEOLOGY	This division covers history, heritage and archaeology.	a) Indigenous history, heritage and archaeology are included in Division 45 Indigenous studies.	1
461009	Recordkeeping informatics			1
430302	Australian history			1
40	ENGINEERING	This division covers engineering.	a) Indigenous engineering is included in Division 45 Indigenous studies.	1
430305	Classical Greek and Roman history			1
350705	Innovation management			1
430304	British history			1
41	ENVIRONMENTAL SCIENCES	This division covers environmental sciences.	a) Indigenous environmental sciences are included in Division 45 Indigenous studies.	1
350706	International business			1
430307	Environmental history			1
350707	Leadership			1
350708	Not-for-profit business and management			1
430306	Digital history			1
430309	Gender history			1
461004	Information governance, policy and ethics			1
430308	European history (excl. British, classical Greek and Roman)			1
440107	Social and cultural anthropology			1
461003	Human information interaction and retrieval			1
440106	Medical anthropology			1
461002	Human information behaviour			1
440105	Linguistic anthropology			1
461001	Digital curation and preservation			1
440104	Environmental anthropology			1
359999	Other commerce, management, tourism and services not elsewhere classified			1
461008	Organisation of information and knowledge resources			1
370799	Hydrology not elsewhere classified			1
440103	Biological (physical) anthropology			1
461007	Open access			1
461006	Library studies			1
440102	Anthropology of gender and sexuality			1
461005	Informetrics			1
440101	Anthropology of development			1
3302	Building	This group covers building.	a) Civil engineering, including construction materials and engineering, and transport engineering is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Surveying is included in Group 4013 Geomatic engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n c) Project management is included in Group 3507 Strategy, management and organisational behaviour.	1
400903	Digital processor architectures			1
360501	Cinema studies			1
400904	Electronic device and system performance evaluation, testing and simulation			1
3304	Urban and regional planning	This group covers urban and regional planning.	a) Management of parks in natural environments is included in Group 4104 Environmental management.                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Agricultural land use and planning is included in Group 3002 Agriculture, land and farm management.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) Transport engineering is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n d) Architectural history and theory is included in Group 3301 Architecture.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n e) Urban and regional studies other than planning is included in Group 4406 Human geography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n f) Urban policy is included in Group 4407 Policy and administration.	1
400901	Analog electronics and interfaces			1
400902	Digital electronic devices			1
451699	Pacific Peoples health and wellbeing not elsewhere classified			1
360506	Visual effects			1
400909	Photonic and electro-optical devices, sensors and systems (excl. communications)			1
400907	Industrial electronics			1
390305	Professional education and training			1
390306	Secondary education			1
390307	Teacher education and professional development of educators			1
48	LAW AND LEGAL STUDIES	This division covers law and legal studies.	a) Criminology, including policing and correctional theory, is included in Group 4402 Criminology.                                                                                                                                                                                                                                                                                                                                                                                                                            +\n b) Legal ethics and human rights and justice issues are included in Group 5001 Applied ethics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n c) History and philosophy of law and justice is included in Group 5002 History and philosophy of specific fields.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Indigenous law and legal studies are included in Division 45 Indigenous studies.	1
360504	Interactive media			1
451211	Te tukatuka reo noa o te Māori (Māori natural language processing)			1
400908	Microelectronics			1
400905	Electronic instrumentation			1
360503	Digital and electronic media art			1
400906	Electronic sensors			1
47	LANGUAGE, COMMUNICATION AND CULTURE	This division covers language, communication and culture	a) Indigenous language, communication and culture are included in Division 45 Indigenous studies.	1
350712	Production and operations management			1
350713	Project management			1
350714	Public sector organisation and management			1
370301	Exploration geochemistry			1
370302	Inorganic geochemistry			1
350715	Quality management			1
350716	Small business organisation and management			1
52	PSYCHOLOGY	This division covers psychology and cognitive sciences.	a) Indigenous psychology is included in Division 45 Indigenous studies.	1
350717	Stakeholder engagement			1
350718	Strategy			1
50	PHILOSOPHY AND RELIGIOUS STUDIES	This division covers philosophy and religious studies.	a) Indigenous philosophy and religious studies are included in Division 45 Indigenous studies.	1
370303	Isotope geochemistry			1
370304	Organic geochemistry			1
350710	Organisational behaviour			1
350711	Organisational planning and management			1
310201	Bioinformatic methods development			1
410299	Ecological applications not elsewhere classified			1
33	BUILT ENVIRONMENT AND DESIGN	This division covers built environment and design.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • architecture;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n • building;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n • design; and                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n • urban and regional planning.	a) Indigenous built environment and design is included in Division 45 Indigenous studies.	1
34	CHEMICAL SCIENCES	This division covers the chemical sciences.	a) Indigenous chemical sciences are included in Division 45 Indigenous studies.	1
31	BIOLOGICAL SCIENCES	This division covers biological sciences.	a) Indigenous biological sciences are included in Division 45 Indigenous studies.	1
32	BIOMEDICAL AND CLINICAL SCIENCES	This division covers biomedical and clinical science.	a) Indigenous biomedical and clinical sciences are included in Division 45 Indigenous studies.	1
510102	Astronomical instrumentation			1
510101	Astrobiology			1
30	AGRICULTURAL, VETERINARY AND FOOD SCIENCES	This division covers the sciences and technologies supporting agriculture.                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • crop and pasture production;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n • agronomy;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n • horticultural production;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n • animal production;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n • forestry sciences;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n • fisheries sciences;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n • food sciences; and,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n • veterinary sciences.	a) Indigenous agricultural, veterinary and food sciences is included in Division 45 Indigenous studies.	1
510104	Galactic astronomy			1
510103	Cosmology and extragalactic astronomy			1
400499	Chemical engineering not elsewhere classified			1
450718	Te whakapono me te mātai whakapono o te Māori (Māori religion and religious studies)			1
450717	Ngā Kaupapa Māori (Māori projects)			1
450719	Te whakahoki taonga a te Māori ki te kāinga (Māori repatriation)			1
450714	Ngā arapāho, ngā kiriata, te hākoritanga me te hopu whakaahua o te Māori (Māori media, film, animation and photography)			1
500303	Decision theory			1
500302	Critical theory			1
450713	Te mātākōrero, te kawe kōrero me te tuhituhi ngaio o te Māori (Māori literature, journalism and professional writing)			1
450716	Te rapunga whakaaro o te Māori (Māori philosophy)			1
500301	Aesthetics			1
450715	Te puoro me ngā mahi a te rēhia o te Māori (Māori music and performing arts)			1
450710	Te hītori Māori (Māori history)			1
470321	Translation and interpretation studies			1
450712	Te mātai i te reo Māori me te reo Māori (Māori linguistics and languages)			1
470320	South-East Asian languages (excl. Indonesian)			1
450711	Te whenua, ahurea me te tuakiri o te Māori (Māori land, culture and identity)			1
321303	Neonatology			1
321301	Adolescent health			1
321302	Infant and child health			1
460999	Information systems not elsewhere classified			1
460511	Stream and sensor data			1
460510	Recommender systems			1
470316	Latin and classical Greek languages			1
470315	Korean language			1
470318	Other Asian languages (excl. South-East Asian)			1
470317	Middle Eastern languages			1
390308	Technical, further and workplace education			1
450307	Aboriginal and Torres Strait Islander marine environment science			1
450306	Aboriginal and Torres Strait Islander land and water management			1
450303	Aboriginal and Torres Strait Islander environmental conservation			1
450302	Aboriginal and Torres Strait Islander earth sciences			1
450304	Aboriginal and Torres Strait Islander environmental knowledges			1
450301	Aboriginal and Torres Strait Islander agriculture and forestry			1
460102	Applications in health			1
460101	Applications in arts and humanities			1
460106	Spatial data and applications			1
460105	Applications in social sciences and education			1
460104	Applications in physical sciences			1
460103	Applications in life sciences			1
451609	Pacific Peoples medicine and treatments			1
451608	Pacific Peoples life course			1
451605	Pacific Peoples epidemiology			1
451604	Pacific Peoples diet and nutrition			1
451607	Pacific Peoples health promotion			1
451606	Pacific Peoples health policy			1
451601	Pacific Peoples and disability			1
510502	Medical physics			1
510501	Biological physics			1
451603	Pacific Peoples cultural determinants of health			1
451602	Pacific Peoples biomedical and clinical science			1
300411	Fertilisers (incl. application)			1
320499	Immunology not elsewhere classified			1
300410	Crop and pasture waste water use			1
300402	Agro-ecosystem function and prediction			1
300401	Agrochemicals and biocides (incl. application)			1
300404	Crop and pasture biochemistry and physiology			1
440509	Women's studies (incl. girls' studies)			1
300403	Agronomy			1
440508	Transgender studies			1
300405	Crop and pasture biomass and bioproducts			1
440507	Studies of men and masculinities			1
520304	Health psychology			1
520303	Counselling psychology			1
300408	Crop and pasture post harvest technologies (incl. transportation and storage)			1
440506	Sexualities			1
440505	Intersectional studies			1
520302	Clinical psychology			1
300407	Crop and pasture nutrition			1
440504	Gender relations			1
300409	Crop and pasture protection (incl. pests, diseases and weeds)			1
440503	Feminist theory			1
310208	Translational and applied bioinformatics			1
440502	Feminist methodologies			1
310207	Statistical and quantitative genetics			1
440501	Feminist and queer theory			1
310206	Sequence analysis			1
401399	Geomatic engineering not elsewhere classified			1
310205	Proteomics and metabolomics			1
330299	Building not elsewhere classified			1
310204	Genomics and transcriptomics			1
310203	Computational ecology and phylogenetics			1
310202	Biological network analysis			1
451619	Pacific Peoples theory of change models for health			1
451616	Pacific Peoples social determinants of health			1
3702	Climate change science	This group covers climate change science.	a) Climate change impacts and adaptation is included in Group 4101 Climate change impacts and adaptation.	1
3703	Geochemistry	This group covers geochemistry.		1
451615	Pacific Peoples remote health			1
451618	Pacific Peoples sport and physical activity			1
3704	Geoinformatics	This group covers geoinformatics.	a) Information systems are included in Group 4609 Information systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n b) Computer vision is included in Group 4603 Computer vision and multimedia computation.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Ecological, population and community analysis is included in Group 3102 Bioinformatics and computational biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n d) Geospatial and geosystem data visualisation is included in Group 4013 Geomatic engineering.	1
451617	Pacific Peoples social, cultural, emotional and spiritual wellbeing			1
451612	Pacific Peoples nursing			1
439999	Other history, heritage and archaeology not elsewhere classified			1
3706	Geophysics	This group covers geophysics.	a) Geophysical fluid dynamics is included in Group 4012 Fluid mechanics and thermal engineering.	1
3707	Hydrology	This group covers hydrology.	a) Surface water pollution processes and water quality measurement is included in Group 4105 Pollution and contamination.	1
451611	Pacific Peoples mothers and babies health and wellbeing			1
451614	Pacific Peoples public health and wellbeing			1
510999	Space sciences not elsewhere classified			1
450802	Te Whāriki - te mātauranga kōhungahunga Māori (Māori early childhood education)			1
3708	Oceanography	This group covers oceanography.	a) Marine geoscience is included in Group 3705 Geology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Marine ecology is included in Group 3103 Ecology.	1
320407	Innate immunity			1
310605	Industrial microbiology (incl. biofeedstocks)			1
400106	Hypersonic propulsion and hypersonic aerothermodynamics			1
450803	Ngā tikanga mātauranga o te Māori (Māori educational methods)			1
320408	Transplantation immunology			1
390409	Learning sciences			1
451613	Pacific Peoples psychology			1
451610	Pacific Peoples midwifery and paediatrics			1
300413	Pollination biology and systems			1
300412	Organic and low chemical input crop production			1
300899	Horticultural production not elsewhere classified			1
370703	Groundwater hydrology			1
370704	Surface water hydrology			1
370705	Urban hydrology			1
379999	Other earth sciences not elsewhere classified			1
370701	Contaminant hydrology			1
370702	Ecohydrology			1
400999	Electronics, sensors and digital hardware not elsewhere classified			1
400512	Transport engineering			1
400513	Water resources engineering			1
4199	Other environmental sciences	This group covers other environmental sciences not elsewhere classified.		1
420599	Nursing not elsewhere classified			1
340499	Medicinal and biomolecular chemistry not elsewhere classified			1
430399	Historical studies not elsewhere classified			1
350299	Banking, finance and investment not elsewhere classified			1
490499	Pure mathematics not elsewhere classified			1
401402	CAD/CAM systems			1
401401	Additive manufacturing			1
401409	Manufacturing safety and quality			1
401408	Manufacturing processes and technologies (excl. textiles)			1
401407	Manufacturing management			1
401406	Machining			1
500204	History and philosophy of science			1
401405	Machine tools			1
500203	History and philosophy of medicine			1
401404	Industrial engineering			1
500202	History and philosophy of law and justice			1
500201	History and philosophy of engineering and technology			1
401403	Flexible manufacturing systems			1
520299	Biological psychology not elsewhere classified			1
450811	Te mātauranga Māori i roto i te mātauranga (Mātauranga Māori in education)			1
450810	Tō te Māori mātauranga hangarau, mātauranga atu anō, mātauranga haere tonu, me te mātauranga hapori (Māori technical, further, continuing and community education)			1
399999	Other education not elsewhere classified			1
510499	Condensed matter physics not elsewhere classified			1
401413	Textile technology			1
401412	Precision engineering			1
401411	Packaging, storage and transportation (excl. food and agricultural products)			1
401410	Microtechnology			1
379901	Earth system sciences			1
460612	Service oriented computing			1
460611	Performance evaluation			1
460610	Operating systems			1
441099	Sociology not elsewhere classified			1
401899	Nanotechnology not elsewhere classified			1
470411	Sociolinguistics			1
470410	Phonetics and speech science			1
480699	Private law and civil obligations not elsewhere classified			1
470404	Corpus linguistics			1
470403	Computational linguistics			1
470406	Historical, comparative and typological linguistics			1
470405	Discourse and pragmatics			1
470408	Lexicography and semantics			1
470407	Language documentation and description			1
470409	Linguistic structures (incl. phonology, morphology and syntax)			1
320899	Medical physiology not elsewhere classified			1
470402	Child language acquisition			1
470401	Applied linguistics and educational linguistics			1
480203	Environmental law			1
480202	Climate change law			1
480201	Animal law			1
300802	Horticultural crop growth and development			1
300801	Field organic and low chemical input horticulture			1
300804	Horticultural crop protection (incl. pests, diseases and weeds)			1
300803	Horticultural crop improvement (incl. selection and breeding)			1
300806	Post harvest horticultural technologies (incl. transportation and storage)			1
400103	Aircraft performance and flight control systems			1
300805	Oenology and viticulture			1
400104	Avionics			1
400101	Aerospace materials			1
400102	Aerospace structures			1
480204	Mining, energy and natural resources law			1
420199	Allied health and rehabilitation science not elsewhere classified			1
450806	Ngā kura kaupapa Māori (Māori primary education)			1
450805	Te mātauranga reo Māori (Māori language education)			1
450808	Te mātauranga kura tuarua Māori (Māori secondary education)			1
310601	Biocatalysis and enzyme technology			1
450807	Te urupare me te whai wāhi Māori ki te mātauranga (Māori responsiveness and engaged education)			1
400107	Satellite, space vehicle and missile design and testing			1
320409	Tumour immunology			1
310602	Bioprocessing, bioproduction and bioproducts			1
310603	Fermentation			1
450801	Ngā kōhanga reo (Māori curriculum and pedagogy)			1
450804	Te mātauranga kura tuatoru Māori (Māori higher education)			1
310604	Industrial biotechnology diagnostics (incl. biosensors)			1
400105	Flight dynamics			1
320405	Humoural immunology and immunochemistry			1
320406	Immunogenetics (incl. genetic immunology)			1
310607	Nanobiotechnology			1
320403	Autoimmunity			1
410399	Environmental biotechnology not elsewhere classified			1
320404	Cellular immunology			1
320401	Allergy			1
460605	Distributed systems and algorithms			1
460604	Dependable systems			1
460603	Cyberphysical systems and internet of things			1
460602	Concurrent/parallel systems and technologies			1
460609	Networking and communications			1
460608	Mobile computing			1
460607	High performance computing			1
460606	Energy-efficient computing			1
330204	Building information modelling and management			1
500208	History of philosophy			1
400599	Civil engineering not elsewhere classified			1
330205	Building organisational studies			1
500207	History of ideas			1
500206	History and philosophy of the social sciences			1
330206	Building science, technologies and systems			1
500205	History and philosophy of the humanities			1
330207	Quantity surveying			1
460601	Cloud computing			1
450809	Te whai wāhi ākonga me ngā mahi whakaako o te Māori (Māori student engagement and teaching)			1
330201	Automation and technology in building and construction			1
330202	Building construction management and project planning			1
330203	Building industry studies			1
340407	Proteins and peptides			1
4106	Soil sciences	This group covers soil sciences.	a) Environmental chemistry is included in Group 3499 Other chemical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n b) Management of land and soil for agricultural production is included in Group 3002 Agriculture, land and farm management.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n c) Carbon capture engineering is included in Group 4004 Chemical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n d) Environmental biogeochemistry is included in Group 4103 Environmental biotechnology.	1
400912	Quantum engineering systems (incl. computing and communications)			1
340405	Glycoconjugates			1
360499	Performing arts not elsewhere classified			1
400913	Radio frequency engineering			1
340406	Molecular medicine			1
400910	Photovoltaic devices (solar cells)			1
400911	Power electronics			1
340403	Characterisation of biological macromolecules			1
340404	Cheminformatics and quantitative structure-activity relationships			1
340401	Biologically active molecules			1
340402	Biomolecular modelling and design			1
370299	Climate change science not elsewhere classified			1
350206	Insurance studies			1
430323	Transnational history			1
350207	International finance			1
430322	Sub-Saharan African history			1
350208	Investment and risk management			1
350209	Not-for-profit finance and risk			1
350201	Environment and climate finance			1
350202	Finance			1
350203	Financial econometrics			1
350204	Financial institutions (incl. banking)			1
4599	Other Indigenous studies	This group covers other Indigenous studies not elsewhere classified.		1
350205	Household finance and financial literacy			1
529999	Other psychology not elsewhere classified			1
490406	Lie groups, harmonic and Fourier analysis			1
490405	Group theory and generalisations			1
490408	Operator algebras and functional analysis			1
490407	Mathematical logic, set theory, lattices and universal algebra			1
490402	Algebraic and differential geometry			1
490401	Algebra and number theory			1
430321	North American history			1
490404	Combinatorics and discrete mathematics (excl. physical combinatorics)			1
430320	New Zealand history			1
490403	Category theory, k theory, homological algebra			1
430312	Histories of race			1
430311	Historical studies of crime			1
430314	History of religion			1
430313	History of empires, imperialism and colonialism			1
430316	International history			1
490409	Ordinary differential equations, difference equations and dynamical systems			1
430315	History of the pacific			1
430318	Middle Eastern and North African history			1
430317	Latin and South American history			1
430319	Migration history			1
4101	Climate change impacts and adaptation	This group covers climate change impacts and adaptation.	a) Climate change science and processes are included in Group 3702 Climate change science.	1
440599	Gender studies not elsewhere classified			1
350699	Marketing not elsewhere classified			1
300110	Transgenesis			1
450211	Aboriginal and Torres Strait Islander women’s education			1
450210	Aboriginal and Torres Strait Islander student engagement and teaching			1
441002	Environmental sociology			1
441001	Applied sociology, program evaluation and social impact assessment			1
490411	Real and complex functions (incl. several variables)			1
490410	Partial differential equations			1
490412	Topology			1
450399	Aboriginal and Torres Strait Islander environmental knowledges and management not elsewhere classified			1
460199	Applied computing not elsewhere classified			1
401806	Nanomanufacturing			1
401805	Nanofabrication, growth and self assembly			1
401804	Nanoelectronics			1
401803	Nanoelectromechanical systems			1
401802	Molecular and organic electronics			1
401801	Micro- and nanosystems			1
401809	Nanophotonics			1
401808	Nanometrology			1
401807	Nanomaterials			1
5001	Applied ethics	This group covers applied ethics. It included human rights and justice issues.	a) Humane treatment of animals in agriculture, domestic or sporting environments is included in Group 3003 Animal production.                                                                                                                                                                                                                                                                                                                                                                   +\n b) Human rights law is included in Group 4807 Public law.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n c) Ethical theory and environmental philosophy, including environmental ethics, is included in Group 5003 Philosophy.	1
5002	History and philosophy of specific fields	This group covers the history and philosophy of specific concepts or fields of study which defy geographical classification.	a) History of architecture is included in Group 3301 Architecture.                                                                                                                                                                                                                                                                                                                                                     +\n b) History of design is included in Group 3303 Design.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) History of the built environment is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Sociology and social studies of science and technology is included in Group 4410 Sociology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n e) Art history is included in Group 3601 Art history, theory and criticism.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n f) Historical linguistics is included in Group 4704 Linguistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n g) History of peoples, nations or geographic areas is included in Group 4303 Historical studies.	1
5003	Philosophy	This group covers philosophy.	a) Mathematical logic is included in Group 4904 Pure mathematics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Political philosophy is included in Group 4408 Political science.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n c) Applied ethics is included in Group 5001 Applied ethics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n d) History of philosophy and the history and philosophy of specific fields of study are included in Group 5002 History and philosophy of specific fields.	1
441016	Urban sociology and community studies			1
5004	Religious studies	This group covers the study of religion. It includes comparative religious studies.	a) Language studies are included in Group 4703 Language studies.                                                                                                                                                                                                                                                                                                                                                                                                                        +\n b) Philosophy of religion is included in Group 5003 Philosophy.	1
5005	Theology	This group covers theology.		1
441015	Sociology of the life course			1
441014	Sociology of religion			1
441013	Sociology of migration, ethnicity and multiculturalism			1
441012	Sociology of inequalities			1
441011	Sociology of health			1
441010	Sociology of gender			1
401810	Nanoscale characterisation			1
441009	Sociology of family and relationships			1
441008	Sociology of culture			1
441007	Sociology and social studies of science and technology			1
441006	Sociological methodology and research methods			1
3299	Other biomedical and clinical sciences	This group covers biomedical and clinical sciences not elsewhere classified.		1
441005	Social theory			1
441004	Social change			1
441003	Rural sociology			1
420105	Orthoptics			1
440199	Anthropology not elsewhere classified			1
420106	Physiotherapy			1
420103	Music therapy			1
420104	Occupational therapy			1
461099	Library and information studies not elsewhere classified			1
420109	Rehabilitation			1
420107	Podiatry			1
420108	Prosthetics and orthotics			1
410301	Biodiscovery			1
480605	Tort law			1
480604	Property law (excl. intellectual property law)			1
480603	Intellectual property law			1
480602	Equity and trusts law			1
480601	Contract law			1
420110	Speech pathology			1
500299	History and philosophy of specific fields not elsewhere classified			1
400507	Fire safety engineering			1
400508	Infrastructure engineering and asset management			1
400505	Construction materials			1
400506	Earthquake engineering			1
400503	Complex civil systems			1
400504	Construction engineering			1
400501	Architectural engineering			1
320803	Systems physiology			1
400502	Civil geotechnical engineering			1
320801	Cell physiology			1
320802	Human biophysics			1
451299	Ngā pūtaiao Māori kāore anō kia whakarōpūhia i wāhi kē (Māori sciences not elsewhere classified)			1
400509	Structural dynamics			1
410306	Environmental nanotechnology and nanometrology			1
410302	Biological control			1
410303	Bioremediation			1
410304	Environmental biotechnology diagnostics (incl. biosensors)			1
410305	Environmental marine biotechnology			1
400510	Structural engineering			1
400511	Timber engineering			1
420101	Arts therapy			1
420102	Audiology			1
3899	Other economics	This group covers economics not elsewhere classified.		1
420803	Traditional Chinese medicine and treatments			1
420802	Naturopathy			1
420801	Chiropractic			1
450699	Aboriginal and Torres Strait Islander sciences not elsewhere classified			1
450212	Cultural responsiveness and working with Aboriginal and Torres Strait Islander communities education			1
300105	Genetically modified field crops and pasture			1
300104	Genetically modified animals			1
300107	Genetically modified trees			1
300106	Genetically modified horticulture plants			1
300109	Non-genetically modified uses of biotechnology			1
440408	Urban community development			1
300108	Livestock cloning			1
440407	Socio-economic development			1
440406	Rural community development			1
440405	Poverty, inclusivity and wellbeing			1
440404	Political economy and social change			1
440403	Labour, migration and development			1
440402	Humanitarian disasters, conflict and peacebuilding			1
401299	Fluid mechanics and thermal engineering not elsewhere classified			1
440401	Development cooperation			1
460499	Cybersecurity and privacy not elsewhere classified			1
3401	Analytical chemistry	This group covers analytical chemistry.		1
3404	Medicinal and biomolecular chemistry	This group covers medicinal and biomolecular chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n It includes cheminformatics.	a) Biochemistry other than medical biochemistry or biomolecular chemistry is included in Group 3101 Biochemistry and cell biology.                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Pharmacology for animal use is included in Group 3009 Veterinary sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n c) Food chemistry is included in Group 3006 Food sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Medical biochemistry is included in Group 3205 Medical biochemistry and metabolomics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n e) Clinical chemistry is included in Group 3202 Clinical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n f) Pharmacology for human use and human toxicology is included in Group 3214 Pharmacology and pharmaceutical sciences.	1
3405	Organic chemistry	This group covers organic chemistry.	a) Immunological and bioassay methods are included in Group 3401 Analytical chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Medicinal and biomolecular chemistry is included in Group 3404 Medicinal and biomolecular chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n c) Organometallic chemistry is included in Group 3402 Inorganic chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Biochemistry is included in Group 3101 Biochemistry and cell biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n e) Pharmacology for animal use is included in Group 3009 Veterinary sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n f) Food chemistry is included in Group 3006 Food sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n g) Medical biochemistry is included in Group 3205 Medical biochemistry and metabolomics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n h) Clinical chemistry is included in Group 3202 Clinical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n i) Pharmacology for human use and human toxicology is included in Group 3214 Pharmacology and pharmaceutical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n j) Environmental chemistry is included in 3701 Atmospheric sciences.	1
3406	Physical chemistry	This group covers physical chemistry.	a) Other material sciences are included in Groups 5104 Condensed matter physics, 3402 Inorganic chemistry, 3403 Macromolecular and materials chemistry, 4003 Biomedical engineering, 4016 Materials engineering and 4019 Resources engineering and extractive metallurgy.                                                                                                                                                                                                                                                            +\n b) Physical organic chemistry is included in Group 3405 Organic chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Chemical engineering, including catalytic process engineering, is included in Group 4004 Chemical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Mechanical engineering is included in Group 4017 Mechanical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n e) Biocatalysis is included in Group 3106 Industrial biotechnology.	1
3407	Theoretical and computational chemistry	This group covers theoretical chemistry and computational chemistry.	a) Reaction kinetics and dynamics is included in 3406 Physical chemistry.	1
470299	Cultural studies not elsewhere classified			1
320199	Cardiovascular medicine and haematology not elsewhere classified			1
300599	Fisheries sciences not elsewhere classified			1
310399	Ecology not elsewhere classified			1
450208	Aboriginal and Torres Strait Islander primary education			1
450207	Aboriginal and Torres Strait Islander men’s education			1
450209	Aboriginal and Torres Strait Islander secondary education			1
450204	Aboriginal and Torres Strait Islander technical, further, continuing and community education			1
450203	Aboriginal and Torres Strait Islander educational methods			1
450206	Aboriginal and Torres Strait Islander language education			1
499999	Other mathematical sciences not elsewhere classified			1
330308	Fire safety design			1
450205	Aboriginal and Torres Strait Islander higher education			1
300101	Agricultural biotechnology diagnostics (incl. biosensors)			1
321499	Pharmacology and pharmaceutical sciences not elsewhere classified			1
450202	Aboriginal and Torres Strait Islander early childhood education			1
300103	Agricultural molecular engineering of nucleic acids and proteins			1
300102	Agricultural marine biotechnology			1
450201	Aboriginal and Torres Strait Islander curriculum and pedagogy			1
440899	Political science not elsewhere classified			1
4303	Historical studies	This group covers the study of the history of peoples, nations or geographic regions. It includes biography.	a) History of architecture is included in Group 3301 Architecture.                                                                                                                                                                                                                                                                                                                                                                                            +\n b) History of design is included in Group 3303 Design.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) History of the built environment is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Art history is included in Group 3601 Art history, theory and criticism.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n e) History of specific concepts or fields of study (including the sciences, humanities and social sciences) which defy geographical classification other than architecture, design, built environment and art are included in Group 5002 History and philosophy of specific fields.	1
451109	Ngā tikanga Māori (Māori customary law)			1
451106	Te rangahau kei rō hapori o te Māori (Māori community-based research)			1
451105	Te whanaketanga ā-hapori, ā-rohe o te Māori (Māori community and regional development)			1
510801	Degenerate quantum gases and atom optics			1
320103	Respiratory diseases			1
451102	Te mātauranga tikanga Māori (Māori anthropology)			1
510803	Quantum information, computation and communication			1
451101	Te mahi kaute o te Māori (Māori accounting)			1
510802	Foundations of quantum mechanics			1
451104	Ngā mahi tauhokohoko o te Māori (Māori commerce)			1
510805	Quantum technologies			1
451103	Te hoahoanga whare o te Māori (Māori architecture)			1
510804	Quantum optics and quantum optomechanics			1
350801	Impacts of tourism			1
321099	Nutrition and dietetics not elsewhere classified			1
350802	Tourism forecasting			1
451111	Te mātauranga ōhanga o te Māori (Māori economics)			1
451110	Te whakaharatau me te whakahaere hoahoa o te Māori (Māori design practice and management)			1
350803	Tourism management			1
350804	Tourism marketing			1
350805	Tourism resource appraisal			1
350806	Tourist behaviour and visitor experience			1
370899	Oceanography not elsewhere classified			1
451117	Te whakahaere o te Māori (Māori management)			1
451116	Ngā ture Māori (Māori legislation)			1
451119	Ngā hinonga pāpori kaupapa atawhai o te Māori (Māori not-for-profit social enterprises)			1
451118	Te whakamākete o te Māori (Māori marketing)			1
451113	Te mātauranga matawhenua ā-iwi me te tatauranga ā-iwi o te Māori (Māori human geography and demography)			1
360603	Performance art			1
360604	Photography, video and lens-based practice			1
451112	Te ahumoni Māori (Māori finance)			1
451115	Te ture me te tika Māori (Māori law and justice)			1
360601	Crafts			1
451599	Pacific Peoples environmental knowledges not elsewhere classified			1
360602	Fine arts			1
451114	Te ture Whenua (Māori land law)			1
451120	Ngā iwi Māori me te ture (Māori peoples and the law)			1
451121	Ngā tirohanga Māori (Māori perspectives)			1
370401	Computational modelling and simulation in earth sciences			1
461399	Theory of computation not elsewhere classified			1
370402	Earth and space science informatics			1
370403	Geoscience data visualisation			1
4301	Archaeology	This group covers archaeology.	a) Remote sensing and surveying are included in Group 4013 Geomatic engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n b) Anthropology is included in Group 4401 Anthropology.	1
380201	Cross-sectional analysis			1
380202	Econometric and statistical methods			1
380205	Time-series analysis			1
380203	Economic models and forecasting			1
380204	Panel data analysis			1
390499	Specialist studies in education not elsewhere classified			1
310799	Microbiology not elsewhere classified			1
401209	Hydrodynamics and hydraulic engineering			1
401208	Geophysical and environmental fluid flows			1
401207	Fundamental and theoretical fluid dynamics			1
401206	Fluid-structure interaction and aeroacoustics			1
401205	Experimental methods in fluid flow, heat and mass transfer			1
320599	Medical biochemistry and metabolomics not elsewhere classified			1
440809	New Zealand government and politics			1
300501	Aquaculture			1
440808	International relations			1
440807	Government and politics of Asia and the Pacific			1
300503	Fish pests and diseases			1
300502	Aquaculture and fisheries stock assessment			1
460899	Human-centred computing not elsewhere classified			1
440806	Gender and politics			1
300505	Fisheries management			1
440805	Environmental politics			1
440804	Defence studies			1
300504	Fish physiology and genetics			1
440803	Comparative government and politics			1
300506	Post-harvest fisheries technologies (incl. transportation)			1
440802	Citizenship			1
401699	Materials engineering not elsewhere classified			1
340205	Main group metal chemistry			1
440801	Australian government and politics			1
401213	Turbulent flows			1
401212	Non-Newtonian fluid flows (incl. rheology)			1
401211	Multiphysics flows (incl. multiphase and reacting flows)			1
401210	Microfluidics and nanofluidics			1
330399	Design not elsewhere classified			1
310301	Behavioural ecology			1
310302	Community ecology (excl. invasive species ecology)			1
310303	Ecological physiology			1
310304	Freshwater ecology			1
3801	Applied economics	This group covers applied economics.	a) Taxation accounting is included in Group 3501 Accounting, auditing and accountability.                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n b) Economic geography is included in Group 4406 Human geography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n c) Taxation law is included in Group 4801 Commercial law.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n d) International finance is included in Group 3502 Banking, finance and investment.	1
310305	Marine and estuarine ecology (incl. marine ichthyology)			1
3802	Econometrics	This group covers econometrics.	a) Financial econometrics is included in Group 3502 Banking, finance and investment.	1
310306	Palaeoecology			1
310307	Population ecology			1
3803	Economic theory	This group covers economic theory.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n It includes history of economic thought.		1
310308	Terrestrial ecology			1
470211	Migrant cultural studies			1
470210	Globalisation and culture			1
470213	Postcolonial studies			1
470212	Multicultural, intercultural and cross-cultural studies			1
320102	Haematology			1
470214	Screen and media culture			1
300999	Veterinary sciences not elsewhere classified			1
470206	Cultural studies of nation and region			1
470205	Cultural studies of agriculture, food and wine			1
470208	Culture, representation and identity			1
470207	Cultural theory			1
470209	Environment and culture			1
470202	Asian cultural studies			1
470201	Arts and cultural policy			1
470204	Cultural and creative industries			1
470203	Consumption and everyday life			1
320101	Cardiology (incl. cardiovascular diseases)			1
450607	Aboriginal and Torres Strait Islander innovation			1
450609	Aboriginal and Torres Strait Islander mathematical, physical and chemical sciences (excl. astronomy and cosmology)			1
450604	Aboriginal and Torres Strait Islander engineering			1
450603	Aboriginal and Torres Strait Islander computing technology use and design			1
450606	Aboriginal and Torres Strait Islander information and knowledge management systems			1
450605	Aboriginal and Torres Strait Islander genomics			1
410199	Climate change impacts and adaptation not elsewhere classified			1
450602	Aboriginal and Torres Strait Islander biological sciences			1
450601	Aboriginal and Torres Strait Islander astronomy and cosmology			1
460407	System and network security			1
460406	Software and application security			1
460405	Hardware security			1
460404	Digital forensics			1
440811	Political theory and political philosophy			1
440810	Peace studies			1
400399	Biomedical engineering not elsewhere classified			1
460403	Data security and protection			1
460402	Data and information privacy			1
460401	Cryptography			1
4703	Language studies	This group covers language studies. It includes comparative language studies and translation and interpretation studies.	a) Writing is included in Division 36 Creative arts and writing.                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Linguistics is included in Group 4704 Linguistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n c) Literary studies are included in Group 4705 Literary studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n d) Philosophy of language is included in Group 5003 Philosophy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n e) Indigenous linguistics and languages is included in Division 45 Indigenous studies.	1
451506	Pacific Peoples land and water management			1
451505	Pacific Peoples fisheries and customary fisheries			1
4704	Linguistics	This group covers linguistics.	a) Linguistic anthropology is included in Group 4401 Anthropology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Psycholinguistics (incl. speech production and comprehension) are included in Group 5204 Cognitive and computational psychology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n c) Language studies are included in Group 4703 Language studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n d) Philosophy of language is included in Group 5003 Philosophy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n e) Indigenous linguistics and languages are included in Division 45 Indigenous studies.	1
320501	Medical biochemistry - amino acids and metabolites			1
300910	Veterinary pathology			1
410203	Ecosystem function			1
300913	Veterinary urology			1
330307	Ergonomics design			1
4705	Literary studies	This group covers literature and literary studies.	a) Writing is included in Division 36 Creative arts and writing.                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Language studies are included in Group 4703 Language studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n c) Biography is included in Group 4303 Historical studies.	1
451507	Pacific Peoples marine environment science			1
451502	Pacific Peoples earth sciences			1
510403	Condensed matter modelling and density functional theory			1
451501	Pacific Peoples agriculture and forestry			1
510402	Condensed matter imaging			1
451504	Pacific Peoples environmental knowledges			1
510405	Soft condensed matter			1
451503	Pacific Peoples environmental conservation			1
510404	Electronic and magnetic properties of condensed matter; superconductivity			1
321006	Sport and exercise nutrition			1
510407	Surface properties of condensed matter			1
510406	Structural properties of condensed matter			1
321004	Nutritional science			1
321005	Public health nutrition			1
461307	Quantum computation			1
461306	Numerical computation and mathematical software			1
520207	Social and affective neuroscience			1
520206	Psychophysiology			1
520205	Psychopharmacology			1
520204	Evolutionary psychological studies			1
520203	Cognitive neuroscience			1
461301	Coding, information theory and compression			1
461305	Data structures and algorithms			1
461304	Concurrency theory			1
461303	Computational logic and formal languages			1
461302	Computational complexity and computability			1
451999	Other Indigenous data, methodologies and global Indigenous studies not elsewhere classified			1
510899	Quantum physics not elsewhere classified			1
321002	Food properties (incl. characteristics and health benefits)			1
321003	Nutrigenomics and personalised nutrition			1
321001	Clinical nutrition			1
370802	Chemical oceanography			1
370803	Physical oceanography			1
370801	Biological oceanography			1
390408	Learning analytics			1
490299	Mathematical physics not elsewhere classified			1
390402	Education assessment and evaluation			1
390404	Educational counselling			1
390405	Educational technology and computing			1
390406	Gender, sexuality and education			1
390407	Inclusive education			1
520202	Behavioural neuroscience			1
520201	Behavioural genetics			1
340199	Analytical chemistry not elsewhere classified			1
510401	Condensed matter characterisation technique development			1
390410	Multicultural education (excl. Aboriginal and Torres Strait Islander, Māori and Pacific Peoples)			1
390411	Special education and disability			1
390412	Teacher and student wellbeing			1
320999	Neurosciences not elsewhere classified			1
410201	Bioavailability and ecotoxicology			1
410202	Biosecurity science and invasive species ecology			1
430299	Heritage, archive and museum studies not elsewhere classified			1
300902	Veterinary anatomy and physiology			1
300903	Veterinary bacteriology			1
300901	Veterinary anaesthesiology and intensive care			1
300906	Veterinary immunology			1
330314	Sustainable design			1
300907	Veterinary medicine (excl. urology)			1
400401	Carbon capture engineering (excl. sequestration)			1
330315	Textile and fashion design			1
300904	Veterinary diagnosis and diagnostics			1
300905	Veterinary epidemiology			1
330310	Interaction and experience design			1
330311	Models and simulations of design			1
330312	Service design			1
300908	Veterinary mycology			1
330313	Social design			1
300909	Veterinary parasitology			1
400408	Reaction engineering (excl. nuclear reactions)			1
400409	Separation technologies			1
400406	Powder and particle technology			1
400407	Process control and simulation			1
310701	Bacteriology			1
400404	Electrochemical energy storage and conversion			1
310702	Infectious agents			1
400405	Food engineering			1
500599	Theology not elsewhere classified			1
400402	Chemical and thermal processes in energy and combustion			1
310703	Microbial ecology			1
320507	Metabolic medicine			1
400403	Chemical engineering design			1
310704	Microbial genetics			1
310705	Mycology			1
320504	Medical biochemistry - lipids			1
310706	Virology			1
320505	Medical biochemistry - nucleic acids			1
320502	Medical biochemistry - carbohydrates			1
320503	Medical biochemistry - inorganic elements and compounds			1
410204	Ecosystem services (incl. pollination)			1
330309	Industrial and product design			1
410205	Fire ecology			1
300911	Veterinary pharmacology			1
410206	Landscape ecology			1
300912	Veterinary surgery			1
330303	Design for disaster relief			1
400411	Water treatment processes			1
330304	Design history, theory and criticism			1
330305	Design management			1
500107	Professional ethics			1
500106	Medical ethics			1
330306	Design practice and methods			1
400410	Wastewater treatment processes			1
330302	Design anthropology			1
400899	Electrical engineering not elsewhere classified			1
520199	Applied and developmental psychology not elsewhere classified			1
460912	Knowledge and information management			1
460911	Inter-organisational, extra-organisational and global information systems			1
460910	Information systems user experience design and development			1
510399	Classical physics not elsewhere classified			1
410699	Soil sciences not elsewhere classified			1
319999	Other biological sciences not elsewhere classified			1
4299	Other health sciences	This group covers other health sciences not elsewhere classified.		1
420499	Midwifery not elsewhere classified			1
470305	Early English languages			1
470304	Comparative language studies			1
470307	English language			1
470306	English as a second language			1
470309	German language			1
470308	French language			1
321406	Pharmacogenomics			1
321407	Toxicology (incl. clinical toxicology)			1
321404	Pharmaceutical delivery technologies			1
321405	Pharmaceutical sciences			1
470301	African languages			1
321402	Clinical pharmacology and therapeutics			1
321403	Clinical pharmacy and pharmacy practice			1
470303	Chinese languages			1
321401	Basic pharmacology			1
470302	Central and Eastern European languages (incl. Russian)			1
480104	Labour law			1
480103	Corporations and associations law			1
480102	Commercial law			1
480101	Banking, finance and securities law			1
480106	Taxation law			1
480105	Not-for-profit law			1
450707	Te ahurea Māori (Māori culture)			1
450706	Te hītori ahurea Māori (Māori cultural history)			1
450703	Te mana tāne (male status)			1
450702	Te mana wahine (female status)			1
450705	Ngā taonga Māori nō mua (Māori artefacts)			1
450704	Te mātai whaipara Māori (Māori archaeology)			1
450701	Te whāomoomo i te tuku ihotanga Māori (conservation of Māori heritage)			1
460506	Graph, social and multimedia data			1
460505	Database systems			1
460504	Data quality			1
460503	Data models, storage and indexing			1
460509	Query processing and optimisation			1
460508	Information retrieval and web search			1
460507	Information extraction and fusion			1
401303	Navigation and position fixing			1
401302	Geospatial information systems and geospatial data modelling			1
5199	Other physical sciences	This group covers any physical sciences not elsewhere classified.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n It includes complex physical systems.		1
401301	Cartography and digital mapping			1
460502	Data mining and knowledge discovery			1
460501	Data engineering and data science			1
500101	Bioethics			1
340108	Sensor technology (incl. chemical aspects)			1
340109	Separation science			1
500105	Legal ethics			1
360199	Art history, theory and criticism not elsewhere classified			1
500104	Human rights and justice issues (excl. law)			1
401306	Surveying (incl. hydrographic surveying)			1
500103	Ethical use of new technology			1
401305	Satellite-based positioning			1
401304	Photogrammetry and remote sensing			1
500102	Business ethics			1
340102	Bioassays			1
340103	Electroanalytical chemistry			1
340101	Analytical spectrometry			1
340106	Metabolomic chemistry			1
340107	Quality assurance, chemometrics, traceability and metrological chemistry			1
340104	Flow analysis			1
340105	Instrumental methods (excl. immunological and bioassay methods)			1
440903	Social program evaluation			1
440902	Counselling, wellbeing and community services			1
440901	Clinical social work practice			1
401799	Mechanical engineering not elsewhere classified			1
340599	Organic chemistry not elsewhere classified			1
350399	Business systems in context not elsewhere classified			1
420402	Models of care and place of birth			1
420403	Psychosocial aspects of childbirth and perinatal mental health			1
420401	Clinical midwifery			1
430202	Critical heritage, museum and archive studies			1
430201	Archival, repository and related studies			1
430204	Digital heritage			1
430203	Cultural heritage management (incl. world heritage)			1
430206	Heritage collections and interpretations			1
430205	Heritage and cultural conservation			1
430208	Intangible heritage			1
430207	Heritage tourism, visitor and audience studies			1
430209	Materials conservation			1
400804	Electrical energy storage			1
400805	Electrical energy transmission, networks and systems			1
500199	Applied ethics not elsewhere classified			1
320906	Peripheral nervous system			1
400802	Electrical circuits and systems			1
320907	Sensory systems			1
400803	Electrical energy generation (incl. renewables, excl. photovoltaics)			1
320904	Computational neuroscience (incl. mathematical neuroscience and theoretical neuroscience)			1
400801	Circuits and systems			1
320905	Neurology and neuromuscular diseases			1
320902	Cellular nervous system			1
320903	Central nervous system			1
320901	Autonomic nervous system			1
400808	Photovoltaic power systems			1
400806	Electrical machines and drives			1
400807	Engineering electromagnetics			1
410603	Soil biology			1
410604	Soil chemistry and soil carbon sequestration (excl. carbon sequestration science)			1
410605	Soil physics			1
410601	Land capability and soil productivity			1
410602	Pedology and pedometrics			1
4204	Midwifery	This group covers midwifery.	a) Medicine, nursing and health curriculum and pedagogy is included in Group 3901 Curriculum and pedagogy.	1
4207	Sports science and exercise	This group covers sports science and exercise.		1
4208	Traditional, complementary and integrative medicine	This group covers traditional, complementary and integrative medicine.	a) Indigenous traditional medicine is included in Division 45 Indigenous studies.	1
451128	Te mātauranga pāpori o te Māori (Māori sociology)			1
451127	Ngā mātai tikanga ā-iwi o te Māori (Māori sociological studies)			1
451129	Te mahi tāpoi Māori (Māori tourism)			1
451123	Ngā mahi tōrangapū Māori (Māori politics)			1
451125	Te pāpātanga pāpori me te aromātai hōtaka o te Māori (Māori social impact and program evaluation)			1
451131	Te whakamahere ā-tāone, ā-rohe o te Māori (Māori urban and regional planning)			1
451130	Te Tiriti o Waitangi (The Treaty of Waitangi)			1
490301	Experimental mathematics			1
420899	Traditional, complementary and integrative medicine not elsewhere classified			1
490303	Numerical solution of differential and integral equations			1
490302	Numerical analysis			1
490304	Optimisation			1
319901	Forensic biology			1
319902	Global change biology			1
440499	Development studies not elsewhere classified			1
4202	Epidemiology	This group covers epidemiology.		1
4203	Health services and systems	This group covers health services and systems.	a) Applications in health is included in Group 4608 Human-centred computing.	1
479999	Other language, communication and culture not elsewhere classified			1
451199	Ngā tāngata, te porihanga me ngā hapori o te Māori kāore anō kia whakarōpūtia i wāhi kē (Māori peoples, society and community not elsewhere classified)			1
480507	Youth justice			1
480506	Litigation, adjudication and dispute resolution			1
480505	Legal practice, lawyering and the legal profession			1
480504	Legal institutions (incl. courts and justice systems)			1
480503	Criminal procedure			1
480502	Civil procedure			1
480501	Access to justice			1
5106	Nuclear and plasma physics	This group covers nuclear and plasma physics.	a) Atom optics is included in Group 5108 Quantum physics.	1
5107	Particle and high energy physics	This group covers particle and high energy physics.	a) Atom optics is included in Group 5108 Quantum physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n b) Nuclear and plasma physics is included in Group 5106 Nuclear and plasma physics.	1
5108	Quantum physics	This group covers quantum physics.	a) Mathematical aspects of quantum physics are included in Group 4902 Mathematical physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n b) Communications technologies are included in Group 4006 Communications engineering.	1
380199	Applied economics not elsewhere classified			1
460909	Information systems philosophy, research methods and theory			1
460908	Information systems organisation and management			1
460907	Information systems for sustainable development and the public good			1
460902	Decision support and group support systems			1
460901	Business process management			1
460906	Information systems education			1
460905	Information systems development methodologies and practice			1
460904	Information security management			1
460903	Information modelling, management and ontologies			1
5110	Synchrotrons and accelerators	This group covers synchrotrons, accelerators, instruments and techniques.		1
339999	Other built environment and design not elsewhere classified			1
360599	Screen and digital media not elsewhere classified			1
401707	Solid mechanics			1
401706	Numerical modelling and mechanical characterisation			1
340504	Organic green chemistry			1
401705	Microelectromechanical systems (MEMS)			1
340505	Physical organic chemistry			1
401704	Mechanical engineering asset management			1
401703	Energy generation, conversion and storage (excl. chemical and electrical)			1
401702	Dynamics, vibration and vibration control			1
401701	Acoustics and noise control (excl. architectural acoustics)			1
340502	Natural products and bioactive compounds			1
340503	Organic chemical synthesis			1
401708	Tribology			1
340501	Free radical chemistry			1
350305	Forensic intelligence			1
370399	Geochemistry not elsewhere classified			1
350306	Forensic science and management			1
350307	Technology management			1
5101	Astronomical sciences	This group covers astronomical sciences, including cosmology.	a) Field theory and string theory are included in Group 5106 Nuclear and plasma physics.                                                                                                                                                                                                                                                                                                                                                                                                                  +\n b) Tropospheric and stratospheric physics are included in Group 3701 Atmospheric sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Planetary geology is included in Group 3705 Geology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n d) Satellite and space vehicle design and testing is included in Group 4001 Aerospace engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n e) Photogrammetry and remote sensing are included in Group 4013 Geomatic engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n f) Communications technologies using satellites are included in Group 4006 Communications engineering.	1
5102	Atomic, molecular and optical physics	This group covers atomic, molecular and optical physics.	a) Atom optics is included in Group 5108 Quantum physics.	1
350301	Business analytics			1
350302	Business information management (incl. records, knowledge and intelligence)			1
5103	Classical physics	This group covers classical physics.	a) Mathematical aspects of classical physics are included in Group 4902 Mathematical physics.                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Classical and physical optics is included in Group 5103 Classical physics.	1
350303	Business information systems			1
3504	Commercial services	This group covers commercial services, other than tourism and transportation and freight services.	a) Tourism is included in Group 3508 Tourism.                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Transportation and freight services are included in Group 3509 Transportation, logistics and supply chains.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n c) Private policing and security services are included in Group 4402 Criminology.	1
320203	Clinical microbiology			1
350304	Business systems in context			1
5105	Medical and biological physics	This group covers medical and biological physics.	a) Human biophysics is included in Group 3208 Medical physiology.	1
360104	Visual cultures			1
360102	Art history			1
360103	Art theory			1
360101	Art criticism			1
3399	Other built environment and design	This group covers built environment and design not elsewhere classified.		1
350799	Strategy, management and organisational behaviour not elsewhere classified			1
380399	Economic theory not elsewhere classified			1
450109	Aboriginal and Torres Strait Islander literature, journalism and professional writing			1
450108	Aboriginal and Torres Strait Islander linguistics and languages			1
450105	Aboriginal and Torres Strait Islander curatorial, archives and museum studies			1
450104	Aboriginal and Torres Strait Islander culture			1
450107	Aboriginal and Torres Strait Islander history			1
450106	Aboriginal and Torres Strait Islander ethics			1
450101	Aboriginal and Torres Strait Islander archaeology			1
450103	Aboriginal and Torres Strait Islander cultural history			1
450102	Aboriginal and Torres Strait Islander artefacts			1
409901	Agricultural engineering			1
349999	Other chemical sciences not elsewhere classified			1
450110	Aboriginal and Torres Strait Islander media, film, animation and photography			1
390199	Curriculum and pedagogy not elsewhere classified			1
409902	Engineering instrumentation			1
409903	Granular mechanics			1
310499	Evolutionary biology not elsewhere classified			1
340704	Theoretical quantum chemistry			1
340702	Radiation and matter			1
340703	Statistical mechanics in chemistry			1
451407	Pacific Peoples higher education			1
3999	Other education	This group covers education not elsewhere classified.		1
420703	Motor control			1
451406	Pacific Peoples technical, further, continuing and community education			1
451409	Pacific Peoples men’s education			1
420702	Exercise physiology			1
451408	Pacific Peoples language education			1
420701	Biomechanics			1
451403	Pacific Peoples curriculum and pedagogy			1
451402	Embedding Pacific Peoples knowledges, histories, culture, country, perspectives and ethics in education			1
510702	Field theory and string theory			1
451405	Pacific Peoples educational methods			1
451404	Pacific Peoples early childhood education			1
510701	Astroparticle physics and particle cosmology			1
300210	Sustainable agricultural development			1
340701	Computational chemistry			1
510703	Particle physics			1
451401	Cultural responsiveness and working with Pacific Peoples communities education			1
350503	Human resources management			1
300204	Agricultural management of nutrients			1
300203	Agricultural land planning			1
350504	Industrial and employee relations			1
520505	Social psychology			1
461208	Software testing, verification and validation			1
300206	Agricultural spatial analysis and modelling			1
350505	Occupational and workplace health and safety			1
520504	Psychology of religion			1
461207	Software quality, processes and metrics			1
350506	Workforce planning			1
300205	Agricultural production systems simulation			1
370599	Geology not elsewhere classified			1
300208	Farm management, rural management and agribusiness			1
350507	Workplace wellbeing and quality of working life			1
520503	Personality and individual differences			1
300207	Agricultural systems analysis and modelling			1
520502	Gender psychology			1
520501	Community psychology			1
300209	Germplasm management			1
461202	Empirical software engineering			1
440305	Population trends and policies			1
461201	Automated software engineering			1
440304	Mortality			1
440303	Migration			1
440302	Fertility			1
461206	Software architecture			1
401199	Environmental engineering not elsewhere classified			1
440301	Family and household studies			1
461205	Requirements engineering			1
461204	Programming languages			1
350501	Business and labour history			1
461203	Formal methods for software			1
350502	Employment equity and diversity			1
360302	Music composition and improvisation			1
360303	Music education			1
3502	Banking, finance and investment	This group covers banking, finance and investment.	a) Financial mathematics is included in Group 4901 Applied mathematics.                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Econometrics other than financial econometrics is included in Group 3802 Econometrics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n c) Demography is included in Group 4403 Demography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n d) International economics is included in Group 3801 Applied economics.	1
360301	Music cognition			1
3507	Strategy, management and organisational behaviour	This group covers strategy, management and organisational behaviour.	a) Tourism is included in Group 3508 Tourism.                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Transportation and freight services are included in Group 3509 Transportation, logistics and supply chains.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n c) Private policing and security services are included in Group 4402 Criminology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Decision theory is included in Group 5003 Philosophy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n e) Human resource management and workforce planning are included in Group 3505 Human resources and industrial relations.	1
3508	Tourism	This group covers tourism.	a) Hospitality, sport and leisure management are included in Group 3504 Commercial services.                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n b) Tourism geography is included in Group 4406 Human geography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n c) Tourism policy is included in Group 4407 Policy and administration.	1
3509	Transportation, logistics and supply chains	This group covers transportation, logistics and supply chains.	a) Transport engineering is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                              +\n b) Transport planning is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n c) Data engineering and data science is included in Group 4605 Data management and data sciences.	1
451413	Pacific Peoples women’s education			1
451899	Pacific Peoples, society and community not elsewhere classified			1
451410	Pacific Peoples primary education			1
360306	Musicology and ethnomusicology			1
451412	Pacific Peoples student engagement and teaching			1
360304	Music performance			1
360305	Music technology and recording			1
451411	Pacific Peoples secondary education			1
370101	Adverse weather events			1
300699	Food sciences not elsewhere classified			1
350999	Transportation, logistics and supply chains not elsewhere classified			1
320299	Clinical sciences not elsewhere classified			1
370102	Air pollution processes and air quality measurement			1
370103	Atmospheric aerosols			1
370104	Atmospheric composition, chemistry and processes			1
370109	Tropospheric and stratospheric physics			1
429999	Other health sciences not elsewhere classified			1
370105	Atmospheric dynamics			1
370106	Atmospheric radiation			1
370107	Cloud physics			1
370108	Meteorology			1
3102	Bioinformatics and computational biology	This group covers bioinformatics and computational biology.	a) Metabolomic chemistry is included in Group 3401 Analytical chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Gene mapping is included in Group 3105 Genetics.	1
4899	Other law and legal studies	This group covers law and legal studies not elsewhere classified.		1
3101	Biochemistry and cell biology	This group covers biochemistry or the chemistry of living organisms. It includes bioinformatics, cell biology and membrane biology.	a) Chemistry, including organic and biomolecular chemistry, is included in Division 34 Chemical sciences.                                                                                                                                                                                                                                                                                                                   +\n b) Cell and molecular biology specific to plants is included in Group 3108 Plant biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n c) Cell and molecular biology specific to animals is included in Group 3109 Zoology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n d) Biochemistry of agricultural plants is included in Group 3004 Crop and pasture production.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n e) Bioinformatics software is included in Group 3102 Bioinformatics and computational biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n f) Biochemistry and proteomics associated with medical sciences is included in Group 3205 Medical biochemistry and metabolomics.	1
451001	Te whaikaha me te Māori (Māori and disability)			1
519999	Other physical sciences not elsewhere classified			1
440799	Policy and administration not elsewhere classified			1
4403	Demography	This group covers demography.	a) Actuarial studies are included in Group 3502 Banking, finance and investment.	1
4404	Development studies	This group covers development studies.	a) Community planning; Management and development of housing markets; Urban and regional analysis and development; Transport planning; and urban design are included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                    +\n b) Economic development and growth is included in Group 3801 Applied economics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n c) Anthropology of development is included in Group 4401 Anthropology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n d) Urban and regional studies are included in Group 4406 Human geography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n e) International and development communication is included in Group 4701 Communication and media studies.	1
4405	Gender studies	This group covers gender studies.	a) Gender, sexuality and education is included 3904 Specialist studies in education.                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n b) Gender history is included in Group 4303 Historical studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n c) Anthropology of gender and sexuality is included in Group 4401 Anthropology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n d) Gender and crime is included in Group 4402 Criminology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n e) Gender and politics is included in Group 4408 Political science.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n f) Gender, policy and administration is included in Group 4407 Policy and administration.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n g) Sociology of gender is included in Group 4410 Sociology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n h) Culture, representation and identity is included in Group 4702 Cultural studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n i) Law, gender and sexuality is included in Group 4804 Law.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n j) Gender psychology is included in Group 5205 Social and personality psychology.	1
320206	Diagnostic radiography			1
4407	Policy and administration	This group covers policy and administration.	a) Health care administration is included in Group 4203 Health services and systems.                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n b) Educational administration is included in Group 3904 Specialist studies in education.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Business Administration is included in Division 35 Commerce, management, tourism and services.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Police administration, procedures and practice is included in Group 4402 Criminology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n e) Administrative law is included in Group 4807 Public law.	1
4408	Political science	This group covers political science.		1
4409	Social work	This group covers social work.	a) Urban and regional studies are included in Group 4406 Human geography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Social policy is included in Group 4407 Policy and administration.	1
450116	Aboriginal and Torres Strait Islander visual arts and crafts			1
450115	Aboriginal and Torres Strait Islander research methods			1
450599	Aboriginal and Torres Strait Islander peoples, society and community not elsewhere classified			1
450118	Conservation of Aboriginal and Torres Strait Islander heritage			1
450117	Aboriginal and Torres Strait Islander ways of knowing, being and doing			1
450112	Aboriginal and Torres Strait Islander philosophy			1
450111	Aboriginal and Torres Strait Islander music and performing arts			1
450114	Aboriginal and Torres Strait Islander repatriation			1
450113	Aboriginal and Torres Strait Islander religion and religious studies			1
321199	Oncology and carcinogenesis not elsewhere classified			1
460399	Computer vision and multimedia computation not elsewhere classified			1
470199	Communication and media studies not elsewhere classified			1
4401	Anthropology	This group covers anthropology.	a) Anthropological genetics is included in Group 3105 Genetics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n b) Archaeology is included in Group 4301 Archaeology.	1
470107	Media studies			1
470106	Media industry studies			1
490199	Applied mathematics not elsewhere classified			1
470108	Organisational, interpersonal and intercultural communication			1
451825	Pacific Peoples tourism			1
380301	History of economic thought			1
451824	Pacific Peoples sociology			1
451826	Pacific Peoples urban and regional planning			1
380304	Microeconomic theory			1
451821	Pacific Peoples social impact and program evaluation			1
451820	Pacific Peoples politics			1
470101	Communication studies			1
451823	Pacific Peoples sociological studies			1
380302	Macroeconomic theory			1
380303	Mathematical economics			1
451822	Pacific Peoples social work and social justice			1
470103	Environmental communication			1
470102	Communication technology and digital media studies			1
470105	Journalism studies			1
470104	International and development communication			1
390101	Creative arts, media and communication curriculum and pedagogy			1
390102	Curriculum and pedagogy theory and development			1
390103	Economics, business and management curriculum and pedagogy			1
390104	English and literacy curriculum and pedagogy (excl. LOTE, ESL and TESOL)			1
390105	Environmental education curriculum and pedagogy			1
390106	Geography education curriculum and pedagogy			1
390107	Humanities and social sciences curriculum and pedagogy (excl. economics, business and management)			1
449999	Other human society not elsewhere classified			1
390108	LOTE, ESL and TESOL curriculum and pedagogy			1
390109	Mathematics and numeracy curriculum and pedagogy			1
450509	Aboriginal and Torres Strait Islander customary law			1
450508	Aboriginal and Torres Strait Islander criminology			1
450505	Aboriginal and Torres Strait Islander community and regional development			1
450504	Aboriginal and Torres Strait Islander commerce			1
450507	Aboriginal and Torres Strait Islander community-based research			1
450506	Aboriginal and Torres Strait Islander community governance and decision making			1
450501	Aboriginal and Torres Strait Islander accounting			1
450503	Aboriginal and Torres Strait Islander architecture			1
320227	Venereology			1
450502	Aboriginal and Torres Strait Islander anthropology			1
320225	Sports medicine			1
320226	Surgery			1
320223	Rheumatology and arthritis			1
349901	Forensic chemistry			1
320224	Rural clinical health			1
460308	Pattern recognition			1
460307	Multimodal analysis and synthesis			1
460306	Image processing			1
460305	Image and video coding			1
390110	Medicine, nursing and health curriculum and pedagogy			1
390111	Physical education and development curriculum and pedagogy			1
390112	Religion curriculum and pedagogy			1
390113	Science, technology and engineering curriculum and pedagogy			1
460309	Video processing			1
390114	Vocational education and training curriculum and pedagogy			1
401105	Life cycle assessment and industrial ecology			1
401104	Health and ecological risk assessment			1
390115	Work integrated learning (incl. internships)			1
401103	Global and planetary environmental engineering			1
401102	Environmentally sustainable engineering			1
460304	Computer vision			1
460303	Computational imaging			1
460302	Audio processing			1
460301	Active sensing			1
451807	Pacific Peoples community-based research			1
310412	Speciation and extinction			1
451806	Pacific Peoples community governance and decision making			1
451809	Pacific Peoples customary law			1
451808	Pacific Peoples criminology			1
451803	Pacific Peoples architecture			1
310899	Plant biology not elsewhere classified			1
451802	Pacific Peoples anthropology			1
451805	Pacific Peoples community and regional development			1
451804	Pacific Peoples commerce			1
401106	Waste management, reduction, reuse and recycling			1
510304	Thermodynamics and statistical physics			1
350907	Rail transportation and freight services			1
320218	Pain			1
350908	Road transportation and freight services			1
510303	Electrostatics and electrodynamics			1
320219	Paramedicine			1
320216	Orthopaedics			1
451801	Pacific Peoples and the law			1
350909	Supply chains			1
320217	Otorhinolaryngology			1
320214	Nephrology and urology			1
320215	Nuclear medicine			1
320212	Intensive care			1
320213	Medical genetics (excl. cancer genetics)			1
320221	Psychiatry (incl. psychotherapy)			1
320222	Radiology and organ imaging			1
440709	Public policy			1
440708	Public administration			1
300602	Food chemistry and food sensory science			1
350901	Air transportation and freight services			1
320220	Pathology (excl. oral pathology)			1
440707	Housing policy			1
350902	Intelligent mobility			1
520108	Testing, assessment and psychometrics			1
300601	Beverage chemistry and beverage sensory science			1
370511	Structural geology and tectonics			1
350903	Logistics			1
440706	Health policy			1
520107	Sport and exercise psychology			1
300604	Food packaging, preservation and processing			1
300603	Food nutritional balance			1
440705	Gender, policy and administration			1
350904	Maritime transportation and freight services			1
520106	Psychology of ageing			1
520105	Psychological methodology, design and analysis			1
300606	Food sustainability			1
350905	Passenger needs			1
440704	Environment policy			1
440703	Economic development policy			1
520104	Industrial and organisational psychology (incl. human factors)			1
350906	Public transport			1
300605	Food safety, traceability, certification and authenticity			1
440702	Crime policy			1
401599	Maritime engineering not elsewhere classified			1
300607	Food technology			1
440701	Communications and media policy			1
370512	Volcanology			1
330499	Urban and regional planning not elsewhere classified			1
451818	Pacific Peoples perspectives			1
451817	Pacific Peoples not-for-profit social enterprises			1
310401	Animal systematics and taxonomy			1
310402	Biogeography and phylogeography			1
310403	Biological adaptation			1
451819	Pacific Peoples political participation and representation			1
370509	Sedimentology			1
451814	Pacific Peoples legislation			1
310404	Evolution of developmental systems			1
310405	Evolutionary ecology			1
451813	Pacific Peoples human geography and demography			1
320209	Gastroenterology and hepatology			1
451816	Pacific Peoples marketing			1
3902	Education policy, sociology and philosophy	This group covers education policy, sociology and philosophy.		1
310406	Evolutionary impacts of climate change			1
3903	Education systems	This group covers systems for the delivery of education services.	a) Educational administration and special education and disability are included in Group 3904 Specialist studies in education.	1
310407	Host-parasite interactions			1
451815	Pacific Peoples management			1
310408	Life histories			1
320207	Emergency medicine			1
451810	Pacific Peoples design practice and management			1
510799	Particle and high energy physics not elsewhere classified			1
320208	Endocrinology			1
310409	Microbial taxonomy			1
451812	Pacific Peoples finance			1
320205	Dermatology			1
369999	Other creative arts and writing not elsewhere classified			1
451811	Pacific Peoples economics			1
320201	Anaesthesiology			1
320210	Geriatrics and gerontology			1
320211	Infectious diseases			1
520599	Social and personality psychology not elsewhere classified			1
370505	Mineralogy and crystallography			1
370506	Palaeontology (incl. palynology)			1
370507	Planetary geology			1
370508	Resource geoscience			1
370501	Biomineralisation			1
370502	Geochronology			1
370503	Igneous and metamorphic petrology			1
310410	Phylogeny and comparative analysis			1
370504	Marine geoscience			1
520103	Forensic psychology			1
520102	Educational psychology			1
340299	Inorganic chemistry not elsewhere classified			1
300202	Agricultural land management			1
321599	Reproductive medicine not elsewhere classified			1
300201	Agricultural hydrology			1
510302	Classical and physical optics			1
440714	Urban policy			1
510301	Acoustics and acoustical devices; waves			1
440713	Tourism policy			1
440712	Social policy			1
440711	Risk policy			1
400299	Automotive engineering not elsewhere classified			1
440710	Research, science and technology policy			1
4801	Commercial law	This group covers commercial law.		1
450516	Aboriginal and Torres Strait Islander marketing			1
500501	Theology			1
4802	Environmental and resources law	This group covers environmental and resources law.		1
4803	International and comparative law	This group covers international and comparative law.		1
450515	Aboriginal and Torres Strait Islander management			1
450518	Aboriginal and Torres Strait Islander peoples and the law			1
4804	Law in context	This group covers law.		1
4805	Legal systems	This group covers legal systems.		1
450517	Aboriginal and Torres Strait Islander not-for-profit social enterprises			1
4806	Private law and civil obligations	This group covers private law and civil obligations.		1
450512	Aboriginal and Torres Strait Islander finance			1
321109	Predictive and prognostic markers			1
450511	Aboriginal and Torres Strait Islander economics			1
4807	Public law	This group covers public law.	a) International humanitarian and human rights law is included in Group 4803 International and comparative law.	1
450514	Aboriginal and Torres Strait Islander legislation			1
321107	Liquid biopsies			1
450513	Aboriginal and Torres Strait Islander human geography and demography			1
321108	Molecular targets			1
321105	Chemotherapy			1
321106	Haematological tumours			1
450510	Aboriginal and Torres Strait Islander design practice and management			1
321103	Cancer genetics			1
321104	Cancer therapy (excl. chemotherapy and radiation therapy)			1
321110	Radiation therapy			1
321111	Solid tumours			1
460799	Graphics, augmented reality and games not elsewhere classified			1
450527	Aboriginal and Torres Strait Islander urban and regional planning			1
450526	Aboriginal and Torres Strait Islander tourism			1
450523	Aboriginal and Torres Strait Islander social work and social justice			1
450522	Aboriginal and Torres Strait Islander social impact and program evaluation			1
450525	Aboriginal and Torres Strait Islander sociology			1
450524	Aboriginal and Torres Strait Islander sociological studies			1
450521	Aboriginal and Torres Strait Islander politics			1
470599	Literary studies not elsewhere classified			1
450520	Aboriginal and Torres Strait Islander political participation and representation			1
321101	Cancer cell biology			1
321102	Cancer diagnosis			1
480399	International and comparative law not elsewhere classified			1
401608	Organic semiconductors			1
401607	Metals and alloy materials			1
401606	Glass			1
401605	Functional materials			1
3099	Other agricultural, veterinary and food sciences	This group covers agricultural and veterinary and food sciences not elsewhere classified.		1
401604	Elemental semiconductors			1
401603	Compound semiconductors			1
389999	Other economics not elsewhere classified			1
401602	Composite and hybrid materials			1
401601	Ceramics			1
401609	Polymers and plastics			1
510299	Atomic, molecular and optical physics not elsewhere classified			1
401611	Wearable materials			1
401610	Timber, pulp and paper			1
460810	Social robotics			1
480412	Medical and health law			1
480411	Media and communication law			1
480410	Legal theory, jurisprudence and legal interpretation			1
480414	Sports law			1
480413	Race, ethnicity and law			1
410101	Carbon sequestration science			1
410102	Ecological impacts of climate change and ecological adaptation			1
410103	Human impacts of climate change and human adaptation			1
430199	Archaeology not elsewhere classified			1
480401	Criminal law			1
480409	Legal education			1
330413	Urban planning and health			1
400301	Biofabrication			1
480408	Law, science and technology			1
400302	Biomaterials			1
480407	Law, gender and sexuality (incl. feminist legal scholarship)			1
480406	Law reform			1
480405	Law and society and socio-legal research			1
330410	Urban analysis and development			1
480403	Law and humanities			1
330411	Urban design			1
330412	Urban informatics			1
480402	Family law			1
400309	Neural engineering			1
400307	Mechanobiology			1
400308	Medical devices			1
400305	Biomedical instrumentation			1
370905	Quaternary environments			1
370906	Regolith and landscape evolution			1
310801	Phycology (incl. marine grasses)			1
400306	Computational physiology			1
310802	Plant biochemistry			1
400303	Biomechanical engineering			1
500499	Religious studies not elsewhere classified			1
320605	Nanotoxicology, health and safety			1
310803	Plant cell and molecular biology			1
400304	Biomedical imaging			1
320606	Regenerative medicine (incl. stem cells)			1
310804	Plant developmental and reproductive biology			1
320604	Nanomedicine			1
310805	Plant pathology			1
310806	Plant physiology			1
320601	Gene and molecular therapy			1
460809	Pervasive computing			1
320602	Medical biotechnology diagnostics (incl. biosensors)			1
460808	Mixed initiative and human-in-the-loop			1
460803	Collaborative and social computing			1
460802	Affective computing			1
460801	Accessible computing			1
460807	Information visualisation			1
330406	Regional analysis and development			1
460806	Human-computer interaction			1
330407	Regulatory planning and development assessment			1
460805	Fairness, accountability, transparency, trust and ethics of computer systems			1
330408	Strategic, metropolitan and regional planning			1
330409	Transport planning			1
460804	Computing education			1
370901	Geomorphology and earth surface processes			1
330402	History and theory of the built environment (excl. architecture)			1
330403	Housing markets, development and management			1
370902	Glaciology			1
330404	Land use and environmental planning			1
370903	Natural hazards			1
400310	Rehabilitation engineering			1
370904	Palaeoclimatology			1
400311	Tissue engineering			1
330401	Community planning			1
340209	Organometallic chemistry			1
451906	Indigenous data and data technologies			1
451905	Global Indigenous studies sciences			1
340207	Metal organic frameworks			1
451907	Indigenous methodologies			1
340208	Non-metal chemistry			1
451902	Global Indigenous studies environmental knowledges and management			1
360299	Creative and professional writing not elsewhere classified			1
451901	Global Indigenous studies culture, language and history			1
451904	Global Indigenous studies peoples, society and community			1
451903	Global Indigenous studies health and wellbeing			1
340201	Bioinorganic chemistry			1
340202	Crystallography			1
340206	Metal cluster chemistry			1
340203	F-block chemistry			1
340204	Inorganic green chemistry			1
410599	Pollution and contamination not elsewhere classified			1
490202	Integrable systems (classical and quantum)			1
490201	Algebraic structures in mathematical physics			1
340210	Solid state chemistry			1
340211	Transition metal chemistry			1
490204	Mathematical aspects of general relativity			1
490203	Mathematical aspects of classical mechanics, quantum mechanics and quantum information theory			1
490206	Statistical mechanics, physical combinatorics and mathematical aspects of condensed matter			1
490205	Mathematical aspects of quantum and conformal field theory, quantum gravity and string theory			1
340699	Physical chemistry not elsewhere classified			1
350499	Commercial services not elsewhere classified			1
321503	Reproduction			1
321501	Foetal development and medicine			1
321502	Obstetrics and gynaecology			1
401203	Biomedical fluid mechanics			1
401202	Bio-fluids			1
5299	Other psychology	This group covers psychology not elsewhere classified.		1
401201	Aerodynamics (excl. hypersonic aerodynamics)			1
300199	Agricultural biotechnology not elsewhere classified			1
5201	Applied and developmental psychology	This group covers the development and application of psychological methods and psychological research findings to solve practical problems of human and animal behaviour and the manner in which those psychological processes change through the life span.		1
3008	Horticultural production	This group covers horticultural production and horticultural science.	a) Food science, including food processing and packaging, is included in Group 3006 Food sciences.                                                                                                                                                                                                                                                                                                                                                                                             +\n b) Genetic engineering and cloning of livestock is included in Group 3001 Agricultural biotechnology.	1
5202	Biological psychology	This group covers biological psychology research into the neural and physiological mechanisms that underpin psychological processes.		1
400704	Biomechatronics			1
389902	Ecological economics			1
3006	Food sciences	This group covers the food sciences.	a) Fermentation for non-food products is included in Group 3106 Industrial biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n b) Human nutrition is included in Group 3210 Nutrition and dietetics.	1
3005	Fisheries sciences	This group covers the sciences and technologies supporting fishing and production of freshwater and marine fisheries (including shellfish and crustaceans).	a) Food science, including food processing and packaging, is included in Group 3006 Food sciences.                                                                                                                                                                                                                                                                                                             +\n b) Genetic engineering and cloning of livestock is included in Group 3001 Agricultural biotechnology.	1
5204	Cognitive and computational psychology	This group covers cognitive and computational psychology. It includes the experimental study, computational and mathematical modelling of cognitive processes.	a) Computational modelling of expert systems that are not intended to study or mimic human or animal behaviour are included in the Group 4602 Artificial intelligence.	1
3499	Other chemical sciences	This group covers other chemical sciences not elsewhere classified.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n It includes forensic chemistry.		1
420303	Family care			1
420304	General practice			1
420301	Aged health care			1
420302	Digital health			1
420307	Health counselling			1
420308	Health informatics and information systems			1
420305	Health and community services			1
420306	Health care administration			1
451007	Te whakatairanga hauora o te Māori (Māori health promotion)			1
451006	Ngā kaupapahere hauora o te Māori (Māori health policy)			1
451009	Ngā rongoā me ngā whakamaimoa o te Māori (Māori medicine and treatments)			1
420309	Health management			1
451008	Ngā wāhanga ora o te Māori (Māori life course)			1
451003	Ngā tokoingoa ahurea o te hauora o te Māori (Māori cultural determinants of health)			1
451002	Ngā pūtaiao koiora-hauora, haumanu hoki o te Māori (Māori biomedical and clinical sciences)			1
451005	Te mātai tahumaero o te Māori (Māori epidemiology)			1
451004	Te horakai me ngā kai o te Māori (Māori diet and nutrition)			1
430103	Archaeology of Australia (excl. Aboriginal and Torres Strait Islander)			1
451010	Ngā kaiwhakawhānau me te mātai mate tamariki o te Māori (Māori midwifery and paediatrics)			1
409999	Other engineering not elsewhere classified			1
430102	Archaeology of Asia, Africa and the Americas			1
451012	Te mahi tapuhi o te Māori (Māori nursing)			1
430105	Archaeology of New Zealand (excl. Māori)			1
451011	Te hauora me te oranga ā-whaea, ā-pēpi o te Māori (Māori mothers and babies health and wellbeing)			1
430104	Archaeology of Europe, the Mediterranean and the Levant			1
430107	Historical archaeology (incl. industrial archaeology)			1
430106	Digital archaeology			1
430108	Maritime archaeology			1
420310	Health surveillance			1
420311	Health systems			1
400705	Control engineering			1
389903	Heterodox economics			1
3004	Crop and pasture production	This group covers the sciences and technologies supporting the production of crops, pastures and grains.                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • agronomy;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n • genetics and field protection procedures for crop and pasture production;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n • post harvest treatment transportation of crops and grains; and                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n • weed science	a) Genetic engineering of crops and pasture is included in Group 3001 Agricultural biotechnology.	1
400706	Field robotics			1
4799	Other language, communication and culture	This group covers language, communication and culture not elsewhere classified.		1
3002	Agriculture, land and farm management	This group covers the management of agriculture, land, farms and rural businesses.	a) Management of parks in natural environments is included in Group 4104 Environmental management.                                                                                                                                                                                                                                                                                                                                                                   +\n b) Soil sciences are included in Group 4106 Soil sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Management of forests for agricultural production is included in Group 3007 Forestry sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Agricultural water management is included in Group 3099 Other agricultural, veterinary and food sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n e) Management and planning of land and parks in built environments is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n f) Management of non-rural businesses is included in Group 3507 Strategy, management and organisational behaviour.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n g) Fisheries management is included in Group 3005 Fisheries sciences.	1
400703	Autonomous vehicle systems			1
389901	Comparative economic systems			1
3001	Agricultural biotechnology	This group covers agricultural biotechnology.	a) Improvement of plants through selective breeding is included in Group 3004 Crop and pasture production and Group 3007 Forestry sciences and Group 3008 Horticultural production.                                                                                                                                                                                                                                                                                                                                  +\n b) Improvement of animals through selective breeding is included in Group 3003 Animal production.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Genetic modification of plants, microbes or animals for environmental purposes and biological control of pests, diseases and exotic species is included in Group 4103 Environmental biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                      +\n c) Genetic modification of plants, microbes or animals for industrial purposes (other than agriculture) is included in Group 3106 Industrial biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n d) Genetic modification of plants, microbes or animals for medical purposes is included in Group 3206 Medical biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n e) Nanobiotechnology is included in Group 3106 Industrial biotechnology and medical biotechnology is included in Group 1119 Nanotechnology health.                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n f) Ethical use of biotechnology is included in Group 5001 Applied ethics.	1
4501	Aboriginal and Torres Strait Islander culture, language and history	This group covers culture, languages, history, creative and performing arts, archaeology, built environment and design, philosophy and religion related to Aboriginal and Torres Strait Islander.		1
4505	Aboriginal and Torres Strait Islander peoples, society and community	This group covers human society, economics, commerce, tourism, community governance and decision making, anthropology, human geography, community-based research, architecture and design, and urban planning related to Aboriginal and Torres Strait Islander.	a) Aboriginal and Torres Strait Islander health policy is included in Group 4504.	1
3215	Reproductive medicine	This group covers reproductive medicine.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n assisted reproduction;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n foetal development and medicine; and                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n obstetrics and gynaecology.	a) Stem cells and tissue engineering are included in Group 3206 Medical biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n b) Paediatrics is included in Group 3213 Paediatrics.	1
3205	Medical biochemistry and metabolomics	This group covers human biochemistry or the chemistry of human living organisms and the human life process. It includes proteomics, metabolomics and metabolic medicine.	a) Medicinal and biomolecular chemistry is included in Group 3404 Medicinal and biomolecular chemistry.                                                                                                                                                                                                                                                                        +\n b) Biochemistry of species other than human or non-agricultural plant and biochemistry not related to medicine or health are included in Group 3101 Biochemistry and cell biology.                                                                                                                                                                                                                                                                                                                                                                                                                         +\n c) Biochemistry of agricultural plants is included in Groups 3004 Crop and pasture production and 3008 Horticultural production.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n d) Clinical chemistry and hormonal control of metabolism is included in Group 3202 Clinical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n e) Nutrition and dietetics is included in Group 3210 Nutrition and dietetics.	1
459999	Other Indigenous studies not elsewhere classified			1
450709	Ngā matatika o te Māori (Māori ethics)			1
450708	Ngā mātai kaitiaki, pūranga me ngā whare tongarewa o te Māori (Māori curatorial, archives and museum studies)			1
450299	Aboriginal and Torres Strait Islander education not elsewhere classified			1
4699	Other information and computing sciences	This group covers other information and computing sciences not elsewhere classified.		1
400701	Assistive robots and technology			1
400702	Automation engineering			1
451018	Ngā tokoingoa pāpori o te hauora o te Māori (Māori social determinants of health)			1
451017	Te oranga ā-pāpori, ā-hinengaro, ā-ahurea, ā-wairua o te Māori (Māori social, cultural, emotional and spiritual wellbeing)			1
451019	Ngā hākinakina me te korikori tinana o te Māori (Māori sport and physical activity)			1
400709	Medical robotics			1
451014	Ngā pūnaha mātauranga hinengaro o te Māori (Māori psychology knowledge systems)			1
451013	Mātauranga hinengaro kaupapa Māori (Māori psychology)			1
451016	Te hauora mamao o te Māori (Māori remote health)			1
430101	Archaeological science			1
400707	Manufacturing robotics			1
400708	Mechatronics hardware design and architecture			1
451015	Te hauora me te oranga tūmatanui o te Māori (Māori public health and wellbeing)			1
451499	Pacific Peoples education not elsewhere classified			1
410504	Surface water quality processes and contaminated sediment assessment			1
451021	Ngā taiohi me ngā whānau Māori (Māori youth and family)			1
451020	Te ariā o ngā tauira panoni hauora o te Māori (Māori theory of change models for health)			1
410501	Environmental biogeochemistry			1
410502	Noise and wave pollution processes and measurement			1
410503	Groundwater quality processes and contaminated land assessment			1
461299	Software engineering not elsewhere classified			1
340605	Molecular imaging (incl. electron microscopy and neutron diffraction)			1
340606	Photochemistry			1
360699	Visual arts not elsewhere classified			1
340603	Colloid and surface chemistry			1
340604	Electrochemistry			1
340609	Transport properties and non-equilibrium processes			1
400710	Micro-manipulation			1
340607	Reaction kinetics and dynamics			1
400711	Simulation, modelling, and programming of mechatronics systems			1
340608	Solution chemistry			1
340601	Catalysis and mechanisms of reactions			1
340602	Chemical thermodynamics and energetics			1
350404	Retail			1
350405	Sport and leisure management			1
370499	Geoinformatics not elsewhere classified			1
519901	Complex physical systems			1
350401	Food and hospitality services			1
350402	Hospitality management			1
350403	Real estate and valuation services			1
360203	Professional writing and journalism practice			1
420799	Sports science and exercise not elsewhere classified			1
420314	Multimorbidity			1
360204	Site-based writing			1
420315	One health			1
360201	Creative writing (incl. scriptwriting)			1
420312	Implementation science and evaluation			1
360202	Digital writing			1
420313	Mental health services			1
420318	People with disability			1
420319	Primary health care			1
420316	Palliative care			1
420317	Patient safety			1
360205	Technical writing			1
350899	Tourism not elsewhere classified			1
420321	Rural and remote health services			1
440399	Demography not elsewhere classified			1
420320	Residential client care			1
450199	Aboriginal and Torres Strait Islander culture, language and history not elsewhere classified			1
380299	Econometrics not elsewhere classified			1
3202	Clinical sciences	This group covers specific clinical aspects of medicine, including causes, diagnosis, treatment and management of specific diseases and conditions.	a) Accelerators and other equipment for the production of radioisotopes for nuclear medicine are included in Group 5110 Synchrotrons and accelerators.                                                                                                                                                                                                                                                                  +\n b) Anthropological genetics is included in Group 3105 Genetics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n c) Medical biochemistry, cardiovascular medicine, dentistry, immunology, medical microbiology, neurosciences, oncology, ophthalmology, paediatrics, pharmacology and medical physiology are included in other groups in Division 32 Biomedical and clinical sciences.                                                                                                                                                                                                                                                                                                                                      +\n d) Oral pathology is included in Group 3203 Dentistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n e) Radiotherapy and cancer genetics is included in Group 3211 Oncology and carcinogenesis.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n f) Psychology is included in Division 52 Psychology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n g) Physiotherapy, podiatry and rehabilitation and therapy are included in Group 4201 Allied health and rehabilitation science.                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n h) Human movement is included in 4207 Sports science and exercise.	1
4516	Pacific Peoples health and wellbeing	This group covers health and wellbeing (including physical, psychological and spiritual wellbeing), health services and biomedical and clinical sciences related to Pacific Peoples.		1
4517	Pacific Peoples sciences	This group covers human society, economics, commerce, tourism, community governance and decision making, anthropology, human geography, community-based research, architecture and design, and urban planning related to Pacific Peoples.	a) Pacific Peoples health policy is included in Group 4516.	1
4901	Applied mathematics	This group covers applied mathematics.	a) Mathematical aspects of physics are included in Group 4902 Mathematical physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n b) Bioinformatics is included in Group 3101 Biochemistry and cell biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Geodesy is included in Group 3706 Geophysics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n d) Applications of control theory to engineering are included in Groups 4007 Control engineering, mechatronics and robotics and 4017 Mechanical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n e) Mathematics applied in economics is included in Division 38 Economics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n f) Complex systems mathematics applied in engineering is included in Division 40 Engineering.	1
450413	Aboriginal and Torres Strait Islander midwifery and paediatrics			1
450899	Te mātauranga Māori kāore anō kia whakarōpūtia i wāhi kē (Māori education not elsewhere classified)			1
3601	Art history, theory and criticism	This group covers art history, theory and criticism.	a) Literary studies are included in Group 4705 Literary studies.                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n b) Curatorial and related studies are included in Group 4302 Heritage, archive and museum studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n c) Creative arts, media and communication curriculum and pedagogy is included in Group 3901 Curriculum and pedagogy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n d) Music therapy is included in Group 4201 Allied health and rehabilitation science.	1
470508	Digital literature			1
480308	International trade and investment law			1
450905	Te ahumoana me te ahumoana tuku iho o te Māori (Māori fisheries and customary fisheries)			1
3199	Other biological sciences	This group covers biological sciences not elsewhere classified. It includes forensic biology and global change biology.		1
330105	Architectural science and technology			1
330104	Architectural history, theory and criticism			1
400699	Communications engineering not elsewhere classified			1
4099	Other engineering	This group covers engineering not elsewhere classified.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • agricultural engineering;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n • engineering instrumentation;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n • granular mechanics.		1
350106	Not-for-profit accounting and accountability			1
4201	Allied health and rehabilitation science	This group covers allied health and rehabilitation science.	a) Nutrition and dietetics is included in Group 3210 Nutrition and dietetics.	1
3104	Evolutionary biology	This group covers evolutionary biology, including biogeography and adaptation.	a) Evolutionary genetics and molecular evolution is included in Group 3105 Genetics.                                                                                                                                                                                                                                                                                                                                                                                                      +\n b) Evolutionary impacts of climate change are included in Group 4101 Climate change impacts and adaptation.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n c) Climate change processes are included in Group 3702 Climate change science.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n d) Ethology and sociobiology are included in Group 3103 Ecology.	1
3105	Genetics	This group covers genetics and heredity.	a) Microbial genetics is included in Group 3107 Microbiology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n b) Animal genetics associated with animal reproduction is included in Group 3003 Animal production.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n c) Genetics of agricultural crops (excl. genetic engineering) is included in Groups 3004 Crop and pasture production and 3008 Horticultural production.                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n d) Genetic engineering of agricultural crops is included in Group 3001 Agricultural biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n e) Genetics associated with human health and medicine is included in Groups 3202 Clinical sciences and 3211 Oncology and carcinogenesis.                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n f) Phylogenetic networks, ontologies and statistical and quantitative genetics are included in Group 3102 Bioinformatics and computational biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n g) Phylogeny is included in Group 3104 Evolutionary biology.	1
3106	Industrial biotechnology	This group covers industrial biotechnology .	a) Environmental biotechnology, nanotechnology and nanometrology is included in Group 4103 Environmental biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Genetic modification of plants, microbes or animals for agricultural purposes is included in Group 3001 Agricultural biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) Genetic modification of plants, microbes or animals for industrial purposes (other than agriculture) is included in Group 3106 Industrial biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n d) Genetic modification of plants, microbes or animals for medical purposes is included in Group 3206 Medical biotechnology.	1
3108	Plant biology	This group covers plant biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • plant pathology other than that associated with plant protection procedures used in agriculture and horticulture; and                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n • phycology (including algae and marine grasses).	a) Palaeobotany and palynology (other than palynology associated with ecological studies) are included in Group 3705 Geology.                                                                                                                                                                                                                                                                                                                                                                                                                          +\n b) Cell and molecular biology not specific to plants or animals is included in Group 3101 Biochemistry and cell biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Plant ecology and palynology associated with ecological studies is included in Group 3103 Ecology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n d) Plant and fungus systematics and taxonomy are included in Group 3104 Evolutionary biology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n e) Plant sciences or plant pathology associated with agriculture, forestry or primary production is included in Division 30 Agricultural, veterinary and food sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n f) Mycology, which for the purpose of this classification includes the study of lichen, is included in Group 3107 Microbiology.	1
4007	Control engineering, mechatronics and robotics	This group covers control engineering, mechatronics and robotics.	a) Automotive mechatronics and autonomous systems are included in Field 400203 Automotive mechatronics and autonomous systems.                                                                                                                                                                                                                                                                                                                                               +\n b) Control Theory is included in Group 4901 Applied mathematics.	1
4008	Electrical engineering	This group covers electrical engineering including electrical circuits and systems and electromagnetics, but excluding electronics, communications and control engineering	a) Electronic circuits and systems are included in Group 4009 Electronics, sensors and digital hardware.                                                                                                                                                                                                                                                                                    +\n b) Electromagnetics of antennas and propagation is included in Group 4006 Communications engineering.	1
320506	Medical biochemistry - proteins and peptides (incl. medical proteomics)			1
330301	Data visualisation and computational (incl. parametric and generative) design			1
5109	Space sciences	This group covers space sciences.	a) Astronomical sciences, including cosmology are included in 5101 Astronomical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Field theory and string theory are included in Group 5106 Nuclear and plasma physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Tropospheric and stratospheric physics are included in Group 3701 Atmospheric sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n d) Planetary geology is included in Group 3705 Geology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n e) Satellite and space vehicle design and testing is included in Group 4001 Aerospace engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n f) Remote sensing is included in Group 4013 Geomatic engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n g) Communications technologies using satellites are included in Group 4006 Communications engineering.	1
5104	Condensed matter physics	This group covers condensed matter physics.	a) Other material sciences are included in Groups 3402 Inorganic chemistry, 3403 Macromolecular and materials chemistry, 4003 Biomedical engineering, 4016 Materials engineering and 4019 Resources engineering and extractive metallurgy.                                                                                                                                                                                                                                                                               +\n b) Nanotechnology is included in Group 4103 Environmental biotechnology, 3106 Industrial biotechnology, 4018 Nanotechnology and 3206 Medical biotechnology.	1
4010	Engineering practice and education	This group covers engineering practice and education.	a) Risk engineering associated with vehicle safety, fire safety and manufacturing safety, is included in Group 4002 Automotive engineering, Group 4005 Civil engineering, and Group 4014 Manufacturing engineering, respectively.                                                                                                                                                                                                                                                                    +\n b) Systems engineering associated with the single disciplines of aerospace engineering, civil engineering, communication systems, electronics and electrical engineering is included in Group 4001 Aerospace engineering, Group 4005 Civil engineering, Group 4006 Communications engineering, Group 4009 Electronics, sensors and digital hardware and Group 4008 Electrical engineering, respectively.                                                                                                                                                                                                   +\n c) Education theory and practice in areas not principally focussed on engineering is included in Group 3901 Curriculum and pedagogy.	1
4016	Materials engineering	This group covers materials engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • ceramics science, technologies and engineering;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n • polymer and textiles engineering;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n • composite and hybrid materials;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n • physical metallurgy and alloy materials;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n • functional materials; and                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n • semiconductor materials science and technologies.	a) Materials physics is included in Group 5104 Condensed matter physics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n b) Materials chemistry, including the theory and design of materials, is included in Group 3403 Macromolecular and materials chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n c) Engineering of materials for automotive applications is included in Group 4002 Automotive engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n d) Biomaterials are included in Group 4003 Biomedical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n e) Powder and particle technology is included in Group 4004 Chemical engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n f) Construction materials are included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n g) Extractive metallurgy is included in Group 4019 Resource engineering and extractive metallurgy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n h) Nanomaterials, molecular and organic electronics, and nanotechnology are included in Group 4018 Nanotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n i) Electro-optical, photonic and photovoltaic devices are included in Group 4009 Electronics, sensors and digital hardware.	1
4002	Automotive engineering	This group covers automotive engineering.	a) Combustion and fuel engineering without automotive applications is included in Group 4004 Chemical engineering.                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Transport engineering other than automotive engineering is included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n c) Mechatronics without automotive applications is included in Group 4007 Control engineering, mechatronics and robotics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n d) Materials engineering without automotive applications is included in Group 4016 Materials engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n e) Heat and mass transfer operations, fluidisation, fluid mechanics and turbulent flows are included in Group 4012 Fluid mechanics and thermal engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n f) Electrical energy generation and storage are included in 4008 Electrical engineering.	1
4004	Chemical engineering	This group covers chemical engineering including process technologies.	a) Chemistry is included in Division 34 Chemical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n b) Carbon sequestration science and modelling are included in Group 4101 Climate change impacts and adaptation.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n c) Combustion and fuel engineering for automotive applications is included in Group 4002 Automotive engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n d) Water resources and quality engineering are included in Group 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n e) Process control and simulation associated with control engineering is included in Group 4007 Control engineering, mechatronics and robotics.                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n f) Fluidisation, fluid mechanics and heat and mass transfer operations are included in Group 4012 Fluid mechanics and thermal engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n g) Biocatalysis is included in Group 3106 Industrial biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n h) Non-Newtonian fluid flows including rheology are included in Group 4012 Fluid mechanics and thermal engineering.	1
4605	Data management and data science	This group covers methods and computing systems for working with data sets.	a) Mathematical transformations and techniques used in the querying of audio, images, or video are included in Group 4603 Computer vision and multimedia computation.                                                                                                                                                                                                                                                                                                            +\n b) The management of or administration strategies for database systems are included in Group 4609 Information systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) Cryptography, privacy, or security aspects of database systems are included in Group 4604 Cybersecurity and privacy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n d) The graphical visualization or visual presentations of data is included in Group 4608 Human-centred computing.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n e) Machine learning techniques for analysis of large data sets is included in Group 4611 Machine learning.	1
4608	Human-centred computing	This group covers methods and technologies which relate to how people and computer systems function together.	a) Use of computing for education other than in the computer sciences and information systems disciplines is included in Division 39 Education.                                                                                                                                                                                                                                                                                                         +\n b) Use of computing for information systems education is included in Group 4609 Information systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n c) Intelligent robotics not focussing primarily on the social aspects of interactions between human and  robot is included in Group 4602 Artificial intelligence.                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Hardware aspects of social robotics is included in Group 4007 Control engineering, mechatronics and robotics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n e) Design of interfaces for, and experience with, business information systems is included in Group 4906 Information systems.	1
35	COMMERCE, MANAGEMENT, TOURISM AND SERVICES	This division covers commerce, management, tourism and services.	a) Commerce, management, tourism and services relating to Aboriginal and Torres Strait Islanders are included in Group 4505 Aboriginal and Torres Strait Islander peoples, society and community, Māori in Group 4511 Te ahurea, reo me te hītori o te Māori (Māori culture, language and history), Pacific Peoples in Group 4517 Pacific Peoples, society and community and other Indigenous in Group 4519 Other Indigenous data, methodologies and global Indigenous studies.	1
3303	Design	This group covers design.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • design history and theory;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • ergonomics;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n • industrial design;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n • textile and fashion design; and                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n • visual communication and graphics design.	a) Architectural design and architectural history is included in Group 3301 Architecture.                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n b) Engineering design is included in Group 4010 Engineering practice and education.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n c) Fine arts are included in Group 3606 Visual Arts.	1
360502	Computer gaming and animation			1
46	INFORMATION AND COMPUTING SCIENCES	This division covers information and computing sciences.	a) Indigenous information and computing sciences are included in Division 45 Indigenous studies.	1
51	PHYSICAL SCIENCES	This division covers the physical sciences. It includes biological physics (other than human biophysics), medical physics and synchrotron and accelerator technologies.	a) Indigenous physical sciences are included in Division 45 Indigenous studies.	1
450305	Aboriginal and Torres Strait Islander fisheries and customary fisheries			1
310699	Industrial biotechnology not elsewhere classified			1
300406	Crop and pasture improvement (incl. selection and breeding)			1
3701	Atmospheric sciences	This group covers atmospheric sciences.	a) Mesospheric, ionospheric and magnetospheric physics is included in Group 5109 Space sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Palaeoclimatology is included in Group 3709 Physical geography and environmental geoscience.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n c) Environmental sciences, including environmental monitoring, carbon sequestration science and impacts of climate change are included in Division 41 Environmental sciences.                                                                                                                                                                                                                                                                                                                                                                                                                              +\n d) Climate change science, including processes, greenhouse gas inventories and climatology is included in Group 3702 Climate change science.	1
3705	Geology	This group covers geology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n It includes palaeontology, including palaeozoology, palaeobotany and palynology, other than that associated with palaeoecological studies.	a) Planetary science (excl. solar system and planetary geology) is included in Group 5101 Astronomical sciences.                                                                                                                                                                                                                                                                                                                                              +\n b) Solid state chemistry and the chemical aspects of crystallography are included in Group 3402 Inorganic chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n c) Oceanography is included in Group 3708 Oceanography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n d) Palaeoclimatology is included in Group 3709 Physical geography and environmental geoscience.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            +\n e) Palaeontology associated with palaeoecological studies is included in Group 3103 Ecology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n f) Materials engineering is included in Group 4016 Materials engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n g) Mining, mineral processing and metallurgy are included in Group 4019 Resources engineering and extractive metallurgy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n h) Solar system planetary science (excl. planetary geology) is included in 5109 Space sciences.	1
3709	Physical geography and environmental geoscience	This group covers physical geography and environmental geoscience	a) Climatology is included in Group 3701 Atmospheric sciences.                                                                                                                                                                                                                                                                                                                                                                                                              +\n b) Environmental sciences, including environmental monitoring, carbon sequestration science and impacts of climate change are included in Division 41 Environmental sciences.                                                                                                                                                                                                                                                                                                                                                                                                                              +\n c) Climate change science, including processes, greenhouse gas inventories and climatology is included in Group 3702 Climate change science.                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n d) Hydrology other than agricultural hydrology is included in Group 3707 Hydrology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n e) Agricultural hydrology is included in Group 3002 Agriculture, land and farm management                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n f) Water reticulation for household and non-agricultural industry use is included in Groups 4004 Chemical engineering and 4005 Civil engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n g) Environmental engineering is included in Group 4011 Environmental engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n h) Social aspects of geography are included in Group 4406 Human geography.	1
310606	Industrial molecular engineering of nucleic acids and proteins			1
320402	Applied immunology (incl. antibody engineering, xenotransplantation and t-cell therapies)			1
4105	Pollution and contamination	This group covers pollution and contamination.	a) Processes, modelling and mitigation of air pollution is included in Group 3701 Atmospheric sciences.                                                                                                                                                                                                                                                                                                                                                                                                            +\n b) Pollution modelling and control, environmental sustainable engineering and waste management is included in Group 4011 Environmental engineering.	1
4102	Ecological applications	This group covers applications of ecology.	a) Conservation and biodiversity are included in Group 4104 Environmental management.                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n b) Basic ecological research which includes terrestrial ecology is included in Group 3103 Ecology.	1
4103	Environmental biotechnology	This group covers environmental biotechnology.	a) Environmental engineering is included in Group 4011 Environmental engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n b) Genetic modification of plants, microbes or animals for agricultural purposes is included in Group 3001 Agricultural biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n c) Genetic modification of plants, microbes or animals for industrial purposes (other than agriculture) is included in Group 3106 Industrial biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n d) Genetic modification of plants, microbes or animals for medical purposes is included in Group 3206 Medical biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n e) Nanometrology is included in Group 4018 Nanotechnology.	1
4104	Environmental management	This group covers environmental management.	a) Ecological applications are included in Group 4102 Ecological applications.                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Management of land for agricultural production other than forestry production is included in Group 3002 Agriculture, land and farm management.                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n c) Management of forests for forestry production is included in Group 3007 Forestry sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n d) Bioremediation and environmental biosensors are included in Group 4103 Environmental biotechnology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n e) Management of land and parks in built environments is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n f) Environment policy is included in Group 4407 Policy and Administration.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n g) Waste management and recycling are included in Group 4011 Environmental engineering.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n h) Environmental education curriculum and pedagogy is included in Group 3901 Curriculum and pedagogy.	1
450213	Embedding Aboriginal and Torres Strait Islander knowledges, histories, culture, country, perspectives and ethics in education			1
3402	Inorganic chemistry	This group covers inorganic chemistry.	a) Other material sciences are included in Groups 5104 Condensed matter physics, 3403 Macromolecular and materials chemistry, 4003 Biomedical engineering, 4016 Materials engineering and 4019 Resources engineering and extractive metallurgy.                                                                                                                                                                                                                                                                                    +\n b) Mineralogy and crystallography are included in Group 3705 Geology.	1
3403	Macromolecular and materials chemistry	This group covers macromolecular and materials chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n It includes nanochemistry.	a) Other material sciences are included in Groups 5104 Condensed matter physics, 3402 Inorganic chemistry, 4003 Biomedical engineering, 4016 Materials engineering and 4019 Resources engineering and extractive metallurgy.                                                                                                                                                                                                                                                                                                                                                  +\n b) Supramolecular chemistry is included in Group 3405 Organic chemistry.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Nanoscale characterisation and nanotechnology other than nanochemistry are included in Group 4018 Nanotechnology.	1
451108	Te mātauranga taihara Māori (Māori criminology)			1
451107	Te kāwana ā-hapori, whakatau take hoki o te Māori (Māori community governance and decision making)			1
451122	Te whai wāhi me te whakakanohi taha tōrāngapū o te Māori (Māori political participation and representation)			1
4302	Heritage, archive and museum studies	This group covers heritage, archive and museum studies. It includes heritage and cultural conservation.	a) Librarianship is included in Group 4610 Library and information studies.                                                                                                                                                                                                                                                                                                                                                                      +\n b) Cultural studies are included in Group 4702 Cultural studies.	1
480499	Law in context not elsewhere classified			1
450608	Aboriginal and Torres Strait Islander knowledge management methods			1
4701	Communication and media studies	This group covers communication and media studies. It includes the social impacts of marketing and advertising.	a) Marketing and advertising, other than their social or cultural impacts, are included in Group 3506 Marketing.                                                                                                                                                                                                                                                                                                                              +\n b) Film, television and digital media are included in Group 3605 Screen and digital media.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Professional writing and journalism practice is included in Group 3602 Creative and professional writing.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n d) Communications technologies are included in Group 4006 Communications engineering.	1
4702	Cultural studies	This group covers cultural studies.	a) Marketing and advertising, other than their social or cultural impacts, are included in Group 3506 Marketing.                                                                                                                                                                                                                                                                                                                                                                                                                         +\n b) Gender studies are included in Group 4405 Gender studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n c) Visual cultures is included in Group 3601 Art history, theory and criticism and print culture is included in Group 4705 Literary studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n d) Intercultural communication and the social impacts of marketing and advertising are included in Group 4701 Communication and media studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n e) Language in culture is included in Group 4704 Linguistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n f) Cultural conservation is included in Group 4302 Heritage, archive and museum studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n g) Indigenous cultural studies are included in Division 45 Indigenous studies.	1
390401	Comparative and cross-cultural education			1
390403	Educational administration, management and leadership			1
330316	Visual communication design (incl. graphic design)			1
4205	Nursing	This group covers nursing.	a) Aged, people with disability, family and residential client care are included in Group 4203 Health services and systems.                                                                                                                                                                                                                                                                                                                                                                                                                                +\n b) Midwifery is included in Group 4204 Midwifery.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n c) Medicine, nursing and health curriculum and pedagogy is included in Group 3901 Curriculum and pedagogy.	1
4206	Public health	This group covers public health.	a) Health informatics is included in Group 4203 Health services and systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n b) Nursing is included in Group 4205 Nursing.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n c) Nutrition and public nutrition intervention is included in Group 3210 Nutrition and dietetics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n d) Health economics is included in Group 3801 Applied economics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n e) Demography is included in Group 4403 Demography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n f) Health policy is included in Group 4407 Policy and administration.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n g) Human bioethics and medical ethics are included in Group 5001 Applied ethics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n h) Medicine, nursing and health curriculum and pedagogy is included in Group 3901 Curriculum and pedagogy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n i) Occupational and workplace health and safety are included Group 3505 Human resources and industrial relations.	1
451124	Ngā ture rawa Māori (Māori resource law)			1
451126	Ngā mahi tauwhiro me te tika pāpori o te Māori (Māori social work and social justice)			1
3501	Accounting, auditing and accountability	This group covers accounting, auditing and accountability.	a) The economics of taxation are included in Group 3801 Applied economics.                                                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Taxation law is included in Group 4801 Commercial law.	1
3503	Business systems in context	This group covers business systems in context.	a) Data models, systems and quality are included in Group 4605 Data management and data science.                                                                                                                                                                                                                                                                                                                                                                                                                   +\n b) Information modelling, management and ontologies are included in Group 4609 Information systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n c) Organisational planning and management is included in Group 3507 Strategy, management and organisational behaviour.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n d) Logistics and supply chain management is included in Group 3509 Transportation, logistics and supply chains.	1
3505	Human resources and industrial relations	This group covers human resources and industrial relations.	a) Organisational and occupational health and safety is included in Group 4206 Public health.	1
3506	Marketing	This group covers marketing.	a) Tourism marketing is included in Group 3508 Tourism.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n b) Social impacts of marketing are included in Group 4701 Communication and media studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Cultural impacts of marketing are included in Group 4702 Cultural studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              +\n d) Market research does not constitute R&D in ANZSRC and as such should not be classified.	1
3103	Ecology	This group covers ecology other than its environmental applications.	a) Palaeontology other than that associated with palaeoecological studies is included in Group 3705 Geology.                                                                                                                                                                                                                                                                                                                                                                                                     +\n b) Applications of ecology, including the ecology of invasive species, are included in Group 4102 Ecological applications.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n c) Population and ecological genetics is included in Group 3105 Genetics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n d) Microbial ecology is included in Group 3107 Microbiology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n e) Freshwater ichthyology and animal physiological ecology are included in Group 3109 Zoology.	1
4402	Criminology	This group covers criminology.	a) Forensic biology is included in Group 3199 Other biological sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Forensic chemistry is included in Group 3499 Other chemical sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n c) Forensic intelligence and forensic science and management is included in Group 3503 Business systems in context.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n d) Forensic epidemiology is included in Group 4202 Epidemiology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n e) Historical studies in crime is included in Group 4303 Historical studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n f) Crime policy is included in Group 4407 Policy and administration.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n g) Digital forensics is included in Group 4604 Cybersecurity and privacy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n h) International criminal law is included in Group 4803 International and comparative law.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n i) Criminal Law is included in Group 4804 Law.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             +\n j) Criminal procedure is included in 4805 Legal systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n k) Forensic evaluation, inference and statistics is included in Group 4905 Statistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     +\n l) Forensic psychology is included in Group 5201 Applied and developmental psychology.	1
4406	Human geography	This group covers human geography.	a) Physical geography is included in Group 3709 Physical geography and environmental geoscience.                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n b) Urban and regional planning is included in Group 3304 Urban and regional planning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n c) Economics, including urban and regional economics, is included in Group 3801 Applied economics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         +\n d) Demography is included in Group 4403 Demography.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n e) Urban and environment policy is included in Group 4407 Policy and administration.	1
4410	Sociology	This group covers sociology.	a) Criminology is included in Group 4402 Criminology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n b) Social policy is included in Group 4407 Policy and administration.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n c) Law and society is included in Division 48 Law and legal Studies.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       +\n d) Language in society (sociolinguistics) is included in Group 4704 Linguistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n e) History and philosophy of the social sciences is included in Group 5002 History and philosophy of specific fields.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n f) Social and political philosophy is included in Group 5003 Philosophy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   +\n g) Religion and society is included in Group 5004 Religious studies.	1
401101	Air pollution modelling and control			1
310411	Plant and fungus systematics and taxonomy			1
320699	Medical biotechnology not elsewhere classified			1
370510	Stratigraphy (incl. biostratigraphy, sequence stratigraphy and basin analysis)			1
370999	Physical geography and environmental geoscience not elsewhere classified			1
3901	Curriculum and pedagogy	This group covers curriculum and pedagogy.	a) Teacher education and the professional development of educators is included in Group 3904 Specialist studies in education.                                                                                                                                                                                                                                                                                                                                                                                              +\n b) Educational linguistics is included in Group 4704 Linguistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n c) Engineering education is included in Group 4010 Engineering practice and education.	1
3904	Specialist studies in education	This group covers specialist studies in education.	a) Education systems, including Māori primary and early childhood education, are included in Group 3903 Education systems.                                                                                                                                                                                                                                                                                                                                                                                 +\n b) Curriculum and pedagogy relating to specific subject areas is included in Group 3901 Curriculum and pedagogy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n c) Economics of education are included in Group 3801 Applied economics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    +\n d) Education policy is included in Group 3903 Education systems.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           +\n e) Educational psychology is included in Group 5201 Applied and developmental psychology.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n f) Educational linguistics is included in Group 4704 Linguistics.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          +\n g) History and philosophy of education is included in Group 5002 History and philosophy of specific fields.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                +\n h) Teacher education and professional development of educators is included in Group 3903 Education systems.	1
320204	Clinimetrics			1
320202	Clinical chemistry (incl. diagnostics)			1
520101	Child and adolescent development			1
450519	Aboriginal and Torres Strait Islander perspectives			1
450999	Ngā mātauranga taiao o te Māori kāore anō kia whakarōpūtia i wāhi kē (Māori environmental knowledges not elsewhere classified)			1
451099	Te hauora me te oranga o te Māori kāore anō kia whakarōpūhia i wāhi kē (Māori health and wellbeing not elsewhere classified)			1
480404	Law and religion			1
420399	Health services and systems not elsewhere classified			1
320603	Medical molecular engineering of nucleic acids and proteins			1
330405	Public participation and community engagement			1
400799	Control engineering, mechatronics and robotics not elsewhere classified			1
401204	Computational methods in fluid flow, heat and mass transfer (incl. computational fluid dynamics)			1
3009	Veterinary sciences	This group covers veterinary sciences practised on livestock for primary production, domestic, sporting and other uses.	a) Field procedures to protect animals against pests and pathogens are included in Group 3003 Animal production.                                                                                                                                                                                                                                                                                                                                  +\n b) Biological or life sciences associated with human health and medicine are included in Division 31 Biological sciences.	1
3007	Forestry sciences	This group covers forestry sciences and silviculture.	a) Management of forests in natural environments other than for forestry production is included in Group 4104 Environmental management.	1
5203	Clinical and health psychology	This group covers clinical and health psychology. It includes the detection, aetiology, treatment and prevention of mental health problems and psychological and behavioural processes in health and illness.	a) Psychiatry where chemical interventions are employed to modify behaviour is included in 3202 Clinical sciences.                                                                                                                                                                                                                               +\n b) Research investigating mental health services is included in the Group 4203 Health services and systems.	1
3003	Animal production	This group covers the sciences and technologies supporting the primary production of livestock, including birds and poultry.                                                                                                                                                                                                                                                                                                                                                                                                                                                  +\n It includes:                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               +\n • bees and apiary culture;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 +\n • egg production; and                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n • animal production for domestic or sporting uses.	a) The culture and production of marine and freshwater animals, including fish, crustaceans and shellfish, is included in Group 3005 Fisheries sciences.                                                                                                                                                                                                                                                                                                                                                                                              +\n b) Veterinary sciences is included in Group 3009 Veterinary sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      +\n c) Food sciences, including food processing and packaging, is included in Group 3006 Food sciences.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        +\n d) Genetic engineering and cloning of livestock is included in Group 3001 Agricultural biotechnology.	1
4399	Other history, heritage and archaeology	This group covers history, heritage and archaeology not elsewhere classified.		1
5205	Social and personality psychology	This group covers social and personality psychology. The study of the manner in which social groups influence the motivations, attitudes, personality and behaviour of the individual and the importance of the ways that individuals differ on those factors.	a) Neuroscientific underpinnings of interpersonal, individual-group and intergroup interactions are included in the Group 5202 Biological psychology.	1
\.


--
-- Data for Name: subject_type_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.subject_type_schema (id, uri) FROM stdin;
1	https://linked.data.gov.au/def/anzsrc-for/2020/
\.


--
-- Data for Name: title_type; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.title_type (schema_id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/alternative.json
1	https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json
\.


--
-- Data for Name: title_type_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.title_type_schema (id, uri) FROM stdin;
1	https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1/
\.


--
-- Data for Name: token; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.token (name, environment, date_created, token, s3_export) FROM stdin;
\.


--
-- Data for Name: traditional_knowledge_label; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.traditional_knowledge_label (schema_id, uri) FROM stdin;
1	https://localcontexts.org/label/tk-attribution/
1	https://localcontexts.org/label/tk-clan/
1	https://localcontexts.org/label/tk-family/
1	https://localcontexts.org/label/tk-multiple-communities/
1	https://localcontexts.org/label/tk-community-voice/
1	https://localcontexts.org/label/tk-creative/
1	https://localcontexts.org/label/tk-verified/
1	https://localcontexts.org/label/tk-non-verified/
1	https://localcontexts.org/label/tk-seasonal/
1	https://localcontexts.org/label/tk-women-general/
1	https://localcontexts.org/label/tk-men-general/
1	https://localcontexts.org/label/tk-men-restricted/
1	https://localcontexts.org/label/tk-women-restricted/
1	https://localcontexts.org/label/tk-culturally-sensitive/
1	https://localcontexts.org/label/tk-secret-sacred/
1	https://localcontexts.org/label/tk-commercial/
1	https://localcontexts.org/label/tk-non-commercial/
1	https://localcontexts.org/label/tk-community-use-only/
1	https://localcontexts.org/label/tk-outreach/
1	https://localcontexts.org/label/tk-open-to-collaboration/
2	https://localcontexts.org/label/bc-provenance/
2	https://localcontexts.org/label/bc-multiple-communities/
2	https://localcontexts.org/label/bc-clan/
2	https://localcontexts.org/label/bc-consent-verified/
2	https://localcontexts.org/label/bc-consent-non-verified/
2	https://localcontexts.org/label/bc-research-use/
2	https://localcontexts.org/label/bc-open-to-collaboration/
2	https://localcontexts.org/label/bc-open-to-commercialization/
2	https://localcontexts.org/label/bc-outreach/
2	https://localcontexts.org/label/bc-non-commercial/
\.


--
-- Data for Name: traditional_knowledge_label_schema; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.traditional_knowledge_label_schema (id, uri) FROM stdin;
1	https://localcontexts.org/labels/traditional-knowledge-labels/
2	https://localcontexts.org/labels/biocultural-labels/
\.


--
-- Data for Name: user_authz_request; Type: TABLE DATA; Schema: api_svc; Owner: postgres
--

COPY api_svc.user_authz_request (id, status, service_point_id, email, client_id, id_provider, subject, responding_user, approved_user, description, date_requested, date_responded) FROM stdin;
\.


--
-- Name: access_type_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.access_type_scheme_id_seq', 1, true);


--
-- Name: app_user_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.app_user_id_seq', 1000000000, false);


--
-- Name: contributor_position_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.contributor_position_scheme_id_seq', 1, true);


--
-- Name: contributor_role_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.contributor_role_scheme_id_seq', 1, true);


--
-- Name: description_type_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.description_type_scheme_id_seq', 1, true);


--
-- Name: language_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.language_scheme_id_seq', 1, true);


--
-- Name: organisation_role_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.organisation_role_scheme_id_seq', 1, true);


--
-- Name: related_object_category_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.related_object_category_scheme_id_seq', 1, true);


--
-- Name: related_object_type_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.related_object_type_scheme_id_seq', 1, true);


--
-- Name: related_raid_type_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.related_raid_type_scheme_id_seq', 1, true);


--
-- Name: service_point_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.service_point_id_seq', 20000004, true);


--
-- Name: subject_type_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.subject_type_scheme_id_seq', 1, true);


--
-- Name: title_type_scheme_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.title_type_scheme_id_seq', 1, true);


--
-- Name: traditional_knowledge_label_schema_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.traditional_knowledge_label_schema_id_seq', 2, true);


--
-- Name: user_authz_request_id_seq; Type: SEQUENCE SET; Schema: api_svc; Owner: postgres
--

SELECT pg_catalog.setval('api_svc.user_authz_request_id_seq', 30000000, false);


--
-- Name: access_type access_type_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.access_type
    ADD CONSTRAINT access_type_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: access_type_schema access_type_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.access_type_schema
    ADD CONSTRAINT access_type_schema_pkey PRIMARY KEY (id);


--
-- Name: app_user app_user_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);


--
-- Name: contributor_position contributor_position_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_position
    ADD CONSTRAINT contributor_position_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: contributor_position_schema contributor_position_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_position_schema
    ADD CONSTRAINT contributor_position_schema_pkey PRIMARY KEY (id);


--
-- Name: contributor_role contributor_role_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_role
    ADD CONSTRAINT contributor_role_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: contributor_role_schema contributor_role_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_role_schema
    ADD CONSTRAINT contributor_role_schema_pkey PRIMARY KEY (id);


--
-- Name: description_type description_type_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.description_type
    ADD CONSTRAINT description_type_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: description_type_schema dwscription_type_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.description_type_schema
    ADD CONSTRAINT dwscription_type_schema_pkey PRIMARY KEY (id);


--
-- Name: language_schema language_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.language_schema
    ADD CONSTRAINT language_schema_pkey PRIMARY KEY (id);


--
-- Name: organisation_role organisation_role_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.organisation_role
    ADD CONSTRAINT organisation_role_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: organisation_role_schema organisation_role_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.organisation_role_schema
    ADD CONSTRAINT organisation_role_schema_pkey PRIMARY KEY (id);


--
-- Name: raid raid_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.raid
    ADD CONSTRAINT raid_pkey PRIMARY KEY (handle);


--
-- Name: raido_operator raido_operator_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.raido_operator
    ADD CONSTRAINT raido_operator_pkey PRIMARY KEY (email);


--
-- Name: related_object_category related_object_category_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_category
    ADD CONSTRAINT related_object_category_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: related_object_category_schema related_object_category_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_category_schema
    ADD CONSTRAINT related_object_category_schema_pkey PRIMARY KEY (id);


--
-- Name: related_object_type related_object_type_new_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_type
    ADD CONSTRAINT related_object_type_new_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: related_object_type_schema related_object_type_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_type_schema
    ADD CONSTRAINT related_object_type_schema_pkey PRIMARY KEY (id);


--
-- Name: related_raid_type related_raid_type_new_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_raid_type
    ADD CONSTRAINT related_raid_type_new_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: related_raid_type_schema related_raid_type_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_raid_type_schema
    ADD CONSTRAINT related_raid_type_schema_pkey PRIMARY KEY (id);


--
-- Name: service_point service_point_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.service_point
    ADD CONSTRAINT service_point_pkey PRIMARY KEY (id);


--
-- Name: subject_type subject_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.subject_type
    ADD CONSTRAINT subject_pkey PRIMARY KEY (id);


--
-- Name: subject_type_schema subject_type_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.subject_type_schema
    ADD CONSTRAINT subject_type_schema_pkey PRIMARY KEY (id);


--
-- Name: title_type title_type_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.title_type
    ADD CONSTRAINT title_type_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: title_type_schema title_type_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.title_type_schema
    ADD CONSTRAINT title_type_schema_pkey PRIMARY KEY (id);


--
-- Name: token token_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.token
    ADD CONSTRAINT token_pkey PRIMARY KEY (name, environment, date_created);


--
-- Name: traditional_knowledge_label traditional_knowledge_label_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.traditional_knowledge_label
    ADD CONSTRAINT traditional_knowledge_label_pkey PRIMARY KEY (schema_id, uri);


--
-- Name: traditional_knowledge_label_schema traditional_knowledge_label_schema_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.traditional_knowledge_label_schema
    ADD CONSTRAINT traditional_knowledge_label_schema_pkey PRIMARY KEY (id);


--
-- Name: service_point unique_name; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.service_point
    ADD CONSTRAINT unique_name UNIQUE (lower_name);


--
-- Name: CONSTRAINT unique_name ON service_point; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON CONSTRAINT unique_name ON api_svc.service_point IS 'initially for uniqueness rather than querying, we want to retain the case
  entered by the user';


--
-- Name: user_authz_request user_authz_request_pkey; Type: CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.user_authz_request
    ADD CONSTRAINT user_authz_request_pkey PRIMARY KEY (id);


--
-- Name: app_user_id_fields_active_key; Type: INDEX; Schema: api_svc; Owner: postgres
--

CREATE UNIQUE INDEX app_user_id_fields_active_key ON api_svc.app_user USING btree (email, client_id, subject) WHERE (enabled = true);


--
-- Name: INDEX app_user_id_fields_active_key; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON INDEX api_svc.app_user_id_fields_active_key IS 'a user can only be attached to one service point at a time.  We could change
  this, but would need to change sign-in to specify service-point';


--
-- Name: idx_raid_service_point_id_date_created; Type: INDEX; Schema: api_svc; Owner: postgres
--

CREATE INDEX idx_raid_service_point_id_date_created ON api_svc.raid USING btree (service_point_id, date_created DESC);


--
-- Name: INDEX idx_raid_service_point_id_date_created; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON INDEX api_svc.idx_raid_service_point_id_date_created IS 'created because load-testing showed the query for the experimental list-raid endpoint was doing a full table scan';


--
-- Name: user_authz_request_once_active_key; Type: INDEX; Schema: api_svc; Owner: postgres
--

CREATE UNIQUE INDEX user_authz_request_once_active_key ON api_svc.user_authz_request USING btree (service_point_id, client_id, subject) WHERE (status = 'REQUESTED'::api_svc.auth_request_status);


--
-- Name: INDEX user_authz_request_once_active_key; Type: COMMENT; Schema: api_svc; Owner: postgres
--

COMMENT ON INDEX api_svc.user_authz_request_once_active_key IS 'a user can only make one active request per service point';


--
-- Name: app_user app_user_service_point_id_fkey; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.app_user
    ADD CONSTRAINT app_user_service_point_id_fkey FOREIGN KEY (service_point_id) REFERENCES api_svc.service_point(id);


--
-- Name: access_type fk_access_type_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.access_type
    ADD CONSTRAINT fk_access_type_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.access_type_schema(id);


--
-- Name: contributor_position fk_contributor_position_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_position
    ADD CONSTRAINT fk_contributor_position_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.access_type_schema(id);


--
-- Name: contributor_role fk_contributor_role_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.contributor_role
    ADD CONSTRAINT fk_contributor_role_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.contributor_role_schema(id);


--
-- Name: description_type fk_description_type_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.description_type
    ADD CONSTRAINT fk_description_type_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.description_type_schema(id);


--
-- Name: language fk_language_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.language
    ADD CONSTRAINT fk_language_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.language_schema(id);


--
-- Name: organisation_role fk_organisation_role_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.organisation_role
    ADD CONSTRAINT fk_organisation_role_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.organisation_role_schema(id);


--
-- Name: related_object_category fk_related_object_category_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_category
    ADD CONSTRAINT fk_related_object_category_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.related_object_category_schema(id);


--
-- Name: related_object_type fk_related_object_type_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_object_type
    ADD CONSTRAINT fk_related_object_type_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.related_object_type_schema(id);


--
-- Name: related_raid_type fk_related_raid_type_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.related_raid_type
    ADD CONSTRAINT fk_related_raid_type_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.related_raid_type_schema(id);


--
-- Name: subject_type fk_subject_type_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.subject_type
    ADD CONSTRAINT fk_subject_type_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.subject_type_schema(id);


--
-- Name: title_type fk_title_type_schema_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.title_type
    ADD CONSTRAINT fk_title_type_schema_id FOREIGN KEY (schema_id) REFERENCES api_svc.title_type_schema(id);


--
-- Name: traditional_knowledge_label fk_traditional_knowledge_label_id; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.traditional_knowledge_label
    ADD CONSTRAINT fk_traditional_knowledge_label_id FOREIGN KEY (schema_id) REFERENCES api_svc.traditional_knowledge_label_schema(id);


--
-- Name: raid raid_service_point_id_fkey; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.raid
    ADD CONSTRAINT raid_service_point_id_fkey FOREIGN KEY (service_point_id) REFERENCES api_svc.service_point(id);


--
-- Name: user_authz_request user_authz_request_approved_user_fkey; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.user_authz_request
    ADD CONSTRAINT user_authz_request_approved_user_fkey FOREIGN KEY (approved_user) REFERENCES api_svc.app_user(id);


--
-- Name: user_authz_request user_authz_request_responding_user_fkey; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.user_authz_request
    ADD CONSTRAINT user_authz_request_responding_user_fkey FOREIGN KEY (responding_user) REFERENCES api_svc.app_user(id);


--
-- Name: user_authz_request user_authz_request_service_point_id_fkey; Type: FK CONSTRAINT; Schema: api_svc; Owner: postgres
--

ALTER TABLE ONLY api_svc.user_authz_request
    ADD CONSTRAINT user_authz_request_service_point_id_fkey FOREIGN KEY (service_point_id) REFERENCES api_svc.service_point(id);


--
-- Name: SCHEMA api_svc; Type: ACL; Schema: -; Owner: postgres
--


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: api_svc; Owner: postgres
--



--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: api_svc; Owner: postgres
--


--
-- PostgreSQL database dump complete
--

