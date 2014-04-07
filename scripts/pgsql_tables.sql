/* 
Changes from IODT schema:
1. Add kbid into each table. There is no need to create a whole set of tables for each ontology. 
   It makes easier to create inter-ontology relationships. 
2. No longer use id range to identify class type. For each relationship (OWL-defined or 
   user-defined), add a column 'ttid' to explicitly specify the tabletype. ttid references tabletype.id. The list 
   of tabletypes are stored in table 'tabletype'.
3. Merge PROPERTY and PROPERTYURL table into PROPERTY table.
4. Use namespace to manage ontology files and versions.
5. Add NegativeObjectPropertyAssertion and NegativeDataPropertyAssertion in OWL 1.1
6. Add namespace table. For every class, add nsid.
*/

-- A utility function for dropping schema objects without exception

create or replace function drop_if_exists (name text) returns INTEGER AS '
DECLARE
  reltype varchar(16);
  relcount int;
BEGIN
  select case when relkind=''r'' then ''table'' 
              when relkind=''i'' then ''index'' 
              when relkind=''S'' then ''sequence''
              when relkind=''v'' then ''view''
              when relkind=''c'' then ''type''
              when relkind=''t'' then ''table'' end, 
         count(*) into reltype, relcount from pg_class c, pg_user u where relname=name 
         and c.relowner = u.usesysid and u.usename = current_user group by relkind;
  
  IF (relcount > 0) THEN EXECUTE ''DROP '' || reltype || '' '' || name || '' CASCADE'';   
    RETURN 1;
  END IF;
  RETURN 0;
END;
'
language plpgsql;

--commit;

select drop_if_exists('equivalentclassgroup');

select drop_if_exists('oneof_seq');

select drop_if_exists('union_seq');

select drop_if_exists('intersection_seq');

select drop_if_exists('alldifferent_seq');

select drop_if_exists('datarange_seq');

select drop_if_exists('classes');
 
select drop_if_exists('resource_view');

select drop_if_exists('propertyMap');

select drop_if_exists('differentindividual');

select drop_if_exists('alldifferentindividual');

select drop_if_exists('sameindividual');

select drop_if_exists('negative_data_prop');

select drop_if_exists('negative_obj_prop');
 
select drop_if_exists('typeof');
 
select drop_if_exists('relationship');
 
select drop_if_exists('domain');
 
select drop_if_exists('range');
 
select drop_if_exists('subclassof');

select drop_if_exists('equivalentclass');

select drop_if_exists('equivalentproperty');

select drop_if_exists('subpropertyof');
 
select drop_if_exists('disjointclass');
 
select drop_if_exists('inversepropertyof');

select drop_if_exists('datatype_restriction');

select drop_if_exists('maxcardinalityclass');
 
select drop_if_exists('mincardinalityclass');
 
select drop_if_exists('cardinalityclass');
 
select drop_if_exists('allvaluesfromclass');
 
select drop_if_exists('somevaluesfromclass');
 
select drop_if_exists('complementclass');
 
select drop_if_exists('intersectionclass');
 
select drop_if_exists('unionclass');
 
select drop_if_exists('datarange');
 
select drop_if_exists('oneof');
 
select drop_if_exists('literal');

select drop_if_exists('datatype');
 
select drop_if_exists('hasvalue');

select drop_if_exists('hasself');

select drop_if_exists('primitiveclass');
 
select drop_if_exists('property');
 
select drop_if_exists('individual');

  
--select drop_if_exists('rdflist');

select drop_if_exists('resourcetype');

--select drop_if_exists('resources');

select drop_if_exists('ontologyfiles');

select drop_if_exists('ontologyimport');

select drop_if_exists('ontologyuri');

select drop_if_exists('namespace');

select drop_if_exists('kb');

select drop_if_exists('disjointunionclass');

-- **************************
-- SEQUENCES
-- **************************
CREATE SEQUENCE oneof_seq;

CREATE SEQUENCE union_seq;

CREATE SEQUENCE intersection_seq;

CREATE SEQUENCE alldifferent_seq;

CREATE SEQUENCE datarange_seq;

CREATE SEQUENCE datatype_restriction_seq;

