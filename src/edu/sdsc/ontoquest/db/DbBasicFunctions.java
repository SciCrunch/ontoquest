package edu.sdsc.ontoquest.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSet;
//import edu.sdsc.ontoquest.graph.GraphEdge;
//import edu.sdsc.ontoquest.graph.GraphNode;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;

import edu.sdsc.ontoquest.rest.BaseBean;

import java.net.URISyntaxException;

import java.sql.SQLException;

//import org.jgrapht.DirectedGraph;
//import org.jgrapht.graph.DefaultDirectedGraph;

/**
 * @version $Id: DbBasicFunctions.java,v 1.7 2013-10-29 23:53:44 jic002 Exp $
 *
 */
public class DbBasicFunctions implements BasicFunctions {
  
  private static DbBasicFunctions _instance = null;
//  private static Log logger = LogFactory.getLog(DbBasicFunctions.class);
  
  private DbBasicFunctions() {}
  
  public synchronized static DbBasicFunctions getInstance() {
    if (_instance == null)
      _instance = new DbBasicFunctions();
    return _instance;
  }
  
  public int getKnowledgeBaseID(String name, Context context) throws OntoquestException {
    Utility.checkBlank(name, OntoquestException.Type.BACKEND);
    StringBuilder sb = new StringBuilder();
    String quotedName = sb.append('\'').
        append(DbUtility.formatSQLString(name)).append('\'').toString();

    Connection conn = null;
    ResultSet rs = null;
    int kbid = -1;
    try {
      String sql = AllConfiguration.getConfig().getString("query.getKbIDByName");
      Utility.checkBlank(sql, OntoquestException.Type.EXECUTOR, "Invalid configuration: getKbIDByName");
      sql = sql.replace(":1", sb.toString());
      conn = DbUtility.getDBConnection(context);
      Statement stmt = conn.createStatement();
      rs = stmt.executeQuery(sql);
      if (rs.next()) {
        kbid = rs.getInt(1);
      }
      if (rs.next()) // more than one match
        throw new OntoquestException(OntoquestException.Type.INPUT, 
            "Found multiple match for knowledge base " + quotedName+".");
      if (kbid == -1) // no match
        throw new OntoquestException(OntoquestException.Type.INPUT,
            "Knowledge base " + quotedName + " is not found.");
      return kbid;
    } catch (Exception e) {
      if (!(e instanceof OntoquestException)) {
        e.printStackTrace();
        throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Failed to retrieve id for knowledge base "+quotedName+". " + e.getMessage(), e);
      } else {
        throw (OntoquestException)e; // throw ontoquest exception up
      }
    } finally {
      try {
        rs.close();
      } catch (Exception e) {}
      DbUtility.releaseDbConnection(conn, context);
    }
  }
  
  public ResourceSet scanClassRelationships(int kbid, Context context, List<Variable> varList)
      throws OntoquestException {
    
    String[] args = new String[]{String.valueOf(kbid)};
    return DbUtility.executeSQLQueryName("query.scanClassRelationships", context, varList, args, 
        "Failed to retrieve class relationships for knowledge base (id = " + kbid + ").", -1);
  }

  public ResourceSet scanPropertyRelationships(int kbid, Context context,
      List<Variable> varList) throws OntoquestException {
    String[] args = new String[]{String.valueOf(kbid)};
    return DbUtility.executeSQLQueryName("query.scanPropertyRelationships", context, varList, args, 
        "Failed to retrieve class relationships for knowledge base (id = " + kbid + ").", -1);
  }

  public ResourceSet scanAllRelationships(int[] kbid, Context context, List<Variable> varList, boolean isWeighted)
      throws OntoquestException {
    String kbCondition = composeKBCondition(kbid, "");
    String queryName = "query.scanRelationshipsWithWeight";
    if (!isWeighted) {
      queryName = "query.scanRelationships";
      kbCondition = (kbCondition.length() > 0) ? kbCondition.substring("and ".length()) : "";
    }
    String[] args = {kbCondition};
    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to scan all relationships.", -1);
  }
  
