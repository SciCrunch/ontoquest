package edu.sdsc.ontoquest.db.functions;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: EstimateNeighborCount.java,v 1.2 2012-04-30 22:42:03 xqian Exp $
 *
 */
public class EstimateNeighborCount implements OntoquestFunction<ResourceSet> {

  public static final int EDGE_INCOMING = 1;
  public static final int EDGE_OUTGOING = -1;
  public static final int EDGE_BOTH = 0;
  int kbid = -1;
  String[] includedProperties = null;
  String[] terms = null;
  int rid = -1, rtid = -1;
//  int[][] nodeIds = null;
  boolean isSynonymIncluded = false;
  boolean excludeHiddenRelationship = true;
  boolean allowSubproperties = false;
  int edgeDirection = EDGE_BOTH;
  int level = 1; // If level=1, get direct neighbors. If level > 2, get neighbors and their neighbors.

  /**
   * Estimate the count of neighbors. 
   * @param terms the query terms
   * @param kbid the knowledge base id
   * @param includedProperties the properties to be included. NULL or empty array means including all properties
   * @param isSynonymIncluded the properties to be excluded.
   * @param edgeDirection one of EDGE_INCOMING, EDGE_OUTGOING, and EDGE_BOTH
   * @param excludeHiddenRelationship if true, no hidden relationships in the result.
   * @param level the level of neighbors (descendants/ancestors). If 0, include all levels of neighbors.
   * @param allowSubproperties if true, include the neighbors which are connected by a subproperty of the includedProperties.
   */
  public EstimateNeighborCount(String[] terms, int kbid, 
      String[] includedProperties, boolean isSynonymIncluded, int edgeDirection, 
      boolean excludeHiddenRelationship, int level, boolean allowSubproperties) {
    this.terms = terms;
    setParameters(kbid, includedProperties, isSynonymIncluded, 
        edgeDirection, excludeHiddenRelationship, level, allowSubproperties);
  }

  public EstimateNeighborCount(int rid, int rtid, int kbid,
      String[] includedProperties, int edgeDirection, 
      boolean excludeHiddenRelationship, int level, boolean allowSubproperties) {
    this.rid = rid;
    this.rtid = rtid;
    setParameters(kbid, includedProperties, true, edgeDirection, excludeHiddenRelationship, level, allowSubproperties);
  }
  
  private void setParameters(int kbid, String[] includedProperties, 
      boolean isSynonymIncluded, int edgeDirection, boolean excludeHiddenRelationship, 
      int level, boolean allowSubproperties) {
    this.kbid = kbid;
    this.includedProperties = includedProperties;
    this.isSynonymIncluded = isSynonymIncluded;
    this.edgeDirection = edgeDirection;
    this.excludeHiddenRelationship = excludeHiddenRelationship;
    this.level = level;
    this.allowSubproperties = allowSubproperties;
  }

  /**
   * @see edu.sdsc.ontoquest.OntoquestFunction#execute(edu.sdsc.ontoquest.Context, java.util.List)
   */
  @Override
  public ResourceSet execute(Context context, List<Variable> varList)
      throws OntoquestException {
    if (rid > 0 && rtid > 0)
      return executeUsingID(context, varList);
    return executeUsingTermArray(context, varList);
  }

  private ResourceSet executeUsingTermArray(Context context,
      List<Variable> varList) throws OntoquestException {
    String queryName = null;
    if (edgeDirection == GetNeighbors.EDGE_INCOMING)
      queryName = "query.getNeighborCountInByLabel";
    else if (edgeDirection == GetNeighbors.EDGE_OUTGOING)
      queryName = "query.getNeighborCountOutByLabel";
    else
      queryName = "query.getNeighborCountBothByLabel";

    String[] args = new String[7];
    args[0] = DbUtility.toQuotedString(terms);
    args[1] = DbUtility.toQuotedString(includedProperties);
    args[2] = String.valueOf(kbid);
    args[3] = String.valueOf(level);
    args[4] = String.valueOf(excludeHiddenRelationship);
    args[5] = String.valueOf(allowSubproperties);
    args[6] = String.valueOf(isSynonymIncluded);

    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to get neighbor count of term(s)("+args[0]+") (kbid="+kbid+")", -1);

  }

  private ResourceSet executeUsingID(Context context,
      List<Variable> varList) throws OntoquestException {
    String queryName = null;
    if (edgeDirection == GetNeighbors.EDGE_INCOMING)
      queryName = "query.getNeighborCountInByID";
    else if (edgeDirection == GetNeighbors.EDGE_OUTGOING)
      queryName = "query.getNeighborCountOutByID";
    else
      queryName = "query.getNeighborCountBothByID";

    String[] args = new String[7];
    args[0] = String.valueOf(rid);
    args[1] = String.valueOf(rtid);
    args[2] = String.valueOf(level);
    args[3] = String.valueOf(excludeHiddenRelationship);
    args[4] = String.valueOf(allowSubproperties);
    args[5] = DbUtility.formatArrayToSingleQuotedStr(includedProperties);
    args[6] = String.valueOf(kbid);

    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to get neighbor count of term(s)("+args[0]+") (kbid="+kbid+")", -1);

  }

}
