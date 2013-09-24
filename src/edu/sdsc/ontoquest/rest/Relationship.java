package edu.sdsc.ontoquest.rest;

import edu.sdsc.ontoquest.Context;

import edu.sdsc.ontoquest.OntoquestException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @version $Id: Relationship.java,v 1.4 2013-09-24 23:12:16 jic002 Exp $
 *
 */
public class Relationship extends BaseBean {
  private int rid1; // A (rid, rtid) pair uniquely identifies a node in ontology graph.
  private int rtid1;
  private String label1;
  private int rid2;
  private int rtid2;
  private String label2;
  private int pid; // property id, a.k.a. edge label id
  private String pname; // property name, a.k.a. edge label
  private Context context;
  
  private static final int DEFAULT_PROPERTY_RTID = 15;
  
  public Relationship(int rid1, int rtid1, String label1, int rid2, int rtid2, String label2, int pid, String pname, Context context) {
    this.rid1 = rid1;
    this.rtid1 = rtid1;
    this.label1 = label1;
    this.rid2 = rid2;
    this.rtid2 = rtid2;
    this.label2 = label2;
    this.pid = pid;
    this.pname = pname;
    this.context = context;
  }

  /**
   * @return the rid1
   */
  public int getRid1() {
    return rid1;
  }

  /**
   * @return the rtid1
   */
  public int getRtid1() {
    return rtid1;
  }

  /**
   * @return the label1
   */
  public String getLabel1() {
    return label1;
  }

  /**
   * @return the rid2
   */
  public int getRid2() {
    return rid2;
  }

  /**
   * @return the rtid2
   */
  public int getRtid2() {
    return rtid2;
  }

  /**
   * @return the label2
   */
  public String getLabel2() {
    return label2;
  }

  /**
   * @return the pid
   */
  public int getPid() {
    return pid;
  }

  /**
   * @return the pname
   */
  public String getPname() {
    return pname;
  }

  public boolean equals(Object o) {
    if (!(o instanceof Relationship)) 
      return false;
    
    Relationship r = (Relationship)o;
    
    return r.getRid1()==getRid1() && r.getRid2()==getRid2() 
      && r.getRtid1()==getRtid1() && r.getRtid2()==getRtid2()
      && r.getPid()==getPid();
  }

  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.rest.BaseBean#toXml(org.w3c.dom.Document)
   */
  @Override
  public Element toXml(Document doc) {
    Element e = doc.createElement("relationship");
    
    Element subElem = doc.createElement("subject");
    subElem.setAttribute(ClassNode.interanIdAttrName, ClassNode.generateId(getRid1(), getRtid1()));
    try
    {
      subElem.setAttribute("id", ClassNode.generateExtId(getRid1(), getRtid1(), context));
    } catch (OntoquestException f)
    {
       System.out.println("Exception from Relationship.toXML()" + f.getMessage());
    }
    if ( getLabel1() != null ) {
      subElem.appendChild(doc.createTextNode(getLabel1()));
      e.appendChild(subElem);
    } else 
      System.out.println("ERROR: getLabel1 returns null");
    
    Element propElem = doc.createElement("property");
    propElem.setAttribute(ClassNode.interanIdAttrName, ClassNode.generateId(getPid(), DEFAULT_PROPERTY_RTID));
    try
    {
      propElem.setAttribute("id", ClassNode.generateExtId(getPid(), DEFAULT_PROPERTY_RTID,context));
    } catch (OntoquestException f)
    {
      System.out.println("Exception from Relationship.toXML()" + f.getMessage());
    }
    
    if ( getPname() != null ) {
      propElem.appendChild(doc.createTextNode(getPname()));
      e.appendChild(propElem);
    } 
    else 
    System.out.println("ERROR: getPname returns null");
    
    Element objElem = doc.createElement("object");
    objElem.setAttribute(ClassNode.interanIdAttrName, ClassNode.generateId(getRid2(), getRtid2()));
    try
    {
      objElem.setAttribute("id", ClassNode.generateExtId(getRid2(), getRtid2(),context));
    } catch (OntoquestException f)
    {
      System.out.println("Exception from Relationship.toXML()" + f.getMessage());
    }
    if ( getLabel2() != null) {
      objElem.appendChild(doc.createTextNode(getLabel2()));
      e.appendChild(objElem);
    } else 
      System.out.println("ERROR: getLabel2 returns null for "+ rid2 + ',' + rtid2);

    return e;
  }
}
