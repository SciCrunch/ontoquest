package edu.sdsc.ontoquest.loader;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import edu.sdsc.ontoquest.loader.struct.OntNode;
import edu.sdsc.ontoquest.loader.struct.OntNodeBinding;

public class BDBUtil {
	private static OntNodeBinding nodeBinding = new OntNodeBinding();

	public static boolean containsNode(Database database, String key)
			throws UnsupportedEncodingException {
		DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
		DatabaseEntry theData = new DatabaseEntry();

		// Perform the get.
		return (database.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS);
	}

	protected static byte[] getBytes(int i) {
		return ByteBuffer.allocate(Integer.SIZE / 8).putInt(i).array();
	}

	public static Database openDB(Environment dbEnv, String dbName,
			boolean isTransactional, boolean allowCreate, boolean sortedDuplicates,
			boolean deferredWrite) {
		return openDB(dbEnv, dbName, isTransactional, allowCreate,
				sortedDuplicates, deferredWrite, false, false);
	}

	public static Database openDB(Environment dbEnv, String dbName,
			boolean isTransactional, boolean allowCreate, boolean sortedDuplicates,
			boolean deferredWrite, boolean isTemporary, boolean isReadOnly) {

		// make a database within that environment
		DatabaseConfig dbConfig = new DatabaseConfig();
		dbConfig.setTransactional(isTransactional);
		dbConfig.setAllowCreate(allowCreate);
		dbConfig.setSortedDuplicates(sortedDuplicates);
		if (!isTransactional)
			dbConfig.setDeferredWrite(deferredWrite);
		dbConfig.setTemporary(isTemporary);
		dbConfig.setReadOnly(isReadOnly);
		Database db = dbEnv.openDatabase(null, dbName, dbConfig);
		return db;
	}

	public static Database openDBToRead(Environment dbEnv, String dbName) {
		return openDB(dbEnv, dbName, true, false, false, false, false, true);
	}

	public static Environment openEnvironment(String envDir,
			boolean isTransactional, boolean allowCreate) {
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(isTransactional);
		envConfig.setAllowCreate(allowCreate);
		// envConfig.setCachePercent(90);
		return new Environment(new File(envDir), envConfig);
	}

	public static int searchData(Database database, String key)
			throws UnsupportedEncodingException {
		DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
		DatabaseEntry theData = new DatabaseEntry();

		// Perform the get.
		if (database.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {

			// Recreate the data String.
			byte[] retData = theData.getData();
			return ByteBuffer.wrap(retData).getInt();
		} else {
			return -1;
		}
	}

	public static OntNode searchNode(Database database, String key)
			throws UnsupportedEncodingException {
		DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
		DatabaseEntry theData = new DatabaseEntry();

		// Perform the get.
		if (database.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {

			// Recreate the data String.
			return nodeBinding.entryToObject(theData);
		} else {
			return null;
		}
	}

	/**
	 * Store the ontology node data
	 * @param database
	 * @param key node's uri or identifier
	 * @param value integer id
	 * @throws UnsupportedEncodingException 
	 */
	public static void storeData(Database database, String key, int value) throws UnsupportedEncodingException {
		DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
		DatabaseEntry theData = new DatabaseEntry(getBytes(value));
		database.put(null, theKey, theData);
	}

	public static void storeNode(Database database, String key, OntNode node) throws UnsupportedEncodingException {
		DatabaseEntry theKey = new DatabaseEntry(key.getBytes("UTF-8"));
		DatabaseEntry theData = new DatabaseEntry();
		nodeBinding.objectToEntry(node, theData);
		database.put(null, theKey, theData);
	}

}
