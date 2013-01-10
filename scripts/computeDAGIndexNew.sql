CREATE OR REPLACE FUNCTION create_dag_index(kb_name TEXT, property_name TEXT, subject_root BOOLEAN, collect_stats BOOLEAN) 
  RETURNS SETOF INTEGER AS $$
/* A convinient function to create dag index */
DECLARE
  rec RECORD;
BEGIN

  FOR rec IN select compute_dag_index(null, kbid, pid, true, true, subject_root,collect_stats ) as dag_id from (
    select kb.id as kbid, p.id as pid from kb, property p where kb.name = kb_name and kb.id = p.kbid and p.name = property_name) t
  LOOP
    return next rec.dag_id;
  END LOOP;
  return;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_index(sql TEXT, kbid INTEGER, pid INTEGER, include_subproperties BOOLEAN, 
    no_hidden BOOLEAN, subject_root BOOLEAN, collect_stats BOOLEAN) 
  RETURNS INTEGER AS $$
/*
  Compute maximum spanning tree on the input DAG.
  @param sql: The SQL which will return edges of a general graph. It is expected to have
  the following integer columns: rid1, rtid1, rid2, rtid2, pid. It is not required that the graph is a DAG.
  If the sql is null, the default sql will be used: select rid1, rtid1, rid2, rtid2, pid from graph_edges e where e.kbid = kbid and e.hidden = false  
  @param kbid: The knowledge base id to which this dag index is associated with. It is not required that all nodes/edges in the
  DAG must come from the same kb. When the kb is deleted, this DAG index will be deleted as well.
  @param pid: property id which identifies the DAG edge type, e.g. id of has_part.
  @param include_subproperties: if true, expand pid to include its subproperties. all edges linked by the 
  subproperties are included into the DAG.  
  @param no_hidden if true, hidden edges are excluded from the DAG.
  @param subject_root: if true, the subject in the edge is the parent node, vice versa.
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
    innerSQL := 'select rid1, rtid1, rid2, rtid2, pid from graph_edges e where rtid1 =1 and rtid2 = 1 and e.kbid = '||kbid||hidden_condition;
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

--raise notice 'final sql -- %', finalSQL;

  dag_id := compute_dag_index_private(finalSQL, kbid, pid, include_subproperties, no_hidden, subject_root);
  
  -- pre-compute the count of descendants and ancestors for the purpose of statistics.
  IF collect_stats THEN
    perform compute_dag_neighbor_count(dag_id);
  END IF;

  RETURN dag_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_index_private(sql TEXT, kbid INTEGER, pid INTEGER, include_subproperties BOOLEAN, no_hidden BOOLEAN, subject_root BOOLEAN) 
  RETURNS INTEGER AS $$
/*
  !!! THIS IS AN INTERNAL FUNCTION. USE compute_dag_index(text, integer, integer, boolean) INSTEAD.
  Compute maximum spanning tree on the input DAG using Prim's algorithms.
  An artificial root (rid=0, rtid=0) will be created for the DAG to guarantee 
  single root.
  This is 2nd generation of DAG index. No more SSPI edges.
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
    node_tbl_name varchar(32);
    idx_tbl_name varchar(32);
    curs1 refcursor;
    rid1 integer;
    rtid1 integer;
    rid2 integer;
    rtid2 integer;
    nid1 integer;
    i integer;

    isBranch boolean;

    no_root boolean := true;

    get_root_sql text;
    insert_edge_sql text;
     
  BEGIN
    -- create dewey index table name and sspi table name
    select max(id)+1 into tbl_id from DAG_INDEX_METADATA;
    IF tbl_id IS NULL THEN
      tbl_id := 1;
    END IF;
    
    node_tbl_name := 'DAG_NODE_'||tbl_id;
    idx_tbl_name := 'DAG_IDX_'||tbl_id;

    EXECUTE 'delete from dag_temp';
    
     -- id: unique id for node (rid, rtid); desc_count: statistics about count of descendants at each level. 
     -- anc_count: count of ancestors at each level; is_branch: true if the node has multiple ancestors. 
    EXECUTE 'drop table if exists '||node_tbl_name||' cascade ';
    EXECUTE 'create table '||node_tbl_name||' (id serial PRIMARY KEY, rid integer, rtid integer, '
      ||'desc_count integer[10], anc_count integer[10], is_branch boolean default false)';
    
    -- id: node id in node_tbl, may have multiple entries if the node is a branch node or its descendants.
    -- idx: dewey index; parent_id: direct parent node id; path_cnt: the number of nodes in the index path, including itself 
    EXECUTE 'drop table if exists '||idx_tbl_name||' cascade ';
    EXECUTE 'create table '||idx_tbl_name||' (nid integer, idx varchar(511), parent_id integer, path_cnt integer)';
    
    -- add a row in meta data table
    INSERT INTO DAG_INDEX_METADATA VALUES (tbl_id, kbid, pid, include_subproperties::boolean, no_hidden::boolean, node_tbl_name, idx_tbl_name, subject_root::boolean, sql);

--    -- create an artificial root to guarantee single root
--    EXECUTE 'insert into '|| node_tbl_name||' values (0, 0, 0, ''0'', null, null, 0, null)';

    -- load all nodes into node_tbl_name
    EXECUTE 'INSERT INTO '|| node_tbl_name ||' (rid, rtid) select rid1,rtid1 from ('||sql
	||') a1 union select rid2, rtid2 from (' || sql ||') a2';

    -- create index to faciliate query
    EXECUTE 'create index '||node_tbl_name||'_1 on '||node_tbl_name||' (rid, rtid)';

    raise notice 'All nodes are loaded into node table %', node_tbl_name;
   
    -- load all edges into dag_temp
    -- In dag_temp, id1 is always at higher level (parent), id2 is at the lower level (child).
    IF subject_root THEN
      insert_edge_sql := 'INSERT INTO dag_temp select n2.id as id1, n1.id as id2 '
    	||' from '||node_tbl_name||' n1, '||node_tbl_name||' n2, ('||sql
    	||') e where n1.rid = e.rid1 and n1.rtid = e.rtid1 and n2.rid = e.rid2 and n2.rtid = e.rtid2';
    ELSE
      insert_edge_sql := 'INSERT INTO dag_temp select n1.id as id1, n2.id as id2 '
    	||' from '||node_tbl_name||' n1, '||node_tbl_name||' n2, ('||sql
    	||') e where n1.rid = e.rid1 and n1.rtid = e.rtid1 and n2.rid = e.rid2 and n2.rtid = e.rtid2';
    END IF;

    EXECUTE insert_edge_sql;
    raise notice 'All edges are loaded into temporary table dag_temp';

    get_root_sql := 'select distinct id2 from dag_temp t1 where not exists (
      select * from dag_temp t2 where t1.id2 = t2.id1)';

    EXECUTE 'UPDATE '||node_tbl_name||' n set is_branch = true from (select id1, count(id2) as cnt from dag_temp e group by id1) c where n.id = c.id1 and c.cnt > 1';

    raise notice 'Updated is_branch flag.';
    
    OPEN curs1 FOR EXECUTE get_root_sql;
    <<loop2>>
    LOOP
      FETCH curs1 INTO nid1;
      IF NOT FOUND THEN EXIT loop2; END IF;
      no_root := false;

      -- use DFS to encode root, then its descendants recursively
      EXECUTE 'select is_branch from '||node_tbl_name||' where id = '||nid1 INTO isBranch;
      perform encode_node(nid1, -1, '', isBranch, node_tbl_name, idx_tbl_name);     
    END LOOP loop2;

    raise notice 'All nodes are indexed.';

    EXECUTE 'update '||idx_tbl_name||' set path_cnt = array_length(regexp_split_to_array(idx, E''\\\\s+''), 1)';
    
    -- if there is no root, the graph is not a DAG
    IF no_root THEN
        select count(*) into i from dag_temp;
        IF i = 0 THEN
            raise notice 'No edge is selected from the input graph. Please check if the graph or select statement is valid.';
        ELSE
            raise exception 'No root can be found in the input graph. Please check if the graph is not a directed acyclic graph (DAG)';
        END IF;
    END IF;

    raise notice 'Building full text search index on column idx...';
    -- add text search index on idx_tbl_name.idx
    EXECUTE 'create index '||idx_tbl_name||'_idx ON '||idx_tbl_name||' USING gin(to_tsvector(''english'', idx))';
    
   -- return dag id
    RETURN tbl_id;
  END;
  
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION encode_node(nid integer, parent_id integer, parent_idx varchar, is_branch boolean, node_tbl_name VARCHAR, idx_tbl_name VARCHAR)
  RETURNS VOID AS $$

DECLARE
  curr_idx VARCHAR(511);
  is_known boolean := false;
  rec RECORD;
BEGIN
--raise notice 'nid = %, parent_id = %, parent_idx = %, is_branch = %', nid, parent_id, parent_idx, is_branch;
  IF nid = parent_id THEN -- loop or root. do nothing
    RETURN;
  END IF;

  IF parent_id <= 0 THEN  -- root node
    curr_idx := ''||nid;
  ELSE
    curr_idx := parent_idx||' '||nid;  
  END IF;
--raise notice 'parent_idx -- %, curr_idx -- %', parent_idx, curr_idx;

  EXECUTE 'INSERT INTO '||idx_tbl_name||' values ('||nid||', '''||curr_idx||''', '||parent_id||', -1)';
  
  -- if the current node is a branch node, its descendants may have been traversed through another path. 
  -- In this case, we can bulk insert all descendants' idx by replace their prefix with current node's idx.
  IF is_branch THEN
  
    -- check if the node has been visited through another path.
    EXECUTE 'SELECT true FROM '||idx_tbl_name||' where parent_id = $1' INTO is_known using nid;
    IF is_known is not null AND is_known = true THEN
      EXECUTE 'INSERT INTO '||idx_tbl_name||' select nid, regexp_replace(idx, ''^.+ '||nid||' '', '''
        || curr_idx || ' ''), '||nid||', -1 from '||idx_tbl_name||' where parent_id = '||nid;
    
      return;
    END IF;
  END IF;
  
  -- go to its children, encode them recursively
  FOR rec IN EXECUTE 'select id1, is_branch from dag_temp e, '||node_tbl_name||' n where id2 = '||nid||' and id1 = n.id' 
  LOOP
    perform encode_node(rec.id1, nid, curr_idx, rec.is_branch, node_tbl_name, idx_tbl_name);
  END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION delete_dag_index(dag_id integer) 
  RETURNS VOID AS $$

  DECLARE
    sql text;
  BEGIN
    select 'drop table if exists '||node_tbl||' cascade ' into sql from dag_index_metadata where id = dag_id;
    EXECUTE sql;

    select 'drop table if exists '||idx_tbl||' cascade ' into sql from dag_index_metadata where id = dag_id;
    EXECUTE sql;

    EXECUTE 'delete from dag_index_metadata where id = '||dag_id;
  EXCEPTION
    WHEN DATA_EXCEPTION THEN
      RAISE NOTICE 'Cannot delete the dag_index (id = %). Please make sure the id exists.', dag_id;
  END;

$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compute_dag_neighbor_count(dag_id INTEGER) 
  RETURNS VOID AS $$
/*
    pre-compute and store the count of descendants for the DAG specified by dag_id
*/
  DECLARE
    rec RECORD;
    node_tbl_name TEXT;
    idx_tbl_name TEXT;
    maxHops INTEGER := 2;
    i INTEGER;
    j INTEGER;
    create_tbl_sql TEXT;
    desc_update_sql TEXT;
    anc_update_sql TEXT;
    desc_cnt_clause TEXT;
    anc_cnt_clause TEXT;
  BEGIN
    select node_tbl, idx_tbl into node_tbl_name, idx_tbl_name from dag_index_metadata where id = dag_id;
    IF node_tbl_name IS NULL THEN
      RAISE EXCEPTION 'No entry found for dag id = %', dag_id;
    END IF;

    EXECUTE 'drop table if exists tmp_dag_cnt cascade ';
    
    create_tbl_sql := 'create table tmp_dag_cnt (id integer, anc_total integer, desc_total integer';
    FOR i IN 1..maxHops LOOP
      create_tbl_sql := create_tbl_sql || ', anc_'||i||' integer, desc_'||i||' integer';
    END LOOP;
    create_tbl_sql := create_tbl_sql || ')';
    
    EXECUTE create_tbl_sql;
    
    EXECUTE 'insert into tmp_dag_cnt(id, desc_total) select t2.nid, count(distinct t1.nid)-1 from '||idx_tbl_name||' t1, '||idx_tbl_name||
          ' t2 where to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''') group by t2.nid';
 
    raise notice 'Added total descendant count.';
    
    EXECUTE 'update tmp_dag_cnt c set anc_total = t.cnt from (select t2.nid, count(distinct t1.nid)-1 as cnt from '||
          idx_tbl_name||' t1, '||idx_tbl_name||
          ' t2 where to_tsvector(''english'', t2.idx)@@to_tsquery(''''''''||t1.nid||'''''''') group by t2.nid) t where c.id = t.nid';

    raise notice 'Added total ancestor count.';
 
    FOR i IN 1..maxHops LOOP
      EXECUTE 'update tmp_dag_cnt c set desc_'||i||'= t.cnt from (select t2.nid, count(distinct t1.nid) as cnt from '||idx_tbl_name||' t1, '||idx_tbl_name||
        ' t2 where (t1.path_cnt - t2.path_cnt) <= $1 and t1.nid != t2.nid and to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''') group by t2.nid) t where t.nid = c.id' using i;
    
      EXECUTE 'update tmp_dag_cnt c set desc_'||i||' = 0 where desc_'||i||' is null';
      
      raise notice 'Added descendant count of level %.', i;

      EXECUTE 'update tmp_dag_cnt c set anc_'||i||'= t.cnt from (select t1.nid, count(distinct t2.nid) as cnt from '||idx_tbl_name||' t1, '||idx_tbl_name||
        ' t2 where (t1.path_cnt - t2.path_cnt) <= $1 and t1.nid != t2.nid and to_tsvector(''english'', t1.idx)@@to_tsquery(''''''''||t2.nid||'''''''') group by t1.nid) t where t.nid = c.id' using i;
    
      EXECUTE 'update tmp_dag_cnt c set anc_'||i||' = 0 where anc_'||i||' is null';
      
      raise notice 'Added ancestor count of level %.', i;
    END LOOP;
    
    desc_update_sql := 'update '||node_tbl_name||' n set desc_count = array[c.desc_total';
    anc_update_sql := 'update '||node_tbl_name||' n set anc_count = array[c.anc_total';
    FOR i IN 1..maxHops LOOP
      desc_update_sql := desc_update_sql ||', c.desc_'||i;
      anc_update_sql := anc_update_sql ||', c.anc_'||i;
    END LOOP;
    
    desc_update_sql := desc_update_sql ||'] from tmp_dag_cnt c where n.id = c.id';
    anc_update_sql := anc_update_sql||'] from tmp_dag_cnt c where n.id = c.id';

