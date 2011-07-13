package edu.sdsc.ontoquest.rest;

/**
 * @version $Id: BaseResource.java,v 1.2 2011-07-13 12:15:42 xqian Exp $
 *
 */
import java.io.IOException;

import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.ext.json.JsonRepresentation;
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

	protected Throwable getAppException() {
		return appException;
	}

	/**
	 * Returns the context object used by Ontoquest engine.
	 * 
	 * @return the context object used by Ontoquest engine.
	 */
	protected Context getOntoquestContext() {
		return ((OntoquestApplication) getApplication()).getOntoquestContext();
	}

	protected void setAppException(Throwable e) {
		this.appException = e;
	}

	protected Representation toErrorJSON() {
		if (appException == null)
			return null;

		try {
			JSONObject errorObj = new JSONObject();
			errorObj.put("error_type", ((OntoquestException) appException).getType()
					.name());
			errorObj.put("error_message", appException.getMessage());
			JsonRepresentation representation = new JsonRepresentation(errorObj);
			return representation;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
}

