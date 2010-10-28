CREATE OR REPLACE FUNCTION split_index(idx_str VARCHAR) 
  RETURNS SETOF id_typ AS $$
/*
  Given a dewey index, e.g. 0.3.67.18.324, split it by dilimiter ".", and return the ancestors as a set of integers.
  Note: use id_typ instead of integer as return type so that the column can be referenced in caller sql.
*/
DECLARE
  i integer;
  str_array text[];
  id id_typ;
BEGIN
  IF idx_str IS NULL THEN
    RETURN;
  END IF;

  str_array := string_to_array(idx_str, '.');
  FOR i IN COALESCE(array_lower(str_array,1),0) .. COALESCE(array_upper(str_array,1),-1)
  LOOP 
    id := ROW( to_number(str_array[i], '9999999999') );
    RETURN NEXT id;
  END LOOP;
  RETURN;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_nba_idx(idx_str VARCHAR, sspi_tbl_name VARCHAR) 
  RETURNS VARCHAR AS $$
  /* Nearest Branch Ancestor (NBA) is the nearest ancestor that is a branch node. Branch node is a node with multiple parents. 
     If the input node itself is a branching node, then its NBA is itself.
     @param idx_str: the dewey index of a node
     @param sspi_tbl_name: the SSPI table for search
     @return the index of the NBA node.
   */
DECLARE
  nba_idx VARCHAR(511);
BEGIN
  EXECUTE 'select distinct idx1 from '||sspi_tbl_name||' where position(idx1 in '''||idx_str
    ||''') = 1 and length(idx1) = (select max(length(idx1)) from '||sspi_tbl_name||' where position (idx1 in '''
    ||idx_str||''') = 1)' INTO nba_idx;

  RETURN nba_idx;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION put_node(tbl_name VARCHAR, theRid INTEGER, theRtid INTEGER)
  RETURNS INTEGER AS $$
/*
  Add a node identified by rid, rtid into the table (tbl_name). If the node
  already exists, just return its current node ID.
  @tbl_name the name of the node table.
  @rid resource id from ontoquest db.
  @rtid resource type id.
  @return the node ID. 
*/
  DECLARE
    nid integer := -1;
  BEGIN
    EXECUTE 'select id from '||tbl_name||' where rid = '||theRid||' and rtid = '||theRtid INTO nid;

    IF nid IS NULL THEN
      EXECUTE 'insert into '||tbl_name||' (rid, rtid) values ('||theRid||', '||theRtid||') returning id' INTO nid;
    END IF;

    RETURN nid;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION encode_index(tbl_name VARCHAR, id1 INTEGER, ori_id INTEGER)
  RETURNS VARCHAR AS $$
  /*
   encode index for node id1 in tbl_name. In general, this function is only used by compute_dag_index(...)
   @param tbl_name tree table name
   @id1 id in tbl_name table. 
   @ori_id original id when the function is invoked by external function. It is used to check cycle.
  */

  DECLARE
    prev_idx varchar(511);
    prev_id integer;
    curr_idx varchar(511);
    rid1 integer;
    rtid1 integer;
  BEGIN

    EXECUTE 'select t1.prev, t2.idx, t1.idx from '||tbl_name||' t1, '||tbl_name ||' t2 where t1.id = '||id1||
      ' and t1.prev = t2.id ' INTO prev_id, prev_idx, curr_idx;

    IF prev_id IS NULL THEN
      RAISE EXCEPTION 'Found an unconnected node. Severe error! DAG tree table is %, id is %', tbl_name, id1;
    END IF;

    -- check cycle
    IF prev_id = ori_id THEN
      EXECUTE 'select rid, rtid from '||tbl_name||' where id = '||id1 INTO rid1, rtid1;
      RAISE EXCEPTION 'The graph contains cycle. One of the node in the cycle: (rid=%, rtid=%)',
                    rid1, rtid1;
    END IF;

    IF curr_idx IS NOT NULL THEN
      RETURN curr_idx;
    END IF;

    IF prev_idx IS NULL THEN
      select encode_index(tbl_name, prev_id, ori_id) into prev_idx;
    END IF; 

    curr_idx := prev_idx ||'.'||id1;
    -- update table
    EXECUTE 'update '||tbl_name||' set idx = '''||curr_idx||''' where id = '||id1;
    
    RETURN curr_idx;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION encode_index(tbl_name VARCHAR, id1 INTEGER)
  RETURNS VARCHAR AS $$
  /*
   encode index for node id1 in tbl_name. In general, this function is only used by compute_dag_index(...)
   @param tbl_name tree table name
   @id1 id in tbl_name table. 
  */
  BEGIN
    return encode_index(tbl_name, id1, id1);
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION is_ancestor_of_nba(idx1 VARCHAR, idx2 VARCHAR, tree_tbl_name VARCHAR, sspi_tbl_name VARCHAR)
  RETURNS BOOLEAN AS $$
/*
  Check if node 2 (dag_index = idx2) is an ancestor of node 1 (dag_index = idx1). node 1 is a branching node.
*/
DECLARE
  idx1_anc TEXT;
BEGIN
  IF position(idx2 IN idx1) = 1 THEN
    return true;
  END IF;
  
  -- Get node1's branching ancestor. check if idx2 is the ancestor of the NBA.
  FOR idx1_anc IN EXECUTE 'select idx2 from '||sspi_tbl_name||' where idx1 = '''||idx1||''''
  --||' and rtype = ''p''' (include all rtype)
  LOOP
    IF is_ancestor_of_nba(idx1_anc, idx2, tree_tbl_name, sspi_tbl_name) THEN
      return true;
    END IF;
  END LOOP;
  RETURN FALSE;
