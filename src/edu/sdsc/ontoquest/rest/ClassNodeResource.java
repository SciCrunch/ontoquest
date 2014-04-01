package edu.sdsc.ontoquest.rest;

import java.util.HashMap;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;
import edu.sdsc.ontoquest.rest.BaseBean.InputType;
import edu.sdsc.ontoquest.rest.BaseBean.NeighborType;
import edu.sdsc.ontoquest.rest.BaseBean.SiblingsType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @version $Id: ClassNodeResource.java,v 1.3 2013-06-21 22:28:27 jic002 Exp $
 *
 */
public class ClassNodeResource extends BaseResource {

  public static final int DefaultResultLimit = 100;

  public static final int DefaultMaxEditDistance = 50;
	
  /** 
   * key of the map is a composite id in a string fromat generated from 
   * funnction generateId(rid, rtid). Example is '1233444-1'
   */
  private Map<String, ClassNode> classNodes = null;

	@Override
	protected void doInit() throws ResourceException {
		try {
			OntoquestApplication application = (OntoquestApplication)getApplication();
			int kbId = application.getKbId();  // default kb id
			Form form = getRequest().getResourceRef().getQueryAsForm();
			for (String key : form.getValuesMap().keySet()) {
				getRequest().getAttributes().put(key, form.getFirstValue(key));
			}
			String kbName = form.getFirstValue("ontology");
			if (kbName != null && kbName.length() > 0) {
				BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
				kbId = basicFunctions.getKnowledgeBaseID(kbName, getOntoquestContext());
			}

			boolean superclassFlag = false;
			String includeSuper = form.getFirstValue("get_super");
			if (includeSuper != null && "true".equalsIgnoreCase(includeSuper))
				superclassFlag = true;

			//      System.out.println("kbName -- " + kbName+", kbId -- " + kbId);

			classNodes = getClassNodes(kbId);

			if (superclassFlag) {
				fetchSuperclasses(classNodes, kbId);
			}
		} catch (Throwable oe) {
			setAppException(oe);
			//      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, oe.getMessage(), oe);
		}
	}

	private void fetchSuperclasses(Map<String, ClassNode> classNodes, int kbid)
			throws OntoquestException {
		if (classNodes == null || classNodes.size() == 0)
			return;

		int[][] nodeIDs = new int[classNodes.size()][2];
		int i = 0;
		for (ClassNode node : classNodes.values()) {
			nodeIDs[i][0] = node.getRid();
			nodeIDs[i][1] = node.getRtid();
			i++;
		}

		// get 1-level superclasses
		getRequest().getAttributes().put("level", 1);

		OntGraph g = OntGraph.get(nodeIDs, NeighborType.SUPERCLASSES, kbid,
				getRequest().getAttributes(), getOntoquestContext());
		for (Relationship edge : g.getEdges()) {
			String nid1 = ClassNode.generateId(edge.getRid1(), edge.getRtid1());
			ClassNode n = classNodes.get(nid1);
			if (n == null) {
				continue; // TODO: throw exception?
			}
      
			n.getSuperclasses().add(
					new SimpleClassNode(edge.getRid2(), edge.getRtid2(),
                   ClassNode.generateExtId(edge.getRid2(),edge.getRtid2(), getOntoquestContext()),
                  edge.getLabel2(), (List<String[]>)null));
		}
	}

