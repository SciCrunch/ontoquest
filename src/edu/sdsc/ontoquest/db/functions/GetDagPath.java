package edu.sdsc.ontoquest.db.functions;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: GetDagPath.java,v 1.2 2012-04-30 22:42:03 xqian Exp $
 *
 */
public class GetDagPath implements OntoquestFunction<ResourceSet> {
  private int rid1, rtid1, rid2, rtid2;
  private int pid;
  private boolean excludeHiddenRelationship = true;
  private boolean allowSubproperties = false;

  private final static int resultLimit = -1;

  /**
   * Get the path between node1 (rid1, rtid1) and node2 (rid2, rtid2) in the DAG.
   * @param rid1 resource id of node 1
   * @param rtid1 resource type id of node 1
   * @param rid2 resource id of node 2
   * @param rtid2 resource type id of node 2
   * @param pid the property id
   * @param allowSubproperties If true, the DAG includes subproperties of the pid.
   * @param excludeHiddenRelationship If true, no hidden edges in the DAG.
   */
  public GetDagPath(int rid1, int rtid1, int rid2, int rtid2, int pid,
                    boolean allowSubproperties, boolean excludeHiddenRelationship) {
    this.rid1 = rid1;
    this.rtid1 = rtid1;
    this.rid2 = rid2;
    this.rtid2 = rtid2;
    this.pid = pid;
    this.excludeHiddenRelationship = excludeHiddenRelationship;
    this.allowSubproperties = allowSubproperties;
  }

  public ResourceSet execute(Context context, List<Variable> varList) throws OntoquestException {
    String queryName = "query.getDagPath";
    String[] args = new String[7];
    args[0] = String.valueOf(rid1);
    args[1] = String.valueOf(rtid1);
    args[2] = String.valueOf(rid2);
    args[3] = String.valueOf(rtid2);
    args[4] = String.valueOf(pid);
    args[5] = String.valueOf(allowSubproperties);
    args[6] = String.valueOf(excludeHiddenRelationship);
    return DbUtility.executeSQLQueryName(queryName, context, varList, args, 
        "Failed to get path between node 1 (rid="+rid1+", rtid="+rtid1+") and node 2(rid="
        +rid2+", rtid="+rtid2+")", resultLimit);

  }
}
