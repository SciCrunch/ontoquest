package edu.sdsc.ontoquest.query;

import edu.sdsc.ontoquest.OntoquestException;

/**
 * @version $Id: XMLSerializable.java,v 1.1 2010-10-28 06:30:32 xqian Exp $
 *
 */
public interface XMLSerializable {
  /**
   * Output the node content to an XML string for debug purpose.
   * 
   * @param indent
   * @return a string representation
   * @throws OntoquestException
   */
  public String toXMLString(String indent) throws OntoquestException;

}