	private Map<String, ClassNode> getClassNodes(int kbId)
			throws OntoquestException {
		// check if the request is to get siblings
		String typeVal = (String) getRequest().getAttributes().get("type");
		if (!Utility.isBlank(typeVal)) {
			SiblingsType type = null;
			try {
				type = SiblingsType.valueOf(typeVal.toUpperCase());
			} catch (IllegalArgumentException iae) {
				throw new OntoquestException(OntoquestException.Type.INPUT,
						"Invalid sibling type: " + typeVal);
			}
			InputType inputType = InputType.TERM;

			String inputStr = (String) getRequest().getAttributes().get("oid");
			if (inputStr != null) {
				inputType = InputType.OID;
			} else {
				inputStr = (String) getRequest().getAttributes().get("classId");
				if (inputStr != null) {
					inputType = InputType.NAME;
				} else {
					inputStr = (String) getRequest().getAttributes().get("term");
					inputType = InputType.TERM;
				}
			}

			inputStr = Reference.decode(inputStr);
			return ClassNode.getSiblings(inputStr, type, kbId, getRequest()
					.getAttributes(), inputType, getOntoquestContext());
		}

		String classId = (String) getRequest().getAttributes().get("classId");
		if (classId != null) {
			classId = Reference.decode(classId);
			//return ClassNode.getByName(classId, kbId, getOntoquestContext());
			if (classId.toLowerCase().startsWith("http://")) 
			  getClassesFromURI(kbId, classId);
			else 
        classNodes = getClassesFromTermID(kbId,classId);
      return classNodes;
		}

		String oid = (String) getRequest().getAttributes().get("oid");
		if (oid != null) {
			return ClassNode.get(oid, getOntoquestContext());
		}

		String term = (String)getRequest().getAttributes().get("term");
		if (term != null) {
			term = Reference.decode(term);
      
      if (term.toLowerCase().startsWith("http://")) 
        getClassesFromURI(kbId, term);
		  else 
        classNodes = getClassesFromTerm(kbId, term);
			// return ClassNode.getByLabel(term, kbId, getOntoquestContext());
      return classNodes;
		}

		String query = (String)getRequest().getAttributes().get("query");
		if (!Utility.isBlank(query)) {
			query = Reference.decode(query);
      
      if ( query.toLowerCase().startsWith("http://")) 
      {
        getClassesFromURI(kbId, query);
        return classNodes;
      }
      
			classNodes= search(query, getRequest().getAttributes(), kbId, getOntoquestContext());
      return classNodes;
		}

		throw new OntoquestException("Unknow search command received. Please check the documentation and construct a valid URL");

	}

	@Get("xml")
	public Representation toXml() {
		if (getAppException() != null)
			return toErrorXml();

		try {
			DomRepresentation representation = new DomRepresentation(
					MediaType.TEXT_XML);
			// Generate a DOM document representing the item.
			Document d = representation.getDocument();
			Element succElem = d.createElement("success");
			d.appendChild(succElem);
			Element dataElem = d.createElement("data");
			succElem.appendChild(dataElem);

			Element classesElem = d.createElement("classes");
			dataElem.appendChild(classesElem);

      if ( classNodes != null) {
			  for (ClassNode classNode : classNodes.values()) { // matched
				  Element classNodeElem = classNode.toXml(d);
				  classesElem.appendChild(classNodeElem);
			  } 
      }
			d.normalizeDocument();

			// Returns the XML representation of this document.
			return representation;
		} catch (Throwable e) {
			e.printStackTrace();
			setAppException(e);
			return toErrorXml();
		}
	}
  
  /**
   * Get a collection of classes that related to the search term. Result is stored in 
   *  this.classNodes.
   * 
   * @param KBid Internal identifier of the knowledge to search on.
   * @param term Search term. It can be any prhase or term identifer.
   */
  private HashMap<String,ClassNode> getClassesFromTerm(int KBid, String term) throws OntoquestException
  {
    HashMap<String,ClassNode> resultMap = new HashMap<String,ClassNode>();

    String lterm= term.toLowerCase();
    String sql = "select n.rid as theRid, n.rtid as theRtid  from graph_nodes n where n.rtid=1 and n.kbid = " +
                 KBid + " and ( lower(name) = '" + lterm + "' or lower(label) = '" + lterm + 
                 "') union select rid1 as theRid, rtid1 as theRtid from graph_nodes n0, graph_nodes n1, graph_edges_raw r1, property p, synonym_property_names sp " +
                 "where r1.rid1 = n0.rid and r1.rtid1 = 1 and n1.rid = r1.rid2 and n1.rtid = r1.rtid2 and p.id = r1.pid and r1.kbid = " +
      KBid + " and ( lower(n1.name) = '" + 
      lterm + "' or lower(n1.label) = '" + lterm + "') and p.name = sp.property_name"; 
    
    List<Variable> varList = new ArrayList<Variable>(2);
    varList.add(new Variable(1));
    varList.add(new Variable(1));

    ResourceSet r = DbUtility.executeSQLQuery(sql, getOntoquestContext(), varList, null, "Error occured when getting rids of the search term " + term,-1);
    while (r.next()) {
      int rid1 = r.getInt(1);
      int rtid1 = r.getInt(2);
      
      resultMap.put(ClassNode.generateId(rid1, rtid1), new ClassNode(KBid, rid1, getOntoquestContext()));
    }
    r.close();
    
    return resultMap;
  }

