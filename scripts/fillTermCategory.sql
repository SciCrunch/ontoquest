select drop_if_exists('term_category');

select drop_if_exists('term_category_tbl');

select drop_if_exists('nif_term');

CREATE TABLE nif_term (
  term text,
  tid varchar(64),
  rid integer,
  rtid integer,
  inferred boolean,
  primary key (rid, rtid, term)
);

CREATE INDEX nif_term_idx on nif_term (lower(term));

CREATE TABLE term_category_tbl (
  rid integer,
  rtid integer,
  category text,
  cat_rid integer,
  cat_rtid integer
  ,primary key (rid, rtid, cat_rid, cat_rtid)
);

CREATE OR REPLACE VIEW term_category AS
  select t.term, t.tid, t.rid, t.rtid, t.inferred, tc.category, tc.cat_rid, tc.cat_rtid from nif_term t left outer join term_category_tbl tc
  on (t.rid = tc.rid and t.rtid = tc.rtid);

CREATE OR REPLACE FUNCTION fill_term_category(theKbid INTEGER, category_list_str TEXT) RETURNS VOID AS $$
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

  -- fill out tid and term
  FOR rec1 IN select rid, rtid, name from graph_nodes where kbid = theKbid and rtid = 1 and name != 'Thing' --limit 50
  LOOP
    FOR rec2 IN select distinct name2 from get_neighborhood(ARRAY[[rec1.rid, rec1.rtid]], synPropIDs, null, false,1,true,false, false,false)
    LOOP
      insert into nif_term (term, tid, rid, rtid) values (rec2.name2, rec1.name, rec1.rid, rec1.rtid);
    END LOOP;

  END LOOP;

  -- fill inferred flag
  UPDATE nif_term SET inferred = has_inferred_def(rid, rtid);
  
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
      from get_neighborhood(rec1.desc_rid, rec1.desc_rtid, subclassPid, false, 0, true, false, true, true) t, nif_term nt
      where t.rid1 = nt.rid and t.rtid1 = nt.rtid;
    raise notice 'add all descendants of category %, %', rec1.desc_rid, rec1.desc_rtid;
  END LOOP;

  -- For every (rid, rtid, cat_rid, cat_rtid) pair, get category (cat_rid, cat_rtid)'s superclasses (sc_rid, sc_rtid), 
  -- remove all pairs (rid, rtid, sc_rid, sc_rtid) in term_category_tbl.
  FOR rec2 IN select desc_rid, desc_rtid from tmp_cat_tree
  LOOP
   delete from term_category_tbl tc1 where (rid, rtid, cat_rid, cat_rtid) in (select tc2.rid, tc2.rtid, ne.rid2, ne.rtid2 
      from term_category_tbl tc2, get_neighborhood(rec2.desc_rid, rec2.desc_rtid, subclassPid, false, 0, true, 
      false, true, false) ne where tc2.cat_rid = rec2.desc_rid and tc2.cat_rtid = rec2.desc_rtid);
    raise notice 'delete redundant descendants of category %, %', rec2.desc_rid, rec2.desc_rtid;
  END LOOP;

  -- fill the label for categories
  update term_category_tbl tc set category = (select n.name from graph_nodes n, graph_edges e, property p 
    where p.id = e.pid and   p.name = 'altLabel' and e.rid2 = n.rid and e.rtid2 = n.rtid and tc.cat_rid = e.rid1 and 
    tc.cat_rtid = e.rtid1);

  update term_category_tbl set category = get_name(cat_rid, cat_rtid, true, null) where category is null;
  raise notice 'category label is updated';

  EXECUTE 'drop table tmp_cat_tree';
END;
$$ LANGUAGE plpgsql;

-- usage
--select fill_term_category(:kbid, '''Anatomical object'', ''brain'', ''Cell'', ''Device'', ''Cellular Component'', ''extracellular structure'', ''cell line'', ''tissue section'', ''molecular entity'', ''Site'', ''Institution'', ''Platform'', ''Population'', ''Disease'', ''Biological_region'', ''gene'', ''Molecule role'', ''Drug'', ''Data object'', ''Assay'', ''Organism'', ''Data role'', ''Chemical role'', ''Reagent role'', ''familial role'', ''cell role'' , ''Quality'', ''Biomaterial_region'', ''Artifact Object'', ''Phenotype'', ''age'', ''Process'', ''behavioral process'', ''biological_process'', ''Regional Part Of Cell'', ''Resource''');
