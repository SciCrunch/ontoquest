package edu.sdsc.ontoquest.db.functions;

import java.io.Serializable;

/**
 * A simplified version of NIFTerm. Only contains rid, rtid and label 
 * (term name). 
 * @version $Id: NIFSimpleTerm.java,v 1.1 2010-10-28 06:30:06 xqian Exp $
 *
 */
public class NIFSimpleTerm implements Serializable, Comparable<NIFSimpleTerm> {

  private static final long serialVersionUID = 3783441370448034292L;

  private int rid;
  private int rtid;
  private String label = null;

  public NIFSimpleTerm(int rid, int rtid, String label) {
    setRid(rid);
    setRtid(rtid);
    setLabel(label);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(NIFSimpleTerm o) {
    if (o != null) {
      if (getRid()==o.getRid() && getRtid() == o.getRtid())
        return 0;
    }
    return -1;
  }

  /**
   * @return the rid
   */
  public int getRid() {
    return rid;
  }

  /**
   * @param rid the rid to set
   */
  public void setRid(int rid) {
    this.rid = rid;
  }

  /**
   * @return the rtid
   */
  public int getRtid() {
    return rtid;
  }

  /**
   * @param rtid the rtid to set
   */
  public void setRtid(int rtid) {
    this.rtid = rtid;
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }

}
