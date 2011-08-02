package edu.sdsc.ontoquest.rest;

/**
 * meta data for Search Reconcile Service.
 * <p>
 * $Id: SearchReconcileMetaData.java,v 1.1 2011-08-02 17:33:17 xqian Exp $
 * 
 */
public class SearchReconcileMetaData {
	private String name = "NIFSTD Reconciliation Service";
	private String identifierSpace = "http://ontology.neuinfo.org/NIF";
	private String schemaSpace = "http://ontology.neuinfo.org/NIF.id";
	private String viewURL = "http://neurolex.org/wiki/{{id}}";

	public SearchReconcileMetaData() {
	}

	public SearchReconcileMetaData(String name, String identifierSpace,
			String schemaSpace, String viewURL) {
		setName(name);
		setIdentifierSpace(identifierSpace);
		setSchemaSpace(schemaSpace);
		setViewURL(viewURL);
	}

	public String getIdentifierSpace() {
		return identifierSpace;
	}

	public String getName() {
		return name;
	}

	public String getSchemaSpace() {
		return schemaSpace;
	}
	public String getViewURL() {
		return viewURL;
	}
	public void setIdentifierSpace(String identifierSpace) {
		this.identifierSpace = identifierSpace;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setSchemaSpace(String schemaSpace) {
		this.schemaSpace = schemaSpace;
	}

	public void setViewURL(String viewURL) {
		this.viewURL = viewURL;
	}
}
