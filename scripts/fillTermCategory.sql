select drop_if_exists('term_category');

select drop_if_exists('term_category_tbl');

select drop_if_exists('nif_term');

CREATE TABLE nif_term (
  term text,
  tid varchar(64),
  rid integer,
  rtid integer,
  inferred boolean,
  is_acronym boolean,
  is_abbrev boolean,
  synonyms character varying(3000),
  acronyms character varying(3000),
  abbreviations character varying(3000),
  primary key (rid, rtid, term)
);

CREATE INDEX nif_term_idx on nif_term (lower(term));

CREATE TABLE term_category_tbl (
  rid integer,
  rtid integer,
  category text,
  cat_rid integer,
  cat_rtid integer,
  cm_type text,
  cm_type_rid integer,
  cm_type_rtid integer
  ,primary key (rid, rtid, cat_rid, cat_rtid)
);


CREATE OR REPLACE VIEW term_category AS 
 SELECT t.term, t.tid, t.rid, t.rtid, t.inferred, t.is_acronym, t.is_abbrev, 
  tc.category, tc.cat_rid, tc.cat_rtid, tc.cm_type, tc.cm_type_rid, tc.cm_type_rtid,
  t.synonyms,t.acronyms, t.abbreviations
   FROM nif_term t
   LEFT JOIN term_category_tbl tc ON t.rid = tc.rid AND t.rtid = tc.rtid
  WHERE t.term !~~ '%http://%'::text;

/*
  deprecated. Please use the fill_term_category_v2 function instead.
*/
CREATE OR REPLACE FUNCTION fill_term_category(theKbid INTEGER, category_list_str TEXT, cm_type_list_str TEXT) RETURNS VOID AS $$
/*
  fills table term_category. Extract all terms (class label + synonyms) in the specified kbid and their
  class name (tid), then populate the term_category table. For each term, if it is a subclass of
  one of the supplied categories, fill the category column. Always use the lowest level of the category.
*/
DECLARE
  rec0 RECORD;
  rec1 RECORD;
  rec2 RECORD;
  sql TEXT;
  counter INTEGER := 0;
  synonymProperties TEXT := '''prefLabel'', ''label'', ''synonym'', ''abbrev'',  ''hasExactSynonym'', ''hasRelatedSynonym'', ''acronym'', ''taxonomicCommonName'', ''ncbiTaxScientificName'', ''ncbiTaxGenbankCommonName'', ''ncbiTaxBlastName'', ''ncbiIncludesName'', ''ncbiInPartName'', ''hasNarrowSynonym'', ''misspelling'', ''misnomer'', ''hasBroadSynonym''';
  synPropIDs integer[];
  categoryIDs integer[][];
  kbid_condition text := '';
  kb_expr text := 'null::int[]';
  i integer;
  j integer;
  subclassPid integer;
  currRow RECORD;
  h integer;
  hasFound boolean;
  
  theRid integer;
  theRtid integer;
  theName text;

BEGIN
    -- retrieve IDs of the categories.
    IF theKbid is not null THEN
      kb_expr := 'array['||theKbid||']';
    END IF;
    sql := 'select * from get_ids('||quote_literal(category_list_str)||', true, true, '||kb_expr||') t where t.rtid = 1';

    FOR rec1 IN EXECUTE sql LOOP
      IF categoryIDs is null THEN
        categoryIDs := ARRAY[[rec1.rid, rec1.rtid]];
      ELSE
        categoryIDs := categoryIDs || ARRAY[[rec1.rid, rec1.rtid]];
      END IF;
    END LOOP;
--raise notice 'category IDs = %', array_to_string(categoryIDs, ',');

    IF categoryIDs is null THEN
      RETURN;
    END IF;
    
    -- get id of property subClassOf.
    sql := 'select * from get_id(''subClassOf'', false, false, '||kb_expr||')';
    FOR rec1 IN EXECUTE sql LOOP
      subclassPid := rec1.rid;
    END LOOP;

    -- clean tables
    delete from nif_term;
    delete from term_category_tbl;
    
    -- figure out synonym property ids
    -- prepare properties to be included.
    IF theKbid is not null AND theKbid > 0 THEN
      kbid_condition := ' and p.kbid = '||theKbid;
    END IF;
    
    IF synonymProperties is not null AND synonymProperties != '*' AND synonymProperties != '' THEN  
      sql := 'select p.id from property p where p.name in ('||synonymProperties||')'||kbid_condition;
      FOR rec0 IN EXECUTE sql
      LOOP
        -- get matched property id(s).
        IF synPropIDs is null THEN
          synPropIDs := ARRAY[rec0.id];
        ELSE
          synPropIDs := synPropIDs || rec0.id;
        END IF;
      END LOOP;
    END IF;

