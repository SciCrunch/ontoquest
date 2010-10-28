import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.configuration.Configuration;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbContext;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.db.functions.GetCenterPieceSubgraph;
import edu.sdsc.ontoquest.graph.Graph;
import edu.sdsc.ontoquest.query.Utility;

/**
 * @version $Id: RunCenterPieceSubgraph.java,v 1.1 2010-10-28 06:30:02 xqian Exp $
 *
 */
public class RunCenterPieceSubgraph {
  OntoquestFunction<Graph> f = null;

  int[] kbid = null;

  int maxPathLen = 10;

//  int[][] nodeIDs = null;

  //  int budget = nodeIDs.length * 4; // max # of nodes in result graph
  int budget = 20; // default budget size
  Graph graph = null;

  protected String configFileName = "config/ontoquest.xml";

  protected Context context;

  public RunCenterPieceSubgraph() throws Exception {
    setUp();
  }
  
  public void setUp() throws Exception {
    // initialize configuration
    AllConfiguration.initialize(configFileName);
    // initialize database connection pool
    Configuration config = AllConfiguration.getConfig();
    String driver = config.getString("database.driver");
    String url = config.getString("database.url");
    String user = config.getString("database.user");
    String password = config.getString("database.password");
    DbConnectionPool conPool = new DbConnectionPool(driver, url, user, password);
    context = new DbContext(conPool);

    // load graph
    graph = DbUtility.loadGraph(kbid, (DbContext) context, true);
  }

  public int[][] generateRandomInputNodes(int desiredNodeCount, int maxID) throws Exception {
    int[][] nids = new int[desiredNodeCount][2];
    Random random = new Random();
    int max = maxID-1;
    for (int i=0; i<desiredNodeCount; i++) {
      nids[i][0] = 1+random.nextInt(max);
      nids[i][1] = 1;
    }
    return nids;
//    nodeIDs = nids;
  }

  public int[][] loadInputNodesFromFile(String fileName) throws Exception {
    LinkedList<int[]> inputNodes = new LinkedList<int[]>();
    BufferedReader input = new BufferedReader(new FileReader(fileName));
    try {
      String line = null;
      while ((line=input.readLine()) != null) {
        String[] sarray = line.split("\\s");
        if (sarray == null || sarray.length != 2)
          throw new Exception("Wrong format of input nodes file! Expect 'integer1 integer2'");
        int rid = Integer.parseInt(sarray[0].trim());
        int rtid = Integer.parseInt(sarray[1].trim());
        inputNodes.add(new int[]{rid, rtid});
      }
    } finally {
      input.close();
    }
    int[][] nodeIDs = new int[inputNodes.size()][2];
    inputNodes.toArray(nodeIDs);

    return nodeIDs;
  }

//  public void run(int desiredNodeCount) throws Exception {
//    // generate input nodes randomly.
//    int[][] nodeIDs = generateRandomInputNodes(desiredNodeCount, graph.getNodeCount());
//    
//    run(graph, nodeIDs);
//  }
//  
//  public void run(String inputNodeFileName) throws Exception {
//    
//    int[][] nodeIDs = loadInputNodesFromFile(inputNodeFileName);
//    run(graph, nodeIDs);
//  }
//  
  
  public void run(int[][] queryNodeIDs) throws Exception {
    f = new GetCenterPieceSubgraph(graph, maxPathLen, budget, queryNodeIDs);
    Graph result = f.execute(context, null);

    ResourceSet nodeRS = result.getNodes();
    while (nodeRS.next()) {
      int rid = nodeRS.getInt(1);
      int rtid = nodeRS.getInt(2);
      System.out.println("node -- " + rid + "   " + rtid);
    }
    nodeRS.close();

    ResourceSet edgeRS = result.getEdges(false);
    while (edgeRS.next()) {
      int rid1 = edgeRS.getInt(1);
      int rtid1 = edgeRS.getInt(2);
      int rid2 = edgeRS.getInt(3);
      int rtid2 = edgeRS.getInt(4);
      int pid = edgeRS.getInt(5);
      System.out.println("edge -- " + rid1 + "  " + rtid1 + "  " + rid2 + "  "
          + rtid2 + "  " + pid);
    }
    edgeRS.close();
  }

  public static void main(String[] args) {
    try {
      RunCenterPieceSubgraph program = new RunCenterPieceSubgraph();
      if (args.length < 1) {
        System.out.println("Usage: java -cp <CLASSPATH> RunCenterPieceSubraph queryNodeFile|desiredNodeIDs");
        System.exit(0);
      }
      int[][] queryNodeIDs = null;
      try {
        int desiredInputNodeCount = Integer.parseInt(args[0]);
        queryNodeIDs = program.generateRandomInputNodes(desiredInputNodeCount, program.graph.getNodeCount());
      } catch (Exception e) {
        // not a number, treat it as a file name
        queryNodeIDs = program.loadInputNodesFromFile(args[0]);
      }
      program.run(queryNodeIDs);
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
