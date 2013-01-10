
DROP TYPE IF EXISTS bm_bitvector CASCADE;

create type bm_bitvector as (
  nbits integer,
  nset integer,
  vec integer[],
  active_val integer,
  active_nbits integer  
);

DROP TYPE IF EXISTS bm_op CASCADE;

create type bm_op as enum ('And', 'Or', 'AndNot');

DROP TYPE IF EXISTS bm_join_type CASCADE;

CREATE TYPE bm_join_type as enum ('SS', 'SO', 'OO');

DROP TYPE IF EXISTS bm_id_type CASCADE;

CREATE TYPE bm_id_type as enum ('SO', 'P');

DROP TYPE IF EXISTS bm_run CASCADE;

create type bm_run as (
  idx integer,
  fill_word integer,
  nWords integer,
  fill boolean,
  vec integer[]
);


DROP AGGREGATE IF EXISTS bm_and(bm_bitvector) CASCADE;

DROP AGGREGATE IF EXISTS bm_or(bm_bitvector) CASCADE;

/*
select drop_if_exists('bm_soid');

select drop_if_exists('bm_pid');

select drop_if_exists('bm_ps');

select drop_if_exists('bm_po');

select drop_if_exists('bm_ssjoin');

select drop_if_exists('bm_sojoin');

select drop_if_exists('bm_oojoin');

create table BM_SOID (
  soid SERIAL,
  rid integer,
  rtid integer,
  kbid integer,
  constraint bm_soid_pk primary key (soid),
  constraint bm_soid_uk unique (rid, rtid)
);

create index bm_soid_kbid on BM_SOID (kbid);

create table BM_PID (
  pid SERIAL,
  rid integer,
  kbid integer,
  constraint bm_pid_pk primary key (pid),
  constraint bm_pid_uk unique (rid)
);

create index bm_pid_kbid on BM_PID (kbid);

create table BM_PS (
  pid integer,
  sid integer,
  kbid integer,
  bv bm_bitvector,
  constraint bm_ps_uk unique (pid, sid)
);

create index bm_ps_kbid on BM_PS (kbid);

create table BM_PO (
  pid integer,
  oid integer,
  kbid integer,
  bv bm_bitvector,
  constraint bm_po_uk unique (pid, oid)
);

create index bm_po_kbid on BM_PO (kbid);

create table bm_SSJOIN (
  pid1 integer,
  pid2 integer,
  kbid integer,
  bv bm_bitvector,
  constraint bm_ss_uk unique (pid1, pid2)
);

create index bm_ssjoin_kbid on BM_SSJOIN (kbid);

create table bm_SOJOIN (
  pid1 integer,
  pid2 integer,
  kbid integer,
  bv bm_bitvector,
  constraint bm_so_uk unique (pid1, pid2)
);

create index bm_sojoin_kbid on BM_SOJOIN (kbid);

create table bm_OOJOIN (
  pid1 integer,
  pid2 integer,
  kbid integer,
  bv bm_bitvector,
  constraint bm_oo_uk unique (pid1, pid2)
);

create index bm_oojoin_kbid on BM_OOJOIN (kbid);

CREATE OR REPLACE FUNCTION bm_compress(bstring bit varying) returns bm_bitvector AS $$
-- compress a bit string to a bitvector using WAH scheme.
DECLARE
  size integer := 0;
  i integer := 0;
  bitVal bit(1);
  bmVec bm_bitvector;
BEGIN
  -- initialize an empty bm_bitvector
  bmVec := ROW(0,0,'{}', 0,0);
  
  size := bit_length(bstring);
  FOR i IN 1..size LOOP
    bitVal := substring(bstring, i, 1);
    IF bitVal = B'1' THEN
      bmVec := bm_setbit(bmVec, i, 1);  -- set position i in bm_bitvector
    END IF;
  END LOOP;
  return null;
END;
$$ LANGUAGE plpgsql;
*/

------------------------------------------------------------------
--   Functions implementing RDFJoin operators
------------------------------------------------------------------
CREATE OR REPLACE FUNCTION bm_ssjoin(prop1_rid integer, prop2_rid integer) returns bm_bitvector AS $$
DECLARE
  bmVec bm_bitvector;
BEGIN
  bmVec := bm_join(prop1_rid, prop2_rid, 'SS');
  return bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_sojoin(prop1_rid integer, prop2_rid integer) returns bm_bitvector AS $$
DECLARE
  bmVec bm_bitvector;
BEGIN
  bmVec := bm_join(prop1_rid, prop2_rid, 'SO');
  return bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_oojoin(prop1_rid integer, prop2_rid integer) returns bm_bitvector AS $$
DECLARE
  bmVec bm_bitvector;
BEGIN
  bmVec := bm_join(prop1_rid, prop2_rid, 'OO');
  return bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_join(prop1_rid integer, prop2_rid integer, join_type bm_join_type) returns bm_bitvector AS $$
DECLARE
  theKbid integer;
  prop1_cond text := '';
  prop1_tbl text := '';
  prop2_cond text := '';
  prop2_tbl text := '';
  sql text := '';
  rec RECORD;
  bmVec bm_bitvector;
BEGIN
  IF prop1_rid IS NOT NULL AND prop1_rid > 0 THEN
    select into theKbid kbid from property where id = prop1_rid;
  ELSEIF prop2_rid IS NOT NULL AND prop2_rid > 0 THEN
    select into theKbid kbid from property where id = prop2_rid;
  END IF;

  IF theKbid is null THEN
    raise exception 'In bm_join. Cannot find kbid for the input edge. prop1_rid = %, prop2_rid = %, join_type = %', prop1_rid, prop2_rid, join_type;
  END IF;

  IF prop1_rid IS NOT NULL AND prop1_rid > 0 THEN
    prop1_cond := 'p1.rid = '||prop1_rid||' and p1.pid = j.pid1';
    prop1_tbl := ', bm_pid_'||theKbid||' p1 ';
  END IF;

  IF prop2_rid IS NOT NULL AND prop2_rid > 0 THEN
    prop2_cond := 'p2.rid = '||prop2_rid||' and p2.pid = j.pid2';
    prop2_tbl := ', bm_pid_'||theKbid||' p2 ';
  END IF;

  sql := 'select bm_or(j.bv) as retVec from bm_'||join_type||'join_'||theKbid||' j '||prop1_tbl||prop2_tbl;
  IF prop1_cond != '' OR prop2_cond != '' THEN
    sql := sql ||' where ';
    IF prop1_cond != '' THEN
      sql := sql || prop1_cond;
      IF prop2_cond != '' THEN
        sql := sql ||' and ';
      END IF;
    END IF;

    IF prop2_cond != '' THEN
      sql := sql || prop2_cond;
    END IF;
  END IF;
raise notice 'SQL = %', sql;

  EXECUTE sql INTO rec;
  bmVec := rec.retVec;
  return bmVec;

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_ps_select(prop_rid integer, sub_rid integer, sub_rtid integer) returns bm_bitvector AS $$
DECLARE
  theKbid integer;
  prop_cond text := '';
  prop_tbl text := '';
  sub_cond text := '';
  sub_tbl text := '';
  sql text := '';
  rec RECORD;
  bmVec bm_bitvector;
