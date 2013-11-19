CREATE OR REPLACE FUNCTION get_name(theRid INTEGER, theRtid INTEGER, prefLabel BOOLEAN, defaultName TEXT)
  RETURNS TEXT AS $$
/*
  Get the name of node identified by (theRid, theRtid). If prefLabel is true, use its rdfs:label
  as node name. If prefLabel is false or rdfs:label is not set, just return the name.
 */
   DECLARE
    rec RECORD;
    result TEXT;  
  BEGIN
    IF prefLabel THEN
      select label into result from graph_nodes where rid = theRid and rtid = theRtid;
    ELSE
      select name into result from graph_nodes where rid = theRid and rtid = theRtid;
    END IF;    
    
    IF result is null THEN
      result := defaultName;
    END IF;
    RETURN result;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_names(idList INTEGER[][], prefLabel BOOLEAN, defaultNames TEXT[])
  RETURNS SETOF node1 AS $$
/*
  Get the name of node identified by (theRid, theRtid). If prefLabel is true, use its rdfs:label
  as node name. If prefLabel is false or rdfs:label is not set, just return the name.
 */
  DECLARE
    rec RECORD;
    result TEXT; 
    defaultName TEXT;
  BEGIN
    FOR i IN ARRAY_LOWER(idList, 1)..ARRAY_UPPER(idList, 1) LOOP
	  IF defaultNames is not null and i < ARRAY_UPPER(defaultNames, 1) THEN
        defaultName := defaultNames[i];
      END IF;
      FOR rec IN select idList[i][1], idList[i][2], get_name(idList[i][1], idList[i][2], prefLabel, defaultName) 
      LOOP
        RETURN NEXT rec;
      END LOOP;
    END LOOP;
	RETURN;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_id(term VARCHAR, useLabel BOOLEAN, is_synonym_searched BOOLEAN, kbid INTEGER[])
    RETURNS SETOF node1 AS $$
  DECLARE
    rec RECORD;
  BEGIN
    FOR rec IN select * from get_ids(quote_literal(term), useLabel, is_synonym_searched, kbid)
    LOOP
      RETURN NEXT rec;
    END LOOP;
    RETURN;
  END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION get_ids(term_list_str VARCHAR,  useLabel BOOLEAN, is_synonym_searched BOOLEAN, kbidList INTEGER[])
    RETURNS SETOF node1 AS $$
  /*
     Get the IDs of the term list.
     @param term_list_str: the serialized term list in the format of '''AAA'', ''BB BBB'''.
     @param useLabel: if true, return the label as term name.
     @param is_synonym_searched: if true, consider the term as synonym as some concept.
     @param kbid: the knowledge base id list. empty list or null to search all kb.
   */
  DECLARE
    kb_condition text := '';
    outCol text := 'name';
    term_cond text := '';
    sql text := '';
    sql2 text := '';
    rec RECORD;
    init_pnames text := '';
   BEGIN
    IF term_list_str IS NULL or length(term_list_str) < 3 THEN
      RETURN;
    END IF;

    IF kbidList is not null AND array_upper(kbidList, 1) is not null THEN
      IF array_lower(kbidList, 1) = array_upper(kbidList, 1) THEN
        IF kbidList[array_lower(kbidList, 1)] > 0 THEN
          kb_condition := ' and n1.kbid = '||kbidList[array_lower(kbidList, 1)];
        END IF;
      ELSE
        kb_condition := ' and n1.kbid in (';
        FOR i IN ARRAY_LOWER(kbidList, 1)..ARRAY_UPPER(kbidList, 1) 
        LOOP
          kb_condition := kb_condition ||kbidList[i]||',';
        END LOOP;
        kb_condition := trim(trailing ',' from kb_condition) ||')';
      END IF;
    END IF;
--raise notice 'kb_condition = %', kb_condition;

    term_cond := 'lower(n1.'||outCol||') in ('||lower(term_list_str)||')';
    IF useLabel THEN
      outCol := 'label';
      term_cond := '('||term_cond||' or lower(n1.'||outCol||') in ('||lower(term_list_str)||')'||')';
    END IF;

    IF is_synonym_searched THEN
      init_pnames := ' and p.name in (''prefLabel'', ''label'', ''synonym'', ''abbrev'', ''hasExactSynonym'', ''hasRelatedSynonym'', ''acronym'', ''taxonomicCommonName'', ''ncbiTaxScientificName'', ''ncbiTaxGenbankCommonName'', ''ncbiTaxBlastName'', ''ncbiIncludesName'', ''ncbiInPartName'', ''hasNarrowSynonym'', ''misspelling'', ''misnomer'', ''hasBroadSynonym'')';
      sql := ' UNION select rid1 as theRid, rtid1 as theRtid, n1.'||outCol||' as theName from graph_nodes n1, graph_edges r1, property p where '
        ||term_cond||' and n1.rid = r1.rid2 and n1.rtid = r1.rtid2 and p.id = r1.pid ' || init_pnames || kb_condition;
    END IF;

    sql2 := 'select distinct rid as theRid, rtid as theRtid, '||outCol||' as theName from graph_nodes n1 where ' || term_cond || kb_condition || sql;