--raise notice 'synPropIDs = %', synPropIDs;

  -- fill out tid and term
  FOR rec1 IN select rid, rtid, name from graph_nodes where kbid = theKbid and rtid = 1 and name != 'Thing' --limit 50
  LOOP
    FOR rec2 IN select distinct name2 from get_neighborhood(ARRAY[[rec1.rid, rec1.rtid]], synPropIDs, null, false,1,true,false, false,false) where name2 is not null
    LOOP
 --raise notice 'rec2.name2=%, rec1.name=%, rid=%, rtid=%', rec2.name2, rec1.name, rec1.rid, rec1.rtid;
      insert into nif_term (term, tid, rid, rtid) values (rec2.name2, rec1.name, rec1.rid, rec1.rtid);
    END LOOP;

  END LOOP;

  -- fill inferred flag
  UPDATE nif_term SET inferred = has_inferred_def(rid, rtid);

  -- fill the is_acronym flag (Xufei, 03.2012)
  UPDATE nif_term t set is_acronym = (select true from relationship r, literal l, property p where r.propertyid = p.id and p.name = 'acronym' and 
	r.subjectid = t.rid and r.subject_rtid = t.rtid and r.objectid = l.id and r.object_rtid = 13 and l.lexicalform = t.term);

  UPDATE nif_term t set is_acronym = false where is_acronym is null;

  -- fill the is_abbrev flag (Xufei, 04.2012)
  UPDATE nif_term t set is_abbrev = (select true from relationship r, literal l, property p where r.propertyid = p.id and p.name = 'abbrev' and 
	r.subjectid = t.rid and r.subject_rtid = t.rtid and r.objectid = l.id and r.object_rtid = 13 and l.lexicalform = t.term);

  UPDATE nif_term t set is_abbrev = false where is_abbrev is null;


  DELETE FROM nif_term where term = '';
  
  select into counter count(*) from nif_term;
  raise notice 'insert all terms and their ids. Count = %', counter;

  -- create hierarchy in input categories. Put the hierarchy in temporary table tmp_cat_tree
  -- first, iterate category pairs. For every pair, check if there exist ancestor-descendant relationship.
  -- If so, add the relationship in tmp_cat_tree. Then, find all known ancestors of the child nodes, and
  -- see which one is the closest ancestor. Update the table with the closest ancestor.
  EXECUTE 'drop table if exists tmp_cat_tree cascade';

  EXECUTE 'create table tmp_cat_tree (desc_rid integer, desc_rtid integer, anc_rid integer, anc_rtid integer, height integer)';
  FOR i IN ARRAY_LOWER(categoryIDs, 1)..ARRAY_UPPER(categoryIDs, 1) LOOP
    INSERT INTO tmp_cat_tree values (categoryIDs[i][1], categoryIDs[i][2], null, null, null);
  END LOOP;

  -- populate categories for all terms
  FOR rec1 IN select desc_rid, desc_rtid from tmp_cat_tree order by height
  LOOP
    insert into term_category_tbl (rid, rtid, cat_rid, cat_rtid) select distinct rid1, rtid1, rec1.desc_rid, rec1.desc_rtid 
      from get_neighborhood(rec1.desc_rid, rec1.desc_rtid, subclassPid, false, 0, true, true, true, true) t, nif_term nt
      where t.rid1 = nt.rid and t.rtid1 = nt.rtid;
