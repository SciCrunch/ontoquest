package edu.sdsc.ontoquest.loader;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

import com.sleepycat.je.Database;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.loader.TmpDataManager.TmpFileType;
import edu.sdsc.ontoquest.loader.struct.IdCounter;
import edu.sdsc.ontoquest.loader.struct.OntNode;
import edu.sdsc.ontoquest.loader.struct.OntNode.NodeType;

public class OntoquestWalkerVisitor extends OWLOntologyWalkerVisitor<Object> {

	private static Log log = LogFactory.getLog(OntoquestWalkerVisitor.class);
	private TmpDataManager tmpDataManager = null;
	private OwlLoader loader = null;

	public OntoquestWalkerVisitor(OWLOntologyWalker walker, OwlLoader loader) {
		super(walker);
		this.tmpDataManager = loader.getTmpDataManager();
		this.loader = loader;
	}

	private OntNode findNode(OWLObject object)
			throws UnsupportedEncodingException, OntoquestException {
		OntNode node = null;
		Database nodeCache = tmpDataManager.getNodeCache();
		if (object instanceof IRI) {
			node = BDBUtil.searchNode(nodeCache, ((IRI) object).toString());
		} else if (object instanceof OWLAnonymousIndividual) {
			node = BDBUtil.searchNode(nodeCache, ((OWLAnonymousIndividual) object)
					.getID().getID());
		} else if (object instanceof OWLLiteral) {
			node = saveLiteral((OWLLiteral) object);
		}

		return node;
	}

	private void saveDomainAxiom(OntNode domain, OntNode property, char type)
			throws OntoquestException {
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Domain);
		// line: propertyid, domainid, rtid, kbid, type
		OwlLoader.printLine(writer, property.getRid(), domain.getRid(), domain
				.getType().getRtid(), loader.getKbid(), type);
	}

	public OntNode saveLiteral(OWLLiteral literal) throws OntoquestException,
	UnsupportedEncodingException {
		IdCounter idCounter = loader.getCounter("sequence.literal");
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Literal);
		String lexicalform = literal.getLiteral().replace('\t', ' ');
		// find datatype
		Database nodeCache = tmpDataManager.getNodeCache();
		OWLDatatype dt = literal.getDatatype();
		OntNode dtNode = (dt == null) ? null : BDBUtil.searchNode(nodeCache, dt
				.getIRI().toString());

		// line: id, lexical_form, lang, datatype_id, kbid, browsertext
		OwlLoader.printLine(writer, String.valueOf(idCounter.increment()),
				lexicalform, (dtNode == null) ? null : String.valueOf(dtNode.getRid()),
						String.valueOf(loader.getKbid()), lexicalform);

		return new OntNode(literal.getLiteral(), NodeType.Literal,
				idCounter.getCurrentValue(), literal.getLiteral());
	}

	private void saveRangeAxiom(OntNode range, OntNode property, char type)
			throws OntoquestException {
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Range);
		// line: propertyid, rangeid, rtid, kbid, type
		OwlLoader.printLine(writer, property.getRid(), range.getRid(), range
				.getType().getRtid(), loader.getKbid(), type);
	}

	private void saveRelationship(OntNode subject, OntNode property,
			OntNode object) throws OntoquestException {
		IdCounter idCounter = loader.getCounter("sequence.relationship");
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Relationship);
		// line: subjectid, subject_rtid, propertyid, objectid, object_rtid, kbid
		OwlLoader.printLine(writer, subject.getRid(), subject.getType().getRtid(),
				property.getRid(), object.getRid(), object.getType().getRtid(),
				loader.getKbid());
	}

	/** Store annotation assertion axiom in relationship table */
	@Override
	public Object visit(OWLAnnotationAssertionAxiom axiom) {
		try {
			// find subject
			OntNode subNode = findNode(axiom.getSubject());
			if (subNode == null)
				throw new Exception(
						"Couldn't find annotation assertion axiom's subject in database: "
								+ axiom.getSubject().toString());

			// find property
			OntNode propNode = findNode(axiom.getProperty());
			if (propNode == null)
				throw new Exception(
						"Couldn't find annotation assertion axiom's property in database: "
								+ axiom.getProperty().toString());

			// find object
			OntNode objNode = findNode(axiom.getValue());
			if (objNode == null)
				throw new Exception(
						"Couldn't find annotation assertion axiom's value in database: "
								+ axiom.getValue().toString());

			saveRelationship(subNode, propNode, objNode);
		} catch (Exception e) {
			log.info("failed to prcess annotation assertion axiom. "
					+ axiom.toString() + " " + e.getMessage());
		}
		// Database
		return null;
	}

	@Override
	public Object visit(OWLAnnotationPropertyDomainAxiom axiom) {
		visit(axiom.getDomain());
		try {
			// find subject
			OntNode domainNode = findNode(axiom.getDomain());
			if (domainNode == null)
				throw new Exception(
						"Couldn't find annotation property domain axiom's domain in database: "
								+ axiom.getDomain().toString());

			// find property
			OntNode propNode = findNode(axiom.getProperty());
			if (propNode == null)
				throw new Exception(
						"Couldn't find annotation assertion axiom's property in database: "
								+ axiom.getProperty().toString());

			saveDomainAxiom(domainNode, propNode, 'a');
		} catch (Exception e) {
			log.info("failed to prcess annotation assertion axiom. "
					+ axiom.toString() + " " + e.getMessage());
		}

		return null;
	}

	@Override
	public Object visit(OWLAnnotationPropertyRangeAxiom axiom) {
		visit(axiom.getRange());
		try {
			// find subject
			OntNode rangeNode = findNode(axiom.getRange());
			if (rangeNode == null)
				throw new Exception(
						"Couldn't find annotation property range axiom's range in database: "
								+ axiom.getRange().toString());

			// find property
			OntNode propNode = findNode(axiom.getProperty());
			if (propNode == null)
				throw new Exception(
						"Couldn't find annotation assertion axiom's property in database: "
								+ axiom.getProperty().toString());

			saveRangeAxiom(rangeNode, propNode, 'a');
		} catch (Exception e) {
			log.info("failed to prcess annotation assertion axiom. "
					+ axiom.toString() + " " + e.getMessage());
		}
		return null;
	}
}