--raise notice 'n1_names = %; sql2 = %', term_list_str, sql2;

    FOR rec IN EXECUTE sql2 LOOP
      RETURN NEXT rec;
    END LOOP;
    RETURN;
  END; 
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compose_pid_condition(pidList INTEGER[], include_subproperties BOOLEAN, negate BOOLEAN)
    RETURNS text AS $$
  DECLARE
    pid_condition text := '';
    subPropertyOf_id int;
    sub_pid INTEGER;
  BEGIN
    IF pidList is null OR ARRAY_UPPER(pidList, 1) is null THEN
      return '';
    END IF;

    pid_condition := ' and pid ';
    IF include_subproperties THEN
      IF negate THEN
        pid_condition := pid_condition || 'NOT ';
      END IF;
      pid_condition := pid_condition || 'IN (';
      FOR i IN ARRAY_LOWER(pidList, 1)..ARRAY_UPPER(pidList, 1) 
      LOOP
        pid_condition := pid_condition || pidList[i];
        -- first, get id of subPropertyOf
        select p.id into subPropertyOf_id from property p, property p2 where p.name = 'subPropertyOf' and p.kbid = p2.kbid and p2.id = pidList[i];
        -- get all descendants of pid
        FOR sub_pid IN select rid1 from get_neighborhood(pidList[i], 15, subPropertyOf_id, false, 0, true, false, false, true)
        LOOP
          pid_condition := pid_condition ||','||sub_pid;
        END LOOP;
        pid_condition := pid_condition ||',';
      END LOOP;
      pid_condition := trim(trailing ',' from pid_condition) ||')';
    ELSE  -- does not include subproperties
      IF ARRAY_UPPER(pidList,1) = ARRAY_LOWER(pidList,1) AND ARRAY_UPPER(pidList,1) > 0 THEN -- one pid only
        IF negate THEN
          pid_condition := pid_condition ||'!';
        END IF;
        pid_condition := pid_condition ||'= '||pidList[ARRAY_LOWER(pidList, 1)];
      ELSE
        IF negate THEN
          pid_condition := pid_condition || 'NOT ';
        END IF;
        pid_condition := pid_condition || 'IN (';
        FOR i IN ARRAY_LOWER(pidList, 1)..ARRAY_UPPER(pidList, 1) 
        LOOP
          pid_condition := pid_condition ||pidList[i]||',';
        END LOOP;
        pid_condition := trim(trailing ',' from pid_condition) || ')';
      END IF;
    END IF;
    return pid_condition;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_neighborhood(idList INTEGER[][], pidList INTEGER[], excludedPidList INTEGER[],
       prefLabel boolean, maxHops INTEGER, no_hidden BOOLEAN, include_subproperties BOOLEAN, class_only BOOLEAN, dir_incoming BOOLEAN)
  RETURNS SETOF edge2 AS $$
  
