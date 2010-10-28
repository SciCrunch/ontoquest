DROP TABLE IF EXISTS DAG_INDEX_METADATA;

-- the table to store the meta data about DAG indexes. Each row represents a DAG and the index created for it.
CREATE TABLE DAG_INDEX_METADATA (
  id integer PRIMARY KEY,   -- a unique id for the DAG.
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  pid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  has_subproperty boolean DEFAULT false, -- if true, the DAG includes edges whose property is a subproperty of the one identified by pid. for example, part-of hierarchy.
  exclude_hidden_edge boolean DEFAULT true,
  tree_tbl varchar(32),   -- dewey index table name
  sspi_tbl varchar(32),    -- SSPI table name
  sql text -- the SQL statement passed into compute_dag_index
);


DROP TABLE IF EXISTS dag_temp;

-- create a temporary table to store the graph.
CREATE TABLE dag_temp (
  id1 integer, 
  id2 integer,
  dag_id integer   -- used to identify the dag
);
    
CREATE INDEX dag_temp_id1_idx ON dag_temp (dag_id, id1);

CREATE INDEX dag_temp_id2_idx ON dag_temp (dag_id, id2);

