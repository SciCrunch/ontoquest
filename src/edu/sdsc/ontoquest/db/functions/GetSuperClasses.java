package edu.sdsc.ontoquest.db.functions;


/**
 * @version $Id: GetSuperClasses.java,v 1.1 2010-10-28 06:30:13 xqian Exp $
 *
 */
public class GetSuperClasses extends GetNeighbors {
  
  /**
   * Gets all levels of superclasses in the same knowledge base as (rid, rtid).
   * @param rid
   * @param rtid
   */
  public GetSuperClasses(int rid, int rtid) {
    this(rid, rtid, 0, 0, true);
  }
  
  public GetSuperClasses(String term, int kbid) {
    super(term, kbid, new String[]{"subClassOf"}, null, true, GetNeighbors.EDGE_OUTGOING, 0, true);
  }
  
  public GetSuperClasses(String[] terms, int kbid, int level, boolean namedClassOnly) {
    super(terms, kbid, new String[]{"subClassOf"}, null, true, 
        GetNeighbors.EDGE_OUTGOING, true, namedClassOnly, level, true);
  }
  /**
   * 
   * @param rid
   * @param rtid
   * @param kbid the id of the knowledge base to search if it is greater than 0. 
   * 0 means the same knowledge base as (rid, rtid) node. A negative number (e.g. -1)
   * means all knowledge bases.
   * @param level return ancestors up to <code>level</code>. 0 means ancestors at all levels.
   */
  public GetSuperClasses(int rid, int rtid, int kbid, int level, boolean namedClassOnly) {
    super(rid, rtid, kbid, new String[]{"subClassOf"}, null, GetNeighbors.EDGE_OUTGOING, true, 
        namedClassOnly, level, true);
  }
  
}
