/*drop table if exists graph_edges_all cascade;

drop table if exists graph_nodes_all cascade;
*/
/*
 * The node table in graph view. Each node is identified by (rid, rtid) pair.
 * @col rid -- resource id.
 * @col rtid -- resource type id.
 * @col name -- name stored in ontology. 
 * @col label -- the text to be displayed as node label. Some ontologies use rdfs:label for display, name as string identifier.
 * @col kbid -- knowledge base id
 * @col anonymous -- if true, the node is an anonymous class. In general, 
 * anonymous is hidden from user. 
 * @col is_obsolete -- if true, the node is deprecated.
 */
/*
create table graph_nodes_all (
  rid integer NOT NULL,
  rtid integer NOT NULL,
  name text,
  label text,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  anonymous boolean default false,
  is_obsolete boolean default false,
  uri varchar(512) default null,
  constraint node_all PRIMARY KEY (rtid, rid)
) with oids;

CREATE INDEX node_kbid ON graph_nodes_all(kbid);

CREATE INDEX node_name ON graph_nodes_all(name);

CREATE INDEX node_name2 ON graph_nodes_all(lower(name), is_obsolete, kbid);

CREATE INDEX node_label ON graph_nodes_all(label);

CREATE INDEX node_label2 ON graph_nodes_all(lower(label), is_obsolete, kbid);

create view graph_nodes as select * from graph_nodes_all where is_obsolete = false;
*/
/*
 * The edge table in graph view. It contains the edge list in the graph.
 * @col rid1 -- resource id of node 1 (source node)
 * @col rtid1 -- resource type id of node 1 (source node)
 * @col pid -- property id which identifies the edge label
 * @col rid2 -- resource id of node 2 (target node)
 * @col rtid2 -- resource type id of node 2 (target node)
 * @col kbid -- knowledge base id
 * @col derived -- if true, the edge is derived, e.g. from a restriction structure.
 * @col hidden -- if true, the edge should be hidden in normal case. Usually, if an edge involves an anonymous 
 * class, it should be hidden, unless the anonymous class must be shown in order to connecting with other nodes.
 * @col restriction_type -- if not null, there exists some kind of restriction on the edge.
 * a means forall (AllValuesFrom), e means exists (SomeValuesFrom), n means minCardinality,
 * c means cardinality, x means maxCardinality.
 * @col restriction_stmt -- the restriction statement (text to be shown along with the edge label).
 * @col is_obsolete -- if true, the edge is deprecated.
 */
/*
create table graph_edges_all (
  rid1 integer NOT NULL,
  rtid1 integer NOT NULL,
  pid integer NOT NULL REFERENCES property (id) ON DELETE CASCADE,
  rid2 integer NOT NULL,
  rtid2 integer NOT NULL,
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  derived boolean default false,
  hidden boolean default false,
  restriction_type char(1),
  restriction_stmt varchar(255),
  is_obsolete boolean default false,
  constraint rtype_chk CHECK (restriction_type in ('a', 'v', 'b', 'm', 'n', null))
  ,foreign key (rid1, rtid1) references graph_nodes_all(rid, rtid) ON DELETE CASCADE
  ,foreign key (rid2, rtid2) references graph_nodes_all(rid, rtid) ON DELETE CASCADE
) with oids;

-- create index on graph_edges
CREATE INDEX edge_id1 ON graph_edges_all (rtid1, rid1, is_obsolete, kbid);

CREATE INDEX edge_id2 ON graph_edges_all (rtid2, rid2, is_obsolete, kbid);

CREATE INDEX edge_pid ON graph_edges_all (pid);

CREATE INDEX edge_kbid ON graph_edges_all (kbid);

create view graph_edges as select * from graph_edges_all where is_obsolete = false;
*/

