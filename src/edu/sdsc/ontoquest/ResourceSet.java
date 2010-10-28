package edu.sdsc.ontoquest;

/**
 * Interface of data iterator. This interface defines how to fetch
 * result one by one. The underlying data source may be a relational
 * database, data file or in-memory structure. This interface encapsulates
 * the actual data format and storage.
 * 
 * @version $Id: ResourceSet.java,v 1.1 2010-10-28 06:30:00 xqian Exp $
 */
public interface ResourceSet extends Closeable {

  /**
   * @return true if there is more record in this result set.
   * @throws OntoquestException if the result is corrupted and other errors.
   */
  public boolean next() throws OntoquestException;
  
  /**
   * Retrieves the meta data about this result set.
   * @return a ResourceSetMetaData object
   * @throws OntoquestException
   */
  public ResourceSetMetaData getMetaData() throws OntoquestException;
  
  public int getID();
  
  public boolean getBoolean(int columnIdx) throws OntoquestException;
  
  public double getDouble(int columnIdx) throws OntoquestException;
  
  public short getShort(int columnIdx) throws OntoquestException;
  
  public int getInt(int columnIdx) throws OntoquestException;
  
  public float getFloat(int columnIdx) throws OntoquestException;
  
  public long getLong(int columnIdx) throws OntoquestException;
  
  public String getString(int columnIdx) throws OntoquestException;
  
  public Object getObject(int columnIdx) throws OntoquestException;
}
