package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;

/**
 * @version $Id: OntGraph.java,v 1.3 2013-06-21 22:28:27 jic002 Exp $
 *
 */
public class OntGraph extends BaseBean {

	private static final String[] defaultExcludedProperties = {"disjointWith", "sameAs"};

	private static int extractLevel(Map<String, Object> attributes) throws OntoquestException {
		int hops = 0;

		try {        
			if (attributes.containsKey("level")) {
				hops = Integer.valueOf(attributes.get("level").toString());
			}
		} catch (NumberFormatException e) {
			throw new OntoquestException("Invalid level parameter -- " + attributes.containsKey("level"));
		}

		return hops;
	}

	//  public OntGraph(Set<ClassNode> nodes, Set<Relationship> edges) {
	//    this.nodes = nodes;
	//    this.edges = edges;
	//  }

	public static OntGraph get(int[][] nodeIDs, NeighborType type, int kbId,
			Map<String, Object> attributes, Context context)
					throws OntoquestException {
		int hops = extractLevel(attributes);
		int edgeType = GetNeighbors.EDGE_BOTH;
		String[] includedProperties = null;
		String[] excludedProperties = defaultExcludedProperties;

		if (type == NeighborType.SUBCLASSES) {
			edgeType = GetNeighbors.EDGE_INCOMING;
			includedProperties = new String[] { "subClassOf" };
		} else if (type == NeighborType.SUPERCLASSES) {
			edgeType = GetNeighbors.EDGE_OUTGOING;
			includedProperties = new String[] { "subClassOf" };
		} else if (type == NeighborType.PARTS) {
			edgeType = GetNeighbors.EDGE_OUTGOING;
			includedProperties = new String[] { "has_part" };
		} else if (type == NeighborType.WHOLE) {
			edgeType = GetNeighbors.EDGE_INCOMING;
			includedProperties = new String[] { "has_part" };
		} else if (type == NeighborType.ALL) {
			edgeType = GetNeighbors.EDGE_BOTH;
		} else if (type == NeighborType.PARENTS) {
			edgeType = GetNeighbors.EDGE_INCOMING;
		} else if (type == NeighborType.CHILDREN) {
			edgeType = GetNeighbors.EDGE_OUTGOING;
		}

		HashMap<String, int[]> nodeMap = new HashMap<String, int[]>();
		OntoquestFunction<ResourceSet> f = null;
		f = new GetNeighbors(nodeIDs, kbId, includedProperties,
				excludedProperties, edgeType, true, true, hops, true);

		return getRelatedClasses(f, nodeMap,
				new HashSet<Relationship>(), context);
	}

