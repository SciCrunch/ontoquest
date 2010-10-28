package edu.sdsc.ontoquest.db.functions;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: GetDagLCA.java,v 1.1 2010-10-28 06:30:12 xqian Exp $
 *
 */
public class GetDagLCA implements OntoquestFunction<ResourceSet> {
    private int[][] nodeIDs;
    private int pid;
    private boolean excludeHiddenRelationship = true;
    private boolean allowSubproperties = false;

//    boolean prefLabel = false, directOnly = false, namedClassOnly = false;
  
    /**
     * Get the Least Common Ancestor (LCA) of input nodeIDs in the Directed Acyclic Graph (DAG) 
     * in which the edges are connected by the pid.
     * @param nodeIDs array of int[2]. Each int[2] represents a node id, the first 
     * int is rid, and the second id rtid.
     * @param pid the id of the property for the DAG.
     * @param allowSubproperties If true, the DAG includes subproperties of the pid.
     * @param excludeHiddenRelationship If true, no hidden edges in the DAG.
     */
    public GetDagLCA(int[][] nodeIDs, int pid, boolean allowSubproperties, boolean excludeHiddenRelationship) {
      this.nodeIDs = nodeIDs;
      this.pid = pid;
      this.excludeHiddenRelationship = excludeHiddenRelationship;
      this.allowSubproperties = allowSubproperties;
    }
    
    public ResourceSet execute(Context context, List<Variable> varList) throws OntoquestException {
      String queryName = "query.getDagLCA";
      StringBuilder sb = new StringBuilder();
      for (int i=0; i<nodeIDs.length; i++) {
        sb.append('{').append(nodeIDs[i][0]).append(',').append(nodeIDs[i][1]).append('}');
        sb.append(',');
      }
      sb.deleteCharAt(sb.length()-1);
      sb.insert(0, '{');
      sb.append('}');
      String[] args = new String[4];
      args[0] = sb.toString();
      args[1] = String.valueOf(pid);
      args[2] = String.valueOf(allowSubproperties);
      args[3] = String.valueOf(excludeHiddenRelationship);
      return DbUtility.executeSQLCommandName(queryName, context, varList, args, 
          "Failed to get the least common ancestor (LCA) of nodes: "+sb);
    }

  }
