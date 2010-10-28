package edu.sdsc.ontoquest.rest;

import java.util.Set;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.db.functions.GetNeighbors.PropertyType;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.rest.BaseBean.InputType;
import edu.sdsc.ontoquest.rest.BaseBean.SiblingsType;

/**
 * @version $Id: SimpleClassNodeResource.java,v 1.1 2010-10-28 06:29:56 xqian Exp $
 *
 */
public class SimpleClassNodeResource extends BaseResource {
  Set<SimpleClassNode> classNodes = null;
  
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
      
      // extract prop_type values, and put into propTypes array
      PropertyType[] propTypes = null;
      String propTypeInput = (String) getRequest().getAttributes().get("prop_type");
      if (propTypeInput != null && propTypeInput.length() > 0) {
        propTypeInput = Reference.decode(propTypeInput);
        String[] propTypeStrings = propTypeInput.split(",");
        propTypes = new PropertyType[propTypeStrings.length];
        for (int i=0; i<propTypeStrings.length; i++) {
          try {
            propTypes[i] = PropertyType.valueOf(propTypeStrings[i].toUpperCase());
          } catch (IllegalArgumentException iae) {
            throw new OntoquestException(OntoquestException.Type.INPUT, "Invalid property type: " + propTypeStrings[i]);
          }
        }
      }
      
      String classId = (String) getRequest().getAttributes().get("classId");
      if (classId != null) {
        classId = Reference.decode(classId);
        classNodes = SimpleClassNode.getByName(classId, kbId, propTypes, getOntoquestContext());
        return;
      }
    
      String oid = (String) getRequest().getAttributes().get("oid");
      if (oid != null) {
        classNodes = SimpleClassNode.get(oid, propTypes, getOntoquestContext());
        return;
      }
      
      String term = (String)getRequest().getAttributes().get("term");
      if (term != null) {
        term = Reference.decode(term);
        classNodes = SimpleClassNode.getByLabel(term, kbId, propTypes, getOntoquestContext());
        return;
      }
      
    } catch (Throwable oe) {
      setAppException(oe);
      //      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, oe.getMessage(), oe);
    }
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
      
      Element classesElem = d.createElement("classes");
      dataElem.appendChild(classesElem);
      
      for (SimpleClassNode classNode : classNodes) { // matched
        Element classNodeElem = classNode.toXml(d);
        classesElem.appendChild(classNodeElem);
      } 
      
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
