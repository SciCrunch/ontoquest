package edu.sdsc.ontoquest.db.functions;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import Jama.Matrix;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestException.Type;
import edu.sdsc.ontoquest.graph.Graph;
import edu.sdsc.ontoquest.graph.InMemoryAdjMapGraph;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: GetCenterPieceSubgraph.java,v 1.1 2010-10-28 06:30:12 xqian Exp $
 *
 */
public class GetCenterPieceSubgraph implements OntoquestFunction<Graph> {

  private int maxPathLen = 10; // maximum allowable path length
  private Graph graph = null;
  private int budget = 40; // default budget
  private int[][] queryNodes = null; // query nodes [Q][2]
  private int lastPdIndex = -1; // index of previous pd in rQArray
  private float cValue = 0.6f; // probability to return to node qi
  private double tolerance = 10^-9;
  private int maxIter = 80;

  /**
   * Given a list of nodes, find the center piece graph.
   * @param graph
   * @param maxPathLen
   * @param budget
   * @param queryNodes set of query nodes with size Q. For node i, i = 0,...,Q-1,
   * queryNodes[i][0] is its rid, queryNodes[i][1] is its rtid.
   */
  public GetCenterPieceSubgraph(Graph graph, int maxPathLen, int budget, int[][] queryNodes) {
    this.graph = graph;
    this.budget = budget;
    this.maxPathLen = maxPathLen;
    this.queryNodes = queryNodes;
  }

  /**
   * Use Center-piece subgraphs algorithm by Hanghang Tong and Christos Faloutsos
   * (Center-Piece Subgraphs: Problem Definition and Fast Solutions) on
   * KDD'06.
   */
  public Graph execute(Context context, List<Variable> varList) throws OntoquestException {
    if (graph.getEdgeCount() <= 0)
      throw new OntoquestException(Type.INPUT, "Empty graph!");
    
    HashMap<Long, Integer> nodeIdxMap = new HashMap<Long, Integer>();

    // step 1: individual score calculation
    Entry[][] rArray = calculateIndividualScore(nodeIdxMap);

    // sort rArray[i] (i=0,...,Q-1) by r(i, j)
    for (int i=0; i<rArray.length; i++) {
      Arrays.sort(rArray[i], new RComparator(i));
    }

    // step 2: combining individual scores
    Entry[] rQArray = combineIndividualScores(rArray);

    // sort rQArray by r(Q, j)
    Arrays.sort(rQArray, new RQComparator());

    System.out.println("rQArray =========");
    dumpRQArray(rQArray);

//    System.out.println("rArray =========");
//    dumpRArray(rArray);

    // step 3: "EXTRACT" subgraph
    return extractSubgraph(rArray, rQArray);
  }

  /**
   * Calculates individual goodness score for each query node (r(i, j)).
   * @return the matrix of sorted entries. The size of the result is
   * Q x N. Q is the size of the query nodes, and N is the size of
   * input graph.
   */
  protected Entry[][] calculateIndividualScore(HashMap<Long, Integer> nodeIdxMap) {
    double[][] weightArray = graph.getWeightMatrix(nodeIdxMap);
    Matrix weightMatrix = new Matrix(weightArray);

    // normalize weightMatrix
    Matrix normalizedWeightMatrix = normalizeByColumn(weightMatrix);

    // create and initialize E
    double[][] E = new double[weightArray.length][queryNodes.length];
    for (int i=0; i<queryNodes.length; i++) {
      int nodeIdx = nodeIdxMap.get(graph.makeKey(queryNodes[i][0], queryNodes[i][1]));
      E[nodeIdx][i] = 1;
    }

    Matrix rMatrix = getSteadyStateProbability(
          normalizedWeightMatrix, cValue, new Matrix(E));

    // dump rMatrix
//    rMatrix.print(5, 4);

    // convert rMatrix from a Matrix to Entry[][]
    HashMap<Integer, Long> reversedIdxMap = new HashMap<Integer, Long>(nodeIdxMap.size());
    for (Long key : nodeIdxMap.keySet()) {
      reversedIdxMap.put(nodeIdxMap.get(key), key);
    }

    Entry[][] result = new Entry[rMatrix.getRowDimension()][rMatrix.getColumnDimension()];
    for (int j = 0; j<rMatrix.getColumnDimension(); j++) {
      Long nodeId = reversedIdxMap.get(j);
      int rid = graph.parseRID(nodeId);
      int rtid = graph.parseRTID(nodeId);
      Entry newEntry = new Entry(rid, rtid);
      double[] rj = new double[rMatrix.getRowDimension()]; // r(i, j) (i = 1,...,Q)
      for (int i = 0; i<rMatrix.getRowDimension(); i++) {
        rj[i] = rMatrix.get(i, j);
      }
      newEntry.setR(rj);
      // initially, each row (result[i]) is the same, which contains an array of N entries.
      // Each entry included the node j's id,
      // r(i,j) (i=1,...,Q), and r(Q,j) (filled later).
      // later, each row will be sorted by r(i,j) to decide the downhill order for each i
      for (int i = 0; i < rMatrix.getRowDimension(); i++) {
        result[i][j] = newEntry;
      }
    }
    return result;
  }

