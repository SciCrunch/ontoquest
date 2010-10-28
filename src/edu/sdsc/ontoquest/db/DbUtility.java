package edu.sdsc.ontoquest.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
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
 * @version $Id: DbUtility.java,v 1.1 2010-10-28 06:30:10 xqian Exp $
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

  public static Graph loadGraph(int[] kbid, DbContext context, boolean isWeighted) throws OntoquestException {
    ArrayList<Variable> varList = new ArrayList<Variable>(5);
    for (int i=0; i<5; i++) {
      varList.add(new Variable(1));
    }
    if (isWeighted)
      varList.add(new Variable(1));
    
    ResourceSet rs = DbBasicFunctions.getInstance().scanAllRelationships(kbid, context, varList, isWeighted);
    Graph graph = new InMemoryAdjMapGraph();
    while (rs.next()) {
      float weight = (isWeighted) ? rs.getFloat(6) : 1;
      graph.addEdge(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), weight);
    }
    return graph;
  }

  /**
   * Format the input string str to SQL style. If str contains single quote ('),
   * escape it with two single quotes('').
   * @param str
   * @return
   */
  public static String formatSQLString(String str) {
    return str.replace("'", "''");
  }
  
  public static String formatArrayToStr(String[] strArray) {
    StringBuilder sb = new StringBuilder();
    for (String s : strArray) {
      if (!Utility.isBlank(s)) {
        sb.append("''").append(s.replace("'", "''''")).append("'',");
      }
    }
    sb.deleteCharAt(sb.length()-1);
    return sb.toString();
  }
  
  public static String formatArrayToSingleQuotedStr(String[] strArray) {
    StringBuilder sb = new StringBuilder();
    for (String s : strArray) {
      if (!Utility.isBlank(s)) {
        sb.append("'").append(s.replace("'", "''")).append("',");
      }
    }
    sb.deleteCharAt(sb.length()-1);
    return sb.toString();
  }
  
  public static String formatIDsToSingleQuotedStr(List<int[]> list) {
    StringBuilder sb = new StringBuilder();
    for (int[] array1 : list) {
      sb.append('(').append(array1[0]).append(',').append(array1[1]).append("),");
    }
    sb.deleteCharAt(sb.length()-1);
    return sb.toString();
  }
  
  public static ResourceSet executeSQLCommandName(String sqlProperty, 
      Context context, 
      List<Variable> varList,
      String[] args,
      String errorMsg) throws OntoquestException {
    try {
      String sql = AllConfiguration.getConfig().getString(sqlProperty);
      return executeSQLCommand(sql, context, varList, args, errorMsg);
    } catch (ConfigurationException ce) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Invalid configuration: " + sqlProperty +". Details: " + ce.getMessage(), ce);
    }
  }

  public static ResourceSet executeSQLCommand(String sql, 
      Context context, 
      List<Variable> varList,
      String[] args,
      String errorMsg) throws OntoquestException {

    Connection conn = null;
    ResultSet rs = null;
    try {
      Utility.checkBlank(sql, OntoquestException.Type.EXECUTOR, 
          "Invalid statement: "+sql);
      for (int i=args.length-1; i>=0; i--) {
        sql = sql.replace(":"+(i+1), args[i]);
      }
      if (logger.isDebugEnabled())
        logger.debug(sql);
//      System.out.println(sql);
      conn = getDBConnection(context);
      Statement stmt = conn.createStatement();
      rs = stmt.executeQuery(sql);
      return new DbResourceSet(rs, varList, (DbContext)context);
    } catch (Exception e) {
      try {
        rs.close();
      } catch (Exception e2) {}
      releaseDbConnection(conn, context);
      if (!(e instanceof OntoquestException)) {
        e.printStackTrace();
        throw new OntoquestException(OntoquestException.Type.BACKEND, errorMsg, e);
      } else {
        throw (OntoquestException)e; // throw ontoquest exception up
      }
    }
  }
  
  public static Connection getDBConnection(Context context) throws SQLException, OntoquestException {
    Utility.checkClass(context, DbContext.class, OntoquestException.Type.EXECUTOR);
    DbConnectionPool conPool = ((DbContext)context).getConPool();
    return conPool.getConnection(DB_CONNECTION_WAIT_MS);
  }

  public static void releaseDbConnection(Connection conn, Context context) {
    ((DbContext)context).getConPool().releaseConnection(conn);
  }

  /**
   * convert a list of string to single-quoted string. For example, "''AAA'', ''BB BBB'', ''VV''"
   * @param names
   * @return
   */
  public static String toQuotedString(String[] names) {
    if (names == null || names.length == 0)
      return "";
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<names.length; i++) {
      if (names[i].length()==0)
        continue;
      // convert one single quote to four single quotes
      sb.append("''").append(DbUtility.formatSQLString(
          DbUtility.formatSQLString(names[i]))).append("''").append(',');
    }
    if (sb.length() > 4)
      sb.deleteCharAt(sb.length()-1);
    return sb.toString();
  }

  /**
   * convert a list of string to single-quoted string. For example, "''AAA'', ''BB BBB'', ''VV''"
   * @param names
   * @return
   */
  public static String formatDoubleQuotedString(String str) {
    if (str == null) return null;
    return str.replace("\"", "\\\"");
  }

  public static void runSqlScript(File scriptFile, Connection conn)
      throws IOException, SQLException {
    BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
    StringBuilder bufSql = new StringBuilder();
    int ch;
    int nextCh;
    boolean inSlashStarComment = false;
    boolean inDashDashComment = false;
    boolean inQuote = false;
    boolean inIncludeScript = false;
    boolean inDoubleDollar = false;
    Statement statement = null;

    try {
      // initialize statement
      statement = conn.createStatement();
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
        
        if (inDoubleDollar) {
          bufSql.append((char)ch);
          if (ch == DOLLAR) {
            nextCh = reader.read();
            if (nextCh == EOF) { // unterminated comment
              break readLoop;
            }
            if (nextCh == DOLLAR) { // exit /* comment
              inDoubleDollar = false;
            }
            bufSql.append((char)nextCh);
          }
          continue;
        }
        
        if (inIncludeScript) {
          if (ch == NEWLINE || ch == SEMICOLON) {
            // run the included script
            String includedScriptName = bufSql.toString().trim();
            File includedScript = new File(includedScriptName);
            if (!includedScript.isAbsolute())
              includedScript = new File(scriptFile.getParentFile(), includedScriptName);
            runSqlScript(includedScript, conn);
            inIncludeScript = false;
            bufSql = new StringBuilder();
            continue;
          }
//          continue;
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
        case BACKSLASH:
          nextCh = reader.read();
          if (nextCh == EOF) { // final ch is BACKSLASH ???
            break readLoop;
          }
          if (nextCh == 'i') { // entering included script clause \i
            inIncludeScript = true;
          } else { // not start of \i
            bufSql.append((char)ch);
            bufSql.append((char)nextCh);
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
        case DOLLAR:
          nextCh = reader.read();
          if (nextCh == EOF) {
            break readLoop;
          }
          if (nextCh == DOLLAR) { // entering the body of pg/plsql stored procedure
            inDoubleDollar = true;
          }
          bufSql.append((char) ch);
          bufSql.append((char) nextCh);
          continue;
        case SINGLEQUOTE:
          inQuote = true;
          bufSql.append((char) ch); // just add to the buffer
          continue; // entering quote
        case SEMICOLON:
          if (inDoubleDollar) { // in body of pl/pgsql function, just append the semicolon.
            bufSql.append((char)ch);
            continue;
          }
          String sSql = bufSql.toString().trim();
          if (logger.isDebugEnabled()) {
            logger.debug("sql=" + sSql);
          }
          statement.execute(sSql);
          if (logger.isDebugEnabled()) {
            SQLWarning sqlw = null;
            if ((sqlw = statement.getWarnings()) != null) {
              logger.debug("sqlw=" + sqlw);
            }
          }
          bufSql = new StringBuilder();
          continue; // start building next command
        default:
          bufSql.append((char) ch); // just add to the buffer
          continue;
        }
      }
      if (inIncludeScript && bufSql.length() > 0) {
        // run the included script
        String includedScriptName = bufSql.toString().trim();
        File includedScript = new File(includedScriptName);
        if (!includedScript.isAbsolute())
          includedScript = new File(scriptFile.getParentFile(), includedScriptName);
        runSqlScript(includedScript, conn);
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

}
