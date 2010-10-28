package edu.sdsc.ontoquest.query.plan;

import java.util.List;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.query.TranslatorHelper;

/**
 * @version $Id: LogicalExpr.java,v 1.1 2010-10-28 06:30:29 xqian Exp $
 *
 */
public class LogicalExpr extends Expression {
  private List<Expression> operands = null;
  public enum LogicalOperator { AND, OR, NOT };
  private LogicalOperator op = LogicalOperator.AND;
  
  public LogicalExpr (LogicalOperator op, List<Expression> operands) {
    this.op = op;
    this.operands = operands;
  }
  
  public LogicalOperator getOperator() {
    return op;
  }
  
  public List<Expression> getOperands() {
    return operands;
  }

  /* (non-Javadoc)
   * @see edu.sdsc.ontoquest.query.plan.PlanNode#checkTypes()
   */
  @Override
  public void checkTypes() throws OntoquestException {
    // TODO Auto-generated method stub
    
  }

  /**
   * @see edu.sdsc.ontoquest.query.XMLSerializable#toXMLString(java.lang.String)
   */
  public String toXMLString(String indent) throws OntoquestException {
    StringBuilder result = new StringBuilder();
    String newIndent = indent + "  ";
    
    // print start tag and attributes
    result.append(indent).append('<').append("LOGICAL_EXPR");
    result.append(" ").append("type").append("=\"");
    result.append(op).append("\">\n");
    
    // print the operands
    for (Expression e : getOperands()) {
      result.append(e.toXMLString(newIndent)).append('\n');
    }
    
    // print input, output, and context list
    printInputXMLString(result, newIndent);
    printOutputXMLString(result, newIndent);
    printContextXMLString(result, newIndent);

    // print end tag
    result.append(indent).append("</").append("LOGICAL_EXPR");
    result.append(">");
    return result.toString();
  }

  /**
   * @see edu.sdsc.ontoquest.query.plan.PlanNode#toSQL(edu.sdsc.ontoquest.query.TranslatorHelper, java.lang.Object)
   */
  @Override
  public String toBackendQuery(TranslatorHelper helper, Object o)
      throws OntoquestException {
    return helper.translateLogicalExpr(this, o);
  }
  
  
}