/*
  Get neighbors of nodes. If a DAG index is created for the pid and suitable for the query, use the index for fast
  search. Otherwise, do a BFS.
  @param idList a list of (rid, rtid).
  @param pidList property id list. for example, the id of property subClassOf, part-of.
  @param excludedPidList property list to be excluded from the query.
  @param prefLabel When true, use rdfs:label as node name, if the label is available. Otherwise, use name column.
  @param maxHops max number of levels to search
  @param no_hidden if true, ignore hidden edges.
  @param include_subproperties if true, include the edges whose edge label is a subPropertyOf pid
  @param class_only if true, retrieve only those neighbors who are classes.
  @param dir_incoming get neighbors which come into the idList.
*/
  DECLARE
    -- element of ancestor array: rid, rtid, init_depth (0)
    neighbors integer[][] ;
    rec RECORD;
    done boolean := false;
    curIdx integer := 1; -- current node index in ancestor array.
    depth integer := 1;
    hidden_condition text := '';
    pid_condition text := '';
    sql text;
    hasAdded boolean := false;
    theRid INTEGER;
    theRtid INTEGER;
    thePid INTEGER;
    fcn_name TEXT;
    rid1_expr TEXT;
    rtid1_expr TEXT;
    rid2_expr TEXT;
    rtid2_expr TEXT;
  BEGIN
    -- add initial nodes into the ancestors queue.
    FOR curIdx IN ARRAY_LOWER(idList,1)..ARRAY_UPPER(idList,1) LOOP
      if neighbors is null then
        neighbors := ARRAY[[idList[curIdx][1], idList[curIdx][2], 0]];
      else
        neighbors := neighbors || ARRAY[idList[curIdx][1], idList[curIdx][2], 0];
      end if;
    END LOOP;

    -- check if dag index is built for pid
    IF ARRAY_UPPER(pidList,1) = ARRAY_LOWER(pidList,1) AND (excludedPidList is null OR ARRAY_UPPER(excludedPidList, 1) IS NULL) THEN -- one pid only
     FOR curIdx IN ARRAY_LOWER(idList,1)..ARRAY_UPPER(idList,1) LOOP
      theRid := idList[curIdx][1];
      theRtid := idList[curIdx][2];
      thePid := pidList[ARRAY_LOWER(pidList,1)];
      IF dir_incoming THEN
        fcn_name := 'get_descendant_nodes_in_dag';
        rid1_expr := 't.rid';
        rtid1_expr := 't.rtid';
        rid2_expr := theRid;
        rtid2_expr := theRtid;
      ELSE
        fcn_name := 'get_ancestor_nodes_in_dag';
        rid2_expr := 't.rid';
        rtid2_expr := 't.rtid';
        rid1_expr := theRid;
        rtid1_expr := theRtid;
      END IF;

      sql := 'select distinct '||rid1_expr||' as rid1, '||rtid1_expr||' as rtid1, get_name('||rid1_expr||', '||rtid1_expr||', '
             ||prefLabel||', null) as name1, '||rid2_expr||' as rid2, '||rtid2_expr||' as rtid2, get_name('||rid2_expr||', '
             ||rtid2_expr||', '||prefLabel||', null) as name2,'||thePid||' as pid, get_name('||thePid||', 15, '||prefLabel
             ||', null) as pname from '||fcn_name||'('||theRid||', '||theRtid||', '||thePid||', '||maxHops||', '
             ||include_subproperties||', '||no_hidden||') as t';
      FOR rec IN EXECUTE sql
      LOOP
        done := true;
        IF class_only and (rec.rtid1 != 1 OR rec.rtid2 != 1) THEN
          continue;
        END IF;
        RETURN NEXT rec;
      END LOOP;
     END LOOP;
    END IF;
    IF done THEN RETURN; END IF;

    IF no_hidden THEN
      hidden_condition := ' and hidden = false';
    END IF;

    pid_condition := compose_pid_condition(pidList, include_subproperties, false);
    pid_condition := pid_condition || compose_pid_condition(excludedPidList, include_subproperties, true);

    curIdx := 1;
    LOOP
      EXIT WHEN curIdx > array_upper(neighbors, 1) or (maxHops != 0 and neighbors[curIdx][3] >= maxHops); 
      IF dir_incoming THEN
        sql := 'SELECT r.rid1, r.rtid1, get_name(r.rid1, r.rtid1, '||prefLabel||', null) as name1, '||
          'r.rid2, r.rtid2, get_name(r.rid2, r.rtid2, '||prefLabel||', null) as name2, r.pid, get_name(r.pid, 15, '||prefLabel||', null) as pname '||
          'FROM graph_edges r WHERE r.rid2 = '||neighbors[curIdx][1]||' and r.rtid2 = '||neighbors[curIdx][2]|| 
          ' and (r.rid1 != r.rid2 OR r.rtid1 != r.rtid2) '|| pid_condition||hidden_condition; 
      ELSE
        sql := 'SELECT r.rid1, r.rtid1, get_name(r.rid1, r.rtid1, '||prefLabel||', null) as name1, '||
          'r.rid2, r.rtid2, get_name(r.rid2, r.rtid2, '||prefLabel||', null) as name2, r.pid, get_name(r.pid, 15, '||prefLabel||', null) as pname '||
          'FROM graph_edges r WHERE r.rid1 = '||neighbors[curIdx][1]||' and r.rtid1 = '||neighbors[curIdx][2]|| 
          ' and (r.rid1 != r.rid2 OR r.rtid1 != r.rtid2) '|| pid_condition||hidden_condition; 
      END IF;