	public static OntGraph get(String inputStr, NeighborType type, int kbId,
			Map<String, Object> attributes, InputType inputType, Context context ) throws OntoquestException {

		int hops = extractLevel(attributes);
		int edgeType = GetNeighbors.EDGE_BOTH;
		String[] includedProperties = null;
		String[] excludedProperties = defaultExcludedProperties;

		if (type == NeighborType.SUBCLASSES) {
			edgeType = GetNeighbors.EDGE_INCOMING;
			includedProperties = new String[]{"subClassOf"};
		} else if (type == NeighborType.SUPERCLASSES) {
			edgeType = GetNeighbors.EDGE_OUTGOING;
			includedProperties = new String[]{"subClassOf"};
		} else if (type == NeighborType.PARTS) {
			edgeType = GetNeighbors.EDGE_OUTGOING;
			includedProperties = new String[] { "has_part" };
		} else if (type == NeighborType.WHOLE) {
			edgeType = GetNeighbors.EDGE_INCOMING;
			includedProperties = new String[] { "has_part" };
		} else if (type == NeighborType.ALL) {
			edgeType = GetNeighbors.EDGE_BOTH;
		} else if (type == NeighborType.PARENTS) {
			edgeType = GetNeighbors.EDGE_INCOMING;
		} else if (type == NeighborType.CHILDREN) {
			edgeType = GetNeighbors.EDGE_OUTGOING;
		}

		HashMap<String, int[]> nodeMap = new HashMap<String, int[]>();
		OntoquestFunction<ResourceSet> f = null;

		if (inputType == InputType.OID) {
			int[] ontoId = ClassNode.parseId(inputStr);
			f = new GetNeighbors(ontoId[0], ontoId[1], kbId, includedProperties, 
					excludedProperties, edgeType, true, true, hops, true);
			nodeMap.put(ClassNode.generateId(ontoId[0], ontoId[1]), ontoId); // add original
		} else if (inputType == InputType.NAME) {
			ResourceSet rs = getBasicFunctions().searchAllIDsByName(inputStr, new int[]{kbId}, true, context, getVarList3());
			LinkedList<int[]> idList = new LinkedList<int[]>();
			while (rs.next()) {
				idList.add(new int[]{rs.getInt(1), rs.getInt(2)});
			}
			rs.close();
			int[][] idArray = new int[idList.size()][2];
			idList.toArray(idArray);
			f = new GetNeighbors(idArray, kbId, includedProperties, excludedProperties, 
					edgeType, true, true, hops, true);
			//      f = new GetNeighbors(new String[]{inputStr}, kbId, true, includedProperties, 
			//          excludedProperties, edgeType, true, true, hops, true);
		} else { // default type: InputType.TERM      
			f = new GetNeighbors(new String[]{inputStr}, kbId, includedProperties, excludedProperties, 
					true, edgeType, true, true, hops, true);
		}

		return getRelatedClasses(f, nodeMap, new HashSet<Relationship>(), context);
	}

	private static OntGraph getRelatedClasses(OntoquestFunction<ResourceSet> f,
			HashMap<String, int[]> nodeMap, Set<Relationship> edgeSet, Context context)
					throws OntoquestException {
		ResourceSet rs = f.execute(context, getVarList8());
		while (rs.next()) {
			// int[] id1 = new int[] { rs.getInt(1), rs.getInt(2) };
			// String idStr = ClassNode.generateId(id1[0], id1[1]);
			// if (!nodeMap.containsKey(idStr)) {
			// nodeMap.put(idStr, id1);
			// }
			//
			// int[] id2 = new int[] { rs.getInt(4), rs.getInt(5) };
			// String idStr2 = ClassNode.generateId(id2[0], id2[1]);
			// if (!nodeMap.containsKey(idStr2)) {
			// nodeMap.put(idStr2, id2);
			// }

			Relationship e = new Relationship(rs.getInt(1), rs.getInt(2),
					rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getString(6),
					rs.getInt(7), rs.getString(8), context);
			if (!edgeSet.contains(e)) {
				edgeSet.add(e);
			}
		}
		rs.close();

		// int[][] idArray = new int[nodeMap.size()][2];
		// nodeMap.values().toArray(idArray);

		// Set<ClassNode> nodes = ClassNode.get(idArray, context);
		return new OntGraph(edgeSet);
	}

	//  private Set<ClassNode> nodes;
	private Set<Relationship> edges;


	public OntGraph(Set<Relationship> edges) {
		this.edges = edges;
	}

	//  /**
	//   * @return the nodes
	//   */
	//  public Set<ClassNode> getNodes() {
	//    return nodes;
	//  }
	//
	/**
	 * @return the edges
	 */
	public Set<Relationship> getEdges() {
		return edges;
	}

	@Override
	public Element toXml(Document doc) {
		Element e = doc.createElement("ontGraph");

		//    Element nodesElem = doc.createElement("classes");
		//    e.appendChild(nodesElem);
		//    for (ClassNode c : getNodes()) {
		//      nodesElem.appendChild(c.toXml(doc));
		//    }

		Element edgesElem = doc.createElement("relationships");
		e.appendChild(edgesElem);
		for (Relationship r : getEdges()) {
			edgesElem.appendChild(r.toXml(doc));
		}

		return e;
	}
}


