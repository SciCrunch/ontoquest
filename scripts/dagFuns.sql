-- TODO: get_descendant_edges_in_dag and get_ancestor_edges_in_dag

---------------------------------------------
--              get_dag_idx                -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_dag_idx(rid1 integer, rtid1 integer, tree_tbl_name VARCHAR)
  RETURNS VARCHAR AS $$
 /*
   get dag index for node (rid1, rtid1) from index table (tree_tbl_name)
 */
DECLARE
  idx1 VARCHAR(511);
BEGIN
  EXECUTE 'select idx from '||tree_tbl_name||' where rid = '||rid1||' and rtid = '||rtid1 INTO idx1;
  IF idx1 IS NULL or idx1 = '' THEN
    RAISE EXCEPTION 'No index found for node (rid=%, rtid=%)!', rid1, rtid1;
  END IF;
  
  RETURN idx1;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--              is_ancestor                -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION is_ancestor(idx1 VARCHAR, idx2 VARCHAR, tree_tbl_name VARCHAR, sspi_tbl_name VARCHAR)
  RETURNS BOOLEAN AS $$
/*
  Check if node 2 (idx2) is an ancestor of node 1 (idx1) in the specified DAG.
*/
DECLARE
  idx1_anc VARCHAR(511);
BEGIN
  -- 1. check if idx2 is direct ancestor of idx1 in dewey index. 
  --    add a dot (.) before comparison is to avoid cases like position('0.16' in '0.166')=1
  IF idx2 = idx1 OR position(idx2||'.' IN idx1||'.') = 1 THEN
    RETURN true;  
  END IF;
  
  -- 2. get idx1's nearest branching ancestor (NBA)
  select get_nba_idx(idx1, sspi_tbl_name) into idx1_anc;
  IF idx1_anc IS NULL or idx1_anc = '' THEN
    --RAISE NOTICE 'no NBA for %', idx1;
    RETURN FALSE;  -- no branching ancestor
  END IF;
  
  -- 3. Get NBA's branching ancestor. check if idx2 is the ancestor of the NBA.
  return is_ancestor_of_nba(idx1_anc, idx2, tree_tbl_name, sspi_tbl_name);

END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--              is_ancestor                -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION is_ancestor(rid1 INTEGER, rtid1 INTEGER, rid2 INTEGER, rtid2 INTEGER, thePid INTEGER)
  RETURNS BOOLEAN AS $$
/*
  Check if node 2 (rid2, rtid2) is an ancestor of node 1 (rid1, rtid1) in the DAG identified by thePid.
*/
DECLARE
  tree_tbl_name VARCHAR(32);
  sspi_tbl_name VARCHAR(32);
  idx1 VARCHAR(511);
  idx2 VARCHAR(511);
BEGIN
  select tree_tbl, sspi_tbl into tree_tbl_name, sspi_tbl_name from dag_index_metadata where pid = thePid;

  idx1 := get_dag_idx(rid1, rtid1, tree_tbl_name);
  idx2 := get_dag_idx(rid2, rtid2, tree_tbl_name);
  return is_ancestor(idx1, idx2, tree_tbl_name, sspi_tbl_name);  
EXCEPTION 
  WHEN RAISE_EXCEPTION THEN
   return false;
END;
$$ LANGUAGE plpgsql;


---------------------------------------------
--      get_sspi_ancestor_nodes            -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_sspi_ancestor_nodes(idx VARCHAR, tree_tbl_name VARCHAR, sspi_tbl_name VARCHAR, level INTEGER)
  RETURNS SETOF idx_node AS $$
  /*
    Return the ancestors of the input node (idx) in SSPI table.
    @param idx -- the DAG index of the input node
    @tree_tbl_name -- name of the DAG tree table.
    @sspi_tbl_name -- name of the SSPI table.
    @level -- maximum length allowed between ancestors and the input node. If level is equal to or less than 0, it means no length restriction.
              In other words, get all ancestors.
  */
  DECLARE
    rec RECORD;
    rec2 RECORD;
    init_depth INTEGER;
    level_expr TEXT;
    child_idx idx_node;
  BEGIN
    -- find the parent of the input node (idx) or its ancestors in SSPI. For example, if idx=0.103.553.63, and we have the following rows
    -- in sspi_tbl:
    -- 0.103   0.122.121.3  p
    -- 0.103.553.63    0.586.452  p
    -- get both 0.122.121.3 and 0.586.452, but make sure the level from them to 0.103 is less than or equal to the input level.
    init_depth := array_upper(string_to_array(idx,'.'),1);
    
    level_expr := '0';
    IF level > 0 THEN
      -- level_expr computes the depth from sspi ancestors to the input node (idx). For the examples above, 0.122.121.3 gets 1, 0.586.452 gets 3.
      level_expr := init_depth||' - array_upper(string_to_array(idx1, ''.''),1)+1';
    END IF;
    
    FOR rec IN EXECUTE 'select idx2, '||level_expr||' as length from '||sspi_tbl_name||' where position(idx1||''.'' in '||quote_literal(idx||'.')||
        ')=1 and rtype = ''p'''
    LOOP
