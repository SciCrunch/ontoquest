package edu.sdsc.ontoquest.model;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.loader.struct.IdCounter;

import edu.sdsc.ontoquest.loader.struct.NamespaceEntity;
import edu.sdsc.ontoquest.query.Utility;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

/**
 *  OntoQuest Knowledge Base
 */
public class KnowledgeBase
{
  private int kbid;

  private OQOntology defaultOntology;
  
  private Map<String,Integer> prefixMap;
  
  private Set<OQOntology> ontologies;
  
  private Map<String, IdCounter> counterMap;
  
  private Map<String, OQProperty> propertyMap;
  
  public KnowledgeBase(int kbid, Map<String, IdCounter> counterMap)
  {
    this.kbid = kbid;
    prefixMap = new TreeMap<String,Integer>();
    ontologies = new TreeSet<OQOntology> ();
    defaultOntology = null;
    this.counterMap = counterMap;
  }
 
  /**
   * add a prefix and its ID to prefix map. If the prefix already exists in
   *  the map, this function doesn't do anything. 
   * @param prefix the String presentation of prefix.
   * @param id
   * 
   *  TODO: Maybe throw exception of the prefix already exists.
   */
  public void addPrefix (String prefix, int id) 
  {
    if ( !prefixMap.containsKey(prefix))
      prefixMap.put(prefix,Integer.valueOf(id));
  }
  
  /**
   * Get the internal id of a prefix String
   * @param prefix
   * @return the id of the prefix. If the prefix is not found in the prefix map, 
   * return -1;
   */
  public int getPrefixId (String prefix) 
  {
    Integer i = prefixMap.get(prefix);
    if ( i == null)
       return -1 ;
    return i.intValue();     
    //return prefixMap.get(prefix);
  }
  
  public int getKBId () { return kbid;}
  
  public void initialize (OWLOntologyManager manager, OWLOntology defaultOntology)
                  throws OntoquestException
  {
    IdCounter ontologyCounter = getCounter("sequence.ontology_uri");

    Set<OWLOntology> ontologies = manager.getOntologies();
    for (OWLOntology ontology : ontologies) {
      OQOntology o = new OQOntology (ontologyCounter.increment(), ontology, this, false);
      if (ontology.equals(defaultOntology)) {
        this.defaultOntology = o;
        o.setAsDefault();
      }
      o.fillEntityies(manager);
      this.ontologies.add(o);
      
    } 
  }
  
  public IdCounter getCounter(String seqProp)
                     throws OntoquestException {
    IdCounter counter = counterMap.get(seqProp);
    Utility.checkNull(counter, OntoquestException.Type.SETTING, "The property "
        + seqProp + " is not found in the configuration. Please check.");
    return counter;
  }
    
    
}