--raise notice 'sql = %', sql;
      -- exit when there is no more elements in neighbors list or we have reached the maxHops level.
      FOR rec IN EXECUTE sql LOOP      
        IF dir_incoming THEN
          theRid := rec.rid1;
          theRtid := rec.rtid1;
        ELSE
          theRid := rec.rid2;
          theRtid := rec.rtid2;
        END IF;

        -- check if rec.rid1, rec.rtid1 is already in the neighbors array.
        FOR i IN ARRAY_LOWER(neighbors,1)..ARRAY_UPPER(neighbors,1) LOOP
          hasAdded := neighbors[i][1] = theRid and neighbors[i][2] = theRtid;
          EXIT WHEN hasAdded;
        END LOOP;
        
        IF hasAdded = false THEN
          -- add the new neighbors to neighborhood array
          neighbors := neighbors || ARRAY[[theRid, theRtid, neighbors[curIdx][3]+1]];
        END IF;

        IF class_only and (rec.rtid1 != 1 OR rec.rtid2 != 1) THEN
          continue;
        END IF;
        RETURN NEXT rec; -- add the record to final result.
      END LOOP;
      curIdx := curIdx + 1;
    END LOOP; 
  RETURN;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_neighborhood(rid INTEGER, rtid INTEGER, pid INTEGER, prefLabel boolean, 
       maxHops INTEGER, no_hidden BOOLEAN, include_subproperties BOOLEAN, class_only BOOLEAN, dir_incoming BOOLEAN) 
  RETURNS SETOF edge2 AS $$
/*
  Get neighborhood of a node (rid, rtid). If a DAG index is created for the pid, use the index for fast
  search. Otherwise, do a BFS.
  @param rid resource id. A pair of rid and rtid identifies a term.
  @param rtid resource type id.
  @param pid property id. for example, the id of property subClassOf.
  @param prefLabel if true, use rdfs:label, if available, as node name. Otherwise, use name column.
  @param maxHops max number of levels to search
  @param no_hidden if true, ignore hidden edges.
  @param include_subproperties if true, include the edges whose edge label is a subPropertyOf pid
  @param class_only if true, retrieve only those neighbors who are classes.
  @param dir_incoming get neighbors which come into the idList.
  @return set of edge2 objects
*/
  DECLARE
    rec RECORD;
  BEGIN
    FOR rec IN select * from get_neighborhood(ARRAY[[rid, rtid]], ARRAY[pid], null, prefLabel, maxHops, 
                           no_hidden, include_subproperties, class_only, dir_incoming)
    LOOP
      RETURN NEXT rec;
    END LOOP;
    RETURN;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_neighborhood(term_list_str VARCHAR, pname_list_str VARCHAR, excluded_pname_list_str VARCHAR, kbid INTEGER, 
       prefLabel boolean, maxHops INTEGER, no_hidden BOOLEAN, include_subproperties BOOLEAN, class_only BOOLEAN, 
       is_synonym_searched BOOLEAN, dir_incoming BOOLEAN) 
  RETURNS SETOF edge2 AS $$
/*
  Get neighborhood of a list of nodes. If a DAG index is created for the pid, use the index for fast
  search. Otherwise, do a BFS.
  @param term_list_str. The string representation of the term list. The format is '''AAA'', ''BB BB'', ''VV V VVV'''.
         For instance, '''Cerebellum'', ''Purkinje Cell'', ''Cerebellar Cortex'''.
  @param pname_list_str. The string representation of the properties. The format is same as term_list_str. e.g. 
         '''has_part'', ''subClassOf'''.
  @param kbid The knowledge base id.
  @param prefLabel if true, use rdfs:label, if available, as node name. Otherwise, use name column.
  @param maxHops max number of levels to search
  @param no_hidden if true, ignore hidden edges.
  @param include_subproperties if true, include the edges whose edge label is a subPropertyOf pid
  @param is_synonym_searched true to search the term as a synonym. False to search as name.
  @param class_only if true, return the class descendants only.
*/
  DECLARE
    rec RECORD;
    rec1 RECORD;
    sql text;
    idList INTEGER[][];
    kb_expr text := 'null::int[]';
  BEGIN
    IF kbid is not null THEN
      kb_expr := 'array['||kbid||']';
    END IF;
    sql := 'select * from get_ids('||quote_literal(term_list_str)||', '||prefLabel||', '||is_synonym_searched
          ||', '||kb_expr||') ';
    IF class_only THEN
      sql := sql ||' t where t.rtid = 1';
    END IF;

    FOR rec1 IN EXECUTE sql LOOP
      IF idList is null THEN
        idList := ARRAY[[rec1.rid, rec1.rtid]];
      ELSE
        idList := idList || ARRAY[[rec1.rid, rec1.rtid]];
      END IF;
    END LOOP;

    -- no matching nodes found 
    IF array_upper(idList, 1) is null THEN
      RETURN;
    END IF;
    FOR rec IN select * from get_neighborhood(idList, pname_list_str, excluded_pname_list_str, kbid, prefLabel, 
                                             maxHops, no_hidden, include_subproperties, class_only, dir_incoming)
    LOOP
      IF class_only and (rec.rtid1 != 1 OR rec.rtid2 != 1) THEN
        continue;
      END IF;
      RETURN NEXT rec;
    END LOOP;
    RETURN;
  END;
