package edu.sdsc.ontoquest.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.memory.InMemoryResourceSet;
import edu.sdsc.ontoquest.query.Variable;

/**
 * Graph stored as an in-memory adjacency list. 
 * @version $Id: InMemoryAdjMapGraph.java,v 1.1 2010-10-28 06:30:35 xqian Exp $
 *
 */
public class InMemoryAdjMapGraph implements Graph {

  // the adjacency list to store the graph. Every entry in adjList is 
  // another list (elementList). The first element in the elementList is
  // a node (rid, rtid, pid=-1). The following elments are the nodes which
  // can be reached from the first node by an edge. Each element contains rid, rtid 
  // of the target node and pid of the edge. 
  private HashMap<Long, LinkedList<Element>> adjMap = new HashMap<Long, LinkedList<Element>>();
  private int edgeCount = 0;
  
  public InMemoryAdjMapGraph() { }
  
  public InMemoryAdjMapGraph(Graph g) {
    //TODO: convert g to adjList graph
  }
  
  /**
   * @see edu.sdsc.ontoquest.graph.Graph#addEdge(int, int, int, int, int)
   */
  public boolean addEdge(int rid1, int rtid1, int rid2, int rtid2, int pid) {
    return addEdge(rid1, rtid1, rid2, rtid2, pid, 1);
  }
  
  public boolean addEdge(int rid1, int rtid1, int rid2, int rtid2, int pid, float weight) {
    List<Element> elementList = findElementList(rid1, rtid1);
    if (elementList != null) {
      if (containsEdge(elementList, rid2, rtid2, pid))
        return false; // the edge already exists
      elementList.add(new Element(rid2, rtid2, pid, weight));
    } else {
     // rid1 and rtid1 do not exist, add a new list
     LinkedList<Element> newElementList = new LinkedList<Element>();
     newElementList.add(new Element(rid2, rtid2, pid, weight));
     adjMap.put(makeKey(rid1, rtid1), newElementList);
    }
    
    // check if we need create an entry for node2 in node list
    List<Element> elementList2 = findElementList(rid2, rtid2);
    if (elementList2 == null) {
      adjMap.put(makeKey(rid2, rtid2), new LinkedList<Element>());
    }
    edgeCount++;
    return true;
  }
  
  /**
   * @see edu.sdsc.ontoquest.graph.Graph#addNode(int, int)
   */
  public boolean addNode(int rid, int rtid) {
    List<Element> elementList = findElementList(rid, rtid);
    if (elementList == null) {
      adjMap.put(makeKey(rid, rtid), new LinkedList<Element>());
      return true;
    }
    return false;
  }
  

  /**
   * @see edu.sdsc.ontoquest.graph.Graph#containsEdge(int, int, int, int, int)
   */
  public boolean containsEdge(int rid1, int rtid1, int rid2, int rtid2, int pid) {
    List<Element> elementList = findElementList(rid1, rtid1);
    if (elementList == null)
      return false; // (rid1, rtid1) is not found.
    return containsEdge(elementList, rid2, rtid2, pid);
  }
  

  /**
   * @see edu.sdsc.ontoquest.graph.Graph#containsEdge(int, int, int, int, int, boolean)
   */
  public boolean containsEdge(int rid1, int rtid1, int rid2, int rtid2,
      int pid, boolean isDirected) {
    boolean result = containsEdge(rid1, rtid1, rid2, rtid2, pid);
    if (!result && isDirected)
      result = containsEdge(rid2, rtid2, rid1, rtid1, pid);
    return result;
  }
  

  /**
   * @see edu.sdsc.ontoquest.graph.Graph#containsNode(int, int)
   */
  public boolean containsNode(int rid, int rtid) {
    return findElementList(rid, rtid) != null;
  }
  

  /**
   * @see edu.sdsc.ontoquest.graph.Graph#getEdgeCount()
   */
  public int getEdgeCount() {
    return edgeCount;
  }

  public ResourceSet getEdges() {
    return getEdges(false);
  }
  
  /**
   * Each row in result resource set represents an edge in the graph.
   * Each row contains (rid1, rtid1, rid2, rtid2, pid, weight(optional)). The
   * data types are (int, int, int, int, int, float(optional)).
   * @see edu.sdsc.ontoquest.graph.Graph#getEdges()
   */
  public ResourceSet getEdges(boolean includeWeight) {
    int columnSize = includeWeight ? 6 : 5;
    Object[][] data = new Integer[getEdgeCount()][columnSize];
    int count = 0;
    for (Long nodeID : adjMap.keySet()) {
      List<Element> elist = adjMap.get(nodeID);
      int rid1 = parseRID(nodeID);
      int rtid1 = parseRTID(nodeID);
      for (Element e : elist) {
        data[count][0] = rid1;
        data[count][1] = rtid1;
        data[count][2] = e.getRid();
        data[count][3] = e.getRtid();
        data[count][4] = e.getPid();
        if (includeWeight) 
          data[count][5] = e.getWeight();
        count++;
      }
    }
    
    List<Variable> varList = new ArrayList<Variable>(columnSize);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    if (includeWeight) 
      varList.add(new Variable(1));
    
    return new InMemoryResourceSet(data, varList);
  }

  public ResourceSet getEdges(int rid1, int rtid1, int rid2, int rtid2, boolean isDirected) {
    
    return null; //TODO
  }
  