BEGIN
  IF prop_rid IS NOT NULL AND prop_rid > 0 THEN
    select into theKbid kbid from property where id = prop_rid;
  ELSEIF sub_rid IS NOT NULL AND sub_rid > 0 and sub_rtid IS NOT NULL and sub_rtid > 0 THEN
    select into theKbid kbid from graph_nodes where rid = sub_rid and rtid = sub_rtid;
  END IF;

  IF theKbid is null THEN
    raise exception 'In bm_ps_select. Cannot find kbid for the input edge. prop_rid = %, sub_rid = %, sub_rtid = %', prop_rid, sub_rid, sub_rtid;
  END IF;

  IF prop_rid IS NOT NULL AND prop_rid > 0 THEN
    prop_cond := 'p.rid = '||prop_rid||' and p.pid = ps.pid';
    prop_tbl := ', bm_pid_'||theKbid||' p ';
  END IF;

  IF sub_rid IS NOT NULL AND sub_rid > 0 and sub_rtid IS NOT NULL and sub_rtid > 0 THEN
    sub_cond := 'so.rid = '||sub_rid||' and so.rtid = '||sub_rtid||' and so.soid = ps.sid';
    sub_tbl := ', bm_soid_'||theKbid||' so ';
  END IF;

  sql := 'select bm_or(ps.bv) as retVec from bm_ps_'||theKbid||' ps '||prop_tbl||sub_tbl;
  IF prop_cond != '' OR sub_cond != '' THEN
    sql := sql ||' where ';
    IF prop_cond != '' THEN
      sql := sql || prop_cond;
      IF sub_cond != '' THEN
        sql := sql ||' and ';
      END IF;
    END IF;

    IF sub_cond != '' THEN
      sql := sql || sub_cond;
    END IF;
  END IF;
--raise notice 'SQL = %', sql;

  EXECUTE sql INTO rec;
  bmVec := rec.retVec;
  return bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_po_select(prop_rid integer, obj_rid integer, obj_rtid integer) returns bm_bitvector AS $$
DECLARE
  theKbid integer;
  prop_cond text := '';
  prop_tbl text := '';
  obj_cond text := '';
  obj_tbl text := '';
  sql text := '';
  rec RECORD;
  bmVec bm_bitvector;
BEGIN
  IF prop_rid IS NOT NULL AND prop_rid > 0 THEN
    select into theKbid kbid from property where id = prop_rid;
  ELSEIF obj_rid IS NOT NULL AND obj_rid > 0 and obj_rtid IS NOT NULL and obj_rtid > 0 THEN
    select into theKbid kbid from graph_nodes where rid = obj_rid and rtid = obj_rtid;
  END IF;

  IF theKbid is null THEN
    raise exception 'In bm_po_select. Cannot find kbid for the input edge. prop_rid = %, obj_rid = %, obj_rtid = %', prop_rid, obj_rid, obj_rtid;
  END IF;


  IF prop_rid IS NOT NULL AND prop_rid > 0 THEN
    prop_cond := 'p.rid = '||prop_rid||' and p.pid = po.pid';
    prop_tbl := ', bm_pid_'||theKbid||' p ';
  END IF;

  IF obj_rid IS NOT NULL AND obj_rid > 0 and obj_rtid IS NOT NULL and obj_rtid > 0 THEN
    obj_cond := 'so.rid = '||obj_rid||' and so.rtid = '||obj_rtid||' and so.soid = po.oid';
    obj_tbl := ', bm_soid_'||theKbid||' so ';
  END IF;

  sql := 'select bm_or(po.bv) as retVec from bm_po_'||theKbid||' po '||prop_tbl||obj_tbl;
  IF prop_cond != '' OR obj_cond != '' THEN
    sql := sql ||' where ';
    IF prop_cond != '' THEN
      sql := sql || prop_cond;
      IF obj_cond != '' THEN
        sql := sql ||' and ';
      END IF;
    END IF;

    IF obj_cond != '' THEN
      sql := sql || obj_cond;
    END IF;
  END IF;
raise notice 'SQL = %', sql;

  EXECUTE sql INTO rec;
  bmVec := rec.retVec;
  return bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_so_select(sub_rid integer, sub_rtid integer, obj_rid integer, obj_rtid integer) returns bm_bitvector AS $$
DECLARE
  theKbid integer;
  obj_cond text := '';
  obj_tbl text := '';
  sub_cond text := '';
  sub_tbl text := '';
  sql text := '';
  rec RECORD;
  bmVec bm_bitvector;
BEGIN
  IF obj_rid IS NOT NULL AND obj_rid > 0 and obj_rtid IS NOT NULL and obj_rtid > 0  THEN
    select into theKbid kbid from graph_nodes where rid = obj_rid and rtid = obj_rtid;
  ELSEIF sub_rid IS NOT NULL AND sub_rid > 0 and sub_rtid IS NOT NULL and sub_rtid > 0 THEN
    select into theKbid kbid from graph_nodes where rid = sub_rid and rtid = sub_rtid;
  END IF;

  IF theKbid is null THEN
    raise exception 'In bm_so_select. Cannot find kbid for the input edge. obj_rid = %, obj_rtid = %, sub_rid = %, sub_rtid = %', obj_rid, obj_rtid, sub_rid, sub_rtid;
  END IF;

  IF obj_rid IS NOT NULL AND obj_rid > 0 and obj_rtid IS NOT NULL and obj_rtid > 0 THEN
    obj_cond := 'so2.rid = '||obj_rid||' and so2.rtid = '||obj_rtid||' and so2.soid = sot.oid';
    obj_tbl := ', bm_soid_'||theKbid||' so2 ';
  END IF;

  IF sub_rid IS NOT NULL AND sub_rid > 0 and sub_rtid IS NOT NULL and sub_rtid > 0 THEN
    sub_cond := 'so.rid = '||sub_rid||' and so.rtid = '||sub_rtid||' and so.soid = sot.sid';
    sub_tbl := ', bm_soid_'||theKbid||' so ';
  END IF;

  sql := 'select bm_or(sot.bv) as retVec from bm_so_'||theKbid||' sot '||obj_tbl||sub_tbl;
  IF obj_cond != '' OR sub_cond != '' THEN
    sql := sql ||' where ';
    IF obj_cond != '' THEN
      sql := sql || obj_cond;
      IF sub_cond != '' THEN
        sql := sql ||' and ';
      END IF;
    END IF;

    IF sub_cond != '' THEN
      sql := sql || sub_cond;
    END IF;
  END IF;
raise notice 'SQL = %', sql;

  EXECUTE sql INTO rec;
  bmVec := rec.retVec;
  return bmVec;
END;
$$ LANGUAGE plpgsql;

/*
  Given a compressed bit vector, return the node IDs (SOID in bm_SOID_X table) whose bits are 1 in the index
*/
CREATE OR REPLACE FUNCTION bm_idx2so_rid(bmVec bm_bitvector, kbid integer) returns setof node2 AS $$
DECLARE
  rec RECORD;
  sql text;
BEGIN
  sql := 'select rid, rtid from bm_soid_'||kbid||' so, bm_decompress('||bm_tochar(bmVec)||') v(val) where so.soid = v.val';
  FOR rec IN EXECUTE sql
  LOOP
    return next rec;
  END LOOP;
  RETURN;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_idx2prop_rid(bmVec bm_bitvector, kbid integer) returns setof node2 AS $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN EXECUTE 'select rid, rtid from bm_pid_'||kbid||' so, bm_decompress('||bm_tochar(bmVec)||') v(val) where so.soid = v.val'
  LOOP
    return next rec;
  END LOOP;
  RETURN;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_create_index(kbid integer) returns void AS $$
