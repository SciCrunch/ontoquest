package edu.sdsc.ontoquest.rest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @version $Id: BasicClassNode.java,v 1.1 2010-10-28 06:29:56 xqian Exp $
 *
 */
public class BasicClassNode extends BaseBean {
  private int rid;
  private int rtid;
  private String label; // rdfs:label
  private String name; // class name, e.g. birnlex_802

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
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.rest.BaseBean#toXml(org.w3c.dom.Document)
   */
  @Override
  public Element toXml(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }

}
