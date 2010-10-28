package edu.sdsc.ontoquest.db.functions;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbContext;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.graph.Graph;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;
import junit.framework.TestCase;

/**
 * @version $Id: GetCenterPieceSubgraphTest.java,v 1.1 2010-10-28 06:30:05 xqian Exp $
 *
 */
public class GetCenterPieceSubgraphTest extends OntoquestTestAdapter {
  OntoquestFunction<Graph> f = null;
  String[] kbNames = {"connectome"};
  int maxPathLen = 10;
  int[][] nodeIDs = new int[][]{{173, 1}, {118, 1}, {10650, 12}};
//  int[][] nodeIDs = new int[][]{{976, 1}, {1072, 1}, {1018, 1}};
//  int budget = nodeIDs.length * 4; // max # of nodes in result graph
  int budget = 12;
  
  public GetCenterPieceSubgraphTest() {}
  
  public GetCenterPieceSubgraphTest(String name) {
    super(name);
  }
  
  public void testExecute() {
    try {
      int[] kbid = new int[kbNames.length];
      for (int i = 0; i < kbNames.length; i++) {
        kbid[i] = basicFunctions.getKnowledgeBaseID(kbNames[i], context);
      }
      Graph graph = DbUtility.loadGraph(kbid, (DbContext)context, true);
      f = new GetCenterPieceSubgraph(graph, maxPathLen, budget, nodeIDs);
      Graph result = f.execute(context, null);
      
      ResourceSet nodeRS = result.getNodes();
      while (nodeRS.next()) {
        int rid = nodeRS.getInt(1);
        int rtid = nodeRS.getInt(2);
        System.out.println("node -- "+rid+"   "+rtid);
      }
      nodeRS.close();
      
      ResourceSet edgeRS = result.getEdges(false);
      while (edgeRS.next()) {
        int rid1 = edgeRS.getInt(1);
        int rtid1 = edgeRS.getInt(2);
        int rid2 = edgeRS.getInt(3);
        int rtid2 = edgeRS.getInt(4);
        int pid = edgeRS.getInt(5);
        System.out.println("edge -- " + rid1+"  "+rtid1+"  "+rid2+"  "+rtid2+"  "+pid);
      }
      edgeRS.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

}
