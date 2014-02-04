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
  private String URI;

  public BasicClassNode () 
  {
    label=null;
    name = null;
    URI = null;
  }
  public BasicClassNode (int rid, int rtid, String label, String name, String URI) 
  {
    this.rid = rid;
    this.rtid = rtid;
    this.label = label;
    this.name = name;
    this.URI = URI;
  }

  public BasicClassNode (int rid, int rtid) 
  {
    this.rid = rid;
    this.rtid = rtid;
    this.label = null;
    this.name = null;
    this.URI = null;
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
  
  public void setURI (String uri) 
  {
    this.URI = uri;
  }
  
  public String getURI () {return URI;}
  
  /**
   * Get the internal rid and rtid as a combined string in the format of "rid-rtid". For example, "123456-1".
   * @return
   */
  public String getId() {return rid+"-"+rtid ;}

  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.rest.BaseBean#toXml(org.w3c.dom.Document)
   */
  @Override
  public Element toXml(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }

}
