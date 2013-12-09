package edu.sdsc.ontoquest.rest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;
import edu.sdsc.ontoquest.db.functions.RunSQL;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: Ontology.java,v 1.2 2013-06-21 22:28:27 jic002 Exp $
 *
 */
public class Ontology extends BaseBean {

  int rid, rtid; 
  String name = "";
  String title = "";
  String subjectKeywords = "";
  String definition = "";
  List<String> creators = new LinkedList<String>();
  String version = "";
  String format = "OWL";
  List<String> contributors = new LinkedList<String>();
  String dateCreated = "";
  Context context;
  
  public Ontology(int rid, int rtid, String name, Context context) {
    this.rid = rid;
    this.rtid = rtid;
    this.name = name;
    this.context = context;
  }
  
  public static Ontology get(String kbName, Context context) throws OntoquestException {
    String sql = "select u.id, rt.id as rtid from ontologyuri u, resourcetype rt, kb kb where is_default = true and rt.name = 'ontologyuri' and u.kbid = kb.id and kb.name = '"+kbName+"'";
    ArrayList<Variable> varList = new ArrayList<Variable>(2);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    
    OntoquestFunction<ResourceSet> f = new RunSQL(sql);
    ResourceSet rs = f.execute(context, varList);
    Ontology ont = null;
    int rid = -1, rtid = -1; 
    if (rs.next()) {
      rid = rs.getInt(1);
      rtid = rs.getInt(2);
      ont = get(rid, rtid, kbName, context);
    }
    rs.close();
    
    return ont;
  }
  
  public static List<Ontology> getAll(Context context) throws OntoquestException {
    String sql = "select u.id, rt.id as rtid, kb.name from ontologyuri u, resourcetype rt, kb kb where is_default = true and rt.name = 'ontologyuri' and u.kbid = kb.id";
    ArrayList<Variable> varList = new ArrayList<Variable>(3);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    
    OntoquestFunction<ResourceSet> f = new RunSQL(sql);
    ResourceSet rs = f.execute(context, varList);
    List<Ontology> results = new LinkedList<Ontology>();
    
    int rid = -1, rtid = -1; 
    String kbName = null;
    while (rs.next()) {
      rid = rs.getInt(1);
      rtid = rs.getInt(2);
      kbName = rs.getString(3);
      Ontology ont = get(rid, rtid, kbName, context);
      results.add(ont);
    }
    rs.close();
    
    return results;
  }

  public static Ontology get(int rid, int rtid, String kbName, Context context) throws OntoquestException {
    OntoquestFunction<ResourceSet> f = new GetNeighbors(rid, rtid, 0, 
        null, null, GetNeighbors.EDGE_OUTGOING, true, false, 0, true);
    
    Ontology ont = new Ontology(rid, rtid, kbName, context);
    
    ResourceSet rs = f.execute(context, getVarList8());
    while (rs.next()) {
      String prop = rs.getString(8);
      String value = rs.getString(6);
      if (prop.equalsIgnoreCase("title")) {
        ont.setTitle(value);
      } else if (prop.equalsIgnoreCase("definition")) {
        ont.setDefinition(value);
      } else if (prop.equalsIgnoreCase("Contributor")) {
        ont.getContributors().add(value);
      } else if (prop.equalsIgnoreCase("Creator")) {
        ont.getCreators().add(value);
      } else if (prop.equalsIgnoreCase("versionInfo")) {
        ont.setVersion(value);
      } else if (prop.equalsIgnoreCase("createdDate")) {
        ont.setDateCreated(value);
      } else if (prop.equalsIgnoreCase("Subject and Keywords")) {
        ont.setSubject(value);
      }
    }
    rs.close();
    return ont;
  }
  
