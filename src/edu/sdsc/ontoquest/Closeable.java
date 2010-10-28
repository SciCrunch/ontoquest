/**
 * 
 */
package edu.sdsc.ontoquest;

/**
 * Any class implements this interface is a resource which can be
 * closed by resource manager.
 * @version $Id: Closeable.java,v 1.1 2010-10-28 06:30:02 xqian Exp $
 *
 */
public interface Closeable {
  public void close() throws OntoquestException;
  
  /**
   * Gets the expected life span of this closeable resource. The resource 
   * will be closed once it reaches its life span.  
   * @return life span in seconds.
   */
  public long getLifeSpan();
  
  /**
   * Gets the creation time of the instance in milliseconds.
   * @return
   */
  public long getCreationTime();
  
  public boolean isClosed();
  
  public boolean isExpired();
  
}
