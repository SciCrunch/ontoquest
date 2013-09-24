package edu.sdsc.ontoquest.rest;

import java.util.ArrayList;
import java.util.HashSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: BaseBean.java,v 1.7 2013-09-24 23:14:37 jic002 Exp $
 * Base bean class that supports common behaviors or attributes shared by
 * all beans.
 * 
 */
public abstract class BaseBean {
	public enum InputType { OID, NAME, TERM, ID }
  
  // added ALLSUBCLASSES to use new function and search fat nodes.
	public enum NeighborType { PARENTS, CHILDREN, SUBCLASSES, SUPERCLASSES, PARTS, WHOLE, ALL,EDGE_RELATION,ALLSUBCLASSES,PARTOF }

	public enum SiblingsType { CLASSES, PARTS }

	private static ArrayList<Variable> varList8 = null;
	private static ArrayList<Variable> varList5 = null;
	private static ArrayList<Variable> varList3 = null;
	private static ArrayList<Variable> varList1 = null;
	private static HashSet<String> definitionPropertySet = null;
	private static HashSet<String> labelPropertySet = null;
	//  private static HashSet<String> externalSourcePropertySet = null;
	private static HashSet<String> synonymPropertySet = null;
	private static BasicFunctions basicFunctions = DbBasicFunctions.getInstance();

	/**
	 * @return the basicFunctions
	 */
	public static BasicFunctions getBasicFunctions() {
		return basicFunctions;
	}

	/**
	 * @return the definitionPropertySet
	 */
	public static HashSet<String> getDefinitionPropertySet() {
		if (definitionPropertySet == null)
			setDefinitionPropertySet();
		return definitionPropertySet;
	}

	public static HashSet<String> getLabelPropertySet() {
		if (labelPropertySet == null)
			setLabelPropertySet();
		return labelPropertySet;
	}

	/**
	 * @return the synonymPropertySet
	 */
	public static HashSet<String> getSynonymPropertySet() {
		if (synonymPropertySet == null)
			setSynonymPropertySet();
		return synonymPropertySet;
	}

	//  private static synchronized void setExternalSourcePropertySet() {
	//    externalSourcePropertySet = new HashSet<String>();
	//    externalSourcePropertySet.add("externalSourceURI");
	//    externalSourcePropertySet.add("hasExternalSource");
	//  }

	public static ArrayList<Variable> getVarList1() {
		if (varList1 == null)
			setVarLists();
		return varList1;
	}

	public static ArrayList<Variable> getVarList3() {
		if (varList3 == null)
			setVarLists();
		return varList3;
	}

	public static ArrayList<Variable> getVarList5() {
		if (varList5 == null)
			setVarLists();
		return varList5;
	}
	/**
	 * @return the varList8
	 */
	public static ArrayList<Variable> getVarList8() {
		if (varList8 == null)
			setVarLists();
		return varList8;
	}

	private static synchronized void setDefinitionPropertySet() {

		// initialize definitionPropertyMap
		definitionPropertySet = new HashSet<String>();
		definitionPropertySet.add("definition");
		definitionPropertySet.add("description");
		definitionPropertySet.add("externallySourcedDefinition");
		definitionPropertySet.add("birnlexDefinition");
		// definitionPropertySet.add("birnlexComment");
		// definitionPropertySet.add("comment");
		definitionPropertySet.add("tempDefinition");

	}

	private static synchronized void setLabelPropertySet() {
		labelPropertySet = new HashSet<String>();
		labelPropertySet.add("label");
		labelPropertySet.add("prefLabel");
	}

	private static synchronized void setSynonymPropertySet() {
		synonymPropertySet = new HashSet<String>();
		synonymPropertySet.add("synonym"); 
		synonymPropertySet.add("abbrev");
		synonymPropertySet.add("hasExactSynonym"); 
		synonymPropertySet.add("hasRelatedSynonym"); 
		synonymPropertySet.add("acronym"); 
		synonymPropertySet.add("altLabel"); 
		synonymPropertySet.add("taxonomicCommonName"); 
		synonymPropertySet.add("ncbiTaxScientificName"); 
		synonymPropertySet.add("ncbiTaxGenbankCommonName"); 
		synonymPropertySet.add("ncbiTaxBlastName"); 
		synonymPropertySet.add("ncbiIncludesName"); 
		synonymPropertySet.add("ncbiInPartName"); 
		synonymPropertySet.add("hasNarrowSynonym"); 
		synonymPropertySet.add("misspelling"); 
		synonymPropertySet.add("misnomer"); 
		synonymPropertySet.add("hasBroadSynonym"); 
	}

	//  public static HashSet<String> getExternalSourcePropertySet() {
	//    if (externalSourcePropertySet == null)
	//      setExternalSourcePropertySet();
	//    return externalSourcePropertySet;
	//  }

	private static synchronized void setVarLists() {
		varList1 = new ArrayList<Variable>(1);
		varList1.add(new Variable("v1", 1));

		varList3 = new ArrayList<Variable>(3);
		varList3.add(new Variable("v1", 1));
		varList3.add(new Variable("v2", 1));
		varList3.add(new Variable("v3", 1));

		varList5 = new ArrayList<Variable>(5);
		varList5.add(new Variable("v1", 1));
		varList5.add(new Variable("v2", 1));
		varList5.add(new Variable("v3", 1));
		varList5.add(new Variable("v4", 1));
		varList5.add(new Variable("v5", 1));

		varList8 = new ArrayList<Variable>(8);
		varList8.add(new Variable("v1", 1));
		varList8.add(new Variable("v2", 1));
		varList8.add(new Variable("v3", 1));
		varList8.add(new Variable("v4", 1));
		varList8.add(new Variable("v5", 1));
		varList8.add(new Variable("v6", 1));
		varList8.add(new Variable("v7", 1));
		varList8.add(new Variable("v8", 1));   
	}

	public abstract Element toXml(Document doc);
}