--  raise notice 'In get_sspi_ancestor_nodes, idx=%, level=%, idx2=%, length=%', idx, level, rec.idx2, rec.length;
  
      -- ignore those descendants whose length to the input node is bigger than max level.
      CONTINUE WHEN rec.length > level;
      
      -- return the child.
      child_idx.idx := rec.idx2;
      RETURN NEXT child_idx;
      
      -- return the descendants of the child.
      CONTINUE WHEN level = rec.length and level > 0;
      FOR rec2 IN select * from get_ancestor_nodes_in_dag(rec.idx2, tree_tbl_name, sspi_tbl_name, level-rec.length)
      LOOP
        RETURN NEXT rec2;
      END LOOP;
    END LOOP;
    
  RETURN;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--    get_ancestor_nodes_in_dag            -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_ancestor_nodes_in_dag(idx VARCHAR, tree_tbl_name VARCHAR, sspi_tbl_name VARCHAR, maxHops INTEGER)
  RETURNS SETOF idx_node AS $$
DECLARE
  rec RECORD;
  level_condition TEXT = '';
  init_level INTEGER;
  sql text;
BEGIN
  IF maxHops > 0 THEN
    init_level = array_upper(string_to_array(idx,'.'),1);
    level_condition := ' and '||init_level||' - array_upper(string_to_array(t.idx,''.''),1) <= '||maxHops;
  END IF;

  -- get ancestors in tree (direct) and sspi (indirect) table, remove the artificial root.
  sql := 'select t.idx from '||tree_tbl_name||' t, split_index('''||idx
    ||''') i where i.id = t.id '||level_condition||' UNION select * from get_sspi_ancestor_nodes('''||idx||''', '''||tree_tbl_name
    ||''', '''||sspi_tbl_name||''', '||maxHops||') EXCEPT select idx from '||tree_tbl_name||' where id = 0 or idx = '''||idx||'''';
--raise notice 'sql = %', sql;
  FOR rec IN EXECUTE sql
  LOOP
    RETURN NEXT rec;
  END LOOP;
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
  rec2 RECORD;
  tree_tbl_name VARCHAR(32);
  sspi_tbl_name VARCHAR(32);
  curr_idx VARCHAR(511);
BEGIN
  -- find the index tables: tree table and sspi table
  select tree_tbl, sspi_tbl into tree_tbl_name, sspi_tbl_name from dag_index_metadata where pid = thePid
    and has_subproperty = include_subproperties and exclude_hidden_edge = no_hidden;
  IF tree_tbl_name IS NULL THEN
    RAISE NOTICE 'No DAG index found for property hierarchy (pid = %)', thePid;
    RETURN;
  END IF;

  -- find the index for the input node.
  curr_idx := get_dag_idx(rid, rtid, tree_tbl_name);
  FOR rec IN select * from get_ancestor_nodes_in_dag(curr_idx, tree_tbl_name, sspi_tbl_name, level)
  LOOP
    FOR rec2 IN EXECUTE 'select rid, rtid from '||tree_tbl_name||' where idx = '||quote_literal(rec.idx)
    LOOP
     RETURN NEXT rec2;
    END LOOP;
  END LOOP; 
  RETURN;
EXCEPTION WHEN RAISE_EXCEPTION THEN
  RETURN;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--               find_lca                  -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION find_lca(idx1 varchar, idx2 varchar, tree_tbl_name varchar, sspi_tbl_name varchar)
RETURNS varchar AS $$
/*
  Find the Least common ancestor (LCA) between node1 (idx1) and node2 (idx2).
  Note: the following case is unhandled:
        Suppose the DAG is: n1->n2->n3, n1->n4->n5, n6->n3, n6->n7->n8->n5. Both n1 and n6 are common ancestors of n1 and n5.
        The current algorithm does not judge which node has least distance. 
*/
DECLARE
  rec RECORD;
  lca_idx VARCHAR(511) := null;
BEGIN
  FOR rec IN select * from get_ancestor_nodes_in_dag(idx1, tree_tbl_name, sspi_tbl_name, 0)
  LOOP
RAISE NOTICE 'rec.idx = %, idx2 = %', rec.idx, idx2;
    IF is_ancestor(idx2, rec.idx, tree_tbl_name, sspi_tbl_name) THEN
RAISE NOTICE 'rec.idx is ancestor of idx2';
      IF lca_idx IS NULL OR is_ancestor(rec.idx, lca_idx, tree_tbl_name, sspi_tbl_name) THEN
        lca_idx := rec.idx;
RAISE NOTICE 'latest lca_idx = %', lca_idx;
      END IF;
    END IF;
  END LOOP;
  return lca_idx; 
END;
$$ LANGUAGE plpgsql;

--select * from get_lca(null, 2);
/*
CREATE OR REPLACE FUNCTION str_array_sort (text[])
RETURNS text[]
LANGUAGE SQL
AS $$
SELECT ARRAY(
    SELECT $1[s.i] AS "foo"
    FROM
        generate_series(array_lower($1,1), array_upper($1,1)) AS s(i)
    ORDER BY length($1[s.i])
);
$$;


select * from str_array_sort(ARRAY['abaaaaa', 'adbc', 'abc']);
*/

---------------------------------------------
--            sort_by_dag_idx              -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION sort_by_dag_idx (nids INT[][], tree_tbl_name VARCHAR)
RETURNS varchar[]
AS $$
DECLARE
  result varchar[];
BEGIN

  EXECUTE 'SELECT ARRAY(SELECT t.idx FROM generate_series(array_lower('||quote_literal($1::text)||'::int[][],1), array_upper('
        ||quote_literal($1::text)||'::int[][],1)) AS s(i), '
        ||tree_tbl_name||' t WHERE ('||quote_literal($1::text)||'::int[][])[s.i][1] = t.rid and ('||quote_literal($1::text)
        ||'::int[][])[s.i][2] = t.rtid ORDER BY length(t.idx))'
  INTO result;
  RETURN result;
END;
$$ LANGUAGE plpgsql;

--select sort_by_dag_idx('{{8148, 1}, {8149, 1}, {300, 2}, {8163, 1}, {8170, 1}}'::int[][], 'dag_tree_1');

---------------------------------------------
--            get_lca_in_dag               -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_lca_in_dag(IN nodes int[][], IN thePid int, IN include_subproperties BOOLEAN, 
    IN no_hidden BOOLEAN, OUT lcaRid int, OUT lcaRtid int)
  AS $$
/*
  Find the least common ancestor (LCA) of input nodes.
  @param nodes: an array of int[2](rid, rtid).
  @param thePid: the edge id
  @param include_subproperties: if true, the DAG should be created with subproperties of thePid.
  @param no_hidden: if true, exlcude hidden edges.
  @param lcaRid: rid of the LCA node.
  @param lcaRtid: rtid of the LCA node.
*/
DECLARE
  rec RECORD;
  tree_tbl_name VARCHAR(32);
  sspi_tbl_name VARCHAR(32);
  idx_array VARCHAR[];
  i integer;
  lca_idx VARCHAR(511);
BEGIN
  lcaRid := 1;
  lcaRtid := null;
  -- get index table name
  select tree_tbl, sspi_tbl into tree_tbl_name, sspi_tbl_name from dag_index_metadata where pid = thePid
    and has_subproperty = include_subproperties and exclude_hidden_edge = no_hidden;
  
  -- sort the nodes by shortest index length.
  select sort_by_dag_idx(nodes, tree_tbl_name) into idx_array;

  RAISE NOTICE 'idx_array = %', idx_array;

  IF idx_array IS NULL or idx_array[array_lower(idx_array,1)] is null THEN
    RAISE EXCEPTION 'No DAG index found for the input nodes. Please build index first!';
  END IF;

  -- find the LCA of the first two elements as La, then find the LCA of La and the third element (Lb), and so on.
  lca_idx := idx_array[array_lower(idx_array,1)];  
  FOR i IN COALESCE(array_lower(idx_array,1),0)+1 .. COALESCE(array_upper(idx_array,1),-1)
  LOOP
RAISE NOTICE 'lca_idx = %', lca_idx;
    lca_idx := find_lca(lca_idx, idx_array[i], tree_tbl_name, sspi_tbl_name);
    IF lca_idx IS NULL THEN
      RETURN; -- no LCA found
    END IF;
  END LOOP;
  
RAISE NOTICE 'lca_idx = %', lca_idx;
  EXECUTE 'select rid, rtid from '||tree_tbl_name||' where idx = '''||lca_idx||'''' INTO lcaRid, lcaRtid; 
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--            get_dag_path      NOTDONE           -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_dag_path(idx1 VARCHAR, idx2 VARCHAR, tree_tbl_name VARCHAR, sspi_tbl_name VARCHAR)
  RETURNS SETOF edge2 AS $$
/*
  Check if node 2 (rid2, rtid2) is an ancestor of node 1 (rid1, rtid1) in the DAG identified by thePid.
*/
DECLARE
  idx_a VARCHAR(511);
  idx_d VARCHAR(511);
BEGIN
  IF is_ancestor(idx1, idx2, tree_tbl_name, sspi_tbl_name) THEN
    idx_a := idx2;
    idx_d := idx1;
  ELSEIF is_ancestor(idx2, idx1, tree_tbl_name, sspi_tbl_name) THEN
    idx_a := idx1;
    idx_d := idx2;
  ELSE
    RETURN;
  END IF;
  RETURN;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--    get_dag_path NOT DONE            -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_dag_path(rid1 int, rtid1 int, rid2 int, rtid2 int, thePid int, include_subproperties BOOLEAN, no_hidden BOOLEAN)
  RETURNS SETOF edge2 AS $$
DECLARE
  idx1 VARCHAR(511);
  idx2 VARCHAR(511);
  tree_tbl_name VARCHAR(32);
  sspi_tbl_name VARCHAR(32);
  rec RECORD;
BEGIN
  -- get index table name
  select tree_tbl, sspi_tbl into tree_tbl_name, sspi_tbl_name from dag_index_metadata where pid = thePid;
  IF tree_tbl_name IS NULL THEN
    RAISE NOTICE 'No DAG index found for property hierarchy (pid = %)', thePid;
    RETURN;
  END IF;

  -- find the index for the input nodes.
  idx1 := get_dag_idx(rid1, rtid1, tree_tbl_name);
  idx2 := get_dag_idx(rid2, rtid2, tree_tbl_name);
  
  FOR rec IN select * from get_dag_path(idx1, idx2, tree_tbl_name, sspi_tbl_name)
  LOOP
    RETURN NEXT rec;
  END LOOP; 
EXCEPTION WHEN RAISE_EXCEPTION THEN
  RETURN;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--    get_sspi_descendant_nodes            -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_sspi_descendant_nodes(idx VARCHAR, tree_tbl_name VARCHAR, sspi_tbl_name VARCHAR, level INTEGER)
  RETURNS SETOF idx_node AS $$
  /*
    Return the descendants of the input node (idx) in SSPI table.
    @param idx -- the DAG index of the input node
    @tree_tbl_name -- name of the DAG tree table.
    @sspi_tbl_name -- name of the SSPI table.
    @level -- maximum length allowed between descendants and the input node. If level is equal to or less than 0, it means no length restriction.
              In other words, get all descendants.
  */
DECLARE
  rec RECORD;
  rec2 RECORD;
  init_depth INTEGER;
  level_expr TEXT;
  child_idx idx_node;
BEGIN
  -- find the child of the input node (idx) or its descendant in SSPI. For example, if idx=0.103, and we have the following rows
  -- in sspi_tbl:
  -- 0.122.121.3   0.103  p
  -- 0.586.452     0.103.553.63  p
  -- get both 0.122.121.3 and 0.586.452, but make sure the level from them to 0.103 is less than or equal to the input level.
  init_depth := array_upper(string_to_array(idx,'.'),1);
  
  level_expr := '0';
  IF level > 0 THEN
    -- level_expr computes the depth from sspi descendant to the input node (idx). For the examples above, 0.122.121.3 gets 1, 0.586.452 gets 3.
    level_expr := 'array_upper(string_to_array(idx2, ''.''),1)-'||init_depth||'+1';
  END IF;
  
  FOR rec IN EXECUTE 'select idx1, '||level_expr||' as length from '||sspi_tbl_name||' where position('||quote_literal(idx||'.')||
      ' in idx2||''.'')=1 and rtype = ''p'''
  LOOP
