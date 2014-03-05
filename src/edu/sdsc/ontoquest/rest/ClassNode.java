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
import edu.sdsc.ontoquest.db.DbContext;
import edu.sdsc.ontoquest.db.DbResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;
import edu.sdsc.ontoquest.db.functions.GetOntologyURL;
import edu.sdsc.ontoquest.query.Utility;

import edu.sdsc.ontoquest.query.Variable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.TreeMap;

import org.semanticweb.owlapi.model.IRI;


/**
 * @version $Id: ClassNode.java,v 1.6 2013-10-29 23:52:45 jic002 Exp $
 *
 */
public class ClassNode extends BasicClassNode {

  protected static String interanIdAttrName = "InternalId";
  
  private static TreeMap<Integer,String> classIDCache = new TreeMap <Integer,String>() ;

	public static String generateId(int rid, int rtid) {
		return rid+"-"+rtid;
	}
  
  public static synchronized String generateExtId ( int rid, int rtid, Context context ) 
     throws OntoquestException
  {
    if (rtid == 1 ) {
      if ( classIDCache == null) 
        classIDCache = new TreeMap <Integer,String>() ;
    
      // try to find the id from cache
      String res = classIDCache.get(rid);
      if (res != null )
         return res;
    }
    
    // otherwise query the database     
    Connection conn = null;
    String id = null;
    ResultSet rs=null;
    
    String sql = "select name from graph_nodes_all where rid=" + rid +
               " and rtid="+rtid;
    try {
      // System.out.println(sql);
      conn = DbUtility.getDBConnection(context);
      Statement stmt = conn.createStatement();
      rs = stmt.executeQuery(sql);
      while (rs.next()) 
      {
        id = rs.getString(1);
      }
      rs.close();
      stmt.close();
      DbUtility.releaseDbConnection(conn, context);
      if (id != null) {
        if (classIDCache.size() < 50000 && rtid == 1)  
          classIDCache.put(rid, id);
        return id;
      } else
        throw new OntoquestException(OntoquestException.Type.BACKEND, 
                 "Can't find id for rid="+rid+" and rtid="+rtid+ "in db."); 
    } catch (Exception e) {
      try {
        rs.close();
      } catch (Exception e2) {
        throw new OntoquestException ("Fail to close result set of DB query.");
      }
      DbUtility.releaseDbConnection(conn, context);
      if (!(e instanceof OntoquestException)) {
        e.printStackTrace();
        throw new OntoquestException(OntoquestException.Type.BACKEND,
            "Error occurs when getting id of rid="+rid+ ",rtid="+rtid, e);
      } else {
        throw (OntoquestException) e; // throw ontoquest exception up
      }
    }
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
				node = new ClassNode(compositeID, null, term, new LinkedList<String>(), new LinkedList<String>(), null);
				nodeIDList.add(new int[]{rid, rtid});
			}

			if (getDefinitionPropertySet().contains(prop)) { // a definition edge
				List<String> comments = node.getComments();
				comments.add(val+" ["+rs.getString(8)+"]");
      } else if (prop.equals(definitionProperty)) {
         node.setDefinition(val);
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
			resultMap.get(compositeID).setURI(urlMap.get(compositeID));
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
  /* The following 4 attributes are removed since we extend this class from BasicClassNode now. 
	private int rid;
	private int rtid;
	private String label; // rdfs:label
	private String name; // class name, e.g. birnlex_802
  */
  
  private String definition;

//	private String url; // ontology url  removed to use uri of the parent class

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
    /* include all properties, no exclude properties. Include synonyms. 
     * only outgoing edges, exclude hidden edges, not just classes, one level deep,
     * don't include subproperties.
     * 
     * The logic of this function is 
     * 1. search this term in the name and lable field of graph_node table (case-insensitive search).
     * 2. finde nodes that has this term as synonyms.
     * 3. get out-going properties of the nodes returned from 1 and 2.
     * */
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
			List<String> synonyms,String definition) throws OntoquestException {
		setId(id);
		setLabel(label);
		setName(name);
		setComments(comments);
		setSynonyms(synonyms);
		otherProperties = new LinkedList<String[]>();
    this.definition = definition;
	}

  /**
   * Construct an instance from database
   * @param rid rid of the class in database
   * @param kbid 
   * @param context context for database connection
   */
  public ClassNode (int kbid, int rid, Context context) throws OntoquestException
  {
    super(rid,1);

    this.comments = new ArrayList<String>(5);
    this.synonyms = new ArrayList<String>(5);
    this.otherProperties = new ArrayList<String[]> (20);
    this.superclasses = new ArrayList<SimpleClassNode> (5);
    
    // first check if this rid is a member in the equivalent class group.
    // set the representive class id. -1 mean rid is not a member. any possitive value
    // means rid is in an equivalent class group and the rep node id is rrid
 /*   int rrid = -1;  
    String sql = "select n.rid from equivalentclassgroup n where n.kbid = " +
                 kbid + " and ridm = " + rid;
    
    List<Variable> varList = new ArrayList<Variable>(1);
    varList.add(new Variable(1));
    
    ResourceSet r = DbUtility.executeSQLQuery(sql, context, varList, null, 
        "Error occured when check if a class node is a member of equivalent class group.",-1);
    while (r.next()) {
      rrid = r.getInt(1);
    }
    r.close();
    
    if (rrid == -1)  // rid is not in an equivalent class group
    {  */
    populateClassNode(kbid, rid, context);  
  }

