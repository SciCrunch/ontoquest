package edu.sdsc.ontoquest.query;

import edu.sdsc.ontoquest.OntoquestException;

/**
 * @version $Id: Variable.java,v 1.1 2010-10-28 06:30:30 xqian Exp $
 *
 */
public class Variable implements Parameter {
//  private int _id = -1;
  private String _name = "";
  private int _datatypeID = -1;
  private static int _serialID = 1;
  
  public Variable(int datatypeID) {
    // auto generate a name
    _name = generateName();
    setDataTypeID(datatypeID);
  }
  
  public Variable(String name, int datatypeID) {
    _name = name;
    setDataTypeID(datatypeID);
  }
  
  public String getName() {
    return _name;
  }
  
  public int getDataTypeID() {
    return _datatypeID;
  }
  
  public void setDataTypeID(int datatypeID) {
    _datatypeID = datatypeID;
  }
  
  private String generateName() {
    return "_v"+(_serialID++);
  }

  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.query.XMLSerializable#toXMLString(java.lang.String)
   */
  public String toXMLString(String indent) throws OntoquestException {
    StringBuilder result = new StringBuilder();
    result.append(indent);
    result.append("<");
    result.append("VARIABLE");
    result.append(" ");
    result.append("name");
    result.append("=\"");
    result.append(_name);
    result.append("\" ");
    result.append("type");
    result.append("=\"");
    result.append(getDataTypeID());
    result.append("\"/>");
    return result.toString();
  }
}
