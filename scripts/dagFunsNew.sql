---------------------------------------------
--              is_ancestor                -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION is_ancestor(rid1 INTEGER, rtid1 INTEGER, rid2 INTEGER, rtid2 INTEGER, thePid INTEGER)
  RETURNS BOOLEAN AS $$
/*
  Check if node 2 (rid2, rtid2) is an ancestor of node 1 (rid1, rtid1) in the DAG identified by thePid.
*/
DECLARE
  node_tbl_name TEXT;
  idx_tbl_name TEXT;
  sql VARCHAR(511);
  idx1 VARCHAR(511);
  idx2 VARCHAR(511);
  result BOOLEAN := false;
BEGIN
  -- find the index tables: node table and idx table
  select node_tbl, idx_tbl into node_tbl_name, idx_tbl_name from dag_index_metadata where pid = thePid;

  IF node_tbl_name IS NULL THEN
    RAISE NOTICE 'No DAG index found for property hierarchy (pid = %)', thePid;
    RETURN FALSE;
  END IF;

  sql := 'select true from '||idx_tbl_name||' t1, '||idx_tbl_name||' t2, '|| node_tbl_name
          ||' n1, '||node_tbl_name||' n2 where n1.id = t1.nid and n1.rid = $1 and n1.rtid = $2 and'
          ||' n2.id = t2.nid and n2.rid = $3 and n2.rtid = $4 and '
          ||' to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''')';

  EXECUTE sql INTO result USING rid1, rtid1, rid2, rtid2;
  IF result is null THEN
    result := false;
  END IF;
  
  return result;
EXCEPTION 
  WHEN RAISE_EXCEPTION THEN
   return false;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--    get_ancestor_nodes_in_dag            -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_ancestor_nodes_in_dag(rid INTEGER, rtid INTEGER, thePid INTEGER, level INTEGER,
    include_subproperties BOOLEAN, no_hidden BOOLEAN)
  RETURNS SETOF node2 AS $$
/*
  Return ancestor nodes up to <code>level</code> hops in the DAG specified by thePid
  @param rid resource id. A (rid, rtid) pair identifies a node uniquely.
  @param rtid resource type id.
  @param thePid the property id which identifies the edge label. A DAG index should have been created for thePid subgraph.
  @param level the maximum hops of ancestors to return. If 0, return all.
  @param include_subproperties if true, the DAG should be created with subproperties of thePid.
  @param no_hidden if true, exlcude hidden edges.
*/
DECLARE
  rec RECORD;
  node_tbl_name VARCHAR(32);
  idx_tbl_name VARCHAR(32);
  sql VARCHAR(511);
BEGIN
  -- find the index tables: node table and idx table
  select node_tbl, idx_tbl into node_tbl_name, idx_tbl_name from dag_index_metadata where pid = thePid
    and has_subproperty = include_subproperties and exclude_hidden_edge = no_hidden;
  IF node_tbl_name IS NULL THEN
    RAISE NOTICE 'No DAG index found for property hierarchy (pid = %)', thePid;
    RETURN;
  END IF;

  -- find the index for the input node.
    sql := 'select distinct n2.rid, n2.rtid from (select extract_ancestor_from_idx(i1.idx, $1) '
		   ||'from '||node_tbl_name||' n1, '||idx_tbl_name
		   ||' i1 where n1.id = i1.nid and n1.rid = $2 and n1.rtid = $3) as anc(nid), '
		   ||node_tbl_name||' n2 where anc.nid = n2.id';

		   /*
  IF level is not null and level > 0 THEN
    sql := 'select n.rid, n.rtid from '||node_tbl_name||' n, (select distinct t2.nid from '
          ||idx_tbl_name||' t1, '||idx_tbl_name||' t2, '|| node_tbl_name
          ||' n1 where (t1.path_cnt - t2.path_cnt) <= $1 and '
          ||' t1.nid != t2.nid and n1.id = t1.nid and n1.rid = $2 and n1.rtid = $3 and'
          ||' to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''')) t where n.id = t.nid';
  ELSE
    sql := 'select n.rid, n.rtid from '||node_tbl_name||' n, (select distinct t2.nid from '
          ||idx_tbl_name||' t1, '||idx_tbl_name||' t2, '|| node_tbl_name
          ||' n1 where t1.nid != t2.nid and n1.id = t1.nid and n1.rid = $2 and n1.rtid = $3 and'
          ||' to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''')) t where n.id = t.nid';
  END IF;
 */
 
  FOR rec IN EXECUTE sql using level, rid, rtid
  LOOP
    RETURN NEXT rec;
  END LOOP;
  RETURN;
