package edu.sdsc.ontoquest.query.plan;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.query.Parameter;
import edu.sdsc.ontoquest.query.XMLSerializable;

/**
 * @version $Id: Condition.java,v 1.1 2010-10-28 06:30:28 xqian Exp $
 *
 */
public class Condition implements XMLSerializable {
  private Parameter _leftOperand = null;
  private Parameter _rightOperand = null;
  private String _operator = null;
  /**
   * @return the _leftOperand
   */
  public Parameter getLeftOperand() {
    return _leftOperand;
  }
  /**
   * @param operand the _leftOperand to set
   */
  public void setLeftOperand(Parameter operand) {
    _leftOperand = operand;
  }
  /**
   * @return the _rightOperand
   */
  public Parameter getRightOperand() {
    return _rightOperand;
  }
  /**
   * @param operand the _rightOperand to set
   */
  public void setRightOperand(Parameter operand) {
    _rightOperand = operand;
  }
  /**
   * @return the _operator
   */
  public String getOperator() {
    return _operator;
  }
  /**
   * @param _operator the _operator to set
   */
  public void setOperator(String _operator) {
    this._operator = _operator;
  }
  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.query.XMLSerializable#toXMLString(java.lang.String)
   */
  public String toXMLString(String indent) throws OntoquestException {
    StringBuilder result = new StringBuilder();
    String newIndent = indent + "  ";
    // print start tag and its attributes
    result.append(indent).append('<').append("CONDITION");
    result.append(' ').append("operator").append("=\"");
    result.append(_operator).append("\">\n");
    
    // print operands
    result.append(_leftOperand.toXMLString(newIndent)).append('\n');
    result.append(_rightOperand.toXMLString(newIndent)).append('\n');


    // print end tag
    result.append(indent).append("</").append("CONDITION");
    result.append(">");

    return result.toString();
  }

}
