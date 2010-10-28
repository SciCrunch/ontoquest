import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbContext;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;


/**
 * A simple example to show how to use ontoquest API.
 * @version $Id: OntoquestExample.java,v 1.1 2010-10-28 06:30:03 xqian Exp $
 *
 */
public class OntoquestExample {
  private String configFile = "config/ontoquest.xml"; // default
  private Context context = null;
  ArrayList<Variable> varList5 = null;
  ArrayList<Variable> varList1 = null;
  ArrayList<Variable> varList2 = null;
  ArrayList<Variable> varList8 = null;
  
  private BasicFunctions basicFunctions = null;
  
  String strToSearch[] = {"Zz", "zz_ ", "%", "'", "Cla", "zz|^An"};
  String names[] = {"Pizza", "PineKernels", "Rosa"};
  String kbName = "pizza";
  
  public OntoquestExample() throws OntoquestException, ConfigurationException {
    setUp();
  }
  
  public OntoquestExample(String configFile) throws OntoquestException, ConfigurationException {
    this.configFile = configFile;
    setUp();
  }
  
  private void setUp() throws OntoquestException, ConfigurationException {
      // initialize configuration
      AllConfiguration.initialize(configFile);
      // initialize database connection pool
      Configuration config = AllConfiguration.getConfig();
      String driver = config.getString("database.driver");
      String url = config.getString("database.url");
      String user = config.getString("database.user");
      String password = config.getString("database.password");
      DbConnectionPool conPool = new DbConnectionPool(driver, url, user, password);   
      context = new DbContext(conPool);
      
      // initialize variable list used in query.
      varList5 = new ArrayList<Variable>(5);
      varList5.add(new Variable("v1", 1));
      varList5.add(new Variable("v2", 1));
      varList5.add(new Variable("v3", 1));
      varList5.add(new Variable("v4", 1));
      varList5.add(new Variable("v5", 1));
      
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
  }

  public void run() throws OntoquestException {
    testListKnowledgeBases();
    testSearchName();
    testSearchNameRegex();
    testGetFirstNeighbors();
  }
  
  public void testListKnowledgeBases() throws OntoquestException {
    ResourceSet rs = basicFunctions.listKnowledgeBases(context, varList2);
    while (rs.next()) {
      System.out.println("list KB -- id = " + rs.getInt(1) + "; name = "+rs.getString(2));
    }
    rs.close();
  }

  public void testSearchName() throws OntoquestException {
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    int count = 0, resultLimit = 5;
    for (int i=0; i<strToSearch.length; i++) {
      count = 0;
      ResourceSet rs = basicFunctions.searchName(strToSearch[i], new int[]{kbid}, context, varList1, resultLimit, false, BasicFunctions.MASK_SEARCH_ALL, 0);
      while (rs.next()) {
        count++;
        System.out.println(strToSearch[i] + "   " +rs.getString(1));
      }
      rs.close();
      System.out.println("search string: '" + strToSearch[i] +"' in kb " + kbName + "; count = "+count);
      
      count = 0;
      ResourceSet rs2 = basicFunctions.searchName(strToSearch[i], null, context, varList1, resultLimit, false, BasicFunctions.MASK_SEARCH_ALL, 0);
      while (rs2.next()) {
        count++;
        System.out.println(strToSearch[i] + "   " + rs2.getString(1));
      }
      System.out.println("search string '" + strToSearch[i] +"' in all kb; count2 = "+count);
      rs2.close();
      
      count = 0;
      int searchType = BasicFunctions.MASK_SEARCH_CLASS | BasicFunctions.MASK_SEARCH_INSTANCE;
      ResourceSet rs3 = basicFunctions.searchName(strToSearch[i], new int[]{kbid}, context, varList1, resultLimit, false, searchType, 0);
      while (rs3.next()) {
        count++;
        System.out.println(strToSearch[i] + "   " +rs3.getString(1));
      }
      rs3.close();
      System.out.println("search string: '" + strToSearch[i] +"' in kb " + kbName + "; search classes and instances only. count = "+count);
    }
  }
  
