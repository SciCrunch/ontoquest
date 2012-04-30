CREATE OR REPLACE FUNCTION set_biopax_labels(theKbid INTEGER) RETURNS BOOLEAN AS $$
  /*
    Set the labels for biopax pathways.
  */
   DECLARE
    rec1 RECORD;
	rec2 RECORD;
	
	uriLeft TEXT = 'http://www.biopax.org/release/biopax-level3.owl#left';
	uriRight TEXT = 'http://www.biopax.org/release/biopax-level3.owl#right';
	
	tmpLeft TEXT := null;
	tmpRight TEXT := null;
	theLabel TEXT;
	undefinedLabel TEXT := 'undefined';
  BEGIN
    -- update label with displayName and standardName
	update graph_nodes_all as n1 set label = (select n2.name from graph_edges e, graph_nodes n2,
	property p where e.rid1 = n1.rid and e.rtid1 = n1.rtid and e.rid2 = n2.rid and e.rtid2 = n2.
	rtid and e.pid = p.id and p.name = 'displayName' and e.kbid = theKbid) where n1.rtid = 12 
	and exists (select n2.name from graph_edges e, graph_nodes n2, property p where e.rid1 = n1.
	rid and e.rtid1 = n1.rtid and e.rid2 = n2.rid and e.rtid2 = n2.rtid and e.pid = p.id and p.
	name = 'displayName');

	update graph_nodes_all as n1 set label = (select n2.name from graph_edges e, graph_nodes n2,
	property p where e.rid1 = n1.rid and e.rtid1 = n1.rtid and e.rid2 = n2.rid and e.rtid2 = n2.
	rtid and e.pid = p.id and p.name = 'standardName' and e.kbid = theKbid) where n1.rtid = 12 
	and exists (select n2.name from graph_edges e, graph_nodes n2, property p where e.rid1 = n1.
	rid and e.rtid1 = n1.rtid and e.rid2 = n2.rid and e.rtid2 = n2.rtid and e.pid = p.id and p.
	name = 'standardName');

	-- update reaction's label. Use "Left1 + left2 ... -> right1 + right2..." as label.
	FOR rec1 IN select distinct rid1, rtid1 from graph_edges e where e.pid in (select rid from 
	graph_nodes where kbid = theKbid and rtid = 15 and (uri = uriLeft or uri = uriRight)) LOOP
	
		tmpLeft := ' ';
		FOR rec2 IN select n.label from graph_edges e, graph_nodes n where rid1 = rec1.rid1 and
		rtid1 = rec1.rtid1 and e.rid2 = n.rid and e.rtid2 = n.rtid and e.pid in (select rid from
		graph_nodes where kbid = theKbid and rtid = 15 and uri = uriLeft) LOOP
			tmpLeft := tmpLeft || '['||rec2.label ||']' ||'+';
		END LOOP; 
		
		IF tmpLeft = ' ' THEN
		    tmpLeft := undefinedLabel;
		ELSE
			tmpLeft := trim(tmpLeft, ' +');
		END IF;
		
		tmpRight := ' ';
		FOR rec2 IN select n.label from graph_edges e, graph_nodes n where rid1 = rec1.rid1 and
		rtid1 = rec1.rtid1 and e.rid2 = n.rid and e.rtid2 = n.rtid and e.pid in (select rid from
		graph_nodes where kbid = theKbid and rtid = 15 and uri = uriRight) LOOP
			tmpRight := tmpRight || '['||rec2.label ||']' ||'+';
		END LOOP; 
		
		IF tmpRight = ' ' THEN
		    tmpRight := undefinedLabel;
		ELSE
			tmpRight := trim(tmpRight, ' +');
		END IF;
		
		IF tmpLeft != undefinedLabel AND tmpRight != undefinedLabel THEN
			theLabel := tmpLeft || ' -> ' || tmpRight;
			update graph_nodes_all set label = theLabel where rid = rec1.rid1 and rtid = rec1.rtid1;
		END IF;
	END LOOP;
  RETURN true;

  END;
$$ LANGUAGE plpgsql;