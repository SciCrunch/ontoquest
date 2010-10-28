package edu.sdsc.ontoquest.query;

import edu.sdsc.ontoquest.OntoquestException;

/**
 * @version $Id: Constant.java,v 1.1 2010-10-28 06:30:31 xqian Exp $
 *
 */
public class Constant implements Parameter {
  String _value = null;
  public Constant(String value) {
    this._value = value;
  }
  /**
   * @return the value
   */
  public String getValue() {
    return _value;
  }
  /**
   * @param value the value to set
   */
  public void setValue(String value) {
    this._value = value;
  }
  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.query.XMLSerializable#toXMLString(java.lang.String)
   */
  public String toXMLString(String indent) throws OntoquestException {
    StringBuilder result = new StringBuilder();
    result.append(indent);
    result.append('<');
    result.append("CONSTANT").append(' ');
    result.append("VALUE");
    result.append("=\"");
    result.append(getValue());
    result.append("\"/>");
    return result.toString();
  }

}
