/*
drop table if exists syn_graph;

create table syn_graph (id1 integer, id2 integer, pid integer default 1, constraint pk_syn_graph primary key (id1, id2, pid));

create index idx_syn_id2 on syn_graph(id2);

DROP TABLE IF EXISTS DAG_INDEX_METADATA_SYN;

-- the table to store the meta data about DAG indexes. Each row represents a DAG and the index created for it.
CREATE TABLE DAG_INDEX_METADATA_SYN (
  id integer PRIMARY KEY,   -- a unique id for the DAG.
  tree_tbl varchar(32),   -- dewey index table name
  sspi_tbl varchar(32),    -- SSPI table name
  sql text -- the SQL statement passed into compute_dag_index
);
*/

DROP TABLE IF EXISTS GRAPH_TEST_RESULT;

CREATE TABLE GRAPH_TEST_RESULT (
  id1 integer,
  id2 integer,
  bfs_result boolean,
  bfs_second float8,
  dag_result boolean,
  dag_second float8
);

CREATE OR REPLACE FUNCTION is_descendant_syn_bfs(id1 integer, id2 integer, pidList INTEGER[], excludedPidList INTEGER[],
       maxHops INTEGER) RETURNS BOOLEAN AS $$
  
/*
  check if node1 is the descendant of node2 using BFS in a synthetic graph.
*/
  DECLARE
    -- element of neighbor array: id, init_depth (0)
    neighbors integer[][];
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
    neighbors := ARRAY[[id2, 0]];
    pid_condition := compose_pid_condition(pidList, false, false);
    
    curIdx := 1;
    LOOP
      EXIT WHEN curIdx > array_upper(neighbors, 1) or (maxHops != 0 and neighbors[curIdx][2] >= maxHops); 
      sql := 'SELECT id1, id2, pid FROM syn_graph r WHERE r.id2 = '||neighbors[curIdx][1]|| 
          ' and r.id1 != r.id2 '|| pid_condition; 
          
      -- exit when there is no more elements in neighbors list or we have reached the maxHops level.
      FOR rec IN EXECUTE sql LOOP      

        -- found path b/n id1 and id2
        IF rec.id1 = id1 THEN
          return true;
        END IF;
        
        -- check if rec.id1 is already in the neighbors array.
        FOR i IN ARRAY_LOWER(neighbors,1)..ARRAY_UPPER(neighbors,1) LOOP
          hasAdded := neighbors[i][1] = rec.id1;
          EXIT WHEN hasAdded;
        END LOOP;
        
        IF hasAdded = false THEN
          -- add the new neighbors to neighborhood array
          neighbors := neighbors || ARRAY[rec.id1, neighbors[curIdx][2]+1];
        END IF;

      END LOOP;
      curIdx := curIdx + 1;
    END LOOP; 
  RETURN false;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_index_syn() 
  RETURNS INTEGER AS $$
  /* build dag index for synthetic graph */
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
    sql text;
    
    no_root boolean := true;

    prev_nid integer;
    idx1_str varchar(511);
    idx2_str varchar(511);
    i integer;
  BEGIN
    -- create dewey index table name and sspi table name
    select max(id)+1 into tbl_id from DAG_INDEX_METADATA_SYN;
    IF tbl_id IS NULL THEN
      tbl_id := 1;
    END IF;
    
    idx_tbl_name := 'DAG_TREE_SYN_'||tbl_id;
    sspi_tbl_name := 'DAG_SSPI_SYN_'||tbl_id;
--    raise notice 'tbl_id = %, idx_tbl_name = %', tbl_id, idx_tbl_name;
    
    -- id: unique id for node (rid, rtid); idx: dewey index string; dist: distance used by MST; prev: previous node in tree
    perform drop_if_exists(idx_tbl_name);
    perform drop_if_exists(sspi_tbl_name);

    EXECUTE 'create table '||idx_tbl_name||' (id serial PRIMARY KEY, rid integer, rtid integer, '
      ||'idx varchar(511), desc_count integer[10], anc_count integer[10], dist integer, prev integer, known boolean default false)';
    -- id1: id in idx_tbl_name (node1); idx1: idx str of node1, make a copy here for convenience.
    EXECUTE 'create table '||sspi_tbl_name||' (id1 integer, idx1 varchar(511), id2 integer, idx2 varchar(511), 
      rtype char(1) CHECK (rtype in (''p'', ''a'')))';

    EXECUTE 'comment on column '||idx_tbl_name||'.desc_count is ''Store the counts of descendants up to level 9. the first element is count of all descendants. The 2nd int is the count at level 1. The 3rd is the count at level 1 and 2, and so on.''';

    EXECUTE 'comment on column '||idx_tbl_name||'.anc_count is ''Store the counts of ancestors up to level 9. the first element is count of all ancestors. The 2nd int is the count at level 1. The 3rd is the count at level 1 and 2, and so on.''';

    EXECUTE 'comment on table '||sspi_tbl_name||' is ''Store the branching edges (type p) and branch ancestor pair (type a). id2 is the parent/branch ancestor of id1.''';
    
    -- add a row in meta data table
    INSERT INTO DAG_INDEX_METADATA_SYN VALUES (tbl_id, idx_tbl_name, sspi_tbl_name, sql);

    -- create an artificial root to guarantee single root
    EXECUTE 'insert into '|| idx_tbl_name||' values (0, 0, 0, ''0'', null, null, 0, null)';
    
    sql := 'select id1 as rid1, 1 as rtid1, id2 as rid2, 1 as rtid2 from syn_graph';
    
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