--    raise notice 'add all descendants of category %, %', rec1.desc_rid, rec1.desc_rtid;
  END LOOP;
  raise notice 'Inserted all categories for all terms';
  
  -- For every (rid, rtid, cat_rid, cat_rtid) pair, get category (cat_rid, cat_rtid)'s superclasses (sc_rid, sc_rtid), 
  -- remove all pairs (rid, rtid, sc_rid, sc_rtid) in term_category_tbl.
  FOR rec2 IN select desc_rid, desc_rtid from tmp_cat_tree
  LOOP
   delete from term_category_tbl tc1 where (rid, rtid, cat_rid, cat_rtid) in (select tc2.rid, tc2.rtid, ne.rid2, ne.rtid2 
      from term_category_tbl tc2, get_neighborhood(rec2.desc_rid, rec2.desc_rtid, subclassPid, false, 0, true, 
      true, true, false) ne where tc2.cat_rid = rec2.desc_rid and tc2.cat_rtid = rec2.desc_rtid);
--    raise notice 'delete redundant descendants of category %, %', rec2.desc_rid, rec2.desc_rtid;
  END LOOP;
  raise notice 'Deleted all redundant descendants of categories.';

  insert into term_category_tbl (rid, rtid, cat_rid, cat_rtid)
  select distinct t.rid, t.rtid, t.rid, t.rtid from nif_term t where 
  t.rtid = 1 and 
  exists ( select 1 from nif_category nc where  lower(t.term) = nc.name)
  and not exists ( select 1 from term_category_tbl tc where tc.rid = t.rid and tc.rtid = 1);

  -- fill the label for categories
  update term_category_tbl tc set category = (select n.name from graph_nodes n, graph_edges e, property p 
    where p.id = e.pid and   p.name = 'altLabel' and e.rid2 = n.rid and e.rtid2 = n.rtid and tc.cat_rid = e.rid1 and 
    tc.cat_rtid = e.rtid1);

  update term_category_tbl set category = get_name(cat_rid, cat_rtid, true, null) where category is null;
  raise notice 'category label is updated';

  -- fill the concept map types
  sql := 'select * from get_ids('||quote_literal(cm_type_list_str)||', true, false, '||kb_expr||') t where t.rtid = 1';

  FOR theRid, theRtid, theName IN EXECUTE sql
  LOOP
    update term_category_tbl tc set (cm_type_rid, cm_type_rtid, cm_type) = (theRid, theRtid, theName) 
      where (tc.rid, tc.rtid) in (select rid, rtid from get_descendant_nodes_in_dag(theRid, theRtid, subclassPid, 0, true, true));
  END LOOP;

  EXECUTE 'drop table tmp_cat_tree';
END;
$$ LANGUAGE plpgsql;

-- usage
--select fill_term_category(:kbid, '''Anatomical object'', ''brain'', ''Cell'', ''Device'', ''Cellular Component'', ''extracellular structure'', ''cell line'', ''tissue section'', ''molecular entity'', ''Site'', ''Institution'', ''Platform'', ''Population'', ''Disease'', ''Biological_region'', ''gene'', ''Molecule role'', ''Drug'', ''Data object'', ''Assay'', ''Organism'', ''Data role'', ''Chemical role'', ''Reagent role'', ''familial role'', ''cell role'' , ''Quality'', ''Biomaterial_region'', ''Artifact Object'', ''Phenotype'', ''age'', ''Process'', ''behavioral process'', ''biological_process'', ''Regional Part Of Cell'', ''Resource''', '''birnlex_6'', ''sao1813327414'', ''birnlex_11013'', ''Function'', ''CHEBI_23367'', ''birnlex_2'', ''birnlex_2087'', ''birnlex_11021'', ''nlx_res_20090101''');

-- Function: fill_term_category_v2(integer, text, text)

-- DROP FUNCTION fill_term_category_v2(integer, text, text);

CREATE OR REPLACE FUNCTION fill_term_category_v2(thekbid integer, category_list_str text, cm_type_list_str text)
  RETURNS void AS
$BODY$
/*
  fills table term_category. Extract all terms (class label + synonyms) in the specified kbid and their
  class name (tid), then populate the term_category table. For each term, if it is a subclass of
  one of the supplied categories, fill the category column. Always use the lowest level of the category.
*/
DECLARE
  rec0 RECORD;
  rec1 RECORD;
  rec2 RECORD;
  sql TEXT;
  counter INTEGER := 0;