  protected Matrix normalizeByColumn(Matrix w) {
    double[][] dm = new double[w.getRowDimension()][w.getColumnDimension()];
    for (int i = 0; i < w.getRowDimension(); i++) {
      double rowsum = 0;
      for (int j = 0; j < w.getColumnDimension(); j++) {
        rowsum += w.get(i, j);
      }
      dm[i][i] = rowsum;
    }
    Matrix D = new Matrix(dm);
    return w.times(D.inverse());
  }

  /**
   * Gets R matrix (Q x N)
   * @param w
   * @param c
   * @param E: N x Q matrix; ei(i = 1,...,Q)
   * @return
   */
  protected Matrix getSteadyStateProbability(Matrix w, float c, Matrix E) {
    Matrix result = new Matrix(E.getRowDimension(), E.getColumnDimension());

    for (int q=0; q<E.getColumnDimension(); q++) {
      Matrix Ei = E.getMatrix(0, E.getRowDimension()-1, q, q).copy();
      Matrix rt = Ei.copy();
      Matrix oldRt = null;

      for (int iterCount = 0; iterCount < maxIter; iterCount++) {
        oldRt = rt;

//        Matrix m1 = Ei.times(1-c);
//        Matrix m2 = w.times(c);
//        Matrix m3 = w.times(c).times(rt);
//        System.out.println("rt before");
//        rt.print(5, 4);
//        System.out.println("Ei.times(1-c)");
//        m1.print(5,4);
//        System.out.println("w.times(c)");
//        m2.print(5,4);
//        System.out.println("w.times(c).times(rt)");
//        m3.print(5, 4);
//        rt = m3.plus(m1);
//        System.out.println("rt after");
//        rt.print(5, 4);

        rt = w.times(c).times(rt).plus(Ei.times(1-c));

        Matrix diffMatrix = oldRt.minus(rt);
        double sum = 0;
        for (int i=0; i<diffMatrix.getRowDimension(); i++) {
          for (int j=0; j<diffMatrix.getColumnDimension(); j++) {
            sum += Math.abs(diffMatrix.get(i, j));
          }
        }
        if (sum < tolerance)
          break;
      }
      result.setMatrix(0, E.getRowDimension()-1, q, q, rt);
    }
    return result.transpose();
  }

  /**
   * @param rArray
   * @return
   */
  protected Entry[] combineIndividualScores(Entry[][] rArray) {
    // use one row of rArray to compute rQ. Remember rArray contains Q copy of r(i,j) matrix.
    // They are sorted by r(i, j) for different i. We just pick the first copy of r(i,j) to compute rQ.
    Entry[] rQ = new Entry[rArray[0].length]; // size: n (graph size)
    for (int j=0; j<rQ.length; j++) {
      double rQj = 1;
      double[] ri = rArray[0][j].getR();
      for (int i=0; i<ri.length; i++) {
        rQj = rQj * ri[i];
      }
      rQ[j] = rArray[0][j];
      rQ[j].setRQ(rQj);
    }
    return rQ;
  }

  protected Graph extractSubgraph(Entry[][] rArray, Entry[] rQArray) throws OntoquestException {
    Graph H = new InMemoryAdjMapGraph();

    // create a rQArray index map which finds position of node j in rQArray given its (rid, rtid)
    HashMap<Long, Integer> rQArrayIdxMap = new HashMap<Long, Integer>();
    for (int j = 0; j < rQArray.length; j++) {
      rQArrayIdxMap.put(graph.makeKey(rQArray[j].getRid(), rQArray[j].getRtid()), j);
    }

    boolean done = false;
    int iterationCount = 0;
    while (!done && H.getNodeCount() <= budget && iterationCount <= budget) {
      // pick up destination node pd
      Entry pd = pickupDestinationNode(rQArray, H);
      if (pd == null) { // no more nodes
        done = true;
        continue;
      }

      double[][] CMatrix = new double[maxPathLen][rQArray.length];
      // CPathMatrix[s][j][2] matrix records the upstream nodes to get max C value
      // CPathMatrix[s][j][0]: index of u node to get CMatrix[s][j]
      // CPathMatrix[s][j][1]: s2 value to get CMatrix[s][j]
      int[][][] CPathMatrix = new int[maxPathLen][rQArray.length][2];

      // for each active source node qi wrt node pd
      for (int i = 0; i < rArray.length; i++) {
        // make CMatrix and CPathMatrix for qi
        // CMatrix: store C[s][j] value w.r.t. qi. s: 0,...,maxPathLen-1; j: 0,...,n-1.
        // Nodes (1...N) are listed according to their position in rQArray.
        // initialize
        for (int s = 0; s < maxPathLen; s++) {
          for (int j = 0; j < rQArray.length; j++) {
            CMatrix[s][j] = 0;
            CPathMatrix[s][j][0] = -1;
            CPathMatrix[s][j][1] = -1;
          }
        }

        // discover a key path P(qi, pd)
        int[] result = discoverSingleKeyPath(CMatrix, CPathMatrix, rArray, rQArray, H, i, pd, rQArrayIdxMap);

//        System.out.println("CMatrix for " + i);
//        for (int k=0; k<CMatrix.length; k++) {
//          System.out.println(Arrays.toString(CMatrix[k]));
//        }

//        System.out.println("CPathMatrix for " + i);
//        for (int k=0; k<CPathMatrix.length; k++) {
//          System.out.println(Arrays.toString(CPathMatrix[k]));
//        }

        // add P(qi, pd) to H. If no more nodes to be added, we can stop after all query nodes are iterated.
        addPath(H, i, result[0], result[1], CPathMatrix, rQArray);
        iterationCount++;
      }
    }
    return H;
  }