--raise notice 'In get_sspi_descendant_nodes, idx=%, level=%, idx1=%, length=%', idx, level, rec.idx1, rec.length;

    -- ignore those descendants whose length to the input node is bigger than max level.
    CONTINUE WHEN rec.length > level;
    
    -- return the child.
    child_idx.idx := rec.idx1;
    RETURN NEXT child_idx;
    
    -- return the descendants of the child.
    CONTINUE WHEN level = rec.length and level > 0;
    FOR rec2 IN select * from get_descendant_nodes_in_dag(rec.idx1, tree_tbl_name, sspi_tbl_name, level-rec.length)
    LOOP
      RETURN NEXT rec2;
    END LOOP;
  END LOOP;
  
  RETURN;
END;
$$ LANGUAGE plpgsql;

---------------------------------------------
--    get_descendant_nodes_in_dag          -- 
---------------------------------------------
CREATE OR REPLACE FUNCTION get_descendant_nodes_in_dag(idx VARCHAR, tree_tbl_name VARCHAR, sspi_tbl_name VARCHAR, maxHops INTEGER)
  RETURNS SETOF idx_node AS $$
DECLARE
  rec RECORD;
  level_condition TEXT = '';
  init_level INTEGER;
  sql text;
BEGIN
  IF maxHops > 0 THEN
    init_level = array_upper(string_to_array(idx,'.'),1);
    level_condition := ' and array_upper(string_to_array(t.idx,''.''),1) - '||init_level||' <= '||maxHops;
  END IF;
  
  -- get descendants in tree (direct) and sspi (indirect) table.
  sql := 'select t.idx from '||tree_tbl_name||' t where position('||quote_literal(idx||'.')||' in t.idx)=1 '
    ||level_condition||' UNION select * from get_sspi_descendant_nodes('''||idx||''', '''||tree_tbl_name
    ||''', '''||sspi_tbl_name||''', '||maxHops||')';
--raise notice 'get_descendant_nodes_in_dag SQL: %', sql;

  FOR rec IN EXECUTE sql 
  LOOP
    RETURN NEXT rec;
  END LOOP;
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
  rec2 RECORD;
  tree_tbl_name VARCHAR(32);
  sspi_tbl_name VARCHAR(32);
  curr_idx VARCHAR(511);
BEGIN
  -- find the index tables: tree table and sspi table
  select tree_tbl, sspi_tbl into tree_tbl_name, sspi_tbl_name from dag_index_metadata where pid = thePid and has_subproperty = include_subproperties and exclude_hidden_edge = no_hidden;
  IF tree_tbl_name IS NULL THEN
--    RAISE NOTICE 'No DAG index found for property hierarchy (pid = %, include_subproperties = %)', thePid, include_subproperties;
    RETURN;
  END IF;

  -- find the index for the input node.
--raise notice 'rid = %, rtid = %, tree_tbl_name = %', rid, rtid, tree_tbl_name;

  curr_idx := get_dag_idx(rid, rtid, tree_tbl_name);
  FOR rec IN select * from get_descendant_nodes_in_dag(curr_idx, tree_tbl_name, sspi_tbl_name, level)
  LOOP
    FOR rec2 IN EXECUTE 'select rid, rtid from '||tree_tbl_name||' where idx = '||quote_literal(rec.idx)
    LOOP
     RETURN NEXT rec2;
    END LOOP;
  END LOOP; 
EXCEPTION WHEN RAISE_EXCEPTION THEN
  RETURN;
END;
$$ LANGUAGE plpgsql;

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
  tree_tbl_name text;
  col_name text;
  sql text;
BEGIN
  -- check if a DAG index is created for the pid
  select 1, tree_tbl into hasIndexed, tree_tbl_name from dag_index_metadata where pid = thePid and has_subproperty = include_subproperties and exclude_hidden_edge = no_hidden;

  -- if no index is found, return -1, and we are done.
  IF hasIndexed is null OR hasIndexed != 1 THEN 
    return -1;
  END IF;

  -- if 10 or more level is asked, return count of all levels.
  IF depth > 7 OR depth < 0 THEN
    depth := 0;
  END IF;

  IF dir_incoming THEN
    col_name := 'desc_count';
  ELSE 
    col_name := 'anc_count';
  END IF;

  sql := 'select '||col_name||'['||(depth+1)||'] from '||tree_tbl_name||' where rid = '||rid||' and rtid = '||rtid;
raise notice 'in get count: %', sql;
  EXECUTE sql into result;
  
  IF result is NULL THEN
    result := -1;
  END IF;

  return result;
END;
$$ LANGUAGE plpgsql;

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