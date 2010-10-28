package edu.sdsc.ontoquest;

import edu.sdsc.ontoquest.query.Variable;

/**
 * An object that can be used to get information about the types and properties of 
 * the columns in a <code>ResultSet</code> object. The following code fragment 
 * creates the ResultSet object rs, creates the ResultSetMetaData object rsmd, 
 * and uses rsmd to find out how many columns rs has.
 * <pre>
 *           // dbConnectionPool is an instance of DbConnectionPool, initialize it first.
 *           conn = dbConnectionPool.getConnection();
 *           ResultSet rs = BasicFunctions.getTripleIDs(conn);
 *           ResultSetMetaData rsmd = rs.getMetaData();
 *           int numberOfColumns = rsmd.getColumnCount();
 * </pre>
 * @version $Id: ResourceSetMetaData.java,v 1.1 2010-10-28 06:30:00 xqian Exp $
 *
 */
public interface ResourceSetMetaData extends Comparable<ResourceSetMetaData>{

  public String getColumnName(int columnIdx) throws OntoquestException;
  
  /**
   * Gets the number of columns in the result set
   * @return
   * @throws OntoquestException
   */
  public int getColumnCount() throws OntoquestException;
  
  /**
   * Get the name of the java class whose instances are manufactured if the method 
   * ResultSet.getObject is called to retrieve a value from the column. 
   * ResultSet.getObject may return a subclass of the class returned by this method.
   * @param columnIdx column index starting from 1
   * @return
   * @throws OntoquestException
   */
  public String getColumnClassName(int columnIdx) throws OntoquestException;
  
  /**
   * Get the variable corresponding to the specified column.
   * @param columnIdx the position of column
   * @return
   * @throws OntoquestException
   */
  public Variable getColumnVariable(int columnIdx) throws OntoquestException;
}
