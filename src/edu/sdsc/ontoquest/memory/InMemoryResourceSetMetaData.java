package edu.sdsc.ontoquest.memory;

import java.util.List;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSetMetaData;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: InMemoryResourceSetMetaData.java,v 1.1 2010-10-28 06:30:40 xqian Exp $
 *
 */
public class InMemoryResourceSetMetaData implements ResourceSetMetaData {
  List<Variable> _varList = null;

  public InMemoryResourceSetMetaData(List<Variable> varList) {
    _varList = varList;
  }
  
  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.ResourceSetMetaData#getColumnClassName(int)
   */
  public String getColumnClassName(int columnIdx) throws OntoquestException {
    Variable v = _varList.get(columnIdx-1);
    // get class name for v's datatype 
    //TODO
    return null;
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

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(ResourceSetMetaData o) {
    // TODO Auto-generated method stub
    return 0;
  }
  

}
