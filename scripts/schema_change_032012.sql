-- ignore this file. The changes should have been incorporated into the 
-- pgsql_tables.sql file. -- CJ

alter table resourcetype drop constraint resourcetype_chk;

alter table resourcetype add  CONSTRAINT resourcetype_chk CHECK (rtype IN ('a', 'b', 'c', 'd', 'e', 'f', 'h', 
  'i', 'k', 'l', 'm', 'n', 'o', 'p', 'r', 't', 'u', 'v', 'x', 'y'));

alter table ontologyuri add column document_uri text;

alter table ontologyuri alter column nsid drop not null;

alter table individual alter column nsid drop not null;

alter table individual add column is_named boolean default true;

alter table property add column is_reflexive boolean default false;

alter table domain add column type char(1);

alter table domain add constraint domain_type_chk CHECK (type in ('a', 'd', 'o'));

alter table range add column type char(1);

alter table range add constraint range_type_chk CHECK (type in ('a', 'd', 'o'));

alter table cardinalityclass add column type char(1) check (type in ('d', 'o'));

alter table mincardinalityclass add column type char(1) check (type in ('d', 'o'));

alter table maxcardinalityclass add column type char(1) check (type in ('d', 'o'));

alter table somevaluesfromclass add column type char(1) check (type in ('d', 'o'));

alter table allvaluesfromclass add column type char(1) check (type in ('d', 'o'));

alter table hasvalue add column type char(1) check (type in ('d', 'o'));

alter table intersectionclass add column type char(1) check (type in ('d', 'o'));

CREATE TABLE hasself (
  id serial PRIMARY KEY,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
);

CREATE INDEX hasself_idx1 ON hasvalue (propertyid);

alter table subpropertyof add column type char(1) check (type in ('a', 'd', 'o'));

alter table equivalentclass add column type char(1) check (type in ('c', 'd', 'o'));

alter table disjointclass add column type char(1) check (type in ('c', 'd', 'o'));

alter table unionclass add column type char(1) check (type in ('d', 'o'));

alter table oneof add column type char(1) check (type in ('d', 'o'));

alter table complementofclass add column type char(1) check (type in ('d', 'o'));

INSERT INTO resourcetype VALUES (21, 'datatype_restriction', 'e');

CREATE SEQUENCE datatype_restriction_seq;

CREATE TABLE datatype_restriction (
  id integer NOT NULL,
  dtid integer NOT NULL REFERENCES datatype (id) ON DELETE CASCADE,
  facetPropId integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  facetIRI varchar(100), 
  literal_id integer NOT NULL REFERENCES literal (id) ON DELETE CASCADE,  
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  browsertext text  
);

CREATE INDEX dt_res_idx1 ON datatype_restriction (id);

CREATE INDEX dt_res_idx2 ON datatype_restriction (dtid);

CREATE INDEX dt_res_idx3 ON datatype_restriction (facetIRI);

CREATE INDEX dt_res_idx4 ON datatype_restriction (kbid);
