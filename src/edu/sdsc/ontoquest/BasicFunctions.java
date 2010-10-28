package edu.sdsc.ontoquest;

import java.util.List;

import edu.sdsc.ontoquest.query.Variable;

/**
 * The interface to declare what the basic functions are required by the system.
 * All implementations MUST provide a default constructor (constructor without
 * any argument) or a static function called getInstance(). Assume the 
 * implementation class is called MyBasicFunction, the signature of
 * getInstance() is
 * <pre>      public static MyBasicFunction getInstance() </pre>
 * or
 * <pre>      public static BasicFunction getInstance() </pre>
 * @version $Id: BasicFunctions.java,v 1.1 2010-10-28 06:30:01 xqian Exp $
 *
 */
public interface BasicFunctions {
  
  public static final int MASK_SEARCH_CLASS = 0x0001;
  public static final int MASK_SEARCH_INSTANCE = 0x0002;
  public static final int MASK_SEARCH_PROPERTY = 0x0004;
  public static final int MASK_SEARCH_LITERAL = 0x0008;
  public static final int MASK_SEARCH_ALL = MASK_SEARCH_CLASS | MASK_SEARCH_INSTANCE | MASK_SEARCH_PROPERTY | MASK_SEARCH_LITERAL;
  
  /**
   * Given a knowledge base (top-level ontology) name, find its ID.
   * @param name knowledge base name
   * @param context query context
   * @return knowledge base id
   * @throws OntoquestException
   */
  public int getKnowledgeBaseID(String name, Context context) throws OntoquestException;

  /**
   * Given a pair of (rtid, rid), find the resource's name.
   * @param rtid resource type id, e.g. id of PrimitiveClass, id of UnionClass
   * @param rid id of the resource
   * @param context
   * @return resource name
   * @throws OntoquestException
   */
  public String getName(int rtid, int rid, Context context) throws OntoquestException;
  
  /**
   * Given an array of id pairs, find the names of the resources.
   * @param nodeIds
   * @param context
   * @return
   * @throws OntoquestException
   */
  public ResourceSet getNames(int[][] nodeIds, Context context) throws OntoquestException;
  
  /**
   * Returns the value of rdfs:label, if available. Otherwise, return name as label.
   * @param rtid
   * @param rid
   * @param context
   * @return
   * @throws OntoquestException
   */
  public String getLabel(int rtid, int rid, Context context) throws OntoquestException;

  /**
   * Returns the value of rdfs:label, if available. Otherwise, return name as label.
   * @param nodeIds 
   * @param context
   * @return
   * @throws OntoquestException
   */
  public ResourceSet getLabels(int[][] nodeIds, Context context) throws OntoquestException;

  /**
   * Given a resource (rid, rtid), find the value of property (name = pname).
   * @param rid id of the resource
   * @param rtid resource type id
   * @param pname property name
   * @param context query context
   * @param varList variable list to bind to the result. One String variable is expected.
   * @return a set of Strings. 
   * @throws OntoquestException
   */
  public ResourceSet getPropertyValue(int rid, int rtid, String pname, 
      Context context, List<Variable> varList) throws OntoquestException;

  /**
   * scan 3 kinds of relationships between classes: subClassOf, disjointWith, equivalentClass
   * @param kbid knowledge base id
   * @param context query context
   * @param varList variable list to be bound by results.
   * @return a cursor to iterate the set of relationships (edges).
   * @throws OntoquestException
   */
  public ResourceSet scanClassRelationships(int kbid, Context context, List<Variable> varList)
      throws OntoquestException;

  /**
   * scan 3 kinds of relationships between properties: subPropertyOf, inverseOf, equivalentProperty
   * @param kbid knowledge base id
   * @param context query context
   * @param varList variable list to be bound by results.
   * @return a cursor to iterate the set of relationships (edges).
   * @throws OntoquestException
   */
  public ResourceSet scanPropertyRelationships(int kbid, Context context,
      List<Variable> varList) throws OntoquestException;
  
  /**
   * scan all relationships (subject property object), including system-defined and user-defined.
   * System-defined relationships are those that are defined by OWL specifications, for
   * example, subClassOf relationship.
   * @param kbid knowledge base (ontology) ids.
   * @param context the query context
   * @return A ResultSet that contains all relationships. Each row in the resultSet is a pentuple:
   * (subjectID, subjectResourceTypeID, propertyID, objectID, objectResourceTypeID). The data type of each row is: 
   * (int int int int int)
   */
  public ResourceSet scanAllRelationships(int[] kbid, Context context, List<Variable> varList, boolean isWeighted)
      throws OntoquestException;
  
