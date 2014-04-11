package edu.sdsc.ontoquest.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.query.Utility;

/**
 * @version $Id: OntologiesResource.java,v 1.1 2010-10-28 06:29:57 xqian Exp $
 *
 */
public class OntologiesResource extends BaseResource {
  private enum QueryType {DOWNLOAD_ONE, LIST_ALL, LIST_ONE }
  
  List<Ontology> ontologies = new LinkedList<Ontology>();
  String selectedOntURL = null;
  QueryType queryType = QueryType.LIST_ALL;
  
  @Override
  protected void doInit() throws ResourceException {
    try {
      OntoquestApplication application = (OntoquestApplication)getApplication();
      
      String type = (String) getRequest().getAttributes().get("type");
      if (Utility.isBlank(type)) {
        queryType = QueryType.LIST_ALL;
        ontologies = Ontology.getAll(getOntoquestContext());
        return;
      }
      
      String kbName = (String) getRequest().getAttributes().get("kbName");
      kbName = Reference.decode(kbName);
      if (Utility.isBlank(kbName)) 
        throw new OntoquestException(OntoquestException.Type.INPUT, "Missing parameter -- ontology name");
      
      if (type.equalsIgnoreCase("download")) {
        queryType = QueryType.DOWNLOAD_ONE;
        selectedOntURL = Ontology.getURI(kbName, getOntoquestContext());
        if (Utility.isBlank(selectedOntURL))
          throw new OntoquestException(OntoquestException.Type.INPUT, "Ontology not found -- " + kbName);
      } else { // default: list all ontologies
        queryType = QueryType.LIST_ONE;
        Ontology ont = Ontology.get(kbName, getOntoquestContext());
        if ( ont != null )
          ontologies.add(ont);
      }
    } catch (Throwable oe) {
      setAppException(oe);
      //      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, oe.getMessage(), oe);
    }
  }
  
  @Get  
  public Representation toXml() {
    if (getAppException() != null)
      return toErrorXml();
    if (queryType == QueryType.DOWNLOAD_ONE) {
      return downloadOntology();
    }
    
    try {
      DomRepresentation representation = new DomRepresentation(
              MediaType.TEXT_XML);
      Document d = representation.getDocument();
      Element succElem = d.createElement("success");
      d.appendChild(succElem);
      Element dataElem = d.createElement("data");
      succElem.appendChild(dataElem);
      
      Element osElem = d.createElement("list");
      dataElem.appendChild(osElem);
      
      for (Ontology ont : ontologies) {
        Element ontElem = ont.toXml(d);
        osElem.appendChild(ontElem);
      }
      
      d.normalizeDocument();

      return representation;
    } catch (Throwable e) {
      e.printStackTrace();
      setAppException(e);
      return toErrorXml();
    }
  }
  
  public Representation downloadOntology() {
    try {
      StringBuilder sb = new StringBuilder();
      URL owlURL = new URL(selectedOntURL);
      URLConnection conn = owlURL.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String inputLine;

      while ((inputLine = in.readLine()) != null) 
        sb.append(inputLine).append('\n');
      in.close();

      // Returns the XML representation of this document.
      return new StringRepresentation(sb.toString(), MediaType.TEXT_XML);
    } catch (IOException e) {
      appException = e;
      return toErrorXml();
    }
  }

}
