package edu.sdsc.ontoquest.db.functions;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Variable;

/**
 * This function is to check if an input class has inferred definition.
 * If a named class (input class) has an equivalent anonymous class which 
 * is defined as some kind of restriction or set class, the result is true.
 * False other wise. 
 * <p>@version $Id: CheckInferredDefinition.java,v 1.2 2012-04-30 22:42:02 xqian Exp $
 *
 */
public class CheckInferredDefinition implements OntoquestFunction<ResourceSet> {

  private String[] terms = null;
  private int[][] nodeIds = null;
  private int kbid = -1;
  private boolean isSynonymIncluded = false;
  
  public CheckInferredDefinition(String[] terms, int kbid, boolean isSynonymIncluded) {
    this.terms = terms;
    this.kbid = kbid;
    this.isSynonymIncluded = isSynonymIncluded;
  }
  
  public CheckInferredDefinition(int[][] nodeIds) {
    this.nodeIds = nodeIds;
  }
  
  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.OntoquestFunction#execute(edu.sdsc.ontoquest.Context, java.util.List)
   */
  @Override
  public ResourceSet execute(Context context, List<Variable> varList)
      throws OntoquestException {
    if (nodeIds != null && nodeIds.length > 0)
      return executeUsingIDs(context, varList);
    return executeUsingTerms(context, varList);
  }

  private ResourceSet executeUsingIDs(Context context, List<Variable> varList)
      throws OntoquestException {
    String queryName = "query.checkInferredDefByIDs";
    String[] args = new String[1];
    StringBuilder sb = new StringBuilder();
    for (int[] nid : nodeIds) {
      sb.append('{').append(nid[0]).append(',').append(nid[1]).append('}').append(',');
    }
    if (sb.length() > 0)
      sb.deleteCharAt(sb.length()-1);
    args[0] = sb.toString();

    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to check inferred classes of term IDs ("+args[0]+")");
  }
  
  private ResourceSet executeUsingTerms(Context context, List<Variable> varList)
      throws OntoquestException {
    String queryName = "query.checkInferredDefByTerms";
    String[] args = new String[3];
    args[0] = DbUtility.toQuotedString(terms);
    args[1] = String.valueOf(isSynonymIncluded);
    args[2] = String.valueOf(kbid);
    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to check inferred classes of term IDs ("+args[0]+")");
  }
}
