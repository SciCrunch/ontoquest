DROP TABLE IF EXISTS rsc_depth;

-- depth table to record a resource's depth in 3 hierarchy tree: subClassOf tree (for class),
-- subPropertyOf tree (for property) and partOf tree (for class). This table is important for
-- computing center-piece subgraphs.
CREATE TABLE rsc_depth (
  rid integer,
  rtid integer,
  treetype char(2) check (treetype in ('sc', 'sp', 'po')),
  kbid integer NOT NULL REFERENCES kb (id) ON DELETE CASCADE,
  depth integer
);

-- expand_ children_depth: compute children's depth of a node.
-- param theRid: the node's rid
-- param theRtid: the node's rtid
-- param theKbid: the knowledge base to search for children
-- param thePid: property id of the edge connecting child and its parent
-- param theLevel: the level of node (theRid, theRtid)
-- param theTreetype: category of thePid
-- param ancestors: array of [rid, rtid]. They are the node's ancestors. Used for checking cycle.
CREATE OR REPLACE FUNCTION expand_children_depth(theRid integer, theRtid integer, theKbid integer, 
  thePid integer, theLevel integer, theTreetype char, ancestorTbl varchar) 
  RETURNS VOID AS $$
  DECLARE 
    rec record;
    chd_level integer;
    tmp_level integer := -1;
    tmp_int integer;
  BEGIN
--    raise notice 'theRid = %, theRtid = %, theLevel = %', theRid, theRtid, theLevel;
    
    -- add the node into ancestor list
    EXECUTE 'insert into '||ancestorTbl||' values ('||theRid||', '||theRtid||
            ', '||theLevel||')';

    -- increment depth by 1
    chd_level := theLevel + 1;

    FOR rec IN
     select rid1, rtid1 from graph_edges where hidden = false and rid2 = theRid and rtid2 = theRtid and kbid = theKbid and pid = thePid
    LOOP
      exit when rec.rid1 is null or rec.rtid1 is null;

      -- check cycles
      EXECUTE 'select depth from '||ancestorTbl||' where rid = '||rec.rid1
        ||' and rtid = '||rec.rtid1 INTO tmp_int;
      IF tmp_int is not null THEN    -- found cycle
        -- if the child is an equivalent class of its parent, just ignore it.
        select 1 into tmp_int from equivalentclass where classid1 = theRid and 
          class_rtid1 = theRtid and classid2 = rec.rid1 and class_rtid2 = rec.rtid1;    
        if found then -- (theRid, theRtid) and (rec.rid1, rec.rtid1) is equivalent.
          continue; -- ignore (rec.rid1, rec.rtid1)
        end if;

        -- otherwise, raise an exception
        raise exception 'Unable to compute depth due to cycles in the hierarchy. rid = %, rtid = %', 
          rec.rid1, rec.rtid1;
      END IF;

      select depth into tmp_level from rsc_depth where rid = rec.rid1 and rtid = rec.rtid1 
        and treetype = theTreetype and kbid = theKbid;
      if NOT FOUND then
        raise exception 'Unknown resource (rid=%, rtid=%) in kb (kbid=%)', rec.rid1, rec.rtid1, theKbid;
      else
        -- update child's depth only if its existing depth is less than chd_level
        if tmp_level < chd_level then
          begin
            update rsc_depth set depth = chd_level where rid = rec.rid1 and rtid = rec.rtid1 and treetype = theTreetype and kbid = theKbid;
          end;
          -- expand to children recursively
          perform expand_children_depth(rec.rid1, rec.rtid1, theKbid, thePid, chd_level, theTreetype, ancestorTbl);
      
        end if;
      end if;

    end loop;

    -- now, we can remove the node from ancestorTbl
    EXECUTE 'delete from '||ancestorTbl||' where rid = '||theRid||' and rtid = '||theRtid;  
    
  END;
$$ LANGUAGE plpgsql;

