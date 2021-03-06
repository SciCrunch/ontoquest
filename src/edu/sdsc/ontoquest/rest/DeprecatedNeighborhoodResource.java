package edu.sdsc.ontoquest.rest;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.rest.BaseBean.InputType;
import edu.sdsc.ontoquest.rest.BaseBean.NeighborType;

/**
 * @deprecated
 * Replaced by NeighborhoodResource.
 * $Id: DeprecatedNeighborhoodResource.java,v 1.1 2010-10-28 06:29:55 xqian Exp $
 *
 */
public class DeprecatedNeighborhoodResource extends BaseResource {
  private NeighborType type = NeighborType.PARENTS;  // default
//  private String classId;
  private DeprecatedOntGraph graph;
  
  @Override
  protected void doInit() throws ResourceException {
    String typeVal = null;
    OntoquestApplication application = (OntoquestApplication)getApplication();
    try {
      // extract optional parameters
      Form form = getRequest().getResourceRef().getQueryAsForm();
      for (String key : form.getValuesMap().keySet()) {
        getRequest().getAttributes().put(key, form.getFirstValue(key));
      }
      
      typeVal = (String) getRequest().getAttributes().get("type");
      this.type = null;
      try {
        type = NeighborType.valueOf(typeVal.toUpperCase());
      } catch (IllegalArgumentException iae) {
        throw new OntoquestException(OntoquestException.Type.INPUT, "Invalid neighbor type: " + typeVal);
      }
      InputType inputType = InputType.TERM;
      
      String inputStr = (String) getRequest().getAttributes().get("oid");     
      if (inputStr != null) {
        inputType = InputType.OID;
      } else {
        inputStr = (String)getRequest().getAttributes().get("classId");
        if (inputStr != null) {
          inputType = InputType.NAME;
        } else {
          inputStr = (String)getRequest().getAttributes().get("term");
          inputType = InputType.TERM;
        }
      }
      
      inputStr = Reference.decode(inputStr);
      graph = DeprecatedOntGraph.get(inputStr, type, application.getKbId(), 
            getRequest().getAttributes(), inputType, getOntoquestContext());
      
    } catch (IllegalArgumentException iae) {
//      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
//          "Invalid neighbor type -- " + typeVal, iae); 
      setAppException(new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, 
          "Invalid neighbor type -- " + typeVal, iae));
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
      
      if (graph != null) { // matched
        Element classNodeElem = graph.toXml(d);
        dataElem.appendChild(classNodeElem);
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