EXCEPTION WHEN RAISE_EXCEPTION THEN
  RETURN;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--    get_descendant_nodes_in_dag          -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_descendant_nodes_in_dag(rid INTEGER, rtid INTEGER, thePid INTEGER, level INTEGER, include_subproperties BOOLEAN, no_hidden BOOLEAN)
  RETURNS SETOF node2 AS $$
/*
  Return descendant nodes up to <code>level</code> hops in the DAG specified by thePid
  @param rid resource id. A (rid, rtid) pair identifies a node uniquely.
  @param rtid resource type id.
  @param thePid the property id which identifies the edge label. A DAG index should have been created for thePid subgraph.
  @param level the maximum hops of ancestors to return. If 0, return all.
  @param include_subproperties if true, the DAG should be created with subproperties of thePid.
  @param no_hidden if true, exlcude hidden edges.
*/
DECLARE
  rec RECORD;
  node_tbl_name VARCHAR(32);
  idx_tbl_name VARCHAR(32);
  sql VARCHAR(511);
BEGIN
  -- find the index tables: node table and idx table
  select node_tbl, idx_tbl into node_tbl_name, idx_tbl_name from dag_index_metadata where pid = thePid
    and has_subproperty = include_subproperties and exclude_hidden_edge = no_hidden;
  IF node_tbl_name IS NULL THEN
    RAISE NOTICE 'No DAG index found for property hierarchy (pid = %)', thePid;
    RETURN;
  END IF;

  -- find the index for the input node.
  IF level is not null and level > 0 THEN
    sql := 'select n.rid, n.rtid from '||node_tbl_name||' n, (select distinct t1.nid from '
          ||idx_tbl_name||' t1, '||idx_tbl_name||' t2, '|| node_tbl_name
          ||' n1 where (t1.path_cnt - t2.path_cnt) <= $1 and '
          ||' t1.nid != t2.nid and n1.id = t2.nid and n1.rid = $2 and n1.rtid = $3 and'
          ||' to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''')) t where n.id = t.nid';
  ELSE
    sql := 'select n.rid, n.rtid from '||node_tbl_name||' n, (select distinct t1.nid from '
          ||idx_tbl_name||' t1, '||idx_tbl_name||' t2, '|| node_tbl_name
          ||' n1 where t1.nid != t2.nid and n1.id = t2.nid and n1.rid = $2 and n1.rtid = $3 and'
          ||' to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''')) t where n.id = t.nid';
  END IF;
  
  FOR rec IN EXECUTE sql using level, rid, rtid
  LOOP
    RETURN NEXT rec;
  END LOOP;
  RETURN;
EXCEPTION WHEN RAISE_EXCEPTION THEN
  RETURN;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--          get_neighbor_count             -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_neighbor_count(rid integer, rtid integer, thePid integer, hops integer, no_hidden boolean, include_subproperties boolean, dir_incoming boolean)
  RETURNS INTEGER AS $$
/*
  get the number of neighbors up to level = hops for the input node (rid, rtid), if a DAG index is created for the pid. 
  Return -1 if the count is not available.
  @param rid resource id of the node
  @param rtid resource type id of the node
  @param pid property id
  @param hops the max level. 0 means all level. 1 means the immediate neighbors. 2 means the sum of neighbor count at level 1 and 2, and so on.
  @param no_hidden if true, do not include hidden edges.
  @param include_subproperties if true, include those neighbors that are connected via sub-property of pid.
*/
DECLARE
  hasIndexed integer := 0;
  depth integer := hops;
  result integer := -1;
  node_tbl_name text;
  col_name text;
  sql text;
BEGIN
  -- check if a DAG index is created for the pid
  select 1, node_tbl into hasIndexed, node_tbl_name from dag_index_metadata where pid = thePid and has_subproperty = include_subproperties and exclude_hidden_edge = no_hidden;

  -- if no index is found, return -1, and we are done.
  IF hasIndexed is null OR hasIndexed != 1 THEN 
    return -1;
  END IF;

  -- if 5 or more level is asked, return count of all levels.
  IF depth > 5 OR depth < 0 THEN
    depth := 0;
  END IF;

  IF dir_incoming THEN
    col_name := 'desc_count';
  ELSE 
    col_name := 'anc_count';
  END IF;

  sql := 'select '||col_name||'['||(depth+1)||'] from '||node_tbl_name||' where rid = '||rid||' and rtid = '||rtid;
