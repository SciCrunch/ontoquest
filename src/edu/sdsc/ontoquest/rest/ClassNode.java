package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;
import edu.sdsc.ontoquest.db.functions.GetOntologyURL;
import edu.sdsc.ontoquest.query.Utility;

/**
 * @version $Id: ClassNode.java,v 1.2 2012-04-30 22:44:14 xqian Exp $
 *
 */
public class ClassNode extends BaseBean {
	public static String generateId(int rid, int rtid) {
		return rid+"-"+rtid;
	}
	private static HashMap<String, ClassNode> getNodeMap(
			OntoquestFunction<ResourceSet> f,
			boolean useLabel, int[][] ontoIDs, Context context)
					throws OntoquestException {

		// use a hash table to store results. 
		HashMap<String, ClassNode> resultMap = new HashMap<String, ClassNode>(); 
		// use a hash table to remove duplicates of synonyms.
		HashMap<String, String> synMap = new HashMap<String, String>(); 

		//    int[][] nodeIDs = ontoIDs;
		LinkedList<int[]> nodeIDList = new LinkedList<int[]>();

		int rid, rtid;

		ResourceSet rs = f.execute(context, getVarList8());
		while (rs.next()) {
			String prop = rs.getString(8);
			String val = rs.getString(6);
			if (val == null || val.length() == 0)
				continue;
			rid = rs.getInt(1);
			rtid = rs.getInt(2);
			String term = rs.getString(3);
			String compositeID = generateId(rid, rtid);
			ClassNode node = resultMap.get(compositeID);
			if (node == null) {
				//        String nifID = nameMap.get(generateId(rid, rtid));
				//        if (nifID == null) 
				//          nifID = getBasicFunctions().getName(rtid, rid, context);
				node = new ClassNode(compositeID, null, term, new LinkedList<String>(), new LinkedList<String>());
				nodeIDList.add(new int[]{rid, rtid});
			}

			if (getDefinitionPropertySet().contains(prop)) { // a definition edge
				List<String> comments = node.getComments();
				comments.add(val+" ["+rs.getString(8)+"]");
			} else if (getLabelPropertySet().contains(prop)) { // a label edge
				if (!useLabel)
					node.setLabel(rs.getString(6));
			} else if (getSynonymPropertySet().contains(prop)){ // synonym edge
				List<String> existingSynonyms = node.getSynonyms();
				String lowerVal = val.toLowerCase();
				// check if the synonym has appeared 
				if (val.equals(node.getLabel()))
				{}  
				else if (!compositeID.equals(synMap.get(lowerVal))) {           
					existingSynonyms.add(val);
					synMap.put(lowerVal, compositeID);
				}
			} else if (rs.getInt(5) == 13) { // other properties, 13 means literal.
				node.getOtherProperties().add(new String[]{rs.getString(8), val});
			}
			resultMap.put(compositeID, node);
		}
		rs.close();

		// fetch node names and URLs
		int[][] nodeIDs = new int[nodeIDList.size()][2];
		nodeIDList.toArray(nodeIDs);
		HashMap<String, String> nameMap = new HashMap<String, String>();
		HashMap<String, String> urlMap = new HashMap<String, String>();
		if (nodeIDs != null && nodeIDs.length > 0) {
			ResourceSet rs2 = getBasicFunctions().getNames(nodeIDs, context);
			while (rs2.next()) {
				int rid1 = rs2.getInt(1);
				int rtid1 = rs2.getInt(2);
				String name = rs2.getString(3);
				nameMap.put(generateId(rid1, rtid1), name);
			}
			rs2.close();

			ResourceSet rs3 = (new GetOntologyURL(nodeIDs)).execute(context, getVarList3());
			while (rs3.next()) {
				int rid2 = rs3.getInt(1);
				int rtid2 = rs3.getInt(2);
				String url = rs3.getString(3);
				urlMap.put(generateId(rid2, rtid2), url);
			}
			rs3.close();
		}

		for (String compositeID : resultMap.keySet()) {
			resultMap.get(compositeID).setName(nameMap.get(compositeID));
			resultMap.get(compositeID).setUrl(urlMap.get(compositeID));
		}

		return resultMap;
	}