CREATE OR REPLACE FUNCTION is_descendant_syn_dag(rid1 INTEGER, rtid1 INTEGER, rid2 INTEGER, rtid2 INTEGER)
  RETURNS BOOLEAN AS $$
/*
  Check if node 1 (rid1, rtid1) is a descendant of node 2 (rid2, rtid2) in the DAG identified by thePid.
*/
DECLARE
  tree_tbl_name VARCHAR(32);
  sspi_tbl_name VARCHAR(32);
  idx1 VARCHAR(511);
  idx2 VARCHAR(511);
BEGIN
  select tree_tbl, sspi_tbl into tree_tbl_name, sspi_tbl_name from dag_index_metadata_syn where id = (select max(id) from dag_index_metadata_syn);

  idx1 := get_dag_idx(rid1, rtid1, tree_tbl_name);
  idx2 := get_dag_idx(rid2, rtid2, tree_tbl_name);
  return is_ancestor(idx1, idx2, tree_tbl_name, sspi_tbl_name);  
EXCEPTION 
  WHEN RAISE_EXCEPTION THEN
   return false;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION graph_test(samplePercentage float) RETURNS VOID AS $$
  -- test performance of DAG-index and BFS graph search.
  -- The graph should be loaded into syn_graph first.
DECLARE
  max_id integer;
--  edge_count integer;
  is_pair_okay boolean;
  id_1 integer;
  id_2 integer;
  selected integer;

  time1 timestamp;
  time2 timestamp;
  bfs_res boolean;
  dag_res boolean;
  bfs_sec float8;
  dag_sec float8;
BEGIN
  delete from graph_test_result;

  -- 1. select 10% pairs of nodes randomly
  ---- First, select max id from the graph
  select case when max(id1) > max(id2) then max(id1) else max(id2) end into max_id from syn_graph;
--  select count(*) into edge_count from syn_graph;

  for i in 1..floor(max_id*max_id*samplePercentage*0.01) loop
    is_pair_okay := false;
    while NOT is_pair_okay loop

      id_1 := round(random()*max_id);
      id_2 := round(random()*max_id);
      if id_1 = id_2 then
        continue;
      end if;
      selected := null;
      select 1 into selected from graph_test_result where id1 = id_1 and id2 = id_2;
      if selected is null then
        insert into graph_test_result values (id_1, id_2, null, null, null, null);
        is_pair_okay := true;
      end if;
    end loop;
  end loop;

  -- 2. For each pair, find their path using BFS and DAG. Record the time in seconds.
  FOR id_1, id_2 IN select id1, id2 from graph_test_result 
  loop
    time1 := to_timestamp(timeofday(), 'Dy Mon DD HH24:MI:SS.US YYYY');
    select is_descendant_syn_bfs(id_1, id_2, array[1], null, 0) into bfs_res;
    time2 := to_timestamp(timeofday(), 'Dy Mon DD HH24:MI:SS.US YYYY');
--raise notice '%, %, bfs_interval = %', id_1, id_2, (time2-time1);
    bfs_sec := extract(epoch from (time2-time1));

    time1 := to_timestamp(timeofday(), 'Dy Mon DD HH24:MI:SS.US YYYY');
    select is_descendant_syn_dag(id_1, 1, id_2, 1) into dag_res;
    time2 := to_timestamp(timeofday(), 'Dy Mon DD HH24:MI:SS.US YYYY');
--raise notice '%, %, dag_interval = %', id_1, id_2, (time2-time1);
    dag_sec := extract(epoch from (time2-time1));

    update graph_test_result set (bfs_result, bfs_second, dag_result, dag_second) = (bfs_res, bfs_sec, dag_res, dag_sec) where id1=id_1 and id2=id_2;
  end loop;
END;
$$ LANGUAGE plpgsql;