  /**
   * Given a term (nodeName), find all matched resources. Please notice one term may
   * match to 0 or more resources. Only exact matches are returned.
   * @param nodeName the query term. 
   * @param kbid the IDs of the knowledge bases to be searched.
   * @param context query context
   * @param varList variable list. Expect 3 variables (int, int, string) in the list.
   * @return a set of resources id pairs (rid, rtid, name).
   * @throws OntoquestException
   */
  public ResourceSet searchAllIDsByName(String nodeName, int[] kbid, Context context, List<Variable> varList)
    throws OntoquestException;

  /**
   * Given a term (nodeName), find all matched resources. Please notice one term may
   * match to 0 or more resources. Only exact matches are returned.
   * @param nodeName the query term. 
   * @param kbid the IDs of the knowledge bases to be searched.
   * @param classOnly return only primitive classes' IDs.
   * @param context query context
   * @param varList variable list. Expect 3 variables (int, int, string) in the list.
   * @return a set of resources id pairs (rid, rtid, name).
   * @throws OntoquestException
   */
  public ResourceSet searchAllIDsByName(String nodeName, int[] kbid, boolean classOnly, Context context, List<Variable> varList)
    throws OntoquestException;

  /**
   * Given an array of terms (nodeNames), find all matched resources. Please notice one term may
   * match to 0 or more resources. Only exact matches are returned.
   * @param nodeNames the query terms. 
   * @param kbid the IDs of the knowledge bases to be searched.
   * @param classOnly return only primitive classes' IDs.
   * @param useLabel if true, return the label, instead of nodeName. If false, return the supplying nodeName
   * @param context query context
   * @param varList variable list. Expect 3 variables (int, int, string) in the list.
   * @return a set of resources id pairs (rid, rtid, name).
   * @throws OntoquestException
   */
  public ResourceSet searchAllIDsByName(String[] nodeNames, int[] kbid, boolean classOnly, boolean useLabel,
      Context context, List<Variable> varList) throws OntoquestException;
  
  /**
   * Given a string, to find possible matches with class name, individual name, 
   * property name or literal in all knowledge bases.
   * @param strToSearch the string to search
   * @param context
   * @param varList the list of variables to bind
   * @return
   * @throws OntoquestException
   */
  public ResourceSet searchName(String strToSearch, Context context, List<Variable> varList) throws OntoquestException;

  /**
   * Given a string, search the knowledge base using LIKE operator. The match is 
   * case-insensitive. This function should replace searchName in near future.
   * @param strToSearch the string to search.
   * @param kbid the knowledge base to search. If 0, search all knowledge bases.
   * @param prefLabel if true, prefer to use the class' rdfs:label property as its name.
   * if false, just use class' name. In some ontologies, e.g. SAO, class' name is actually 
   * an id. The real name is its rdfs:label field.
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
   * @param context query context.
   * @param varList the list of variables to bind
   * @return
   * @throws OntoquestException
   */
  public ResourceSet searchNameLike(String strToSearch, int kbid, boolean prefLabel, int searchType, Context context, List<Variable> varList) throws OntoquestException;

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
   * @param maxEditDistance maximum edit distance between result and query. No limit if the value is equal or less than 0. 
   * @return a resource set containing matched strings. It contains only one column with type string.
   * @throws OntoquestException
   */
  public ResourceSet searchName(String strToSearch, int[] kbid, Context context, 
      List<Variable> varList, int resultLimit, boolean useNegation, int searchType, int maxEditDistance) 
    throws OntoquestException;