-- **************************
-- META TABLES
-- **************************
CREATE TABLE kb (
  id serial PRIMARY KEY,
  name varchar(255) NOT NULL,
  creation_date timestamp with time zone default current_timestamp
);

CREATE INDEX kb_idx1 ON kb (name);

-- namespace: for default namespace, prefix is blank (null).
CREATE TABLE namespace (
  id serial PRIMARY KEY,
  prefix varchar(255),
  url text,
  is_internal boolean default false, -- not declared by owl file, used internal only
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX ns_idx1 ON namespace (prefix);

CREATE INDEX ns_idx2 ON namespace (url);

CREATE INDEX ns_idx3 ON namespace (kbid);


CREATE TABLE ontologyuri
(
  id serial NOT NULL,
  uri text NOT NULL,
  is_default boolean,
  nsid integer,
  kbid integer NOT NULL,
  browsertext text,
  document_uri text,
  owl_content text,
  owl_file_length bigint,
  version_iri character varying(500),
  CONSTRAINT ontologyuri_pkey PRIMARY KEY (id),
  CONSTRAINT ontologyuri_kbid_fkey FOREIGN KEY (kbid)
      REFERENCES kb (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT ontologyuri_nsid_fkey FOREIGN KEY (nsid)
      REFERENCES namespace (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);

CREATE INDEX ontologyuri_idx1 ON ontologyuri (kbid);

CREATE INDEX ontologyuri_idx2 ON ontologyuri (lower(uri));

CREATE INDEX ontologyuri_idx3 ON ontologyuri (is_default);

CREATE INDEX ontologyuri_idx4 ON ontologyuri (nsid);

CREATE TABLE ontologyimport (
  ont_id integer NOT NULL REFERENCES ontologyuri (id) ON DELETE CASCADE,
  imported varchar(255), -- imported ontology uri
  imported_ont_id integer REFERENCES ontologyuri (id) ON DELETE SET NULL,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
);

CREATE INDEX ont_import_idx1 ON ontologyimport (ont_id);

CREATE INDEX ont_import_idx2 ON ontologyimport (imported);

CREATE INDEX ont_import_idx3 ON ontologyimport (kbid);

/*
CREATE TABLE resources (
  id serial PRIMARY KEY,
  name text NOT NULL,
  nsid integer NOT NULL REFERENCES namespace (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX resources_idx1 ON resources (kbid);

CREATE INDEX resources_idx2 ON resources (name);

CREATE INDEX resources_idx3 ON resources (nsid);


-- An ontology is described in one or more files. This table is used to record the information of ontology
-- files. It is assumed that each file has distinct namespace. The table is useful to reconstruct ontology
-- files from the database and track updates (sort of version control).
CREATE TABLE ontologyfiles (
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  nsid integer NOT NULL REFERENCES namespace (id) ON DELETE CASCADE,
  filename varchar(255)
);

CREATE INDEX ontfile_idx1 ON ontologyfiles (kbid);

CREATE INDEX ontfile_idx2 ON ontologyfiles (nsid);
*/

-- Store names of tables and their types. For example, tables with class type (rtype=c) are: primitiveclass, 
-- unionclass, allvaluesfromclass, somevaluesfromclass, cardinalityclass, etc.;
-- This is a static table with fixed values. The values are loaded at the end of this script. Since the values 
-- are fixed, I use integer, instead of serial, as the data type of id.
-- tables with individual type (rtype=i) are individual;
-- Valid ttypes are: c - class, i - individual, l - literal, d - datatype, p - property, r - datarange, s - resources
CREATE TABLE resourcetype (
  id integer PRIMARY KEY, 
  name varchar(64) NOT NULL,
  rtype varchar(1) NOT NULL,
  CONSTRAINT resourcetype_chk CHECK (rtype IN ('a', 'b', 'c', 'd', 'e', 'f', 'h', 
  'i', 'k', 'l', 'm', 'n', 'o', 'p', 'r', 't', 'u', 'v', 'x', 'y'))
);

CREATE INDEX resourcetype_idx1 ON resourcetype (name);

CREATE INDEX resourcetype_idx2 ON resourcetype (rtype);

-- ***************************
-- PROPERTY-RELATED TABLES
-- ***************************

-- inverse property is stored separately in inversepropertyof table
CREATE TABLE property (
  id serial PRIMARY KEY,
  name text NOT NULL,
  hashcode integer,
  is_object boolean default true,
  is_transitive boolean default false,
  is_symmetric boolean default false,
  is_functional boolean default false,
  is_inversefunctional boolean default false,
  is_datatype boolean default false,  -- Is dataProperty?
  is_owl boolean default true, -- is owl property or rdf:Property?
  is_annotation boolean default false, -- is annotation property?
  is_system boolean default false, -- protege-related field
  is_deprecated boolean default false, -- is owl:DeprecatedProperty?
  nsid integer REFERENCES namespace (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
  , browsertext text
  , is_reflexive boolean default false
);

CREATE INDEX property_idx1 ON property (name);

CREATE INDEX property_idx2 ON property (lower(name));

CREATE INDEX property_idx3 ON property (kbid);

CREATE INDEX property_idx4 ON property (nsid);

CREATE INDEX property_idx5 ON property (hashcode);

--CREATE INDEX property_idx5 ON property (is_object);

--CREATE INDEX property_idx6 ON property (is_transitive);

--CREATE INDEX property_idx7 ON property (is_symmetric);

--CREATE INDEX property_idx8 ON property (is_functional);

--CREATE INDEX property_idx9 ON property (is_inversefunctional);

--CREATE INDEX property_idx10 ON property (is_datatype);

CREATE INDEX property_idx11 ON property (is_system);

-- owl:inverseOf
CREATE TABLE inversepropertyof (
  propertyid1 integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  propertyid2 integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
);

CREATE INDEX inv_prop_idx1 ON inversepropertyof (propertyid1);

CREATE INDEX inv_prop_idx2 ON inversepropertyof (propertyid2);

CREATE INDEX inv_prop_idx3 ON inversepropertyof (kbid);

-- rdfs:subPropertyOf
CREATE TABLE subpropertyof (
  childid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  parentid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
  , type char(1) check (type in ('a', 'd', 'o'))
);

CREATE INDEX sub_prop_idx1 ON subpropertyof (childid);

CREATE INDEX sub_prop_idx2 ON subpropertyof (parentid);

CREATE INDEX sub_prop_idx3 ON subpropertyof (kbid);

-- owl:equivalentProperty
CREATE TABLE equivalentproperty (
  propertyid1 integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  propertyid2 integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
);

CREATE INDEX eq_prop_idx1 ON equivalentproperty (propertyid1);

CREATE INDEX eq_prop_idx2 ON equivalentproperty (propertyid2);

CREATE INDEX eq_prop_idx3 ON equivalentproperty (kbid);

-- ********************************
-- END OF PROPERTY-RELATED TABLES
-- ********************************

-- ********************************
-- INDIVIDUAL, DATATYPE, LITERAL, LIST
-- ********************************
CREATE TABLE individual (
  id serial PRIMARY KEY,
  name text,   -- removed the not null constraint - CJ
  hashcode integer,
  is_owl boolean default true, -- is owl individual or rdfs individual?
  nsid integer REFERENCES namespace (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
  , is_named boolean default true  -- is named or anonymous individual
);

CREATE INDEX individual_idx1 ON individual (hashcode);

CREATE INDEX individual_idx2 ON individual (lower(name));

CREATE INDEX individual_idx3 ON individual (kbid);

CREATE INDEX individual_idx4 ON individual (nsid);

CREATE TABLE datatype (
  id serial PRIMARY KEY,
  name text NOT NULL,
  hashcode integer,
  nsid integer NOT NULL REFERENCES namespace (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
);

CREATE INDEX datatype_idx1 ON datatype (hashcode);

CREATE INDEX datatype_idx2 ON datatype (lower(name));

CREATE INDEX datatype_idx3 ON datatype (kbid);

CREATE INDEX datatype_idx4 ON datatype (nsid);

CREATE TABLE literal (
  id serial PRIMARY KEY,
  lexicalform text NOT NULL,
  langtag varchar(100),
  datatypeid integer NOT NULL REFERENCES datatype (id) ON DELETE CASCADE,
  hashcode integer,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
  , browsertext text
);

CREATE INDEX literal_idx2 ON literal (datatypeid);

--removed by Jing Chen, because some literals are too long and btree indexes are not allow on the in postgresql
--CREATE INDEX literal_idx3 ON literal (lower(lexicalform));

CREATE INDEX literal_idx4 ON literal (kbid);

/*
CREATE TABLE rdflist (
  id integer NOT NULL,
  position integer NOT NULL,
  elementid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX rdflist_idx1 ON rdflist (id);

CREATE INDEX rdflist_idx2 ON rdflist (elementid, rtid);

CREATE INDEX rdflist_idx3 ON rdflist (kbid);
*/

-- ********************************
-- CLASSES AND RESTRICTIONS
-- ********************************

-- Restrictions are modeled as special classes, which can be used in other restrictions or relationships

CREATE TABLE primitiveclass (
  id serial PRIMARY KEY,
  name text NOT NULL,
  hashcode integer,
  is_system boolean default false,  -- Based on version 3.5.5 of OWLAPI package, this flag is true when the URI is owl:Thing or owl:Nothing
  is_owlclass boolean default true, -- is owl:Class (true) or rdfs:Class (false)?
  is_deprecated boolean default false, -- is owl:DeprecatedClass?
  nsid integer REFERENCES namespace (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
);

CREATE INDEX prim_class_idx1 ON primitiveclass (name);

CREATE INDEX prim_class_idx2 ON primitiveclass (hashcode);

CREATE INDEX prim_class_idx3 ON primitiveclass (kbid);

CREATE INDEX prim_class_idx4 ON primitiveclass (nsid);

CREATE INDEX prim_class_idx5 ON primitiveclass (is_system);

CREATE INDEX prim_class_idx6 ON primitiveclass (lower(name));

-- owl:allValuesFrom restriction
-- The combination of (rtid, rangeclassid) references a row in a particular class table.
CREATE TABLE allvaluesfromclass (
  id serial PRIMARY KEY,
  propertyid integer REFERENCES property (id) ON DELETE CASCADE,
  rangeclassid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
  , type char(1) check (type in ('d', 'o')) -- d: data restriction, o: object restriction
);

CREATE INDEX allvalues_idx1 ON allvaluesfromclass (propertyid);

CREATE INDEX allvalues_idx2 ON allvaluesfromclass (rtid, rangeclassid);

CREATE INDEX allvalues_idx3 ON allvaluesfromclass (kbid);

-- owl:someValuesFrom restriction
-- The combination of (rtid, rangeclassid) references a row in a particular class table.
CREATE TABLE somevaluesfromclass (
  id serial PRIMARY KEY,
  propertyid integer REFERENCES property (id) ON DELETE CASCADE,
  rangeclassid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
  , type char(1) check (type in ('d', 'o')) -- d: data restriction, o: object restriction
);

CREATE INDEX somevalues_idx1 ON somevaluesfromclass (propertyid);

CREATE INDEX somevalues_idx2 ON somevaluesfromclass (rtid, rangeclassid);

CREATE INDEX somevalues_idx3 ON somevaluesfromclass (kbid);

-- owl:cardinality
CREATE TABLE cardinalityclass (
  id serial PRIMARY KEY,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  cardinality integer NOT NULL,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  browsertext text,
  type char(1) check (type in ('d', 'o')),  -- d: dataCardinality o: objectCardinality
  constraint uk_card UNIQUE (id, propertyid, cardinality) 
);

CREATE INDEX cardinality_idx1 ON cardinalityclass (propertyid);

CREATE INDEX cardinality_idx2 ON cardinalityclass (cardinality);

CREATE INDEX cardinality_idx3 ON cardinalityclass (kbid);

-- owl:minCardinality
CREATE TABLE mincardinalityclass (
  id serial PRIMARY KEY,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  mincardinality integer NOT NULL,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE, 
  browsertext text,
  type char(1) check (type in ('d', 'o')),  -- d: dataCardinality o: objectCardinality
  constraint uk_mincard UNIQUE (id, propertyid, mincardinality) 
);

CREATE INDEX mincard_idx1 ON mincardinalityclass (propertyid);

CREATE INDEX mincard_idx2 ON mincardinalityclass (mincardinality);

CREATE INDEX mincard_idx3 ON mincardinalityclass (kbid);

-- owl:maxCardinality
CREATE TABLE maxcardinalityclass (
  id serial PRIMARY KEY,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  maxcardinality integer NOT NULL,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  browsertext text,
  type char(1) check (type in ('d', 'o')),  -- d: dataCardinality o: objectCardinality
  constraint uk_maxc UNIQUE (id, propertyid, maxcardinality)
);

CREATE INDEX maxcard_idx1 ON maxcardinalityclass (propertyid);

CREATE INDEX maxcard_idx2 ON maxcardinalityclass (maxcardinality);

CREATE INDEX maxcard_idx3 ON maxcardinalityclass (kbid);

-- owl:complementOf
CREATE TABLE complementclass (
  id serial PRIMARY KEY,
  complementclassid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
  , type char(1) check (type in ('d', 'o'))  -- d: dataCardinality o: objectCardinality
);

CREATE INDEX complement_idx1 ON complementclass (rtid, complementclassid);

CREATE INDEX complement_idx2 ON complementclass (kbid);

-- owl:intersectionOf
-- The combination of (classid, rtid) references a row in a particular class table
CREATE TABLE intersectionclass (
  id integer,
  classid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  browsertext text,
  type char(1) check (type in ('d', 'o')),  -- d: data o: object
  constraint uk_intersection UNIQUE (id, classid, rtid)
);

CREATE INDEX intersection_idx1 ON intersectionclass (id);

CREATE INDEX intersection_idx2 ON intersectionclass (rtid, classid);

CREATE INDEX intersection_idx3 ON intersectionclass (kbid);

-- owl:unionOf
-- The combination of (classid, rtid) references a row in a particular class table
CREATE TABLE unionclass (
  id integer NOT NULL,
  classid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  browsertext text,
  type char(1) check (type in ('d', 'o')),  -- d: data o: object
  constraint uk_union UNIQUE (id, classid, rtid)
);

CREATE INDEX union_idx1 ON unionclass (id);

CREATE INDEX union_idx2 ON unionclass (rtid, classid);

CREATE INDEX union_idx3 ON unionclass (kbid);

-- hasSelf
CREATE TABLE hasself (
  id serial PRIMARY KEY,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
);

CREATE INDEX hasself_idx1 ON hasself (propertyid);

-- owl:hasValue
CREATE TABLE hasvalue (
  id serial PRIMARY KEY,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  valueid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
  , type char(1) check (type in ('d', 'o'))  -- d: dataCardinality o: objectCardinality
);

CREATE INDEX hasvalue_idx1 ON hasvalue (propertyid);

CREATE INDEX hasvalue_idx2 ON hasvalue (rtid, valueid);

CREATE INDEX hasvalue_idx3 ON hasvalue (kbid);

-- owl:oneOf
CREATE TABLE oneof (
  id integer NOT NULL,
  valueid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
  , type char(1) check (type in ('d', 'o'))  -- d: dataCardinality o: objectCardinality
);

CREATE INDEX oneof_idx1 ON oneof (id);

CREATE INDEX oneof_idx2 ON oneof (rtid, valueid);

CREATE INDEX oneof_idx3 ON oneof (kbid);

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

-- ********************************
-- END OF CLASS AND RESTRICTIONS
-- ********************************

-- ******************************************
-- PROPERTY CHARACTERISTICS -- domain and range
-- ******************************************

-- rdfs:domain
-- The combination of (rtid, domainid) references a row in a particular class table.
CREATE TABLE domain (
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  domainid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
  , type char(1) CHECK (type in ('a', 'd', 'o'))
);

CREATE INDEX domain_idx1 ON domain (propertyid);

CREATE INDEX domain_idx2 ON domain (rtid, domainid);

CREATE INDEX domain_idx3 ON domain (kbid);

-- owl:dataRange
CREATE TABLE datarange (
  id integer NOT NULL,
  rangeid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , browsertext text
);

CREATE INDEX datarange_idx1 ON datarange (id);

CREATE INDEX datarange_idx2 ON datarange (rangeid, rtid);

CREATE INDEX datarange_idx3 ON datarange (kbid);

-- rdfs:range
-- rangeid can refer to one of the following categories: class (primitive or restriction class), datatype, datarange. 
-- The combination of (rtid, rangeid) references a row in a particular class/datatype/datarange table.
CREATE TABLE range (
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  rangeid integer NOT NULL,
  rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , type char(1) CHECK (type in ('a', 'd', 'o'))
);

CREATE INDEX range_idx1 ON range (propertyid);

CREATE INDEX range_idx2 ON range (rtid, rangeid);

CREATE INDEX range_idx3 ON range (kbid);

-- *********************************
-- END OF PROPERTY CHARACTERISTICS -- domain and range
-- *********************************

-- ******************
-- RELATIONSHIPS
-- ******************

-- owl:disjointWith
CREATE TABLE disjointclass (
  classid1 integer NOT NULL,
  rtid1 integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  classid2 integer NOT NULL,
  rtid2 integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , type char(1) check (type in ('c', 'd', 'o'))
);

CREATE INDEX disjoint_idx1 ON disjointclass (rtid1, classid1);

CREATE INDEX disjoint_idx2 ON disjointclass (rtid2, classid2);

-- owl:disjointunion
create table disjointunionclass (
  pclassid integer NOT NULL,
  prtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  cclassid integer NOT NULL,
  crtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  kbid  integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX disjointunion_idx1 ON disjointunionclass (pclassid, prtid);

CREATE INDEX disjointunion_idx2 ON disjointunionclass (cclassid, crtid);

-- rdfs:subClassOf
-- If DAG indexing mechanism is used to compute inference 
-- on the fly, column inferred can be removed.
CREATE TABLE subclassof (
  childid integer NOT NULL,
  child_rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  parentid integer NOT NULL,
  parent_rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX subclass_idx1 ON subclassof (child_rtid, childid);

CREATE INDEX subclass_idx2 ON subclassof (parent_rtid, parentid);

-- owl:equivalentClass
CREATE TABLE equivalentclass (
  classid1 integer NOT NULL,
  class_rtid1 integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  classid2 integer NOT NULL,
  class_rtid2 integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
  , type char(1) check (type in ('c', 'd', 'o'))
);

CREATE INDEX eqclass_idx1 ON equivalentclass (class_rtid1, classid1);

CREATE INDEX eqclass_idx2 ON equivalentclass (class_rtid2, classid2);

CREATE INDEX eqclass_idx3 ON equivalentclass (kbid);

-- user-defined relationship
-- If DAG indexing mechanism is used to compute inference 
-- on the fly, column inferred can be removed.
CREATE TABLE relationship (
  subjectid integer NOT NULL,
  subject_rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  objectid integer NOT NULL,
  object_rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX rel_idx1 ON relationship (subject_rtid, subjectid);

CREATE INDEX rel_idx2 ON relationship (object_rtid, objectid);

CREATE INDEX rel_idx3 ON relationship (inferred);

CREATE INDEX rel_idx4 ON relationship (kbid);

-- owl:differentFrom
CREATE TABLE differentindividual (
  individualid1 integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  individualid2 integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX diff_ind_idx1 ON differentindividual (individualid1);

CREATE INDEX diff_ind_idx2 ON differentindividual (individualid2);

CREATE INDEX diff_ind_idx3 ON differentindividual (kbid);

-- owl:allDifferent
CREATE TABLE alldifferentindividual (
  id integer NOT NULL,
  individualid integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE
  , browsertext text
);

CREATE INDEX alldiff_ind_idx1 ON alldifferentindividual (id);

CREATE INDEX alldiff_ind_idx2 ON alldifferentindividual (individualid);

CREATE INDEX alldiff_ind_idx3 ON alldifferentindividual (kbid);

-- owl:sameAs
CREATE TABLE sameindividual (
  individualid1 integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  individualid2 integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX sameind_idx1 ON sameindividual (individualid1);

CREATE INDEX sameind_idx2 ON sameindividual (individualid2);

CREATE INDEX sameind_idx3 ON sameindividual (kbid);

-- NegativeObjectPropertyAssertion in OWL 1.1
CREATE TABLE negative_obj_prop (
  individualid1 integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  individualid2 integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX neg_obj_prop_idx1 ON negative_obj_prop (individualid1);

CREATE INDEX neg_obj_prop_idx2 ON negative_obj_prop (individualid2);

CREATE INDEX neg_obj_prop_idx3 ON negative_obj_prop (propertyid);

CREATE INDEX neg_obj_prop_idx4 ON negative_obj_prop (kbid);

-- NegativeDataPropertyAssertion in OWL 1.1 
CREATE TABLE negative_data_prop (
  individualid integer NOT NULL REFERENCES individual (id) ON DELETE CASCADE,
  literalid integer NOT NULL REFERENCES literal (id) ON DELETE CASCADE,
  propertyid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX neg_data_prop_idx1 ON negative_data_prop (individualid);

CREATE INDEX neg_data_prop_idx2 ON negative_data_prop (literalid);

CREATE INDEX neg_data_prop_idx3 ON negative_data_prop (propertyid);

CREATE INDEX neg_data_prop_idx4 ON negative_data_prop (kbid);


CREATE TABLE equivalentclassgroup
(
  id serial ,
  rid bigint,
  ridm bigint,
  kbid integer,
  rid_is_obsolete boolean,
  CONSTRAINT equivalentclassgroup_pkey PRIMARY KEY (id),
  CONSTRAINT equivalentclassgroup_kbid_fkey FOREIGN KEY (kbid)
      REFERENCES kb (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT equivalentclassgroup_rid_fkey FOREIGN KEY (rid)
      REFERENCES primitiveclass (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT equivalentclassgroup_ridm_fkey FOREIGN KEY (ridm)
      REFERENCES primitiveclass (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT equivalentclassgroup_ridm_key UNIQUE (ridm)
);


CREATE INDEX equivalentclassgroup_kbid_idx
  ON equivalentclassgroup
  USING btree
  (kbid);

-- Index: equivalentclassgroup_rid_ridm_idx

-- DROP INDEX equivalentclassgroup_rid_ridm_idx;

CREATE INDEX equivalentclassgroup_rid_ridm_idx
  ON equivalentclassgroup
  USING btree
  (rid, ridm);

-- Index: equivalentclassgroup_ridm_idx

-- DROP INDEX equivalentclassgroup_ridm_idx;

CREATE INDEX equivalentclassgroup_ridm_idx
  ON equivalentclassgroup
  USING btree
  (ridm);


-- instance of a class
-- If DAG indexing mechanism is used to compute inference 
-- on the fly, column inferred can be removed.
CREATE TABLE typeof (
  instanceid integer NOT NULL,
  instance_rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  classid integer NOT NULL,
  class_rtid integer NOT NULL REFERENCES resourcetype (id) ON DELETE CASCADE,
  inferred boolean default false,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE 
);

CREATE INDEX typeof_idx1 ON typeof (instanceid, instance_rtid);

CREATE INDEX typeof_idx2 ON typeof (classid, class_rtid);

CREATE INDEX typeof_idx3 ON typeof (inferred);

CREATE INDEX typeof_idx4 ON typeof (kbid);

-- **************************
-- Views
-- **************************
/* deprecated
CREATE VIEW classes (id, ctype) AS
  select id, 'primitiveclass' as ctype from primitiveclass UNION
  select id, 'allvaluesfromclass' as ctype from allvaluesfromclass UNION
  select id, 'somevaluesfromclass' as ctype from somevaluesfromclass UNION
  select id, 'cardinalityclass' as ctype from cardinalityclass UNION
  select id, 'mincardinalityclass' as ctype from mincardinalityclass UNION
  select id, 'maxcardinalityclass' as ctype from maxcardinalityclass UNION
  select id, 'complementclass' as ctype from complementclass UNION
  select id, 'intersectionclass' as ctype from intersectionclass UNION
  select id, 'unionclass' as ctype from unionclass UNION
  select id, 'hasvalue' as ctype from hasvalue UNION
  select id, 'oneof' as ctype from oneof
;

CREATE VIEW resource_view (id, name, nsid, hashcode, rtype, kbid) AS
  select id, name, nsid, hashcode, 'primitiveclass' as rtype, kbid from primitiveclass
UNION 
  select id, name, nsid, hashcode, 'property' as rtype, kbid from property
UNION
  select id, name, nsid, hashcode, 'datatype' as rtype, kbid from datatype
;
*/
-- ***************************
-- Utility tables
-- ***************************
/* deprecated
-- the table which maps the partOf property
CREATE TABLE propertyMap (
  std_property text,
  real_property text,
  pid int NOT NULL REFERENCES property(id) ON DELETE CASCADE,
  kbid int NOT NULL REFERENCES kb(id) ON DELETE CASCADE
);
*/

-- ****************************
-- Preload data
-- ****************************
INSERT INTO resourcetype VALUES (1, 'primitiveclass', 'c');

INSERT INTO resourcetype VALUES (2, 'allvaluesfromclass', 'a');

INSERT INTO resourcetype VALUES (3, 'somevaluesfromclass', 'v');

INSERT INTO resourcetype VALUES (4, 'cardinalityclass', 'b');

INSERT INTO resourcetype VALUES (5, 'mincardinalityclass', 'm');

INSERT INTO resourcetype VALUES (6, 'maxcardinalityclass', 'n');

INSERT INTO resourcetype VALUES (7, 'complementclass', 't');

INSERT INTO resourcetype VALUES (8, 'intersectionclass', 'x');

INSERT INTO resourcetype VALUES (9, 'unionclass', 'u');

INSERT INTO resourcetype VALUES (10, 'hasvalue', 'h');

INSERT INTO resourcetype VALUES (11, 'oneof', 'o');

INSERT INTO resourcetype VALUES (12, 'individual', 'i');

INSERT INTO resourcetype VALUES (13, 'literal', 'l');

INSERT INTO resourcetype VALUES (14, 'datatype', 'd');

INSERT INTO resourcetype VALUES (15, 'property', 'p');

INSERT INTO resourcetype VALUES (16, 'datarange', 'r');

INSERT INTO resourcetype VALUES (17, 'alldifferentindividual', 'y');

INSERT INTO resourcetype VALUES (18, 'ontologyuri', 'k');

--INSERT INTO resourcetype VALUES (19, 'rdflist', 's');

INSERT INTO resourcetype VALUES (20, 'hasself', 'f');

INSERT INTO resourcetype VALUES (21, 'datatype_restriction', 'e');

/* store the loading history of ontologies */
CREATE TABLE kb_history
(
  id integer,
  name character varying,
  creation_date timestamp without time zone,
  removal_date timestamp without time zone
);

/* this table stores the top category terms used for populating 
   term_category_tbl */
-- DROP TABLE top_categories;
CREATE TABLE top_categories
(
  rid integer,
  rtid integer,
  label text,
  uri character varying(512),
  kbid integer
)
WITH (
  OIDS=FALSE
);

-- Index: top_categories_kbid_idx

-- DROP INDEX top_categories_kbid_idx;

CREATE INDEX top_categories_kbid_idx
  ON top_categories
  USING btree
  (kbid);

-- Index: top_categories_rid_idx

-- DROP INDEX top_categories_rid_idx;

CREATE INDEX top_categories_rid_idx
  ON top_categories
  USING btree
  (rid);
  
CREATE TABLE synonym_property_names
(
  property_name character varying(100),
  CONSTRAINT synonym_property_names_pkey PRIMARY KEY (property_name)
)
WITH (
  OIDS=FALSE
);
COMMENT ON TABLE synonym_property_names
  IS 'When Ontoquest search synonyms, it uses a defined property name list to search relations on these properties. This table stores all the synonym property names so that they are maintained in one place. ';
  
insert into synonym_property_names
values 
 ('prefLabel'),
 ('label'),
  ('has_exact_synonym'),
  ('hasExactSynonym'), 
  ('synonym'), 
  ('abbrev'), 
  ('has_related_synonym'), 
  ('hasRelatedSynonym'), 
  ('acronym'),
  ( 'taxonomicCommonName'), 
  ('ncbiTaxScientificName'), 
  ('ncbiTaxGenbankCommonName'), 
  ('ncbiTaxBlastName'),
  ('ncbiIncludesName'), 
  ('ncbiInPartName'), 
  ('has_narrow_synonym'), 
  ('hasNarrowSynonym'), 
  ('misspelling'), 
  ('misnomer'),
  ('has_broad_synonym'),
  ( 'hasBroadSynonym');