DECLARE
  rec RECORD;
  nodeSize integer;
  select_sql text;
  update_sql text;
  ps_select_template text := 'select distinct so.soid as nid from graph_edges e, bm_soid_'||kbid||' so, bm_soid_'||kbid
	||' s, bm_pid_'||kbid||' p where s.soid = :soid: and p.pid = :pid: and s.rid = e.rid1 and s.rtid = e.rtid1'
        ||' and p.rid = e.pid and so.rid = e.rid2 and so.rtid = e.rtid2 and e.hidden = false and e.is_obsolete = false'
        ||' order by nid';
  ps_update_template text := 'update bm_ps_'||kbid||' set bv = ? where sid = :soid: and pid = :pid:';
  po_select_template text := 'select distinct so.soid as nid from graph_edges e, bm_soid_'||kbid||' so, bm_soid_'||kbid
	||' o, bm_pid_'||kbid||' p where o.soid = :soid: and p.pid = :pid: and o.rid = e.rid2 and o.rtid = e.rtid2'
        ||' and p.rid = e.pid and so.rid = e.rid1 and so.rtid = e.rtid1 and e.hidden = false and e.is_obsolete = false'
        ||' order by nid';
  po_update_template text := 'update bm_po_'||kbid||' set bv = ? where oid = :soid: and pid = :pid:';
  so_select_template text := 'select distinct p.pid as nid from graph_edges e, bm_soid_'||kbid||' s, bm_soid_'||kbid
	||' o, bm_pid_'||kbid||' p where s.soid = :sid: and o.soid = :oid: and s.rid = e.rid1 and s.rtid = e.rtid1'
        ||' and p.rid = e.pid and o.rid = e.rid2 and o.rtid = e.rtid2 and e.hidden = false and e.is_obsolete = false'
        ||' order by nid';
  so_update_template text := 'update bm_so_'||kbid||' set bv = ? where sid = :sid: and oid = :oid:';
  ss_select_template text := 'select distinct so.soid as nid from graph_edges e1, graph_edges e2, bm_soid_'
        ||kbid||' so, bm_pid_'||kbid||' p1, bm_pid_'||kbid||' p2 where e1.rid1 = e2.rid1 and e1.rtid1 = e2.rtid1'
        ||' and e1.hidden = false and e2.hidden = false and e1.is_obsolete = false and e2.is_obsolete = false'
        ||' and e1.pid = p1.rid and e2.pid = p2.rid and so.rid = e1.rid1 and so.rtid = e1.rtid1 and'
        ||' p1.pid = :pid1: and p2.pid = :pid2: order by so.soid';
  ss_update_template text := 'update bm_ssjoin_'||kbid||' set bv = ? where pid1 = :pid1: and pid2 = :pid2:';
  soj_select_template text := 'select distinct so.soid as nid from graph_edges e1, graph_edges e2, bm_soid_'
        ||kbid||' so, bm_pid_'||kbid||' p1, bm_pid_'||kbid||' p2 where e1.rid1 = e2.rid2 and e1.rtid1 = e2.rtid2'
        ||' and e1.hidden = false and e2.hidden = false and e1.is_obsolete = false and e2.is_obsolete = false'
        ||' and e1.pid = p1.rid and e2.pid = p2.rid and so.rid = e1.rid1 and so.rtid = e1.rtid1 and'
        ||' p1.pid = :pid1: and p2.pid = :pid2: order by so.soid';
  soj_update_template text := 'update bm_sojoin_'||kbid||' set bv = ? where pid1 = :pid1: and pid2 = :pid2:';
  oo_select_template text := 'select distinct so.soid as nid from graph_edges e1, graph_edges e2, bm_soid_'
        ||kbid||' so, bm_pid_'||kbid||' p1, bm_pid_'||kbid||' p2 where e1.rid2 = e2.rid2 and e1.rtid2 = e2.rtid2'
        ||' and e1.hidden = false and e2.hidden = false and e1.is_obsolete = false and e2.is_obsolete = false'
        ||' and e1.pid = p1.rid and e2.pid = p2.rid and so.rid = e1.rid2 and so.rtid = e1.rtid2 and'
        ||' p1.pid = :pid1: and p2.pid = :pid2: order by so.soid';
  oo_update_template text := 'update bm_oojoin_'||kbid||' set bv = ? where pid1 = :pid1: and pid2 = :pid2:';
BEGIN
  -- create tables first.
  PERFORM bm_create_tables(kbid);

  -- populate SOID and PID tables.
  EXECUTE 'insert into BM_SOID_'||kbid||' (rid, rtid) select rid, rtid from graph_nodes where is_obsolete = false and kbid = '
   ||kbid|| 'order by rtid, rid';
  EXECUTE 'insert into BM_PID_'||kbid||' (rid) select rid from graph_nodes where is_obsolete = false and rtid = 15 and kbid = '
   ||kbid|| 'order by rid';

  -- get node size. This is also the size of bit vector
  EXECUTE 'SELECT count(*) from BM_SOID_'||kbid INTO nodeSize;

  -- populate pid and node id in the triple tables PS and PO.
  EXECUTE 'insert into BM_PS_'||kbid||' (pid, sid) select distinct p.pid, soid from graph_edges e, bm_soid_'||kbid
   ||' so, bm_pid_'||kbid||' p where hidden = false and is_obsolete = false and kbid = '||kbid
   ||' and so.rid = e.rid1 and so.rtid = e.rtid1 and p.rid = e.pid';
  EXECUTE 'insert into BM_PO_'||kbid||' (pid, oid) select distinct p.pid, soid from graph_edges e, bm_soid_'||kbid
   ||' so, bm_pid_'||kbid||' p where hidden = false and is_obsolete = false and kbid = '||kbid
   ||' and so.rid = e.rid2 and so.rtid = e.rtid2 and p.rid = e.pid';
  EXECUTE 'insert into BM_SO_'||kbid||' (sid, oid) select distinct s.soid, o.soid from graph_edges e, bm_soid_'||kbid
   ||' s, bm_soid_'||kbid||' o where hidden = false and is_obsolete = false and kbid = '||kbid
   ||' and s.rid = e.rid1 and s.rtid = e.rtid1 and o.rid = e.rid2 and o.rtid = e.rtid2';

  -- populate bitvector in the triple tables PS and PO.

  FOR rec IN EXECUTE 'select pid, sid from BM_PS_'||kbid
  LOOP
    select_sql := replace(ps_select_template, ':soid:', rec.sid::text);
    select_sql := replace(select_sql, ':pid:', rec.pid::text);
    update_sql := replace(ps_update_template, ':soid:', rec.sid::text);
    update_sql := replace(update_sql, ':pid:', rec.pid::text);
    PERFORM bm_fill_vec(select_sql, update_sql, nodeSize);
  END LOOP;

  FOR rec IN EXECUTE 'select pid, oid from BM_PO_'||kbid
  LOOP
    select_sql := replace(po_select_template, ':soid:', rec.oid::text);
    select_sql := replace(select_sql, ':pid:', rec.pid::text);
    update_sql := replace(po_update_template, ':soid:', rec.oid::text);
    update_sql := replace(update_sql, ':pid:', rec.pid::text);
    PERFORM bm_fill_vec(select_sql, update_sql, nodeSize);
  END LOOP;

  FOR rec IN EXECUTE 'select sid, oid from BM_SO_'||kbid
  LOOP
    select_sql := replace(so_select_template, ':sid:', rec.sid::text);
    select_sql := replace(select_sql, ':oid:', rec.oid::text);
    update_sql := replace(so_update_template, ':sid:', rec.sid::text);
    update_sql := replace(update_sql, ':oid:', rec.oid::text);
