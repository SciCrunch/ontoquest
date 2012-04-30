package edu.sdsc.ontoquest.loader;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sleepycat.je.Database;
import com.sleepycat.je.Environment;

import edu.sdsc.ontoquest.AllConfiguration;

/**
 * Manages temporary data
 * @author xqian
 *
 */
public class TmpDataManager {
	// public enum DBName {
	// Datatype, Individual, NamedClass, Property, Literal, SomeValuesFrom,
	// AllValuesFrom
	// };

	public enum TmpFileType {
		AllDifferentIndividual, AllValuesFrom, CardinialityClass, ComplementClass, Datarange, Datatype, DatatypeRestriction, DifferentIndividual, DisjointClass, Domain, EquivalentClass, EquivalentProperty, HasSelf, HasValue, Individual, IntersectionClass, InversePropertyOf, Literal, MaxCardinalityClass, MinCardinalityClass, NamedClass, OneOf, Property, Range, Relationship, SameIndividual, SomeValuesFrom, SubpropertyOf, SubclassOf, TypeOf, UnionClass
	}

	private Environment dbEnv = null;

	private Configuration config = null;
	// private HashMap<DBName, Database> databaseMap = new HashMap<DBName,
	// Database>();
	private HashMap<TmpFileType, PrintWriter> tmpFileWriterMap = new HashMap<TmpFileType, PrintWriter>();
	private Database nodeCache = null;
	private String kbName = null;
	private File tmpDir = null;
	private HashMap<TmpFileType, String> bulkLoadStmt = new HashMap<TmpFileType, String>();

	private static Log logger = LogFactory.getLog(TmpDataManager.class);

	public TmpDataManager(Configuration config) throws Exception {
		this.config = config;
		initDatabase();
		initTmpFileWriters();
		initBulkLoadStatements();
	}

	public void close() {
		try {
			nodeCache.close();
		} catch (Exception e) {
		}
		dbEnv.close();

		for (PrintWriter out : tmpFileWriterMap.values()) {
			try {
				out.close();
			} catch (Exception e) {
			}
		}
	}

	private Database createTempDB(Environment dbEnv, String name) {
		return BDBUtil.openDB(dbEnv, name, false, true, false, false, true, false);

	}

	public String getBulkLoadStmt(TmpFileType type) {
		return bulkLoadStmt.get(type);
	}

	public Database getNodeCache() {
		return nodeCache;
	}

	public File getTmpFile(TmpFileType type) {
		return new File(tmpDir, kbName + "_" + type);
	}

	public PrintWriter getWriter(TmpFileType type) {
		return tmpFileWriterMap.get(type);
	}

	private void initBulkLoadStatements() {
		bulkLoadStmt.put(TmpFileType.AllValuesFrom, "allvaluesfromclass");
		bulkLoadStmt.put(TmpFileType.CardinialityClass, "cardinalityclass");
		bulkLoadStmt.put(TmpFileType.ComplementClass, "complementclass");
		// bulkLoadStmt.put(TmpFileType.Datarange, "");
		bulkLoadStmt.put(TmpFileType.Datatype, "datatype");
		bulkLoadStmt.put(TmpFileType.DatatypeRestriction, "datatype_restriction");
		bulkLoadStmt.put(TmpFileType.DifferentIndividual, "differentindividual");
		bulkLoadStmt.put(TmpFileType.DisjointClass, "disjointclass");
		bulkLoadStmt.put(TmpFileType.Domain, "domain");
		bulkLoadStmt.put(TmpFileType.EquivalentClass, "equivalentclass");
		// bulkLoadStmt.put(TmpFileType.EquivalentProperty, "equivalentproperty");
		bulkLoadStmt.put(TmpFileType.HasSelf, "hasself");
		bulkLoadStmt.put(TmpFileType.HasValue, "hasvalue");
		bulkLoadStmt.put(TmpFileType.Individual,
				"individual(id,name,nsid,kbid,browsertext,is_named)");
		bulkLoadStmt.put(TmpFileType.IntersectionClass, "intersectionclass");
		// bulkLoadStmt.put(TmpFileType.InversePropertyOf, "");
		bulkLoadStmt.put(TmpFileType.Literal, "literal");
		bulkLoadStmt.put(TmpFileType.MaxCardinalityClass, "maxcardinalityclass");
		bulkLoadStmt.put(TmpFileType.MinCardinalityClass, "mincardinalityclass");
		bulkLoadStmt.put(TmpFileType.NamedClass,
				"primitiveclass(id,name,nsid,kbid,browsertext,is_system)");
		bulkLoadStmt.put(TmpFileType.OneOf, "oneof");
		bulkLoadStmt.put(TmpFileType.Property,
				"property(id,name,is_object,is_transitive,is_symmetric,"
						+ "is_functional,is_inversefunctional,is_datatype,is_annotation,"
						+ "is_system,nsid,kbid,browsertext,is_reflexive)");
		bulkLoadStmt.put(TmpFileType.Range, "range");
		bulkLoadStmt.put(TmpFileType.Relationship, "relationship");
		bulkLoadStmt.put(TmpFileType.SameIndividual, "sameindividual");
		bulkLoadStmt.put(TmpFileType.SomeValuesFrom, "somevaluesfromclass");
		bulkLoadStmt.put(TmpFileType.SubclassOf, "subclassof");
		bulkLoadStmt.put(TmpFileType.SubpropertyOf, "subpropertyof");
		bulkLoadStmt.put(TmpFileType.TypeOf, "typeof");
		bulkLoadStmt.put(TmpFileType.UnionClass, "unionclass");

	}

	private void initDatabase() throws Exception {
		//		String envDir = config.getString("berkeleyDB.envDir");
		// try {
		String envDir = AllConfiguration.getFileAbsPath("berkeleyDB.envDir");
		// } catch (ConfigurationException e) {
		// throw new OntoquestException(OntoquestException.Type.BACKEND,
		// e.getMessage(), e);
		// }
		dbEnv = BDBUtil.openEnvironment(envDir, true, true);

		nodeCache = createTempDB(dbEnv, config.getString("ontology_name"));
	}

	private void initTmpDir() throws Exception {
		String tmpDirName = AllConfiguration.getFileAbsPath("tmp_dir");
		// create tmp dir if not existing
		tmpDir = new File(tmpDirName);
		if (!tmpDir.exists()) {
			tmpDir.mkdir();
		}
	}

	private void initTmpFileWriters() throws Exception {
		this.kbName = AllConfiguration.getConfig().getString("ontology_name");

		initTmpDir();

		for (TmpFileType ft : TmpFileType.values()) {
			File f = getTmpFile(ft);
			PrintWriter writer = new PrintWriter(f);
			tmpFileWriterMap.put(ft, writer);
		}
	}
}