  public String getName(int rtid, int rid, Context context) throws OntoquestException {
    String queryName = "query.getNameByID";
    String[] args = new String[2];
    args[0] = String.valueOf(rid);
    args[1] = String.valueOf(rtid);
    List<Variable> varList = new ArrayList<Variable>(1);
    varList.add(new Variable(1));
    ResourceSet rs = DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Couldn't find resource name! resource id = "+rid + "; resource type id = " + rtid, -1);
    String name = null;
    if (rs.next()) {
      name = rs.getString(1);
    }
    rs.close();
    return name;
  }
  
  public ResourceSet getNames(int[][] nodeIds, Context context) throws OntoquestException {
    String queryName = "query.getNamesByIDArray";
    String[] args = new String[1];
    StringBuilder sb = new StringBuilder();
    for (int[] nid : nodeIds) {
      sb.append('{').append(nid[0]).append(',').append(nid[1]).append('}').append(',');
    }
    sb.deleteCharAt(sb.length()-1);
    args[0] = sb.toString();
    List<Variable> varList = new ArrayList<Variable>(3);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Couldn't find resource names!", -1);
  }
  
  /**
   * Returns the value of rdfs:label, if available. Otherwise, return name as label.
   * @param rtid
   * @param rid
   * @param context
   * @return
   * @throws OntoquestException
   */
  public String getLabel(int rtid, int rid, Context context) throws OntoquestException {
    String queryName = "query.getLabelByID";
    String[] args = new String[2];
    args[0] = String.valueOf(rid);
    args[1] = String.valueOf(rtid);
    List<Variable> varList = new ArrayList<Variable>(1);
    varList.add(new Variable(1));
    ResourceSet rs = DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Couldn't find resource name! resource id = "+rid + "; resource type id = " + rtid, -1);
    String name = null;
    if (rs.next()) {
      name = rs.getString(1);
    }
    rs.close();
    return name;
  }

  public ResourceSet getLabels(int[][] nodeIds, Context context) throws OntoquestException {
    String queryName = "query.getLabelsByIDArray";
    String[] args = new String[1];
    StringBuilder sb = new StringBuilder();
    for (int[] nid : nodeIds) {
      sb.append('{').append(nid[0]).append(',').append(nid[1]).append('}').append(',');
    }
    sb.deleteCharAt(sb.length()-1);
    args[0] = sb.toString();
    List<Variable> varList = new ArrayList<Variable>(3);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Couldn't find labels!", -1);
  }