  private void populateClassNode(int kbid, int rid, Context context) throws OntoquestException 
  {

    // get the node infor first
    String sql = "select label, name, uri from graph_nodes where kbid = " +kbid + 
           " and rid ="+ rid + " and rtid=1";

    List<Variable> varList = new ArrayList<Variable>(3);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    
    ResourceSet r = DbUtility.executeSQLQuery(sql, context, varList, null, 
        "Error occured when getting class node info for node " + rid,-1);
    while (r.next()) {
      setLabel( r.getString(1));
      setName ( r.getString(2));
      setURI(r.getString(3));
    }
    r.close();
    if ( getName() == null ) 
      throw new OntoquestException ("Class id " + rid + " not found in knowledge base " + kbid);
    
    // get properties 
    sql = "select p.label, n2.name, n2.label, n2.rid, n2.rtid, n2.uri " + 
    "from graph_edges_raw e join graph_nodes n2 on n2.rid = e.rid2 and n2.rtid = e.rtid2 " + 
    " join graph_nodes p on (p.rid = e.pid and p.rtid = 15)" + 
    "where e.rid1 = "+rid +" and  e.rtid1 =1 and e.kbid= " + kbid;
    
    varList = new ArrayList<Variable>(6);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));

    ResourceSet rs = DbUtility.executeSQLQuery(sql, context, varList, null, 
        "Error occured when getting properties of class node " + rid,-1);
    while (rs.next()) {
      String prop = rs.getString(1);
      String val = rs.getString(3);
      if (val == null || val.length() == 0 || prop.equals("label"))
        continue;
     // int n2rid = rs.getInt(4);
     // String term = rs.getString(2);

      //populating properties
      if (getDefinitionPropertySet().contains(prop)) { // a definition edge
        comments.add(val+" ["+prop+"]");
      } else if (prop.equals(definitionProperty)) {
         setDefinition(val);
      } else if (getSynonymPropertySet().contains(prop)){ // synonym edge
          synonyms.add(val);
      } else if (rs.getInt(5) == 13) { // other properties, 13 means literal.
        otherProperties.add(new String[]{prop, val});
      }
    }
    rs.close();
    //    }

    // get subclass info
    sql = "select n.rid, n.rtid, n.name, n.label, n.uri from subclassof s join " +
          " graph_nodes n on s.parentid = n.rid where n.rtid=1 and s.kbid = " + kbid + 
          " and s.childid = " + rid;

    varList = new ArrayList<Variable>(5);
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));
    varList.add(new Variable(1));

    rs = DbUtility.executeSQLQuery(sql, context, varList, null,
      "Error occured when getting parent classes of class node " + rid,-1);
    while (rs.next()) {
    superclasses.add(new SimpleClassNode (rs.getInt(1),
                                          rs.getInt(2),
                                          rs.getString(3),  //name
                                          rs.getString(4),  //Label 
                                          rs.getString(5)));  //URI
    }
    rs.close();
  }

  /**
   *  construnct a Class node from its URI
   * @param kbid
   * @param uri  URI of the class.
   * @param context Context to Ontoquest database.
   * @throws OntoquestException
   */
  public ClassNode (int kbid, String uri, Context context) throws OntoquestException
  {
    int rid = -1;
    String sql = "select rid from graph_nodes where kbid = " +kbid + 
           " and uri ='"+ uri + "' and rtid=1";

    List<Variable> varList = new ArrayList<Variable>(1);
    varList.add(new Variable(1));
    
    ResourceSet r = DbUtility.executeSQLQuery(sql, context, varList, null, 
        "Error occured when getting class id for " + uri,-1);
    while (r.next()) {
      rid = r.getInt(1);
    }
    r.close();
    
    setRid (rid); 
    setRtid (1);

    this.comments = new ArrayList<String>(5);
    this.synonyms = new ArrayList<String>(5);
    this.otherProperties = new ArrayList<String[]> (20);
    this.superclasses = new ArrayList<SimpleClassNode> (5);
  
    populateClassNode(kbid, rid, context);  
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

	/*
	 * @return the label
	 */
/*	public String getLabel() {
		return label;
	} */

	/**
	 * @return the properties
	 */
	public List<String[]> getOtherProperties() {
		return otherProperties;
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
		return getURI();
	}
  
  public String getDefinition() 
  {
    return definition;
  }
  
  public void setDefinition(String def) 
  {
    definition = def;
  }

	/**
	 * @param comments the comments to set
	 */
	public void setComments(List<String> comments) {
		this.comments = comments;
	}

	public void setId(String id) throws OntoquestException {
		int[] ontoId = parseId(id);
		setRid( ontoId[0]);
		setRtid(ontoId[1]);
	}

	/**
	 * @param properties the properties to set
	 */
	public void setOtherProperties(List<String[]> otherProperties) {
		this.otherProperties = otherProperties;
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
/*	public void setUrl(String url) {
     setURI(url);
	} */

	@Override
	public Element toXml(Document doc) {
		Element e = doc.createElement("class");

		Element idElem = doc.createElement("id");
		e.appendChild(idElem);
    idElem.setAttribute(interanIdAttrName, getId());
		idElem.appendChild(doc.createTextNode(getName()));

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
    
	  Element definitionElem = doc.createElement("definition");
	  e.appendChild(definitionElem);
	  if (getDefinition() != null)
	    definitionElem.appendChild(doc.createTextNode(getDefinition()));
    

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