END;  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_index_private(sql TEXT, kbid INTEGER, pid INTEGER, include_subproperties BOOLEAN, no_hidden BOOLEAN) 
  RETURNS INTEGER AS $$
/*
  !!! THIS IS AN INTERNAL FUNCTION. USE compute_dag_index(text, integer, integer, boolean) INSTEAD.
  Compute maximum spanning tree on the input DAG using Prim's algorithms.
  An artificial root (rid=0, rtid=0) will be created for the DAG to guarantee 
  single root. 
  @param sql: The SQL which will return edges of DAG. It is expected to have
  the following integer columns: rid1, rtid1, rid2, rtid2. (rid2, rtid2) should be
  the parent of (rid1, rtid1) in DAG heirarchy.
  @param kbid: The knowledge base id to which this dag index is associated with. It is not required that all nodes/edges in the
  DAG must come from the same kb. When the kb is deleted, this DAG index will be deleted as well.
  @param pid: property id which identifies the DAG edge type, e.g. id of has_part.
  @param include_subproperties: if true, expand pid to include its subproperties. all edges linked by the 
  subproperties are included into the DAG.  
  @paran no_hidden: if true, the hidden edges are excluded from the DAG.
*/

  DECLARE
    tbl_id integer := 1;
    idx_tbl_name varchar(32);
    sspi_tbl_name varchar(32);
    curs1 refcursor;
    rid1 integer;
    rtid1 integer;
    rid2 integer;
    rtid2 integer;
    nid1 integer;
    nid2 integer;
    dist1 integer;
    dist2 integer;

    no_root boolean := true;

    prev_nid integer;
    idx1_str varchar(511);
    idx2_str varchar(511);
    i integer;
  BEGIN
    -- create dewey index table name and sspi table name
    select max(id)+1 into tbl_id from DAG_INDEX_METADATA;
    IF tbl_id IS NULL THEN
      tbl_id := 1;
    END IF;
    idx_tbl_name := 'DAG_TREE_'||tbl_id;
    sspi_tbl_name := 'DAG_SSPI_'||tbl_id;
