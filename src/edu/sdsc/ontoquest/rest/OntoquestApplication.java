package edu.sdsc.ontoquest.rest;

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbContext;

/**
 * @version $Id: OntoquestApplication.java,v 1.3 2013-08-03 05:32:22 jic002 Exp $
 *
 */
public class OntoquestApplication extends Application {

	private boolean initialized = false;
	protected static final String configFileName = "config/ontoquest.xml";
	protected Context context;

	private String kbName = null;
	private int kbId = -1;

	public OntoquestApplication() {
		super();
	}

	public OntoquestApplication(String kbName) {
		super();
		this.kbName = kbName;
	}

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public Restlet createRoot() {

		init();

		// Create a router Restlet that defines routes.
		Router router = new Router(getContext());

		// Defines a router for downloading or showing details of an ontology. {type} values: download, detail
		router.attach("/ontologies/{type}/{kbName}", OntologiesResource.class);
		// Defines a route for the resource "list of ontologies"
		router.attach("/ontologies", OntologiesResource.class);

		// Defines a route for the resource "class node"
		router.attach("/concepts/oid/{oid}", ClassNodeResource.class);
		router.attach("/concepts/{classId}", ClassNodeResource.class);
		router.attach("/concepts/term/{term}", ClassNodeResource.class);
		router.attach("/concepts/search/{query}", ClassNodeResource.class);
		router.attach("/concepts/siblings/{type}/oid/{oid}", ClassNodeResource.class);
		router.attach("/concepts/siblings/{type}/{classId}", ClassNodeResource.class);
		router.attach("/concepts/siblings/{type}/term/{term}", ClassNodeResource.class);


		// Defines a route for related concepts
		router.attach("/rel/{type}/{classId}", NeighborhoodResource.class);
		router.attach("/rel/{type}/oid/{oid}", NeighborhoodResource.class);
		router.attach("/rel/{type}/term/{term}", NeighborhoodResource.class);
	  router.attach("/rel/{type}/id/{id}", NeighborhoodResource.class);

		// Deprecated services. For back compatibility only. Defines a route for related concepts
		router.attach("/graph/{type}/{classId}", DeprecatedNeighborhoodResource.class);
		router.attach("/graph/{type}/oid/{oid}", DeprecatedNeighborhoodResource.class);
		router.attach("/graph/{type}/term/{term}", DeprecatedNeighborhoodResource.class);

		// Defines a route for auto-complete term search
		router.attach("/keyword/{query}", TermResource.class);
		// classIds, oids, terms are separated by ';', e.g. sao-123456;birnlex-23467
		// If a term includes ;, use \ to escape, e.g. terms "abc", 'cde;ee' become
		// abc;cde\;ee
		router.attach("/keyword/{query}/anc/oids/{oids}", TermResource.class);
		router.attach("/keyword/{query}/anc/{classIds}", TermResource.class);
		router.attach("/keyword/{query}/anc/terms/{terms}", TermResource.class);

		// defines a route for Google Refine service
		router.attach("/reconcile",
				SearchReconcileResource.class);

		// Define a routine to get properties only
		router.attach("/getprop/oid/{oid}", SimpleClassNodeResource.class);
		router.attach("/getprop/{classId}", SimpleClassNodeResource.class);
		router.attach("/getprop/term/{term}", SimpleClassNodeResource.class);

		// Define NIF Card routines
		router.attach("/nifcard/{listType}", NIFCardResource.class);
		return router;
	}

	public int getKbId() {
		return kbId;
	}




	/**
	 * @return the ontoquest context
	 */
	public Context getOntoquestContext() {
		return context;
	}

	private synchronized void init() {
		if (initialized)
			return;

		try {
			ServletContext servletContext = null;
			String app_path = "./";

			Object v = getContext().getAttributes().get("org.restlet.ext.servlet.ServletContext");
			if (v != null) {
				servletContext = (ServletContext)v;

				File configFile = new File(configFileName);
				if (!configFile.exists()) {
					String realPath = servletContext.getRealPath("");
					if (!realPath.contains("ROOT"))
					{
						app_path = realPath.replace('\\', '/');
						if (!app_path.endsWith("/"))
							app_path = app_path.concat("/");      
					}

				}
			}

			// initialize configuration
			AllConfiguration.initialize(app_path + configFileName, app_path);
			// initialize database connection pool
			Configuration config = AllConfiguration.getConfig();
			String driver = config.getString("database.driver");
			String url = config.getString("database.url");
			String user = config.getString("database.user");
			String password = config.getString("database.password");
			DbConnectionPool conPool = new DbConnectionPool(driver, url, user, password);   
			context = new DbContext(conPool);

			// initialize Ontology Name
			if (servletContext != null) {
				kbName = servletContext.getInitParameter("OntologyName");
			}

			BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
			kbId = basicFunctions.getKnowledgeBaseID(kbName, context);

			initialized = true;

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
}