CREATE OR REPLACE FUNCTION set_label(theRid INTEGER, theRtid INTEGER, isRecursive BOOLEAN) RETURNS TEXT AS $$
/*
  Set the label identified by (theRid, theRtid) in graph_nodes_all table.
  @param theRid -- the rid of the node
  @param theRtid -- the rtid of the node
  @param isRecursive -- In some cases, the label of an anonymous class, e.g. union class, cannot be determined before
                        the labels of its members are set. In such cases, if isRecursive is false, exit the function without
                        setting the label and return false. Is isRecursive is true, set its memebers' label recursively and
                        then set the label of the input node.
  
  Steps: 
  1) First, check if the resource has a rdfs:label property. If so, use the value of rdfs:label as the resource's label. Return true.
  2) Otherwise, check if the resource has a name. If so, use the name as label. Return true.
  3) For anonymous classes, check if the resources used in class definition have labels or not. If yes, set the label depending on class type.
  Return true. If not, decide if we need to set the labels of member classes, depending on the isRecursive flag.
*/
  DECLARE
    rec RECORD;
    theRid2 INTEGER;
    theRtid2 INTEGER;
    thePid INTEGER;
    intVal INTEGER;
    theLabel TEXT;
    theLabel2 TEXT;
  BEGIN

    -- First, check if the resource has a rdfs:label property. If so, use the value of rdfs:label as the resource's label. Return true.
    FOR rec IN select n.name from graph_nodes_all n, graph_edges_all r, property p
      where r.rid1 = theRid and r.rtid1 = theRtid and r.pid = p.id and p.name = 'label' and p.is_system = true
      and r.rid2 = n.rid and r.rtid2 = n.rtid
    LOOP
      update graph_nodes_all set label = rec.name where rid = theRid and rtid = theRtid;
      return rec.name;
    END LOOP;

    -- Otherwise, check if the resource has a name. If so, use the name as label. Return true.
    IF theRtid IN (1, 12, 13, 14, 15) THEN
      update graph_nodes_all set label = name where rid = theRid and rtid = theRtid and label is null RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 4 THEN
      select get_label(propertyid, 15), cardinality into theLabel, intVal from cardinalityclass where id = theRid;
      IF position(' ' IN theLabel) > 0 THEN
        theLabel := '"'||theLabel||'"';
      END IF;

      update graph_nodes_all set label = 'something with '||theLabel||' = '||intVal where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 5 THEN
      select get_label(propertyid, 15), mincardinality into theLabel, intVal from mincardinalityclass where id = theRid;
      IF position(' ' IN theLabel) > 0 THEN
        theLabel := '"'||theLabel||'"';
      END IF;

      update graph_nodes_all set label = 'something with '||theLabel||' >= '||intVal where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 6 THEN
      select get_label(propertyid, 15), maxcardinality into theLabel, intVal from maxcardinalityclass where id = theRid;
      IF position(' ' IN theLabel) > 0 THEN
        theLabel := '"'||theLabel||'"';
      END IF;

      update graph_nodes_all set label = 'something with '||theLabel||' <= '||intVal where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 10 THEN
      select get_label(propertyid, 15), get_label(valueid, rtid) into theLabel, theLabel2 from hasvalue where id = theRid;
      IF position(' ' IN theLabel) > 0 THEN
        theLabel := '"'||theLabel||'"';
      END IF;

      IF position(' ' IN theLabel2) > 0 THEN
        theLabel2 := '"'||theLabel2||'"';
      END IF;

      update graph_nodes_all set label = 'something with value of '||theLabel||' to be '||theLabel2 where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 7 THEN
      select get_label(complementclassid, rtid) into theLabel2 from complementclass where id = theRid;
  
      IF theLabel2 is null OR length(theLabel2) <= 0 THEN
        IF isRecursive THEN
          select set_label(complementclassid, rtid, isRecursive) into theLabel2 from complementclass where id = theRid;
        ELSE
          return NULL;
        END IF;
      END IF;

      update graph_nodes_all set label = 'not ('||theLabel2||')' where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 8 THEN
      theLabel2 := 'intersection of (';
      FOR rec IN select get_label(classid, rtid) as label from intersectionclass where id = theRid
      LOOP
        IF rec.label is null OR length(rec.label)  <= 0 THEN
          IF isRecursive THEN
            select set_label(classid, rtid, isRecursive) into theLabel from intersectionclass where id = theRid;
          ELSE
            return NULL;
          END IF;
        ELSE
          theLabel := rec.label;
        END IF;

          IF position(',' in theLabel) > 0 THEN
            theLabel2 := theLabel2||'"'||theLabel||'", ';
          ELSE
            theLabel2 := theLabel2||theLabel||', ';
          END IF;
      END LOOP;
      theLabel2 := rtrim(theLabel2, ', ') ||')';
      update graph_nodes_all set label = theLabel2 where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 9 THEN
      theLabel2 := 'union of (';
      FOR rec IN select get_label(classid, rtid) as label from unionclass where id = theRid
      LOOP
        IF rec.label is null OR length(rec.label)  <= 0 THEN
          IF isRecursive THEN
            select set_label(classid, rtid, isRecursive) into theLabel from unionclass where id = theRid;
          ELSE
            return NULL;
          END IF;
        ELSE
          theLabel := rec.label;
        END IF;

          IF position(',' in theLabel) > 0 THEN
            theLabel2 := theLabel2||'"'||theLabel||'", ';
          ELSE
            theLabel2 := theLabel2||theLabel||', ';
          END IF;
      END LOOP;
      theLabel2 := rtrim(theLabel2, ', ') ||')';
      update graph_nodes_all set label = theLabel2 where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 11 THEN
      theLabel2 := 'one of (';
      FOR rec IN select get_label(valueid, rtid) as label from oneof where id = theRid
      LOOP
        IF rec.label is null OR length(rec.label) <= 0 THEN
          IF isRecursive THEN
            select set_label(valueid, rtid, isRecursive) into theLabel from oneof where id = theRid;
          ELSE
            return NULL;
          END IF;
        ELSE
          theLabel := rec.label;
        END IF;

        IF position(',' in theLabel) > 0 THEN
          theLabel2 := theLabel2||'"'||theLabel||'", ';
        ELSE
          theLabel2 := theLabel2||theLabel||', ';
        END IF;
      END LOOP;
      theLabel2 := rtrim(theLabel2, ', ') ||')';
      update graph_nodes_all set label = theLabel2 where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 16 THEN
      update graph_nodes_all set label = substring(name, 5) where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 17 THEN
      update graph_nodes_all set label = (select distinct browsertext from alldifferentindividual where id = theRid)
        where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 2 THEN
      select get_label(rangeclassid, rtid), get_label(propertyid, 15) into theLabel2, theLabel from allvaluesfromclass where id = theRid;
      IF theLabel2 is null OR length(theLabel2) <= 0 THEN
        IF isRecursive THEN
            select set_label(rangeclassid, rtid, isRecursive) into theLabel2 from allvaluesfromclass where id = theRid;
        ELSE
          return NULL;
        END IF;
      END IF;

      IF position(' ' IN theLabel2) > 0 THEN
        theLabel2 := '"'||theLabel2||'"';
      END IF;

      IF position(' ' IN theLabel) > 0 THEN
        theLabel := '"'||theLabel||'"';
      END IF;

      update graph_nodes_all set label = 'something [forall]'||theLabel||' '||theLabel2 where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    IF theRtid = 3 THEN
      select get_label(rangeclassid, rtid), get_label(propertyid, 15) into theLabel2, theLabel from somevaluesfromclass where id = theRid;
      IF theLabel2 is null OR length(theLabel2) <= 0 THEN
        IF isRecursive THEN
            select set_label(rangeclassid, rtid, isRecursive) into theLabel2 from somevaluesfromclass where id = theRid;
        ELSE
          return NULL;
        END IF;
      END IF;

      IF position(' ' IN theLabel2) > 0 THEN
        theLabel2 := '"'||theLabel2||'"';
      END IF;

      IF position(' ' IN theLabel) > 0 THEN
        theLabel := '"'||theLabel||'"';
      END IF;

      update graph_nodes_all set label = 'something [exists]'||theLabel||' '||theLabel2 where rid = theRid and rtid = theRtid RETURNING label INTO theLabel;
      return theLabel;
    END IF;

    return NULL;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_label(theRid INTEGER, theRtid INTEGER) RETURNS TEXT AS $$
  DECLARE
    theLabel TEXT := null;
  BEGIN
    select label into theLabel from graph_nodes_all where rid = theRid and rtid = theRtid;
    return theLabel;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_labels(theKbid INTEGER, setLabelFlag boolean) RETURNS VOID AS $$
  /*
     set labels for all nodes in graph_nodes_all table for the specified kbid. 
   */
  DECLARE
     /* maxIteration: In many cases, the label of an anonymous class, e.g. union class, cannot be 
        assigned before the labels of its member are set. In this case, we try multiple iterations of assigning
        labels for all nodes before recursively set label for a complex class. The maxIteration determines how
        many iterations to go. */
    maxIteration INTEGER := 5;
    i INTEGER;
  BEGIN
    -- clear old labels
	IF setLabelFlag THEN
		update graph_nodes_all set label = null where kbid = theKbid;
	
		-- iterate several assignment processes
		FOR i IN 1..maxIteration LOOP
		  perform set_label(rid, rtid, false) from graph_nodes_all where label is null and kbid = theKbid;
		END LOOP;

		-- for rest of nodes, use recursive assignment.
		perform set_label(rid, rtid, true) from graph_nodes_all where label is null and kbid = theKbid;
	ELSE
		perform set_label(rid, rtid, false) from graph_nodes_all where label is null and kbid = theKbid;
	END IF;

    -- if there are still unsigned label, use name
    update graph_nodes_all set label = name where label is null and kbid = theKbid;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_obsolete_flag (theKbid INTEGER) RETURNS VOID AS $$
  /*
     set obsolete flag in nodes and edges table for the specified kbid. 
   */
  DECLARE
    obsoleteClassNames TEXT := '''_birnlex_retired_class'', ''ObsoleteClass''';
    rec1 RECORD;
    rec RECORD;
    kbIdArray integer[];
    idList integer[][] := null;
  BEGIN
    kbIdArray := ARRAY[theKbid];

    -- find ID of top-level obsolete classes
    FOR rec1 IN select * from get_ids(obsoleteClassNames, true, true, kbIdArray) LOOP
      IF idList is null THEN
        idList := ARRAY[[rec1.rid, rec1.rtid]];
      ELSE
        idList := idList || ARRAY[[rec1.rid, rec1.rtid]];
      END IF;
      -- update is_obsolete flag of top-level obsolete classes
      update graph_nodes_all set is_obsolete = true where rid = rec1.rid and rtid = rec1.rtid;
    END LOOP;

    -- if no ID is found, return
    IF idList is null THEN
      RETURN;
    END IF;

    -- fetch subclasses of obsolete classes
    FOR rec IN select * from get_neighborhood(idList, '''subClassOf''', null, theKbId, false, 0, false, true, false, true)
    LOOP
      -- update the is_obsolete flag of these subclasses
      update graph_nodes_all set is_obsolete = true where rid = rec.rid1 and rtid = rec.rtid1;
    END LOOP;

    -- update the is_obsolete flag in edge tables
    update graph_edges_all set is_obsolete = true where 
      (rid1, rtid1) in (select rid, rtid from graph_nodes_all where is_obsolete = true) or
      (rid2, rtid2) in (select rid, rtid from graph_nodes_all where is_obsolete = true);
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_inference_edges(theKbid integer) 
  RETURNS VOID AS $$
  DECLARE
    maxIteration INTEGER := 5;
    j INTEGER;
  BEGIN
    -- loop several times to handle nested cases.
	FOR j IN 1..maxIteration LOOP
		-- insert inferred edge from intersection class.
		-- e.g. X -- subclassOf/equivalent -> (intersectionOf(A, B, C)) will generate:
		-- X subclassOf/equivalent A, X subClassOf/equivalent B, X subClassOf/equivalent C
		INSERT INTO graph_edges_all select distinct e.rid1, e.rtid1, e.pid, i.classid, i.rtid, e.kbid, true, false,
		null as restriction_type, null as restriction_stmt
		from graph_edges_all e, intersectionclass i, resourcetype rt
		where e.rid2 = i.id and e.rtid2 = rt.id and rt.rtype = 'x' and e.kbid = theKbid
		and not exists(select * from graph_edges_all e2 where e.rid1 = e2.rid1 and e.rtid1 = e2.rtid1 and e.pid =e2.pid
		and i.classid = e2.rid2 and i.rtid = e2.rtid2);
   raise notice 'Iteration %, inserted inferred intersection classes', j;
		
		-- insert inferred edge from allValuesFrom class.
		-- e.g. Wine -- subclassOf/equivalent/intersectionOf -> (something --:forall:hasMaker-> Winery) will generate:
		-- Wine -- :forall:hasMaker -> Winery
		INSERT INTO graph_edges_all select distinct s.rid1, s.rtid1, t.propertyid, t.rangeclassid, t.rtid, t.kbid, true, false, 'v', 'exists'
        from allvaluesfromclass t, graph_edges s, resourcetype rt, property p
        where s.rid2 = t.id and s.rtid2 = rt.id and rt.rtype = 'v' and s.pid = p.id and p.name in ('equivalentClass', 'subClassOf', 'intersectionOf')
        and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
        and exists(select * from graph_nodes_all n where rangeclassid = n.rid and t.rtid = n.rtid)
        and not exists(select * from graph_edges_all e2 where s.rid1=e2.rid1 and s.rtid1=e2.rtid1 and t.propertyid=e2.pid
        and t.rangeclassid=e2.rid2 and t.rtid=e2.rtid2) and s.kbid = theKbid;
  raise notice 'Iteration %, inserted inferred allValuesFrom classes', j;
		  
		INSERT INTO graph_edges_all select distinct s.rid1, s.rtid1, t.propertyid, t.rangeclassid, t.rtid, t.kbid, true, false, 'v', 'exists'
      from somevaluesfromclass t, graph_edges s, resourcetype rt, property p
      where s.rid2 = t.id and s.rtid2 = rt.id and rt.rtype = 'v' and s.pid = p.id and p.name in ('equivalentClass', 'subClassOf', 'intersectionOf')
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where rangeclassid = n.rid and t.rtid = n.rtid)
      and not exists(select * from graph_edges_all e2 where s.rid1=e2.rid1 and s.rtid1=e2.rtid1 and t.propertyid=e2.pid
          and t.rangeclassid=e2.rid2 and t.rtid=e2.rtid2) and s.kbid = theKbid;		
  raise notice 'Iteration %, inserted inferred someValuesFrom classes', j;
		
	END LOOP;
 
	END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_inference_edges2(theKbid integer)
  RETURNS VOID AS $$

  DECLARE
  BEGIN
	-- for equivalent classes (A equivalentClass B) and all edges involved A (A rel X), add edges B rel X in edge table. Similar for X rel A. Vice versa.
	INSERT INTO graph_edges_all select e1.rid2 as rid1, e1.rtid2 as rtid1, e2.pid, e2.rid2, e2.rtid2, e2.kbid, 
	true, e2.hidden, e2.restriction_type, e2.restriction_stmt, e2.is_obsolete 
	from graph_edges_all e1, property p, property p2, graph_edges_all e2 where e1.rid1 = e2.rid1 and e1.rtid1 = e2.rtid1 
	and e1.pid = p.id and p.name = 'equivalentClass' and e2.pid != p.id and e2.pid = p2.id and p2.is_annotation = false
	and not exists(select * from graph_edges_all e3 where e1.rid2=e3.rid1 and e1.rtid2=e3.rtid1 and e2.pid=e3.pid) 
	and e2.kbid = theKbid;
  raise notice 'Inserted inferred equivalent relationships type 1';

	INSERT INTO graph_edges_all select e1.rid1 as rid1, e1.rtid1 as rtid1, e2.pid, e2.rid2, e2.rtid2, e2.kbid, 
	true, e2.hidden, e2.restriction_type, e2.restriction_stmt, e2.is_obsolete 
	from graph_edges_all e1, property p, property p2, graph_edges_all e2 where e1.rid2 = e2.rid1 and e1.rtid2 = e2.rtid1 
	and e1.pid = p.id and p.name = 'equivalentClass' and e2.pid != p.id and e2.pid = p2.id and p2.is_annotation = false
	and not exists(select * from graph_edges_all e3 where e1.rid1=e3.rid1 and e1.rtid1=e3.rtid1 and e2.pid=e3.pid)
	and e2.kbid = theKbid;
  raise notice 'Inserted inferred equivalent relationships type 2';

	INSERT INTO graph_edges_all select e2.rid1 as rid1, e2.rtid1 as rtid1, e2.pid, e1.rid2, e1.rtid2, e2.kbid, 
	true, e2.hidden, e2.restriction_type, e2.restriction_stmt, e2.is_obsolete 
	from graph_edges_all e1, property p, property p2, graph_edges_all e2 where e1.rid1 = e2.rid2 and e1.rtid1 = e2.rtid2 
	and e1.pid = p.id and p.name = 'equivalentClass' and e2.pid != p.id and e2.pid = p2.id and p2.is_annotation = false 
	and not exists(select * from graph_edges_all e3 where e2.rid1=e3.rid1 and e2.rtid1=e3.rtid1 and e2.pid=e3.pid)
	and e2.kbid = theKbid;
  raise notice 'Inserted inferred equivalent relationships type 3';

	INSERT INTO graph_edges_all select e2.rid1 as rid1, e2.rtid1 as rtid1, e2.pid, e1.rid1, e1.rtid1, e2.kbid, 
	true, e2.hidden, e2.restriction_type, e2.restriction_stmt, e2.is_obsolete 
	from graph_edges_all e1, property p, property p2, graph_edges_all e2 where e1.rid2 = e2.rid2 and e1.rtid2 = e2.rtid2 
	and e1.pid = p.id and p.name = 'equivalentClass' and e2.pid != p.id and e2.pid = p2.id and p2.is_annotation = false 
	and not exists(select * from graph_edges_all e3 where e2.rid1=e3.rid1 and e2.rtid1=e3.rtid1 and e2.pid=e3.pid)
	and e2.kbid = theKbid;
  raise notice 'Inserted inferred equivalent relationships type 4';

	-- an extra check to remove those subClassOf edges which are not properly defined. 
	-- e.g. if someone improperly declared A subClassOf B, A equivalentClass B, we will have cycles after adding
	-- inference edges. In this case, we shall remove inference edges.
	DELETE FROM graph_edges_all e1 where pid in (select id from property where name = 'subClassOf' and kbid=theKbid) 
	and exists (select * from graph_edges e2 where e1.pid = e2.pid and
	e1.rid1 = e2.rid2 and e1.rtid1 = e2.rtid2 and e1.rid2 = e2.rid1 and e1.rtid2 = e2.rtid2) and derived = true;
  raise notice 'Delete improper subClassOf relationships';

	-- for equivalent properties (A equivalentProperty B) and all edges involved A (X A Y), add edges X B Y in edge table. Similar for Y A X.
	INSERT INTO graph_edges_all select e1.rid2 as rid1, e1.rtid2 as rtid1, e2.pid, e2.rid2, e2.rtid2, e2.kbid, 
	true, e2.hidden, e2.restriction_type, e2.restriction_stmt, e2.is_obsolete 
	from graph_edges_all e1, property p, property p2, graph_edges_all e2 where e1.rid1 = e2.rid1 and e1.rtid1 = e2.rtid1 
	and e1.pid = p.id and p.name = 'equivalentClass' and e2.pid != p.id and e2.pid = p2.id and p2.is_annotation = false
	and not exists(select * from graph_edges_all e3 where e1.rid2=e3.rid1 and e1.rtid2=e3.rtid1 and e2.pid=e3.pid) 
	and e2.kbid = theKbid;
  raise notice 'Inserted inferred equivalent properties type 1';

	-- Handle inverseOf properties, e.g. p1 inverseOf p2. For all edges involving p1, e.g. X p1 Y, add Y p2 X into the graph
	INSERT INTO graph_edges_all select e.rid2, e.rtid2, i.propertyid2, e.rid1, e.rtid1, e.kbid, true, e.hidden, e.restriction_type, 
	e.restriction_stmt, e.is_obsolete from graph_edges_all e, inversepropertyof i where 
	e.pid = i.propertyid1 and i.propertyid1 != i.propertyid2 and not exists (select * from graph_edges e2 where e.rid2 = e2.rid1 
	and e.rtid2 = e2.rtid1 and i.propertyid2 = e2.pid and e.rid1 = e2.rid2 and e.rtid1 = e2.rtid2) and i.kbid = theKbid;
  raise notice 'Inserted inferred inverseOf relationship type 1';

	INSERT INTO graph_edges_all select e.rid2, e.rtid2, i.propertyid1, e.rid1, e.rtid1, e.kbid, true, e.hidden, e.restriction_type, 
	e.restriction_stmt, e.is_obsolete from graph_edges_all e, inversepropertyof i where 
	e.pid = i.propertyid2 and i.propertyid1 != i.propertyid2 and not exists (select * from graph_edges e2 where e.rid2 = e2.rid1 
	and e.rtid2 = e2.rtid1 and i.propertyid1 = e2.pid and e.rid1 = e2.rid2 and e.rtid1 = e2.rtid2) and i.kbid = theKbid;
  raise notice 'Inserted inferred inverseOf relationship type 2';

  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_graph(theKbid integer) 
  RETURNS BOOLEAN AS $$
  DECLARE
    result boolean := false;
  BEGIN
    select update_graph(theKbid, true) into result;
	return result;
  END;