--    raise notice 'tbl_id = %, idx_tbl_name = %', tbl_id, idx_tbl_name;
    
    -- id: unique id for node (rid, rtid); idx: dewey index string; dist: distance used by MST; prev: previous node in tree
    EXECUTE 'drop table if exists '||idx_tbl_name||' cascade ';
    EXECUTE 'drop table if exists '||sspi_tbl_name||' cascade ';

    EXECUTE 'create table '||idx_tbl_name||' (id serial PRIMARY KEY, rid integer, rtid integer, '
      ||'idx varchar(511), desc_count integer[10], anc_count integer[10], dist integer, prev integer, known boolean default false)';
    -- id1: id in idx_tbl_name (node1); idx1: idx str of node1, make a copy here for convenience.
    EXECUTE 'create table '||sspi_tbl_name||' (id1 integer, idx1 varchar(511), id2 integer, idx2 varchar(511), 
      rtype char(1) CHECK (rtype in (''p'', ''a'')))';

    EXECUTE 'comment on column '||idx_tbl_name||'.desc_count is ''Store the counts of descendants up to level 9. the first element is count of all descendants. The 2nd int is the count at level 1. The 3rd is the count at level 1 and 2, and so on.''';

    EXECUTE 'comment on column '||idx_tbl_name||'.anc_count is ''Store the counts of ancestors up to level 9. the first element is count of all ancestors. The 2nd int is the count at level 1. The 3rd is the count at level 1 and 2, and so on.''';

    EXECUTE 'comment on table '||sspi_tbl_name||' is ''Store the branching edges (type p) and branch ancestor pair (type a). id2 is the parent/branch ancestor of id1.''';
    
    -- add a row in meta data table
    INSERT INTO DAG_INDEX_METADATA VALUES (tbl_id, kbid, pid, include_subproperties::boolean, no_hidden::boolean, idx_tbl_name, sspi_tbl_name, sql);

    -- create an artificial root to guarantee single root
    EXECUTE 'insert into '|| idx_tbl_name||' values (0, 0, 0, ''0'', null, null, 0, null)';
    
    -- load graph into dag_temp and idx_tbl_name
    OPEN curs1 FOR EXECUTE sql;
    <<loop1>>
    LOOP
      FETCH curs1 INTO rid1, rtid1, rid2, rtid2;
      if not found then exit loop1; end if;

      select put_node(idx_tbl_name, rid1, rtid1) into nid1;
      select put_node(idx_tbl_name, rid2, rtid2) into nid2;
      insert into dag_temp values (nid1, nid2, tbl_id);
    END LOOP loop1;
    CLOSE curs1;
    raise notice 'All nodes are loaded into index table %', idx_tbl_name;

    -- connect artificial root with actual root nodes. Notice there may be more than one root node.
    OPEN curs1 FOR select distinct id2 from dag_temp t1 where dag_id = tbl_id and not exists (
        select * from dag_temp t2 where t1.id2 = t2.id1 and t1.dag_id = t2.dag_id);
    <<loop2>>
    LOOP
      FETCH curs1 INTO nid1;
      IF NOT FOUND THEN EXIT loop2; END IF;
      no_root := false;

      INSERT INTO dag_temp values (nid1, 0, tbl_id);
    END LOOP loop2;
    raise notice 'Artificial root is created and linked to all top ( actual root) nodes.';

    -- if there is no root, the graph is not a DAG
    IF no_root THEN
        select count(*) into i from dag_temp;
        IF i = 0 THEN
            raise notice 'No edge is selected from the input graph. Please check if the graph or select statement is valid.';
        ELSE
            raise exception 'No root can be found in the input graph. Please check if the graph is not a directed acyclic graph (DAG)';
        END IF;
    END IF;

    -- start MST procedure
    -- initialize dist column (max dist = 2^31), except dist[root] = 0 (default).
    EXECUTE 'update '||idx_tbl_name||' set dist = 2^31-1 where id != 0';

    -- iterate nodes
    <<loop3>>
    LOOP
      EXECUTE 'SELECT id, dist FROM '||idx_tbl_name||' where dist = (select min(dist) from '
              ||idx_tbl_name||' where known = false) and known = false' INTO nid2, dist2;
--raise notice 'nid2 = %, dist2 = %' , nid2, dist2;
      EXIT loop3 WHEN nid2 IS NULL;

      -- update node status
      EXECUTE 'UPDATE '||idx_tbl_name||' SET known = true where id = '||nid2;

      -- iterate current node's neighbors, update their distance.
      <<loop4>>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
      FOR nid1 IN select id1 from dag_temp where id2 = nid2 and dag_id = tbl_id
      LOOP
        EXECUTE 'select dist from '||idx_tbl_name||' where id = '||nid1 INTO dist1;
        IF dist2-1 < dist1 THEN
          EXECUTE 'update '||idx_tbl_name||' set (dist, prev) = ('||(dist2-1)||','||nid2||') where id = '||nid1;