--  synonymProperties TEXT := '''prefLabel'', ''label'', ''synonym'', ''abbrev'',  ''hasExactSynonym'', ''hasRelatedSynonym'', ''acronym'', ''taxonomicCommonName'', ''ncbiTaxScientificName'', ''ncbiTaxGenbankCommonName'', ''ncbiTaxBlastName'', ''ncbiIncludesName'', ''ncbiInPartName'', ''hasNarrowSynonym'', ''misspelling'', ''misnomer'', ''hasBroadSynonym''';
--  synPropIDs integer[];
  categoryIDs integer[][];
--  kbid_condition text := '';
  kb_expr text := 'null::int[]';
  i integer;
  j integer;
  subclassPid integer;
  currRow RECORD;
  h integer;
  hasFound boolean;
  
  theRid integer;
  theRtid integer;
  theName text;

BEGIN
    -- retrieve IDs of the categories.
    IF theKbid is not null THEN
      kb_expr := 'array['||theKbid||']';
    END IF;

  -- create hierarchy in input categories. Put the hierarchy in temporary table tmp_cat_tree
  -- first, iterate category pairs. For every pair, check if there exist ancestor-descendant relationship.
  -- If so, add the relationship in tmp_cat_tree. Then, find all known ancestors of the child nodes, and
  -- see which one is the closest ancestor. Update the table with the closest ancestor.
--  EXECUTE 'drop table if exists tmp_cat_tree cascade';
  delete from top_categories where kbid=thekbid;

    sql := 'select distinct rid, rtid from get_ids('||quote_literal(category_list_str)||', true, true, '||kb_expr||') t where t.rtid = 1';

    FOR rec1 IN EXECUTE sql LOOP
      INSERT INTO top_categories (rid, rtid, kbid) values (rec1.rid, rec1.rtid,thekbid);
--      IF categoryIDs is null THEN
        categoryIDs := ARRAY[[rec1.rid, rec1.rtid]];
--      ELSE
--        categoryIDs := categoryIDs || ARRAY[[rec1.rid, rec1.rtid]];
--      END IF; */
    END LOOP;
--raise notice 'category IDs = %', array_to_string(categoryIDs, ',');

    IF categoryIDs is null THEN
      RETURN;
    END IF;

    update top_categories tc
      set label =(select n.label from graph_nodes n where kbid=theKbid and n.rid = tc.rid and n.rtid = tc.rtid),
          uri = (select n.uri from graph_nodes n where kbid=theKbid and n.rid = tc.rid and n.rtid = tc.rtid)
    where tc.kbid = theKbid ;
    
    -- get id of property subClassOf.
    sql := 'select * from get_id(''subClassOf'', false, false, '||kb_expr||')';
    FOR rec1 IN EXECUTE sql LOOP
      subclassPid := rec1.rid;
    END LOOP;

    -- clean tables
    delete from nif_term;
    delete from term_category_tbl;
    
    -- figure out synonym property ids
    -- prepare properties to be included.
 --   IF theKbid is not null AND theKbid > 0 THEN
 --     kbid_condition := ' and p.kbid = '||theKbid;
 --   END IF;
    
  /*  IF synonymProperties is not null AND synonymProperties != '*' AND synonymProperties != '' THEN  
      sql := 'select p.id from property p where p.name in ('||synonymProperties||')'||kbid_condition;
      FOR rec0 IN EXECUTE sql
      LOOP
        -- get matched property id(s).
        IF synPropIDs is null THEN
          synPropIDs := ARRAY[rec0.id];
        ELSE
          synPropIDs := synPropIDs || rec0.id;
        END IF;
      END LOOP;
    END IF; */

