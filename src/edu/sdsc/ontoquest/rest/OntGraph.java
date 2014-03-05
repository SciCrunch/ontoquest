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
import edu.sdsc.ontoquest.db.DbContext;
import edu.sdsc.ontoquest.db.DbResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;
import edu.sdsc.ontoquest.rest.BaseBean.InputType;
import edu.sdsc.ontoquest.rest.BaseBean.NeighborType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * @version $Id: OntGraph.java,v 1.7 2013-10-22 19:35:03 jic002 Exp $
 *
 */
public class OntGraph extends BaseBean {

	private static final String[] defaultExcludedProperties = {"disjointWith", "sameAs"};
  
  private boolean isTruncated;

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

		if (type == NeighborType.SUBCLASSES ) {
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

		return getRelatedClasses(f, context);
	}

	public static OntGraph get(String inputStr, NeighborType type, int kbId,
			Map<String, Object> attributes, InputType inputType, Context context, int lmt, boolean includeDerived ) throws OntoquestException {

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
      
      // if just search for 1 level, use the short cut.
      if ( hops == 1 ) {
        String sql = getSQLStatement(inputStr, kbId, edgeType, includeDerived);
        return new OntGraph(sql, context, lmt);
      }
			ResourceSet rs = getBasicFunctions().searchAllIDsByName(inputStr, new int[]{kbId}, true, context, getVarList3());
			LinkedList<int[]> idList = new LinkedList<int[]>();
			while (rs.next()) {
				idList.add(new int[]{rs.getInt(1), rs.getInt(2)});
			}
			rs.close();
			int[][] idArray = new int[idList.size()][2];
			idList.toArray(idArray);
			f = new GetNeighbors(idArray, kbId, includedProperties, excludedProperties, 
					edgeType, true, true, hops, true, lmt);
			//      f = new GetNeighbors(new String[]{inputStr}, kbId, true, includedProperties, 
			//          excludedProperties, edgeType, true, true, hops, true);
		} else { // default type: InputType.TERM      
			f = new GetNeighbors(new String[]{inputStr}, kbId, includedProperties, excludedProperties, 
					true, edgeType, true, true, hops, true, lmt);
		}

