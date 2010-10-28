package edu.sdsc.ontoquest.db.functions;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: GetNeighbors.java,v 1.1 2010-10-28 06:30:11 xqian Exp $
 *
 */
public class GetNeighbors implements OntoquestFunction<ResourceSet> {
  public static final int EDGE_INCOMING = 1;
  public static final int EDGE_OUTGOING = -1;
  public static final int EDGE_BOTH = 0;
  public enum PropertyType {OBJECT, DATATYPE, ANNOTATION, SYSTEM, ALL};
  
  int kbid = -1;
  String[] includedProperties = null;
  String[] excludedProperties = null;
  String[] terms = null;
  int[][] nodeIds = null;
  boolean isSynonymIncluded = false;
  boolean isClassOnly = true;
  boolean excludeHiddenRelationship = true;
  boolean allowSubproperties = false;
  boolean searchByName = false;
  int edgeDirection = EDGE_BOTH;
  int level = 1; // If level=1, get direct neighbors. If level > 2, get neighbors and their neighbors.
  PropertyType[] propTypes = null;
  
  /**
   * Get neighbors of the input node. Exclude hidden edges.
   * @param term the input term node
   * @param kbid knowledge base id, positive integer means the specified knowledge base. 0 means same knowledge base as the (rid, rtid), -1 means all knowledge bases. 
   * @param includedProperties the properties to be included. NULL or empty array means including all properties
   * @param excludedProperties the properties to be excluded.
   * @param isSynonymIncluded If true, search the term as a synonym as well.
   * @param edgeDirection one of EDGE_INCOMING, EDGE_OUTGOING, EDGE_BOTH
   * @param level the level of neighbors (descendants/ancestors). If 0, include all levels of neighbors.
   * @param allowSubproperties if true, include the neighbors which are connected by a subproperty of the includedProperties.
   */
  public GetNeighbors(String term, int kbid, 
      String[] includedProperties, String[] excludedProperties, boolean isSynonymIncluded, 
      int edgeDirection, int level, boolean allowSubproperties) {
    this(new String[]{term}, kbid, includedProperties, excludedProperties,
        isSynonymIncluded, edgeDirection, level, allowSubproperties);
  }
  
  /**
   * Get neighbors of the input nodes. Exclude hidden edges.
   * @param terms the input terms
   * @param kbid knowledge base id, positive integer means the specified knowledge base. 0 means same knowledge base as the (rid, rtid), -1 means all knowledge bases. 
   * @param includedProperties the properties to be included. NULL or empty array means including all properties
   * @param excludedProperties the properties to be excluded.
   * @param isSynonymIncluded If true, search the term as a synonym as well.
   * @param edgeDirection one of EDGE_INCOMING, EDGE_OUTGOING, EDGE_BOTH
   * @param level the level of neighbors (descendants/ancestors). If 0, include all levels of neighbors.
   * @param allowSubproperties if true, include the neighbors which are connected by a subproperty of the includedProperties.
   */
  public GetNeighbors(String[] terms, int kbid, 
      String[] includedProperties, String[] excludedProperties, boolean isSynonymIncluded, 
      int edgeDirection, int level, boolean allowSubproperties) {
    this(terms, kbid, includedProperties, excludedProperties, 
        isSynonymIncluded, edgeDirection, true, true, level, allowSubproperties);
  }
    
  /**
   * Get neighbors of the input nodes.
   * @param terms the input terms
   * @param kbid the knowledge base id
   * @param includedProperties the properties to be included. NULL or empty array means including all properties
   * @param excludedProperties the properties to be excluded.
   * @param isSynonymIncluded If true, search the term as a synonym as well.
   * @param edgeDirection one of EDGE_INCOMING, EDGE_OUTGOING, EDGE_BOTH
   * @param excludeHiddenRelationship If true, exclude hidden relationships.
   * @param isClassOnly If true, only fetch neighbors which are classes.
   * @param level the level of neighbors (descendants/ancestors). If 0, include all levels of neighbors.
   * @param allowSubproperties if true, include the neighbors which are connected by a subproperty of the includedProperties.
   */
  public GetNeighbors(String[] terms, int kbid, 
      String[] includedProperties,  String[] excludedProperties, boolean isSynonymIncluded, int edgeDirection, 
      boolean excludeHiddenRelationship, boolean isClassOnly, int level, boolean allowSubproperties) {
    this(terms, kbid, includedProperties, excludedProperties, isSynonymIncluded, edgeDirection,
        excludeHiddenRelationship, isClassOnly, level, allowSubproperties, new PropertyType[]{PropertyType.ALL});
  }
  
  public GetNeighbors(String[] terms, int kbid, 
      String[] includedProperties,  String[] excludedProperties, boolean isSynonymIncluded, int edgeDirection, 
      boolean excludeHiddenRelationship, boolean isClassOnly, int level, boolean allowSubproperties, PropertyType[] propTypes) {
    this.terms = terms;
    setParameters(kbid, includedProperties, excludedProperties, isSynonymIncluded, 
        edgeDirection, excludeHiddenRelationship, isClassOnly, level, allowSubproperties, propTypes);
  }