  public ResourceSet getPropertyValue(int rid, int rtid, String pname, 
      Context context, List<Variable> varList) throws OntoquestException {
    String queryName = "query.getPropertyValue";
    Utility.checkBlank(pname, OntoquestException.Type.INPUT, 
        "Property name is empty");
    String[] args = new String[3];
    args[0] = String.valueOf(rid);
    args[1] = String.valueOf(rtid);
    args[2] = DbUtility.formatSQLString(pname);
    
    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to fetch property value! property name = " + pname + 
        "; rid = "+rid +"; rtid = "+rtid, -1);
  }
  
  public ResourceSet listKnowledgeBases(Context context, List<Variable> varList) throws OntoquestException {
    return DbUtility.executeSQLQueryName("query.listKB", context, varList, 
        new String[]{}, "Unable to list all knowledge bases.", -1);
  }

  public ResourceSet searchName(String strToSearch, Context context, List<Variable> varList) throws OntoquestException {
    return searchName(strToSearch, null, context, varList, 0, false, 15, 0);
  }
  
  /**
   * Given a string, to find possible matches with class name, individual name, property name or literal.
   * The results are ordered by edit distance.
   * @param strToSearch the string to search
   * @param kbid IDs of the knowledge base (ontology) to search. Search all knowledge bases if kbid is null or empty.
   * @param context query context.
   * @param varList the list of variables to bind
   * @param resultLimit max number of results to return. return all results if resultLimit = 0.
   * @param useNegation find the strings that DO NOT match the input. Default value is false.
   * @param searchType a flag to indicate which type(s) of resources to search. The definitions are:
   * <UL><LI>If searchType & MASK_SEARCH_CLASS(1) > 0, classes are searched. 
   * <LI>If searchType & MASK_SEARCH_INSTANCE (2) > 0, instances are searched. 
   * <LI>If searchType & MASK_SEARCH_PROPERTY (4) > 0, properties are searched. 
   * <LI>If searchType & MASK_SEARCH_LITERAL (8) > 0, literals are searched. 
   * <LI>If searchType & (MASK_SEARCH_CLASS|MASK_SEARCH_INSTANCE|MASK_SEARCH_PROPERTY|MASK_SEARCH_LITERAL)=0, 
   * the searchType is invalid. An exception is thrown.
   * </UL>
   * <BR>For example,
   * <UL> <LI>searchType = 1 -> search classes only
   * <LI>searchType = 3 -> search both classes and instances
   * <LI>searchType = 15 -> search all categories (classes, instances, properties, literals)
   * <LI>searchType = 11 -> search classes, instances and literals
   * </UL>
   * @param maxEditDistance maximum edit distance between result and query.  No limit if the value is equal or less than 0. 
   * @return a resource set containing matched strings. It contains only one column with type string.
   * @throws OntoquestException
   */
  public ResourceSet searchName(String strToSearch, int[] kbid, Context context, 
      List<Variable> varList, int resultLimit, boolean useNegation, 
      int searchType, int maxEditDistance) 
    throws OntoquestException {
    return searchName(strToSearch, kbid, context, varList, resultLimit, useNegation, searchType, maxEditDistance, false);
  }
  
  public ResourceSet searchNameStartWith(String strToSearch, int[] kbid, Context context, 
      List<Variable> varList, int resultLimit, boolean useNegation, 
      int searchType, int maxEditDistance) 
    throws OntoquestException {
    return searchName(strToSearch, kbid, context, varList, resultLimit, useNegation, searchType, maxEditDistance, true);
  }

  public ResourceSet searchName(String strToSearch, int[] kbid, Context context, 
                                List<Variable> varList, int resultLimit, boolean useNegation, 
                                int searchType, int maxEditDistance, boolean startWith) 
      throws OntoquestException {
    // process the string to query
    Utility.checkBlank(strToSearch, OntoquestException.Type.INPUT, "Empty string to search!");
    String queryStr = strToSearch.toLowerCase();
    boolean addEscapeChar = (queryStr.indexOf('%') >= 0) || (queryStr.indexOf('_')>=0 || (queryStr.indexOf('\\')>=0));
    queryStr = queryStr.replace("\\", "\\\\\\\\");
    queryStr = queryStr.replace("%", "\\\\%");
    queryStr = queryStr.replace("_", "\\\\_");
    queryStr = DbUtility.formatSQLString(queryStr);
    if (!startWith)
      queryStr = '%' + queryStr;
 
    // process search type
    StringBuilder subSQL = new StringBuilder();
    String sql = null;
    try {
      if ((searchType & MASK_SEARCH_CLASS) > 0) {
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameLikeInClasses"));
      } 
      if ((searchType & MASK_SEARCH_INSTANCE) > 0) {
        subSQL.append(subSQL.length()>0?" union ":"");
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameLikeInInstances"));
      }
      if ((searchType & MASK_SEARCH_PROPERTY) > 0) {
        subSQL.append(subSQL.length()>0?" union ":"");
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameLikeInProperties"));
      }
      if ((searchType & MASK_SEARCH_LITERAL) > 0) {
        subSQL.append(subSQL.length()>0?" union ":"");
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameLikeInLiterals"));
      }
      if (subSQL.length() == 0) // empty string
        throw new OntoquestException(OntoquestException.Type.INPUT, 
            "Invalid searchType. It should range from 1 to 15.");
      
      sql = AllConfiguration.getConfig().getString("query.searchNameLike");
      sql = sql.replace(":0", subSQL.toString());
    } catch (ConfigurationException ce) {
      throw new OntoquestException(OntoquestException.Type.EXECUTOR, 
          "Invalid configuration. " + ce.getMessage(), ce);
    }
    
    // process maxEditDistance
    
    // prepare arguments for SQL
    String[] args = new String[7];
    args[0] = queryStr;
    args[1] = composeKBCondition(kbid, null);
    args[2] = addEscapeChar?"E":"";
    args[3] = strToSearch.replace("'", "''");
    args[4] = (useNegation)?"NOT":"";
    args[5] = (maxEditDistance<=0)?"":" where d <= "+maxEditDistance;
    args[6] = (resultLimit<=0)?"ALL":String.valueOf(resultLimit);
    
    return DbUtility.executeSQLQuery(sql, context, varList, args, 
        "Failed to search name :" + strToSearch, resultLimit);
  }
   
  public ResourceSet searchTerm(String strToSearch, int[] kbid, Context context, 
      List<Variable> varList, int resultLimit, boolean useNegation, 
      int maxEditDistance, boolean startWith) throws OntoquestException {
    // process the string to query
    Utility.checkBlank(strToSearch, OntoquestException.Type.INPUT, "Empty string to search!");
    String queryStr = strToSearch.toLowerCase();
    boolean addEscapeChar = (queryStr.indexOf('%') >= 0) || (queryStr.indexOf('_')>=0 || (queryStr.indexOf('\\')>=0));
    queryStr = queryStr.replace("\\", "\\\\\\\\");
    queryStr = queryStr.replace("%", "\\\\%");
    queryStr = queryStr.replace("_", "\\\\_");
    queryStr = DbUtility.formatSQLString(queryStr);
    if (!startWith)
      queryStr = '%' + queryStr;
    // process search type
    StringBuilder subSQL = new StringBuilder();
    String sql = null;
    try {
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameLikeInClasses"));
        subSQL.append(" union ");
        subSQL.append(AllConfiguration.getConfig().getString("query.searchTermLikeInLiterals"));
      sql = AllConfiguration.getConfig().getString("query.searchNameLike");
      sql = sql.replace(":0", subSQL.toString());
    } catch (ConfigurationException ce) {
      throw new OntoquestException(OntoquestException.Type.EXECUTOR, 
          "Invalid configuration. " + ce.getMessage(), ce);
    }
    
    // process maxEditDistance
    
    // prepare arguments for SQL
    String[] args = new String[7];
    args[0] = queryStr;
    args[1] = composeKBCondition(kbid, null);
    args[2] = addEscapeChar?"E":"";
    args[3] = strToSearch.replace("'", "''");
    args[4] = (useNegation)?"NOT":"";
    args[5] = (maxEditDistance<=0)?"":" where d <= "+maxEditDistance;
    args[6] = (resultLimit<=0)?"ALL":String.valueOf(resultLimit);
    
    return DbUtility.executeSQLQuery(sql, context, varList, args, 
        "Failed to search term :" + strToSearch, resultLimit);
  }
  
  /**
   * Search possible match in class name, property name, individual name or literal.
   * @param strToSearch the input string to search
   * @param kbid IDs of the knowledge bases to search
   * @param context query context
   * @param varList variables to be bound
   * @param resultLimit max number of results to return. return all results if resultLimit = 0.
   * @param useNegation find the strings that DO NOT match the input. Default value is false.
   * @param searchType @see {@link #searchName(String, int[], Context, List, int, boolean, int)}
   * @return a resource set containing matched strings. It contains only one column with type string.
   * @throws OntoquestException
   */
  public ResourceSet searchNameRegex(String strToSearch, int[] kbid, Context context, 
      List<Variable> varList, int resultLimit, boolean useNegation, int searchType) throws OntoquestException {
    // process the string to query
    Utility.checkBlank(strToSearch, OntoquestException.Type.INPUT, "Empty string to search!");    
    String queryStr = DbUtility.formatSQLString(strToSearch);

    // process search type
    StringBuilder subSQL = new StringBuilder();
    String sql = null;
    try {
      if ((searchType & MASK_SEARCH_CLASS) > 0) {
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameRegExInClasses"));
      } 
      if ((searchType & MASK_SEARCH_INSTANCE) > 0) {
        subSQL.append(subSQL.length()>0?" union ":"");
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameRegExInInstances"));
      }
      if ((searchType & MASK_SEARCH_PROPERTY) > 0) {
        subSQL.append(subSQL.length()>0?" union ":"");
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameRegExInProperties"));
      }
      if ((searchType & MASK_SEARCH_LITERAL) > 0) {
        subSQL.append(subSQL.length()>0?" union ":"");
        subSQL.append(AllConfiguration.getConfig().getString("query.searchNameRegExInLiterals"));
      }
      if (subSQL.length() == 0) // empty string
        throw new OntoquestException(OntoquestException.Type.INPUT, 
            "Invalid searchType. It should range from 1 to 15.");
      
      sql = AllConfiguration.getConfig().getString("query.searchNameRegEx");
      sql = sql.replace(":0", subSQL.toString());
    } catch (ConfigurationException ce) {
      throw new OntoquestException(OntoquestException.Type.EXECUTOR, 
          "Invalid configuration. " + ce.getMessage(), ce);
    }

    // prepare arguments for SQL
    String[] args = new String[4];
    args[0] = queryStr;
    args[1] = composeKBCondition(kbid, null);
    args[2] = useNegation?"!~*":"~*";
    args[3] = (resultLimit==0)?"ALL":String.valueOf(resultLimit);
    
    return DbUtility.executeSQLQuery(sql, context, varList, args, 
        "Failed to search name :" + strToSearch, -1);
  }

  /**
   * Expect to return a resource set with three columns: rid (int), rtid (int), name (string).
   */
  public ResourceSet searchAllIDsByName(String nodeName, int[] kbid, Context context, 
      List<Variable> varList)
      throws OntoquestException {
    return searchAllIDsByName(nodeName, kbid, false, context, varList);
  }
  
  public ResourceSet searchAllIDsByName(String nodeName, int[] kbid, boolean classOnly, 
      Context context, List<Variable> varList)
      throws OntoquestException {
    return searchAllIDsByName(new String[]{nodeName}, kbid, classOnly, true, context, varList);
  }

  public ResourceSet searchAllIDsByName(String[] nodeNames, int[] kbid, boolean classOnly, boolean useLabel,
      Context context, List<Variable> varList)
      throws OntoquestException {
    // process the string to query
    Utility.checkNull(nodeNames, OntoquestException.Type.INPUT, "Empty string to search!");    
    String queryStr = DbUtility.formatArrayToStr(nodeNames);
    // get connection pool
    Utility.checkClass(context, DbContext.class, OntoquestException.Type.EXECUTOR);
//    DbConnectionPool conPool = ((DbContext)context).getConPool();
    // prepare arguments for SQL
    String[] args = new String[4];
    args[0] = queryStr;
    args[1] = composeKBArray(kbid);
    args[2] = (classOnly)?" where rtid = 1":"";
    args[3] = String.valueOf(useLabel);
    return DbUtility.executeSQLQueryName("query.searchAllIDByName", context, varList, args, 
        "Failed to find matched IDs for '"+queryStr+"'", -1);
  }
  
  public String composeKBCondition(int[] kbid, String tableAlias) {
    StringBuilder kbCondition = new StringBuilder();
    if (kbid != null && kbid.length > 0) {
      if (Utility.isBlank(tableAlias)) {
        kbCondition.append("and kbid ");
      } else { // "and $tableAlias.kbid "
        kbCondition.append("and ").append(tableAlias).append(".").append("kbid ");
      }
      if (kbid.length == 1) {
        kbCondition.append("= ").append(kbid[0]);
      } else {
        kbCondition.append("in (");
        for (int i=0; i<kbid.length; i++) {
          kbCondition.append(kbid[i]);
          if (i < kbid.length-1)
            kbCondition.append(',');
        }
        kbCondition.append(')');
      }
    }
    return kbCondition.toString();
  }
  
  public String composePnameCondition(String[] pnames, String tableAlias, boolean isNegated) {
    StringBuilder pnameCondition = new StringBuilder();
    if (pnames != null && pnames.length > 0) {
      if (Utility.isBlank(tableAlias)) {
        pnameCondition.append("and name ");
      } else {
        pnameCondition.append("and ").append(tableAlias).append(".name ");
      }
      if (pnames.length == 1) {
        pnameCondition.append(isNegated?"!":"");
        pnameCondition.append("= '").append(DbUtility.formatSQLString(pnames[0])).append('\'');
      } else {
        pnameCondition.append(isNegated?"not ":"");
        pnameCondition.append("in (");
        for (int i=0; i<pnames.length; i++) {
          pnameCondition.append('\'').append(DbUtility.formatSQLString(pnames[i])).append('\'');
          if (i < pnames.length-1)
            pnameCondition.append(',');
        }
        pnameCondition.append(')');
      }
    }
    return pnameCondition.toString();
  }

  public ResourceSet listRootResources(int kbid, boolean excludeOWLThing, 
      boolean prefLabel, Context context, List<Variable> varList) throws OntoquestException {
    String queryName = "query.listRoot";
    String[] args = new String[3];
    args[0] = String.valueOf(kbid);
    args[1] = String.valueOf(excludeOWLThing);
    args[2] = String.valueOf(prefLabel);
    
    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to fetch root terms in knowledge base (kbid = " + kbid + ")", -1);
  }

  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.BasicFunctions#searchNameLike(java.lang.String, int, boolean, int, edu.sdsc.ontoquest.Context, java.util.List)
   */
  public ResourceSet searchNameLike(String strToSearch, int kbid,
      boolean prefLabel, int searchType, Context context, List<Variable> varList)
      throws OntoquestException {
    String[] args = new String[4];
    args[0] = DbUtility.formatSQLString(strToSearch);
    if (kbid > 0)
      args[1] = String.valueOf(kbid);
    else
      args[1] = "null";
    
    // search type
    StringBuilder sb = new StringBuilder();
    sb.append(((searchType & MASK_SEARCH_LITERAL) > 0)?'1':'0');
    sb.append(((searchType & MASK_SEARCH_PROPERTY) > 0)?'1':'0');
    sb.append(((searchType & MASK_SEARCH_INSTANCE) > 0)?'1':'0');
    sb.append(((searchType & MASK_SEARCH_CLASS) > 0)?'1':'0');
    args[2] = sb.toString();
    
    args[3] = String.valueOf(prefLabel);
    return DbUtility.executeSQLQueryName("query.searchNameLike2", context, 
        varList, args, "Failed to search name: " + strToSearch, -1);
  }

  public String composeKBArray(int[] kbid) {
    StringBuilder sb = new StringBuilder();
    sb.append("array[");
    for (int i : kbid) {
      sb.append(i).append(',');
    }
    sb.deleteCharAt(sb.length()-1);
    sb.append(']');
    return sb.toString();
  }

  public ResourceSet searchSubclasses ( int kbid, String term_id, Context context,boolean includingEquivalentClass) throws OntoquestException
  {
    String sql = "select * from get_closure_by_term_id('"+term_id+"','subClassOf',"+kbid+",true,"+
                   includingEquivalentClass +")";
    String errmsg = "";
    return DbUtility.executeSQLQuery( sql, context, BaseBean.getVarList8(), null, errmsg, -1);
  }

  public ResourceSet searchHasPart ( int kbid, String term_id, Context context,boolean includingEquivalentClass) throws OntoquestException
  {
    String sql = "select * from get_closure_by_term_id('"+term_id+"','part_of',"+kbid+",false,"+
                   includingEquivalentClass +")";
    String errmsg = "";
    return DbUtility.executeSQLQuery( sql, context, BaseBean.getVarList8(), null, errmsg, -1);
  }

  public ResourceSet searchpartOf ( int kbid, String term_id, Context context,boolean includingEquivalentClass) throws OntoquestException
  {
    String sql = "select * from get_closure_by_term_id('"+term_id+"','part_of',"+kbid+",true,"+
                   includingEquivalentClass +")";
    String errmsg = "";
    return DbUtility.executeSQLQuery( sql, context, BaseBean.getVarList8(), null, errmsg, -1);
  }


  public ResourceSet searchClosure ( int kbid, String term_id, Context context,
                                      String propertyName, boolean incoming, boolean includingEquivalentClass)
             throws OntoquestException
  {
    String sql = "select * from get_closure_by_term_id('"+term_id+"','" + propertyName + "',"+kbid+","+ incoming+ 
                  "," + includingEquivalentClass+ ")";
    String errmsg = "";
    return DbUtility.executeSQLQuery( sql, context, BaseBean.getVarList8(), null, errmsg, -1);
  }


  /*
  public void  addDesendantsToGraph(int kbid, int rid, int rtid, int propId,
                                                                  Context context,
                                                                  DirectedGraph<GraphNode, GraphEdge> g) throws SQLException,
                                                                                           OntoquestException,
                                                                                           URISyntaxException
  {
  //  DirectedGraph<GraphNode, GraphEdge> g = new DefaultDirectedGraph<GraphNode, GraphEdge>(GraphEdge.class);
    
    GraphNode n = new GraphNode( rid, rtid, kbid, context);
    g.addVertex(n);
  
    String stmt = "select e.rid2, e.rtid2, n.name, n.label, n.uri, p.name, e.pid, case when position(':' in p.browsertext) >=1 then " + 
                   "(ns.url || substring(p.browsertext,length(ns.prefix)+1)) else (ns.url ||p.browsertext) end as puri " +
                   "from graph_edges e, property p, graph_nodes n, namespace ns where e.pid = p.id  and p.kbid = e.kbid " +
                  "and ns.id = p.nsid and n.kbid = e.kbid and e.rid2=n.rid and e.rtid2=n.rtid and rid1 = " + rid + 
                  " and rtid1 = " + rtid + " and e.kbid = " + kbid + " and e.pid in ( 6435,7137, 6307,7602,7044," + propId + ")";

    Connection conn = DbUtility.getDBConnection(context);

  //  boolean found = false;
    ResultSet r = DbUtility.runSqlQuery(stmt, conn);
    while (r.next()) 
    {
       GraphNode n2 = new GraphNode( r.getString(3), r.getString(4), r.getString(5), r.getInt(1), r.getInt(2), kbid);
       g.addVertex(n2);
       GraphEdge e = new GraphEdge(r.getInt(7), r.getString(6),r.getString(7), kbid);
       boolean result = g.addEdge(n, n2, e);
       System.out.println (result);
       addDesendantsToGraph(kbid, n2.getRid(), n2.getRTid(), propId, context, g);
    }
    DbUtility.releaseDbConnection(conn, context);
    //return g;
  }   
//    DbUtility.releaseDbConnection(conn, context);
  
  
    public void  addAscendantsToGraph(int kbid, int rid, int rtid, int[] propIds,
                                                                  Context context,
                                                                  DirectedGraph<GraphNode, GraphEdge> g) throws SQLException,
                                                                                           OntoquestException,
                                                                                           URISyntaxException
  {
  //  DirectedGraph<GraphNode, GraphEdge> g = new DefaultDirectedGraph<GraphNode, GraphEdge>(GraphEdge.class);
    
    String idstr = "";
    
    for ( int i =0 ; i < propIds.length; i++ ) 
    {
      idstr += Integer.toString(propIds[i]);
      if (i <propIds.length-1) 
        idstr += ',';
    }
    
    GraphNode n = new GraphNode( rid, rtid, kbid, context);
    g.addVertex(n);
  
    String stmt = "select e.rid1, e.rtid1, n.name, n.label, n.uri, p.name, e.pid, case when position(':' in p.browsertext) >=1 then " + 
                   "(ns.url || substring(p.browsertext,length(ns.prefix)+1)) else (ns.url ||p.browsertext) end as puri " +
                   "from graph_edges e, property p, graph_nodes n, namespace ns where e.pid = p.id  and p.kbid = e.kbid " +
                  "and ns.id = p.nsid and n.kbid = e.kbid and e.rid1=n.rid and e.rtid1=n.rtid and rid2 = " + rid + 
                  " and rtid2 = " + rtid + " and e.kbid = " + kbid + " and e.pid in ( 6435,7137, 6307,7602,7044," + idstr + ")";

    Connection conn = DbUtility.getDBConnection(context);

  //  boolean found = false;
    ResultSet r = DbUtility.runSqlQuery(stmt, conn);
    while (r.next()) 
    {
       GraphNode n1 = new GraphNode( r.getString(3), r.getString(4), r.getString(5), r.getInt(1), r.getInt(2), kbid);
       g.addVertex(n1);
       GraphEdge e = new GraphEdge(r.getInt(7), r.getString(6),r.getString(8), kbid);
       boolean result = g.addEdge(n1, n, e);
       if (r.getInt(7) != 6313)
         System.out.println (r.getInt(7));
       addAscendantsToGraph(kbid, n1.getRid(), n1.getRTid(), propIds, context, g);
    }
    DbUtility.releaseDbConnection(conn, context);
    //return g;
  }   
//    DbUtility.releaseDbConnection(conn, context);
  
  */
}
