/* TABLES AND ROUTINES for NIF Card application. 
    Usage: call fill_nif_cards()
*/

DROP TABLE IF EXISTS nif_brain_region_all cascade;

DROP TABLE IF EXISTS nif_cell cascade;

DROP TABLE IF EXISTS nif_brain_cell_all cascade;

DROP TABLE IF EXISTS nif_disease cascade;

CREATE TABLE nif_brain_region_all (
  id serial primary key,
  rid integer NOT NULL,
  rtid integer NOT NULL,
  concept_id text NOT NULL,
  preferred_label text,
  label text,
  neuronames_id text,
  umls_cui text,
  superclass text,
  superpart text,
  definition text,
  synonyms text,
  abbreviation text,
  acronym text,
  image_url text,
  CONSTRAINT anatomical_structure_concept_id_key UNIQUE (concept_id)
);

CREATE OR REPLACE view nif_brain_region as select id, rid, rtid, concept_id, preferred_label, label, neuronames_id, umls_cui, superpart as parent, 
definition, synonyms, abbreviation, acronym, image_url from nif_brain_region_all;

CREATE TABLE nif_cell
(
  id serial primary key,
  rid integer NOT NULL,
  rtid integer NOT NULL,
  concept_id text NOT NULL,
  preferred_label text,
  label text,
  sao_id text,
  umls_cui text,
  parent text,
  definition text,
  synonyms text,
  abbreviation text,
  acronym text,
  image_url text,
  cell_ontology text
--  ,  CONSTRAINT cell_concept_id_key UNIQUE (concept_id)
);

CREATE TABLE nif_brain_cell_all
(
  card_id serial primary key,
  brain_region_id integer references nif_brain_region_all on delete cascade,
  brain_region_concept_id text,
  cell_id integer references nif_cell,
  cell_concept_id text,
  derived boolean
);

CREATE OR REPLACE VIEW nif_brain_cell as select card_id, brain_region_id, brain_region_concept_id, cell_id, cell_concept_id from nif_brain_cell_all;

CREATE TABLE nif_disease
(
  id serial primary key,
  rid integer NOT NULL,
  rtid integer NOT NULL,
  concept_id text NOT NULL,
  label text,
  umls_cui text,
  parent text,
  definition text,
  synonyms text,
  abbreviation text,
  acronym text
);

CREATE OR REPLACE FUNCTION fill_nif_cards() RETURNS VOID AS $$

