package edu.sdsc.ontoquest.loader.struct;

import org.semanticweb.owlapi.model.OWLOntology;

public class NamespaceEntity {
	private String prefix;
	private String uri;
	private int kbid;
	private int id;
	private boolean isInternal = false;
	private OWLOntology ontology = null;
	
	public NamespaceEntity(int id, String prefix, String uri, int kbid, OWLOntology ontology) {
		this.id = id;
		this.prefix = prefix;
		this.uri = uri;
		this.kbid = kbid;
		this.setOntology(ontology);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getKbid() {
		return kbid;
	}

	public void setKbid(int kbid) {
		this.kbid = kbid;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isInternal() {
		return isInternal;
	}

	public void setInternal(boolean isInternal) {
		this.isInternal = isInternal;
	}

	public OWLOntology getOntology() {
		return ontology;
	}

	public void setOntology(OWLOntology ontology) {
		this.ontology = ontology;
	}
}
