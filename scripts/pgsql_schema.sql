-- the main, top-lavel script to create tables and functions. 

--create schema ont2 AUTHORIZATION ontoquest;
--alter user ontoquest SET search_path to ont2,public;

\i pgsql_tables.sql

\i createType.sql

\i basicFuns.sql

\i graph.sql

\i edit_distance.sql

\i createDAGTableNew.sql

\i computeDAGIndexNew.sql

\i dagFunsNew.sql

\i computeGoodnessScore.sql

\i bitmap.sql

\i fillTermCategory.sql