  public GetNeighbors(String[] terms, int kbid, boolean searchByName,
      String[] includedProperties,  String[] excludedProperties, int edgeDirection, 
      boolean excludeHiddenRelationship, boolean isClassOnly, int level, boolean allowSubproperties) {
    this(terms, kbid, searchByName, includedProperties, excludedProperties, edgeDirection,
        excludeHiddenRelationship, isClassOnly, level, allowSubproperties, new PropertyType[]{PropertyType.ALL});
  }
  
  public GetNeighbors(String[] terms, int kbid, boolean searchByName,
      String[] includedProperties,  String[] excludedProperties, int edgeDirection, 
      boolean excludeHiddenRelationship, boolean isClassOnly, int level, boolean allowSubproperties, PropertyType[] propTypes) {
    this.terms = terms;
    this.searchByName = searchByName;
    setParameters(kbid, includedProperties, excludedProperties, false, 
        edgeDirection, excludeHiddenRelationship, isClassOnly, level, allowSubproperties, propTypes);
  }

  
  /**
   * Get neighbors of the input node identified by id (rid, rtid)
   * @param rid resource id. A pair of (rid, rtid) identifies a node.
   * @param rtid resource type id
   * @param kbid knowledge base id, positive integer means the specified knowledge base. 0 means same knowledge base as the (rid, rtid), -1 means all knowledge bases. 
   * @param includedProperties the properties to be included. NULL or empty array means including all properties
   * @param excludedProperties the properties to be excluded.
   * @param edgeDirection one of EDGE_INCOMING, EDGE_OUTGOING, EDGE_BOTH
   * @param excludeHiddenRelationship If true, exclude hidden relationships.
   * @param isClassOnly If true, only fetch neighbors which are classes.
   * @param level the level of neighbors (descendants/ancestors). If 0, include all levels of neighbors.
   * @param allowSubproperties if true, include the neighbors which are connected by a subproperty of the includedProperties.
   */
  public GetNeighbors(int rid, int rtid, int kbid, 
      String[] includedProperties, String[] excludedProperties, int edgeDirection, 
      boolean excludeHiddenRelationship, boolean isClassOnly, int level, boolean allowSubproperties) {
    this(new int[][]{{rid, rtid}}, kbid, includedProperties, excludedProperties, edgeDirection,
        excludeHiddenRelationship, isClassOnly, level, allowSubproperties); 
  }
  
  /**
   * Get neighbors of the input nodes.
   * @param nodeIDs array of int[2]. Each int[2] represents a node id, the first 
     * int is rid, and the second id rtid.
   * @param kbid knowledge base id, positive integer means the specified knowledge base. 0 means same knowledge base as the (rid, rtid), -1 means all knowledge bases. 
   * @param includedProperties the properties to be included. NULL or empty array means including all properties
   * @param excludedProperties the properties to be excluded.
   * @param edgeDirection one of EDGE_INCOMING, EDGE_OUTGOING, EDGE_BOTH
   * @param excludeHiddenRelationship If true, exclude hidden relationships.
   * @param isClassOnly If true, only fetch neighbors which are classes.
   * @param level the level of neighbors (descendants/ancestors). If 0, include all levels of neighbors.
   * @param allowSubproperties if true, include the neighbors which are connected by a subproperty of the includedProperties.
   */
  public GetNeighbors(int[][] nodeIDs, int kbid,
      String[] includedProperties, String[] excludedProperties, int edgeDirection, 
      boolean excludeHiddenRelationship, boolean isClassOnly, int level, boolean allowSubproperties) {
    this(nodeIDs, kbid, includedProperties, excludedProperties, edgeDirection,
        excludeHiddenRelationship, isClassOnly, level, allowSubproperties, new PropertyType[]{PropertyType.ALL});
  }
  
  public GetNeighbors(int[][] nodeIDs, int kbid,
      String[] includedProperties, String[] excludedProperties, int edgeDirection, 
      boolean excludeHiddenRelationship, boolean isClassOnly, int level, boolean allowSubproperties, PropertyType[] propTypes) {
    this.nodeIds = nodeIDs;
    setParameters(kbid, includedProperties, excludedProperties, false, 
        edgeDirection, excludeHiddenRelationship, isClassOnly, level, allowSubproperties, propTypes);
  }

  private void setParameters(int kbid, String[] includedProperties, String[] excludedProperties,
      boolean isSynonymIncluded, int edgeDirection, boolean excludeHiddenRelationship, 
      boolean isClassOnly, int level, boolean allowSubproperties, PropertyType[] propTypes) {
    this.kbid = kbid;
    this.includedProperties = includedProperties;
    this.excludedProperties = excludedProperties;
    this.isSynonymIncluded = isSynonymIncluded;
    this.edgeDirection = edgeDirection;
    this.excludeHiddenRelationship = excludeHiddenRelationship;
    this.isClassOnly = isClassOnly;
    this.level = level;
    this.allowSubproperties = allowSubproperties;
    this.propTypes = propTypes;
  }
  