  public List<int[]> getEdgesAsArray(int rid1, int rtid1, int rid2, int rtid2, boolean isDirected) {
    List<int[]> result = getEdgesAsArray(rid1, rtid1, rid2, rtid2);
    if ((result == null || result.size() == 0) && !isDirected)
      result = getEdgesAsArray(rid2, rtid2, rid1, rtid1);
    else
      result.addAll(getEdgesAsArray(rid2, rtid2, rid1, rtid1));
    return result;
  }
  
  public List<int[]> getEdgesAsArray(int rid1, int rtid1, int rid2, int rtid2) {
    List<int[]> result = new ArrayList<int[]>();
    List<Element> elementList = findElementList(rid1, rtid1);
    if (elementList == null)
      return result;
    for (Element e : elementList) {
      if (e.getRid() == rid2 && e.getRtid() == rtid2)
        result.add(new int[]{rid1, rtid1, rid2, rtid2, e.getPid()});
    }
    return result;
  }

  public float getEdgeWeight(int rid1, int rtid1, int rid2, int rtid2) {
    float result = 0;
    List<Element> elementList = findElementList(rid1, rtid1);
    if (elementList == null)
      return result;
    for (Element e : elementList) {
      if (e.getRid() == rid2 && e.getRtid() == rtid2)
        result += e.getWeight();
    }
    return result;
  }
  
  /**
   * @see edu.sdsc.ontoquest.graph.Graph#getNodeCount()
   */
  public int getNodeCount() {
    return adjMap.size();
  }


  /**
   * @return a resource set of node ids. Each row in resource set contains two integers: rid and rtid
   * @see edu.sdsc.ontoquest.graph.Graph#getNodes()
   */
  public ResourceSet getNodes() {
    
    Integer[][] result = new Integer[getNodeCount()][2];
    int i = 0;
    for (Long key : adjMap.keySet()) {
      result[i][0] = parseRID(key);
      result[i][1] = parseRTID(key);
      i++;
    }
    List<Variable> varList = new ArrayList<Variable>(2);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    return new InMemoryResourceSet(result, varList);
  }

  /**
   * @return array of node ids. The size is int[n][2], n = number of nodes. int[i][0] = rid(i), 
   * int[i][1] = rtid(i).
   */
  public int[][] getNodesAsArray() {
    int[][] result = new int[getNodeCount()][2];
    int i = 0;
    for (Long key : adjMap.keySet()) {
      result[i][0] = parseRID(key);
      result[i][1] = parseRTID(key);
      i++;
    }
    return result;
  }
  
  /**
   * Gets the edge weight as an N by N matrix. The index of nodes is stored in
   * nodeIdxMap. If the given nodeIdxMap is not empty initially, it will be
   * emptied first.
   * @return
   */
  public double[][] getWeightMatrix(HashMap<Long, Integer> nodeIdxMap) {
    nodeIdxMap.clear();
    int count = 0;
    for (Long key : adjMap.keySet()) {
      nodeIdxMap.put(key, count++);
    }
    double[][] matrix = new double[count][count];
    double weight = 0;
    for (Long key : adjMap.keySet()) {
      List<Element> elementList = adjMap.get(key);
      for (Element e : elementList) {
        int idx1 = nodeIdxMap.get(key);
        int idx2 = nodeIdxMap.get(makeKey(e.getRid(), e.getRtid()));
        weight = e.getWeight();
        matrix[idx1][idx2] += weight;
        matrix[idx2][idx1] += weight;
      }
    }
    return matrix;  
  }
  
  /**
   * Finds the sublist in which the head node is (rid, rtid). (pid = -1 or ignored)
   * @param rid
   * @param rtid
   * @return
   */
  private List<Element> findElementList(int rid, int rtid) {
    return adjMap.get(makeKey(rid, rtid));
//    for (ArrayList<Element> elementList : adjList) {
//      // find the list for (rid1, rtid1)
//      Element e = elementList.get(0);
//      if (rid == e.getRid() && rtid == e.getRtid())
//        return elementList;
//    }
//    return null; // not found
  }
  
  private boolean containsEdge(List<Element> elementList, int rid2, int rtid2, int pid) {
    for (Element e : elementList) {
      if (e.getPid()==pid && e.getRid()==rid2 && e.getRtid()== rtid2)
        return true;
    }
    return false;
  }
  
  public long makeKey(int rid, int rtid) {
    return (((long)rtid)<<32)|rid;
  }
  
  public int parseRID(long key) {
    return (int)key;
  }
  
  public int parseRTID(long key) {
    return (int)(key>>32);
  }
  /**
   * Element is the entry in the adjacency list.
   * @version $Id: InMemoryAdjMapGraph.java,v 1.1 2010-10-28 06:30:35 xqian Exp $
   *
   */
  public class Element {
    private int rid, rtid, pid;
    private float weight;
    public Element(int rid2, int rtid2, int pid) {
      this(rid2, rtid2, pid, 1); // default weight = 1
    }

    public Element(int rid2, int rtid2, int pid, float weight) {
      this.rid = rid2;
      this.rtid = rtid2;
      this.pid = pid;
      this.weight = weight;
    }
    
    public int getRid() {
      return rid;
    }

    public int getRtid() {
      return rtid;
    }

    public int getPid() {
      return pid;
    }
    
    public float getWeight() {
      return weight;
    }
  }
}