		return getRelatedClasses(f, context);
	}
  /**
   *
   * @param inputStr  termID for the property to search for
   * @param kbId     KnowledgeBase id to search in
   * @param inputType  Specify if it is a termID, OID or name
   * @param context   
   * @return
   * @throws OntoquestException
   */

  public static OntGraph getAllEdges(String inputStr, int kbId, InputType inputType, Context context ) throws OntoquestException {

    HashSet<Relationship> result = new HashSet<Relationship>();
    
    Connection conn = null;
    ResultSet rs = null;
    
    String sql ;
    if ( inputType == InputType.ID) {
       sql = "select e.rid1, e.rtid1, n1.label, e.rid2, e.rtid2, n2.label, pid, p.name from graph_edges_all e, property p, graph_nodes_all n1, graph_nodes_all n2 " + 
           " where e.kbid =" + kbId + " and p.id = e.pid and p.name = '" +
                 inputStr + "' and n1.rid = rid1 and n1.rtid = rtid1 " + 
           " and n2.rid = rid2 and n2.rtid = rtid2";
    } else 
    {
      throw new OntoquestException ("Edge-relation search on term is not implemented yet.");
    }
    
    try {
      Utility.checkBlank(sql, OntoquestException.Type.EXECUTOR,
          "Invalid statement: " + sql);
      // System.out.println(sql);
      conn = DbUtility.getDBConnection(context);
      Statement stmt = conn.createStatement();
      rs = stmt.executeQuery(sql);
      while ( rs.next() ) 
      {
        String label1 = rs.getString(3);
        if ( label1 == null)
          throw new OntoquestException("label for " + rs.getInt(1) + "-" + rs.getInt(2) + "not found in graph_nodes_all for " + kbId) ;
        String label2 = rs.getString(6);
        if ( label2 == null)
          throw new OntoquestException("label for " + rs.getInt(4) + "-" + rs.getInt(5) + "not found in graph_nodes_all for " + kbId) ;
        Relationship e = new Relationship(rs.getInt(1), rs.getInt(2),
            rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getString(6),
            rs.getInt(7), rs.getString(8), context);
        result.add(e); 
      }
      rs.close();
      stmt.close();
      DbUtility.releaseDbConnection(conn, context);
      
      return new OntGraph(result, false);
    } catch (Exception e) {
      try {
        rs.close();
      } catch (Exception e2) {
      }
      DbUtility.releaseDbConnection(conn, context);
      if (!(e instanceof OntoquestException)) {
        e.printStackTrace();
        throw new OntoquestException(OntoquestException.Type.BACKEND,
            "Error in getAllEdges function", e);
      } else {
        throw (OntoquestException) e; // throw ontoquest exception up
      }
    }

  }


	private static OntGraph getRelatedClasses(OntoquestFunction<ResourceSet> f, Context context)
					throws OntoquestException {
    
	  Set<Relationship> edgeSet = new HashSet<Relationship>();
    
		ResourceSet rs = f.execute(context, getVarList8());
    int counter = 0;
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
			String label1 = rs.getString(3);
			if ( label1 == null)
			  throw new OntoquestException("label for " + rs.getInt(1) + "-" + rs.getInt(2) + " not found in graph_nodes_all") ;
			String label2 = rs.getString(6);
			if ( label2 == null)
			  throw new OntoquestException("label for " + rs.getInt(4) + "-" + rs.getInt(5) + " not found in graph_nodes_all") ;

			Relationship e = new Relationship(rs.getInt(1), rs.getInt(2),
					rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getString(6),
					rs.getInt(7), rs.getString(8), context);
			if (!edgeSet.contains(e)) {
				edgeSet.add(e);
			}
      counter ++;
		}
		rs.close();

		// int[][] idArray = new int[nodeMap.size()][2];
		// nodeMap.values().toArray(idArray);

		// Set<ClassNode> nodes = ClassNode.get(idArray, context);
    if ( f instanceof GetNeighbors)  
    {
      if (((GetNeighbors)f).getResultLimit() >0 && ((GetNeighbors)f).getResultLimit() < counter) 
      {
        return new OntGraph(edgeSet, true);
      }
    }
		return new OntGraph(edgeSet, false);
	}

	//  private Set<ClassNode> nodes;
	private Collection<Relationship> edges;

  public int size () { return edges.size(); }
  
  public boolean isTruncated () {return isTruncated;}

	public OntGraph(Collection<Relationship> edges, boolean isTruncated) {
		this.edges = edges;
    this.isTruncated = isTruncated;
	}

  /*
   * The sql statement has to return 11 columens that matches the constuctor 
   * of Relationship
   */
  private OntGraph(String sql, Context context, int lmt) throws OntoquestException
  {
    this.edges = new TreeSet<Relationship>();
    

    List<Variable> varList = new ArrayList<Variable>(11);
    for ( int i = 0 ; i < 11 ; i++ ) 
       varList.add(new Variable(1));
    
    int counter = 0;
    ResourceSet rs = DbUtility.executeSQLQuery(sql, context, varList, null, 
        "Error occured when getting graph info from sql " + sql, lmt);
    while (rs.next()) {
      Relationship e = new Relationship(
        rs.getInt(1), rs.getInt(2),rs.getString(3), rs.getInt(4), rs.getInt(5),
        rs.getString(6), rs.getInt(7), rs.getString(8),rs.getString(9),
        rs.getString(10),rs.getString(11));
      if (!edges.contains(e))
         edges.add(e);
      counter ++;
    }
    rs.close();

    if ( lmt > 0 && lmt < counter)
      this.isTruncated = true;
    else 
      this.isTruncated = false;

  }
  
 static private String getSQLStatement(String inputStr, int kbId, int edgeType, boolean includeDerivedEdges) 
  {

    String edgeTableName = "graph_edges_raw";
    if (includeDerivedEdges ) 
    {
      edgeTableName = "graph_edges";
    }
    
    String lterm = inputStr.toLowerCase();
    String getRidsubQuery = "select distinct rid, rtid from graph_nodes where rtid = 1 and (lower(name) = '"
         + lterm + "' or lower(label) = '" + lterm + "') and kbid = "+ kbId;
    String getRidIncludeSynonymSubQuery = 
             "select distinct r.subjectid as rid, subject_rtid as rtid " + 
             "from graph_nodes n1, relationship r, property p, synonym_property_names sp " + 
             "where (lower(n1.name) = '"+ lterm + "' or lower(n1.label) = '" + lterm + "')" + 
             " and r.subject_rtid = 1 and n1.rid = r.objectid and n1.rtid = r.object_rtid and p.id = r.propertyid \n" + 
             " and p.name = sp.property_name and r.kbid = " + kbId + 
              " union "+ getRidsubQuery;
    if (edgeType == GetNeighbors.EDGE_OUTGOING) {
      return "select n0.rid, n0.rtid, gn.label, e.rid2, e.rtid2, n2.label, e.pid , gp.label, gn.name, n2.name, gp.name " +
         "from " + edgeTableName + " e, (" + getRidIncludeSynonymSubQuery + ") n0, " +
         "graph_nodes n2, graph_nodes gp, graph_nodes gn " + 
         "where n0.rid = e.rid1 and e.rtid1 = 1 and n2.rid = e.rid2 and " +
         "n2.rtid = 1 and e.rtid2 = 1 and gp.rid = e.pid and gp.rtid = 15 and gn.rid = n0.rid and gn.rtid = n0.rtid ";
    } else if ( edgeType == GetNeighbors.EDGE_INCOMING) 
    {
      return "select n2.rid, n2.rtid, n2.label, e.rid2, e.rtid2, gn.label, e.pid , gp.label, n2.name, gn.name, gp.name " +
         "from " + edgeTableName + " e, ("+ getRidIncludeSynonymSubQuery + ") n0, " +
         "graph_nodes n2, graph_nodes gp, graph_nodes gn " + 
         "where n0.rid = e.rid2 and e.rtid1 = 1 and n2.rid = e.rid1 and " +
         "n2.rtid = 1 and e.rtid2 = 1 and gp.rid = e.pid and gp.rtid = 15 and gn.rid = n0.rid and gn.rtid = n0.rtid ";
  
    } else if ( edgeType == GetNeighbors.EDGE_BOTH ) {
      return 
          "(" +
          "select n0.rid, n0.rtid, gn.label, e.rid2, e.rtid2, n2.label, e.pid , gp.label, gn.name, n2.name, gp.name " +
         "from " + edgeTableName + " e, ("+ getRidIncludeSynonymSubQuery + ") n0, " +
         "graph_nodes n2, graph_nodes gp, graph_nodes gn " + 
         "where n0.rid = e.rid1 and e.rtid1 = 1 and n2.rid = e.rid2 and " +
         "n2.rtid = 1 and e.rtid2 = 1 and gp.rid = e.pid and gp.rtid = 15 and gn.rid = n0.rid and gn.rtid = n0.rtid " +
         " ) union ("  +  
         "select n2.rid, n2.rtid, n2.label, e.rid2, e.rtid2, gn.label, e.pid , gp.label, n2.name, gn.name, gp.name " +
         "from " + edgeTableName + " e, ("+ getRidIncludeSynonymSubQuery + ") n0, " +
         "graph_nodes n2, graph_nodes gp, graph_nodes gn " + 
         "where n0.rid = e.rid2 and e.rtid1 = 1 and n2.rid = e.rid1 and " +
         "n2.rtid = 1 and e.rtid2 = 1 and gp.rid = e.pid and gp.rtid = 15 and gn.rid = n0.rid and gn.rtid = n0.rtid" +
         ")"  ;
    }
    
    return null;
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
	public Collection<Relationship> getEdges() {
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
      Element p = r.toXml (doc);
      if (p != null)
			  edgesElem.appendChild(p);
      else 
        System.out.println ("null value for element found");
		}

		return e;
	}
}


