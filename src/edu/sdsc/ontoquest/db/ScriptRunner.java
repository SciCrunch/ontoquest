package edu.sdsc.ontoquest.db;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.sdsc.ontoquest.AllConfiguration;


public class ScriptRunner {

	private static Log log = LogFactory.getLog(ScriptRunner.class);
	private static final String DEFAULT_CONFIG_FILE = "config/ontoquest.xml";

	public static void main(String[] args) throws Exception {
		try {
			if (args.length > 1) {
				System.out
				.println("Usage: java -cp <CLASSPATH> edu.sdsc.ontoquest.db.ScriptRunner [<property_file>]");
				System.exit(1);
			}

			String configFilePath = (args.length == 0) ? DEFAULT_CONFIG_FILE
					: args[0];
			AllConfiguration.initialize(configFilePath);
			// String propFileName = "OWLDbPlugin/scripts/ontoQuest.properties";
			// String propFileName = args[0];
			// Utility.getPresetProperties(new File(propFileName));
			// String dbType = Utility.getAppProperty(cls.getSimpleName() + ".dbType",
			// "PostgreSQL", cls);
			// String url = Utility.getAppProperty(cls.getSimpleName() + ".url", null,
			// cls);
			// if (url == null || url.length() == 0)
			// throw Utility
			// .generateMissingPropertyException(cls, "url", propFileName);
			// String user = Utility.getAppProperty(cls.getSimpleName() + ".user",
			// null,
			// cls);
			// if (user == null || user.length() == 0)
			// throw Utility.generateMissingPropertyException(cls, "user",
			// propFileName);
			// String password = Utility.getAppProperty(cls.getSimpleName()
			// + ".password", null, cls);
			// if (password == null || password.length() == 0)
			// throw Utility.generateMissingPropertyException(cls, "password",
			// propFileName);
			// String driver = Utility.getAppProperty(cls.getSimpleName() + ".driver",
			// null, cls);
			// if (driver == null || driver.length() == 0)
			// throw Utility.generateMissingPropertyException(cls, "driver",
			// propFileName);
			// String owlFileName = Utility.getAppProperty(cls.getSimpleName()
			// + ".owlfile", null, cls);
			// if (owlFileName == null || owlFileName.length() == 0)
			// throw Utility.generateMissingPropertyException(cls, "owlfile",
			// propFileName);
			// String ontName = Utility.getAppProperty(cls.getSimpleName() +
			// ".ontName",
			// null, cls);
			// if (ontName == null || ontName.length() == 0)
			// throw Utility.generateMissingPropertyException(cls, "ontName",
			// propFileName);
			// // validate ontology name
			// boolean isValid = true;
			// for (int i = 0; i < ontName.length(); ++i) {
			// char c = ontName.charAt(i);
			// if (!Character.isJavaIdentifierPart(c) && (c != ' ')) {
			// isValid = false;
			// break;
			// }
			// }
			//
			// if (!isValid) {
			// throw new IllegalArgumentException(
			// "Invalid ontology name! The name can contain only letters, digits, "
			// +
			// "currency symbol('$'), \nconnecting punctuation character('_'), space(' '), and combining mark.");
			// }
			//
			// // print inputs
			// log.info("OWL File to load: " + owlFileName);
			// log.info("Ontology Name: " + ontName);
			// log.info("OntoQuest Database: " + url);

			// Properties dbProperties = Utility.loadDbProperties(dbType);

			long time1 = System.currentTimeMillis();
			// ScriptRunner sr = new ScriptRunner(dbType, driver, url, user, password,
			// ontName, dbProperties);
			ScriptRunner sr = new ScriptRunner(AllConfiguration.getConfig());
			sr.run();
			long time2 = System.currentTimeMillis();

			log.info("Time to run the script (ms): " + (time2 - time1));
		} catch (Throwable localThrowable) {
			log.fatal("Failed to run script! " + localThrowable.toString()
					+ ". Cause: " + localThrowable.getCause());
			String str1 = "";
			for (StackTraceElement str4 : localThrowable.getStackTrace())
				str1 = str1 + str4.toString() + "\n";
			log.fatal("Exception Trace : \n" + str1);
		}
	}

	private DbConnectionPool conPool = null;
	private String dbType = null;
	private String finalOntName = null;
	private String ontName = null; // ontology name
	// Properties dbProperties = null;
	private Configuration config = null;

  public static final String suffixOld = "_old";
  public static final String suffixTmp = "_tmp";

	// //!!!!!!! IMPORTANT! TO USE ONE CONNECTION TO EXECUTE ALL STATEMENTS!
	private Connection con = null;

	// public ScriptRunner(String dbType, String driver, String url, String user,
	// String password, String ontName, Properties dbProperties) {
	//
	// this.dbType = dbType;
	// this.finalOntName = ontName;
	// this.ontName = (ontName + "_tmp");
	// this.dbProperties = dbProperties;
	// conPool = new DbConnectionPool(driver, url, user, password);
	// }

	public ScriptRunner(Configuration config) {
		this.config = config;
		this.conPool = setConPool(config);
		setOntName(config);
	}

	public ScriptRunner(Configuration config, DbConnectionPool conPool) {
		this.config = config;
		this.conPool = conPool;
		setOntName(config);
	}

	public void run() throws Exception {
		runExternalScript("script_name");
	}

	private void runExternalScript(String scriptName) throws Exception {

		try {
			con = conPool.getConnection();
			con.setAutoCommit(false);

			HashMap<String, String> inputRow = new HashMap<String, String>();
			inputRow.put(":ontName", this.ontName);
			inputRow.put(":finalName", this.finalOntName);
			inputRow.put(":oldName", this.finalOntName + suffixOld);

			File schemaFile = AllConfiguration.getFile(scriptName);
			if (log.isInfoEnabled()) {
				log.info("Running script " + schemaFile.getAbsolutePath() + " ...");
			}
			DbUtility.runSqlScript(schemaFile, inputRow, con);
			if (log.isInfoEnabled()) {
				log.info("Finished executing script " + schemaFile.getAbsolutePath() + " ...");
			}
		} catch (Exception e) {
			try {
				con.rollback();
			} catch (Exception e2) {
				if (log.isErrorEnabled())
					log.error("Unable to rollback inserted rows. Please delete manually!");
			}
			throw e;
		}
		con.commit();
		conPool.releaseConnection(con);

	}

	private DbConnectionPool setConPool(Configuration config) {
		String driver = config.getString("database.driver");
		String url = config.getString("database.url");
		String user = config.getString("database.user");
		String password = config.getString("database.password");
		return new DbConnectionPool(driver, url, user, password);

	}

	private void setOntName(Configuration config) {
		String ontNameTmp = config.getString("ontology_name");

		// validate ontology name
		boolean isValid = true;
		for (int i = 0; i < ontNameTmp.length(); ++i) {
			char c = ontNameTmp.charAt(i);
			if (!Character.isJavaIdentifierPart(c) && (c != ' ')) {
				isValid = false;
				break;
			}
		}

		if (!isValid) {
			throw new IllegalArgumentException(
					"Invalid ontology name! The name can contain only letters, digits, "
							+ "currency symbol('$'), \nconnecting punctuation character('_'), space(' '), and combining mark.");
		}

		this.finalOntName = ontNameTmp;
		this.ontName = (ontNameTmp + suffixTmp);
	}
}
