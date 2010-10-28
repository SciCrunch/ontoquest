package edu.sdsc.ontoquest.db;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSetMetaData;
import edu.sdsc.ontoquest.query.Variable;

/**
 * Meta data for SQLResourceSet.
 * @version $Id: DbResourceSetMetaData.java,v 1.1 2010-10-28 06:30:08 xqian Exp $
 *
 */
public class DbResourceSetMetaData implements ResourceSetMetaData {
  List<Variable> _varList = null;
  ResultSetMetaData _rsMetaData = null;
  
  public DbResourceSetMetaData(ResultSetMetaData rsMetaData, List<Variable> varList) 
      throws OntoquestException {
    try {
      if (rsMetaData.getColumnCount() != varList.size()) {
        throw new OntoquestException(OntoquestException.Type.EXECUTOR, 
            "The number of atributes in the Result set is incorrect. Expected size : "+
            varList.size() + "; Actual size : " + rsMetaData.getColumnCount());
      }
      _varList = varList;
      _rsMetaData = rsMetaData;
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Failed to access meta data.", e);
    }
  }
  
  /**
   * @see edu.sdsc.ontoquest.ResourceSetMetaData#getColumnClass(int)
   */
  public String getColumnClassName(int columnIdx) throws OntoquestException {
    try {
      return _rsMetaData.getColumnClassName(columnIdx);
    } catch (SQLException e) {
      throw new OntoquestException(OntoquestException.Type.BACKEND, 
          "Unable to access meta data. ", e);
    }
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSetMetaData#getColumnCount()
   */
  public int getColumnCount() throws OntoquestException {
    return _varList.size();
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSetMetaData#getColumnName(int)
   */
  public String getColumnName(int columnIdx) throws OntoquestException {
    return _varList.get(columnIdx-1).getName();
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSetMetaData#getColumnVariable(int)
   */
  public Variable getColumnVariable(int columnIdx) throws OntoquestException {
    return _varList.get(columnIdx-1);
  }

  public int compareTo(ResourceSetMetaData metaData2) {
    return 0;
    //TODO
  }
}
