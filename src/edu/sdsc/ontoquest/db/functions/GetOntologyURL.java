package edu.sdsc.ontoquest.db.functions;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: GetOntologyURL.java,v 1.1 2010-10-28 06:30:14 xqian Exp $
 *
 */
public class GetOntologyURL implements OntoquestFunction<ResourceSet>{

  int[][] nodeIDs = null;
  public GetOntologyURL(int rid, int rtid) {
    nodeIDs = new int[][]{{rid, rtid}};
  }
  
  public GetOntologyURL(int[][] nodeIDs) {
    this.nodeIDs = nodeIDs;
  }
  
  /**
   * @see edu.sdsc.ontoquest.OntoquestFunction#execute(edu.sdsc.ontoquest.Context, java.util.List)
   */
  @Override
  public ResourceSet execute(Context context, List<Variable> varList)
      throws OntoquestException {
    String queryName = "query.getURLByIDs";
    String[] args = new String[2];
    StringBuilder sb = new StringBuilder();
    for (int[] nid : nodeIDs) {
      sb.append('{').append(nid[0]).append(',').append(nid[1]).append('}').append(',');
    }
    if (sb.length() > 0)
      sb.deleteCharAt(sb.length()-1);
    args[0] = sb.toString();
    args[1] = String.valueOf(nodeIDs.length);
    return DbUtility.executeSQLCommandName(queryName, context, varList, args, 
        "Failed to get URL of the nodes -- " + args[0]);
  }

}