--raise notice 'select sql = %, update_sql = %', select_sql, update_sql;
    PERFORM bm_fill_vec(select_sql, update_sql, nodeSize);
  END LOOP;

  -- populate pid1 and pid2 in JOIN tables
  EXECUTE 'insert into BM_SSJOIN_'||kbid||' (pid1, pid2) select distinct p1.pid as pid1, p2.pid as pid2'
  ||' from graph_edges e1, graph_edges e2, BM_PID_'||kbid||' p1, BM_PID_'||kbid||' p2 where e1.rid1 = e2.rid1'
  ||' and e1.rtid1 = e2.rtid1 and e1.pid != e2.pid and e1.hidden = false and e2.hidden = false and'
  ||' e1.is_obsolete = false and e2.is_obsolete = false and p1.rid = e1.pid and p2.rid = e2.pid and e1.kbid = '
  ||kbid||' order by pid1, pid2';
  EXECUTE 'insert into BM_SOJOIN_'||kbid||' (pid1, pid2) select distinct p1.pid as pid1, p2.pid as pid2'
  ||' from graph_edges e1, graph_edges e2, BM_PID_'||kbid||' p1, BM_PID_'||kbid||' p2 where e1.rid1 = e2.rid2'
  ||' and e1.rtid1 = e2.rtid2 and e1.pid != e2.pid and e1.hidden = false and e2.hidden = false and'
  ||' e1.is_obsolete = false and e2.is_obsolete = false and p1.rid = e1.pid and p2.rid = e2.pid and e1.kbid = '
  ||kbid||' order by pid1, pid2';
  EXECUTE 'insert into BM_OOJOIN_'||kbid||' (pid1, pid2) select distinct p1.pid as pid1, p2.pid as pid2'
  ||' from graph_edges e1, graph_edges e2, BM_PID_'||kbid||' p1, BM_PID_'||kbid||' p2 where e1.rid2 = e2.rid2'
  ||' and e1.rtid2 = e2.rtid2 and e1.pid != e2.pid and e1.hidden = false and e2.hidden = false and'
  ||' e1.is_obsolete = false and e2.is_obsolete = false and p1.rid = e1.pid and p2.rid = e2.pid and e1.kbid = '
  ||kbid||' order by pid1, pid2';

  -- populate bitvector in the join tables SSJOIN, SOJOIN, and OOJOIN.
  FOR rec IN EXECUTE 'select pid1, pid2 from BM_SSJOIN_'||kbid
  LOOP
    select_sql := replace(ss_select_template, ':pid1:', rec.pid1::text);
    select_sql := replace(select_sql, ':pid2:', rec.pid2::text);
    update_sql := replace(ss_update_template, ':pid1:', rec.pid1::text);
    update_sql := replace(update_sql, ':pid2:', rec.pid2::text);
--raise notice 'select sql = %, update_sql = %', select_sql, update_sql;
    PERFORM bm_fill_vec(select_sql, update_sql, nodeSize);
  END LOOP;

  FOR rec IN EXECUTE 'select pid1, pid2 from BM_SOJOIN_'||kbid
  LOOP
    select_sql := replace(soj_select_template, ':pid1:', rec.pid1::text);
    select_sql := replace(select_sql, ':pid2:', rec.pid2::text);
    update_sql := replace(soj_update_template, ':pid1:', rec.pid1::text);
    update_sql := replace(update_sql, ':pid2:', rec.pid2::text);
--raise notice 'select sql = %, update_sql = %', select_sql, update_sql;
    PERFORM bm_fill_vec(select_sql, update_sql, nodeSize);
  END LOOP;

  FOR rec IN EXECUTE 'select pid1, pid2 from BM_OOJOIN_'||kbid
  LOOP
    select_sql := replace(oo_select_template, ':pid1:', rec.pid1::text);
    select_sql := replace(select_sql, ':pid2:', rec.pid2::text);
    update_sql := replace(oo_update_template, ':pid1:', rec.pid1::text);
    update_sql := replace(update_sql, ':pid2:', rec.pid2::text);
--raise notice 'select sql = %, update_sql = %', select_sql, update_sql;
    PERFORM bm_fill_vec(select_sql, update_sql, nodeSize);
  END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_create_tables(kbid integer) returns void AS $$
-- create index and id tables for the input knowlodge base identified by kbid.
DECLARE
  soid_tbl_name VARCHAR(32) := 'BM_SOID_'||kbid;
  pid_tbl_name VARCHAR(32) := 'BM_PID_'||kbid;
  ps_tbl_name VARCHAR(32) := 'BM_PS_'||kbid;
  po_tbl_name VARCHAR(32) := 'BM_PO_'||kbid;
  so_tbl_name VARCHAR(32) := 'BM_SO_'||kbid;
  ssjoin_tbl_name VARCHAR(32) := 'BM_SSJOIN_'||kbid;
  sojoin_tbl_name VARCHAR(32) := 'BM_SOJOIN_'||kbid;
  oojoin_tbl_name VARCHAR(32) := 'BM_OOJOIN_'||kbid;
BEGIN
  EXECUTE 'drop table if exists '||soid_tbl_name||' cascade ';
  EXECUTE 'drop table if exists '||pid_tbl_name||' cascade ';
  EXECUTE 'drop table if exists '||ps_tbl_name||' cascade ';
  EXECUTE 'drop table if exists '||po_tbl_name||' cascade ';
  EXECUTE 'drop table if exists '||so_tbl_name||' cascade ';
  EXECUTE 'drop table if exists '||ssjoin_tbl_name||' cascade ';
  EXECUTE 'drop table if exists '||sojoin_tbl_name||' cascade ';
  EXECUTE 'drop table if exists '||oojoin_tbl_name||' cascade ';

  EXECUTE 'create table '||soid_tbl_name||' (soid serial PRIMARY KEY, rid integer, rtid integer, UNIQUE(rid, rtid))';
  EXECUTE 'create table '||pid_tbl_name||' (pid serial PRIMARY KEY, rid integer, UNIQUE(rid))';
  EXECUTE 'create table '||ps_tbl_name||' (pid integer REFERENCES '||pid_tbl_name||', sid integer REFERENCES '||soid_tbl_name
           ||', bv bm_bitvector, UNIQUE(pid, sid))';
  EXECUTE 'create table '||po_tbl_name||' (pid integer REFERENCES '||pid_tbl_name||', oid integer REFERENCES '||soid_tbl_name
           ||', bv bm_bitvector, UNIQUE(pid, oid))';
  EXECUTE 'create table '||so_tbl_name||' (sid integer REFERENCES '||soid_tbl_name||', oid integer REFERENCES '||soid_tbl_name
           ||', bv bm_bitvector, UNIQUE(sid, oid))';
  EXECUTE 'create table '||ssjoin_tbl_name||' (pid1 integer REFERENCES '||pid_tbl_name||', pid2 integer REFERENCES '||pid_tbl_name
           ||', bv bm_bitvector, UNIQUE(pid1, pid2))';
  EXECUTE 'create table '||sojoin_tbl_name||' (pid1 integer REFERENCES '||pid_tbl_name||', pid2 integer REFERENCES '||pid_tbl_name
           ||', bv bm_bitvector, UNIQUE(pid1, pid2))';
  EXECUTE 'create table '||oojoin_tbl_name||' (pid1 integer REFERENCES '||pid_tbl_name||', pid2 integer REFERENCES '||pid_tbl_name
           ||', bv bm_bitvector, UNIQUE(pid1, pid2))';

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_fill_vec (selectSQL text, updateSQL text, vecSize integer) RETURNS VOID AS $$
/* fill a bitvector of a row in triple or join table.
   @param selectSQL: the SQL to select positive node IDs. The bits corresponding to those nodes will be set to 1 in the bit vector.
   The SQL must project only one column named as nid.
   @param updateSQL: the SQL to update the row after the bitvector is created. It should contain a question mark (?) that will 
   be replaced by the bit vector.
   @param vecSize: the expected bit size in the bit vector
*/
DECLARE
  rec RECORD;
  bmVec bm_bitvector;
  last_nid integer;
  bvStr text;
