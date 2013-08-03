package edu.sdsc.ontoquest.rest;

import java.util.Map;

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

import java.util.Calendar;

/**
 * @version $Id: NeighborhoodResource.java,v 1.3 2013-08-03 05:35:39 jic002 Exp $
 *
 */
public class NeighborhoodResource extends BaseResource {
  private NeighborType type = NeighborType.PARENTS;  // default
//  private String classId;
  private OntGraph graph;
  
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
        type = NeighborType.valueOf(typeVal.toUpperCase().replace('-', '_'));
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
          if ( inputStr !=null) {
            inputType = InputType.TERM;
          } else 
          {
            inputStr = (String)getRequest().getAttributes().get("id");
            inputType =InputType.ID;
          }
        }
      }
      
      inputStr = Reference.decode(inputStr);
      Map<String, Object> attributes = getRequest().getAttributes();
      if (attributes.get("level") == null) {
        attributes.put("level", 1); // default level = 1
      }
      long t1 = Calendar.getInstance().getTimeInMillis();
      if ( type == NeighborType.EDGE_RELATION) 
      {
        graph =  OntGraph.getAllEdges(inputStr, application.getKbId(), 
            inputType, getOntoquestContext());
      } else {
        graph = OntGraph.get(inputStr, type, application.getKbId(), 
            attributes, inputType, getOntoquestContext());
      }
      long t2 = Calendar.getInstance().getTimeInMillis();
      
      System.out.println ("Wall Clock time for runing OntGraph.get is: " + (t2-t1)+ " milliseconds." );
      
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
