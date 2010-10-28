package edu.sdsc.ontoquest;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

/**
 * This singleton class contains several sub-configurations used by the system.
 * @version $Id: AllConfiguration.java,v 1.1 2010-10-28 06:30:01 xqian Exp $
 *
 */
public class AllConfiguration {
  private CompositeConfiguration config = null;
  private String workDir = ".";
  private static AllConfiguration _instance = null;
  
  private AllConfiguration(String configFilePath, String appDir) throws ConfigurationException {
    workDir = appDir.replace('\\', '/');
    if (!workDir.endsWith("/"))
      workDir += "/";
    
    config = new CompositeConfiguration();
//    config.setDelimiterParsingDisabled(false);
    
    // load application configuration
    XMLConfiguration appConfig = new XMLConfiguration(configFilePath);
    config.addConfiguration(appConfig);
    
    // load database statement configuration
    List dbStmtConfigFiles = appConfig.getList("database.stmt_file");
    if (dbStmtConfigFiles == null || dbStmtConfigFiles.size() == 0)
      throw new ConfigurationException(
          "The configuration format is incorrect. Could not find element stmt_file.");
    
    Iterator it = dbStmtConfigFiles.iterator();
    AbstractConfiguration.setDefaultListDelimiter('|');
    while (it.hasNext()) {
      String dbStmtConfigFile = (String)it.next();
      if (dbStmtConfigFile == null || dbStmtConfigFile.length() == 0)
        throw new ConfigurationException(
            "The configuration format is incorrect. Could not find element stmt_file.");
    
      XMLConfiguration dbStmtConfig = new XMLConfiguration(
          getFile(dbStmtConfigFile, "database.stmt_file", workDir));
      config.addConfiguration(dbStmtConfig);
    }
    // load optional logging configuration
    String logConfigPath = appConfig.getString("log_config");
    if (logConfigPath != null && logConfigPath.length() > 0) {
      PropertiesConfiguration logConfig = 
        new PropertiesConfiguration(getFile(logConfigPath, "log_config", workDir));
      config.addConfiguration(logConfig);
    }
  }
  
  public static Configuration getConfig() 
      throws ConfigurationException {
    if (_instance == null) 
      throw new ConfigurationException("Configuration is not initialized yet!");
    return _instance.config;
  }
  
  public static synchronized void initialize(String configFilePath) 
      throws ConfigurationException {
    initialize(configFilePath, ".");
  }
  
  public static synchronized void initialize(String configFilePath, String appDir) 
      throws ConfigurationException {
    _instance = new AllConfiguration(configFilePath, appDir);
  }
  
  public static File getFile(String key) throws ConfigurationException {
    if (_instance == null) 
      throw new ConfigurationException("Configuration is not initialized yet!");
    String fileName = _instance.config.getString(key);
    if (fileName == null)
      throw new ConfigurationException("No entry for '" + key + "' is found in the configuration.");
    return getFile(fileName, key, _instance.workDir);
  }
  
  private static File getFile(String fileName, String key, String workDir) throws ConfigurationException {
    File f = new File(fileName);
    if (!f.exists()) {
      f = new File(workDir + fileName);
      if (!f.exists()) {
        throw new ConfigurationException(
            "File does not exist. Property key -- " + key + "; value -- " + fileName + "; absolute path -- "
            + f.getAbsolutePath());
      }
    }
    return f;   
  }
}
