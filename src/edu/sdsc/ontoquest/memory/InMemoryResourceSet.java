package edu.sdsc.ontoquest.memory;

import java.util.List;

import edu.sdsc.ontoquest.AbstractResourceSet;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSetMetaData;
import edu.sdsc.ontoquest.query.Variable;

import java.sql.Timestamp;

/**
 * @version $Id: InMemoryResourceSet.java,v 1.1 2010-10-28 06:30:40 xqian Exp $
 *
 */
public class InMemoryResourceSet extends AbstractResourceSet {
  Object[][] _data = null;
  InMemoryResourceSetMetaData _metaData = null;
  int _currentPosition = -1;
  
  public InMemoryResourceSet(Object[][] data, List<Variable> varList) {
    _data = data;
    _metaData = new InMemoryResourceSetMetaData(varList);
  }
  
  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getBoolean(int)
   */
  public boolean getBoolean(int columnIdx) throws OntoquestException {
    Object o = _data[_currentPosition][columnIdx-1];
    if (o instanceof Boolean)
      return ((Boolean)o).booleanValue();
    else
      return Boolean.valueOf(o.toString());
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getDouble(int)
   */
  public double getDouble(int columnIdx) throws OntoquestException {
    Object o = _data[_currentPosition][columnIdx-1];
    if (o instanceof Double)
      return ((Double)o).doubleValue();
    else
      return Double.valueOf(o.toString());
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getTimestamp(int)
   */
  public Timestamp getTimestamp(int columnIdx) throws OntoquestException {
    Object o = _data[_currentPosition][columnIdx-1];
    if (o instanceof Timestamp)
      return (Timestamp)o;
    else
      return Timestamp.valueOf(o.toString());
  }


  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getFloat(int)
   */
  public float getFloat(int columnIdx) throws OntoquestException {
    Object o = _data[_currentPosition][columnIdx-1];
    if (o instanceof Float)
      return ((Float)o).floatValue();
    else
      return Float.valueOf(o.toString());
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getInt(int)
   */
  public int getInt(int columnIdx) throws OntoquestException {
    Object o = _data[_currentPosition][columnIdx-1];
    if (o instanceof Integer)
      return ((Integer)o).intValue();
    else
      return Integer.valueOf(o.toString());
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getLong(int)
   */
  public long getLong(int columnIdx) throws OntoquestException {
    Object o = _data[_currentPosition][columnIdx-1];
    if (o instanceof Long)
      return ((Long)o).longValue();
    else
      return Long.valueOf(o.toString());
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getMetaData()
   */
  public ResourceSetMetaData getMetaData() throws OntoquestException {
    return _metaData;
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getObject(int)
   */
  public Object getObject(int columnIdx) throws OntoquestException {
    return _data[_currentPosition][columnIdx-1];
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getShort(int)
   */
  public short getShort(int columnIdx) throws OntoquestException {
    Object o = _data[_currentPosition][columnIdx-1];
    if (o instanceof Short)
      return ((Short)o).shortValue();
    else
      return Short.valueOf(o.toString());
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getString(int)
   */
  public String getString(int columnIdx) throws OntoquestException {
    return _data[_currentPosition][columnIdx-1].toString();
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#next()
   */
  public boolean next() throws OntoquestException {
    return (++_currentPosition < _data.length);
  }

  /**
   * @see edu.sdsc.ontoquest.Closeable#close()
   */
  public void close() throws OntoquestException {
    _currentPosition = _data.length-1; // move the cursor to end
    _isClosed = true;
  }
}