--raise notice 'desc_update_sql -- %', desc_update_sql;
--raise notice 'anc_update_sql -- %', anc_update_sql;

    EXECUTE desc_update_sql;
    
    EXECUTE anc_update_sql;
     
 --   EXECUTE 'drop table tmp_dag_cnt';
  END;

$$ LANGUAGE plpgsql;

/*
CREATE OR REPLACE FUNCTION compute_dag_neighbor_count2(dag_id INTEGER) 
  RETURNS VOID AS $$
  DECLARE
    rec RECORD;
    node_tbl_name TEXT;
    idx_tbl_name TEXT;
    maxHops INTEGER := 2;
    i INTEGER;
    j INTEGER;
    create_tbl_sql TEXT;
    desc_update_sql TEXT;
    anc_update_sql TEXT;
    desc_cnt_clause TEXT;
    anc_cnt_clause TEXT;
  BEGIN
    select node_tbl, idx_tbl into node_tbl_name, idx_tbl_name from dag_index_metadata where id = dag_id;
    IF node_tbl_name IS NULL THEN
      RAISE EXCEPTION 'No entry found for dag id = %', dag_id;
    END IF;

    EXECUTE 'drop table if exists tmp_dag_cnt cascade ';
    
    create_tbl_sql := 'create table tmp_dag_cnt (id integer, anc_total integer, desc_total integer';
    FOR i IN 1..maxHops LOOP
      create_tbl_sql := create_tbl_sql || ', anc_'||i||' integer, desc_'||i||' integer';
    END LOOP;
    create_tbl_sql := create_tbl_sql || ')';
    
    EXECUTE create_tbl_sql;
    
    EXECUTE 'insert into tmp_dag_cnt(id, desc_total) select t2.nid, count(distinct t1.nid) from '||idx_tbl_name||' t1, '||idx_tbl_name||
          ' t2 where strpos(t1.idx, t2.idx||'' '') = 1 group by t2.nid';
    
    EXECUTE 'update tmp_dag_cnt c set anc_total = (select count(distinct t1.nid) from '||idx_tbl_name||' t1, '||idx_tbl_name||
          ' t2 where strpos(t2.idx, t1.idx||'' '') = 1 and c.id = t2.nid)';
 
  
    FOR i IN 1..maxHops LOOP
      EXECUTE 'update tmp_dag_cnt c set desc_'||i||'= (select count(distinct t1.nid) from '||idx_tbl_name||' t1, '||idx_tbl_name||
        ' t2 where (array_length(regexp_split_to_array(t1.idx, E''\\\\s+''), 1) - '||
        ' array_length(regexp_split_to_array(t2.idx, E''\\\\s+''), 1)) = $1 and strpos(t1.idx, t2.idx||'' '') = 1 '||
        ' and t2.nid = c.id)' using i;
    
      EXECUTE 'update tmp_dag_cnt c set anc_'||i||'= (select count(distinct t2.nid) from '||idx_tbl_name||' t1, '||idx_tbl_name||
        ' t2 where (array_length(regexp_split_to_array(t1.idx, E''\\\\s+''), 1) - '||
        ' array_length(regexp_split_to_array(t2.idx, E''\\\\s+''), 1)) = $1 and strpos(t1.idx, t2.idx||'' '') = 1 '||
        ' and t1.nid = c.id)' using i;
    
    END LOOP;
    
    desc_update_sql := 'update '||node_tbl_name||' n set desc_count = array[c.desc_total';
    anc_update_sql := 'update '||node_tbl_name||' n set anc_count = array[c.anc_total';
    FOR i IN 1..maxHops LOOP
      desc_cnt_clause := 'c.desc_'||i;
      anc_cnt_clause := 'c.anc_'||i;
      FOR j IN 1..i-1 LOOP
        desc_cnt_clause := desc_cnt_clause||'+c.desc_'||j;
        anc_cnt_clause := anc_cnt_clause||'+c.anc_'||j;
      END LOOP;
      desc_update_sql := desc_update_sql ||', '||desc_cnt_clause;
      anc_update_sql := anc_update_sql ||', '||anc_cnt_clause;
    END LOOP;
    
    desc_update_sql := desc_update_sql ||'] from tmp_dag_cnt c where n.id = c.id';
    anc_update_sql := anc_update_sql||'] from tmp_dag_cnt c where n.id = c.id';
    
    EXECUTE desc_update_sql;
    
    EXECUTE anc_update_sql;
    
 --   EXECUTE 'drop table tmp_dag_cnt';
  END;

$$ LANGUAGE plpgsql;
*/
