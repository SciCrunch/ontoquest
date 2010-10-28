package edu.sdsc.ontoquest;


/**
 * @version $Id: AbstractResourceSet.java,v 1.1 2010-10-28 06:29:59 xqian Exp $
 *
 */
public abstract class AbstractResourceSet implements ResourceSet {
  private long _creationTime = -1;
  private int _id = -1;
  protected boolean _isClosed = false;
  private long _lifeSpan = 360; // 360 seconds by default
  
  private static int _idSerial = 0;
  protected void initialize() throws OntoquestException {
    _creationTime = System.currentTimeMillis();
    _id = generateID();
  }

  /**
   * @see edu.sdsc.ontoquest.Closeable#getCreateTime()
   */
  public long getCreationTime() {
    return _creationTime;
  }

  protected void setCreationTime(long timeInMillis) {
    _creationTime = timeInMillis;
  }
  
  /**
   * @see edu.sdsc.ontoquest.ResourceSet#getID()
   */
  public int getID() {
    return _id;
  }

  private static synchronized int generateID() {
    return ++_idSerial;
  }

  /**
   * @see edu.sdsc.ontoquest.ResourceSet#isClosed()
   */
  public boolean isClosed() {
    return _isClosed;
  }

  /**
   * @return the _lifeSpan
   */
  public long getLifeSpan() {
    return _lifeSpan;
  }

  /**
   * @param span the _lifeSpan to set
   */
  public void setLifeSpan(long span) {
    _lifeSpan = span;
  }

  /**
   * @see edu.sdsc.ontoquest.Closeable#isExpired()
   */
  public boolean isExpired() {
    return (System.currentTimeMillis()-getCreationTime()) > 1000*getLifeSpan();
  }

}