--raise notice 'synPropIDs = %', synPropIDs;

  -- fill out tid and term
 insert into nif_term (term, tid, rid, rtid)
  select distinct n2.name, n.name, n.rid, n.rtid from graph_nodes n, relationship e, property p, graph_nodes n2, synonym_property_names sp
  where n.rid = e.subjectid and n.rtid = 1 and e.subject_rtid = 1 and n.kbid = theKbid
     and p.id = e.propertyid and p.name = sp.property_name
     and n2.rid = e.objectid and n2.rtid = e.object_rtid ;


  DELETE FROM nif_term where term = '';
  
  select into counter count(*) from nif_term;
  raise notice 'insert all terms and their ids. Count = %', counter;

  update nif_term t
    set synonyms = array_to_string(array(select name2 from get_neighborhood(ARRAY[[t.rid,t.rtid]],
     '''synonym'',''has_exact_synonym'',''hasExactSynonym'',''hasSynonym'',''exact_synonym'',''hasRelatedSynonym'',''hasNarrowSynonym'',''systematic_synonym'',''hasBroadSynonym''',
      null,theKbid,true,1,true,true, false,false)),',')
  where synonyms is null;

  update nif_term t set synonyms = null where synonyms = '';

  update nif_term t
    set abbreviations = array_to_string(array(select name2 from get_neighborhood(ARRAY[[t.rid,t.rtid]],
     '''abbrev''',null,theKbid,true,1,true,true, false,false)),',')
  where abbreviations is null;

  update nif_term t set abbreviations = null where abbreviations = '';

  update nif_term t
    set acronyms = array_to_string(array(select name2 from get_neighborhood(ARRAY[[t.rid,t.rtid]],
     '''acronym''',null,theKbid,true,1,true,true, false,false)),',')
  where acronyms is null;

  update nif_term t set acronyms = null where acronyms = '';
    
/*     

  FOR rec1 IN select rid, rtid, name from graph_nodes where kbid = theKbid and rtid = 1 and name != 'Thing' --limit 50
  LOOP
    FOR rec2 IN select distinct name2 from get_neighborhood(ARRAY[[rec1.rid, rec1.rtid]], synPropIDs, null, false,1,true,false, false,false) where name2 is not null
    LOOP
 --raise notice 'rec2.name2=%, rec1.name=%, rid=%, rtid=%', rec2.name2, rec1.name, rec1.rid, rec1.rtid;
      insert into nif_term (term, tid, rid, rtid) values (rec2.name2, rec1.name, rec1.rid, rec1.rtid);
    END LOOP;

  END LOOP;
*/

  -- fill inferred flag
  raise notice 'Filling inferred flag';
  UPDATE nif_term SET inferred = has_inferred_def(rid, rtid);

  -- fill the is_acronym flag (Xufei, 03.2012)
  raise notice 'Filling is_acronym flag';
  UPDATE nif_term t set is_acronym = (select true from relationship r, literal l, property p where r.propertyid = p.id and p.name = 'acronym' and 
	r.subjectid = t.rid and r.subject_rtid = t.rtid and r.objectid = l.id and r.object_rtid = 13 and l.lexicalform = t.term);

  UPDATE nif_term t set is_acronym = false where is_acronym is null;

  -- fill the is_abbrev flag (Xufei, 04.2012)
  UPDATE nif_term t set is_abbrev = (select true from relationship r, literal l, property p where r.propertyid = p.id and p.name = 'abbrev' and 
	r.subjectid = t.rid and r.subject_rtid = t.rtid and r.objectid = l.id and r.object_rtid = 13 and l.lexicalform = t.term);

  UPDATE nif_term t set is_abbrev = false where is_abbrev is null;

--  EXECUTE 'create table tmp_cat_tree (desc_rid integer, desc_rtid integer, anc_rid integer, anc_rtid integer, height integer, desc_label character varying)';