BEGIN
  bmVec := ROW(0,0,'{}', 0, 0);
  FOR rec IN EXECUTE selectSQL LOOP
    bmVec := bm_setbit(bmVec, rec.nid-1, 1);
    last_nid := rec.nid;
  END LOOP;

  -- If the last_nid is not equal to the vector size, append 0 in the vector to ensure the size is correct. 
  IF last_nid < vecSize THEN
    bmVec := bm_setbit(bmVec, vecSize-1, 0);
  ELSEIF last_nid > vecSize THEN
    raise notice 'WARNING! The bitvector size is smaller than expected. vector size = %, but encounter node with id = %', vecSize, last_nid;
  END IF;

  bmVec := bm_doCount(bmVec);
  -- update the bitvector
  bvStr := 'ROW('||bmVec.nbits||','||bmVec.nset||',ARRAY['||array_to_string(bmVec.vec, ',')||'],'||bmVec.active_val||','||bmVec.active_nbits||')';
--raise notice 'FFFFFFFFF bvStr = %', bvStr;
  EXECUTE replace(updateSQL, '?', bvStr);
END;
$$ LANGUAGE plpgsql;

--------------------------------------------------------------------
-- BIT_VECTOR related stuff (WAH compression, bit operator, etc)
--------------------------------------------------------------------
CREATE OR REPLACE FUNCTION bm_op_and(bmVec1 bm_bitvector, bmVec2 bm_bitvector) RETURNS bm_bitvector AS $$
DECLARE
  nbits1 integer;
  nbits2 integer;
  bmVec bm_bitvector;
BEGIN
  nbits1 := bm_numBits(bmVec1);
  nbits2 := bm_numBits(bmVec2);
  IF (nbits1 IS NULL OR nbits1 = 0) AND (nbits2 IS NULL OR nbits2 = 0) THEN
    bmVec := ROW(0,0,'{}',0,0);
    RETURN bmVec;
  ELSEIF nbits1 IS NULL OR nbits1 = 0 THEN  -- empty bmVec1
    RETURN bmVec2;
  ELSEIF nbits2 IS NULL OR nbits2 = 0 THEN -- empty bmVec2
    RETURN bmVec1;
  END IF;

  bmVec := bm_generic_op(bmVec1, bmVec2, 'And');
  return bmVec;
END;
$$LANGUAGE plpgsql;

CREATE AGGREGATE bm_and (bm_bitvector) (
  SFUNC=bm_op_and,
  STYPE=bm_bitvector
);

CREATE OR REPLACE FUNCTION bm_op_or(bmVec1 bm_bitvector, bmVec2 bm_bitvector) RETURNS bm_bitvector AS $$
DECLARE
  nbits1 integer;
  nbits2 integer;
  bmVec bm_bitvector;
BEGIN
  nbits1 := bm_numBits(bmVec1);
  nbits2 := bm_numBits(bmVec2);
  IF (nbits1 IS NULL OR nbits1 = 0) AND (nbits2 IS NULL OR nbits2 = 0) THEN
    bmVec := ROW(0,0,'{}',0,0);
    RETURN bmVec;
  ELSEIF nbits1 IS NULL OR nbits1 = 0 THEN  -- empty bmVec1
    RETURN bmVec2;
  ELSEIF nbits2 IS NULL OR nbits2 = 0 THEN -- empty bmVec2
    RETURN bmVec1;
  END IF;

  bmVec := bm_generic_op(bmVec1, bmVec2, 'Or');
  return bmVec;
END;
$$LANGUAGE plpgsql;

CREATE AGGREGATE bm_or (bm_bitvector) (
  SFUNC=bm_op_or,
  STYPE=bm_bitvector
);

CREATE OR REPLACE FUNCTION bm_append_counter(bmVec bm_bitvector, val integer, cnt integer) returns bm_bitvector AS $$
DECLARE
  head integer;
  w integer;
  size integer;
  back integer;
BEGIN
  head := 2 + val;
  w := (head << 30) + cnt;
  
  bmVec.nbits := bmVec.nbits + cnt * 31;
  size := ARRAY_UPPER(bmVec.vec, 1) - ARRAY_LOWER(bmVec.vec, 1)+1;
  IF size is null OR size = 0 THEN  -- if1
    bmVec.vec := bmVec.vec || w;
  ELSE
    back := bmVec.vec[size];
    IF back::bit(32)>>30 = head::bit(32) THEN  -- if2 (use bit shift, not int shift)
      back = back + cnt;
      bmVec.vec[size] := back;
    ELSEIF back = 2147483647 AND head = 3 THEN
      bmVec.vec[size] := w + 1;
    ELSEIF back = 0 AND head = 2 THEN
      bmVec.vec[size] := w + 1;
    ELSE
      bmVec.vec := bmVec.vec || w;
    END IF;  -- end if2
  END IF;  -- end if1

  RETURN bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_append_fill(bmVec bm_bitvector, nWords integer, v integer) returns bm_bitvector AS $$
DECLARE
  back integer;
  size integer;
BEGIN
  size := ARRAY_UPPER(bmVec.vec, 1) - ARRAY_LOWER(bmVec.vec, 1)+1;
  IF size is null OR size = 0 THEN  -- if1, empty vec
    IF v = 0 THEN -- if2
      bmVec.vec := bmVec.vec || (x'80000000'::integer | nWords);
    ELSE -- else2
      bmVec.vec := bmVec.vec || (x'C0000000'::integer | nWords);
    END IF; -- endif2
  ELSEIF nWords > 1 THEN -- elseif1
    back := bmVec.vec[size];
    IF v = 0 THEN -- if3
      IF bm_isZeroFill32(back) THEN -- if4
        bmVec.vec[size] := back + nWords;
      ELSE -- else4
        bmVec.vec := bmVec.vec || (x'80000000'::integer | nWords);
      END IF; -- endif4
    ELSEIF bm_isOneFill32(back) THEN -- elseif3
      bmVec.vec[size] := back + nWords;
    ELSE -- else3
      bmVec.vec := bmVec.vec || (x'C0000000'::integer | nWords);
    END IF; -- endif3
  ELSE -- else1
    IF v != 0 THEN
      bmVec.active_val := 2147483647;
    ELSE
      bmVec.active_val := 0;
    END IF;
    bmVec := bm_append_literal(bmVec);
  END IF; --endif1
  RETURN bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_append_literal(bmVec bm_bitvector) returns bm_bitvector AS $$
DECLARE
  back integer;
  size integer;
BEGIN
  size := ARRAY_UPPER(bmVec.vec, 1) - ARRAY_LOWER(bmVec.vec, 1) + 1;
