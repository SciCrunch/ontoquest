package edu.sdsc.ontoquest.rest;

import edu.sdsc.ontoquest.BasicFunctions;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @version $Id: NeighborhoodResource.java,v 1.7 2013-10-29 23:54:56 jic002 Exp $
 *
 */
public class NeighborhoodResource extends BaseResource {
  private NeighborType type = NeighborType.PARENTS;  // default
//  private String classId;
  private OntGraph graph;

  private static int defaultResultLimit = 1000;
  
  @Override
  protected void doInit() throws ResourceException {
    String typeVal = null;
    OntoquestApplication application = (OntoquestApplication)getApplication();
    int kbId = application.getKbId();  // default kb id
    try {
      // extract optional parameters
      Form form = getRequest().getResourceRef().getQueryAsForm();
      for (String key : form.getValuesMap().keySet()) {
        getRequest().getAttributes().put(key, form.getFirstValue(key));
      }
      
      String kbName = form.getFirstValue("ontology");
      if (kbName != null && kbName.length() > 0) {
        BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
        kbId = basicFunctions.getKnowledgeBaseID(kbName, getOntoquestContext());
      }

      boolean includingEquivalentClass = false;
      String includeEqClass = form.getFirstValue("equivalentclass");
      if (includeEqClass  != null) {
        if (includeEqClass.equalsIgnoreCase("true"))
          includingEquivalentClass=true;
        else if ( ! includeEqClass.equalsIgnoreCase("false"))
          throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid value for parameter 'equivalentclass'. Supported values are 'true' and 'false'.");
      }

      int lmt = defaultResultLimit;
      String limitFlag = form.getFirstValue ("limit");
      if ( limitFlag != null ) 
      {
        try {
          lmt = Integer.valueOf(limitFlag);
          
        } catch (NumberFormatException e) 
        {
          throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Value of parameter 'limit' can only be integers.");
        }
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
        getAllSubClassGraph(inputStr, kbId, getOntoquestContext(),includingEquivalentClass);
      } else if (type == NeighborType.ALLPARTSOF) { 
        getAllPartOfGraph(inputStr, kbId, getOntoquestContext(),includingEquivalentClass);
      } else if ( type == NeighborType.ALLHASPARTS) {
        getAllHasPartGraph(inputStr, kbId, getOntoquestContext(),includingEquivalentClass);
      } else if (type == NeighborType.CLOSURE) { 

        String propertyName = form.getFirstValue("property");
        if (propertyName == null) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Parameter 'property' needs to be specified in URL.");
        }

        boolean incoming = true;
        String directionStr = form.getFirstValue("direction");
        if (directionStr != null) {
          if (directionStr.equalsIgnoreCase("outgoing"))
            incoming=false;
          else if ( ! directionStr.equalsIgnoreCase("incoming"))
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid value for parameter 'direction'. Supported values are 'outgoing' and 'incoming'.");
        }
        
        
        getClosureGraph(inputStr, kbId, getOntoquestContext(), propertyName, incoming,includingEquivalentClass);
      } else if ( type == NeighborType.EDGE_RELATION) 
      {
        graph = OntGraph.getAllEdges(inputStr, kbId,inputType, getOntoquestContext());
      } else {
        graph = OntGraph.get(inputStr, type, kbId,attributes, inputType, getOntoquestContext(), lmt);
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


  private void getClosureGraph(String term, int kbid, Context c, 
                                 String propertyName, boolean incoming, boolean includingEquivalentClass) throws OntoquestException
  {
    Collection<Relationship> edgeSet = new LinkedHashSet<Relationship> (500);
    
    ResourceSet  rs = DbBasicFunctions.getInstance().searchClosure( kbid, term, c, propertyName, incoming,includingEquivalentClass);
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

    graph = new OntGraph(edgeSet, false);
  }

  
  private void getAllHasPartGraph(String term, int kbid, Context c, boolean includingEquivalentClass) throws OntoquestException
  {
    Collection<Relationship> edgeSet = new LinkedHashSet<Relationship> (500);
    
    ResourceSet  rs = DbBasicFunctions.getInstance().searchHasPart( kbid, term, c,includingEquivalentClass);
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

    graph = new OntGraph(edgeSet,false);
  }

  private void getAllPartOfGraph(String term, int kbid, Context c, boolean includingEquivalentClass) throws OntoquestException
  {
    Collection<Relationship> edgeSet = new LinkedHashSet<Relationship> (500);
    
    ResourceSet  rs = DbBasicFunctions.getInstance().searchpartOf( kbid, term, c,includingEquivalentClass);
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

    graph = new OntGraph(edgeSet,false);
  }
  
  private void getAllSubClassGraph(String term, int kbid, Context c, boolean includingEquivalentClass) throws OntoquestException
  {
    Collection<Relationship> edgeSet = new ArrayList<Relationship> (500);
    
    ResourceSet  rs = DbBasicFunctions.getInstance().searchSubclasses( kbid, term, c,includingEquivalentClass);
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

    graph = new OntGraph(edgeSet, false);
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
      
      // create the flag 
      Element flagElem = d.createElement("truncatedResults");
      flagElem.appendChild(d.createTextNode(graph.isTruncated()?"true":"false"));
      succElem.appendChild(flagElem);
      
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
