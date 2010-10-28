package edu.sdsc.ontoquest.rest;

/**
 * @version $Id: BaseResource.java,v 1.1 2010-10-28 06:29:55 xqian Exp $
 *
 */
import java.io.IOException;

import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;

/**
 * Base resource class that supports common behaviors or attributes shared by
 * all resources.
 * 
 */
public abstract class BaseResource extends ServerResource {

    Throwable appException = null;
    
    protected void setAppException(Throwable e) {
      this.appException = e;
    }
    
    protected Throwable getAppException() {
      return appException;
    }
    
    protected Representation toErrorXml() {
      if (appException == null)
        return null;
      try {
        DomRepresentation representation = new DomRepresentation(
                MediaType.TEXT_XML);
        // Generate a DOM document representing the item.
        Document d = representation.getDocument();
        Element failElem = d.createElement("failure");
        d.appendChild(failElem);
        if (appException instanceof OntoquestException) {
          Element errTypeElem = d.createElement("type");
          errTypeElem.appendChild(d.createTextNode(((OntoquestException)appException).getType().name()));
          failElem.appendChild(errTypeElem);
        }
        
        Element errMsgElem = d.createElement("message");
        String message = appException.getMessage();
        if (message == null)
          message = appException.getClass().getName();
        errMsgElem.appendChild(d.createTextNode(message));
        failElem.appendChild(errMsgElem);
       
        d.normalizeDocument();

        // Returns the XML representation of this document.
        return representation;
      } catch (IOException e) {
        e.printStackTrace();
      }

      return null;
    }
    
    /**
     * Returns the context object used by Ontoquest engine.
     * 
     * @return the context object used by Ontoquest engine.
     */
    protected Context getOntoquestContext() {
      return ((OntoquestApplication) getApplication()).getOntoquestContext();
    }
}

