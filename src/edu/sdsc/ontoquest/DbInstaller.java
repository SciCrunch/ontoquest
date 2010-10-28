package edu.sdsc.ontoquest;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbUtility;

/**
 * @version $Id: DbInstaller.java,v 1.1 2010-10-28 06:29:59 xqian Exp $
 * @deprecated
 */
public class DbInstaller {

  private String configFile = "config/ontoquest.xml"; // default
  private DbConnectionPool conPool = null;
  
  public DbInstaller() throws OntoquestException, ConfigurationException {
    initialize(configFile);
  }
  
  public DbInstaller(String configFileName) throws OntoquestException, ConfigurationException {
    configFile = configFileName;
    initialize(configFile);
  }

  private void initialize(String configFileName) throws OntoquestException, ConfigurationException {
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

  public void run() throws IOException, SQLException, ConfigurationException, OntoquestException {
    File schemaFile = AllConfiguration.getFile("database.schema_file");
    DbUtility.runSqlScript(schemaFile, conPool.getConnection(10000));
  }
  
  public static void main(String[] args) {
    try {
      DbInstaller instance = null;
      if (args.length > 0) {
        instance = new DbInstaller(args[0]);
      } else {
        instance = new DbInstaller();
      }
      instance.run();
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