$$ LANGUAGE plpgsql;

/*
  Transform an owl knowledge base to a graph easy to view. Node table as 6 columns: rid, rtid, name, label, kbid, is_anonymous.
  Node name is class name, e.g. sao1065676773. Node label is the value of rdfs:label, e.g. hippocampus.
  
  Rules to transform nodes are:
  *) add all primitive classes, excluding system classes and non-owl classes. System classes are created by 
     protege to represent meta class, e.g. rdfs:Class, rdf:Property. Non-owl classes are generally created by
     protege as well, e.g. owl:DeprecatedClass.
  *) add all individuals, excluding non-owl individuals. Non-owl individuals are usually created by protege, 
     e.g. rdf:XMLLiteral.
  *) add all properties as nodes.
  *) add all literals as nodes.
  *) add all data types as nodes.
  *) add owl:Thing as a node. It is a special node. It is used to classify any unspecified node.
  *) add hasValue as nodes. All anonymous classes are transformed to nodes, although they may be hidden in the graph.
     The node name is "hasValue class", and the node label is "something <property_label> as <value>", 
     e.g. "something has Inherent 3D Shape as false".
  *) add dataRange as an anonymous nodes. Use dataRange's browsertext column as name.
  *) add cardinality as an anonymous nodes. Use "Cardinality Restriction" as name, use 
     "something with <property_label> = <cardinality>" as label. For instance, "something with number of Axons = 1".
  *) add minCardinality as anonymous nodes. Use "Min Cardinality Restriction" as name, use
     "something with <property_label> >= <cardinality>" as label. For instance, "something with number of Axons >= 1".  
  *) add maxCardinality as anonymous nodes. Use "Max Cardinality Restriction" as name, use
     "something with <property_label> <= <cardinality>" as label. For instance, "something with number of Axons <= 1".
  *) add someValuesFrom as anonymous nodes. Use "SomeValuesFrom Restriction" as name, use
     "something [exists]<property_label> <value>" as label. For instance, 'something [exists]has_quality DNA'.
  *) add allValuesFrom as anonymous nodes. Use "AllValuesFrom Restriction" as name, use
     "something [forall]<property_label> <value>" as label. For instance, 'something [forall]"has Neurotransmitter" Glutamate'.
  *) add complement classes as anonymous nodes. Use "Complement Class" as name, use
     "not (<class>)" as label. For instance, 'not (Chordata)'.
  *) add union classes as anonymous nodes. Use "Union Class" as name, use
     "union of (<class1>, <class2>, ...)" as label. For instance, "union of (Right limbic lobe, Left limbic lobe)".
  *) add intersection classes as anonymous nodes. Use "Intersection Class" as name, use
     "intersection of (<class1>, <class2>, ...)" as label. For instance, 'intersection of (something [exists]has_quality DNA, chromosome)'.
  *) add oneOf as anonymous nodes. Use "Enumerated Class" as name, use
     "one of (<class1>, <class2>, ...)" as label. For instance, "one of (Rat elderly, Rat late elderly, Rat early elderly)".
  *) add allDifferentIndividual as anonymous nodes. Use "All Different Individual" as name, use
     "AllDifferent {<individual1>, <individual2>, ...}" as label. For instance, "AllDifferent {birn_annot:ABA_app, birn_annot:BAMS_app}".
  *) add hasSelf as anonymous node. Use "hasSelf class" as node name. Its browsertext as label.
  *) add datatype_restriction as anonymous node. Use "Datatype restriction" as node name. Its browsertext as label.
  
  Notice that some class' labels cannot be set unless its member classes get the labels. So the label assigning 
  process runs recursively until all labels are set. To speed up the process, I run a procedure 5 times to assign labels 
  for unassigned nodes. After 5 runs, if there remain unset nodes, I then do a recursive assignment for them. 
  
  Rules to transform edges are:
  *) add subClassOf edges, excluding those represent equivalent classes (A subClassOf B and B subClassOf A).
     Besides, exclude those edges that involve system classes or non-owl classes.
     If the source or target node is an anonymous class, set the edge as hidden. The same rule to set hidden flag is applied to other edges.
  *) add equivalentClass edges. 
  *) add typeOf relationships as non-hidden edges.
  *) add user-defined relationships as non-hidden edges.
  *) add subPropertyOf edges, excluding equivalent properties (A subPropertyOf B and B subPropertyOf A) and system-defined subProperty edges.
  *) add equivalentProperty edges.
  *) add inverseOf edges.
  *) add disjointClasses as edges.
  *) add differentFrom as edges.
  *) add sameAs as edges.
  *) If a property has both domain and range, combine domain and range into one edge. 
     Treat domain as subject (source) and range as object (target). For example, 
       <owl:DatatypeProperty rdf:ID="yearValue">
         <rdfs:domain rdf:resource="#VintageYear" />    
         <rdfs:range  rdf:resource="&xsd;positiveInteger" />
       </owl:DatatypeProperty>
     In the case aboe, an edge "VintageYear yearValue positiveInteger" is created.
     If a property has domain restriction, but no range, use owl:Thing as range. vice versa for range restriction.     
     Also, add "<property> domain <domain_class>" and "<property> range <range_class>" as edges. For the example above,
     "yearValue domain VintageYear" and "yearValue range positiveInteger" are added.
  *) add complementOf edges.
  *) add unionOf edges between union class and its members.
  *) add intersectionOf edges between intersection class and its members.
  *) add oneOf edges between enumeration class and its members.
  *) add allDifferentIndividuals as edges.
  *) add inferred edge from allValuesFrom class. For instance, "Wine -- subclassOf -> (something --:forall:hasMaker-> Winery)"
     will generate: "Wine -- :forall:hasMaker -> Winery".
     Also, add "<allValuesFrom anonymous class> <property> <range_class>" as a hidden edge. 
     e.g. "(something [forall]hasMaker Winery) hasMaker Winery".
  *) add edges from someValuesFrom class, similar to how allValuesFrom class is added.
  *) add derived edge from cardinality class as non-hidden edge. For example, 
     "Vintage -subClassOf-> (something -hasVintageYear(card=1)-> integer)" will be converted to
     "Vintage -hasVintageYear(card=1)->integer".
     Also, add "<Cardinality anonymous class> hasCardinality <value>" as a hidden edge.
  *) add minCardinality class, similar to how cardinalityClass is added.
  *) add maxCardinality class, similar to how cardinalityClass is added. 
  *) update status of some hidden edges. If a restriction appears as object of another non-hidden edge (edge A), 
     set the corresponding restriction edge (edge B) to non-hidden. Otherwise, edge A cannot be reached. 
*/
CREATE OR REPLACE FUNCTION update_graph(theKbid integer, setLabelFlag boolean) 
  RETURNS BOOLEAN AS $$

  DECLARE
    owlRestrictionRid integer;
    owlRestrictionRtid integer;
    onPropertyRid integer;
    onPropertyRtid integer;
    rdfTypeRid integer;
    rdfTypeRtid integer;
	owlNSID integer;
	rdfNSID integer;
  BEGIN
    -- clean up existing nodes and edges
    DELETE FROM graph_edges_all where kbid = theKbid;
    DELETE FROM graph_nodes_all where kbid = theKbid;
    raise notice 'Clean up graph_edges_all and graph_nodes_all table.';

    SELECT p.id, rt.id INTO owlRestrictionRid, owlRestrictionRtid FROM primitiveclass p, resourcetype rt where p.name = 'Restriction' and rt.rtype = 'c' and is_system = true and kbid = theKbid;

	SELECT id into owlNSID from namespace where prefix = 'owl:' and kbid = theKbid;

	IF owlRestrictionRid is null OR owlRestrictionRid <= 0 THEN
		
		INSERT INTO primitiveclass (name,is_system,is_owlclass,is_deprecated,nsid,kbid,browsertext) values (
		'Restriction', true, false, false, owlNSID, theKbid, 'owl:Restriction') RETURNING id into owlRestrictionRid;
		
		SELECT rt.id into owlRestrictionRtid from resourcetype rt where rt.rtype = 'c';
	END IF;
	
    SELECT p.id, rt.id INTO onPropertyRid, onPropertyRtid FROM property p, resourcetype rt where p.name = 'onProperty' and rt.rtype = 'p' and is_system = true and kbid = theKbid;

	IF onPropertyRid is null OR onPropertyRid <= 0 THEN
		INSERT INTO property (name,is_object,is_owl,is_system,nsid,kbid,browsertext) values (
		'onProperty', false, false, true, owlNSID,theKbid, 'owl:onProperty'
		) RETURNING id INTO onPropertyRid;
		
		SELECT rt.id into onPropertyRtid from resourcetype rt where rt.rtype = 'p';
	END IF;
	
 	SELECT id into rdfNSID from namespace where prefix = 'rdf:' and kbid = theKbid;

	SELECT p.id, rt.id INTO rdfTypeRid, rdfTypeRtid FROM property p, resourcetype rt where p.name = 'type' and rt.rtype = 'p' and is_system = true and kbid = theKbid;

	IF rdfTypeRid is null OR rdfTypeRid  <= 0 THEN
		INSERT INTO property (name,is_object,is_owl,is_system,nsid,kbid,browsertext) values (
		'type', false, false, true, rdfNSID,theKbid, 'rdf:type'
		) RETURNING id INTO rdfTypeRid;
		
		SELECT rt.id into rdfTypeRtid from resourcetype rt where rt.rtype = 'p';
	END IF;

    -- insert primitive classes into nodes table
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, r.name, null, kbid, false, false, get_ontology(r.id, rt.id)
      FROM primitiveclass r, resourcetype rt WHERE kbid = theKbid --and is_system = false -- and is_owlclass = true 
      and rt.name = 'primitiveclass';
    raise notice 'Added all primitive classes as nodes.';

    -- insert individuals into nodes table
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, r.name, null, kbid, false, false, get_ontology(r.id, rt.id)
      FROM individual r, resourcetype rt WHERE kbid = theKbid --and is_owl = true
      and rt.name = 'individual';
    raise notice 'Added all individuals as nodes.';

    -- insert properties into nodes table
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, r.name, null, kbid, false, false, get_ontology(r.id, rt.id)
      FROM property r, resourcetype rt WHERE kbid = theKbid
      and rt.name = 'property';
    raise notice 'Added all properties as nodes.';

    -- insert literals into nodes table
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, r.lexicalform, null, kbid, false, false, null
      FROM literal r, resourcetype rt WHERE kbid = theKbid
      and rt.name = 'literal';
    raise notice 'Added all literals as nodes.';

    -- insert datatypes into nodes table
    INSERT INTO graph_nodes_all  SELECT r.id as rid, rt.id as rtid, r.name, null, kbid, false, false, null
      FROM datatype r, resourcetype rt WHERE kbid = theKbid and rt.name = 'datatype';
    raise notice 'Added all data types as nodes.';

    -- insert owl:Thing and owl:Restriction into nodes table, which is a special class
    /*
    INSERT INTO graph_nodes_all select r.id as rid, rt.id as rtid, r.name, r.name, r.kbid, false, false, get_ontology(r.id, rt.id)
     from primitiveclass r, resourcetype rt
      where r.name = 'Thing' and is_system = true and rt.rtype = 'c' and kbid = theKbid;

    INSERT INTO graph_nodes_all select r.id as rid, rt.id as rtid, r.name, r.name, r.kbid, false, false, get_ontology(r.id, rt.id)
     from primitiveclass r, resourcetype rt
      where r.name = 'Restriction' and is_system = true and rt.rtype = 'c' and kbid = theKbid RETURNING rid, rtid INTO owlRestrictionRid, owlRestrictionRtid;
    raise notice 'Added owl:Thing and owl:Restriction as special node.';
*/
/*
    -- insert owl:onProperty into nodes table, which is a special class
    INSERT INTO graph_nodes_all select r.id as rid, rt.id as rtid, r.name, r.name, r.kbid, false, false, get_ontology(r.id, rt.id)
     from property r, resourcetype rt
      where r.name = 'onProperty' and is_system = true and rt.rtype = 'p' and kbid = theKbid RETURNING rid, rtid INTO onPropertyRid, onPropertyRtid;
    raise notice 'Added owl:onProperty as special node.';
*/
	
    -- insert hasValue classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'hasValue class', 'something '||n.label||' as '||n2.label as label, r.kbid, true, false, null
      FROM hasValue r, resourcetype rt, resourcetype rt2, graph_nodes_all n, graph_nodes_all n2 
      where r.kbid = theKbid and r.propertyid = n.rid and n.rtid = rt2.id and rt2.name = 'property' and 
      r.valueid = n2.rid and r.rtid = n2.rtid and rt.name = 'hasvalue';
    raise notice 'Added hasValue classes as nodes.';

    -- insert datarange classes
    INSERT INTO graph_nodes_all SELECT distinct r.id as rid, rt.id as rtid, r.browsertext, r.browsertext, kbid, true, false, null
      FROM datarange r, resourcetype rt WHERE kbid = theKbid and rt.name = 'datarange';
    raise notice 'Added datarange classes as nodes.';

    -- insert cardinality classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'Cardinality Restriction', r.browsertext, kbid, true, false, null
      FROM cardinalityclass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'cardinalityclass';
    raise notice 'Added cardinality classes as nodes.';

    -- insert minCardinality classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'Min Cardinality Restriction', r.browsertext, kbid, true, false, null
      FROM mincardinalityclass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'mincardinalityclass';
    raise notice 'added minCardinality classes as nodes.';
    
    -- insert maxCardinality classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'Max Cardinality Restriction', r.browsertext, kbid, true, false, null
      FROM maxcardinalityclass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'maxcardinalityclass';
    raise notice 'added maxCardinality classes as nodes.';
    
    -- insert someValuesFrom classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'SomeValuesFrom Restriction', r.browsertext, kbid, true, false, null
      FROM someValuesFromClass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'somevaluesfromclass';
    raise notice 'added someValuesFrom classes as nodes.';

    -- insert allValuesFrom classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'AllValuesFrom Restriction', r.browsertext, kbid, true, false, null
      FROM allValuesFromClass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'allvaluesfromclass';
    raise notice 'added allValuesFrom classes as nodes.';

    -- insert complement classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'Complement class', r.browsertext, kbid, true, false, null
      FROM complementclass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'complementclass';
    raise notice 'added complement classes as nodes.';
    
    -- insert union classes into nodes table
    INSERT INTO graph_nodes_all SELECT distinct r.id as rid, rt.id as rtid, 'Union Class', r.browsertext, kbid, true, false, null
      FROM unionclass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'unionclass';
    raise notice 'added union classes as nodes.';

    -- insert intersection classes into nodes table
    INSERT INTO graph_nodes_all SELECT distinct r.id as rid, rt.id as rtid, 'Intersection Class', r.browsertext, kbid, true, false, null
      FROM intersectionclass r, resourcetype rt WHERE kbid = theKbid and rt.name = 'intersectionclass';
    raise notice 'added intersection classes as nodes.';
    
    -- insert oneOf classes
    INSERT INTO graph_nodes_all SELECT distinct r.id as rid, rt.id as rtid, 'Enumeration Class', r.browsertext, kbid, true, false, null
      FROM oneof r, resourcetype rt WHERE kbid = theKbid and rt.name = 'oneof';
    raise notice 'added oneOf classes as nodes.';
    
    -- insert allDifferentIndividual classes
    INSERT INTO graph_nodes_all SELECT distinct r.id as rid, rt.id as rtid, 'All Different Individuals', r.browsertext, kbid, true, false, null
      FROM alldifferentindividual r, resourcetype rt WHERE kbid = theKbid and rt.name = 'alldifferentindividual';
    raise notice 'added allDifferentIndividual classes as nodes.';

	-- insert hasSelf classes
    INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'hasSelf class', r.browsertext as label, r.kbid, true, false, null
      FROM hasSelf r, resourcetype rt where r.kbid = theKbid and rt.name = 'hasself';
    raise notice 'Added hasSelf classes as nodes.';

	-- insert datatype restriction classes
	INSERT INTO graph_nodes_all SELECT r.id as rid, rt.id as rtid, 'datatype restriction', r.browsertext as label, r.kbid, true, false, null
		FROM datatype_restriction r, resourcetype rt where r.kbid = theKbid and rt.name = 'datatype_restriction';
   raise notice 'Added datatype restrictions as nodes.';
		
    -- insert all ontology uri classes
    INSERT INTO graph_nodes_all SELECT distinct r.id as rid, rt.id as rtid, r.uri, r.uri, kbid, false, false, null
      FROM ontologyuri r, resourcetype rt WHERE kbid = theKbid and rt.name = 'ontologyuri';
    raise notice 'added all ontology uri as nodes.';

    -- insert subclassOf edges, remove those representing equivalent classes 
    INSERT INTO graph_edges_all select distinct childid, child_rtid, p.id as pid, parentid, parent_rtid, t.kbid, false, 
    false as hidden, -- hidden to be false
 --     case when parent_rtid in (2,3,4,5,6,10,16) or child_rtid in (2,3,4,5,6,10,16) then true else false end as hidden,
      null as restriction_type, null as restriction_stmt
      from subclassof t, property p where p.name = 'subClassOf' and p.is_system = true and p.kbid = t.kbid and 
      not exists(select * from subclassof t2 where t2.childid = t.parentid and t2.child_rtid = t.parent_rtid 
        and t2.parentid = t.childid and t2.parent_rtid = t.child_rtid) 
      and exists(select * from graph_nodes_all n where childid = n.rid and child_rtid = n.rtid)
      and exists(select * from graph_nodes_all n where parentid = n.rid and parent_rtid = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added subclassOf relationships as edges.';

    -- insert equivalentClass edges
    INSERT INTO graph_edges_all select distinct t.classid1, t.class_rtid1, p.id, t.classid2, t.class_rtid2, t.kbid, 
      false, false, null as restriction_type, null as restriction_stmt
      from equivalentclass t, property p where p.name = 'equivalentClass' and p.is_system = true and p.kbid = t.kbid
      and t.kbid = theKbid;
    raise notice 'added equivalentClass relationships as edges.';

    -- insert typeof edges
    INSERT INTO graph_edges_all select distinct t.instanceid, t.instance_rtid, p.id as pid, t.classid, t.class_rtid, 
      t.kbid, false, false, null as restriction_type, null as restriction_stmt 
      from typeof t, property p where p.name = 'type' and p.is_system = true and p.kbid = t.kbid
      and exists (select * from graph_nodes_all n where t.classid = n.rid and t.class_rtid = n.rtid)
--      and exists (select * from graph_nodes_all n where t.instanceid = n.rid and t.instance_rtid = n.rtid)
      and instance_rtid != 1 and t.kbid = theKbid;
    raise notice 'added typeof relationships as edges.';

    -- insert relationship edges
    INSERT INTO graph_edges_all select distinct subjectid, subject_rtid, propertyid, objectid, object_rtid, t.kbid, 
      false, false, null as restriction_type, null as restriction_stmt from relationship t where 
      exists(select * from graph_nodes_all n where subjectid = n.rid and subject_rtid = n.rtid)
      and exists(select * from graph_nodes_all n where objectid = n.rid and object_rtid = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added user-defined relationships as edges.';

    -- insert subPropertyOf edges
    INSERT INTO graph_edges_all select distinct childid, rt.id, p.id as pid, parentid, rt.id, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from subpropertyof t, property p, resourcetype rt 
      where p.name = 'subPropertyOf' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'p' and
      not exists(select * from subpropertyof t2 where t2.childid = t.parentid and t2.parentid = t.childid) 
      and exists(select * from graph_nodes_all n where childid = n.rid and n.rtid = rt.id)
      and exists(select * from graph_nodes_all n where parentid = n.rid and n.rtid = rt.id)
      and t.kbid = theKbid;
    raise notice 'added subPropertyOf relationships as edges.';

    -- insert equivalentProperty edges
    INSERT INTO graph_edges_all select distinct t.propertyid1, rt.id, p.id, t.propertyid2, rt.id, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from equivalentproperty t, property p, resourcetype rt 
      where p.name = 'equivalentProperty' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'p'
      and exists(select * from graph_nodes_all n where propertyid1 = n.rid and n.rtid = 15)
      and exists(select * from graph_nodes_all n where propertyid2 = n.rid and n.rtid = 15)
      and t.kbid = theKbid;
    raise notice 'added equivalentProperty relationships as edges.';

    -- insert inverseOf edges
    INSERT INTO graph_edges_all select distinct t.propertyid1, rt.id, p.id, t.propertyid2, rt.id, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from inversepropertyof t, property p, resourcetype rt 
      where p.name = 'inverseOf' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'p'
      and exists(select * from graph_nodes_all n where propertyid1 = n.rid and n.rtid = 15)
      and exists(select * from graph_nodes_all n where propertyid2 = n.rid and n.rtid = 15)
      and t.kbid = theKbid;
    raise notice 'added inverseOf relationships as edges.';

    -- insert disjointclass edges
    INSERT INTO graph_edges_all select distinct classid1, rtid1, p.id as pid, classid2, rtid2, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from disjointclass t, property p where p.name = 'disjointWith' and p.is_system = true and p.kbid = t.kbid
      and exists(select * from graph_nodes_all n where classid1 = n.rid and rtid1 = n.rtid)
      and exists(select * from graph_nodes_all n where classid2 = n.rid and rtid2 = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added disjointclass relationships as edges.';

    -- insert differentFrom edges
    INSERT INTO graph_edges_all select distinct individualid1, rt.id, p.id as pid, individualid2, rt.id, t.kbid, 
      false, false, null as restriction_type, null as restriction_stmt
      from differentindividual t, property p, resourcetype rt 
      where p.name = 'differentFrom' and p.is_system = true and p.kbid = t.kbid and rt.rtype='i'
      and exists(select * from graph_nodes_all n where individualid1 = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where individualid2 = n.rid and rt.id = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added differentFrom relationships as edges.';

    -- insert sameAs edges
    INSERT INTO graph_edges_all select distinct individualid1, rt.id, p.id as pid, individualid2, rt.id, t.kbid, 
      false, false, null as restriction_type, null as restriction_stmt
      from sameindividual t, property p, resourcetype rt 
      where p.name = 'sameAs' and p.is_system = true and p.kbid = t.kbid and rt.rtype='i'
      and exists(select * from graph_nodes_all n where individualid1 = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where individualid2 = n.rid and rt.id = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added sameAs relationships as edges.';

    -- insert Virtuoso-style triplets about hasvalue restrictions
    INSERT INTO graph_edges_all select t.id, 10, rdfTypeRid, owlRestrictionRid, owlRestrictionRtid, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from hasvalue t where t.kbid = theKbid;
      
    INSERT INTO graph_edges_all select t.id, 10, onPropertyRid, t.propertyid, 15, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from hasvalue t where t.kbid = theKbid;

    INSERT INTO graph_edges_all select t.id, 10, p.id, t.valueid, t.rtid, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from hasvalue t, property p where 
      t.kbid = theKbid and p.kbid = theKbid and p.name = 'hasValue' and p.is_system = true;

    raise notice 'added Virtuoso-style triplets about hasValue restrictions';

    -- insert Virtuoso-style triplets about hasself restrictions
    INSERT INTO graph_edges_all select t.id, 20, rdfTypeRid, owlRestrictionRid, owlRestrictionRtid, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from hasself t where t.kbid = theKbid;
      
    INSERT INTO graph_edges_all select t.id, 20, onPropertyRid, t.propertyid, 15, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from hasself t where t.kbid = theKbid;

    raise notice 'added Virtuoso-style triplets about hasSelf restrictions';

    -- insert domain and range edges. Treat domain as subject (source) and range as object (target).
    -- If a property has domain restriction, but no range, use owl:Thing as range. 
    -- vice versa for range restriction.
    INSERT INTO graph_edges_all select distinct t1.domainid, t1.rtid as rtid1, t1.propertyid, t2.rangeid, 
      t2.rtid as rtid2, t1.kbid, false, false, null as restriction_type, null as restriction_stmt
      from domain t1, range t2 where t1.propertyid = t2.propertyid and t1.kbid = t2.kbid
      and exists(select * from graph_nodes_all n where domainid = n.rid and t1.rtid = n.rtid)
      and exists(select * from graph_nodes_all n where rangeid = n.rid and t2.rtid = n.rtid)
      and t1.kbid = theKbid;
    raise notice 'added inferred domain-range relationships as edges.';
    
    INSERT INTO graph_edges_all select distinct t1.domainid, t1.rtid as rtid1, t1.propertyid, c.id as rid2, 
      rt.id as rtid2, t1.kbid, true, false, null as restriction_type, null as restriction_stmt
      from domain t1, resourcetype rt, primitiveclass c where rt.rtype = 'c' and c.name = 'Thing' and c.is_system = true
      and c.kbid = t1.kbid and not exists (select * from range t2 where t1.propertyid = t2.propertyid)
      and exists(select * from graph_nodes_all n where domainid = n.rid and t1.rtid = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt.id = n.rtid)
      and t1.kbid = theKbid;
    raise notice 'added domain relationships as edges.';

    INSERT INTO graph_edges_all select distinct c.id as rid1, rt.id as rtid1, t1.propertyid, t1.rangeid, t1.rtid as rtid2,
      t1.kbid, true, false, null as restriction_type, null as restriction_stmt
      from range t1, resourcetype rt, primitiveclass c where rt.rtype = 'c' and c.name = 'Thing' and c.is_system = true
      and c.kbid = t1.kbid and not exists (select * from domain t2 where t1.propertyid = t2.propertyid)
      and exists(select * from graph_nodes_all n where rangeid = n.rid and t1.rtid = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt.id = n.rtid)
      and t1.kbid = theKbid;
    raise notice 'added range relationships as edges.';

    -- Also, insert domain and range as properties of the restricted property. 
    INSERT INTO graph_edges_all select distinct t.propertyid, rt.id, p.id, t.domainid, t.rtid, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from domain t, property p, resourcetype rt 
      where p.name = 'domain' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'p'
      and exists(select * from graph_nodes_all n where propertyid = n.rid and n.rtid = 15)
      and exists(select * from graph_nodes_all n where t.domainid = n.rid and n.rtid = t.rtid)
      and t.kbid = theKbid;
    raise notice 'added domain as properties of the restricted property into edge table.';

    INSERT INTO graph_edges_all select distinct t.propertyid, rt.id, p.id, t.rangeid, t.rtid, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from range t, property p, resourcetype rt 
      where p.name = 'range' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'p'
      and exists(select * from graph_nodes_all n where propertyid = n.rid and n.rtid = 15)
      and exists(select * from graph_nodes_all n where t.rangeid = n.rid and n.rtid = t.rtid)
      and t.kbid = theKbid;
    raise notice 'added domain as properties of the restricted property into edge table.';

    -- insert complementOf edges from complementclass.
    INSERT INTO graph_edges_all select distinct t.id, rt.id as rtid1, p.id as pid, t.complementclassid, t.rtid, t.kbid, 
      false, false, null as restriction_type, null as restriction_stmt
      from complementclass t, property p, resourcetype rt 
      where p.name = 'complementOf' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 't'
      and exists(select * from graph_nodes_all n where t.complementclassid = n.rid and n.rtid = t.rtid)
      and t.kbid = theKbid;
    raise notice 'added complementOf relationships as edges.';

    -- insert unionOf edges from unionclass
    INSERT INTO graph_edges_all select distinct t.id, rt.id as rtid1, p.id as pid, t.classid, t.rtid, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from unionclass t, property p, resourcetype rt 
      where p.name = 'unionOf' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'u'
      and exists(select * from graph_nodes_all n where t.classid = n.rid and n.rtid = t.rtid)
      and t.kbid = theKbid;
    raise notice 'added unionOf relationships as edges.';
    
    -- insert intersectionOf edges from intersectionclass.
    INSERT INTO graph_edges_all select distinct t.id, rt.id as rtid1, p.id as pid, t.classid, t.rtid, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from intersectionclass t, property p, resourcetype rt 
      where p.name = 'intersectionOf' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'x'
      and exists(select * from graph_nodes_all n where t.classid = n.rid and n.rtid = t.rtid)
      and t.kbid = theKbid;
    raise notice 'added intersectionOf relationships as edges.';

	/*
    -- insert inferred edge from intersection class.
    -- e.g. X -- subclassOf/equivalent -> (intersectionOf(A, B, C)) will generate:
    -- X subclassOf/equivalent A, X subClassOf/equivalent B, X subClassOf/equivalent C
    INSERT INTO graph_edges_all select distinct e.rid1, e.rtid1, e.pid, i.classid, i.rtid, e.kbid, true, false,
	null as restriction_type, null as restriction_stmt
	from graph_edges_all e, intersectionclass i, resourcetype rt
	where e.rid2 = i.id and e.rtid2 = rt.id and rt.rtype = 'x' and e.kbid = theKbid;
    raise notice 'added derived intersectionOf relationships as edges.';
*/
    -- insert oneOf edges from oneof table.
    INSERT INTO graph_edges_all select distinct t.id, rt.id as rtid1, p.id as pid, t.valueid, t.rtid, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from oneof t, property p, resourcetype rt 
      where p.name = 'oneOf' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'o'
      and exists(select * from graph_nodes_all n where t.valueid = n.rid and n.rtid = t.rtid)
      and t.kbid = theKbid;
    raise notice 'added oneOf relationships as edges.';
    
    -- insert allDifferentIndividual edges 
    INSERT INTO graph_edges_all select distinct t.id, rt.id as rtid1, p.id as pid, t.individualid, rt2.id, t.kbid, false, false,
      null as restriction_type, null as restriction_stmt
      from alldifferentindividual t, property p, resourcetype rt, resourcetype rt2 
      where p.name = 'distinctMembers' and p.is_system = true and p.kbid = t.kbid and rt.rtype = 'y' and rt2.rtype = 'i'
      and exists(select * from graph_nodes_all n where t.individualid = n.rid and n.rtid = rt2.id)
      and t.kbid = theKbid;
    raise notice 'added allDifferentIndividual relationships as edges.';

    -- insert allValuesFrom edges (hidden)
    INSERT INTO graph_edges_all select distinct t.id, rt.id, propertyid, rangeclassid, t.rtid, t.kbid, false, true, 'a', 'forall'
      from allvaluesfromclass t, resourcetype rt where rt.rtype = 'a'
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where rangeclassid = n.rid and t.rtid = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added allValuesFrom relationships as hidden edges.';

	/*
    -- insert inferred edge from allValuesFrom class.
    -- e.g. Wine -- subclassOf -> (something --:forall:hasMaker-> Winery) will generate:
    -- Wine -- :forall:hasMaker -> Winery
    INSERT INTO graph_edges_all select distinct s.childid, s.child_rtid, t.propertyid, t.rangeclassid, t.rtid, t.kbid, true, false, 'a', 'forall'
      from allvaluesfromclass t, subclassof s, resourcetype rt
      where s.parentid = t.id and s.parent_rtid = rt.id and rt.rtype = 'a'
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where rangeclassid = n.rid and t.rtid = n.rtid)
      and not exists(select * from graph_edges_all e2 where s.childid=e2.rid1 and s.child_rtid=e2.rtid1 and t.propertyid=e2.pid
          and t.rangeclassid=e2.rid2 and t.rtid=e2.rtid2)
      and t.kbid = theKbid;
    raise notice 'added inferred subclassOf-allValuesFrom relationships as edges.';
*/

    -- insert Virtuoso-style triplets about allValuesFrom restrictions
    INSERT INTO graph_edges_all select t.id, 2, rdfTypeRid, owlRestrictionRid, owlRestrictionRtid, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from allvaluesfromclass t where t.kbid = theKbid;
      
    INSERT INTO graph_edges_all select t.id, 2, onPropertyRid, t.propertyid, 15, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from allvaluesfromclass t where t.kbid = theKbid;

    INSERT INTO graph_edges_all select t.id, 2, p.id, t.rangeclassid, t.rtid, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from allvaluesfromclass t, property p where 
      t.kbid = theKbid and p.kbid = theKbid and p.name = 'allValuesFrom' and p.is_system = true;

    raise notice 'added Virtuoso-style triplets about allValuesFrom restrictions';
    
    -- insert someValuesFrom edges (hidden)
    INSERT INTO graph_edges_all select distinct t.id, rt.id, propertyid, rangeclassid, t.rtid, t.kbid, false, true, 'v', 'exists'
      from somevaluesfromclass t, resourcetype rt where rt.rtype = 'v'
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where rangeclassid = n.rid and t.rtid = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added someValuesFrom relationships as hidden edges.';

	/*
    -- insert derived edge from someValuesFrom class, similar to those derived allValuesFrom edges.
    INSERT INTO graph_edges_all select distinct s.childid, s.child_rtid, t.propertyid, t.rangeclassid, t.rtid, t.kbid, true, false, 'v', 'exists'
      from somevaluesfromclass t, subclassof s, resourcetype rt
      where s.parentid = t.id and s.parent_rtid = rt.id and rt.rtype = 'v'
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where rangeclassid = n.rid and t.rtid = n.rtid)
      and not exists(select * from graph_edges_all e2 where s.childid=e2.rid1 and s.child_rtid=e2.rtid1 and t.propertyid=e2.pid
          and t.rangeclassid=e2.rid2 and t.rtid=e2.rtid2)
      and t.kbid = theKbid;
    raise notice 'added inferred subclassOf-someValuesFrom relationships as edges.';
	*/
	
    -- insert Virtuoso-style triplets about someValuesFrom restrictions
    INSERT INTO graph_edges_all select t.id, 3, rdfTypeRid, owlRestrictionRid, owlRestrictionRtid, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from somevaluesfromclass t where t.kbid = theKbid;
      
    INSERT INTO graph_edges_all select t.id, 3, onPropertyRid, t.propertyid, 15, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from somevaluesfromclass t where t.kbid = theKbid;

    INSERT INTO graph_edges_all select t.id, 3, p.id, t.rangeclassid, t.rtid, t.kbid, true, false, 
      null as restriction_type, null as restriction_stmt from somevaluesfromclass t, property p where 
      t.kbid = theKbid and p.kbid = theKbid and p.name = 'someValuesFrom' and p.is_system = true;

    raise notice 'added Virtuoso-style triplets about someValuesFrom restrictions';

    -- insert cardinality edges (hidden)
    INSERT INTO graph_edges_all select distinct t.id, rt.id, propertyid, c.id, rt2.id, t.kbid, false, true, 'b', 'cardinality='||cardinality
      from cardinalityclass t, resourcetype rt, datatype c, resourcetype rt2 
      where rt.rtype = 'b' and rt2.rtype = 'd' and c.name = 'int' and c.kbid = t.kbid
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt2.id = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added cardinality relationships as hidden edges.';

    -- insert derived edge from cardinality class. For example, 
    -- Vintage -subClassOf-> (something -hasVintageYear(card=1)-> integer) will be converted to
    -- Vintage -hasVintageYear(card=1)->integer
    INSERT INTO graph_edges_all select distinct s.childid, s.child_rtid, t.propertyid, c.id, rt2.id, t.kbid, true, false, 'b', 'cardinality='||cardinality
      from cardinalityclass t, resourcetype rt, datatype c, resourcetype rt2, subclassof s
      where s.parentid = t.id and s.parent_rtid = rt.id and rt.rtype = 'b' and rt2.rtype = 'd' and c.name = 'int' and c.kbid = t.kbid
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt2.id = n.rtid)
      and not exists(select * from graph_edges_all e2 where s.childid=e2.rid1 and s.child_rtid=e2.rtid1 and t.propertyid=e2.pid
          and c.id=e2.rid2 and rt2.id=e2.rtid2)
      and t.kbid = theKbid;
    raise notice 'added subclassOf-cardinality relationships as edges.';

    -- insert minCardinality edges (hidden)
    INSERT INTO graph_edges_all select distinct t.id, rt.id, propertyid, c.id, rt2.id, t.kbid, false, true, 'm', 'cardinality>='||mincardinality
      from mincardinalityclass t, resourcetype rt, datatype c, resourcetype rt2 
      where rt.rtype = 'm' and rt2.rtype = 'd' and c.name = 'int' and c.kbid = t.kbid
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt2.id = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added minCardinality relationships as hidden edges.';

    -- insert derived edge from minCardinality class, similar to cardinality edges.
    INSERT INTO graph_edges_all select distinct s.childid, s.child_rtid, t.propertyid, c.id, rt2.id, t.kbid, true, false, 'm', 'cardinality>='||mincardinality
      from mincardinalityclass t, resourcetype rt, datatype c, resourcetype rt2, subclassof s
      where s.parentid = t.id and s.parent_rtid = rt.id and rt.rtype = 'm' and rt2.rtype = 'd' and c.name = 'int' and c.kbid = t.kbid
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt2.id = n.rtid)
      and not exists(select * from graph_edges_all e2 where s.childid=e2.rid1 and s.child_rtid=e2.rtid1 and t.propertyid=e2.pid
          and c.id=e2.rid2 and rt2.id=e2.rtid2)
      and t.kbid = theKbid;
    raise notice 'added subclassOf-minCardinality relationships as edges.';

    -- insert maxCardinality edges (hidden)
    INSERT INTO graph_edges_all select distinct t.id, rt.id, propertyid, c.id, rt2.id, t.kbid, false, true, 'n', 'cardinality<='||maxcardinality
      from maxcardinalityclass t, resourcetype rt, datatype c, resourcetype rt2 
      where rt.rtype = 'n' and rt2.rtype = 'd' and c.name = 'int' and c.kbid = t.kbid
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt2.id = n.rtid)
      and t.kbid = theKbid;
    raise notice 'added maxCardinality relationships as hidden edges.';

    -- insert derived edge from maxCardinality class, similar to cardinality edges.
    INSERT INTO graph_edges_all select distinct s.childid, s.child_rtid, t.propertyid, c.id, rt2.id, t.kbid, true, false, 'n', 'cardinality<='||maxcardinality
      from maxcardinalityclass t, resourcetype rt, datatype c, resourcetype rt2, subclassof s
      where s.parentid = t.id and s.parent_rtid = rt.id and rt.rtype = 'n' and rt2.rtype = 'd' and c.name = 'int' and c.kbid = t.kbid
      and exists(select * from graph_nodes_all n where t.id = n.rid and rt.id = n.rtid)
      and exists(select * from graph_nodes_all n where c.id = n.rid and rt2.id = n.rtid)
      and not exists(select * from graph_edges_all e2 where s.childid=e2.rid1 and s.child_rtid=e2.rtid1 and t.propertyid=e2.pid
          and c.id=e2.rid2 and rt2.id=e2.rtid2)
      and t.kbid = theKbid;
    raise notice 'added subclassOf-maxCardinality relationships as edges.';
    
	-- insert datatype restriction as edges.
/*	INSERT INTO graph_edges_all select t.dtid, rt.id as rtid1, t.facetPropId, t.literal_id, rt2.id as rtid2, t.kbid, true, false, 'd', 'datatype'
	  from datatype_restriction t, resourcetype rt, resourcetype rt2
	  where rt.rtype = 'd' and rt2.rtype = 'l' and t.kbid = theKbid; */
	  
	INSERT INTO graph_edges_all select t.id as rid1, rt.id as rtid1, p.id as pid, t.dtid, rt2.id as rtid2,  t.kbid, true, false, 'd', 'datatype'
	  from datatype_restriction t, resourcetype rt, resourcetype rt2, property p
	  where rt.rtype = 'e' and rt2.rtype = 'd' and p.name = 'onDatatype' and p.kbid = t.kbid and t.kbid = theKbid;

	INSERT INTO graph_edges_all select t.id as rid1, rt.id as rtid1, t.facetPropId as pid, t.literal_id, rt2.id as rtid2,  t.kbid, true, false, 'd', 'datatype'
	  from datatype_restriction t, resourcetype rt, resourcetype rt2
	  where rt.rtype = 'e' and rt2.rtype = 'l' and t.kbid = theKbid;
	  
    -- for inversedOf property, add its inversed edge
    INSERT INTO graph_edges_all select distinct g.rid2, g.rtid2, ip.propertyid2 as pid, g.rid1, g.rtid1, g.kbid, true, g.hidden, g.restriction_type, g.restriction_stmt
      from graph_edges_all g, inversepropertyof ip where g.pid = ip.propertyid1 and g.kbid = theKbid and not exists (select * from graph_edges_all g2 where g2.rid1 = g.rid2 and
      g2.rtid1 = g.rtid2 and g2.pid = ip.propertyid2 and g2.rid2 = g.rid1 and g2.rtid2 = g.rtid1 and g2.kbid = g.kbid);
    raise notice 'added inferred inversedOf relationships as edges';

--    perform update_inference_edges(theKbid);
	
    -- update status of some hidden edges. If a restriction appears as object of another non-hidden edge, set the corresponding restriction edge to non-hidden.
    UPDATE graph_edges_all g1 set hidden = false where hidden = true and g1.rtid1 in 
      (select id from resourcetype where rtype in ('v', 'a', 'b', 'm', 'n', 'h'))
      and exists (select * from graph_edges_all g2 where g2.rtid2 = g1.rtid1 and g2.rid2 = g1.rid1 and g2.hidden = false);
    raise notice 'updated the status of hidden edges.';

    -- update node labels.
	perform set_labels(theKbid, setLabelFlag);
	raise notice 'updated node labels.';
 	
    -- update is_obsolete flag.
    perform set_obsolete_flag(theKbid);
    raise notice 'obsolete flag is updated.';

    RETURN true;

  END;
$$ LANGUAGE plpgsql;