--raise notice 'IN append_literal. size = %, active_val = %', size, bmVec.active_val;
  IF size is null OR size = 0 THEN -- if1
    bmVec.vec := bmVec.vec || bmVec.active_val;
  ELSE -- else1
    back := bmVec.vec[size];
    
    IF bmVec.active_val = 0 THEN -- if2
      IF back = 0 THEN  -- if3
        bmVec.vec[size] := -2147483646;
      ELSEIF bm_isZeroFill32(back) THEN
        back := back + 1;
        bmVec.vec[size] := back;
      ELSE
        bmVec.vec := bmVec.vec || bmVec.active_val;
      END IF;  -- end if3
    ELSEIF bmVec.active_val = 2147483647 THEN  -- elseif2
      IF back = 2147483647 THEN -- if4
        bmVec.vec[size] := -1073741822;
      ELSEIF bm_isOneFill32(back) THEN 
        back := back + 1;
        bmVec.vec[size] := back;
      ELSE
        bmVec.vec := bmVec.vec || bmVec.active_val;
      END IF; -- end if4
    ELSE
        bmVec.vec := bmVec.vec || bmVec.active_val;
    END IF; -- end if2
    
  END IF; -- end if1
  
  bmVec.nbits := bmVec.nbits + 31;
  bmVec.active_val := 0;
  bmVec.active_nbits := 0;
  bmVec.nset := 0;
--  raise notice 'DDDDDDDDDDD nset = %, nbits = %, numBits = %, vec = %, active_val = %, active_nbits = %', bmVec.nset, bmVec.nbits, bm_numBits(bmVec), bmVec.vec, bmVec.active_val, bmVec.active_nbits;
  
  RETURN bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_constants(OUT MAXBITS int, OUT SECONDBIT int, OUT ALLONES int, OUT MAXCNT int,
     OUT FILLBIT int, OUT HEADER0 int, OUT HEADER1 int) RETURNS record AS $$
BEGIN
  MAXBITS := 31;
  SECONDBIT := 30;
  ALLONES := 2147483647;
  MAXCNT := 1073741823;
  FILLBIT := 1073741824;
  HEADER0 := -2147483648;
  HEADER1 := -1073741824;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_decompress(bmVec bm_bitvector) RETURNS SETOF integer AS $$
  -- decompress a bit vector, and return uncompressed bit string
DECLARE
  vecSize integer;
  nbits integer;
  bmVec2 bm_bitvector;
--  results integer[] := '{0}';
--  res_cnt integer := 0;
  idx integer := 0;
  i integer;
  j integer;
  v integer;
  cnt integer;
  tmpBitStr bit(32);
  
  constants RECORD;
BEGIN
  -- get constants
  constants := bm_constants();
  
  bmVec2 := bmVec;
  vecSize := ARRAY_UPPER(bmVec2.vec, 1) - ARRAY_LOWER(bmVec2.vec, 1)+1;
  IF bmVec2.nbits = 0 AND vecSize > 0 THEN
    bmVec2 := bm_doCount(bmVec);
    nbits := bmVec2.nbits;
  END IF;
  
  FOR i IN ARRAY_LOWER(bmVec2.vec, 1)..ARRAY_UPPER(bmVec2.vec, 1) LOOP -- loop1
    v := bmVec2.vec[i];
    IF bm_isAFill32(v) THEN -- if1, fill word 
      cnt := v & constants.MAXCNT; -- cnt tells us how many all one or all zero words in this fill word.
      IF bm_isOneFill32(v) THEN -- if2, 1-fill. 
        FOR j IN 1..cnt * constants.SECONDBIT LOOP -- loop2
--          res_cnt := res_cnt + 1;
          idx := idx + 1;
--          results[res_cnt] := idx;
          return next idx;
        END LOOP;  -- end loop2
      ELSE -- else2, 0-fill
        FOR j IN 1..cnt * constants.SECONDBIT LOOP -- loop3
          idx := idx + 1;
        END LOOP;  -- end loop3
      END IF; -- end if2
    ELSE -- literal
      tmpBitStr := v::bit(32);
      FOR j IN 2..constants.MAXBITS LOOP -- loop4, start from 2nd bit
        idx := idx + 1;
        IF substring(tmpBitStr, j, 1)::integer > 0 THEN -- if3, bit value = 1
--          res_cnt := res_cnt + 1;
--          results[res_cnt] := idx;
          return next idx;
        END IF;
      END LOOP; -- end loop4
    END IF; -- end if1
  END LOOP; -- end loop1
  
  -- decompress active word
  IF bmVec2.active_nbits > 0 THEN
    tmpBitStr := bmVec2.active_val::bit(32);
    j := (32-bmVec2.active_nbits+1);  -- initial bit in the active word (=word_size - nbits + 1)
    FOR i IN j..32 LOOP -- loop 5
      idx := idx + 1;
      IF substring(tmpBitStr, i, 1)::integer > 0 THEN -- if4, bit val = 1
--        res_cnt := res_cnt + 1;
--        results[res_cnt] := idx;
        return next idx;
      END IF;
    END LOOP; -- end loop5
  END IF;

  RETURN;  
--  RETURN results;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_doCount(bmVec bm_bitvector) RETURNS bm_bitvector AS $$
  -- return the number of bits counted and modify the member variable nset to
  -- the correct value. Return the updated bit vector back.
DECLARE
  i integer;
  v integer;
  tmp integer;
BEGIN
  bmVec.nset := 0;
  bmVec.nbits := 0;
  
  -- sanity check. If the bmVec is empty, return it.
  IF ARRAY_LOWER(bmVec.vec, 1) is null THEN
    RETURN bmVec;
  END IF;

  FOR i IN ARRAY_LOWER(bmVec.vec, 1)..ARRAY_UPPER(bmVec.vec, 1) LOOP
    v := bmVec.vec[i];
    IF not bm_isAFill32(v) THEN
      bmVec.nbits := bmVec.nbits + 31;
      bmVec.nset := bmVec.nset + bm_bitcount(v);
    ELSE
      tmp := (v & x'3FFFFFFF'::integer) * 31;
      bmVec.nbits := bmVec.nbits + tmp;
      IF bm_isOneFill32(v) THEN
        bmVec.nset := bmVec.nset + tmp;
      END IF;
    END IF;
  END LOOP;
  
  return bmVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_generic_op(bmVecA bm_bitvector, bmVecB bm_bitvector, opType bm_op) RETURNS bm_bitvector AS $$
DECLARE
  bmVec1 bm_bitvector;
  bmVec2 bm_bitvector;
  retVec bm_bitvector;
  numBits1 integer;
  numBits2 integer;
  xrun bm_run;
  yrun bm_run;
  size1 integer;
  size2 integer;
  nWords integer;
BEGIN
  -- initialize an empty result bitvector
  retVec := ROW(0,0, '{}', 0,0);

  bmVec1 := bm_doCount(bmVecA);
  bmVec2 := bm_doCount(bmVecB);
  numBits1 := bm_numBits(bmVec1);
  numBits2 := bm_numBits(bmVec2);

  -- make sure two bitvector have the same size. 
  IF numBits1 < numBits2 THEN
    bmVec1 := bm_setbit(bmVec1, numBits2-1, 0);
  ELSEIF numBits1 > numBits2 THEN
    bmVec2 := bm_setbit(bmVec2, numBits1-1, 0);
  END IF;

  size1 := ARRAY_UPPER(bmVec1.vec, 1) - ARRAY_LOWER(bmVec1.vec, 1)+1;
  size2 := ARRAY_UPPER(bmVec2.vec, 1) - ARRAY_LOWER(bmVec2.vec, 1)+1;
