package edu.sdsc.ontoquest;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;

import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbContext;
import edu.sdsc.ontoquest.graph.Graph;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;
import junit.framework.TestCase;

/**
 * A superclass for all ontoquest tests. It makes the 
 * setup of configuration easier.
 * @version $Id: OntoquestTestAdapter.java,v 1.1 2010-10-28 06:29:52 xqian Exp $
 *
 */
public class OntoquestTestAdapter extends TestCase {
  protected String configFileName = "config/ontoquest.xml";
  protected Context context;
  protected BasicFunctions basicFunctions = null;
  protected ArrayList<Variable> varList5 = null;
  protected ArrayList<Variable> varList6 = null;
  protected ArrayList<Variable> varList3 = null;
  protected ArrayList<Variable> varList1 = null;
  protected ArrayList<Variable> varList2 = null;
  protected ArrayList<Variable> varList8 = null;
  
  public OntoquestTestAdapter() {
    super();
  }
  
  public OntoquestTestAdapter(String name) {
    super(name);
  }
  
  public void setUp() {
    try {
      // initialize configuration
      AllConfiguration.initialize(configFileName);
      // initialize database connection pool
      Configuration config = AllConfiguration.getConfig();
      String driver = config.getString("database.driver");
      String url = config.getString("database.url");
      String user = config.getString("database.user");
      String password = config.getString("database.password");
      DbConnectionPool conPool = new DbConnectionPool(driver, url, user, password);   
      context = new DbContext(conPool);
      
      // initialize basic function implementation class
      String bfClassName = config.getString("basic_function_class");
      Utility.checkBlank(bfClassName, OntoquestException.Type.SETTING);
      Class<?> bfClass = null;
      try {
        bfClass = Class.forName(bfClassName);
        Method m = bfClass.getMethod("getInstance", new Class[]{});
        basicFunctions = (BasicFunctions)m.invoke(null, new Object[]{});
      } catch (Throwable e) {
        if (bfClass == null) 
          throw new OntoquestException(OntoquestException.Type.SETTING, 
              "Unable to find basic function class "+bfClassName+".", e);
      
        // the class is found, but we failed to invoke getInstance(). 
        // Try the default constructor
        try {
          basicFunctions = (BasicFunctions)bfClass.getConstructor(new Class[]{}).newInstance(new Object[]{});
        } catch (Throwable t) {
          throw new OntoquestException(OntoquestException.Type.SETTING,
              "Found Class (" + bfClassName + 
              "), but failed to instantiate it. Check the implementation.", t);
        }      
      }
      
      // initialize variable lists.
      setVarList();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
  }

  public void setVarList() {
    // initialize variable list used in query.
    varList5 = new ArrayList<Variable>(5);
    varList5.add(new Variable("v1", 1));
    varList5.add(new Variable("v2", 1));
    varList5.add(new Variable("v3", 1));
    varList5.add(new Variable("v4", 1));
    varList5.add(new Variable("v5", 1));
    
    varList6 = new ArrayList<Variable>(6);
    varList6.add(new Variable("v1", 1));
    varList6.add(new Variable("v2", 1));
    varList6.add(new Variable("v3", 1));
    varList6.add(new Variable("v4", 1));
    varList6.add(new Variable("v5", 1));
    varList6.add(new Variable("v6", 1));

    varList1 = new ArrayList<Variable>(1);
    varList1.add(new Variable("v1", 1));
    
    varList2 = new ArrayList<Variable>(2);
    varList2.add(new Variable("v1", 1));
    varList2.add(new Variable("v2", 1));

    varList8 = new ArrayList<Variable>(8);
    varList8.add(new Variable("v1", 1));
    varList8.add(new Variable("v2", 1));
    varList8.add(new Variable("v3", 1));
    varList8.add(new Variable("v4", 1));
    varList8.add(new Variable("v5", 1));
    varList8.add(new Variable("v6", 1));
    varList8.add(new Variable("v7", 1));
    varList8.add(new Variable("v8", 1));   
    
    varList3 = new ArrayList<Variable>(3);
    varList3.add(new Variable("v1", 1));
    varList3.add(new Variable("v2", 1));
    varList3.add(new Variable("v3", 1));
  }
}