  public void testSearchNameRegex() throws OntoquestException {
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    int count = 0;
    for (int i=0; i<strToSearch.length; i++) {
      count = 0;
      ResourceSet rs = basicFunctions.searchNameRegex(strToSearch[i], new int[]{kbid}, context, varList1, 10, false, BasicFunctions.MASK_SEARCH_ALL);
      while (rs.next()) {
        count++;
        System.out.println(strToSearch[i] + "   " +rs.getString(1));
      }
      rs.close();
      System.out.println("search regex string: '" + strToSearch[i] +"' in kb " + kbName + "; count = "+count);
      
      count = 0;
      ResourceSet rs2 = basicFunctions.searchNameRegex(strToSearch[i], null, context, varList1, 10, false, BasicFunctions.MASK_SEARCH_ALL);
      while (rs2.next()) {
        count++;
        System.out.println(strToSearch[i] + "   " + rs2.getString(1));
      }
      System.out.println("search regex string '" + strToSearch[i] +"' in all kb; count2 = "+count);
      rs2.close();

      count = 0;
      int searchType = BasicFunctions.MASK_SEARCH_CLASS | BasicFunctions.MASK_SEARCH_INSTANCE;
      ResourceSet rs3 = basicFunctions.searchNameRegex(strToSearch[i], new int[]{kbid}, context, varList1, 10, false, searchType);
      while (rs3.next()) {
        count++;
        System.out.println(strToSearch[i] + "   " +rs3.getString(1));
      }
      rs3.close();
      System.out.println("search regex string: '" + strToSearch[i] +"' in kb " + kbName + "; search classes and instances only. count = "+count);
    }
  }

  public void testGetFirstNeighbors() throws OntoquestException {
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    int count = 0;
    OntoquestFunction<ResourceSet> f = null;
    for (int i=0; i<names.length; i++) {
      System.out.println("get incoming relationships in kb pizza...");
      f = new GetNeighbors(names[0], kbid, null, null, true, GetNeighbors.EDGE_INCOMING, 0, true );
      ResourceSet rs = f.execute(context, varList8);
      while (rs.next()) {
        count++;
        System.out.println(rs.getInt(1)+" "+rs.getInt(2)+" "+rs.getString(3)+" "+rs.getInt(4)+" "+rs.getInt(5)+" "+rs.getString(6)+" "+rs.getInt(7)+" "+rs.getString(8));
      }
      rs.close();

      System.out.println("get outgoing relationships in all kb...");
      f = new GetNeighbors(names[1], kbid, null, null, true, GetNeighbors.EDGE_OUTGOING, 0, true );
      rs = f.execute(context, varList8);
      while (rs.next()) {
        count++;
        System.out.println(rs.getInt(1)+" "+rs.getInt(2)+" "+rs.getString(3)+" "+rs.getInt(4)+" "+rs.getInt(5)+" "+rs.getString(6)+" "+rs.getInt(7)+" "+rs.getString(8));
      }
      rs.close();

      System.out.println("*** get bi-directional subClassOf and label relationships ...");
      f = new GetNeighbors(names, kbid, new String[]{"subClassOf", "label"}, null, true, GetNeighbors.EDGE_BOTH, 0, true);
      rs = f.execute(context, varList8);
      while (rs.next()) {
        count++;
        System.out.println(rs.getInt(1)+" "+rs.getInt(2)+" "+rs.getString(3)+" "+rs.getInt(4)+" "+rs.getInt(5)+" "+rs.getString(6)+" "+rs.getInt(7)+" "+rs.getString(8));
      }
      rs.close();
    }
  }
  
  public static void main(String[] args) {
    try {
      OntoquestExample example = new OntoquestExample();
      example.run();
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