--raise notice 'AAAAAAAAAAA size1=%, size2=%', size1, size2;
  IF size1 is not null AND size1 > 0 THEN  -- if1
    xrun := ROW(0, 0, 0, false, bmVec1.vec);
    yrun := ROW(0, 0, 0, false, bmVec2.vec);
    xrun := bm_run_decode(xrun);
    yrun := bm_run_decode(yrun);

    WHILE xrun.idx < size1-1 OR yrun.idx < size2-1 LOOP
      IF xrun.nWords = 0 THEN -- if2
        xrun := bm_run_increment(xrun);
        xrun := bm_run_decode(xrun);
      END IF; -- endif2

      IF yrun.nWords = 0 THEN -- if3
        yrun := bm_run_increment(yrun);
        yrun := bm_run_decode(yrun);
      END IF; -- endif3
--raise notice 'BBBBBB xrun.nWords=%, xrun.fill_word=%, xrun.idx=%', xrun.nWords, xrun.fill_word, xrun.idx;
--raise notice 'BBBBBB yrun.nWords=%, yrun.fill_word=%, yrun.idx=%', yrun.nWords, yrun.fill_word, yrun.idx;
      IF xrun.fill THEN -- if4
        IF yrun.fill THEN -- if5
          nWords := xrun.nWords;  -- nWords = min(xrun.nWords, yrun.nWords)
          IF nWords > yrun.nWords THEN
            nWords := yrun.nWords;
          END IF;
          retVec := bm_append_fill(retVec, nWords, bm_get_op_result(opType, xrun.fill_word, yrun.fill_word));
          xrun.nWords := xrun.nWords - nWords;
          yrun.nWords := yrun.nWords - nWords;
        ELSE -- else5
          retVec.active_val := bm_get_op_result(opType, xrun.fill_word, yrun.vec[yrun.idx+1]);
          retVec := bm_append_literal(retVec);
          xrun.nWords := xrun.nWords - 1;
          yrun.nWords := 0;
        END IF; -- endif5
      ELSEIF yrun.fill THEN -- elseif4
        retVec.active_val := bm_get_op_result(opType, yrun.fill_word, xrun.vec[xrun.idx+1]);
        retVec := bm_append_literal(retVec);
        yrun.nWords := yrun.nWords - 1;
        xrun.nWords := 0;
      ELSE
        retVec.active_val := bm_get_op_result(opType, xrun.vec[xrun.idx+1], yrun.vec[yrun.idx+1]);
        retVec := bm_append_literal(retVec);
        yrun.nWords := 0;
        xrun.nWords := 0;
      END IF; -- endif4
--  raise notice 'GGGGGGGGG nset = %, nbits = %, numBits = %, vec = %, active_val = %, active_nbits = %', retVec.nset, retVec.nbits, bm_numBits(retVec), retVec.vec, retVec.active_val, retVec.active_nbits;
    END LOOP;
  END IF; -- endif1
  retVec.active_val := bm_get_op_result(opType, bmVec1.active_val, bmVec2.active_val);
  retVec.active_nbits := bmVec1.active_nbits;
    
  retVec := bm_doCount(retVec);
  return retVec;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_get_op_result(opType bm_op, a integer, b integer) RETURNS integer AS $$