  /**
   * Similar to searchName, except that this method only finds names start with strToSearch.
   * @param strToSearch
   * @param kbid
   * @param context
   * @param varList
   * @param resultLimit
   * @param useNegation
   * @param searchType
   * @param maxEditDistance
   * @return
   * @throws OntoquestException
   */
  public ResourceSet searchNameStartWith(String strToSearch, int[] kbid, Context context, 
      List<Variable> varList, int resultLimit, boolean useNegation, int searchType, int maxEditDistance) 
    throws OntoquestException;

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
      List<Variable> varList, int resultLimit, boolean useNegation, int searchType) throws OntoquestException;

  /**
   * Search concept name and its synonyms, labels, abbreviations, etc.
   * @param strToSearch the partial string to search
   * @param kbid the id array of kb to search
   * @param context
   * @param varList list of one var
   * @param resultLimit e.g. 30. The max count of matches.
   * @param useNegation if true, search the term not matched.
   * @param maxEditDistance maximum edit distances, e.g. 40
   * @param startWith if true, return only terms beginning with the strToSearch.
   * @return
   * @throws OntoquestException
   */
  public ResourceSet searchTerm(String strToSearch, int[] kbid, Context context, 
      List<Variable> varList, int resultLimit, boolean useNegation, 
      int maxEditDistance, boolean startWith) throws OntoquestException;
  
  /**
   * Given a node name, find its matched node IDs. For each ID, find its 
   * first-order neighbors.
   * @param nodeName term name, it can be a class name, property name, individual
   * name, or literal. The name must exactly match to what is stored in database.
   * @param edgeDirection one of the following values: EDGE_INCOMING, EDGE_OUTGOING,
   * and EDGE_BOTH.
   * @param kbid the ids of the knowledge bases to be searched. search all
   * knowledge bases if kbid is null or empty.
   * @param pnames the list of property names. If null or empty, search edges
   * along all possible properties. Otherwise, search along those specified
   * properties only.
   * @param context query context
   * @param varList the variables to be bound.
   * @return a Resource set of edges. Each row contains rid1, rtid1, name1,
   * rid2, rtid2, name2, pid, pname. (rid1, rtid1) is the composite id of 
   * node1. name1 is node1's display text. (rid2, rtid2) is the composite 
   * id of node2. name2 is node2's display text. pid is the property
   * id and pname is the property's name. The data types are (int, int, String,
   * int, int, String, int, String).
   * @throws OntoquestException
   */
//  public ResourceSet getFirstNeighbors(String nodeName, int edgeDirection, 
//      int[] kbid, String[] pnames, Context context, List<Variable> varList) throws OntoquestException;

  /**
   * Given a group of node names, find their matched node IDs. For each ID,
   * find its first-order neighbors.
   * @param nodeNames term names. A term can be a class name, property name, individual
   * name, or literal. The name must exactly match to what is stored in database.
   * @param edgeDirection one of the following values: EDGE_INCOMING, EDGE_OUTGOING,
   * and EDGE_BOTH.
   * @param kbid the ids of the knowledge bases to be searched. search all
   * knowledge bases if kbid is null or empty.
   * @param pnames the list of property names. If null or empty, search edges
   * along all possible properties. Otherwise, search along those specified
   * properties only.
   * @param context query context
   * @param varList the variables to be bound.
   * @return a Resource set of edges. Each row contains rid1, rtid1, name1,
   * rid2, rtid2, name2, pid, pname. (rid1, rtid1) is the composite id of 
   * node1. name1 is node1's display text. (rid2, rtid2) is the composite 
   * id of node2. name2 is node2's display text. pid is the property
   * id and pname is the property's name. The data types are (int, int, String,
   * int, int, String, int, String).
   * @throws OntoquestException
   */
//  public ResourceSet getFirstNeighbors(String[] nodeNames, int edgeDirection, 
//      int[] kbid, String[] pnames, Context context, List<Variable> varList) throws OntoquestException;

  /**
   * list all knowledge bases names and IDs.
   * @param context query context
   * @param varList the variables to bind to the results
   * @return a ResourceSet of (int, string). The first column is the ID of
   * knowledge base (kbid), the second is the kb's name.
   * @throws OntoquestException
   */
  public ResourceSet listKnowledgeBases(Context context, List<Variable> varList) throws OntoquestException;

  /**
   * list all top-level resources in subClassOf hierarchy. 
   * @param kbid the knowledge base id
   * @param excludeOWLThing if true, don't consider owl:Thing as root.
   * @param prefLabel if true, prefer to use the class' rdfs:label property as its name.
   * if false, just use class' name. In some ontologies, e.g. SAO, class' name is actually 
   * an id. The real name is its rdfs:label field.
   * @param context query context
   * @return a ResourceSet of nodes. Each row includes three values: rid, rtid, and label.
   * The data type is (int, int, String). 
   * @throws OntoquestException
   */
  public ResourceSet listRootResources(int kbid, boolean excludeOWLThing, 
      boolean prefLabel, Context context, List<Variable> varList) throws OntoquestException;
  
}
