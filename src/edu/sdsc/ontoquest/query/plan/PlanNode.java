package edu.sdsc.ontoquest.query.plan;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.query.Parameter;
import edu.sdsc.ontoquest.query.TranslatorHelper;
import edu.sdsc.ontoquest.query.Utility;
import edu.sdsc.ontoquest.query.XMLSerializable;

/**
 * Top-level plan node.
 * @version $Id: PlanNode.java,v 1.1 2010-10-28 06:30:29 xqian Exp $
 *
 */
public abstract class PlanNode implements XMLSerializable {
  private List<Parameter> _outputList = null; // projections

  private List<Parameter> _inputList = null; // input parameters

  private Context _context = null; // query-dependent context
  
  /**
   * Checks if the data types are compatible in this node and its children.
   * Right now, we only handle types that can be automatically converted.
   * @return 
   * @throws OntoquestException if there is conflicts in type checking 
   * 
   */
  public abstract void checkTypes() throws OntoquestException ;

  public abstract String toBackendQuery(TranslatorHelper helper, Object o) throws OntoquestException;
  
  /**
   * @return the list of parameters to be projected. If there is no output for
   *         this node, NULL is returned.
   */
  public List<Parameter> getOutputList() {
    return _outputList;
  }

  /**
   * @param list
   *          The projection list to set.
   */
  public void setOutputList(List<Parameter> list) {
    _outputList = list;
  }

  /**
   * Return true if there is input parameter in the plan node.
   * Otherwise return false.
   * @return
   */
  public boolean hasOutputParameter() {
    return Utility.isBlank(_outputList);
  }

  /**
   * @return the list of parameters which must have been bound. If there is no
   *         input for this plan node, NULL is returned.
   */
  public List<Parameter> getInputList() {
    return _inputList;
  }

  /**
   * @param list
   *          The input list to set.
   */
  public void setInputList(List<Parameter> list) {
    _inputList = list;
  }

  /**
   * Return true if there is input parameter in the plan node.
   * Otherwise return false.
   * @return
   */
  public boolean hasInputParameter() {
    return Utility.isBlank(_inputList);
  }

  /**
   * Retrieve the query-dependent context.
   * @return
   */
  public Context getContext() {
    return _context;
  }
  
  public void setContext(Context context) {
    _context = context;
  }
  
  /**
   * Return true if there is context-dependent parameter associated with the plan node.
   * Otherwise return false.
   * @return
   */
  public boolean hasContext() {
    return _context == null;
  }

  protected void printInputXMLString(StringBuilder result, String indent) 
      throws OntoquestException {
    String newIndent = indent + "  ";
    
    // print input list
    result.append(indent).append('<').append("INPUT").append(">\n");
    if (!Utility.isBlank(getInputList())) {
      for (Parameter in : getInputList()) {
        result.append(in.toXMLString(newIndent)).append('\n');
      }
    }
    result.append(indent).append("</").append("INPUT").append(">\n");
  }

  protected void printOutputXMLString(StringBuilder result, String indent)
      throws OntoquestException {
    String newIndent = indent + "  ";

    // print input list
    result.append(indent).append('<').append("OUTPUT")
        .append(">\n");
    if (!Utility.isBlank(getOutputList())) {
      for (Parameter out : getOutputList()) {
        result.append(out.toXMLString(newIndent)).append('\n');
      }
    }
    result.append(indent).append("</").append("OUTPUT")
        .append(">\n");
  }

  protected void printContextXMLString(StringBuilder result, String indent)
      throws OntoquestException {
    String newIndent = indent + "  ";

    // print input list
    result.append(indent).append('<').append("CONTEXT").append(">\n");
    if (hasContext()) {
        result.append(getContext().toXMLString(newIndent)).append('\n');
    }
    result.append(indent).append("</").append("CONTEXT").append(">\n");
  }
}