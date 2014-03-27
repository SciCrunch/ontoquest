package edu.sdsc.ontoquest.loader.struct;

import org.semanticweb.owlapi.model.IRI;


public class OntologyEntity {
	private int id;
	private IRI iri;
	private boolean isDefault = false;
	private NamespaceEntity namespace = null;
	private int kbid; 
	private String browserText;  // this field is the same as IRI in db. The loader uses iri.toString() to populate this field.
	private IRI documentIRI;
  private IRI versionIRI;
	
	public OntologyEntity(int id, IRI iri, NamespaceEntity namespace, int kbid, 
			boolean isDefault, String browserText, IRI documentIRI, IRI versionIRI) {
		this.id = id;
		this.iri = iri;
		this.namespace = namespace;
		this.kbid = kbid;
		this.isDefault = isDefault;
		this.browserText = browserText;
		this.documentIRI = documentIRI;
    this.versionIRI = versionIRI;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public IRI getIri() {
		return iri;
	}

	public void setIri(IRI iri) {
		this.iri = iri;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public NamespaceEntity getNamespace() {
		return namespace;
	}

	public void setNamespace(NamespaceEntity namespace) {
		this.namespace = namespace;
	}

	public int getKbid() {
		return kbid;
	}

	public void setKbid(int kbid) {
		this.kbid = kbid;
	}

	public String getBrowserText() {
		return browserText;
	}

	public void setBrowserText(String browserText) {
		this.browserText = browserText;
	}

	public IRI getDocumentIRI() {
		return documentIRI;
	}

	public void setDocumentIRI(IRI documentIRI) {
		this.documentIRI = documentIRI;
	}
  
  public void setVersionIRI(IRI versioniri) 
  {
     this.versionIRI = versioniri;
  }
  
  public IRI getVersionIRI () {return this.versionIRI;}
}