  public ResourceSet execute(Context context, List<Variable> varList)
      throws OntoquestException {
    // if id is known, use different query template.
    if (nodeIds != null && nodeIds.length > 0) 
      return executeUsingIDArray(context, varList);
    else if (terms != null && terms.length > 0) {
      return searchByName?executeUsingNameArray(context, varList)
          :executeUsingTermArray(context, varList);
    } else
      throw new OntoquestException("No search term or id is specified!");
  }

  private ResourceSet executeUsingNameArray(Context context,
      List<Variable> varList) throws OntoquestException {
    String queryName = null;
    if (edgeDirection == GetNeighbors.EDGE_INCOMING)
      queryName = "query.getNeighborInByName";
    else if (edgeDirection == GetNeighbors.EDGE_OUTGOING)
      queryName = "query.getNeighborOutByName";
    else
      queryName = "query.getNeighborBothByName";

    String[] args = new String[10];
    args[0] = DbUtility.toQuotedString(terms);
    args[1] = DbUtility.toQuotedString(includedProperties);
    args[2] = DbUtility.toQuotedString(excludedProperties);
    args[3] = String.valueOf(kbid);
    args[4] = String.valueOf(level);
    args[5] = String.valueOf(excludeHiddenRelationship);
    args[6] = String.valueOf(allowSubproperties);
    args[7] = String.valueOf(isClassOnly);
    args[8] = String.valueOf(isSynonymIncluded);
    args[9] = composePropertyType();

    return DbUtility.executeSQLCommandName(queryName, context, varList, args, 
        "Failed to get neighbors of term(s)("+args[0]+") (kbid="+kbid+")");

  }

  private ResourceSet executeUsingTermArray(Context context,
      List<Variable> varList) throws OntoquestException {
    String queryName = null;
    if (edgeDirection == GetNeighbors.EDGE_INCOMING)
      queryName = "query.getNeighborInByLabel";
    else if (edgeDirection == GetNeighbors.EDGE_OUTGOING)
      queryName = "query.getNeighborOutByLabel";
    else
      queryName = "query.getNeighborBothByLabel";

    String[] args = new String[10];
    args[0] = DbUtility.toQuotedString(terms);
    args[1] = DbUtility.toQuotedString(includedProperties);
    args[2] = DbUtility.toQuotedString(excludedProperties);
    args[3] = String.valueOf(kbid);
    args[4] = String.valueOf(level);
    args[5] = String.valueOf(excludeHiddenRelationship);
    args[6] = String.valueOf(allowSubproperties);
    args[7] = String.valueOf(isClassOnly);
    args[8] = String.valueOf(isSynonymIncluded);
    args[9] = composePropertyType();

    return DbUtility.executeSQLCommandName(queryName, context, varList, args, 
        "Failed to get neighbors of term(s)("+args[0]+") (kbid="+kbid+")");

  }
        
  private ResourceSet executeUsingIDArray(Context context,
      List<Variable> varList) throws OntoquestException {
    String queryName = null;
    if (edgeDirection == GetNeighbors.EDGE_INCOMING)
      queryName = "query.getNeighborInByIDArray";
    else if (edgeDirection == GetNeighbors.EDGE_OUTGOING)
      queryName = "query.getNeighborOutByIDArray";
    else
      queryName = "query.getNeighborBothByIDArray";

    String[] args = new String[9];
    StringBuilder sb = new StringBuilder();
    for (int[] nid : nodeIds) {
      sb.append('{').append(nid[0]).append(',').append(nid[1]).append('}').append(',');
    }
    if (sb.length() > 0)
      sb.deleteCharAt(sb.length()-1);
    args[0] = sb.toString();
    args[1] = DbUtility.toQuotedString(includedProperties);
    args[2] = DbUtility.toQuotedString(excludedProperties);
    args[3] = String.valueOf(kbid);
    args[4] = String.valueOf(level);
    args[5] = String.valueOf(excludeHiddenRelationship);
    args[6] = String.valueOf(allowSubproperties);
    args[7] = String.valueOf(isClassOnly);
    args[8] = composePropertyType();

    return DbUtility.executeSQLCommandName(queryName, context, varList, args, 
        "Failed to get neighbors of term(s)("+args[0]+") (kbid="+kbid+")");
  }

  private String composePropertyType() {
    String result = "";
    String head = ", property p where t.pid = p.id and (";
    StringBuilder sb = new StringBuilder();
    if (propTypes == null)
      return result;
    for (PropertyType propType : propTypes) {
      if (propType == PropertyType.ALL) {
        return "";  // ignore all other flags if All is requested.
      } else if (propType == PropertyType.OBJECT) {
        sb.append(" or p.is_object = true");
      } else if (propType == PropertyType.DATATYPE) {
        sb.append(" or p.is_datatype = true");
      } else if (propType == PropertyType.ANNOTATION) {
        sb.append(" or p.is_annotation = true");
      } else if (propType == PropertyType.SYSTEM) {
        sb.append(" or p.is_system = true");
      }
    }
    if (sb.length() > 0) {
      result = head + sb.substring(" or ".length()-1) + ")";
    }
    return result;
  }
}
