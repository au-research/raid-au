DROP TABLE IF EXISTS raido.subject;
CREATE TABLE IF NOT EXISTS raido.subject (
    id varchar(6) not null,
    name text not null,
    description text,
    note text,
    primary key (id)
);