$$ LANGUAGE plpgsql;
 
CREATE OR REPLACE FUNCTION get_neighborhood(idList INTEGER[][], pname_list_str VARCHAR, excluded_pname_list_str VARCHAR, kbid INTEGER, prefLabel boolean, 
       maxHops INTEGER, no_hidden BOOLEAN, include_subproperties BOOLEAN, class_only BOOLEAN, dir_incoming BOOLEAN) 
    RETURNS SETOF edge2 AS $$
/*
  Get neighborhood of a list of nodes. If a DAG index is created for the pid, use the index for fast
  search. Otherwise, do a BFS.
  @param idList a list of term ids. Each inner array has two elements: rid and rtid. An example input is ARRAY[[1234, 1], [3252, 1]].
  @param pname_list_str. The string representation of the properties. The format is same as term_list_str. e.g. 
         '''has_part'', ''subClassOf'''.
  @param excluded_pname_list_str. the property names to be excluded. Same format as pname_list_str. null means nothing to be excluded.
  @param kbid The knowledge base id. 0 means the same knowledge base as idList[1]. null or -1 means all knowledge bases.
  @param prefLabel if true, use rdfs:label, if available, as node name. Otherwise, use name column.
  @param maxHops max number of levels to search
  @param no_hidden if true, ignore hidden edges.
  @param include_subproperties if true, include the edges whose edge label is a subPropertyOf pid
  @param class_only if true, return the class descendants only.
  @param dir_incoming get neighbors which come into the idList.
  @return set of edge2 objects
*/
  DECLARE
    rec RECORD;
    sql text;
    pidList INTEGER[] := null;
    excludedPidList INTEGER[] := null;
    kbid_condition TEXT := '';
    tbl_expr TEXT := '';
  BEGIN
    IF idList is null OR array_upper(idList, 1) is null THEN
      return;
    END IF;

    IF kbid is not null AND kbid > 0 THEN
      kbid_condition := ' and p.kbid = '||kbid;
    ELSIF kbid = 0 THEN
      kbid_condition := ' and p.kbid = n.kbid and n.rid = '||idList[1][1]||' and n.rtid = '||idList[1][2];
      tbl_expr := ', graph_nodes n ';
    END IF;
    
    -- prepare properties to be included.
    IF pname_list_str is not null AND pname_list_str != '*' AND pname_list_str != '' THEN  
      sql := 'select p.id from property p'||tbl_expr||' where p.name in ('||pname_list_str||')'||kbid_condition;
      FOR rec IN EXECUTE sql
      LOOP
        -- get matched property id(s).
        IF pidList is null THEN
          pidList := ARRAY[rec.id];
        ELSE
          pidList := pidList || rec.id;
        END IF;
      END LOOP;
 
      -- no matching properties found
      IF array_upper(pidList, 1) is null THEN
        RETURN;
      END IF;
    END IF;

    -- prepare properties to be excluded.
    IF excluded_pname_list_str is not null AND excluded_pname_list_str != '*' AND excluded_pname_list_str != '' THEN  
      sql := 'select p.id from property p'||tbl_expr||' where p.name in ('||excluded_pname_list_str||')'||kbid_condition;
--raise notice 'xcld sql : %', sql;
      FOR rec IN EXECUTE sql
      LOOP
        -- get matched property id(s).
        IF excludedPidList is null THEN
          excludedPidList := ARRAY[rec.id];
        ELSE
          excludedPidList := excludedPidList || rec.id;
        END IF;
      END LOOP;
    END IF;
    FOR rec IN select distinct * from get_neighborhood(idList, pidList, excludedPidList, prefLabel, maxHops, no_hidden, include_subproperties, class_only, dir_incoming)
    LOOP
      RETURN NEXT rec;
    END LOOP;
