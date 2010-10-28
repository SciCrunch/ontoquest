--DROP TYPE IF EXISTS edge1;

--CREATE TYPE edge1 AS (rid1 INTEGER, rtid1 INTEGER, name1 TEXT, 
--    rid2 INTEGER, rtid2 INTEGER, name2 TEXT);

DROP TYPE IF EXISTS edge2 CASCADE;

CREATE TYPE edge2 AS (rid1 INTEGER, rtid1 INTEGER, name1 TEXT, 
     rid2 INTEGER, rtid2 INTEGER, name2 TEXT, pid INTEGER, pname TEXT);

DROP TYPE IF EXISTS node1 CASCADE;

CREATE TYPE node1 AS (rid INTEGER, rtid INTEGER, name TEXT);

DROP TYPE IF EXISTS node2 CASCADE;

CREATE TYPE node2 AS (rid INTEGER, rtid INTEGER);

DROP TYPE IF EXISTS idx_node CASCADE;

CREATE TYPE idx_node AS (idx VARCHAR(511));


DROP TYPE IF EXISTS idx_edge CASCADE;

CREATE TYPE idx_edge AS (idx1 VARCHAR(511), idx2 VARCHAR(511));

-- used in computeDAGIndex script
DROP TYPE IF EXISTS id_typ CASCADE;

CREATE TYPE id_typ AS (id INTEGER);

DROP AGGREGATE IF EXISTS str_agg (text);

CREATE AGGREGATE str_agg(
  basetype    = text,
  sfunc       = textcat,
  stype       = text,
  initcond    = ''
);
