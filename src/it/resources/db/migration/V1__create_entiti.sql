CREATE SEQUENCE entities_id_seq;

CREATE TABLE entities (
    id integer NOT NULL DEFAULT nextval('entities_id_seq'),
	name varchar,
	state varchar
);

ALTER SEQUENCE entities_id_seq OWNED BY entities.id;