--raise notice 'update (id, dist, prev) = (%, %, %)', nid1, -1, nid2;
        END IF;
      END LOOP loop4;

    END LOOP loop3;
    -- encoding dewey index.
    FOR nid1, idx1_str IN EXECUTE 'select id, idx from '||idx_tbl_name
    LOOP
      IF idx1_str IS NULL THEN
        perform encode_index(idx_tbl_name, nid1);
      END IF;
    END LOOP;

    -- add extra edges into sspi table, the type is p.
    FOR nid1, idx1_str, nid2, idx2_str IN EXECUTE 'select i1.id, i1.idx, i2.id, i2.idx from dag_temp t, '
     ||idx_tbl_name||' i1, '|| idx_tbl_name||' i2 where i1.id = t.id1 and i2.id = t.id2 and t.dag_id = '
     || tbl_id ||' and position(i2.idx in i1.idx) = 0'
    LOOP
/*      IF position(idx1_str in idx2_str) = 1 THEN
        perform delete_dag_index(tbl_id);
        execute 'delete from dag_temp where dag_id = '||tbl_id;
        
        RAISE EXCEPTION 'The input graph contains cycle! The edge to form cycle is from %(idx=%) to %(idx=%)', 
          nid1, idx1_str, nid2, idx2_str;
      END IF;
-- cycle detection is done at encode_index step.
*/      
      EXECUTE 'insert into '||sspi_tbl_name||' values ('||nid1||', '''||idx1_str||''', '||nid2||', '''||idx2_str||
        ''', ''p'')';
    END LOOP;

    -- add branching pairs into sspi table. A branching node is a node that has more than one parent.
    -- say X and Y are branching nodes, X is an ancestor of Y, and there is no other branching nodes between X and Y,
    -- then record the pair in the sspi table. The type is a (for branching ancestor).
    
    perform compute_dag_surrogate_pred(tbl_id, idx_tbl_name, sspi_tbl_name);
    
    -- check whether there are cycles in the graph. Only extra edges may cause cycles.
    -- For every extra edge (nid1->nid2) in sspi table (rtype = 'p'), check if nid2 can lead to nid1.
    FOR idx1_str, idx2_str IN EXECUTE 'select idx1, idx2 from '||sspi_tbl_name||' where rtype = ''p'''
    LOOP
      IF is_ancestor_of_nba(idx2_str, idx1_str, idx_tbl_name, sspi_tbl_name) THEN
        -- clean up first
        perform delete_dag_index(tbl_id);
        execute 'delete from dag_temp where dag_id = '||tbl_id;
        
        RAISE EXCEPTION 'The input graph contains cycle! The edge to form cycle is from % to %', 
          idx1_str, idx2_str;
      END IF;
    END LOOP;

    -- clean up
    -- delete data from dag_temp
    EXECUTE 'ALTER TABLE '||idx_tbl_name||' drop column dist';
    EXECUTE 'ALTER TABLE '||idx_tbl_name||' drop column prev';
    EXECUTE 'ALTER TABLE '||idx_tbl_name||' drop column known';
    
    EXECUTE 'DELETE FROM dag_temp where dag_id = '|| tbl_id;

    -- return dag id
    RETURN tbl_id;
  END;

$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_dag_index(dag_id integer) 
  RETURNS VOID AS $$

  DECLARE
    sql text;
  BEGIN
    select 'drop table if exists '||tree_tbl||' cascade ' into sql from dag_index_metadata where id = dag_id;
    EXECUTE sql;

    select 'drop table if exists '||sspi_tbl||' cascade ' into sql from dag_index_metadata where id = dag_id;
    EXECUTE sql;

    EXECUTE 'delete from dag_index_metadata where id = '||dag_id;
  EXCEPTION
    WHEN DATA_EXCEPTION THEN
      RAISE NOTICE 'Cannot delete the dag_index (id = %). Please make sure the id exists.', dag_id;
  END;

$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_surrogate_pred(dag_id integer, tree_tbl varchar, sspi_tbl varchar) 
  RETURNS VOID AS $$
