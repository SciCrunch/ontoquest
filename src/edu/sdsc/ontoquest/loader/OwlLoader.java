package edu.sdsc.ontoquest.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.postgresql.copy.CopyManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLHasValueRestriction;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLNaryDataRange;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLPropertyAssertionObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyRange;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.PrefixOWLOntologyFormat;

import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImpl;

import com.sleepycat.je.Database;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbContext;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.loader.TmpDataManager.TmpFileType;
import edu.sdsc.ontoquest.loader.struct.IdCounter;
import edu.sdsc.ontoquest.loader.struct.NamespaceEntity;
import edu.sdsc.ontoquest.loader.struct.OntNode;
import edu.sdsc.ontoquest.loader.struct.OntNode.NodeType;
import edu.sdsc.ontoquest.loader.struct.OntologyEntity;
import edu.sdsc.ontoquest.query.Utility;

public class OwlLoader {
	private static class Options {
		@Option(name = "-c", usage = "configuration file (default: config/ontoquest.xml)")
		public String configFilePath;

		@Option(name = "-d", usage = "ontology file", required = true)
		public String owlFilePath;
	}

	private static final String DEFAULT_CONFIG_FILE = "config/ontoquest.xml";
	private static Log log = LogFactory.getLog(OwlLoader.class);

	public static final Pattern UNWANTED_CHAR = Pattern
			.compile("[\\p{Space}||[^\\p{ASCII}]]+");

	public static boolean bulkLoad(String tblColName, File file,
			String delimiter, Connection con) throws SQLException, IOException {
		String sql = "COPY " + tblColName
				+ " from STDIN with delimiter as E'\t' NULL as '[NULL]'";
		// System.out.println(sql);
		CopyManager cm = ((org.postgresql.PGConnection)con).getCopyAPI();
		InputStream inStream = new FileInputStream(file);
		cm.copyIn(sql, inStream);
		inStream.close();
		return true;
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		CmdLineParser parser = new CmdLineParser(options);
		parser.parseArgument(args);

		String configFilePath = (options.configFilePath != null) ? options.configFilePath
				: DEFAULT_CONFIG_FILE;

		OwlLoader loader = new OwlLoader(configFilePath, options.owlFilePath);
		loader.run();

		System.exit(0);
	}