  protected Entry pickupDestinationNode(Entry[] rQArray, Graph H) {
    for (int j = lastPdIndex+1; j<rQArray.length; j++) {
      // the rest of nodes have no edge with any query node.
      if ((new Double(0)).equals(rQArray[j].getRQ()))
        return null;

      if (!H.containsNode(rQArray[j].getRid(), rQArray[j].getRtid())) {
        lastPdIndex = j;
        return rQArray[j];
      }
    }
    lastPdIndex = rQArray.length-1; // no more node to pick
    return null;
  }

  /**
   *
   * @param CMatrix the extracted goodness score from qi to node u along the
   * prefix path P(i, u). The value in each slot is the score.
   * @param CPathMatrix the paths shown in CArray. The value in CPathMatrix[s][i][u]
   * is the upstream node leading to u.
   * @param rArrayi the ith subarray in rArray (sorted nodes in descending
   * order of r(i, j)(j=0,...,n-1): {u1=qi, u2,u3,...,pd=un}
   * @param rQArray r(Q, j) (j=0,...,n-1)
   * @param H output graph
   * @return int[2]. First int is the s value with which C[s][i][pdIdx] has the max value.
   * Second int is the pdIdx value.
   * @throws Exception
   */
  protected int[] discoverSingleKeyPath(double[][] CMatrix, int[][][] CPathMatrix,
      Entry[][] rArray, Entry[] rQArray, Graph H, int i, Entry pd, HashMap<Long, Integer> rQArrayIdxMap)
        throws OntoquestException {
    int s2, pdIdx = -1, maxS = -1;

    // rArray[i] is sorted in descending order of r(i,j)(j=1,...,n)
    for (int j = 0; j < rArray[i].length; j++) {
      // Table 3, 2.1: Let v = uj
      int rid_j = rArray[i][j].getRid();
      int rtid_j = rArray[i][j].getRtid();
      int jIdx = rQArrayIdxMap.get(graph.makeKey(rid_j, rtid_j));
      for (int s = 1; s < maxPathLen; s++) {
        s2 = (H.containsNode(rid_j, rtid_j)) ? s : (s-1);
        // let C[s][i][j] = max(u|u->di, j(C[s2][i][u] + rQArray[j]))
        double tmpMaxC = -1;
        int tmpMaxIdx = -1;
        // find the max tmpMaxC in j's uphill nodes
        for (int u = 0; u <= j; u++) { // j is downhill from u
          // remember nodes in CMatrix[s][i] are listed using the order of rQArray.
          // So, we need to find the position of rArray[i][u] in CMatrix.
          int uIdx = rQArrayIdxMap.get(graph.makeKey(rArray[i][u].getRid(), rArray[i][u].getRtid()));
          if (tmpMaxC < CMatrix[s2][uIdx] + rQArray[jIdx].getRQ()) {
            tmpMaxC = CMatrix[s2][uIdx] + rQArray[jIdx].getRQ();
            tmpMaxIdx = uIdx;
          }
        }
        CMatrix[s][jIdx] = tmpMaxC;
        CPathMatrix[s][jIdx][0] = tmpMaxIdx;
        CPathMatrix[s][jIdx][1] = s2;
      }

      // find pd's index in rArray[i] and CMatrix[s][i]
      if (rQArray[j].getRid() == pd.getRid() && rQArray[j].getRtid() == pd.getRtid())
        pdIdx = j;

    }
    // output the s with which CMatrix[s][i][pdIdx]/s has max value.
    double maxCValue = -1;
    for (int s = 1; s < maxPathLen; s++) {
      if (maxCValue < CMatrix[s][pdIdx]/s) {
        maxCValue = CMatrix[s][pdIdx]/s;
        maxS = s;
      }
    }

    return new int[]{maxS, pdIdx};
  }