/*
  compute surrogate predecessors in DAG. Used in computing DAG index.
*/
  DECLARE
    unprocessedNodes integer[]; -- the queue to store all unprocessed branching nodes
    nid1 integer;
    nid2 integer;
    idx1 varchar(511);
    idx2 varchar(511);
    nid integer;
    i integer;
    done boolean;
    tmp integer;
  BEGIN
    -- Initially, push all branching nodes into unprocessedNodes queue.
    -- -- unprocessedNodes := ARRAY(select distinct id1 from sspi_tbl);
    FOR nid1 IN EXECUTE 'select distinct id1 from '||sspi_tbl
    LOOP
--raise notice 'add into array: %', nid1;
      unprocessedNodes := array_append(unprocessedNodes, nid1); 
    END LOOP;

    -- iterate the unprocessedNode to process each branch node
    FOR i IN COALESCE(array_lower(unprocessedNodes,1),0) .. COALESCE(array_upper(unprocessedNodes,1),-1) 
    LOOP
      -- find its nearest branching ancestors
      -- first, iterate its parent
      FOR idx1, nid2, idx2 IN EXECUTE 'select t1.idx as idx1, id2, t2.idx as idx2 from dag_temp d, '||tree_tbl
        ||' t1, '||tree_tbl ||' t2 where d.id1 = '||unprocessedNodes[i]||' and d.id1 = t1.id and d.id2 = t2.id'
      LOOP
--raise notice '(i, id1, idx1, id2, idx2) = (%, %, %, %, %)', i, unprocessedNodes[i], idx1, nid2, idx2;
        done := false;
        -- for every parent, check if it is a branching node
        WHILE NOT done LOOP
          EXECUTE 'SELECT 1 FROM '||sspi_tbl||' where id1 = '||nid2||' and rtype = ''p''' INTO tmp;
          IF tmp IS null OR tmp != 1 THEN
--raise notice 'go up...';
            -- the predecessor node is not a branching node, go up to check its parent.
            EXECUTE 'select id2, t.idx as idx2 from dag_temp d, '||tree_tbl||' t where d.id1 = '||nid2
            ||' and d.id2 = t.id' INTO nid2, idx2;
          ELSE
