package edu.sdsc.ontoquest.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.sdsc.ontoquest.AbstractResourceSet;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSetManager;
import edu.sdsc.ontoquest.ResourceSetMetaData;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.Variable;

import java.sql.Timestamp;

/**
 * A resource set wrapping around a java.sql.ResultSet object. When the SQLResourceSet
 * is instantiated, the actual data (rows) are still in the underlying database
 * and the wrapped java.sql.ResultSet is the handle to fetch the rows.
 *
 * @version $Id: DbResourceSet.java,v 1.1 2010-10-28 06:30:09 xqian Exp $
 *
 */
public class DbResourceSet extends AbstractResourceSet {

  private List<ResultSet> rsList = null;
  private int currentRSIdx = 0;
  private DbResourceSetMetaData _metaData = null;
  private DbContext _context = null;
  
  private static Log logger = LogFactory.getLog(DbResourceSet.class);
  
  /**
   * Creates an instance which wraps around result set rs.
   * @param rs the sql result set to be wrapped.
   */
  public DbResourceSet(ResultSet rs, List<Variable> varList, DbContext context) throws OntoquestException {
    Utility.checkNull(rs, OntoquestException.Type.EXECUTOR, "Backend result set is null.");
    rsList = new ArrayList<ResultSet>(1);
    rsList.add(rs);
    initialize(varList, context);
  }
  
  public DbResourceSet(List<ResultSet> rsList, List<Variable> varList, DbContext context) throws OntoquestException {
    Utility.checkBlank(rsList, OntoquestException.Type.EXECUTOR, 
        "Backend result set list is empty");
    this.rsList = rsList;
    ResultSet rs1 = rsList.get(0);
    for (ResultSet rs2 : rsList) {
      if (!canUnion(rs1, rs2))
        throw new OntoquestException(OntoquestException.Type.EXECUTOR, 
            "The number of columns and their data types must be same in all result sets");
    }
    initialize(varList, context);
  }

//  public DbResourceSet(List<Variable> varList, DbContext contextList, <ResourceSet> rscSetList) throws OntoquestException {
//    Utility.checkBlank(rsList, OntoquestException.Type.EXECUTOR, "Input resource set is null");
//    rsList = new ArrayList<ResultSet>();
//    
//  }
  
  private void initialize(List<Variable> varList, DbContext context) throws OntoquestException {
    super.initialize();
    try {
      _metaData = new DbResourceSetMetaData(rsList.get(0).getMetaData(), varList);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get meta data of the result set. ", e);
    }
    _context = context;
    ResourceSetManager.getInstance().addResourceSet(this);
  }
  
  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getBoolean(int)
   */
  public boolean getBoolean(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getBoolean(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get boolean value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getDouble(int)
   */
  public double getDouble(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getDouble(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get double value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#getFloat(int)
   */
  public float getFloat(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getFloat(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get float value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#getInt(int)
   */
  public int getInt(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getInt(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get integer value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#getLong(int)
   */
  public long getLong(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getLong(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get long value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#getTimestamp(int)
   */
  public Timestamp getTimestamp(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getTimestamp(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get timestamp value for attribute " + columnIdx, e);
    }
  }


  /**
   * @see edu.sdsc.ontoquest.ResultSet#getMetaData()
   */
  public ResourceSetMetaData getMetaData() throws OntoquestException {
    return _metaData;
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#getObject(int)
   */
  public Object getObject(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getObject(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get object value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#getShort(int)
   */
  public short getShort(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getShort(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get short value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#getString(int)
   */
  public String getString(int columnIdx) throws OntoquestException {
    try {
      return rsList.get(currentRSIdx).getString(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to get string value for attribute " + columnIdx, e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResultSet#next()
   */
  public boolean next() throws OntoquestException {
    try {
      boolean hasNext = rsList.get(currentRSIdx).next();
      if (!hasNext && (rsList.size() > (++currentRSIdx)))
        hasNext = rsList.get(currentRSIdx).next();
      return hasNext;
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to check the iterator", e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.Closeable#close()
   */
  public synchronized void close() throws OntoquestException {
    if (isClosed()) return;
    
    for (ResultSet rs : rsList) {
      // return the connection to pool.
      try {
        Connection conn = rs.getStatement().getConnection();
        _context.getConPool().releaseConnection(conn);
      } catch (SQLException e) {
        logger.warn("Unable to release JDBC connection.");
      }
      // close the result set.
      try {
        rs.close();
      } catch (SQLException e) {
          logger.warn("Unable to close the resource set.", e);
      }
    }
    _isClosed = true;
  }

  public boolean add(List<ResultSet> rsList2) throws OntoquestException {
    if (canUnion(rsList.get(currentRSIdx), rsList2.get(0))) {
      rsList.addAll(rsList2);
      return true;
    }
    return false;
  }
  
  /**
   * Checks whether or not result sets can union. If two result sets
   * have same number of columns and the columns' datatypes are identical,
   * the result sets can union.
   * @param rs1
   * @param rs2
   * @return
   */
  private boolean canUnion(ResultSet rs1, ResultSet rs2) {
    //TODO
    return true;
    
  }

  protected List<ResultSet> getResultSetList() {
    return rsList;
  }
  
  protected int getCurrentPosition() {
    return currentRSIdx;
  }
  
  /**
   * merge two resource sets. The resource sets must share same variable
   * list. The input resource set rs2's position will be reset to the beginning.
   * rs2 will also be removed from resource set manager.
   * @param rs2
   * @return
   * @throws OntoquestException
   */
  public void merge(DbResourceSet rs2) throws OntoquestException {
    List<ResultSet> rsList2 = rs2.getResultSetList();
    if (Utility.isBlank(rsList2))
      return;
    
    // check whether meta data match to each other
    if (getMetaData().compareTo(rs2.getMetaData()) != 0)
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to merge two resource sets. The meta data do not match. Both resource sets must share same variables.");
    
    // try to combine result sets
    if (!add(rsList2))
      throw new OntoquestException(OntoquestException.Type.BACKEND,
          "Unable to merge two resource sets. Could not combine sql result sets.");
    
    // update creation time. Use the one created later.
    setCreationTime(Math.max(getCreationTime(), rs2.getCreationTime()));
    // remove rs2 from resource set manager
    ResourceSetManager.getInstance().removeResourceSetWithoutClose(rs2.getID());
  }
}
