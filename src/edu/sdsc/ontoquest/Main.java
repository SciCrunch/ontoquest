package edu.sdsc.ontoquest;

import org.apache.commons.configuration.Configuration;

import edu.sdsc.ontoquest.db.DbConnectionPool;

/**
 * The main entrance of the application. 
 * @version $Id: Main.java,v 1.1 2010-10-28 06:29:59 xqian Exp $
 *
 */
public class Main {
  private static final String DEFAULT_CONFIG_FILE = "config/ontoquest.xml";
  private DbConnectionPool conPool = null;
  
  private Main() {}
  
  private void initialize(String configFileName) throws Exception {
    // initialize configuration
    AllConfiguration.initialize(configFileName);
    // initialize database connection pool
    Configuration config = AllConfiguration.getConfig();
    String driver = config.getString("database.driver");
    String url = config.getString("database.url");
    String user = config.getString("database.user");
    String password = config.getString("database.password");
    conPool = new DbConnectionPool(driver, url, user, password);    
  }
    
  public static void main(String[] args) throws Exception {
    Main app = new Main();
    String configFileName = DEFAULT_CONFIG_FILE;
    if (args.length > 0) {
      configFileName = args[0];
    }
    app.initialize(configFileName);
    // TODO: start ontoquest server, listen to requests...
  }
}
