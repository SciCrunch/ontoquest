/* TABLES AND ROUTINES for NIF Card application. 
    Usage: call fill_nif_cards()
*/
/*
DROP TABLE IF EXISTS nif_brain_region cascade;

DROP TABLE IF EXISTS nif_cell cascade;

DROP TABLE IF EXISTS nif_brain_cell cascade;

CREATE TABLE nif_brain_region (
  id serial primary key,
  rid integer NOT NULL,
  rtid integer NOT NULL,
  concept_id text NOT NULL,
  preferred_label text,
  label text,
  neuronames_id text,
  umls_cui text,
  parent text,
  definition text,
  synonyms text,
  abbreviation text,
  acronym text,
  image_url text,
  CONSTRAINT anatomical_structure_concept_id_key UNIQUE (concept_id)
);

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

CREATE TABLE nif_brain_cell
(
  card_id serial primary key,
  brain_region_id integer,
  cell_id integer
);
*/

CREATE OR REPLACE FUNCTION fill_nif_cards() RETURNS VOID AS $$

BEGIN
  perform fill_nif_brain_region();
  perform fill_nif_cell();
  perform fill_nif_brain_cell();

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fill_nif_brain_region() RETURNS VOID AS $$

  DECLARE
    root varchar(64) := '''birnlex_1167''';
    theKbid integer;
  BEGIN
    DELETE FROM nif_brain_region;

    select into theKbid id from kb where name = 'NIF';

    INSERT INTO nif_brain_region (rid, rtid, concept_id, parent) SELECT rid1, rtid1, name1, name2 FROM 
      get_neighborhood(root, '''subClassOf''', null, theKbid, false, 0, true, false, true, false, true);

    UPDATE nif_brain_region SET label = (SELECT get_name(rid, rtid, true, null));
    UPDATE nif_brain_region SET umls_cui = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''UmlsCui'', ''umls_ID''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region SET preferred_label = (select name2 from get_neighborhood(ARRAY[[rid, rtid]], '''prefLabel''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region SET neuronames_id = (select name2 from get_neighborhood(ARRAY[[rid, rtid]], '''neuronamesID''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region SET definition = (select trim(str_agg(distinct name2||'; ')) from get_neighborhood(ARRAY[[rid, rtid]], '''externallySourcedDefinition'', ''hasDefinitionSource'', ''birnlexDefinition'', ''definition'', ''hasDefinition'', ''altDefinition'', ''definitionSource'', ''oboDefinition'', ''tempDefinition''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region SET synonyms = (select trim(str_agg(distinct '"'||name2||'" ')) from get_neighborhood(ARRAY[[rid, rtid]], '''synonym'', ''hasExactSynonym'', ''hasRelatedSynonym'', ''taxonomicCommonName'', ''ncbiTaxScientificName'', ''ncbiTaxGenbankCommonName'', ''ncbiTaxBlastName'', ''ncbiIncludesName'', ''ncbiInPartName'', ''hasNarrowSynonym'', ''misspelling'', ''misnomer'', ''hasBroadSynonym''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region SET abbreviation = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''abbrev''', null, theKbid, false, 1, true, false, false, false));

    UPDATE nif_brain_region SET acronym = (select trim(str_agg(distinct name2||' ')) from get_neighborhood(ARRAY[[rid, rtid]], '''acronym''', null, theKbid, false, 1, true, false, false, false));

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

    UPDATE nif_cell SET definition = (select trim(str_agg(distinct name2||'; ')) from get_neighborhood(ARRAY[[rid, rtid]], '''externallySourcedDefinition'', ''hasDefinitionSource'', ''birnlexDefinition'', ''definition'', ''hasDefinition'', ''altDefinition'', ''definitionSource'', ''oboDefinition'', ''tempDefinition''', null, theKbid, false, 1, true, false, false, false));

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
    excludedRegions text[] := ARRAY['birnlex_1677', 'birnlex_942', 'birnlex_763', 'birnlex_1667', 'birnlex_1509', 'birnlex_949', 'birnlex_1304', 'nlx_anat_20090501', 'birnlex_1167'];
  BEGIN
    DELETE FROM nif_brain_cell;

    EXECUTE 'CREATE TABLE nif_tmp_bc (brain_region_id integer,  cell_id integer, derived boolean)';

    select into theKbid id from kb where name = 'NIF';

    select into somaLocatedIn id from property where name = 'soma_located_in' and kbid = theKbid;

    select into hasProperPart id from property where name = 'has_proper_part' and kbid = theKbid;

    select into properPartOf id from property where name = 'proper_part_of' and kbid = theKbid;

    -- case 1: With macro relation 'soma_located_in':  e.g., 'Neuron X'  is a 'Neuron'  whose 'soma_located_in' some 'Brain region Y'
    INSERT INTO nif_tmp_bc (brain_region_id, cell_id, derived) select r.id as brain_region_id, c.id as cell_id, false from graph_edges, nif_brain_region r, nif_cell c where pid  = somaLocatedIn and rid1 = c.rid and rtid1 = c.rtid and rid2 = r.rid and rtid2 = r.rtid;

    -- case 2: With expanded relation in terms of 'proper_part_of' and 'has_proper_part'.
    -- e.g., Neuron X is a Neuron which 'has_proper_part' some 'Somatic portion' and that 'Somatic portion' is 'proper_part_of' some 'Brain Region Y'.
  
    INSERT INTO nif_tmp_bc (brain_region_id, cell_id, derived) select r.id, c.id, false from graph_edges e1, graph_edges e2, graph_edges e3, nif_brain_region r, nif_cell c where e1.rid2 = r.rid and e1.rtid2 = r.rtid and e1.pid = properPartOf and e1.rid1 = e2.rid2 and e1.rtid1 = e2.rtid2 and e2.rid1 = e3.rid2 and e2.rtid1 = e3.rtid2 and e3.rid1 = c.rid and e3.rtid1 = c.rtid and e3.pid = hasProperPart and not exists (select * from nif_tmp_bc bc where r.id = bc.brain_region_id and c.id = bc.cell_id);

    -- insert derived edges. If cell X is located in region Y, and Y is subclass of region Z, insert Z X into brain_cell table.
    FOR rec IN select distinct bc.cell_id, r.rid, r.rtid from nif_brain_region r, nif_tmp_bc bc where bc.brain_region_id = r.id 
    LOOP
      INSERT INTO nif_tmp_bc (brain_region_id, cell_id, derived) 
        SELECT r.id, rec.cell_id, true FROM nif_brain_region r, (select rid2, rtid2 from 
        get_neighborhood(ARRAY[[rec.rid, rec.rtid]], '''subClassOf''', null, theKbid, false, 0, true, false, true, false)) t 
        WHERE r.rid = t.rid2 and r.rtid = t.rtid2;
    END LOOP;

    -- remove those derived edges whose region is in the exclusion list.
    DELETE FROM nif_tmp_bc WHERE derived = true and brain_region_id IN (select id from nif_brain_region where concept_id = ANY(excludedRegions));

    INSERT INTO nif_brain_cell (brain_region_id, cell_id) select distinct brain_region_id, cell_id FROM nif_tmp_bc; 
     
    EXECUTE 'DROP TABLE nif_tmp_bc';
  END;
$$ LANGUAGE plpgsql;

--select fill_nif_cards();

--select bc.*, r.label from nif_brain_cell bc, nif_brain_region r where bc.brain_region_id = r.id order by cell_id;