--raise notice 'AASDAAAAAAAASD';
    RETURN;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_root(theKbid integer, excludeOWLThing boolean, prefLabel boolean)
  RETURNS SETOF node1 AS $$
/*
  Get root class in subClassOf hierarchy.
  @param kbid: the knowledge base id
  @excludeOWLThing: if true, ignore owl:Thing as root. If false, allow owl:Thing as root
  @prefLabel: if true, prefer using property rdfs:label as class name. Otherwise, use the real class name.
*/
  DECLARE
    rec RECORD;
    sql TEXT;
    prefLabelStr VARCHAR(5) := 'false';
  BEGIN
    IF prefLabel THEN
      prefLabelStr := 'true';
    END IF;

    IF excludeOWLThing THEN
      sql := 'select distinct s1.childid as rid, 1 as rtid, get_name(s1.childid, 1, '
             || prefLabelStr ||', c1.name) from subclassof s1, primitiveclass c1'
             || ' where parentid = (SELECT id from primitiveclass where browsertext = ''owl:Thing'' and kbid = '
             || theKbid ||') and parent_rtid = 1 and child_rtid = 1 and not exists (select * from subclassof s2'
             || ' where s1.childid = s2.childid and s2.child_rtid = 1 and s1.parentid != s2.parentid and'
             || ' s2.parent_rtid = 1) and s1.childid = c1.id and c1.is_system = false';
    ELSE
      sql := 'select distinct parentid as rid, parent_rtid as rtid, get_name(parentid, parent_rtid, '
             || prefLabelStr ||', c1.name) from subclassof s1, primitiveclass c1 where s1.kbid = '
             || theKbid||' and parent_rtid = 1 and' 
             || ' not exists (select * from subclassof s2 where s1.parentid = s2.childid and'
             || ' s1.parent_rtid = s2.child_rtid) and c1.id = s1.parentid and c1.is_owlclass = true';
    END IF;
    
    FOR rec IN EXECUTE sql LOOP
      RETURN NEXT rec;
    END LOOP;
    
    RETURN;

  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION search_term(searchStr text, theKbid integer, searchType bit, prefLabel boolean)
  RETURNS SETOF node1 AS $$
  /*
    search term using LIKE in the specified ontology.
    @param searchStr: the string to be searched.
    @param theKbid: the knowledge base id to be searched. If theKbid <= 0, all knowledge base is searched.
    @param searchType: the resource type flag in bits. say the searchType has bit value DCBA. 
                       If bit A is 1, then include primitive class type.
                       If bit B is 1, then include individual type.
                       If bit C is 1, then include property type.
                       If bit D is 1, then include literal type.
    @param prefLabel: if true, search rdfs:label first. And use the label as resource name.
  */
  DECLARE
    /*
     :1 - lower_case of the search string, with single quotes escaped.
     :2 - kbid condition
     :3 - resource type condition if prefLabel is false. If prefLabel is true, empty string.
     :4 - project columns
    */
    sql1 text := 'select rid, rtid, label from graph_nodes n where label ilike ''%:1%'' ESCAPE ''#'' :2 :3';
    sql2 text := 'select n.rid, n.rtid, n2.label from graph_nodes n, graph_nodes n2, graph_edges r, property p '
                 || 'where r.rid2=n2.rid and r.rtid2=n2.rtid and r.pid=p.id and p.name=''label'' and r.rid1=n.rid '
                 || 'and r.rtid1=n.rtid and n2.label ilike ''%:1%'' ESCAPE ''#'' :2 :3';
    sql text;
    str text;
    kbCondition varchar(16) := ' ';
    typeCondition varchar(64);
    rec record;
    hasMatch boolean := false;
    i integer;
  BEGIN
    -- sanity check
    IF searchStr is null OR length(searchStr) = 0 THEN
      RETURN;
    END IF;
    
    -- prepare search string
    str := searchStr;
    str := replace(str, '#', '##');
    str := replace(str, '_', '#_');
    str := replace(str, '%', '#%');
    str := replace(str, '''', '''''');
    
    -- prepare kbid condition
    IF theKbid > 0 THEN
      kbCondition := ' AND n.kbid = '||theKbid;
    END IF;
    
    -- prepare search type condition
    typeCondition := set_resource_type(searchType);
    IF length(typeCondition) > 0 THEN
      typeCondition := ' AND n.rtid IN ('||typeCondition||')';
    ELSE
      typeCondition := ' ';
    END IF;
    
    -- choose template 
    IF prefLabel THEN
      sql := sql2;
    ELSE
      sql := sql1;
    END IF;    

    FOR i IN 1..2 LOOP
      -- run at most twice. If the first run does not produce any result, try the other sql statement

      -- replace parameters    
      sql := replace(sql, ':1', str);
      sql := replace(sql, ':2', kbCondition);
      sql := replace(sql, ':3', typeCondition);
    
raise notice 'sql = %', sql;
      -- run sql, put row in return set.
      FOR rec IN EXECUTE sql
      LOOP
        hasMatch := true;
        RETURN NEXT rec;
      END LOOP;

      EXIT WHEN hasMatch;

      -- no result in first run, try the other statement.
      IF prefLabel THEN
        sql := sql1;
      ELSE
        sql := sql2;
      END IF;   
    END LOOP;
    RETURN;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_resource_type(theType bit) RETURNS VARCHAR AS $$
  /*
    compose rtid condition based on the input type. say theType has value DCBA. 
    If bit A is 1, then include primitive class type.
    If bit B is 1, then include individual type.
    If bit C is 1, then include property type.
    If bit D is 1, then include literal type.
    
  */
  
  DECLARE
    type_masks bit(4)[4] := ARRAY[B'0001', B'0010', B'0100', B'1000'];    
    type_ids varchar(2)[4] := ARRAY['1', '12', '15', '13']; 
    i integer;
    result VARCHAR(64) := '';
  BEGIN
    FOR i IN 1..4 LOOP
      IF theType & type_masks[i] != B'0000' THEN
        IF length(result) > 0 THEN
          result := result ||', ';
        END IF;
        result := result || type_ids[i];
      END IF;
    END LOOP;
    
    RETURN result;
  END;
  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_ontology(rid int, rtid int) RETURNS VARCHAR AS $$
  DECLARE
    sql_class TEXT := 'select ns.url||r.name as url from primitiveclass r, namespace ns where r.id = :1 and r.nsid = ns.id';
    sql_property TEXT := 'select ns.url||r.name as url from property r, namespace ns where r.id = :1 and r.nsid = ns.id';
    sql_individual TEXT := 'select ns.url||r.name as url from individual r, namespace ns where r.id = :1 and r.nsid = ns.id';
    sql TEXT;
    rec RECORD;
  BEGIN
  	IF rtid = 1 THEN
      sql := sql_class;
    ELSIF rtid = 15 THEN
      sql := sql_property;
    ELSIF rtid = 12 THEN
      sql := sql_individual;
    ELSE
      return null;
    END IF;

    sql := replace(sql, ':1', ''||rid);

    FOR rec IN EXECUTE sql LOOP
      return rec.url;
    END LOOP;

    return null;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION has_inferred_def(term text, kbid INTEGER[]) RETURNS BOOLEAN AS $$
  DECLARE
    rec RECORD;
    result BOOLEAN;
  BEGIN
    FOR rec IN select * from get_id(term, true, true, kbid)
    LOOP
      result := has_inferred_def(rec.rid, rec.rtid);
      IF result = true THEN
        RETURN result;
      END IF;
    END LOOP;
    RETURN FALSE;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION has_inferred_def(rid int, rtid int) RETURNS BOOLEAN AS $$
  -- If class A has an equivalent class which is an anonymous class, then return true. Otherwise, return false.
  DECLARE
    res_rtids INTEGER[] := ARRAY[2,3,7,8,9,10,11];
    rec RECORD;
  BEGIN
    select into rec * from equivalentclass where classid1 = rid and class_rtid1 = rtid and 
      class_rtid2 = any(res_rtids);
    IF rec.classid2 IS NULL THEN
      return false;
    ELSE
      return true;
    END IF;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION has_inferred_def(term_list_str VARCHAR, useLabel BOOLEAN, 
      is_synonym_searched BOOLEAN, kbidList INTEGER[])
    RETURNS SETOF node1 AS $$
  /*
     return the terms that have inferred definition. If class A has an equivalent class 
     which is an anonymous class, it means that class A has inferred definition. 
     @param term_list_str: the serialized term list in the format of '''AAA'', ''BB BBB'''.
     @param useLabel: if true, return the label as term name.
     @param is_synonym_searched: if true, consider the term as synonym as some concept.
     @param kbidList: the knowledge base id list. empty list or null to search all kb.
     @return those terms that have inferred definition.
   */
  DECLARE
    rec RECORD;
    result BOOLEAN;
  BEGIN
    FOR rec IN select * from get_ids(term_list_str, useLabel, is_synonym_searched, kbidList)
    LOOP
      result := has_inferred_def(rec.rid, rec.rtid);
      IF result = true THEN
        RETURN NEXT rec;
      END IF;
    END LOOP;
    RETURN;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION has_inferred_def(idList INTEGER[][])
  RETURNS SETOF node1 AS $$
/*
   return the terms that have inferred definition. If class A has an equivalent class 
   which is an anonymous class, it means that class A has inferred definition. 
   @param idList: input ID list
   @return those class ID pairs that have inferred definition. 
 */
  DECLARE
    rec RECORD;
    result BOOLEAN; 
  BEGIN
    FOR i IN ARRAY_LOWER(idList, 1)..ARRAY_UPPER(idList, 1) LOOP
      FOR rec IN select idList[i][1] as rid, idList[i][2] as rtid, get_name(idList[i][1], idList[i][2], true, null) 
      LOOP
        result := has_inferred_def(rec.rid, rec.rtid);
        IF result = true THEN
          RETURN NEXT rec;
        END IF;
      END LOOP;
    END LOOP;
    RETURN;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_kb(kbName VARCHAR)
  RETURNS boolean AS $$
  DECLARE
    dag_id INTEGER;
    v_kbid Integer;
  BEGIN
    FOR dag_id IN select d.id from dag_index_metadata d, kb k 
      where d.kbid = k.id and k.name = kbName 
    LOOP
      perform delete_dag_index(dag_id);
    END LOOP;
    
    select id into v_kbid from kb where name = kbName;

    if v_kbid is null then
      raise 'Knowledgebase % not found in db.', kbName;    
    end if;
/*    
    delete from graph_edges_all where kbid = v_kbid;
    delete from graph_nodes_all where kbid = v_kbid;
    delete from allvaluesfromclass where kbid = v_kbid;
    delete from alldifferentindividual where kbid = v_kbid;
    delete from cardinalityclass where kbid = v_kbid;
    delete from complementclass where kbid = v_kbid;
    delete from datarange where kbid = v_kbid;
    delete from datatype_restriction where kbid = v_kbid;
    delete from differentindividual where kbid = v_kbid;
    delete from disjointclass where kbid = v_kbid;
    delete from disjointunionclass where kbid = v_kbid;
    delete from domain where kbid = v_kbid;

    delete from equivalentclass where kbid = v_kbid;
    delete from equivalentclassgroup where kbid = v_kbid;
    delete from equivalentproperty where kbid = v_kbid;
 
    delete from hasself where kbid = v_kbid;
    delete from hasvalue where kbid = v_kbid;
    delete from intersectionclass where kbid = v_kbid;
    delete from inversepropertyof where kbid = v_kbid;
    delete from maxcardinalityclass where kbid = v_kbid;
    delete from mincardinalityclass where kbid = v_kbid;
    delete from oneof where kbid = v_kbid;

    delete from relationship where kbid = v_kbid;
    delete from sameindividual where kbid = v_kbid;
    delete from somevaluesfromclass where kbid = v_kbid;
    delete from subpropertyof   where kbid = v_kbid;
    delete from subclassof      where kbid = v_kbid;
    delete from typeof where kbid = v_kbid;
    delete from unionclass where kbid = v_kbid;

    delete from range           where kbid = v_kbid;

    delete from datatype where kbid = v_kbid;
    delete from property        where kbid = v_kbid;
    delete from ontologyuri     where kbid = v_kbid;
    delete from namespace       where kbid = v_kbid;
    delete from literal         where kbid = v_kbid;
    delete from individual      where kbid = v_kbid;
    delete from primitiveclass where kbid = v_kbid;
    delete from ontologyimport where kbid = v_kbid;
*/
    insert into kb_history (id, name, creation_date, removal_date)
    select  id,name, creation_date, current_time
    from kb where name = kbName;
    delete from kb where name = kbName;
    return true;
  END;
$$ LANGUAGE plpgsql;

create or replace function str_aggregate(str1 text, str2 text) returns text as $$
begin
  if (length(str1) > 0 ) then
    return str1 || ';' || str2;
  else
    return str2;
  end if;
end;
$$ language 'plpgsql';

-- create the aggregate function

create aggregate stragg (basetype=text, sfunc=str_aggregate,
stype=text, initcond='' );

-- select (ARRAY[[158065, 1], [160218, 1]])[i][1] from generate_series(1,2) i;

