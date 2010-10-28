package edu.sdsc.ontoquest.db.functions;

/**
 * @version $Id: GetSubClasses.java,v 1.1 2010-10-28 06:30:10 xqian Exp $
 *
 */
public class GetSubClasses extends GetNeighbors {

  /**
   * Gets subclasses at all levels in the same knowledge base as (rid, rtid).
   * @param rid
   * @param rtid
   */
  public GetSubClasses(int rid, int rtid) {
    this(rid, rtid, 0, 0, true);
  }

  /**
   * Get subclasses of input node (rid, rtid)
   * @param rid
   * @param rtid
   * @param kbid the id of the knowledge base to search if it is greater than 0. 
   * 0 means the same knowledge base as (rid, rtid) node. A negative number (e.g. -1)
   * means all knowledge bases.
   * @param level return children up to <code>level</code>. 0 means children at all levels.
   */
  public GetSubClasses(int rid, int rtid, int kbid, int level, boolean namedClassOnly) {
    super(rid, rtid, kbid, new String[]{"subClassOf"}, null, GetNeighbors.EDGE_INCOMING, true, 
        namedClassOnly, level, true);
  }
  
  public GetSubClasses(String term, int kbid) {
    super(term, kbid, new String[]{"subClassOf"}, null, true, GetNeighbors.EDGE_INCOMING, 0, true);
  }
  
  /**
   * Get subclasses of input nodes.
   * @param terms the list of terms
   * @param kbid knowledge base id
   * @param level the level of subclasses to fetch
   * @param namedClassOnly If true, get primitive classes only
   */
  public GetSubClasses(String[] terms, int kbid, int level, boolean namedClassOnly) {
    super(terms, kbid, new String[]{"subClassOf"}, null, true, 
        GetNeighbors.EDGE_INCOMING, true, namedClassOnly, level, true);
  }

}
