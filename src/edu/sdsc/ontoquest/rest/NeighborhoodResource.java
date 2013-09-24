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
import edu.sdsc.ontoquest.Context;

import edu.sdsc.ontoquest.ResourceSet;

import edu.sdsc.ontoquest.db.DbBasicFunctions;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * @version $Id: NeighborhoodResource.java,v 1.4 2013-09-24 23:10:20 jic002 Exp $
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
      if ( type == NeighborType.ALLSUBCLASSES) 
      {
        getAllSubClassGraph(inputStr, application.getKbId(), getOntoquestContext());
      } else if (type == NeighborType.PARTOF) { 
        getAllPartOfGraph(inputStr, application.getKbId(), getOntoquestContext());
      } else if ( type == NeighborType.EDGE_RELATION) 
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
  
  private void getAllPartOfGraph(String term, int kbid, Context c) throws OntoquestException
  {
    Set<Relationship> edgeSet = new HashSet<Relationship> ();
    
    ResourceSet  rs = DbBasicFunctions.getInstance().searchpartOf( kbid, term, c);
    while (rs.next()) {
      String label1 = rs.getString(3);
      if ( label1 == null)
        throw new OntoquestException("label for " + rs.getInt(1) + "-" + rs.getInt(2) + " not found in graph_nodes_all") ;
      String label2 = rs.getString(6);
      if ( label2 == null)
        throw new OntoquestException("label for " + rs.getInt(4) + "-" + rs.getInt(5) + " not found in graph_nodes_all") ;

      Relationship e = new Relationship(rs.getInt(1), rs.getInt(2),
          rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getString(6),
          rs.getInt(7), rs.getString(8), c);
      if (!edgeSet.contains(e)) {
        edgeSet.add(e);
      }
    }
    rs.close();

    graph = new OntGraph(edgeSet);
  }

  
  private void getAllSubClassGraph(String term, int kbid, Context c) throws OntoquestException
  {
    Set<Relationship> edgeSet = new HashSet<Relationship> ();
    
    ResourceSet  rs = DbBasicFunctions.getInstance().searchSubclasses( kbid, term, c);
    while (rs.next()) {
      String label1 = rs.getString(3);
      if ( label1 == null)
        throw new OntoquestException("label for " + rs.getInt(1) + "-" + rs.getInt(2) + " not found in graph_nodes_all") ;
      String label2 = rs.getString(6);
      if ( label2 == null)
        throw new OntoquestException("label for " + rs.getInt(4) + "-" + rs.getInt(5) + " not found in graph_nodes_all") ;

      Relationship e = new Relationship(rs.getInt(1), rs.getInt(2),
          rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getString(6),
          rs.getInt(7), rs.getString(8), c);
      if (!edgeSet.contains(e)) {
        edgeSet.add(e);
      }
    }
    rs.close();

    graph = new OntGraph(edgeSet);
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
