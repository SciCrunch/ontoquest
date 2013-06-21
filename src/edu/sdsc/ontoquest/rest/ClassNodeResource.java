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
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.rest.BaseBean.InputType;
import edu.sdsc.ontoquest.rest.BaseBean.NeighborType;
import edu.sdsc.ontoquest.rest.BaseBean.SiblingsType;

/**
 * @version $Id: ClassNodeResource.java,v 1.3 2013-06-21 22:28:27 jic002 Exp $
 *
 */
public class ClassNodeResource extends BaseResource {

	HashMap<String, ClassNode> classNodes = null;

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

	private void fetchSuperclasses(HashMap<String, ClassNode> classNodes, int kbid)
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
                  edge.getLabel2(), null));
		}
	}

	private HashMap<String, ClassNode> getClassNodes(int kbId)
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
			return ClassNode.getByName(classId, kbId, getOntoquestContext());
		}

		String oid = (String) getRequest().getAttributes().get("oid");
		if (oid != null) {
			return ClassNode.get(oid, getOntoquestContext());
		}

		String term = (String)getRequest().getAttributes().get("term");
		if (term != null) {
			term = Reference.decode(term);
			return ClassNode.getByLabel(term, kbId, getOntoquestContext());
		}

		String query = (String)getRequest().getAttributes().get("query");
		if (!Utility.isBlank(query)) {
			query = Reference.decode(query);
			return ClassNode.search(query, getRequest().getAttributes(), kbId, getOntoquestContext());
		}

		return null;

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

			for (ClassNode classNode : classNodes.values()) { // matched
				Element classNodeElem = classNode.toXml(d);
				classesElem.appendChild(classNodeElem);
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
}