-- compute depth for hierarchy tree w.r.t. property (id = thePid), e.g. subClassOf, subPropertyOf, partOf
CREATE OR REPLACE FUNCTION compute_depth(theKbid integer, thePid integer, theTreetype char) RETURNS VOID AS $$
  DECLARE
    root record;
    rsc_type_expr text;
  BEGIN

    -- clean up depth table w.r.t. thePid and theKbid
    delete from rsc_depth where treetype = theTreetype and kbid = theKbid;

    -- prepare resource type condition
    if theTreetype = 'sc' OR theTreetype = 'po' then
      rsc_type_expr := ' and rtid <= 11 ';
    elsif theTreetype = 'sp' then
      rsc_type_expr := ' and rtid = 15 ';
    else
      raise exception 'unknown hierarchy tree type: %', theTreetype;
    end if;

    -- load nodes in the knowledge base (kbid = theKbid), initialize depth to 0.
    EXECUTE 'insert into rsc_depth select rid, rtid, '''||theTreetype||''', '||theKbid||
      ', 1 from graph_nodes where kbid = '||theKbid || rsc_type_expr;

    -- create temp table to store ancestors
    EXECUTE 'CREATE TEMP TABLE ancestors_tmp (rid integer, rtid integer, depth integer, constraint atpk primary key (rid, rtid))';

    -- find roots in the hierarchy tree of property (thePid).
    FOR root IN select distinct rv1.rid2, rv1.rtid2
      from graph_edges rv1 where kbid = theKbid and pid = thePid and hidden = false and not exists (select * from graph_edges rv2 
      where rv1.rid2 = rv2.rid1 and rv1.rtid2 = rv2.rtid1 and rv1.pid = rv2.pid and rv1.kbid = rv2.kbid and hidden = false)
    LOOP
      EXIT WHEN root.rid2 is null or root.rtid2 is null;

      -- for every root node, compute its children's depth recursively.
      perform expand_children_depth(root.rid2, root.rtid2, theKbid, thePid, 1, theTreetype, 'ancestors_tmp');
    end loop;

    DROP TABLE IF EXISTS ancestors_tmp;

  END;
$$ LANGUAGE plpgsql;

--- ***** compute pi value ***** ---

DROP TABLE IF EXISTS rel_pi;

CREATE TABLE rel_pi (
  rel_oid oid, -- oid of the instance relationship in graph_edges
  cid1 integer,
  ctid1 integer,
  pid integer,
  cid2 integer,
  ctid2 integer,
  pi integer -- pi value
--  , weight real
);

CREATE OR REPLACE FUNCTION compute_pi () RETURNS VOID AS $$
  DECLARE
    individual_rtid integer;
    rel record;
    r1 refcursor;
    r1Rec record;
    r2 refcursor;
    r2Rec record;
  BEGIN
    delete from rel_pi;

    select id into individual_rtid from resourcetype where name = 'individual';

    <<loop1>>
    for rel in select oid, pid from graph_edges_all where is_obsolete = false and hidden = false and rtid1 = individual_rtid and rtid2 = individual_rtid
    loop
      exit when rel.oid is null;
      
      open r1 for select r.rid2, r.rtid2 from graph_edges r, graph_edges_all r3, property p where r3.is_obsolete = false and r3.oid = rel.oid
                and r.rid1 = r3.rid1 and r.rtid1 = r3.rtid1 and r.pid = p.id and p.name = 'type' and p.is_system = true;
     
      <<loop2>>
      loop
        fetch r1 into r1Rec;
        if not found then
          exit loop2;
        end if;

        -- I tried MOVE statement to rewind r2, instead of open it repeatly. The rewinding doesn't work.
        -- see http://groups.google.com/group/pgsql.hackers/browse_thread/thread/c5022f809ed1fbd6
        --execute 'move first in r2';

        open r2 for select r.rid2, r.rtid2 from graph_edges r, graph_edges_all r3, property p where r3.is_obsolete = false and r3.oid = rel.oid
                and r.rid1 = r3.rid2 and r.rtid1 = r3.rtid2 and r.pid = p.id and p.name = 'type' and p.is_system = true;

        <<loop3>>
        loop
          fetch r2 into r2Rec;
          if not found then
            exit loop3;
          end if;
 
          insert into rel_pi (rel_oid, cid1, ctid1, pid, cid2, ctid2) values (rel.oid, r1Rec.rid2, r1Rec.rtid2, rel.pid, r2Rec.rid2, r2Rec.rtid2);

        end loop loop3;
        close r2;
      end loop loop2;      
      close r1;
    end loop loop1;
    
    execute 'update rel_pi r set pi = (
      select count(rel_oid) from rel_pi r1 where r.cid1 = r1.cid1 and r.ctid1 = r1.ctid1 and r.pid = r1.pid 
      and r.cid2 = r1.cid2 and r.ctid2 = r1.ctid2 group by cid1, ctid1, pid, cid2, ctid2)';

  END;
$$ LANGUAGE plpgsql;

DROP TABLE IF EXISTS rel_weight;

CREATE TABLE rel_weight (
  rel_oid oid,
  weight real default 0.0001
);

CREATE OR REPLACE FUNCTION compute_weight() RETURNS VOID AS $$
  DECLARE
    mu_sp_p real;
    mu_sc_u real;
    mu_sc_v real;
    mu_po_u real;
    mu_po_v real;
    degree_u integer;
    degree_v integer;
    sigma real;
    alpha real;
    Hsp integer; -- height of subPropertyOf tree
    Hsc integer; -- height of subClassOf tree
    Hpo integer; -- height of partOf tree
    depth_u integer;
    depth_p integer;
    depth_v integer;
    theWeight real;

    relRec record;
  BEGIN
    insert into rel_weight(rel_oid) select oid from graph_edges_all where is_obsolete = false and hidden = false;

    -- compute class and property specificity
    perform compute_depth(k.id, p.id, 'sc') from kb k, property p where k.id = p.kbid and p.name = 'subClassOf';

    perform compute_depth(k.id, p.id, 'sp') from kb k, property p where k.id = p.kbid and p.name = 'subPropertyOf';

    perform compute_depth(k.id, p.id, 'po') from kb k, property p where k.id = p.kbid and p.name = 'partOf';

    ---- store height of SP, SC, and PO tree wrt every kb. It is used for easy lookup.
    EXECUTE 'create table tmp_height (kbid integer, treetype char(2), height integer)';
  
    insert into tmp_height select kbid, treetype, max(depth) from rsc_depth group by kbid, treetype;

    -- compute instance participation selectivity (IPS), aka pi value
    perform compute_pi();

    -- compute span heuristic (SPAN), IGNORED for now
    alpha := 0;

    -- combine all scores to get weight
    FOR relRec IN select * from graph_edges where hidden = false
    LOOP
      exit when relRec.rid1 is null or relRec.rid2 is null;

      -- compute mu values
      select height into Hsp from tmp_height where kbid = relRec.kbid and treetype = 'sp';
      IF FOUND AND Hsp > 0 THEN
        select depth/Hsp::real into mu_sp_p from rsc_depth where rid = relRec.pid and treetype = 'sp';
      ELSE
        mu_sp_p := 0;
      END IF;

      select height into Hsc from tmp_height th, rsc_depth rd 
        where rd.rid = relRec.rid1 and rd.rtid = relRec.rtid1 and rd.kbid = th.kbid and rd.treetype = 'sc';
      IF FOUND AND Hsc > 0 THEN
        select depth/Hsc::real into mu_sc_u from rsc_depth where rid = relRec.rid1 and rtid = relRec.rtid1 and treetype = 'sc';
      ELSE
        mu_sc_u := 0;
      END IF;

      select height into Hsc from tmp_height th, rsc_depth rd 
        where rd.rid = relRec.rid2 and rd.rtid = relRec.rtid2 and rd.kbid = th.kbid and rd.treetype = 'sc';
      IF FOUND AND Hsc > 0 THEN
        select depth/Hsc::real into mu_sc_v from rsc_depth where rid = relRec.rid2 and rtid = relRec.rtid2 and treetype = 'sc';
      ELSE
        mu_sc_v := 0;
      END IF;

      select height into Hpo from tmp_height th, rsc_depth rd 
        where rd.rid = relRec.rid1 and rd.rtid = relRec.rtid1 and rd.kbid = th.kbid and rd.treetype = 'po';
      IF FOUND AND Hpo > 0 THEN
        select depth/Hpo::real into mu_po_u from rsc_depth where rid = relRec.rid1 and rtid = relRec.rtid1 and treetype = 'po';
      ELSE
        mu_po_u := 0;
      END IF;

      select height into Hpo from tmp_height th, rsc_depth rd 
        where rd.rid = relRec.rid2 and rd.rtid = relRec.rtid2 and rd.kbid = th.kbid and rd.treetype = 'po';
      IF FOUND AND Hpo > 0 THEN
        select depth/Hpo::real into mu_po_v from rsc_depth where rid = relRec.rid2 and rtid = relRec.rtid2 and treetype = 'po';
      ELSE
        mu_po_v := 0;
      END IF;

      -- get degree(u) and degree(v)
      select count(*) into degree_u from graph_edges 
        where (rid1 = relRec.rid1 and rtid1 = relRec.rtid1) or (rid2 = relRec.rid1 and rtid2 = relRec.rtid1) and hidden = false;
      select count(*) into degree_v from graph_edges 
        where (rid1 = relRec.rid2 and rtid1 = relRec.rtid2) or (rid2 = relRec.rid2 and rtid2 = relRec.rtid2) and hidden = false;

      -- compute sigma value
      select 1/pi::real into sigma from rel_pi where rel_oid = relRec.oid;
      IF NOT FOUND THEN
        sigma := 0;
      END IF;

      -- now, compute the weight
      -- 0.001 -- base weight
      theWeight := (mu_sp_p + 0.5 * (mu_sc_u/degree_u + mu_sc_v/degree_v) + 0.5 * (mu_po_u/degree_u + mu_po_v/degree_v) + sigma + alpha) / 5;
      update rel_weight set weight = theWeight where rel_oid = relRec.oid;

    END LOOP;

    EXECUTE 'drop table tmp_height';
  END;
$$ LANGUAGE plpgsql;