	public static HashMap<String, ClassNode> getSiblings(String inputStr,
			SiblingsType type,
			int kbId, Map<String, Object> attributes, InputType inputType, Context context) 
					throws OntoquestException {

		int edgeType1 = GetNeighbors.EDGE_BOTH;
		String[] includedProperties1 = null;
		String[] excludedProperties = null;
		int chdRidIdx = 1, chdRtidIdx = 2; // the index for child node's rid and
		// rtid in resource set.
		int parRidIdx = 4, parRtidIdx = 5;
		if (type == SiblingsType.CLASSES) {
			edgeType1 = GetNeighbors.EDGE_OUTGOING;
			includedProperties1 = new String[] { "subClassOf" };
			chdRidIdx = 1;
			chdRtidIdx = 2;
			parRidIdx = 4;
			parRtidIdx = 5;
		} else if (type == SiblingsType.PARTS) {
			edgeType1 = GetNeighbors.EDGE_INCOMING;
			includedProperties1 = new String[] { "has_part" };
			chdRidIdx = 4;
			chdRtidIdx = 5;
			parRidIdx = 1;
			parRtidIdx = 2;
		}

		// get parents first
		HashSet<String> originalNodes = new HashSet<String>();
		HashMap<String, int[]> parentNodes = new HashMap<String, int[]>();

		OntoquestFunction<ResourceSet> f = null;

		if (inputType == InputType.OID) {
			int[] ontoId = ClassNode.parseId(inputStr);
			f = new GetNeighbors(ontoId[0], ontoId[1], kbId, includedProperties1,
					excludedProperties, edgeType1, true, true, 1, true);
			originalNodes.add(inputStr);
			// nodeMap.put(ClassNode.generateId(ontoId[0], ontoId[1]), ontoId); // add
			// original
		} else if (inputType == InputType.NAME) {
			f = new GetNeighbors(new String[] { inputStr }, kbId, true,
					includedProperties1, excludedProperties, edgeType1, true, true, 1,
					true);
		} else { // default type: InputType.TERM
			f = new GetNeighbors(new String[] { inputStr }, kbId,
					includedProperties1, excludedProperties, true, edgeType1, true, true,
					1, true);
		}

		ResourceSet rs = f.execute(context, getVarList8());
		while (rs.next()) {
			// add input nodes
			int chdRid = rs.getInt(chdRidIdx);
			int chdRtid = rs.getInt(chdRtidIdx);
			originalNodes.add(ClassNode.generateId(chdRid, chdRtid));

			// add parent nodes
			int parRid = rs.getInt(parRidIdx);
			int parRtid = rs.getInt(parRtidIdx);
			parentNodes.put(ClassNode.generateId(parRid, parRtid), new int[] {
				parRid, parRtid });
		}
		rs.close();

		if (parentNodes.size() == 0) { // no match, return empty graph
			return new HashMap<String, ClassNode>();
		}

		// From each parent node, get all children except the input one.
		int[][] parentNodeArray = new int[parentNodes.size()][2];
		parentNodes.values().toArray(parentNodeArray);

		int edgeType = GetNeighbors.EDGE_BOTH;
		String[] includedProperties = null;
		if (type == SiblingsType.CLASSES) {
			edgeType = GetNeighbors.EDGE_INCOMING;
			includedProperties = new String[] { "subClassOf" };
		} else if (type == SiblingsType.PARTS) {
			edgeType = GetNeighbors.EDGE_OUTGOING;
			includedProperties = new String[] { "has_part" };
		}

		HashMap<String, int[]> nodeMap = new HashMap<String, int[]>();
		f = new GetNeighbors(parentNodeArray, kbId, includedProperties, 
				excludedProperties, edgeType, true, true, 1, true);
		rs = f.execute(context, getVarList8());
		while (rs.next()) {
			int[] id1 = new int[] { rs.getInt(chdRidIdx), rs.getInt(chdRtidIdx) };
			String idStr = ClassNode.generateId(id1[0], id1[1]);
			if (!originalNodes.contains(idStr) && !nodeMap.containsKey(idStr)) {
				nodeMap.put(idStr, id1);
			}
		}
		rs.close();
		int[][] idArray = new int[nodeMap.size()][2];
		nodeMap.values().toArray(idArray);

		return ClassNode.get(idArray, context);
	}
	public static int[] parseId(String id) throws OntoquestException {
		int[] ontoId = new int[2];
		int idx = id.indexOf('-');
		if (idx <= 0) {
			throw new OntoquestException(OntoquestException.Type.INPUT, "Invalid OntoQuest ID: " + id);
		}

		try {
			ontoId[0] = Integer.parseInt(id.substring(0, idx));
			ontoId[1] = Integer.parseInt(id.substring(idx+1));   
		} catch (NumberFormatException nfe) {
			throw new OntoquestException(OntoquestException.Type.INPUT, "Invalid OntoQuest ID: " + id);
		}

		return ontoId;
	}
	/**
	 * Search concepts by input <code>term</code>.
	 * @param term the search term
	 * @param attributes optional request parameters.
	 * @param defaultKbId the default kbid. If no kbid is specified, the default kbid is used. 
	 * The default kb name is declared in web.xml.
	 * @param context ontoquest context
	 * @return
	 * @throws OntoquestException
	 */
	public static HashMap<String, ClassNode> search(String term,
			Map<String, Object> attributes, int kbId, Context context)
					throws OntoquestException {
		// 1. prepare input parameters
		//    int kbId = defaultKbId;
		//    Object kbObj = attributes.get("ontology");
		//    if (kbObj != null) {
		//      String kbStr = Reference.decode((String)kbObj);
		//      kbId = getBasicFunctions().getKnowledgeBaseID(kbStr, context);
		//    }

		int resultLimit = DefaultResultLimit;
		Object rlObj = attributes.get("result_limit");
		if (rlObj != null) {
			try {
				resultLimit = Integer.parseInt(rlObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}

		boolean beginWith = false;
		Object bwObj = attributes.get("begin_with");
		if (bwObj != null) {
			try {
				beginWith = Boolean.parseBoolean(bwObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}

		int maxEditDistance = DefaultMaxEditDistance;
		Object medObj = attributes.get("max_ed");
		if (medObj != null) {
			try {
				maxEditDistance = Integer.parseInt(medObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}

		boolean negated = false;
		Object nObj = attributes.get("negated");
		if (nObj != null) {
			try {
				negated = Boolean.parseBoolean(nObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}

		// 2. Find matching terms first
		ResourceSet rs = getBasicFunctions().searchTerm(term, new int[]{kbId}, context, getVarList1(), 
				resultLimit, negated, maxEditDistance, beginWith);
		LinkedList<String> matchedTerms = new LinkedList<String>();
		while (rs.next()) {
			matchedTerms.add(rs.getString(1));
		}
		rs.close();

		if (matchedTerms.size() == 0) {
			return new HashMap<String, ClassNode>(); // no match, return empty result.
		}

		// 3. Fetch corresponding ClassNode objects for the matching terms.
		String[] terms = new String[matchedTerms.size()];
		matchedTerms.toArray(terms);
		return getByLabel(terms, kbId, context);
	}
	private int rid;
	private int rtid;
	private String label; // rdfs:label
	private String name; // class name, e.g. birnlex_802

	private String url; // ontology url

	private List<String> comments;
	private List<String> synonyms;
	private List<String[]> otherProperties; // String[0] -- property name,
	// String[1] -- property value
	private List<SimpleClassNode> superclasses = new LinkedList<SimpleClassNode>(); // xufei,

	// 04/2012,
	// requested
	// by
	// Anita
	private static     String[] getClassProperties = null;

	public static final int DefaultResultLimit = 100;

	public static final int DefaultMaxEditDistance = 50;

	public static HashMap<String, ClassNode> get(int[][] ontoIDs, Context context)
			throws OntoquestException {
		if (ontoIDs == null || ontoIDs.length == 0)
			return new HashMap<String, ClassNode>();

		OntoquestFunction<ResourceSet> f = new GetNeighbors(ontoIDs, 0, getClassProperties, null, 
				GetNeighbors.EDGE_OUTGOING, true, false, 1, false);
		return getNodeMap(f, true, ontoIDs, context);
	}

	// private static Set<ClassNode> get(OntoquestFunction<ResourceSet> f,
	// boolean useLabel, int[][] ontoIDs, Context context) throws
	// OntoquestException {
	// HashMap<String, ClassNode> resultMap = getNodeMap(f, useLabel, ontoIDs,
	// context);
	// return new HashSet<ClassNode>(resultMap.values());
	// }
	//
	public static HashMap<String, ClassNode> get(String idStr, Context context)
			throws OntoquestException {
		String[] ids = idStr.split(";");
		int[][] ontoIDs = new int[ids.length][2];
		for (int i=0; i<ids.length; i++) {
			ontoIDs[i] = parseId(ids[i]);
		}
		return get(ontoIDs, context);
	}

	public static HashMap<String, ClassNode> getByLabel(String term, int kbId,
			Context context) throws OntoquestException {
		String[] terms = term.split(";");
		return getByLabel(terms, kbId, context);

	}

	public static HashMap<String, ClassNode> getByLabel(String[] terms, int kbId,
			Context context) throws OntoquestException {
		OntoquestFunction<ResourceSet> f = new GetNeighbors(terms, kbId,
				getClassProperties, null, true, GetNeighbors.EDGE_OUTGOING, true,
				false, 1, false);
		return getNodeMap(f, true, null, context);
	}

	public static HashMap<String, ClassNode> getByName(String name, int kbId,
			Context context) throws OntoquestException {
		String[] names = name.split(";");
		OntoquestFunction<ResourceSet> f = new GetNeighbors(names, kbId, true, getClassProperties, null,
				GetNeighbors.EDGE_OUTGOING, true, false, 1, false);
		return getNodeMap(f, false, null, context);
	}

	public ClassNode(String id, String name, String label, List<String> comments, 
			List<String> synonyms) throws OntoquestException {
		setId(id);
		setLabel(label);
		setName(name);
		setComments(comments);
		setSynonyms(synonyms);
		otherProperties = new LinkedList<String[]>();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ClassNode)) 
			return false;

		ClassNode c = (ClassNode)o;
		return this.getId().equals(c.getId());
	}

	/**
	 * @return the comments
	 */
	public List<String> getComments() {
		return comments;
	}

	public String getId() {
		return generateId(rid, rtid);
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the properties
	 */
	public List<String[]> getOtherProperties() {
		return otherProperties;
	}

	/**
	 * @return the rid
	 */
	public int getRid() {
		return rid;
	}

	/**
	 * @return the rtid
	 */
	public int getRtid() {
		return rtid;
	}

	public List<SimpleClassNode> getSuperclasses() {
		return superclasses;
	}

	/**
	 * @return the synonyms
	 */
	public List<String> getSynonyms() {
		return synonyms;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(List<String> comments) {
		this.comments = comments;
	}

	public void setId(String id) throws OntoquestException {
		int[] ontoId = parseId(id);
		rid = ontoId[0];
		rtid = ontoId[1];
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setOtherProperties(List<String[]> otherProperties) {
		this.otherProperties = otherProperties;
	}

	/**
	 * @param rid the rid to set
	 */
	public void setRid(int rid) {
		this.rid = rid;
	}

	/**
	 * @param rtid the rtid to set
	 */
	public void setRtid(int rtid) {
		this.rtid = rtid;
	}

	public void setSuperclasses(List<SimpleClassNode> superclasses) {
		this.superclasses = superclasses;
	}

	/**
	 * @param synonyms the synonyms to set
	 */
	public void setSynonyms(List<String> synonyms) {
		this.synonyms = synonyms;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public Element toXml(Document doc) {
		Element e = doc.createElement("class");

		Element idElem = doc.createElement("id");
		e.appendChild(idElem);
		idElem.appendChild(doc.createTextNode(getId()));

		Element nameElem = doc.createElement("name");
		e.appendChild(nameElem);
		nameElem.appendChild(doc.createTextNode(getName()));

		Element labelElem = doc.createElement("label");
		e.appendChild(labelElem);
		if (getLabel() != null)
			labelElem.appendChild(doc.createTextNode(getLabel()));

		Element urlElem = doc.createElement("url");
		e.appendChild(urlElem);
		if (getUrl() != null)
			urlElem.appendChild(doc.createTextNode(getUrl()));

		if (!Utility.isBlank(comments)) {
			Element commentsElem = doc.createElement("comments");
			e.appendChild(commentsElem);
			for (String comment : comments) {
				Element commentElem = doc.createElement("comment");
				commentElem.appendChild(doc.createTextNode(comment));
				commentsElem.appendChild(commentElem);
			}
		}

		if (!Utility.isBlank(synonyms)) {
			Element synonymsElem = doc.createElement("synonyms");
			e.appendChild(synonymsElem);
			for (String synonym : synonyms) {
				Element synonymElem = doc.createElement("synonym");
				synonymElem.appendChild(doc.createTextNode(synonym));
				synonymsElem.appendChild(synonymElem);
			}
		}

		if (!Utility.isBlank(otherProperties)) {
			Element opElem = doc.createElement("other_properties");
			e.appendChild(opElem);
			for (String[] prop : otherProperties) {
				Element propElem = doc.createElement("property");
				propElem.setAttribute("name", prop[0]);
				propElem.appendChild(doc.createTextNode(prop[1]));
				opElem.appendChild(propElem);
			}
		}

		if (!Utility.isBlank(superclasses)) {
			Element opElem = doc.createElement("superclasses");
			e.appendChild(opElem);
			for (SimpleClassNode node : superclasses) {
				Element superElem = node.toXml(doc);
				opElem.appendChild(superElem);
			}
		}

		return e;
	}

}