  /**
   * Try to add path P(i, Pd). If nothing to add or the nodes and edges already exist in H, return false.
   * @param H
   * @param i
   * @param s
   * @param pdIdx
   * @param CPathMatrix
   * @param rQArray
   * @return
   * @throws OntoquestException
   */
  protected boolean addPath(Graph H, int i, int s, int pdIdx, int[][][] CPathMatrix, Entry[] rQArray)
      throws OntoquestException {
    int e1Idx = pdIdx;
    int s2 = s;
//    int e1Idx = CPathMatrix[s][pdIdx][0]; // index in rQArray
//    int s2 = CPathMatrix[s][pdIdx][1];
//    if (e1Idx < 0) return false; // no path to add
    int e2Idx = -1;
    Entry e1, e2;
    boolean result = false;
    while (true) { // trace back the path
      e1 = rQArray[e1Idx];
      e2Idx = CPathMatrix[s2][e1Idx][0];
      if (e2Idx < 0 || e1Idx == e2Idx) {
        result = result | H.addNode(e1.getRid(), e1.getRtid());
        return result;
      }
      e2 = rQArray[e2Idx];
      List<int[]> edgeList = graph.getEdgesAsArray(e1.getRid(), e1.getRtid(), e2.getRid(), e2.getRtid(), false);
      int edgeCount = 0;
      for (int[] edge : edgeList) {
        result = result | H.addEdge(edge[0], edge[1], edge[2], edge[3], edge[4]);
        edgeCount++;
      }
      if (edgeCount == 0) { // no edge between e1 and e2, just add nodes
        result = result | H.addNode(e1.getRid(), e1.getRtid());
        result = result | H.addNode(e2.getRid(), e2.getRtid());
      }
      e1Idx = e2Idx;
      s2 = CPathMatrix[s2][e1Idx][1];
    }
  }

  private void dumpRArray(Entry[][] rArray) {
    for (int j=0; j<rArray[0].length; j++) {
      System.out.print(rArray[0][j].getRid()+"|"+rArray[0][j].getRtid()+"  ");
    }
    System.out.println();

    for (int j=0; j<rArray[0].length; j++) {
      double[] ri = rArray[0][j].getR();
      for (int k=0; k<ri.length; k++) {
        System.out.print(ri[k]);
        System.out.print("   ");
      }
      System.out.println();
    }
  }

  private void dumpRQArray(Entry[] rQArray) {
    for (int j=0; j<rQArray.length; j++) {
      System.out.print(rQArray[j].getRid()+","+rQArray[j].getRtid());
      System.out.print("   ");
    }
    System.out.println();

    for (int j=0; j<rQArray.length; j++) {
      System.out.print(rQArray[j].getRQ());
      System.out.print("   ");
    }
    System.out.println();

  }

  public class Entry {
    private int rid, rtid; // id of graph node at j (j = 1,...N)
    private double rQ; // goodness score wrt query set Q
    private double[] r; // individual goodness score wrt every query node.
    public Entry(int rid, int rtid) {
      this.rid = rid;
      this.rtid = rtid;
    }

    public void setRQ(double rQ) { this.rQ = rQ; }
    public void setR(double[] r) { this.r = r; }

    /**
     * @return the rid
     */
    public int getRid() {
      return rid;
    }

    /**
     * @return the rtid
     */
    public int getRtid() {
      return rtid;
    }

    /**
     * @return the rQ
     */
    public double getRQ() {
      return rQ;
    }

    /**
     * @return the r
     */
    public double[] getR() {
      return r;
    }

    public double getRi(int i) {
      return r[i];
    }
  }

  /**
   * The comparator to compare entries by RQ value.
   * @version $Id: GetCenterPieceSubgraph.java,v 1.1 2010-10-28 06:30:12 xqian Exp $
   *
   */
  private class RQComparator implements Comparator<Entry> {

    public int compare(Entry o1, Entry o2) {
      return Double.compare(o2.getRQ(), o1.getRQ());
    }

    public boolean equals(Entry o1, Entry o2) {
      return compare(o1, o2) == 0;
    }
  }

  /**
   * The comparator to compare entries by r(i, j) score.
   */
  private class RComparator implements Comparator<Entry> {
    int i = 0; // compare the individual goodness score for query node i.
    public RComparator(int i) {
      this.i = i;
    }

    public int compare(Entry o1, Entry o2) {
      return Double.compare(o2.getRi(i), o1.getRi(i));
    }

    public boolean equals(Entry o1, Entry o2) {
      return compare(o1, o2) == 0;
    }
  }
}
