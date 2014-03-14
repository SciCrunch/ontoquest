package edu.sdsc.ontoquest.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.graph.Graph;
import edu.sdsc.ontoquest.graph.InMemoryAdjMapGraph;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;


/**
 * @version $Id: DbUtility.java,v 1.4 2013-09-24 23:08:50 jic002 Exp $
 *
 */
public class DbUtility {
	private static Log logger = LogFactory.getLog(DbUtility.class);

	private static final int SLASH = '/';
	private static final int BACKSLASH = '\\';
	private static final int STAR = '*';
	private static final int DASH = '-';
	private static final int SINGLEQUOTE = '\'';
	private static final int NEWLINE = '\n';
	private static final int SEMICOLON = ';';
	private static final int EOF = -1;
	private static final int ATSIGN = '@';
	private static final int DOLLAR = '$';
	public static final long DB_CONNECTION_WAIT_MS = 5000; // 5 second

	public static boolean executeSQLCommand(String sql, Context context,
			String[] args, String errorMsg) throws OntoquestException {
		Connection conn = null;
		try {
			Utility.checkBlank(sql, OntoquestException.Type.EXECUTOR,
					"Invalid statement: " + sql);
			for (int i = args.length - 1; i >= 0; i--) {
				sql = sql.replace(":" + (i + 1), args[i]);
			}
			if (logger.isDebugEnabled())
				logger.debug(sql);
			System.out.println(sql);
			conn = getDBConnection(context);
			Statement stmt = conn.createStatement();
			return stmt.execute(sql);
		} catch (Exception e) {
			if (!(e instanceof OntoquestException)) {
				e.printStackTrace();
				throw new OntoquestException(OntoquestException.Type.BACKEND,
						errorMsg, e);
			} else {
				throw (OntoquestException) e; // throw ontoquest exception up
			}
		} finally {
			releaseDbConnection(conn, context);
		}
	}

	public static boolean executeSQLCommandName(String sqlProperty,
			Context context, String[] args, String errorMsg)
					throws OntoquestException {
		try {
			String sql = AllConfiguration.getConfig().getString(sqlProperty);
			return executeSQLCommand(sql, context, args, errorMsg);
		} catch (ConfigurationException ce) {
			throw new OntoquestException(OntoquestException.Type.BACKEND,
					"Invalid configuration: " + sqlProperty + ". Details: "
							+ ce.getMessage(), ce);
		}
	}

	public static ResourceSet executeSQLQuery(String sql, Context context,
			List<Variable> varList, String[] args, String errorMsg, int resultLimit)
					throws OntoquestException {

		Connection conn = null;
		ResultSet rs = null;
    
    if ( resultLimit > 0  ) 
    {
      if ( ! sql.matches("(?is).* limit :?\\d+.*") )
      {
        sql = sql + " limit " + (resultLimit +1);
      }
    }

		try {
			Utility.checkBlank(sql, OntoquestException.Type.EXECUTOR,
					"Invalid statement: " + sql);
      if ( args != null) 
			  for (int i = args.length - 1; i >= 0; i--) {
				  sql = sql.replace(":" + (i + 1), args[i]);
			  }
			if (logger.isDebugEnabled())
				logger.debug(sql);
			// System.out.println(sql);
			conn = getDBConnection(context);
			Statement stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			return new DbResourceSet(rs, varList, (DbContext) context);
		} catch (Exception e) {
			try {
				rs.close();
			} catch (Exception e2) {
			}
			releaseDbConnection(conn, context);
			if (!(e instanceof OntoquestException)) {
				e.printStackTrace();
				throw new OntoquestException(OntoquestException.Type.BACKEND,
						errorMsg, e);
			} else {
				throw (OntoquestException) e; // throw ontoquest exception up
			}
		}
	}

	public static ResourceSet executeSQLQueryName(String sqlProperty,
			Context context, List<Variable> varList, String[] args,
			String errorMsg, int resultLimit) throws OntoquestException {
		try {
			String sql = AllConfiguration.getConfig().getString(sqlProperty);
			return executeSQLQuery(sql, context, varList, args, errorMsg, resultLimit);
		} catch (ConfigurationException ce) {
			throw new OntoquestException(OntoquestException.Type.BACKEND,
					"Invalid configuration: " + sqlProperty + ". Details: "
							+ ce.getMessage(), ce);
		}
	}

