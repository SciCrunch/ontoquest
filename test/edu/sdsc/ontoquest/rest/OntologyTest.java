package edu.sdsc.ontoquest.rest;

import java.util.List;
import java.util.Set;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestTestAdapter;

/**
 * @version $Id: OntologyTest.java,v 1.1 2010-10-28 06:30:36 xqian Exp $
 *
 */
public class OntologyTest extends OntoquestTestAdapter {
  public void testGetURI() throws OntoquestException {
    String kbName = "NIF";
    String uri = Ontology.getURI(kbName, context);
    assertNotNull(uri);
    System.out.println("URI -- " + uri);
  }
  
  public void testGetAll() throws OntoquestException {
    List<Ontology> ontologies = Ontology.getAll(context);
    assertTrue(ontologies.size() > 0);
    for (Ontology ont : ontologies) {
      System.out.println("Ontology Name -- " + ont.getName());
    }
  }
  
  public void testGet() throws OntoquestException {
    Ontology ont = Ontology.get("NIF", context);
    assertNotNull(ont);
    System.out.println("Ontology Title -- " + ont.getTitle());
  }
  
}
