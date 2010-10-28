package edu.sdsc.ontoquest;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import junit.framework.TestCase;

/**
 * @version $Id: AllConfigurationTest.java,v 1.1 2010-10-28 06:29:53 xqian Exp $
 *
 */
public class AllConfigurationTest extends TestCase {
  
  public void setUp() {
    try {
      AllConfiguration.initialize("config/ontoquest.xml");
    } catch (ConfigurationException ce) {
      ce.printStackTrace();
    }
  }
  
  public void testGetConfig() throws Exception {
    Configuration config = AllConfiguration.getConfig();
    String dbType = config.getString("database.type");
    assertEquals("PostgreSQL", dbType);
    String getKBIDStmt = config.getString("query.getKbIDByName");
    assertNotNull(getKBIDStmt);
    assertTrue(getKBIDStmt.length() > 0);
  }
  
}