raise notice 'in get count: %', sql;
  EXECUTE sql into result;
  
  IF result is NULL THEN
    result := -1;
  END IF;

  return result;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--          get_neighbor_count             -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_neighbor_count(term_list_str VARCHAR, pname_list_str VARCHAR, kbid INTEGER, prefLabel BOOLEAN,
       hops INTEGER, no_hidden BOOLEAN, include_subproperties BOOLEAN, is_synonym_searched BOOLEAN, dir_incoming BOOLEAN) 
  RETURNS INTEGER AS $$
/*
  Get count of neighborhood of a list of nodes. If a DAG index is not created for the pid, return -1.
  @param term_list_str. The string representation of the term list. The format is '''AAA'', ''BB BB'', ''VV V VVV'''.
         For instance, '''Cerebellum'', ''Purkinje Cell'', ''Cerebellar Cortex'''.
  @param pname_list_str. The string representation of the properties. The format is same as term_list_str. e.g. 
         '''has_part'', ''subClassOf'''.
  @param kbid The knowledge base id.
  @param prefLabel if true, use rdfs:label, if available, as node name. Otherwise, use name column.
  @param hops number of levels to include
  @param no_hidden if true, ignore hidden edges.
  @param include_subproperties if true, include the edges whose edge label is a subPropertyOf pid
  @param is_synonym_searched true to search the term as a synonym. False to search as name.
  @param dir_incoming if true, count the incoming neighbors. Otherwise, count the outgoing neighbors.
*/
  DECLARE
    rec RECORD;
    rec1 RECORD;
    sql text;
    idList INTEGER[][];
    pidList INTEGER[] := null;
    kb_expr text := 'null::int[]';
    tmpCount integer := 0;
    result integer := 0;
    kbid_condition text := '';
    tbl_expr TEXT := '';
    hasNoResult BOOLEAN := true;
  BEGIN
    IF kbid is not null THEN
      kb_expr := 'array['||kbid||']';
    END IF;
    sql := 'select * from get_ids('||quote_literal(term_list_str)||', '||prefLabel||', '||is_synonym_searched
          ||', '||kb_expr||') ';

    FOR rec1 IN EXECUTE sql LOOP
      IF idList is null THEN
        idList := ARRAY[[rec1.rid, rec1.rtid]];
      ELSE
        idList := idList || ARRAY[[rec1.rid, rec1.rtid]];
      END IF;
    END LOOP;

    -- no matching nodes found 
    IF array_upper(idList, 1) is null THEN
      RETURN -1;
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
        RETURN -1;
      END IF;
    END IF;

    -- sum up the count for each set of (rid, rtid, pid).
    FOR i IN ARRAY_LOWER(idList, 1)..ARRAY_UPPER(idList, 1) 
    LOOP
      FOR j IN ARRAY_LOWER(pidList, 1)..ARRAY_UPPER(pidList, 1)
      LOOP
        select get_neighbor_count(idList[i][1], idList[i][2], pidList[j], hops, no_hidden, include_subproperties, dir_incoming) into tmpCount;
        IF tmpCount is null OR tmpCount < 0 THEN
          tmpCount := 0; -- ignore if the count is not available. Maybe different strategy should be used here.
        ELSE
          hasNoResult := false;
        END IF;
        result := result + tmpCount;
      END LOOP;
    END LOOP;

    IF hasNoResult THEN
      return -1;
    END IF;

    return result;
  END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--       extract_ancestor_from_idx         -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION extract_ancestor_from_idx(idx VARCHAR, level INTEGER) 
  RETURNS setof int AS $$
  DECLARE
    nodeArray text[];
	beginIdx integer := 1;
	endIdx integer;
	length integer;
	finalLevel integer;
  BEGIN
    IF level is null THEN
	  finalLevel := 0;
	ELSE
      finalLevel := level;
	END IF;
	
    select string_to_array(idx, ' ') into nodeArray;
	IF nodeArray is null THEN
	  RETURN;
	END IF;
	
	-- empty or only one element in the idx which means the node is a root. 
	length := array_length(nodeArray, 1);
	IF length <= 1 THEN
	  RETURN;
	END IF;
	
	IF length > level + 1 AND level > 0 THEN
	  beginIdx := length - level;
	ELSE
	  beginIdx := 1;
	END IF;
	
	endIdx := length - 1;
	
	FOR i IN beginIdx .. endIdx LOOP
	  RETURN NEXT nodeArray[i];
	END LOOP;
	RETURN;
  END;
$$ LANGUAGE plpgsql;
