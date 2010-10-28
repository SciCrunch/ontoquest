package edu.sdsc.ontoquest.rest;

import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * This class runs a HTTP server listening on port 8182. It is mainly used for testing REST services.
 * @version $Id: RESTServer.java,v 1.1 2010-10-28 06:29:54 xqian Exp $
 *
 */
public class RESTServer {

    public static void main(String[] args) throws Exception {
        // Create a new Component.
        Component component = new Component();

        // Add a new HTTP server listening on port 8182.
        component.getServers().add(Protocol.HTTP, 8182);

        // Attach the sample application.
        component.getDefaultHost().attach("/ontoquest",
                new OntoquestApplication("NIF"));

        // Start the component.
        component.start();
    }

}