package edu.sdsc.ontoquest.model;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;

public class OQProperty
{
  private OQOntology ontology;
  
  private int id;
  
  private OWLEntity property;
  
  public OQProperty(int id, OWLEntity property, OQOntology ontology)
  {
    this.id = id;
    this.property = property;
    this.ontology = ontology;
  }
  
  public OQOntology getOntlogy () { return ontology; }
  
  public int getId () { return id;}
  
}
