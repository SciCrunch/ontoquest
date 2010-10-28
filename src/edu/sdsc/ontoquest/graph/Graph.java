package edu.sdsc.ontoquest.graph;

import java.util.HashMap;
import java.util.List;

import edu.sdsc.ontoquest.ResourceSet;

/**
 * @version $Id: Graph.java,v 1.1 2010-10-28 06:30:34 xqian Exp $
 *
 */
public interface Graph {

  public int getNodeCount();
  
  public int getEdgeCount();
  
  public ResourceSet getEdges(boolean includeWeight);
  
  /**
   * @return array of node ids. The size is int[n][2], n = number of nodes. int[i][0] = rid(i), 
   * int[i][1] = rtid(i).
   */
  public int[][] getNodesAsArray();

  public ResourceSet getEdges(int rid1, int rtid1, int rid2, int rtid2, boolean isDirected);
  
  public List<int[]> getEdgesAsArray(int rid1, int rtid1, int rid2, int rtid2, boolean isDirected);
  
  /**
   * @return a resource set of node ids. Each row in resource set contains two integers: rid and rtid
   */
  public ResourceSet getNodes();
  
  public boolean containsNode(int rid, int rtid);
  
  public boolean containsEdge(int rid1, int rtid1, int rid2, int rtid2, int pid);
  
  public boolean containsEdge(int rid1, int rtid1, int rid2, int rtid2, int pid, boolean isDirected);
  
  public boolean addEdge(int rid1, int rtid1, int rid2, int rtid2, int pid);
  
  public boolean addEdge(int rid1, int rtid1, int rid2, int rtid2, int pid, float weight);

  public boolean addNode(int rid, int rtid);
  
  /**
   * Gets the edge weight as an N by N matrix. The index of nodes is stored in
   * nodeIdxMap. If the given nodeIdxMap is not empty initially, it will be
   * emptied first.
   * @return
   */
  public double[][] getWeightMatrix(HashMap<Long, Integer> nodeIdxMap);
  
  public long makeKey(int rid, int rtid);
  
  public int parseRID(long key);
  
  public int parseRTID(long key);

  }
