package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;
import edu.sdsc.ontoquest.db.functions.GetNeighbors.PropertyType;
import edu.sdsc.ontoquest.query.Utility;

/**
 * @version $Id: SimpleClassNode.java,v 1.3 2013-06-21 22:28:30 jic002 Exp $
 *
 */
public class SimpleClassNode extends BasicClassNode {
	private static Set<SimpleClassNode> get(OntoquestFunction<ResourceSet> f, 
			boolean hasLabel, int[][] ontoIDs, Context context) throws OntoquestException {
		Map<String, SimpleClassNode> resultMap = getNodeMap(f, hasLabel, ontoIDs,
				context);
		return new HashSet<SimpleClassNode>(resultMap.values());

	}

	public static Set<SimpleClassNode> get(String id, PropertyType[] propTypes,
			Context context) throws OntoquestException {
		int[] ontoId = ClassNode.parseId(id);
		OntoquestFunction<ResourceSet> f = new GetNeighbors(
				new int[][] {{ontoId[0], ontoId[1]}}, 0, null, null, 
				GetNeighbors.EDGE_OUTGOING, true, false, 1, false, propTypes);
		return get(f, true, null, context);
	}

	public static Set<SimpleClassNode> getByLabel(String term, int kbId, PropertyType[] propTypes, Context context) throws OntoquestException {
		return getByLabel(new String[]{term}, kbId, propTypes, context);

	}

	public static Set<SimpleClassNode> getByLabel(String[] terms, int kbId, PropertyType[] propTypes, Context context) throws OntoquestException {
		OntoquestFunction<ResourceSet> f = new GetNeighbors(terms, kbId, 
				null, null, true, GetNeighbors.EDGE_OUTGOING, true, false, 1, false, propTypes);
		return get(f, true, null, context);
	}

	public static Set<SimpleClassNode> getByName(String name, int kbId, PropertyType[] propTypes, Context context) throws OntoquestException {
		OntoquestFunction<ResourceSet> f = new GetNeighbors(new String[]{name}, kbId, true, null, null,
				GetNeighbors.EDGE_OUTGOING, true, false, 1, false, propTypes);
		return get(f, false, null, context);
	}

	private static Map<String, SimpleClassNode> getNodeMap(
			OntoquestFunction<ResourceSet> f,
			boolean hasLabel, int[][] ontoIDs, Context context) throws OntoquestException {

		// use a hash table to store results. 
		HashMap<String, SimpleClassNode> resultMap = new HashMap<String, SimpleClassNode>(); 

		LinkedList<int[]> nodeIDList = new LinkedList<int[]>();    
		int rid, rtid;

		ResourceSet rs = f.execute(context, getVarList8());
		while (rs.next()) {
			//      String prop = rs.getString(8);
			String val = rs.getString(6);
			if (val == null || val.length() == 0)
				continue;
			rid = rs.getInt(1);
			rtid = rs.getInt(2);
			String term = rs.getString(3);
			String compositeID = ClassNode.generateId(rid, rtid);
			SimpleClassNode node = resultMap.get(compositeID);
			if (node == null) {
				node = new SimpleClassNode(compositeID, null, term, new LinkedList<String[]>());
				nodeIDList.add(new int[]{rid, rtid});
			}

			node.getProperties().add(new String[]{rs.getString(8), val});

			resultMap.put(compositeID, node);
		}
		rs.close();

		// fetch node names
		int[][] nodeIDs = new int[nodeIDList.size()][2];
		nodeIDList.toArray(nodeIDs);

		HashMap<String, String> nameMap = new HashMap<String, String>();
		if (nodeIDs != null && nodeIDs.length > 0) {
			ResourceSet rs2 = getBasicFunctions().getNames(nodeIDs, context);
			while (rs2.next()) {
				int rid1 = rs2.getInt(1);
				int rtid1 = rs2.getInt(2);
				String name = rs2.getString(3);
				nameMap.put(ClassNode.generateId(rid1, rtid1), name);
			}
			rs2.close();
		}

		for (String compositeID : resultMap.keySet()) {
			resultMap.get(compositeID).setName(nameMap.get(compositeID));
		}

		// fetch node labels if necessary
		if (!hasLabel) {
			HashMap<String, String> labelMap = new HashMap<String, String>();
			if (nodeIDs != null && nodeIDs.length > 0) {
				ResourceSet rs2 = getBasicFunctions().getLabels(nodeIDs, context);
				while (rs2.next()) {
					int rid1 = rs2.getInt(1);
					int rtid1 = rs2.getInt(2);
					String label = rs2.getString(3);
					labelMap.put(ClassNode.generateId(rid1, rtid1), label);
				}
				rs2.close();
			}
			for (String compositeID : resultMap.keySet()) {
				resultMap.get(compositeID).setLabel(labelMap.get(compositeID));
			}
		}

		return resultMap;
	}

	private List<String[]> properties; // String[0] -- property name, String[1] -- property value

	public SimpleClassNode(int rid, int rtid, String name, String label, List<String[]> properties) {
		setRid(rid);
		setRtid(rtid);
		setLabel(label);
		setName(name);
		setProperties(properties);		
	}

	public SimpleClassNode(String id, String name, String label, List<String[]> properties) 
			throws OntoquestException {
		int[] ontoId = ClassNode.parseId(id);
		setRid(ontoId[0]);
		setRtid(ontoId[1]);
		setLabel(label);
		setName(name);
		setProperties(properties);
	}
	/**
	 * @return the properties
	 */
	public List<String[]> getProperties() {
		return properties;
	}
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(List<String[]> properties) {
		this.properties = properties;
	}
	/* (non-Javadoc)
	 * @see edu.sdsc.ontoquest.rest.BaseBean#toXml(org.w3c.dom.Document)
	 */
	@Override
	public Element toXml(Document doc) {
		Element e = doc.createElement("class");

		Element idElem = doc.createElement("id");
		e.appendChild(idElem);
    idElem.setAttribute(ClassNode.interanIdAttrName, ClassNode.generateId(getRid(), getRtid()));
    String n = getName();
		idElem.appendChild(doc.createTextNode(n));

		if (!Utility.isBlank(getName())) {
			Element nameElem = doc.createElement("name");
			e.appendChild(nameElem);
			nameElem.appendChild(doc.createTextNode(getName()));
		}

		if (!Utility.isBlank(getLabel())) {
			Element labelElem = doc.createElement("label");
			e.appendChild(labelElem);
			if (getLabel() != null)
				labelElem.appendChild(doc.createTextNode(getLabel()));
		}

		if (!Utility.isBlank(properties)) {
			Element opElem = doc.createElement("properties");
			e.appendChild(opElem);
			for (String[] prop : properties) {
				Element propElem = doc.createElement("property");
				propElem.setAttribute("name", prop[0]);
				propElem.appendChild(doc.createTextNode(prop[1]));
				opElem.appendChild(propElem);
			}
		}

		return e;
	}

}