	public static int fetchSeqNextVal(String seqProp, Context context)
			throws OntoquestException {
		ArrayList<Variable> varList1 = new ArrayList<Variable>();
		varList1.add(new Variable("v1", 1));
		ResourceSet rs = null;
		try {
			String seqName = AllConfiguration.getConfig().getString(seqProp);
			rs = DbUtility.executeSQLQueryName("query.get_seq_nextval",
					context, varList1, new String[] { seqName },
					"Failed to fetch the next value of sequence: " + seqName, -1);
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (ConfigurationException ce) {
			throw new OntoquestException(OntoquestException.Type.BACKEND,
					"Invalid configuration: " + seqProp + ". Details: "
							+ ce.getMessage(), ce);
		} finally {
			if (rs != null)
				rs.close();

		}
		return 1;
	}

	public static String formatArrayToSingleQuotedStr(String[] strArray) {
		StringBuilder sb = new StringBuilder();
		for (String s : strArray) {
			if (!Utility.isBlank(s)) {
				sb.append("'").append(s.replace("'", "''")).append("',");
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public static String formatArrayToStr(String[] strArray) {
		StringBuilder sb = new StringBuilder();
		for (String s : strArray) {
			if (!Utility.isBlank(s)) {
				sb.append("''").append(s.replace("'", "''''")).append("'',");
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * convert a list of string to single-quoted string. For example,
	 * "''AAA'', ''BB BBB'', ''VV''"
	 * 
	 * @param names
	 * @return
	 */
	public static String formatDoubleQuotedString(String str) {
		if (str == null)
			return null;
		return str.replace("\"", "\\\"");
	}

	public static String formatIDsToSingleQuotedStr(List<int[]> list) {
		StringBuilder sb = new StringBuilder();
		for (int[] array1 : list) {
			sb.append('(').append(array1[0]).append(',').append(array1[1])
			.append("),");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * Format the input string str to SQL style. If str contains single quote
	 * ('), escape it with two single quotes('').
	 * 
	 * @param str
	 * @return
	 */
	public static String formatSQLString(String str) {
		return str.replace("'", "''");
	}

	public static Connection getDBConnection(Context context)
			throws SQLException, OntoquestException {
		Utility.checkClass(context, DbContext.class,
				OntoquestException.Type.EXECUTOR);
		DbConnectionPool conPool = ((DbContext) context).getConPool();
		return conPool.getConnection(DB_CONNECTION_WAIT_MS);
	}

	public static Graph loadGraph(int[] kbid, DbContext context,
			boolean isWeighted) throws OntoquestException {
		ArrayList<Variable> varList = new ArrayList<Variable>(5);
		for (int i = 0; i < 5; i++) {
			varList.add(new Variable(1));
		}
		if (isWeighted)
			varList.add(new Variable(1));

		ResourceSet rs = DbBasicFunctions.getInstance().scanAllRelationships(
				kbid, context, varList, isWeighted);
		Graph graph = new InMemoryAdjMapGraph();
		while (rs.next()) {
			float weight = (isWeighted) ? rs.getFloat(6) : 1;
			graph.addEdge(rs.getInt(1), rs.getInt(2), rs.getInt(3),
					rs.getInt(4), rs.getInt(5), weight);
		}
		return graph;
	}

	public static void releaseDbConnection(Connection conn, Context context) {
		((DbContext) context).getConPool().releaseConnection(conn);
	}

	protected static String replaceInputs(String paramString, HashMap<String, String> paramHashMap)
	{
		String str1 = paramString;
		Iterator<String> localIterator = paramHashMap.keySet().iterator();
		while (localIterator.hasNext())
		{
			String str2 = localIterator.next();
			str1 = str1.replace(str2, paramHashMap.get(str2));
		}
		return str1;
	}

	protected static boolean runSqlCommand(String stmtStr, 
			List inputRow, Connection con) throws SQLException {
		//	    Connection con = conPool.getConnection();
		PreparedStatement pstmt = null;
		try {
			pstmt = con.prepareStatement(stmtStr);
			if (inputRow != null) {
				for (int i=0; i<inputRow.size(); i++) {
					pstmt.setObject(i+1, inputRow.get(i));
				}
			}
			return pstmt.execute();

		} finally {
			try {
				if (pstmt != null) pstmt.close();
			} catch (SQLException sqle) {}
			//	      conPool.releaseConnection(con);
		}
	}

	public static ResultSet runSqlQuery(String stmtStr, Connection con) throws SQLException {
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			return stmt.executeQuery(stmtStr);
		} catch (SQLException e) {
			try {
				if (stmt != null) stmt.close();
			} catch (SQLException sqle) {}
			throw e;
		}
	}

	public static void runSqlScript(File scriptFile, Connection conn)
			throws IOException, SQLException {
		runSqlScript(scriptFile, null, conn);
		// BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
		// StringBuilder bufSql = new StringBuilder();
		// int ch;
		// int nextCh;
		// boolean inSlashStarComment = false;
		// boolean inDashDashComment = false;
		// boolean inQuote = false;
		// boolean inIncludeScript = false;
		// boolean inDoubleDollar = false;
		// Statement statement = null;
		//
		// try {
		// // initialize statement
		// statement = conn.createStatement();
		// readLoop: while ((ch = reader.read()) != EOF) {
		// if (inSlashStarComment) {
		// if (ch == STAR) {
		// nextCh = reader.read();
		// if (nextCh == EOF) { // unterminated comment
		// break readLoop;
		// }
		// if (nextCh == SLASH) { // exit /* comment
		// inSlashStarComment = false;
		// }
		// }
		// continue;
		// }
		// if (inDashDashComment) {
		// if (ch == NEWLINE) { // exit -- comment
		// inDashDashComment = false;
		// }
		// continue;
		// }
		//
		// if (inDoubleDollar) {
		// bufSql.append((char) ch);
		// if (ch == DOLLAR) {
		// nextCh = reader.read();
		// if (nextCh == EOF) { // unterminated comment
		// break readLoop;
		// }
		// if (nextCh == DOLLAR) { // exit /* comment
		// inDoubleDollar = false;
		// }
		// bufSql.append((char) nextCh);
		// }
		// continue;
		// }
		//
		// if (inIncludeScript) {
		// if (ch == NEWLINE || ch == SEMICOLON) {
		// // run the included script
		// String includedScriptName = bufSql.toString().trim();
		// File includedScript = new File(includedScriptName);
		// if (!includedScript.isAbsolute())
		// includedScript = new File(
		// scriptFile.getParentFile(),
		// includedScriptName);
		// runSqlScript(includedScript, conn);
		// inIncludeScript = false;
		// bufSql = new StringBuilder();
		// continue;
		// }
		// // continue;
		// }
		//
		// if (inQuote) {
		// bufSql.append((char) ch); // just add to the buffer
		// if (ch == BACKSLASH) {
		// nextCh = reader.read();
		// if (nextCh == EOF) { // unterminated comment
		// break readLoop;
		// }
		// continue; // ignore escaped character
		// }
		// if (ch == SINGLEQUOTE) { // exit quote
		// inQuote = false;
		// }
		// continue;
		// }
		// switch (ch) {
		// case SLASH:
		// nextCh = reader.read();
		// if (nextCh == EOF) { // final ch is SLASH ???
		// break readLoop;
		// }
		// if (nextCh == STAR) { // entering /* comment
		// inSlashStarComment = true;
		// } else { // not start of /* comment
		// bufSql.append((char) ch);
		// bufSql.append((char) nextCh);
		// }
		// continue;
		// case BACKSLASH:
		// nextCh = reader.read();
		// if (nextCh == EOF) { // final ch is BACKSLASH ???
		// break readLoop;
		// }
		// if (nextCh == 'i') { // entering included script clause \i
		// inIncludeScript = true;
		// } else { // not start of \i
		// bufSql.append((char) ch);
		// bufSql.append((char) nextCh);
		// }
		// continue;
		// case DASH:
		// nextCh = reader.read();
		// if (nextCh == EOF) { // final ch is DASH ???
		// break readLoop;
		// }
		// if (nextCh == DASH) { // entering -- comment
		// inDashDashComment = true;
		// } else { // not start of -- comment
		// bufSql.append((char) ch);
		// bufSql.append((char) nextCh);
		// }
		// continue;
		// case DOLLAR:
		// nextCh = reader.read();
		// if (nextCh == EOF) {
		// break readLoop;
		// }
		// if (nextCh == DOLLAR) { // entering the body of pg/plsql
		// // stored procedure
		// inDoubleDollar = true;
		// }
		// bufSql.append((char) ch);
		// bufSql.append((char) nextCh);
		// continue;
		// case SINGLEQUOTE:
		// inQuote = true;
		// bufSql.append((char) ch); // just add to the buffer
		// continue; // entering quote
		// case SEMICOLON:
		// if (inDoubleDollar) { // in body of pl/pgsql function, just
		// // append the semicolon.
		// bufSql.append((char) ch);
		// continue;
		// }
		// String sSql = bufSql.toString().trim();
		// if (logger.isDebugEnabled()) {
		// logger.debug("sql=" + sSql);
		// }
		// statement.execute(sSql);
		// if (logger.isDebugEnabled()) {
		// SQLWarning sqlw = null;
		// if ((sqlw = statement.getWarnings()) != null) {
		// logger.debug("sqlw=" + sqlw);
		// }
		// }
		// bufSql = new StringBuilder();
		// continue; // start building next command
		// default:
		// bufSql.append((char) ch); // just add to the buffer
		// continue;
		// }
		// }
		// if (inIncludeScript && bufSql.length() > 0) {
		// // run the included script
		// String includedScriptName = bufSql.toString().trim();
		// File includedScript = new File(includedScriptName);
		// if (!includedScript.isAbsolute())
		// includedScript = new File(scriptFile.getParentFile(),
		// includedScriptName);
		// runSqlScript(includedScript, conn);
		// }
		// } finally {
		// if (statement != null) {
		// try {
		// statement.close();
		// } catch (SQLException ex) {
		// }
		// }
		// }
	}

	public static void runSqlScript(File scriptFile,
			HashMap<String, String> inputValues, Connection conn) throws IOException,
			SQLException {
		BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
		StringBuilder bufSql = new StringBuilder();
		int ch;
		int nextCh;
		boolean inSlashStarComment = false;
		boolean inDashDashComment = false;
		boolean inQuote = false;
		Statement statement = null;

		try {
			readLoop: while ((ch = reader.read()) != EOF) {
				if (inSlashStarComment) {
					if (ch == STAR) {
						nextCh = reader.read();
						if (nextCh == EOF) { // unterminated comment
							break readLoop;
						}
						if (nextCh == SLASH) { // exit /* comment
							inSlashStarComment = false;
						}
					}
					continue;
				}
				if (inDashDashComment) {
					if (ch == NEWLINE) { // exit -- comment
						inDashDashComment = false;
					}
					continue;
				}
				if (inQuote) {
					bufSql.append((char) ch); // just add to the buffer
					if (ch == BACKSLASH) {
						nextCh = reader.read();
						if (nextCh == EOF) { // unterminated comment
							break readLoop;
						}
						continue; // ignore escaped character
					}
					if (ch == SINGLEQUOTE) { // exit quote
						inQuote = false;
					}
					continue;
				}
				switch (ch) {
				case SLASH:
					nextCh = reader.read();
					if (nextCh == EOF) { // final ch is SLASH ???
						break readLoop;
					}
					if (nextCh == STAR) { // entering /* comment
						inSlashStarComment = true;
					} else { // not start of /* comment
						bufSql.append((char) ch);
						bufSql.append((char) nextCh);
					}
					continue;
				case DASH:
					nextCh = reader.read();
					if (nextCh == EOF) { // final ch is DASH ???
						break readLoop;
					}
					if (nextCh == DASH) { // entering -- comment
						inDashDashComment = true;
					} else { // not start of -- comment
						bufSql.append((char) ch);
						bufSql.append((char) nextCh);
					}
					continue;
				case SINGLEQUOTE:
					inQuote = true;
					bufSql.append((char) ch); // just add to the buffer
					continue; // entering quote
				case SEMICOLON:
					String sSql = bufSql.toString().trim();
					if (sSql.toLowerCase().startsWith("select")) {
						sSql = replaceInputs(sSql, inputValues);
						if (logger.isDebugEnabled()) {
							logger.debug("sql=" + sSql);
						}
						ResultSet rs = runSqlQuery(sSql, conn);
						while (rs.next()) {
							System.out.println(rs.getString(1));
						}
					} else {
						sSql = replaceInputs(sSql, inputValues);
						if (logger.isDebugEnabled()) {
							logger.debug("sql=" + sSql);
						}
						runSqlCommand(sSql, null, conn);
					}
					conn.commit();

					// statement.execute(sSql);
					// if (log.isLoggable(Level.INFO)) {
					// SQLWarning sqlw = null;
					// if ((sqlw = statement.getWarnings()) != null) {
					// log.info("sqlw=" + sqlw);
					// }
					// }
					bufSql = new StringBuilder();
					continue; // start building next command
				default:
					bufSql.append((char) ch); // just add to the buffer
					continue;
				}
			}
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException ex) {
				}
			}
		}
	}

	public static void runSqlScript(File scriptFile,
			HashMap<String, String> inputValues, DbConnectionPool conPool)
					throws IOException, SQLException {
		Connection c = null;
		try {
			c = conPool.getConnection();
			runSqlScript(scriptFile, inputValues, c);
		} finally {
			conPool.releaseConnection(c);
		}
	}

	public static boolean schemaExists(DbConnectionPool conPool)
			throws SQLException {
		return tableExists("KB", conPool);
	}

	public static boolean tableExists(String tableName, DbConnectionPool conPool)
			throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		try {
			String sql = "select 1 from " + tableName;
			conn = conPool.getConnection();
			stmt = conn.createStatement();
			try {
				stmt.executeQuery(sql);
			} catch (SQLException sqle) {
				return false;
			}
			return true;
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
			conPool.releaseConnection(conn);
		}
	}

	/**
	 * convert a list of string to single-quoted string. For example,
	 * "''AAA'', ''BB BBB'', ''VV''"
	 * 
	 * @param names
	 * @return
	 */
	public static String toQuotedString(String[] names) {
		if (names == null || names.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (String name : names) {
			if (name.length() == 0)
				continue;
			// convert one single quote to four single quotes
			sb.append("''")
			.append(DbUtility.formatSQLString(DbUtility.formatSQLString(name)))
			.append("''").append(',');
		}
		if (sb.length() > 4)
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

}