  public static String getURI(String kbName, Context context) throws OntoquestException {
  
  String sqlTemplate = "select uri from ontologyuri, kb kb where kbid = kb.id and kb.name = ':1' and is_default = true";
    String sql = sqlTemplate.replace(":1", DbUtility.formatSQLString(kbName));
    
    ArrayList<Variable> varList = new ArrayList<Variable>(1);
    varList.add(new Variable(1));
    
    OntoquestFunction<ResourceSet> f = new RunSQL(sql);
    ResourceSet rs = f.execute(context, varList);
    String result = null; 
    if (rs.next()) {
      result = rs.getString(1);
    }
    rs.close();
    return result;
  }

  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.rest.BaseBean#toXml(org.w3c.dom.Document)
   */
  @Override
  public Element toXml(Document doc) 
  {
    Element e = doc.createElement("ontologyBean");
    
    Element idElem = doc.createElement("id");
    e.appendChild(idElem);
    idElem.setAttribute("InternalId", ClassNode.generateId(rid, rtid));

    try
    {
      idElem.appendChild(doc.createTextNode(ClassNode.generateExtId(rid, rtid, context)));
    } catch (OntoquestException f)
    {
    }
    
    Element nameElem = doc.createElement("displayLabel");
    e.appendChild(nameElem);
    nameElem.appendChild(doc.createTextNode(getName()));

/*    
    Element titleElem = doc.createElement("title");
    e.appendChild(titleElem);
    titleElem.appendChild(doc.createTextNode(getTitle()));
    
    Element subjectElem = doc.createElement("subject");
    e.appendChild(subjectElem);
    subjectElem.appendChild(doc.createTextNode(subjectKeywords));
    
    Element creatorsElem = doc.createElement("creators");
    e.appendChild(creatorsElem);
    for (String creator : creators) {
      Element creatorElem = doc.createElement("creator");
      creatorsElem.appendChild(creatorElem);
      creatorElem.appendChild(doc.createTextNode(creator));
    }
    
    Element versionElem = doc.createElement("version");
    e.appendChild(versionElem);
    versionElem.appendChild(doc.createTextNode(version));
*/    
    Element formatElem = doc.createElement("format");
    e.appendChild(formatElem);
    formatElem.appendChild(doc.createTextNode(format));

/*    
    Element contributorsElem = doc.createElement("contributors");
    e.appendChild(contributorsElem);
    for (String contributor : contributors) {
      Element contributorElem = doc.createElement("contributor");
      contributorsElem.appendChild(contributorElem);
      contributorElem.appendChild(doc.createTextNode(contributor));
    }
    
    Element dateElem = doc.createElement("date_created");
    e.appendChild(dateElem);
    dateElem.appendChild(doc.createTextNode(dateCreated));
*/    
    return e;
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

  /**
   * @return the subject
   */
  public String getSubject() {
    return subjectKeywords;
  }

  /**
   * @param subject the subject to set
   */
  public void setSubject(String subject) {
    this.subjectKeywords = subject;
  }

  /**
   * @return the definition
   */
  public String getDefinition() {
    return definition;
  }

  /**
   * @param definition the definition to set
   */
  public void setDefinition(String definition) {
    this.definition = definition;
  }

  /**
   * @return the creators
   */
  public List<String> getCreators() {
    return creators;
  }

  /**
   * @param creators the creators to set
   */
  public void setCreators(List<String> creators) {
    this.creators = creators;
  }

  /**
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * @param version the version to set
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * @return the format
   */
  public String getFormat() {
    return format;
  }

  /**
   * @param format the format to set
   */
  public void setFormat(String format) {
    this.format = format;
  }

  /**
   * @return the contributors
   */
  public List<String> getContributors() {
    return contributors;
  }

  /**
   * @param contributors the contributors to set
   */
  public void setContributors(List<String> contributors) {
    this.contributors = contributors;
  }

  /**
   * @return the dateCreated
   */
  public String getDateCreated() {
    return dateCreated;
  }

  /**
   * @param dateCreated the dateCreated to set
   */
  public void setDateCreated(String dateCreated) {
    this.dateCreated = dateCreated;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }
}
