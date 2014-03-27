package edu.sdsc.ontoquest.model;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.loader.struct.IdCounter;
import edu.sdsc.ontoquest.loader.struct.NamespaceEntity;

import edu.sdsc.ontoquest.loader.struct.OntologyEntity;

import java.util.Map;
import java.util.Set;

import java.util.TreeMap;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

public class OQOntology implements Comparable<OQOntology> 
{
  private int id;
  private OWLOntology ontology;
  private Map<String,NamespaceEntity> namespaces;
  private KnowledgeBase kb;
  private String defaultPrefix;
  private boolean isDefault;
  private IRI documentIRI;

  public OQOntology(int id, OWLOntology ontology, KnowledgeBase kb, boolean isDefault)
  {
    this.id = id;
    this.ontology = ontology;
    this.kb = kb;
    namespaces = new TreeMap<String,NamespaceEntity>();
    this.isDefault = isDefault;
    documentIRI = null;
  }
  
  int getId () { return id;}
  
  public boolean equals ( Object obj) 
  {
    if (obj instanceof OQOntology) 
    {
      return id == ((OQOntology)obj).getId();
    }
    return false;
  }

  public void setAsDefault() 
  {
    isDefault = true;
  }
  
  @Override
  public int compareTo(OQOntology o2)
  {
    if ( id == o2.getId() ) 
      return 0;
    else if ( id > o2.getId())
      return 1;
    else return -1;
  }
  
  public void fillEntityies(OWLOntologyManager manager) 
            throws OntoquestException
  {
    IdCounter nsCounter = kb.getCounter("sequence.namespace");

    // get name space definitions.
    OWLOntologyFormat format = manager.getOntologyFormat(ontology);
    IRI ontIRI = ontology.getOntologyID().getOntologyIRI();
 
    if (format.isPrefixOWLOntologyFormat()) {
      PrefixOWLOntologyFormat prefixOntFormat = (PrefixOWLOntologyFormat)format;
      Set<String> prefixNames = prefixOntFormat.getPrefixNames();
      defaultPrefix = prefixOntFormat.getDefaultPrefix();
        
      for (String prefixName : prefixNames) {
        String uri = prefixOntFormat.getPrefix(prefixName);
        int id = kb.getPrefixId(uri);
        if (id == -1) {
           id = nsCounter.increment();
           kb.addPrefix(uri, id);
        }
        NamespaceEntity ns = new NamespaceEntity(nsCounter.increment(), prefixName, uri,kb.getKBId(), ontology, id);
        if ( namespaces.put(prefixName, ns) != null ) 
          throw new OntoquestException ( "Duplicate name space prefix " + prefixName +" found in Ontology " + 
                                        ontology.getOntologyID().getOntologyIRI().toString() + 
                                         ".\nPrefix" );
      }
    } else {
          defaultPrefix=ontIRI.getNamespace();          
    }
    
    // get Ontology file path
    documentIRI = manager.getOntologyDocumentIRI(ontology);  
      
    //  


  }
}