--raise notice 'found NBA';
            -- found nearest branching ancestor, put into sspi_tbl
            EXECUTE 'insert into '||sspi_tbl||' values ('||unprocessedNodes[i]||', '''||idx1||''', '
              || nid2||', '''||idx2||''', ''a'')';
            done := true;
          END IF;
          IF nid2 = 0 THEN
            -- reach the root.
            -- an option is to insert the (nid1, root) pair into sspi table.
            done := true;
          END IF;
        END LOOP;
      END LOOP;
    END LOOP;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_dag_index(kb_name TEXT, property_name TEXT, collect_stats BOOLEAN) 
  RETURNS SETOF INTEGER AS $$
/* A convinient function to create dag index */
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN select compute_dag_index(null, kbid, pid, true, true) as dag_id from (
    select kb.id as kbid, p.id as pid from kb, property p where kb.name = kb_name and kb.id = p.kbid and p.name = property_name) t
  LOOP
    IF collect_stats THEN
      perform compute_dag_neighbor_count(rec.dag_id, true);
      perform compute_dag_neighbor_count(rec.dag_id, false);
    END IF;
    return next rec.dag_id;
  END LOOP;
  return;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_index(sql TEXT, kbid INTEGER, pid INTEGER, include_subproperties BOOLEAN, no_hidden BOOLEAN) 
  RETURNS INTEGER AS $$
/*
  Compute maximum spanning tree on the input DAG using Prim's algorithms.
  An artificial root (rid=0, rtid=0) will be created for the DAG to guarantee 
  single root. 
  @param sql: The SQL which will return edges of a general graph. It is expected to have
  the following integer columns: rid1, rtid1, rid2, rtid2, pid. It is not required that the graph is a DAG.
  If the sql is null, the default sql will be used: select rid1, rtid1, rid2, rtid2, pid from graph_edges e where e.kbid = kbid and e.hidden = false  
  @param kbid: The knowledge base id to which this dag index is associated with. It is not required that all nodes/edges in the
  DAG must come from the same kb. When the kb is deleted, this DAG index will be deleted as well.
  @param pid: property id which identifies the DAG edge type, e.g. id of has_part.
  @param include_subproperties: if true, expand pid to include its subproperties. all edges linked by the 
  subproperties are included into the DAG.  
  @param no_hidden if true, hidden edges are excluded from the DAG.
*/
DECLARE
  innerSQL text;
  finalSQL text;
  pid_condition text;
  hidden_condition text := '';
  subPropertyOf_id int;
  sub_pid INTEGER;
  dag_id INTEGER;
BEGIN
  IF no_hidden THEN
    hidden_condition := ' and hidden = false';
  END IF;
  
  innerSQL := sql;
  IF innerSQL is null OR length(innerSQL) = 0 THEN
    innerSQL := 'select rid1, rtid1, rid2, rtid2, pid from graph_edges e where e.kbid = '||kbid||hidden_condition;
  END IF;

  IF include_subproperties = false THEN
    pid_condition := ''||pid;
  ELSE
    -- expand pid
    -- first, get id of subPropertyOf
    select id into subPropertyOf_id from property p where name = 'subPropertyOf' and p.kbid = kbid;
    
    pid_condition := ''||pid;
    -- get all descendants of pid
    FOR sub_pid IN select rid1 from get_neighborhood(pid, 15, subPropertyOf_id, false, 0, true, false, false, true)
    LOOP
      pid_condition := pid_condition ||','||sub_pid;
    END LOOP;
  END IF;

  finalSQL := 'select rid1, rtid1, rid2, rtid2 from ('||innerSQL||') g where (rid1 != rid2  or rtid1 != rtid2) and g.pid in ('||pid_condition||')';
  dag_id := compute_dag_index_private(finalSQL, kbid, pid, include_subproperties, no_hidden);
  
  -- pre-compute the count of descendants and ancestors for the purpose of statistics.
 -- perform compute_dag_neighbor_count(dag_id, true);

 -- perform compute_dag_neighbor_count(dag_id, false);

  RETURN dag_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_neighbor_count(dag_id INTEGER, dir_incoming BOOLEAN) 
  RETURNS VOID AS $$
/*
    pre-compute and store the count of descendants for the DAG specified by dag_id
*/
DECLARE
  tree_tbl_name TEXT;
  sspi_tbl_name TEXT;
  maxHops INTEGER := 5;
  countArray INTEGER[5];
  rec RECORD;
  prevCount INTEGER;
  hasGetAll BOOLEAN;
  tmpCount INTEGER;
  colName TEXT;
  rowCount INTEGER := 0;
BEGIN
  select tree_tbl, sspi_tbl into tree_tbl_name, sspi_tbl_name from dag_index_metadata where id = dag_id;
  IF tree_tbl_name IS NULL THEN
    RAISE EXCEPTION 'No entry found for dag id = %', dag_id;
  END IF;

  FOR rec IN EXECUTE 'select idx from '||tree_tbl_name
  LOOP
    rowCount := rowCount + 1;
    prevCount := -1;
    hasGetAll := false;
    FOR i IN 1..maxHops LOOP
      IF hasGetAll THEN
        countArray[i] := prevCount;
        continue;
      END IF;

      IF dir_incoming THEN
        -- the first element is count of all descendants. The 2nd int is the count at level 1. The 3rd is the count at level 1 and 2, and so on. 
        select count(*) into tmpCount from get_descendant_nodes_in_dag(rec.idx, tree_tbl_name, sspi_tbl_name, i-1);
        colName := 'desc_count';
      ELSE
        -- the first element is count of all ancestors. The 2nd int is the count at level 1. The 3rd is the count at level 1 and 2, and so on. 
        select count(*) into tmpCount from get_ancestor_nodes_in_dag(rec.idx, tree_tbl_name, sspi_tbl_name, i-1);
        colName := 'anc_count';
      END IF;
      countArray[i] := tmpCount;
      hasGetAll := (prevCount = countArray[i]);
      prevCount := countArray[i];
    END LOOP;
    EXECUTE 'update '||tree_tbl_name||' set '|| colName||' = ''{'||array_to_string(countArray, ',')||'}''::int[] where idx = '''||rec.idx||'''';
    IF rowCount % 500 = 0 THEN
      raise notice 'rowCount = %; update % count for %', rowCount, colName, rec.idx;
    END IF;
  END LOOP;
END;
$$ LANGUAGE plpgsql;