BEGIN
  perform fill_nif_brain_region();
  perform fill_nif_cell();
  perform fill_nif_brain_cell();
  perform fill_nif_disease();

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fill_nif_brain_region() RETURNS VOID AS $$

  DECLARE
    root varchar(64) := '''birnlex_6''';
    partof_root varchar(64):= '''birnlex_1099''';
    theKbid integer;
  BEGIN
    DELETE FROM nif_brain_region_all;

    EXECUTE 'CREATE TABLE nif_tmp_b (rid integer,  rtid integer, concept_id text, superclass text, superpart text)';

    select into theKbid id from kb where name = 'NIF';

    INSERT INTO nif_tmp_b (rid, rtid, concept_id, superclass) (SELECT rid1, rtid1, name1, trim(str_agg(name2||' ')) FROM 
      get_neighborhood(root, '''subClassOf''', null, theKbid, false, 0, true, false, true, false, true) group by rid1, rtid1, name1);

    INSERT INTO nif_tmp_b (rid, rtid, concept_id, superpart) (SELECT rid2, rtid2, name2, trim(str_agg(name1||' ')) FROM 
      get_neighborhood(partof_root, '''has_part''', null, theKbid, false, 0, true, true, true, false, false) group by rid2, rtid2, name2);

    INSERT INTO nif_brain_region_all (rid, rtid, concept_id, superclass, superpart) (SELECT rid, rtid, concept_id, max(superclass), max(superpart) 
      FROM nif_tmp_b group by rid, rtid, concept_id);
    
    UPDATE nif_brain_region_all SET label = (SELECT get_name(rid, rtid, true, null));
    UPDATE nif_brain_region_all SET umls_cui = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''UmlsCui'', ''umls_ID''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region_all SET preferred_label = (select name2 from get_neighborhood(ARRAY[[rid, rtid]], '''prefLabel''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region_all SET neuronames_id = (select name2 from get_neighborhood(ARRAY[[rid, rtid]], '''neuronamesID''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region_all SET definition = (select trim(str_agg(distinct name2||' ['||pname||']; ')) from get_neighborhood(ARRAY[[rid, rtid]], '''externallySourcedDefinition'', ''birnlexDefinition'', ''definition'', ''hasDefinition'', ''altDefinition'', ''oboDefinition'', ''tempDefinition''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region_all SET synonyms = (select trim(str_agg(distinct '"'||name2||'" ')) from get_neighborhood(ARRAY[[rid, rtid]], '''synonym'', ''hasExactSynonym'', ''hasRelatedSynonym'', ''taxonomicCommonName'', ''ncbiTaxScientificName'', ''ncbiTaxGenbankCommonName'', ''ncbiTaxBlastName'', ''ncbiIncludesName'', ''ncbiInPartName'', ''hasNarrowSynonym'', ''misspelling'', ''misnomer'', ''hasBroadSynonym''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region_all SET abbreviation = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''abbrev''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region_all SET acronym = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''acronym''', null, theKbid, false, 1, true, false, false, false));

    EXECUTE 'DROP TABLE nif_tmp_b';

  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fill_nif_cell() RETURNS VOID AS $$

  DECLARE
    root varchar(64) := '''sao1813327414''';
    theKbid integer;
  BEGIN
    DELETE FROM nif_cell;

    select into theKbid id from kb where name = 'NIF';

    INSERT INTO nif_cell (rid, rtid, concept_id, parent) (SELECT rid1, rtid1, name1, trim(str_agg(name2||' ')) FROM 
      get_neighborhood(root, '''subClassOf''', null, theKbid, false, 0, true, false, true, false, true) group by rid1, rtid1, name1);

    UPDATE nif_cell SET label = (SELECT get_name(rid, rtid, true, null));

    UPDATE nif_cell SET preferred_label = (select name2 from get_neighborhood(ARRAY[[rid, rtid]], '''prefLabel''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_cell SET sao_id = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''sao_ID''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_cell SET umls_cui = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''UmlsCui'', ''umls_ID''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_cell SET definition = (select trim(str_agg(distinct name2||' ['||pname||']; ')) from get_neighborhood(ARRAY[[rid, rtid]], '''externallySourcedDefinition'', ''birnlexDefinition'', ''definition'', ''hasDefinition'', ''altDefinition'',
     ''oboDefinition'', ''tempDefinition''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_cell SET synonyms = (select trim(str_agg(distinct '"'||name2||'" ')) from get_neighborhood(ARRAY[[rid, rtid]], '''synonym'', ''hasExactSynonym'', ''hasRelatedSynonym'', ''taxonomicCommonName'', ''ncbiTaxScientificName'', ''ncbiTaxGenbankCommonName'', ''ncbiTaxBlastName'', ''ncbiIncludesName'', ''ncbiInPartName'', ''hasNarrowSynonym'', ''misspelling'', ''misnomer'', ''hasBroadSynonym''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_cell SET abbreviation = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''abbrev''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_cell SET acronym = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''acronym''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_cell SET cell_ontology = (select name2 from get_neighborhood(ARRAY[[rid, rtid]], '''cell_ontology_ID''', null, theKbid, false, 1, true, false, false, false));

  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fill_nif_brain_cell() RETURNS VOID AS $$

  DECLARE
    theKbid integer;
    somaLocatedIn integer;
    hasProperPart integer;
    properPartOf integer;
    rec RECORD;
    excludedRegions text[] := ARRAY['Thing', 'MaterialEntity', 'IndependentContinuant', 'Continuant', 'Entity', 'birnlex_1677', 'birnlex_942', 'birnlex_763', 'birnlex_1667', 'birnlex_1509', 'birnlex_949', 'birnlex_1304', 'nlx_anat_20090501', 'birnlex_1167'];
  BEGIN
    DELETE FROM nif_brain_cell_all;

    EXECUTE 'CREATE TABLE nif_tmp_bc (brain_region_id integer,  cell_id integer, derived boolean)';

    select into theKbid id from kb where name = 'NIF';

    select into somaLocatedIn id from property where name = 'soma_located_in' and kbid = theKbid;

    select into hasProperPart id from property where name = 'has_proper_part' and kbid = theKbid;

    select into properPartOf id from property where name = 'proper_part_of' and kbid = theKbid;

    -- case 1: With macro relation 'soma_located_in':  e.g., 'Neuron X'  is a 'Neuron'  whose 'soma_located_in' some 'Brain region Y'
    INSERT INTO nif_tmp_bc (brain_region_id, cell_id, derived) select r.id as brain_region_id, c.id as cell_id, false from graph_edges, 
    nif_brain_region_all r, nif_cell c where pid  = somaLocatedIn and rid1 = c.rid and rtid1 = c.rtid and rid2 = r.rid and rtid2 = r.rtid;

    -- case 2: With expanded relation in terms of 'proper_part_of' and 'has_proper_part'.
    -- e.g., Neuron X is a Neuron which 'has_proper_part' some 'Somatic portion' and that 'Somatic portion' is 'proper_part_of' some 'Brain Region Y'.
  
    INSERT INTO nif_tmp_bc (brain_region_id, cell_id, derived) select r.id, c.id, false from graph_edges e1, graph_edges e2, graph_edges e3, 
    nif_brain_region_all r, nif_cell c where e1.rid2 = r.rid and e1.rtid2 = r.rtid and e1.pid = properPartOf and e1.rid1 = e2.rid2 and 
    e1.rtid1 = e2.rtid2 and e2.rid1 = e3.rid2 and e2.rtid1 = e3.rtid2 and e3.rid1 = c.rid and e3.rtid1 = c.rtid and e3.pid = hasProperPart 
    and not exists (select * from nif_tmp_bc bc where r.id = bc.brain_region_id and c.id = bc.cell_id);

    -- insert derived edges. If cell X is located in region Y, and Y is subclass of region Z, insert Z X into brain_cell table.
    FOR rec IN select distinct bc.cell_id, r.rid, r.rtid from nif_brain_region_all r, nif_tmp_bc bc where bc.brain_region_id = r.id 
    LOOP
      INSERT INTO nif_tmp_bc (brain_region_id, cell_id, derived) 
        SELECT r.id, rec.cell_id, true FROM nif_brain_region_all r, (select rid2, rtid2 from 
        get_neighborhood(ARRAY[[rec.rid, rec.rtid]], '''subClassOf''', null, theKbid, false, 0, true, false, true, false)) t 
        WHERE r.rid = t.rid2 and r.rtid = t.rtid2;
    END LOOP;
    
    -- insert derived edges. If cell X is located in region Y, and Y is part of region Z, insert Z X into brain_cell table.
    FOR rec IN select distinct bc.cell_id, r.rid, r.rtid from nif_brain_region_all r, nif_tmp_bc bc where bc.brain_region_id = r.id 
    LOOP
      INSERT INTO nif_tmp_bc (brain_region_id, cell_id, derived) 
        SELECT r.id, rec.cell_id, true FROM nif_brain_region_all r, (select rid1, rtid1 from 
        get_neighborhood(ARRAY[[rec.rid, rec.rtid]], '''has_part''', null, theKbid, false, 0, true, true, true, true)) t 
        WHERE r.rid = t.rid1 and r.rtid = t.rtid1;
    END LOOP;
    
    
    -- remove those derived edges whose region is in the exclusion list.
    DELETE FROM nif_tmp_bc WHERE derived = true and brain_region_id IN (select id from nif_brain_region_all where concept_id = ANY(excludedRegions));

    INSERT INTO nif_brain_cell_all (brain_region_id, brain_region_concept_id, cell_id, cell_concept_id, derived) 
    select distinct bc.brain_region_id, r.concept_id, bc.cell_id, c.concept_id, bc.derived FROM nif_tmp_bc bc, nif_brain_region_all r, nif_cell c
    where bc.brain_region_id = r.id and bc.cell_id = c.id; 
     
    EXECUTE 'DROP TABLE nif_tmp_bc';
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fill_nif_disease() RETURNS VOID AS $$

  DECLARE
    root varchar(64) := '''birnlex_11013''';
    theKbid integer;
  BEGIN
    DELETE FROM nif_disease;

    select into theKbid id from kb where name = 'NIF';

    INSERT INTO nif_disease (rid, rtid, concept_id, parent) (SELECT rid1, rtid1, name1, trim(str_agg(name2||' ')) FROM 
      get_neighborhood(root, '''subClassOf''', null, theKbid, false, 0, true, false, true, false, true) group by rid1, rtid1, name1);

    UPDATE nif_disease SET label = (SELECT get_name(rid, rtid, true, null));

    UPDATE nif_disease SET umls_cui = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''UmlsCui'', ''umls_ID''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_disease SET definition = (select trim(str_agg(distinct name2||' ['||pname||']; ')) from get_neighborhood(ARRAY[[rid, rtid]], '''externallySourcedDefinition'', ''birnlexDefinition'', ''definition'', ''hasDefinition'', ''altDefinition'',
     ''oboDefinition'', ''tempDefinition''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_disease SET synonyms = (select trim(str_agg(distinct '"'||name2||'" ')) from get_neighborhood(ARRAY[[rid, rtid]], '''synonym'', ''hasExactSynonym'', ''hasRelatedSynonym'', ''taxonomicCommonName'', ''ncbiTaxScientificName'', ''ncbiTaxGenbankCommonName'', ''ncbiTaxBlastName'', ''ncbiIncludesName'', ''ncbiInPartName'', ''hasNarrowSynonym'', ''misspelling'', ''misnomer'', ''hasBroadSynonym''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_disease SET abbreviation = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''abbrev''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_disease SET acronym = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''acronym''', null, theKbid, false, 1, true, false, false, false));

  END;
$$ LANGUAGE plpgsql;

--select fill_nif_cards();

--select bc.*, r.label from nif_brain_cell_all bc, nif_brain_region_all r where bc.brain_region_id = r.id order by cell_id;

