package edu.sdsc.ontoquest.rest;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.db.functions.RunSQL;
import edu.sdsc.ontoquest.query.Utility;

/**
 * @version $Id: StatResource.java,v 1.1 2010-10-28 06:29:54 xqian Exp $
 *
 */
public class StatResource extends BaseResource {
  int conceptCount = -1;
  int termCount = -1;
  
  @Override
  protected void doInit() throws ResourceException {
    try {
      OntoquestApplication application = (OntoquestApplication)getApplication();
      int kbId = application.getKbId();  // default kb id
      Form form = getRequest().getResourceRef().getQueryAsForm();
      for (String key : form.getValuesMap().keySet()) {
        getRequest().getAttributes().put(key, form.getFirstValue(key));
      }
      String kbName = form.getFirstValue("ontology");
      if (kbName != null && kbName.length() > 0) {
        BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
        kbId = basicFunctions.getKnowledgeBaseID(kbName, getOntoquestContext());
      }
//      System.out.println("kbName -- " + kbName+", kbId -- " + kbId);
      // check if the request is to get siblings
      String typeVal = (String) getRequest().getAttributes().get("type");
      if (!Utility.isBlank(typeVal)) {
        Context context = getOntoquestContext();
        if (typeVal.equalsIgnoreCase("termcount")) {
          termCount = getTermCount(kbId, context);
        } else if (typeVal.equalsIgnoreCase("conceptcount")) {
          conceptCount = getConceptCount(kbId, context);
        } else if (typeVal.equalsIgnoreCase("all")) {
          termCount = getTermCount(kbId, context);
          conceptCount = getConceptCount(kbId, context);          
        }
      }
    } catch (Throwable oe) {
      setAppException(oe);
      //      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, oe.getMessage(), oe);
    }
  }      
  
  public static int getConceptCount(int kbId, Context context) {
    int count = 0;
    try {
      String sql = "select count(*) from primitiveclass where kbid = "+kbId
      +" and is_owlclass = true and is_system = false";
      OntoquestFunction<ResourceSet> f = new RunSQL(sql);
      ResourceSet rs = f.execute(context, BaseBean.getVarList1());
      if (rs.next()) {
        count = rs.getInt(1);
      }
      rs.close();
    } catch (OntoquestException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return count;
  }
  
  public static int getTermCount(int kbId, Context context) {
    int count = 0;
    try {
      String sql = "select count(*) from (select distinct lower(s.label) from graph_edges r, property p, graph_nodes s, ( "
        + "select id from primitiveclass where is_owlclass = true and is_system = false) rids "
        + "where r.rid1 = rids.id and r.rtid1 = 1 and r.pid = p.id and p.name in ('synonym', 'abbrev', " 
        + "'acronym') and s.rid = r.rid2 and s.rtid = r.rtid2 and r.kbid = " + kbId + " and s.label != '' "
        + " union select browsertext from primitiveclass where kbid = "+kbId
        +" and is_owlclass = true and is_system = false) st";
      OntoquestFunction<ResourceSet> f = new RunSQL(sql);
      ResourceSet rs = f.execute(context, BaseBean.getVarList1());
      if (rs.next()) {
        count = rs.getInt(1);
      }
      rs.close();
    } catch (OntoquestException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return count;
  }

  @Get("xml")
  public Representation toXml() {
    if (getAppException() != null)
      return toErrorXml();
    try {
      DomRepresentation representation = new DomRepresentation(
              MediaType.TEXT_XML);
      // Generate a DOM document representing the item.
      Document d = representation.getDocument();
      Element succElem = d.createElement("success");
      d.appendChild(succElem);
      Element dataElem = d.createElement("data");
      succElem.appendChild(dataElem);

      
      d.normalizeDocument();

      // Returns the XML representation of this document.
      return representation;
    } catch (Throwable e) {
      e.printStackTrace();
      setAppException(e);
      return toErrorXml();
    }
  }

}