DECLARE
BEGIN
  IF opType = 'And' THEN
    return (a & b);
  ELSEIF opType = 'AndNot' THEN
    return (a & (b::bit(32) # x'FFFFFFFF')::integer);
  ELSEIF opType = 'Or' THEN
    return (a | b);
  ELSE
    raise exception 'Unsupported bit operator type (bm_op): %', opType::text;
  END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_numBits(bmVec bm_bitvector) RETURNS integer AS $$
  -- count the total number of bits in the bit vector bmVec.
DECLARE
BEGIN
  return bmVec.nbits + bmVec.active_nbits;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_run_decode(bmRun bm_run) RETURNS bm_run AS $$
  -- decode the word at current position in bmRun. Return the decoded run back.
DECLARE
  v integer;
BEGIN
  v := bmRun.vec[bmRun.idx+1];
  IF (v)::bit(32) > x'7FFFFFFF' THEN -- if1; if a word is bitwise greater than ALLONES, it must be a fill word
    IF (v)::bit(32) > x'C0000000' THEN -- if2; one-fill word
      bmRun.fill_word := x'7FFFFFFF'::integer; 
    ELSE -- else2; zero-fill
      bmRun.fill_word := 0;
    END IF; -- endif2
    bmRun.nWords := v & x'3FFFFFFF'::integer;
    bmRun.fill := true;
  ELSE -- else1; it is a literal
    bmRun.nWords := 1;
    bmRun.fill := false;
  END IF;
  return bmRun;
END;
$$ LANGUAGE plpgsql;

/*
CREATE OR REPLACE FUNCTION bm_run_end(bmRun bm_run) RETURNS boolean AS $$
DECLARE
  size integer;
BEGIN
  size := ARRAY_UPPER(bmVec.vec, 1) - ARRAY_LOWER(bmVec.vec, 1)+1;
  IF size is null OR size = 0 THEN  -- if1
    RETURN true;
  END IF;
  return bmRun.idx >= size - 1;
END;
$$ LANGUAGE plpgsql;
*/

CREATE OR REPLACE FUNCTION bm_run_increment(bmRun bm_run) RETURNS bm_run AS $$
DECLARE
BEGIN
  bmRun.idx := bmRun.idx + 1;
  return bmRun;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_setbit(bmVec bm_bitvector, ind integer, val integer) RETURNS bm_bitvector AS $$
/* set the bit at position ind in bitvector bmVec to val.
   @param bmVec: the bitvector to be updated.
   @param ind: the position in bmVec. zero-based.
   @param val: bit value, either 1 or 0.
*/
DECLARE
  bmVec2 bm_bitvector;
  nbits integer;
  diff integer;
  w integer;
  u integer;
  i integer;
  size integer;
BEGIN
  IF val != 0 AND val != 1 THEN
    raise exception 'Invalid bit value: %. Expecting 1 or 0 only.', val;
  END IF;
  
  bmVec2 := bm_doCount(bmVec);
  nbits := bm_numBits(bmVec2);

--  raise notice 'Before setting. ind = %, val = %, nset = %, nbits = %, numBits = %, vec = %, active_val = %, active_nbits = %', ind, val, bmVec2.nset, bmVec2.nbits, nbits, bmVec2.vec, bmVec2.active_val, bmVec2.active_nbits;
  
  IF ind >= nbits THEN  -- if1, append a new bit at ind
    diff := ind - nbits + 1;
--raise notice 'AAAAAA diff = %', diff;
    IF bmVec2.active_nbits > 0 THEN -- if2
      IF ind + 1 >= bmVec2.nbits + 31 THEN -- if3
        diff := diff - (31 - bmVec2.active_nbits);
        bmVec2.active_val := bmVec2.active_val << (31 - bmVec2.active_nbits);
        IF diff = 0 AND val != 0 THEN -- if4
          bmVec2.active_val := bmVec2.active_val + 1;
        END IF;
        bmVec2 := bm_append_literal(bmVec2);
      ELSE -- if3
        bmVec2.active_nbits := bmVec2.active_nbits + diff;
        bmVec2.active_val := bmVec2.active_val << diff;
        IF val != 0 THEN
          bmVec2.active_val := bmVec2.active_val + 1;
        END IF;
        diff := 0;
      END IF; -- end if3
    END IF; -- end if2
    
    IF diff != 0 THEN -- if5
      w := diff/31;
      diff := diff - w * 31;
--raise notice 'GGGGGGGGGG. w = %, diff = %', w, diff;
      IF diff != 0 THEN -- if6
        IF w > 1 THEN -- if7
          bmVec2 := bm_append_counter(bmVec2, 0, w);
        ELSEIF w != 0 THEN
          bmVec2 := bm_append_literal(bmVec2);
        END IF; -- end if7
        bmVec2.active_nbits := diff;
        IF val != 0 THEN
          bmVec2.active_val := bmVec2.active_val + 1;
        END IF;
      ELSEIF val != 0 THEN -- elseif6
        IF w > 2 THEN -- if8
          bmVec2 := bm_append_counter(bmVec2, 0, w-1);
        ELSEIF w = 2 THEN -- elseif8
          bmVec2 := bm_append_literal(bmVec2);
        END IF; -- end if8
        bmVec2.active_val := 1;
        bmVec2 := bm_append_literal(bmVec2);
      ELSEIF w > 1 THEN
        bmVec2 := bm_append_counter(bmVec2, 0, w);
      ELSEIF w != 0 THEN
        bmVec2 := bm_append_literal(bmVec2);
      END IF; -- end if6      
    END IF; -- end if5
    
    bmVec2 := bm_doCount(bmVec2);
    nbits := bm_numBits(bmVec2);

    IF nbits != ind + 1 THEN
      raise notice 'Warning! nbits != ind + 1. nbits=%, ind=%', nbits, ind;
    END IF;
    
    IF bmVec2.nset != 0 AND val != 0 THEN
      bmVec2.nset := bmVec2.nset + 1;
    END IF;
    RETURN bmVec2;
  END IF; -- end if1
  
  IF ind >= bmVec2.nbits THEN -- if11  (modify an active bit)
    u := bmVec2.active_val;
    IF val != 0 THEN -- if12
      bmVec2.active_val := bmVec2.active_val | (1<<bmVec2.active_nbits - (ind-bmVec2.nbits)-1);
    ELSE -- else12
      bmVec2.active_val := bmVec2.active_val & (1<<bmVec2.active_nbits - (ind-bmVec2.nbits)-(1::bit(32)#x'FFFFFFFF')::integer);
    END IF; -- end if12
    
    IF bmVec2.nset != 0 AND u != bmVec2.active_val THEN -- if12
      IF val != 0 THEN
        bmVec2.nset = bmVec2.nset + 1;      
      ELSE
        bmVec2.nset = bmVec2.nset - 1;      
      END IF;    
    END IF; -- end if12
    
    RETURN bmVec2;
  END IF; -- end if11
  
  size := ARRAY_UPPER(bmVec2.vec, 1)-ARRAY_LOWER(bmVec2.vec, 1)+1;
  IF size*31 = bmVec2.nbits THEN -- if13
    i := ind / 31;
    u := bmVec2.vec[i+1];
    w := 1 << 30 - (ind % 31);
    
    IF val != 0 THEN  -- if14
      u := u | w;
    ELSE
      u := u & (w::bit(32) # x'FFFFFFFF')::integer;
    END IF; -- end if14
    bmVec2.vec[i+1] := u;
    
    IF bmVec2.nset != 0 AND bmVec2.vec[i+1] != u THEN  -- if15
      IF val != 0 THEN
        bmVec2.nset := bmVec2.nset + 1;
      ELSE
        bmVec2.nset := bmVec2.nset - 1;
      END IF;
    END IF;  -- end if15
    
    RETURN bmVec2;
  END IF; -- end if13
  
  RAISE EXCEPTION 'Untested code detected. Would rather die!';
  
END;
$$ LANGUAGE plpgsql;

-------------- WAH Auxiliary Functions ---------------
---------------------------------------------------
CREATE OR REPLACE FUNCTION bm_isAFill32(v integer) RETURNS boolean AS $$
  DECLARE
  BEGIN
    RETURN v::bit(32) & x'80000000' = x'80000000';
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_isZeroFill32(v integer) RETURNS boolean AS $$
  DECLARE
  BEGIN
    RETURN v::bit(32) & x'C0000000' = x'80000000';
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_isOneFill32(v integer) RETURNS boolean AS $$
  DECLARE
  BEGIN
    RETURN v::bit(32) & x'C0000000' = x'C0000000';
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_tochar(bmVec bm_bitvector) RETURNS text AS $$
  DECLARE
    result text ;
  BEGIN
    result := 'ROW('||bmVec.nbits||','||bmVec.nset||',';
    result := result||'''{'||array_to_string(bmVec.vec, ',')||'}'','||bmVec.active_val||','||bmVec.active_nbits||')';
raise notice 'string representation: %', result;
    return result;
  END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION bm_bitcount(i integer) RETURNS integer AS $$
DECLARE n integer;
DECLARE amount integer;
  BEGIN
    amount := 0;
    FOR n IN 1..32 LOOP
      amount := amount + ((i >> (n-1)) & 1);
    END LOOP;
    RETURN amount;
  END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION bm_test() RETURNS VOID AS $$
  -- test bm functions
DECLARE
  obj bm_bitvector;
  bmVec bm_bitvector;
  bmVec2 bm_bitvector;
  rec1 RECORD;
  rec2 RECORD;
  i integer;
BEGIN
--  obj := row(3, 0, '{3,5}', 2, 5);
--  raise notice 'obj.nbits = %, obj.vec = %', obj.nbits, obj.vec;
  
--  bmVec := ROW(0,0, '{1073742720, -2147483646, 2097151}', 15, 4);
/*
  bmVec := ROW(0,0,'{}', 0, 0);
  bmVec := bm_doCount(bmVec);
  raise notice 'nset = %, nbits = %, numBits = %, vec = %, active_val = %, active_nbits = %', bmVec.nset, bmVec.nbits, bm_numBits(bmVec), bmVec.vec, bmVec.active_val, bmVec.active_nbits;

  bmVec := bm_setbit(bmVec, 0, 1);
  bmVec := bm_setbit(bmVec, 21, 1);
  bmVec := bm_setbit(bmVec, 22, 1);
  bmVec := bm_setbit(bmVec, 23, 1);

  FOR i IN 0..24 LOOP
    bmVec := bm_setbit(bmVec, i+103, 1);
  END LOOP;

  FOR i IN 0..66 LOOP
    bmVec := bm_setbit(bmVec, i, 1);
  END LOOP; 
  FOR i IN 84..87 LOOP
    bmVec := bm_setbit(bmVec, i, 1);
  END LOOP;
  FOR i IN 94..102 LOOP
    bmVec := bm_setbit(bmVec, i, 1);
  END LOOP;
  FOR i IN 126..127 LOOP
    bmVec := bm_setbit(bmVec, i, 1);
  END LOOP;
*/
/*
  bmVec := bm_setbit(bmVec, 0, 1);
  FOR i IN 21..23 LOOP
    bmVec := bm_setbit(bmVec, i, 1);
  END LOOP; 
  FOR i IN 126..127 LOOP
    bmVec := bm_setbit(bmVec, i, 1);
  END LOOP; 
*/
   
--  bmVec := bm_doCount(bmVec);
  select into rec1 bv from bm_po_2 where pid = 35 and oid = 3;

  select into rec2 bv from bm_po_2 where pid = 35 and oid = 78;

  bmVec := bm_op_and(rec1.bv, rec2.bv);
--  bmVec := bm_generic_op(rec1.bv, rec2.bv, 'Or');
--  bmVec := bm_doCount(bmVec);

  raise notice 'nset = %, nbits = %, numBits = %, vec = %, active_val = %, active_nbits = %', bmVec.nset, bmVec.nbits, bm_numBits(bmVec), bmVec.vec, bmVec.active_val, bmVec.active_nbits;


END;
$$ LANGUAGE plpgsql;