  /**
   * Get a collection of classes that related to the search term identifier, for example HP_0000316. Result is stored in 
   *  this.classNodes.
   * 
   * @param KBid Id of the Knowledge base.
   * @param termId The term identifier, which is normally the fragment of the class URI.
   */
  private HashMap<String,ClassNode> getClassesFromTermID(int KBid, String termId) throws OntoquestException
  {
    HashMap<String,ClassNode> resultMap = new HashMap<String,ClassNode>();

    String lterm= termId.toLowerCase();
    String sql = "select n.rid as theRid, n.rtid as theRtid  from graph_nodes n where n.rtid=1 and n.kbid = " +
                 KBid + " and lower(name) = '" + lterm + "' union select rid1 as theRid, rtid1 as theRtid from graph_nodes n0, graph_nodes n1, graph_edges_raw r1, property p, synonym_property_names sp " +
                 "where r1.rid1 = n0.rid and r1.rtid1 = 1 and n1.rid = r1.rid2 and n1.rtid = r1.rtid2 and p.id = r1.pid and r1.kbid = " +
      KBid + " and  lower(n1.name) = '" + 
      lterm + "' and p.name = sp.property_name"; 
    
    List<Variable> varList = new ArrayList<Variable>(2);
    varList.add(new Variable(1));
    varList.add(new Variable(1));

    ResourceSet r = DbUtility.executeSQLQuery(sql, getOntoquestContext(), varList, null, "Error occured when getting rids of the search term ID" + termId,-1);
    while (r.next()) {
      int rid1 = r.getInt(1);
      int rtid1 = r.getInt(2);
      
      resultMap.put(ClassNode.generateId(rid1, rtid1), new ClassNode(KBid, rid1, getOntoquestContext()));
    }
    r.close();
    
    return resultMap;
  }



  private void getClassesFromURI(int KBid, String uri) throws OntoquestException
  {
      classNodes = new HashMap<String,ClassNode>();
      ClassNode node = new ClassNode(KBid, uri, getOntoquestContext());
      
      classNodes.put(ClassNode.generateId( node.getRid(), node.getRtid()), node);
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
  private Map<String, ClassNode> search(String term,
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
    ResourceSet rs = ClassNode.getBasicFunctions().searchTerm(term, new int[]{kbId}, 
                                                              context, ClassNode.getVarList1(), 
        resultLimit, negated, maxEditDistance, beginWith);
    TreeSet<String> matchedTerms = new TreeSet<String>();
    while (rs.next()) {
      matchedTerms.add(rs.getString(1).toLowerCase());
    }
    rs.close();

    if (matchedTerms.size() == 0) {
      return new HashMap<String, ClassNode>(); // no match, return empty result.
    }

    // 3. Fetch corresponding ClassNode objects for the matching terms.
    TreeMap <String, ClassNode> result = new TreeMap <String,ClassNode> ();
    int counter = 0;
    for (String termStr : matchedTerms) 
    {
      Map<String, ClassNode> nodemap = getClassesFromTerm(kbId, termStr);
      for (Map.Entry<String, ClassNode> r : nodemap.entrySet())
      {
        if ( !result.containsKey(r.getKey())) 
        {
          result.put(r.getKey(), r.getValue());
          counter++;
          if ( resultLimit >0 && counter == resultLimit )
             break;
        }
      }
    }
    
    return result;
  }
}