/*
  FOR i IN ARRAY_LOWER(categoryIDs, 1)..ARRAY_UPPER(categoryIDs, 1) LOOP
    INSERT INTO tmp_cat_tree (desc_rid, desc_rtid, anc_rid, anc_rtid, height, desc_label)
    values (categoryIDs[i][1], categoryIDs[i][2], null, null, null, 
         (select n.label from graph_nodes n where kbid=theKbid and n.rid = categoryIDs[i][1] and n.rtid=categoryIDs[i][2]));
  END LOOP;
*/

  -- populate categories for all terms that are subclasses of top categories.
  insert into term_category_tbl (rid, rtid, cat_rid, cat_rtid, category) 
    with recursive incoming_closure ( rid1, rtid1, cat_rid, cat_rtid, category
      ) as (
       select e.rid1, e.rtid1, n.rid, n.rtid, n.label 
       from graph_edges e, 
           ( (select nn.rid, nn.rtid, nn.label from graph_nodes nn, top_categories tt 
               where nn.rid= tt.rid and nn.rtid=1 and nn.kbid= thekbid and tt.kbid = thekbid) 
            union
             ( select nn1.rid, 1, nn1.label 
               from equivalentclassgroup g, top_categories tt2, graph_nodes nn1
               where tt2.rid = g.ridm 
                 and nn1.rtid = 1 and nn1.rid = g.rid
                 and g.kbid= thekbid and tt2.kbid = thekbid) )  n 
       where n.rtid=1 and ( e.rid1 <> e.rid2 or e.rtid1 <> e.rtid2 ) 
            and e.rid2 = n.rid and e.rtid2 = n.rtid and e.pid = subclassPid
      union 
        select ge.rid1, ge.rtid1, ie.cat_rid,ie.cat_rtid, ie.category 
         from incoming_closure ie, graph_edges ge
         where ie.rid1 = ge.rid2 and ie.rtid1 = ge.rtid2 and
           ( ge.rid1 <> ge.rid2 or ge.rtid1 <> ge.rtid2 ) and ge.pid = subclassPid
      )
      select distinct r.rid1, r.rtid1, r.cat_rid, r.cat_rtid, r.category from incoming_closure r;

  raise notice 'Inserted all categories for all terms';

  -- categorize all the equivalent class
  insert into term_category_tbl (rid, rtid, cat_rid, cat_rtid, category) 
    select g.ridm, 1 , t1.cat_rid, t1.cat_rtid, t1.category
    from term_category_tbl t1, equivalentclassgroup g
    where t1.rid = g.rid and t1.rtid = 1 and g.kbid=thekbid
       and not exists ( select 1 from term_category_tbl tt where tt.rid = g.ridm);


  -- insert the category terms into the final table if they are not in it yet.
  insert into term_category_tbl (rid, rtid, cat_rid, cat_rtid, category )
  select distinct t.rid, t.rtid, t.rid, t.rtid, t.label 
  from top_categories t
    where t.kbid = thekbid and 
      not exists ( select 1 from term_category_tbl tcb 
                       where tcb.rid = t.rid and 
                             tcb.rtid = t.rtid and 
                             tcb.cat_rid = t.rid and 
                             tcb.cat_rtid = t.rtid);
  
  -- For every (rid, rtid, cat_rid, cat_rtid) pair, get category (cat_rid, cat_rtid)'s superclasses (sc_rid, sc_rtid), 
  -- remove all pairs (rid, rtid, sc_rid, sc_rtid) in term_category_tbl.
  FOR rec2 IN select rid, rtid from top_categories where kbid=thekbid
  LOOP
   delete from term_category_tbl tc1 where (rid, rtid, cat_rid, cat_rtid) in (select tc2.rid, tc2.rtid, ne.rid2, ne.rtid2 
      from term_category_tbl tc2, get_neighborhood(rec2.rid, rec2.rtid, subclassPid, false, 0, true, 
      true, true, false) ne where tc2.cat_rid = rec2.rid and tc2.cat_rtid = rec2.rtid);
--    raise notice 'delete redundant descendants of category %, %', rec2.desc_rid, rec2.desc_rtid;
  END LOOP;
  raise notice 'Deleted all redundant descendants of categories.';

  -- fill the label for categories
  update term_category_tbl tc set category = (select n.name from graph_nodes n, graph_edges e, property p 
    where p.id = e.pid and   p.name = 'altLabel' and e.rid2 = n.rid and e.rtid2 = n.rtid and tc.cat_rid = e.rid1 and 
    tc.cat_rtid = e.rtid1)
  where category is null;

  update term_category_tbl set category = get_name(cat_rid, cat_rtid, true, null) where category is null;
  raise notice 'category label is updated';

  -- fill the concept map types
  sql := 'select * from get_ids('||quote_literal(cm_type_list_str)||', true, false, '||kb_expr||') t where t.rtid = 1';

  FOR theRid, theRtid, theName IN EXECUTE sql
  LOOP
    update term_category_tbl tc set (cm_type_rid, cm_type_rtid, cm_type) = (theRid, theRtid, theName) 
      where (tc.rid, tc.rtid) in (select rid, rtid from get_descendant_nodes_in_dag(theRid, theRtid, subclassPid, 0, true, true));
  END LOOP;

--  EXECUTE 'drop table tmp_cat_tree';
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

