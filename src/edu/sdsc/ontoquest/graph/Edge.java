package edu.sdsc.ontoquest.graph;

/**
 * @version $Id: Edge.java,v 1.1 2010-10-28 06:30:34 xqian Exp $
 *
 */
public interface Edge {
  public int getNode1RID();
  public int getNode1RTID();
  public int getNode2RID();
  public int getNode2RTID();
  public int getPID();
}
