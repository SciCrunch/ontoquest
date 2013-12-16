ontoquest
=========

Ontoquest webservice

Reqs:
Apache ant to build the package.
Postgres database version 8 or newer.
Tomcat 6 or newer for the webapp server.

Create a UTF8 encoded database with plperl and pgperl language installed.
Run scripts/pgsql_schema.sql against the newly created databse.

Configuration:
Modify config/ontoquest.xml and fill in the following parameters:

url
user
password
ontology_name (pick a name for your ontology)

Modify WEB-INF/web.xml.  
Configure the OntologyName with the same name as whats configured in
config/ontoquest.xml

Build:
run ant -> this will create a war file in the dist directory that can be deployed.  
           It will also create a jar file that can be used as a library.

