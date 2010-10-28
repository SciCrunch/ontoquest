package edu.sdsc.ontoquest.query.plan;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.query.Constant;
import edu.sdsc.ontoquest.query.Parameter;
import edu.sdsc.ontoquest.query.TranslatorHelper;
import edu.sdsc.ontoquest.query.Variable;

/**
 * @version $Id: Predicate.java,v 1.1 2010-10-28 06:30:29 xqian Exp $
 *
 */
public class Predicate extends Expression {
  public enum AccessPattern {VVC, VCV, CVV, CCV, CVC, VCC, VVV, CCC};

  private Parameter subject = null;
  private Parameter property = null;
  private Parameter object = null;  
  private Condition condition = null;
  
  private AccessPattern accessPattern = null;

  /**
   * @return the accessPattern
   */
  public AccessPattern getAccessPattern() {
    return accessPattern;
  }
  
  public Predicate(Condition c, Parameter subject, 
                        Parameter property, Parameter object) {
    this.subject = subject;
    this.property = property;
    this.object = object;
    condition = c;
  }
  
  protected void setAccessPattern() {
    if (subject == null || property == null || object == null)
      return;
    
    if (subject instanceof Variable && property instanceof Variable && object instanceof Constant) {
      accessPattern = AccessPattern.VVC;
    } else if (subject instanceof Variable && property instanceof Constant && object instanceof Variable) {
      accessPattern = AccessPattern.VCV;
    } else if (subject instanceof Constant && property instanceof Variable && object instanceof Variable) {
      accessPattern = AccessPattern.CVV;
    } else if (subject instanceof Constant && property instanceof Constant && object instanceof Variable) {
      accessPattern = AccessPattern.CCV;
    } else if (subject instanceof Constant && property instanceof Variable && object instanceof Constant) {
      accessPattern = AccessPattern.CVC;
    } else if (subject instanceof Variable && property instanceof Constant && object instanceof Constant) {
      accessPattern = AccessPattern.VCC;
    } else if (subject instanceof Variable && property instanceof Variable && object instanceof Variable) {
      accessPattern = AccessPattern.VVV;
    } else if (subject instanceof Constant && property instanceof Constant && object instanceof Constant) {
      accessPattern = AccessPattern.CCC;
    }
  }
  
  /**
   * @return the condition
   */
  public Condition getCondition() {
    return condition;
  }
  /**
   * @param condition the condition to set
   */
  public void setCondition(Condition condition) {
    this.condition = condition;
  }
  /**
   * @return the subject
   */
  public Parameter getSubject() {
    return subject;
  }
  /**
   * @param subject the subject to set
   */
  public void setSubject(Parameter subject) {
    this.subject = subject;
  }
  /**
   * @return the property
   */
  public Parameter getProperty() {
    return property;
  }
  /**
   * @param property the property to set
   */
  public void setProperty(Parameter property) {
    this.property = property;
  }
  /**
   * @return the object
   */
  public Parameter getObject() {
    return object;
  }
  /**
   * @param object the object to set
   */
  public void setObject(Parameter object) {
    this.object = object;
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
    String indent2 = indent + "  ";
    String newIndent = indent2 + "  ";
    
    // print start tag and its attributes
    result.append(indent).append('<').append("PREDICATE").append(">\n");
    // subject tag
    result.append(indent2).append('<').append("SUBJECT").append(">\n");
    result.append(getSubject().toXMLString(newIndent)).append('\n');
    result.append(indent2).append("</").append("SUBJECT").append(">\n");
    
    // property tag
    result.append(indent2).append('<').append("PROPERTY").append(">\n");
    result.append(getProperty().toXMLString(newIndent)).append('\n');
    result.append(indent2).append("</").append("PROPERTY").append(">\n");
    
    // object tag
    result.append(indent2).append('<').append("OBJECT").append(">\n");
    result.append(getObject().toXMLString(newIndent)).append('\n');
    result.append(indent2).append("</").append("OBJECT").append(">\n");
    
    // condition tag
    result.append(indent2).append('<').append("CONDITION").append(">\n");
    result.append(getCondition().toXMLString(newIndent)).append('\n');
    result.append(indent2).append("</").append("CONDITION").append(">\n");
    
    // print input, output, and context list
    printInputXMLString(result, newIndent);
    printOutputXMLString(result, newIndent);
    printContextXMLString(result, newIndent);

    // print end tag
    result.append(indent).append("</").append("PREDICATE").append('>');
    return result.toString();
  }

  /**
   * @see edu.sdsc.ontoquest.query.plan.PlanNode#toSQL(edu.sdsc.ontoquest.query.TranslatorHelper, java.lang.Object)
   */
  @Override
  public String toBackendQuery(TranslatorHelper helper, Object o)
      throws OntoquestException {
    return helper.translatePredicate(this, o);
  }
  
  
}