	public static void printLine(PrintWriter writer, Object... values) {
		int lastIdx = values.length - 1;
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null)
				writer.write("[NULL]");
			else if (values[i] instanceof Integer)
				writer.print(((Integer) values[i]).intValue());
			else if (values[i] instanceof Float)
				writer.print(((Float) values[i]).floatValue());
			else if (values[i] instanceof Double)
				writer.print(((Double) values[i]).doubleValue());
			else if (values[i] instanceof Long)
				writer.print(((Long) values[i]).longValue());
			else if (values[i] instanceof Short)
				writer.print(((Short) values[i]).shortValue());
			else if (values[i] instanceof Byte)
				writer.print(((Byte) values[i]).byteValue());
			else if (values[i] instanceof Character)
				writer.print(((Character) values[i]).charValue());
			else
				writer.print(values[i]);
			writer.write((i == lastIdx) ? '\n' : '\t');
		}
	}

	private DbConnectionPool conPool = null;

	protected Context context;
	private String ontName = null;
	private String tmpDirName = "tmp";
	private int kbid = -1;
	private File tmpDir;

	private TmpDataManager tmpDataManager = null;

	HashMap<Object, Integer> classCache = new HashMap<Object, Integer>();
	HashMap<String, NamespaceEntity> prefixIDMap = new HashMap<String, NamespaceEntity>();

	HashMap<OWLOntology, OntologyEntity> ontologyMap = new HashMap<OWLOntology, OntologyEntity>();

	/** holds the ID counters for every kind of entity */
	HashMap<String, IdCounter> counterMap = new HashMap<String, IdCounter>();

	private String ontologyFilePath = null;


	private OWLOntologyManager manager = null;

	public OwlLoader(String configFilePath, String ontologyFilePath)
			throws Exception {
		initialize(configFilePath);
		this.ontologyFilePath = ontologyFilePath;
	}

	private void addBuiltInEntities(OWLOntologyManager manager) {
		//TODO
	}

	/**
	 * delete the ontology in the database if it exists.
	 * 
	 * @return true if the ontology is deleted. false if it does not exist, or
	 *         the deletion is failed.
	 * @throws SQLException
	 * @throws ConfigurationException
	 * @throws OntoquestException
	 */
	private boolean cleanKB(Connection con) throws SQLException,
	ConfigurationException, OntoquestException {
		con.commit();
		String sql = AllConfiguration.getConfig().getString(
				"query.getKbIDByName");
		List<Object> inputRow = new ArrayList<Object>(1);
		inputRow.add(ontName);
		boolean result = DbUtility.executeSQLCommandName("cmd.delete_kb",
				context, new String[] { ontName },
				"Unable to delete ontology -- " + ontName);
		// con.commit();
		return result;
	}

	private String composeBrowserText(IRI entityIRI, NamespaceEntity ns) {
		String browserText = entityIRI.toString();
		if (ns != null) {
			browserText = ":".equals(ns.getPrefix()) ? entityIRI.getFragment() : ns
					.getPrefix() + entityIRI.getFragment();
		}
		return browserText;
	}

	private OntNode createProperty(IRI entityIRI, String ontologyId)
			throws UnsupportedEncodingException, OntoquestException {
		OntNode prop = findNode(entityIRI, ontologyId, NodeType.Property);
		if (prop != null)
			return prop;

		IdCounter idCounter = getCounter("sequence.property");
		int entityId = idCounter.increment();
		Database nodeCache = tmpDataManager.getNodeCache();
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Property);

		NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
		String browserText = composeBrowserText(entityIRI, ns);

		// save the entry in BDB.
		OntNode node = new OntNode(entityIRI.toString(), NodeType.Property,
				entityId, browserText);
		BDBUtil.storeNode(nodeCache, entityIRI.toString(), node);
		// save to tmp data file
		// line: id, name, is_object, is_transitive, is_symmetric, is_functional,
		// is_inversefunctional, is_datatype, is_annotation, is_system,
		// nsid, kbid, browsertext, is_reflexive
		printLine(writer, entityId, entityIRI.getFragment(), false, false, false,
				false, false, false, true, true,
				(ns == null ? null : ns.getId()), kbid,
				browserText, false);
		return node;
	}

	private void fillClassMapping(OWLOntologyManager manager)
			throws OntoquestException, UnsupportedEncodingException,
			FileNotFoundException {
		log.info("Start preparing data for primitive classes...");

		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.NamedClass);

		IdCounter clsCounter = getCounter("sequence.primitive_class");
		Database tmpClassDB = tmpDataManager.getNodeCache();
		Set<OWLOntology> ontologies = manager.getOntologies();
		OntNode node = new OntNode(null, null, -1, null);
		for (OWLOntology ontology : ontologies) {
			String ontologyId = getOntologyId(ontology);
			log.debug("Processing classes in ontology " + ontologyId);
			Set<OWLClass> classes = ontology.getClassesInSignature(false);
			for (OWLClass cls : classes) {
				// the class already exists. In some cases, two classes with same URI
				// are treated as different classes by OWLAPI. Semantically, they are
				// the same.
				if (BDBUtil.containsNode(tmpClassDB, cls.getIRI().toString()))
					continue;

				IRI classIRI = cls.getIRI();
				NamespaceEntity ns = prefixIDMap.get(classIRI.getStart());
				String browserText = composeBrowserText(classIRI, ns);
				// String browserText = classIRI.toString();
				// if (ns != null) {
				// browserText = ":".equals(ns.getPrefix()) ? classIRI.getFragment()
				// : ns.getPrefix() + classIRI.getFragment();
				// }
				int classId = clsCounter.increment();
				node.set(classIRI.toString(), NodeType.NamedClass,
						classId, browserText);
				BDBUtil.storeNode(tmpClassDB, classIRI.toString(), node);
				// line: id, name, nsid, kbid, browser_text, is_system
				printLine(writer, String.valueOf(classId), classIRI.getFragment(),
						(ns == null ? null : String.valueOf(ns.getId())),
						String.valueOf(kbid), browserText,
						String.valueOf(cls.isBuiltIn()));
				// System.out.println("Class: " + cls.getIRI());
			}
		}
		log.info("End preparing data for primitive classes...");
	}

	private void fillDatatypeMapping(OWLOntologyManager manager)
			throws OntoquestException, UnsupportedEncodingException,
			FileNotFoundException {
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Datatype);

		IdCounter clsCounter = getCounter("sequence.datatype");
		Database tmpDB = tmpDataManager.getNodeCache();
		OntNode node = new OntNode(null, null, -1, null);
		Set<OWLOntology> ontologies = manager.getOntologies();
		for (OWLOntology ontology : ontologies) {
			String ontologyId = getOntologyId(ontology);
			log.debug("Processing datatypes in ontology " + ontologyId);
			Set<OWLDatatype> entities = ontology.getDatatypesInSignature();
			for (OWLDatatype entity : entities) {
				if (BDBUtil.containsNode(tmpDB, entity.getIRI().toString()))
					continue;

				int entityId = clsCounter.increment();
				IRI entityIRI = entity.getIRI();
				// save the entry in BDB.
				NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
				String browserText = composeBrowserText(entityIRI, ns);
				node.set(entityIRI.toString(), NodeType.Datatype, entityId, browserText);
				BDBUtil.storeNode(tmpDB, entityIRI.toString(), node);
				// String browserText = entityIRI.toString();
				// if (ns != null) {
				// browserText = ":".equals(ns.getPrefix()) ? entityIRI.getFragment()
				// : ns.getPrefix() + entityIRI.getFragment();
				// }

				// line: id, name, hashcode(null), nsid, kbid, browsertext
				printLine(writer, String.valueOf(entityId), entityIRI.getFragment(),
						null, (ns == null ? null : String.valueOf(ns.getId())),
						String.valueOf(kbid), browserText);
			}

		}
	}

	/**
	 * Based on their types, store OWL entity in tmp file for bulk load into db.
	 * In the mean while, save it in BDB for quick lookup when saving axioms and
	 * annotations.
	 * 
	 * @param manager
	 * @throws OntoquestException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private void fillEntityMappings(OWLOntologyManager manager,
			OWLOntology topOntology) throws OntoquestException,
			UnsupportedEncodingException, FileNotFoundException {
		// processing prefix (namespace) mapping
		fillPrefixMapping(manager);

		// processing ontology entity
		fillOntologyMap(manager, topOntology);

		// store class ids
		fillClassMapping(manager);

		// store individual ids
		fillIndividualMapping(manager);

		// store property data
		fillPropertyMapping(manager);

		// store datatypes
		fillDatatypeMapping(manager);

	}

	private void fillIndividualMapping(OWLOntologyManager manager)
			throws OntoquestException, UnsupportedEncodingException,
			FileNotFoundException {
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Individual);

		IdCounter idCounter = getCounter("sequence.individual");
		Database tmpDB = tmpDataManager.getNodeCache();
		OntNode node = new OntNode(null, null, -1, null);
		Set<OWLOntology> ontologies = manager.getOntologies();
		for (OWLOntology ontology : ontologies) {
			String ontologyId = getOntologyId(ontology);
			log.debug("Processing individuals in ontology " + ontologyId);
			Set<OWLNamedIndividual> entities = ontology
					.getIndividualsInSignature(false);
			for (OWLNamedIndividual entity : entities) {
				if (BDBUtil.containsNode(tmpDB, entity.getIRI().toString()))
					continue;

				int entityId = idCounter.increment();
				IRI entityIRI = entity.getIRI();
				// save the entry in BDB.
				NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
				String browserText = composeBrowserText(entityIRI, ns);

				node.set(entityIRI.toString(), NodeType.Individual, entityId,
						browserText);
				BDBUtil.storeNode(tmpDB, entityIRI.toString(), node);
				// String browserText = entityIRI.toString();
				// if (ns != null) {
				// browserText = ":".equals(ns.getPrefix()) ? entityIRI.getFragment()
				// : ns.getPrefix() + entityIRI.getFragment();
				// }

				// line: id, name, nsid, kbid, browsertext, is_named
				printLine(writer, String.valueOf(entityId), entityIRI.getFragment(),
						(ns == null ? null : String.valueOf(ns.getId())),
						String.valueOf(kbid), browserText, String.valueOf(entity.isNamed()));
			}

			Set<OWLAnonymousIndividual> entities2 = ontology
					.getReferencedAnonymousIndividuals();
			for (OWLAnonymousIndividual entity : entities2) {
				if (BDBUtil.searchData(tmpDB, entity.getID().getID()) > 0)
					continue;

				int entityId = idCounter.increment();

				// save the entry in BDB.
				BDBUtil.storeData(tmpDB, entity.getID().getID(), entityId);
				// System.out.println("Individual: " + entity.getIRI());
				printLine(writer, String.valueOf(entityId), entity.toStringID(), null,
						String.valueOf(kbid), String.valueOf(entity.isNamed()));
			}

		}
	}

	private void fillOntologyMap(OWLOntologyManager manager, OWLOntology defaultOntology) throws OntoquestException {
		Set<OWLOntology> ontologies = manager.getOntologies();
		IdCounter ontologyCounter = getCounter("sequence.ontology_uri");
		for (OWLOntology ontology : ontologies) {
			boolean isDefault = ontology.equals(defaultOntology);
			OWLOntologyFormat format = manager.getOntologyFormat(ontology);
			String prefix = null;
			IRI ontIRI = ontology.getOntologyID().getOntologyIRI();
			if (format.isPrefixOWLOntologyFormat()) {
				PrefixOWLOntologyFormat prefixOntFormat = (PrefixOWLOntologyFormat) format;
				prefix = prefixOntFormat.getDefaultPrefix();
			} else {
				prefix = ontIRI.getStart();
			}
			NamespaceEntity ns = getNamespace(ontology, prefix);
			// if (ns == null)
			// System.out.println(getOntologyId(ontology) + '|' + prefix);
			OntologyEntity o = new OntologyEntity(ontologyCounter.increment(), 
					ontIRI, ns, kbid, isDefault, 
					(ontIRI == null) ? null
							: ontIRI.toString(), manager.getOntologyDocumentIRI(ontology));
			ontologyMap.put(ontology, o);
		}
	}

	private void fillPrefixMapping(OWLOntologyManager manager) throws OntoquestException {
		IdCounter nsCounter = getCounter("sequence.namespace");
		Set<OWLOntology> ontologies = manager.getOntologies();
		for (OWLOntology ontology : ontologies) {
			OWLOntologyFormat format = manager.getOntologyFormat(ontology);
			// String ontologyId = getOntologyId(ontology);
			if (format.isPrefixOWLOntologyFormat()) {
				PrefixOWLOntologyFormat prefixOntFormat = (PrefixOWLOntologyFormat)format;
				Set<String> prefixNames = prefixOntFormat.getPrefixNames();
				for (String prefixName : prefixNames) {
					String uri = prefixOntFormat.getPrefix(prefixName);
					NamespaceEntity ns = new NamespaceEntity(nsCounter.increment(), prefixName, uri, kbid, ontology);
					// String key = ontologyId + "|" + uri;
					prefixIDMap.put(uri, ns);
					log.debug("Namespace -- " + uri + " -- " + ns.getPrefix()
							+ ns.getUri());
				}
			}
		}
	}
	private void fillProperty(OWLEntity entity, Database tmpDB, int entityId,
			OWLOntology ontology, String ontologyId, PrintWriter writer)
					throws UnsupportedEncodingException {
		if (BDBUtil.containsNode(tmpDB, entity.getIRI().toString()))
			return;

		IRI entityIRI = entity.getIRI();
		NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
		String browserText = composeBrowserText(entityIRI, ns);
		// String browserText = entityIRI.toString();
		// if (ns != null) {
		// browserText = ":".equals(ns.getPrefix()) ? entityIRI.getFragment() : ns
		// .getPrefix() + entityIRI.getFragment();
		// }

		// save the entry in BDB.
		OntNode node = new OntNode(entityIRI.toString(), NodeType.Property,
				entityId, browserText);
		BDBUtil.storeNode(tmpDB, entityIRI.toString(), node);
		// save to tmp data file
		// line: id, name, is_object, is_transitive, is_symmetric, is_functional,
		// is_inversefunctional, is_datatype, is_annotation, is_system,
		// nsid, kbid, browsertext, is_reflexive
		printLine(
				writer,
				entityId,
				entityIRI.getFragment(),
				entity.isOWLObjectProperty(),
				entity.isOWLObjectProperty() ? entity.asOWLObjectProperty()
						.isTransitive(ontology) : null,
						entity.isOWLObjectProperty() ? entity.asOWLObjectProperty()
								.isSymmetric(ontology) : null,
								entity.isOWLObjectProperty() ? entity.asOWLObjectProperty()
										.isFunctional(ontology) : null,
										entity.isOWLObjectProperty() ? entity.asOWLObjectProperty()
												.isInverseFunctional(ontology) : null,
												entity.isOWLDataProperty(),
												entity.isOWLAnnotationProperty(), entity.isBuiltIn(),
												(ns == null ? null : ns.getId()), kbid,
												browserText,
												entity.isOWLObjectProperty() ? entity.asOWLObjectProperty()
														.isReflexive(ontology) : null);

	}

	private void fillPropertyMapping(OWLOntologyManager manager)
			throws OntoquestException, UnsupportedEncodingException,
			FileNotFoundException {
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Property);

		IdCounter idCounter = getCounter("sequence.property");
		Database tmpPropertyDB = tmpDataManager.getNodeCache();
		Set<OWLOntology> ontologies = manager.getOntologies();
		for (OWLOntology ontology : ontologies) {
			String ontologyId = getOntologyId(ontology);
			log.debug("Processing annotation properties in ontology " + ontologyId);
			Set<OWLAnnotationProperty> entities = ontology
					.getAnnotationPropertiesInSignature();
			for (OWLAnnotationProperty entity : entities) {
				fillProperty(entity, tmpPropertyDB, idCounter.increment(), ontology,
						ontologyId, writer);
			}

			log.debug("Processing object properties in ontology " + ontologyId);
			Set<OWLObjectProperty> entities2 = ontology
					.getObjectPropertiesInSignature();
			for (OWLObjectProperty entity : entities2) {
				fillProperty(entity, tmpPropertyDB, idCounter.increment(), ontology,
						ontologyId, writer);
			}

			log.debug("Processing data properties in ontology " + ontologyId);
			Set<OWLDataProperty> entities3 = ontology.getDataPropertiesInSignature();
			for (OWLDataProperty entity : entities3) {
				fillProperty(entity, tmpPropertyDB, idCounter.increment(), ontology,
						ontologyId, writer);
			}
		}
	}

	private OntNode findNode(OWLObject object, String ontologyId)
			throws UnsupportedEncodingException, OntoquestException {
		return findNode(object, ontologyId, null);
	}

	@SuppressWarnings("rawtypes")
	private OntNode findNode(OWLObject object, String ontologyId,
			NodeType expectedType)
					throws UnsupportedEncodingException, OntoquestException {
		OntNode node = null;
		Database nodeCache = tmpDataManager.getNodeCache();
		if (object instanceof IRI) {
			node = BDBUtil.searchNode(nodeCache, ((IRI) object).toString());
			if (node == null 
					&& (expectedType == null || expectedType.equals(NodeType.Literal))) { // not an OWL entity, maybe a incompletely defined RDF
				// resource. Make it literal
				OWLLiteral l = new OWLLiteralImpl(manager.getOWLDataFactory(),
						((IRI) object).toString(), "en");
				node = saveLiteral(l);
			}
		} else if (object instanceof OWLAnonymousIndividual) {
			node = BDBUtil.searchNode(nodeCache, ((OWLAnonymousIndividual) object)
					.getID().getID());
		} else if (object instanceof OWLLiteral) {
			node = saveLiteral((OWLLiteral) object);
		} else if (object instanceof OWLEntity) {
			node = BDBUtil.searchNode(nodeCache, ((OWLEntity) object).getIRI()
					.toString());
		} else if (object instanceof OWLQuantifiedRestriction) {
			node = saveQuantifiedRestriction((OWLQuantifiedRestriction) object,
					ontologyId);
		} else if (object instanceof OWLObjectHasSelf) {
			node = saveHasSelfRestriction((OWLObjectHasSelf) object, ontologyId);
		} else if (object instanceof OWLHasValueRestriction) {
			node = saveHasValueRestriction((OWLHasValueRestriction) object,
					ontologyId);
		} else if (object instanceof OWLObjectOneOf) {
			node = saveOneOf((OWLObjectOneOf) object, ontologyId);
		} else if (object instanceof OWLObjectComplementOf) {
			node = saveComplementOf((OWLObjectComplementOf) object, ontologyId);
		} else if (object instanceof OWLNaryBooleanClassExpression) {
			node = saveNaryBooleanClass((OWLNaryBooleanClassExpression) object,
					ontologyId);
		} else if (object instanceof OWLDataOneOf) {
			node = saveOneOf((OWLDataOneOf) object, ontologyId);
		} else if (object instanceof OWLDataComplementOf) {
			node = saveComplementOf((OWLDataComplementOf) object, ontologyId);
		} else if (object instanceof OWLNaryDataRange) {
			node = saveNaryDataRange((OWLNaryDataRange) object, ontologyId);
		} else if (object instanceof OWLDatatypeRestriction) {
			node = saveDatatypeRestriction((OWLDatatypeRestriction) object,
					ontologyId);
		} else {
			throw new OntoquestException("Unsupported OWL object: " + object);
		}
		return node;
	}

	/**
	 * Get the sequence (id counter) that is specified by the seqProp
	 * 
	 * @param seqProp
	 * @return
	 * @throws OntoquestException
	 */
	protected IdCounter getCounter(String seqProp) throws OntoquestException {
		IdCounter counter = counterMap.get(seqProp);
		Utility.checkNull(counter, OntoquestException.Type.SETTING, "The property "
				+ seqProp + " is not found in the configuration. Please check.");
		return counter;
	}

	protected int getKbid() {
		return kbid;
	}

	private NamespaceEntity getNamespace(OWLOntology ontology, String prefix) {
		return prefixIDMap.get(prefix);
	}

	// private void printSignature(OWLOntology ontology) {
	// Set<OWLEntity> entities = ontology.getSignature(true);
	// for (OWLEntity e : entities) {
	// System.out.println(e.getEntityType() + " " + e.getIRI() + " "
	// + e.isBuiltIn() + " " + e.isBottomEntity() + " " + e.isTopEntity());
	// }
	// }

	private String getOntologyId(OWLOntology ontology) {
		return ontology.getOntologyID().toString();
	}

	protected TmpDataManager getTmpDataManager() {
		return tmpDataManager;
	}

	private void initCounterMap() throws ConfigurationException {
		Configuration config = AllConfiguration.getConfig();
		java.util.Iterator<String> keyIt = config.getKeys("sequence");
		while (keyIt.hasNext()) {
			String key = keyIt.next();
			counterMap.put(key, new IdCounter(key, context));
		}
	}

	private void initialize(String configFileName)
			throws Exception {
		// initialize configuration
		AllConfiguration.initialize(configFileName);
		// initialize database connection pool
		Configuration config = AllConfiguration.getConfig();
		String driver = config.getString("database.driver");
		String url = config.getString("database.url");
		String user = config.getString("database.user");
		String password = config.getString("database.password");
		ontName = config.getString("ontology_name");

		// validate ontology name
		boolean isValid = true;
		for (int i = 0; i < ontName.length(); ++i) {
			char c = ontName.charAt(i);
			if (!Character.isJavaIdentifierPart(c) && (c != ' ')) {
				isValid = false;
				break;
			}
		}

		if (!isValid) {
			throw new IllegalArgumentException(
					"Invalid ontology name! The name can contain only letters, digits, "
							+ "currency symbol('$'), \nconnecting punctuation character('_'), space(' '), and combining mark.");
		}

		conPool = new DbConnectionPool(driver, url, user, password);
		context = new DbContext(conPool);
		tmpDataManager = new TmpDataManager(config);
	}

	private void postprocessing(Connection con) throws ConfigurationException,
	OntoquestException, SQLException {
		// increment oneof_seq to correct position;
		String seqSetvalCommand = "select setval(':seqName', :val)";
		Iterator<String> seqKeys = AllConfiguration.getConfig().getKeys("sequence");
		//		String[] sequences = { "datatype_restriction", "intersectionclass",
		//				"oneof",
		//		"unionclass" };
		Statement stmt = con.createStatement();
		while (seqKeys.hasNext()) {
			String seqProp = seqKeys.next();
			// for (String seq : sequences) {
			// String sequenceName = String.format("sequence.%s", seq);
			IdCounter counter = getCounter(seqProp);
			if (!counter.isUsed())
				continue; // not used
			String seqName = AllConfiguration.getConfig().getString(seqProp);
			String sql = seqSetvalCommand.replace(":seqName", seqName);
			sql = sql.replace(":val", String.valueOf(counter.getCurrentValue()));
			stmt.execute(sql);
			log.debug("Updated sequence " + seqName + "'s value from "
					+ counter.getInitialValue() + " to "
					+ counter.getCurrentValue());
		}
		stmt.close();
	}

	private void processAnnotationAssertionAxioms(OWLOntology ontology)
			throws OntoquestException, UnsupportedEncodingException {
		String ontologyId = getOntologyId(ontology);
		Set<OWLAnnotationAssertionAxiom> axioms = ontology
				.getAxioms(AxiomType.ANNOTATION_ASSERTION);
		for (OWLAnnotationAssertionAxiom axiom : axioms) {
			// find subject
			OntNode subNode = findNode(axiom.getSubject(), ontologyId);
			if (subNode == null)
				throw new OntoquestException(
						"Couldn't find annotation assertion axiom's subject in database: "
								+ axiom.getSubject().toString());

			// find property
			OntNode propNode = findNode(axiom.getProperty(), ontologyId);
			if (propNode == null)
				throw new OntoquestException(
						"Couldn't find annotation assertion axiom's property in database: "
								+ axiom.getProperty().toString());

			// find object
			OntNode objNode = findNode(axiom.getValue(), ontologyId);
			if (objNode == null)
				throw new OntoquestException(
						"Couldn't find annotation assertion axiom's value in database: "
								+ axiom.getValue().toString());

			saveRelationship(subNode, propNode, objNode);
		}
	}

	private void processAxioms(OWLOntologyManager manager) throws UnsupportedEncodingException, OntoquestException {
		for (OWLOntology ont : manager.getOntologies()) {
			processClassAssertionAxioms(ont);
			processSubClassOfAxioms(ont);
			processSubPropertyOfAxioms(ont);
			processNaryClassAxioms(ont);
			processPropertyAssertionAxioms(ont);
			processPropertyDomainAxioms(ont);
			processPropertyRangeAxioms(ont);
			processNaryPropertyAxioms(ont);
			processNaryIndividualAxioms(ont);
			processAnnotationAssertionAxioms(ont);
			processDeclarationAxioms(ont);
			processUnimplementedAxioms(ont);
		}
	}

	private void processClassAssertionAxioms(OWLOntology ontology)
			throws UnsupportedEncodingException, OntoquestException {
		String ontologyId = getOntologyId(ontology);
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.TypeOf);

		Set<OWLClassAssertionAxiom> axioms = ontology
				.getAxioms(AxiomType.CLASS_ASSERTION);
		for (OWLClassAssertionAxiom axiom : axioms) {
			OWLClassExpression ce = axiom.getClassExpression();
			OntNode ceNode = findNode(ce, ontologyId);
			if (ceNode == null)
				throw new OntoquestException(
						"Couldn't find the class expression of classAssertionAxiom in database: "
								+ axiom.getClassExpression());

			OntNode iNode = findNode(axiom.getIndividual(), ontologyId);
			if (iNode == null)
				throw new OntoquestException(
						"Couldn't find the individual of classAssertionAxiom in database: "
								+ axiom.getIndividual());

			// line:instanceid, instance_rtid, classid, class_rtid, inferred, kbid
			printLine(writer, iNode.getRid(), iNode.getType().getRtid(),
					ceNode.getRid(), ceNode.getType().getRtid(), false, kbid);
		}

	}

	private void processDeclarationAxioms(OWLOntology ontology)
			throws UnsupportedEncodingException, OntoquestException {
		String ontologyId = getOntologyId(ontology);
		Set<OWLDeclarationAxiom> axioms = ontology.getAxioms(AxiomType.DECLARATION);
		for (OWLDeclarationAxiom axiom : axioms) {
			OWLEntity e = axiom.getEntity();
			OntNode node = findNode(e, ontologyId);
			if (node == null)
				throw new OntoquestException("Unhandled entity: " + e.getIRI());
		}
	}

	private void processNaryClassAxioms(OWLOntology ontology)
			throws UnsupportedEncodingException, OntoquestException {
		String ontologyId = getOntologyId(ontology);
		PrintWriter writer1 = tmpDataManager.getWriter(TmpFileType.DisjointClass);

		Set<OWLDisjointClassesAxiom> axioms = ontology
				.getAxioms(AxiomType.DISJOINT_CLASSES);
		for (OWLDisjointClassesAxiom axiom : axioms) {
			Set<OWLDisjointClassesAxiom> pairs = axiom.asPairwiseAxioms();
			for (OWLDisjointClassesAxiom a : pairs) {
				List<OWLClassExpression> pair = a.getClassExpressionsAsList();
				saveNaryClassAxiomInPairwise(pair, ontologyId, writer1, 'c');
			}
		}

		PrintWriter writer2 = tmpDataManager.getWriter(TmpFileType.EquivalentClass);
		Set<OWLEquivalentClassesAxiom> axioms2 = ontology
				.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		for (OWLEquivalentClassesAxiom axiom : axioms2) {
			Set<OWLEquivalentClassesAxiom> pairs = axiom.asPairwiseAxioms();
			for (OWLEquivalentClassesAxiom a : pairs) {
				List<OWLClassExpression> pair = a.getClassExpressionsAsList();
				saveNaryClassAxiomInPairwise(pair, ontologyId, writer2, 'c');
			}
		}

	}

	private void processNaryIndividualAxioms(OWLOntology ontology)
			throws UnsupportedEncodingException, OntoquestException {
		String ontologyId = getOntologyId(ontology);
		PrintWriter writer1 = tmpDataManager
				.getWriter(TmpFileType.DifferentIndividual);

		Set<OWLDifferentIndividualsAxiom> axioms = ontology
				.getAxioms(AxiomType.DIFFERENT_INDIVIDUALS);
		for (OWLDifferentIndividualsAxiom axiom : axioms) {
			Set<OWLDifferentIndividualsAxiom> pairs = axiom.asPairwiseAxioms();
			for (OWLDifferentIndividualsAxiom a : pairs) {
				List<OWLIndividual> pair = a.getIndividualsAsList();
				saveNaryIndividualAxiomInPairwise(pair, ontologyId, writer1);
			}
		}

		PrintWriter writer2 = tmpDataManager.getWriter(TmpFileType.SameIndividual);
		Set<OWLSameIndividualAxiom> axioms2 = ontology
				.getAxioms(AxiomType.SAME_INDIVIDUAL);
		for (OWLSameIndividualAxiom axiom : axioms2) {
			Set<OWLSameIndividualAxiom> pairs = axiom.asPairwiseAxioms();
			for (OWLSameIndividualAxiom a : pairs) {
				List<OWLIndividual> pair = a.getIndividualsAsList();
				saveNaryIndividualAxiomInPairwise(pair, ontologyId, writer2);
			}
		}

	}

	private void processNaryPropertyAxioms(OWLOntology ontology)
			throws OntoquestException, UnsupportedEncodingException {
		Set<OWLDisjointDataPropertiesAxiom> axioms = ontology
				.getAxioms(AxiomType.DISJOINT_DATA_PROPERTIES);
		for (OWLDisjointDataPropertiesAxiom axiom : axioms) {
			throw new OntoquestException("Not implemented!");
		}

		Set<OWLDisjointObjectPropertiesAxiom> axioms2 = ontology
				.getAxioms(AxiomType.DISJOINT_OBJECT_PROPERTIES);
		for (OWLDisjointObjectPropertiesAxiom axiom : axioms2) {
			throw new OntoquestException("Not implemented!");
		}

		PrintWriter writer2 = tmpDataManager.getWriter(TmpFileType.EquivalentClass);
		Set<OWLEquivalentDataPropertiesAxiom> axioms3 = ontology
				.getAxioms(AxiomType.EQUIVALENT_DATA_PROPERTIES);
		for (OWLEquivalentDataPropertiesAxiom axiom : axioms3) {
			throw new OntoquestException("Not implemented!");
		}

		Set<OWLEquivalentObjectPropertiesAxiom> axioms4 = ontology
				.getAxioms(AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
		for (OWLEquivalentObjectPropertiesAxiom axiom : axioms4) {
			throw new OntoquestException("Not implemented!");
		}

	}

	private void processPropertyAssertionAxioms(OWLOntology ontology)
			throws UnsupportedEncodingException, OntoquestException {
		String ontologyId = getOntologyId(ontology);
		Set<OWLDataPropertyAssertionAxiom> axioms = ontology
				.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION);
		for (OWLDataPropertyAssertionAxiom axiom : axioms) {
			savePropertyAssertionAxiom(axiom.getSubject(), axiom.getProperty(),
					axiom.getObject(), ontologyId);
		}

		Set<OWLObjectPropertyAssertionAxiom> axioms2 = ontology
				.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION);
		for (OWLObjectPropertyAssertionAxiom axiom : axioms2) {
			savePropertyAssertionAxiom(axiom.getSubject(), axiom.getProperty(),
					axiom.getObject(), ontologyId);
		}
	}

	private void processPropertyDomainAxioms(OWLOntology ontology)
			throws OntoquestException, UnsupportedEncodingException {
		String ontologyId = getOntologyId(ontology);
		Set<OWLAnnotationPropertyDomainAxiom> axioms = ontology
				.getAxioms(AxiomType.ANNOTATION_PROPERTY_DOMAIN);
		for (OWLAnnotationPropertyDomainAxiom axiom : axioms) {
			saveDomainAxiom(axiom.getDomain(), axiom.getProperty(), ontologyId, 'a');
		}

		Set<OWLDataPropertyDomainAxiom> axioms2 = ontology
				.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN);
		for (OWLDataPropertyDomainAxiom axiom : axioms2) {
			saveDomainAxiom(axiom.getDomain(), axiom.getProperty(), ontologyId, 'd');
		}

		Set<OWLObjectPropertyDomainAxiom> axioms3 = ontology
				.getAxioms(AxiomType.OBJECT_PROPERTY_DOMAIN);
		for (OWLObjectPropertyDomainAxiom axiom : axioms3) {
			saveDomainAxiom(axiom.getDomain(), axiom.getProperty(), ontologyId, 'o');
		}
	}

	private void processPropertyRangeAxioms(OWLOntology ontology)
			throws OntoquestException, UnsupportedEncodingException {
		String ontologyId = getOntologyId(ontology);
		Set<OWLAnnotationPropertyRangeAxiom> axioms = ontology
				.getAxioms(AxiomType.ANNOTATION_PROPERTY_RANGE);
		for (OWLAnnotationPropertyRangeAxiom axiom : axioms) {
			saveRangeAxiom(axiom.getRange(), axiom.getProperty(), ontologyId, 'a');
		}

		Set<OWLDataPropertyRangeAxiom> daxioms = ontology
				.getAxioms(AxiomType.DATA_PROPERTY_RANGE);
		for (OWLDataPropertyRangeAxiom axiom : daxioms) {
			saveRangeAxiom(axiom.getRange(), axiom.getProperty(), ontologyId, 'd');
		}

		Set<OWLObjectPropertyRangeAxiom> oaxioms = ontology
				.getAxioms(AxiomType.OBJECT_PROPERTY_RANGE);
		for (OWLObjectPropertyRangeAxiom axiom : oaxioms) {
			saveRangeAxiom(axiom.getRange(), axiom.getProperty(), ontologyId, 'o');
		}

	}

	private void processSubClassOfAxioms(OWLOntology ontology)
			throws OntoquestException, UnsupportedEncodingException {
		String ontologyId = getOntologyId(ontology);

		Set<OWLSubClassOfAxiom> axioms = ontology.getAxioms(AxiomType.SUBCLASS_OF);
		for (OWLSubClassOfAxiom axiom : axioms) {
			OntNode subNode = findNode(axiom.getSubClass(), ontologyId);
			if (subNode == null)
				throw new OntoquestException(
						"Couldn't find subClassOf axiom's subClass in database: "
								+ axiom.getSubClass().toString());

			// find property
			OntNode superNode = findNode(axiom.getSuperClass(), ontologyId);
			if (superNode == null)
				throw new OntoquestException(
						"Couldn't find subClassOf axiom's superClass in database: "
								+ axiom.getSuperClass().toString());

			PrintWriter writer = tmpDataManager.getWriter(TmpFileType.SubclassOf);
			// line: childid, child_rtid, parentid, parent_rtid, inferred, kbid
			printLine(writer, subNode.getRid(), subNode.getType().getRtid(),
					superNode.getRid(), superNode.getType().getRtid(), false, kbid);
		}

		// add subclassof Owl:Thing as well
		// TODO
	}

	private void processSubPropertyOfAxioms(OWLOntology ontology) 		throws OntoquestException, UnsupportedEncodingException {
		String ontologyId = getOntologyId(ontology);

		Set<OWLSubAnnotationPropertyOfAxiom> axioms = ontology
				.getAxioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF);
		for (OWLSubAnnotationPropertyOfAxiom axiom : axioms) {
			saveSubPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(),
					ontologyId, 'a');
		}

		Set<OWLSubDataPropertyOfAxiom> axioms2 = ontology
				.getAxioms(AxiomType.SUB_DATA_PROPERTY);
		for (OWLSubDataPropertyOfAxiom axiom : axioms2) {
			saveSubPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(),
					ontologyId, 'd');
		}

		Set<OWLSubObjectPropertyOfAxiom> axioms3 = ontology
				.getAxioms(AxiomType.SUB_OBJECT_PROPERTY);
		for (OWLSubObjectPropertyOfAxiom axiom : axioms3) {
			saveSubPropertyOfAxiom(axiom.getSubProperty(), axiom.getSuperProperty(),
					ontologyId, 'o');
		}
	}

	private void processUnimplementedAxioms(OWLOntology ontology)
			throws OntoquestException {
		if (ontology.getAxioms(AxiomType.DATATYPE_DEFINITION).size() > 0) {
			throw new OntoquestException("Not implemented! "
					+ AxiomType.DATATYPE_DEFINITION);
		}
		if (ontology.getAxioms(AxiomType.DISJOINT_UNION).size() > 0) {
			throw new OntoquestException("Not implemented! "
					+ AxiomType.DISJOINT_UNION);
		}
		if (ontology.getAxioms(AxiomType.HAS_KEY).size() > 0) {
			throw new OntoquestException("Not implemented! " + AxiomType.HAS_KEY);
		}
		if (ontology.getAxioms(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION).size() > 0) {
			throw new OntoquestException("Not implemented! "
					+ AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION);
		}
		if (ontology.getAxioms(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION).size() > 0) {
			throw new OntoquestException("Not implemented! "
					+ AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION);
		}
		if (ontology.getAxioms(AxiomType.SWRL_RULE).size() > 0) {
			throw new OntoquestException("Not implemented! " + AxiomType.SWRL_RULE);
		}

	}

	public void run() throws Exception {
		// print inputs
		log.info("OWL File to load: " + ontologyFilePath);
		log.info("Ontology Name: " + ontName);
		log.info("OntoQuest Database: " + conPool.getDbURL());

		// First, we create an OWLOntologyManager object. The manager will load
		// and
		// save ontologies.
		manager = OWLManager.createOWLOntologyManager();
		// Now, we create the file from which the ontology will be loaded.
		// Here the ontology is stored in a file locally in the ontologies
		// subfolder of the examples folder.
		File inputOntologyFile = new File(ontologyFilePath);
		// We use the OWL API to load the ontology.
		OWLOntology ontology = manager
				.loadOntologyFromOntologyDocument(inputOntologyFile);

		Connection con = conPool.getConnection(1000);
		con.setAutoCommit(false);
		try {
			long time1 = System.currentTimeMillis();
			// The ontoquest schema MUST be loaded first.
			if (!DbUtility.schemaExists(conPool)) {
				throw new Exception(
						"Ontoquest schema is not created. Please create required schema objects first!");
			}

			// create tmp dir if not existing
			tmpDir = new File(tmpDirName);
			if (!tmpDir.exists()) {
				tmpDir.mkdir();
			}

			// clean the knowledge base if it exists in the database
			// cleanKB(con);
			classCache.clear();

			// initialization
			initCounterMap();

			// set kbid
			kbid = getCounter("sequence.kb").increment();

			// printSignature(ontology);
			fillEntityMappings(manager, ontology);

			// process axioms
			processAxioms(manager);

			// close all writers
			tmpDataManager.close();

			storeToBackend(con);

			postprocessing(con);

			// walk the ontology, use visitor to handle the storing process.
			// OWLOntologyWalker walker = new OWLOntologyWalker(
			// Collections.singleton(ontology));
			// OWLOntologyWalkerVisitor<Object> visitor = new OntoquestWalkerVisitor(
			// walker, this);
			//
			// walker.walkStructure(visitor);

			long time2 = System.currentTimeMillis();
			log.info("The ontology " + ontName
					+ " has been saved. Total time (sec): " + (time2 - time1) / 1000);
		} catch (Exception e) {
			try {
				con.rollback();
				cleanKB(con);
			} catch (Exception e2) {
				log.warn("Unable to rollback inserted rows. Please delete manually!");
			}
			throw e;
		} finally {
			tmpDataManager.close();
			con.commit();
			conPool.releaseConnection(con);
		}
	}

	@SuppressWarnings("rawtypes")
	private OntNode saveCardinalityRestriction(OWLCardinalityRestriction r,
			String ontologyId) throws UnsupportedEncodingException,
			OntoquestException {
		String seqName = null;
		NodeType nodeType = null;
		TmpFileType fileType = null;
		if (r instanceof OWLDataExactCardinality
				|| r instanceof OWLObjectExactCardinality) {
			nodeType = NodeType.Cardinality;
			fileType = TmpFileType.CardinialityClass;
		} else if (r instanceof OWLDataMinCardinality
				|| r instanceof OWLObjectMinCardinality) {
			nodeType = NodeType.MinCardinality;
			fileType = TmpFileType.MinCardinalityClass;
		} else if (r instanceof OWLDataMaxCardinality
				|| r instanceof OWLObjectMaxCardinality) {
			nodeType = NodeType.MaxCardinality;
			fileType = TmpFileType.MaxCardinalityClass;
		}

		seqName = String.format("sequence.%s", nodeType.toString().toLowerCase());

		int cardinality = r.getCardinality();
		OntNode propNode = savePropertyExpression(r.getProperty());
		IRI entityIRI = IRI.create(propNode.getName());
		NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
		String propText = composeBrowserText(entityIRI, ns);
		String browserText = String.format("%s %s %d", propText, nodeType,
				cardinality);
		// use the browserText as key to store the class in nodeCache
		// first, check if it is already saved
		Database nodeCache = tmpDataManager.getNodeCache();
		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;
		// the node does not exist, save it in cache.
		IdCounter idCounter = getCounter(seqName);
		node = new OntNode(browserText, nodeType, idCounter.increment(),
				browserText);
		BDBUtil.storeNode(nodeCache, browserText, node);

		// save the row in tmp data file
		PrintWriter writer = tmpDataManager.getWriter(fileType);
		// line: id, propertyid, cardinality, kbid, browsertext, type
		char type = r.isObjectRestriction() ? 'o' : 'd';
		printLine(writer, idCounter.getCurrentValue(), propNode.getRid(),
				cardinality, kbid, browserText, type);

		return node;
	}

	private OntNode saveComplementOf(OntNode ceNode, String ontologyId, char type)
			throws UnsupportedEncodingException, OntoquestException {
		if (ceNode == null)
			throw new OntoquestException(
					"Unknown operand in DataComplementOf data range: ");

		String ceText = ceNode.getBrowserText();

		if (ceText.indexOf(' ') >= 0)
			ceText = String.format("(%s)", ceText);
		String browserText = String.format("complementOf %s", ceText);

		// use the browserText as key to store the class in nodeCache
		// first, check if it is already saved
		Database nodeCache = tmpDataManager.getNodeCache();
		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;
		// the node does not exist, save it in cache.
		IdCounter idCounter = getCounter("sequence.complementclass");
		node = new OntNode(browserText, NodeType.ComplementClass,
				idCounter.increment(), browserText);
		BDBUtil.storeNode(nodeCache, browserText, node);

		// save the row in tmp data file
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.ComplementClass);
		// line: id, classid, rtid, kbid, browsertext, type
		printLine(writer, idCounter.getCurrentValue(), ceNode.getRid(), ceNode
				.getType().getRtid(), kbid, browserText, type);

		return node;
	}

	private OntNode saveComplementOf(OWLDataComplementOf r, String ontologyId)
			throws UnsupportedEncodingException, OntoquestException {

		OWLDataRange c = r.getDataRange();
		OntNode ceNode = findNode(c, ontologyId);
		return saveComplementOf(ceNode, ontologyId, 'd');
	}

	private OntNode saveComplementOf(OWLObjectComplementOf r, String ontologyId)
			throws UnsupportedEncodingException, OntoquestException {

		OWLClassExpression c = r.getOperand();
		OntNode ceNode = findNode(c, ontologyId);
		return saveComplementOf(ceNode, ontologyId, 'o');
	}

	private OntNode saveDatatypeRestriction(OWLDatatypeRestriction r,
			String ontologyId) throws UnsupportedEncodingException,
			OntoquestException {
		Database nodeCache = tmpDataManager.getNodeCache();
		OntNode node = BDBUtil.searchNode(nodeCache, r.toString());
		if (node != null)
			return node;

		IdCounter idCounter = getCounter("sequence.datatype_restriction");
		int id = idCounter.increment();
		node = new OntNode(r.toString(), NodeType.DatatypeRestriction, id,
				r.toString());
		// save to cache
		BDBUtil.storeNode(nodeCache, r.toString(), node);

		// write to tmp file
		PrintWriter writer = tmpDataManager
				.getWriter(TmpFileType.DatatypeRestriction);
		OWLDatatype datatype = r.getDatatype();
		OntNode dtNode = findNode(datatype, ontologyId);
		Set<OWLFacetRestriction> frSet = r.getFacetRestrictions();
		for (OWLFacetRestriction fr : frSet) {
			OWLFacet f = fr.getFacet();
			OWLLiteral l = fr.getFacetValue();
			OntNode pNode = createProperty(f.getIRI(), ontologyId);
			OntNode lNode = findNode(l, ontologyId);
			// line: id, datatypeId, facetPropertyId,facetIRI, literal_id, kbid,
			// browsertext
			printLine(writer, id, dtNode.getRid(), pNode.getRid(), f.getIRI()
					.toString(), lNode.getRid(), kbid, r.toString());
		}
		return node;
	}

	private void saveDomainAxiom(OWLObject domain, OWLObject property,
			String ontologyId, char type) throws OntoquestException,
			UnsupportedEncodingException {
		// find subject
		OntNode domainNode = findNode(domain, ontologyId);
		if (domainNode == null)
			throw new OntoquestException(
					"Couldn't find property domain axiom's domain in database: "
							+ domain.toString());

		// find property
		OntNode propNode = findNode(property, ontologyId);
		if (propNode == null)
			throw new OntoquestException(
					"Couldn't find property domain axiom's property in database: "
							+ property.toString());

		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Domain);
		// line: propertyid, domainid, rtid, kbid, type
		OwlLoader.printLine(writer, propNode.getRid(), domainNode.getRid(),
				domainNode.getType().getRtid(), kbid, type);
	}

	private OntNode saveHasSelfRestriction(OWLObjectHasSelf r, String ontologyId)
			throws UnsupportedEncodingException, OntoquestException {

		OntNode propNode = savePropertyExpression(r.getProperty());
		IRI entityIRI = IRI.create(propNode.getName());
		NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
		String propText = composeBrowserText(entityIRI, ns);

		if (propText.indexOf(' ') >= 0)
			propText = String.format("(%s)", propText);
		String browserText = String.format("has_self %s", propText);

		// use the browserText as key to store the class in nodeCache
		// first, check if it is already saved
		Database nodeCache = tmpDataManager.getNodeCache();
		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;
		// the node does not exist, save it in cache.
		IdCounter idCounter = getCounter("sequence.hasself");
		node = new OntNode(browserText, NodeType.HasSelf, idCounter.increment(),
				browserText);
		BDBUtil.storeNode(nodeCache, browserText, node);

		// save the row in tmp data file
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.HasSelf);
		// line: id, propertyid, kbid, browsertext
		printLine(writer, idCounter.getCurrentValue(), propNode.getRid(), kbid,
				browserText);

		return node;
	}

	private OntNode saveHasValueRestriction(OWLHasValueRestriction r,
			String ontologyId) throws UnsupportedEncodingException,
			OntoquestException {

		OntNode propNode = savePropertyExpression(r.getProperty());
		IRI entityIRI = IRI.create(propNode.getName());
		NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
		String propText = composeBrowserText(entityIRI, ns);

		OWLObject value = r.getValue();
		OntNode valNode = findNode(value, ontologyId);
		if (valNode == null)
			throw new OntoquestException("Unhandled the value in HasValue: " + value);

		String valNodeText = valNode.getBrowserText();
		if (valNodeText.indexOf(' ') >= 0)
			valNodeText = String.format("(%s)", valNodeText);
		String browserText = String.format("%s has %s", propText, valNodeText);

		// use the browserText as key to store the class in nodeCache
		// first, check if it is already saved
		Database nodeCache = tmpDataManager.getNodeCache();
		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;
		// the node does not exist, save it in cache.
		IdCounter idCounter = getCounter("sequence.hasvalue");
		node = new OntNode(browserText, NodeType.HasValue, idCounter.increment(),
				browserText);
		BDBUtil.storeNode(nodeCache, browserText, node);

		// save the row in tmp data file
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.HasValue);
		// line: id, propertyid, valueid, rtid, kbid, browsertext, type
		char type = r.isObjectRestriction() ? 'o' : 'd';
		printLine(writer, idCounter.getCurrentValue(), propNode.getRid(),
				valNode.getRid(), valNode.getType().getRtid(), kbid, browserText, type);

		return node;
	}

	public OntNode saveLiteral(OWLLiteral literal) throws OntoquestException,
	UnsupportedEncodingException {
		IdCounter idCounter = getCounter("sequence.literal");
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Literal);
		String lexicalform = literal.getLiteral().replace('\t', ' ');
		// find datatype
		Database nodeCache = tmpDataManager.getNodeCache();
		OWLDatatype dt = literal.getDatatype();
		OntNode dtNode = (dt == null) ? null : BDBUtil.searchNode(nodeCache, dt
				.getIRI().toString());

		String normalizedLexicalform = Normalizer.normalize(lexicalform, Form.NFD);
		normalizedLexicalform = UNWANTED_CHAR.matcher(normalizedLexicalform)
				.replaceAll(" ");
		if (normalizedLexicalform == null) {
			normalizedLexicalform = "";
		}
		// line: id, lexicalform, langtag, datatype_id, hashcode(null), kbid,
		// browsertext
		OwlLoader.printLine(writer, String.valueOf(idCounter.increment()),
				normalizedLexicalform, literal.getLang(),
				(dtNode == null) ? null : String.valueOf(dtNode.getRid()),
						null, String.valueOf(kbid),
						normalizedLexicalform);

		return new OntNode(literal.getLiteral(), NodeType.Literal,
				idCounter.getCurrentValue(), literal.getLiteral());
	}

	private OntNode saveNaryBooleanClass(OWLNaryBooleanClassExpression e,
			String ontologyId) throws UnsupportedEncodingException,
			OntoquestException {
		NodeType nodeType = (e instanceof OWLObjectIntersectionOf) ? NodeType.IntersectionClass
				: NodeType.UnionClass;
		TmpFileType fileType = TmpFileType.valueOf(nodeType.toString());
		String seqName = String.format("sequence.%s", nodeType.toString()
				.toLowerCase());
		String shortName = (nodeType.equals(NodeType.IntersectionClass)) ? " and "
				: " or ";
		IdCounter idCounter = getCounter(seqName);
		PrintWriter writer = tmpDataManager.getWriter(fileType);
		Database nodeCache = tmpDataManager.getNodeCache();

		Set<OWLClassExpression> operands = e.getOperands();
		List<OntNode> opNodes = new ArrayList<OntNode>(operands.size());
		for (OWLClassExpression i : operands) {
			OntNode n = findNode(i, ontologyId);
			if (n == null)
				throw new OntoquestException(
						"Unknown class expression in NaryBooleanClassExpression: " + i);
			opNodes.add(n);
		}

		// compose the browser text
		StringBuilder sb = new StringBuilder();
		for (OntNode n : opNodes) {
			String btext = n.getBrowserText();
			if (btext.indexOf(' ') >= 0)
				sb.append('(').append(btext).append(')');
			else
				sb.append(btext);
			sb.append(shortName);
		}
		if (sb.length() > shortName.length())
			sb.delete(sb.length() - shortName.length(), sb.length());
		String browserText = sb.toString();

		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;

		int id = idCounter.increment();
		node = new OntNode(browserText, nodeType, id, browserText);

		for (OntNode n : opNodes) {
			// line: id, classid, rtid, kbid, browsertext, type
			printLine(writer, id, n.getRid(), n.getType().getRtid(), kbid,
					browserText, 'o');
		}
		return node;

	}

	private void saveNaryClassAxiomInPairwise(List<OWLClassExpression> pair,
			String ontologyId, PrintWriter writer, char type)
					throws OntoquestException,
					UnsupportedEncodingException {
		if (pair.size() != 2)
			throw new OntoquestException(
					"Expecting class pair with size 2. The actual size = " + pair.size());
		OntNode node1 = findNode(pair.get(0), ontologyId);
		if (node1 == null)
			throw new OntoquestException(
					"Couldn't find the class expression of NaryClassAxiom in database: "
							+ pair.get(0));

		OntNode node2 = findNode(pair.get(1), ontologyId);
		if (node2 == null)
			throw new OntoquestException(
					"Couldn't find the class expression of NaryClassAxiom in database: "
							+ pair.get(1));

		// line: rid1, rtid1, rid2, rtid2, inferred, kbid, type
		printLine(writer, node1.getRid(), node1.getType().getRtid(),
				node2.getRid(), node2.getType().getRtid(), false, kbid, type);
	}

	private OntNode saveNaryDataRange(OWLNaryDataRange e,
			String ontologyId) throws UnsupportedEncodingException,
			OntoquestException {
		NodeType nodeType = (e instanceof OWLObjectIntersectionOf)? NodeType.IntersectionClass : NodeType.UnionClass;
		TmpFileType fileType = TmpFileType.valueOf(nodeType.toString());
		String seqName = String.format("sequence.%s", nodeType.toString()
				.toLowerCase());
		String shortName = (nodeType.equals(NodeType.IntersectionClass)) ? " and "
				: " or ";
		IdCounter idCounter = getCounter(seqName);
		PrintWriter writer = tmpDataManager.getWriter(fileType);
		Database nodeCache = tmpDataManager.getNodeCache();

		Set<OWLDataRange> operands = e.getOperands();
		List<OntNode> opNodes = new ArrayList<OntNode>(operands.size());
		for (OWLDataRange i : operands) {
			OntNode n = findNode(i, ontologyId);
			if (n == null)
				throw new OntoquestException(
						"Unknown class expression in NaryBooleanClassExpression: " + i);
			opNodes.add(n);
		}

		// compose the browser text
		StringBuilder sb = new StringBuilder();
		for (OntNode n : opNodes) {
			String btext = n.getBrowserText();
			if (btext.indexOf(' ') >= 0)
				sb.append('(').append(btext).append(')');
			else
				sb.append(btext);
			sb.append(shortName);
		}
		if (sb.length() > shortName.length())
			sb.delete(sb.length() - shortName.length(), sb.length());
		String browserText = sb.toString();

		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;

		int id = idCounter.increment();
		node = new OntNode(browserText, nodeType, id, browserText);

		for (OntNode n : opNodes) {
			// line: id, classid, rtid, kbid, browsertext, type
			printLine(writer, id, n.getRid(), n.getType().getRtid(), kbid,
					browserText, 'd');
		}
		return node;

	}

	private void saveNaryIndividualAxiomInPairwise(List<OWLIndividual> pair,
			String ontologyId, PrintWriter writer) throws OntoquestException,
			UnsupportedEncodingException {
		if (pair.size() != 2)
			throw new OntoquestException(
					"Expecting individual pair with size 2. The actual size = "
							+ pair.size());
		OntNode node1 = findNode(pair.get(0), ontologyId);
		if (node1 == null)
			throw new OntoquestException(
					"Couldn't find the individual of NaryIndividualAxiom in database: "
							+ pair.get(0));

		OntNode node2 = findNode(pair.get(1), ontologyId);
		if (node2 == null)
			throw new OntoquestException(
					"Couldn't find the individual of NaryIndividualAxiom in database: "
							+ pair.get(1));

		// line: rid1, rid2, kbid
		printLine(writer, node1.getRid(), node2.getRid(), kbid);
	}

	private OntNode saveOneOf(OWLDataOneOf r, String ontologyId) throws UnsupportedEncodingException,
	OntoquestException {
		IdCounter idCounter = getCounter("sequence.oneof");
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.OneOf);
		Database nodeCache = tmpDataManager.getNodeCache();

		Set<OWLLiteral> literals = r.getValues();
		List<OntNode> indNodes = new ArrayList<OntNode>(literals.size());
		for (OWLLiteral l : literals) {
			indNodes.add(saveLiteral(l));
		}

		// compose the browser text
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (OntNode n : indNodes) {
			sb.append(n.getBrowserText()).append(' ');
		}
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append('}');
		String browserText = sb.toString();

		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;

		int id = idCounter.increment();
		node = new OntNode(browserText, NodeType.OneOf, id,browserText);

		for (OntNode n : indNodes) {
			// line: id, valueid, rtid, kbid, browsertext, type
			printLine(writer, id, n.getRid(), n.getType().getRtid(), kbid,
					browserText, 'd');
		}
		return node;
	}

	private OntNode saveOneOf(OWLObjectOneOf r, String ontologyId)
			throws UnsupportedEncodingException, OntoquestException {
		IdCounter idCounter = getCounter("sequence.oneof");
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.OneOf);
		Database nodeCache = tmpDataManager.getNodeCache();

		Set<OWLIndividual> individuals = r.getIndividuals();
		List<OntNode> indNodes = new ArrayList<OntNode>(individuals.size());
		for (OWLIndividual i : individuals) {
			String key = (i instanceof OWLNamedIndividual) ? ((OWLNamedIndividual) i)
					.getIRI().toString() : ((OWLAnonymousIndividual) i).getID().getID();
					indNodes.add(BDBUtil.searchNode(nodeCache, key));
		}

		// compose the browser text
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (OntNode n : indNodes) {
			sb.append(n.getBrowserText()).append(' ');
		}
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append('}');
		String browserText = sb.toString();

		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;

		int id = idCounter.increment();
		node = new OntNode(browserText, NodeType.OneOf, id, browserText);

		for (OntNode n : indNodes) {
			// line: id, valueid, rtid, kbid, browsertext, type
			printLine(writer, id, n.getRid(), n.getType().getRtid(), kbid,
					browserText, 'o');
		}
		return node;
	}

	private void savePropertyAssertionAxiom(OWLIndividual subject,
			OWLPropertyExpression property, OWLPropertyAssertionObject object,
			String ontologyId) throws UnsupportedEncodingException,
			OntoquestException {
		// find subject
		OntNode subNode = findNode(subject, ontologyId);
		if (subNode == null)
			throw new OntoquestException(
					"Couldn't find property assertion axiom's subject in database: "
							+ subNode);

		// find property
		OntNode propNode = findNode(property, ontologyId);
		if (propNode == null)
			throw new OntoquestException(
					"Couldn't find property assertion axiom's property in database: "
							+ property.toString());

		// find object
		OntNode objNode = findNode(object, ontologyId);
		if (objNode == null)
			throw new OntoquestException(
					"Couldn't find property assertion axiom's object in database: "
							+ objNode);

		// save to relationship table
		saveRelationship(subNode, propNode, objNode);
	}

	@SuppressWarnings("rawtypes")
	private OntNode savePropertyExpression(OWLPropertyExpression p)
			throws UnsupportedEncodingException, OntoquestException {
		Database nodeCache = tmpDataManager.getNodeCache();
		if (p instanceof OWLProperty) {
			return BDBUtil.searchNode(nodeCache, ((OWLProperty) p).getIRI()
					.toString());
		} else {
			throw new OntoquestException(
					"Unsupported anonymous property expression: " + p);
		}
	}


	private OntNode saveQuantifiedRestriction(OWLQuantifiedRestriction r,
			String ontologyId) throws UnsupportedEncodingException,
			OntoquestException {
		if (r instanceof OWLCardinalityRestriction)
			return saveCardinalityRestriction((OWLCardinalityRestriction) r,
					ontologyId);

		String seqName = null;
		NodeType nodeType = null;
		TmpFileType fileType = null;
		String shortTypeName = null;
		if (r instanceof OWLDataAllValuesFrom
				|| r instanceof OWLObjectAllValuesFrom) {
			nodeType = NodeType.AllValuesFrom;
			shortTypeName = "all";
		} else if (r instanceof OWLDataSomeValuesFrom
				|| r instanceof OWLObjectSomeValuesFrom) {
			nodeType = NodeType.SomeValuesFrom;
			shortTypeName = "some";
		}
		seqName = String.format("sequence.%s", nodeType.toString().toLowerCase());
		fileType = TmpFileType.valueOf(nodeType.toString());

		OntNode propNode = savePropertyExpression(r.getProperty());
		IRI entityIRI = IRI.create(propNode.getName());
		NamespaceEntity ns = prefixIDMap.get(entityIRI.getStart());
		String propText = composeBrowserText(entityIRI, ns);

		OWLPropertyRange range = r.getFiller();
		OntNode rangeNode = findNode(range, ontologyId);
		if (rangeNode == null)
			throw new OntoquestException("Unhandled OWL property range: " + range);

		String rangeNodeBrowserText = rangeNode.getBrowserText();
		if (rangeNodeBrowserText.indexOf(' ') >= 0)
			rangeNodeBrowserText = String.format("(%s)", rangeNodeBrowserText);
		String browserText = String.format("%s %s %s", propText, shortTypeName,
				rangeNodeBrowserText);
		// String browserText = r.toString();
		// use the browserText as key to store the class in nodeCache
		// first, check if it is already saved
		Database nodeCache = tmpDataManager.getNodeCache();
		OntNode node = BDBUtil.searchNode(nodeCache, browserText);
		if (node != null)
			return node;
		// the node does not exist, save it in cache.
		IdCounter idCounter = getCounter(seqName);
		node = new OntNode(browserText, nodeType, idCounter.increment(),
				browserText);
		BDBUtil.storeNode(nodeCache, browserText, node);

		// save the row in tmp data file
		PrintWriter writer = tmpDataManager.getWriter(fileType);
		// line: id, propertyid, rangeclassid, rtid, kbid, browsertext, type
		char type = r.isObjectRestriction() ? 'o' : 'd';
		printLine(writer, idCounter.getCurrentValue(), propNode.getRid(),
				rangeNode.getRid(), rangeNode.getType().getRtid(), kbid, browserText,
				type);

		return node;
	}

	private void saveRangeAxiom(OWLObject range, OWLObject property,
			String ontologyId, char type) throws OntoquestException,
			UnsupportedEncodingException {
		OntNode rangeNode = findNode(range, ontologyId);
		if (rangeNode == null)
			throw new OntoquestException(
					"Couldn't find property range axiom's range in database: "
							+ range.toString());

		// find property
		OntNode propNode = findNode(property, ontologyId);
		if (propNode == null)
			throw new OntoquestException(
					"Couldn't find annotation assertion axiom's property in database: "
							+ property.toString());

		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Range);
		// line: propertyid, rangeid, rtid, kbid, type
		OwlLoader.printLine(writer, propNode.getRid(), rangeNode.getRid(),
				rangeNode.getType().getRtid(), kbid, type);
	}

	private void saveRelationship(OntNode subject, OntNode property,
			OntNode object) throws OntoquestException {
		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.Relationship);
		// line: subjectid, subject_rtid, propertyid, objectid, object_rtid,
		// inferred, kbid
		OwlLoader.printLine(writer, subject.getRid(), subject.getType().getRtid(),
				property.getRid(), object.getRid(), object.getType().getRtid(), false,
				kbid);
	}

	private void saveSubPropertyOfAxiom(OWLObject subProperty, OWLObject superProperty,
			String ontologyId, char type) throws OntoquestException,
			UnsupportedEncodingException {
		OntNode subNode = findNode(subProperty, ontologyId);
		if (subNode == null)
			throw new OntoquestException(
					"Couldn't find subpropertyof axiom's sub-property in database: "
							+ subProperty.toString());

		// find property
		OntNode superNode = findNode(superProperty, ontologyId);
		if (superNode == null)
			throw new OntoquestException(
					"Couldn't find subpropertyof axiom's super-property in database: "
							+ superProperty.toString());

		PrintWriter writer = tmpDataManager.getWriter(TmpFileType.SubpropertyOf);
		// line: childid, parentid, inferred, kbid, type
		printLine(writer, subNode.getRid(), superNode.getRid(), false, kbid, type);
	}

	private void storeKB(Connection con) throws SQLException {
		// String
		String sql = "insert into kb values (?, ?, default)";
		PreparedStatement stmt = con.prepareStatement(sql);
		stmt.setInt(1, kbid);
		stmt.setString(2, ontName);
		stmt.execute();
		stmt.close();
	}

	private void storeNamespaces(Connection con) throws SQLException {
		String sql = "insert into namespace values(?,?,?,?,?)";
		PreparedStatement stmt = con.prepareStatement(sql);
		for (NamespaceEntity entity : prefixIDMap.values()) {
			stmt.clearParameters();
			stmt.setInt(1, entity.getId());
			stmt.setString(2, entity.getPrefix());
			stmt.setString(3, entity.getUri());
			stmt.setBoolean(4, entity.isInternal());
			stmt.setInt(5, entity.getKbid());
			stmt.execute();
		}
		stmt.close();
	}

	private void storeOntologies(Connection con)
			throws SQLException {
		String sql = "insert into ontologyuri values(?,?,?,?,?,?,?)";
		PreparedStatement stmt = con.prepareStatement(sql);
		for (OntologyEntity entity : ontologyMap.values()) {
			stmt.clearParameters();
			stmt.setInt(1, entity.getId());
			stmt.setString(2, entity.getIri().toString());
			stmt.setBoolean(3, entity.isDefault());
			if (entity.getNamespace() == null)
				stmt.setNull(4, Types.INTEGER);
			else
				stmt.setInt(4, entity.getNamespace().getId());
			stmt.setInt(5, entity.getKbid());
			stmt.setString(6, entity.getBrowserText());
			IRI documentIRI = entity.getDocumentIRI();
			if (documentIRI == null)
				stmt.setNull(7, java.sql.Types.VARCHAR);
			else
				stmt.setString(7, entity.getDocumentIRI().toString());
			stmt.execute();
		}
		stmt.close();
	}

	private void storeOntologyImports(Connection con) throws SQLException {
		String sql = "insert into ontologyimport values(?,?,?,?)";
		PreparedStatement stmt = con.prepareStatement(sql);
		for (OWLOntology ontology : ontologyMap.keySet()) {
			OntologyEntity ontologyEntity = ontologyMap.get(ontology);
			Set<OWLOntology> importedOntologies = ontology.getImports();
			for (OWLOntology importedOntology : importedOntologies) {
				OntologyEntity importedEntity = ontologyMap.get(importedOntology);
				stmt.setInt(1, ontologyEntity.getId());
				stmt.setString(2, importedEntity.getIri().toString());
				stmt.setInt(3, importedEntity.getId());
				stmt.setInt(4, ontologyEntity.getKbid());
				stmt.execute();
			}
		}
		stmt.close();
	}

	private void storeSystemEntities(Connection con) throws SQLException,
	OntoquestException {
		Statement stmt = con.createStatement();
		int rdfNSID = prefixIDMap.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#").getId();
		int owlNSID = prefixIDMap.get("http://www.w3.org/2002/07/owl#").getId();
		int rdfsNSID = prefixIDMap.get("http://www.w3.org/2000/01/rdf-schema#")
				.getId();
		// int xsdNSID =
		// prefixIDMap.get("http://www.w3.org/2001/XMLSchema#").getId();

		IdCounter counter = getCounter("sequence.property");

		// insert system properties
		try {
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'allValuesFrom',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:AllValuesFrom',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'backwardCompatibleWith',null,true,false,false,false,false,false,true,true,true,false,"
					+ owlNSID + "," + kbid + ",'owl:backwardCompatibleWith',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'cardinality',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:cardinality',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'complementOf',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:comlementOf',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'differentFrom',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:differentFrom',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'disjointWith',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:disjointWith',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'distinctMembers',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:distinctMembers',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'equivalentClass',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:equivalentClass',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'equivalentProperty',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:equivalentProperty',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'hasValue',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:hasValue',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'imports',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:imports',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'incompatibleWith',null,true,false,false,false,false,false,true,true,true,false,"
					+ owlNSID + "," + kbid + ",'owl:incompatibleWith',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'intersectionOf',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:intersectionOf',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'inverseOf',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:inverseOf',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'maxCardinality',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:maxCardinality',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'minCardinality',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:minCardinality',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'oneOf',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:oneOf',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'onDatatype',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:onDatatype',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'onProperty',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:onProperty',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'priorVersion',null,true,false,false,false,false,false,true,true,true,false,"
					+ owlNSID + "," + kbid + ",'owl:priorVersion',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'sameAs',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:sameAs',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'someValuesFrom',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:someValuesFrom',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'unionOf',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:unionOf',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'valuesFrom',null,false,false,false,false,false,false,false,false,true,false,"
					+ owlNSID + "," + kbid + ",'owl:valuesFrom',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'versionInfo',null,false,false,false,false,false,true,true,true,true,false,"
					+ owlNSID + "," + kbid + ",'owl:versionInfo',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'first',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfNSID + "," + kbid + ",'rdf:first',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'object',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfNSID + "," + kbid + ",'rdf:object',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'predicate',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfNSID + "," + kbid + ",'rdf:predicate',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'rest',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfNSID + "," + kbid + ",'rdf:rest',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'comment',null,false,false,false,false,false,true,true,true,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:comment',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'domain',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:domain',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'isDefinedBy',null,true,false,false,false,false,false,true,true,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:isDefinedBy',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'label',null,false,false,false,false,false,true,true,true,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:label',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'member',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:member',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'range',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:range',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'seeAlso',null,true,false,false,false,false,false,true,true,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:seeAlso',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'subClassOf',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:subClassOf',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'subPropertyOf',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfsNSID + "," + kbid + ",'rdfs:subPropertyOf',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'subject',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfNSID + "," + kbid + ",'rdf:subject',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'type',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfNSID + "," + kbid + ",'rdf:type',false)");
			stmt.addBatch("insert into property values("
					+ counter.increment()
					+ ",'value',null,false,false,false,false,false,false,false,false,true,false,"
					+ rdfNSID + "," + kbid + ",'rdf:value',false)");
			// stmt.addBatch("insert into property values("
			// + counter.increment()
			// +
			// ",'',null,false,false,false,false,false,false,false,false,true,false,"
			// + rdfNSID + "," + kbid + ",'rdf:value',false)");
			stmt.executeBatch();

			// insert system classes
			// stmt.addBatch("insert into primitiveclass(default,'AllDifferent', null,true,false,false,"
			// + owlNSID + "," + kbid + ",'owl:AllDifferent',false)");
			// stmt.addBatch("insert into primitiveclass(default,'AllValuesFromRestriction', null,true,false,false,"
			// + owlNSID + "," + kbid + ",'owl:AllValuesFromClass',false)");
			stmt.close();
		} catch (BatchUpdateException bue) {
			System.out.println(bue.getMessage());
			SQLException e = bue.getNextException();
			e.printStackTrace();
			throw e;
		}
	}

	private void storeToBackend(Connection con)
			throws SQLException, IOException,
			OntoquestException {
		storeKB(con);
		storeNamespaces(con);
		storeOntologies(con);
		storeOntologyImports(con);

		// the loading order is important!
		TmpFileType[] types = new TmpFileType[] { TmpFileType.NamedClass,
				TmpFileType.Property, TmpFileType.Datatype, TmpFileType.Individual,
				TmpFileType.Literal, TmpFileType.AllValuesFrom,
				TmpFileType.SomeValuesFrom, TmpFileType.CardinialityClass,
				TmpFileType.MaxCardinalityClass, TmpFileType.MinCardinalityClass,
				TmpFileType.UnionClass, TmpFileType.IntersectionClass,
				TmpFileType.HasValue, TmpFileType.OneOf, TmpFileType.HasSelf,
				TmpFileType.DatatypeRestriction, TmpFileType.DisjointClass,
				TmpFileType.EquivalentClass, TmpFileType.ComplementClass,
				TmpFileType.DifferentIndividual, TmpFileType.SameIndividual,
				TmpFileType.Domain, TmpFileType.Range, TmpFileType.Relationship,
				TmpFileType.SubclassOf, TmpFileType.SubpropertyOf, TmpFileType.TypeOf
		};

		for (TmpFileType type : types) {
			bulkLoad(tmpDataManager.getBulkLoadStmt(type),
					tmpDataManager.getTmpFile(type), "\t", con);
			log.debug(type + " has been stored into the database.");
		}

		storeSystemEntities(con);
	}

}
