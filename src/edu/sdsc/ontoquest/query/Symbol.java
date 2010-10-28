package edu.sdsc.ontoquest.query;

import edu.sdsc.ontoquest.query.plan.PlanNode;

/**
 * @version $Id: Symbol.java,v 1.1 2010-10-28 06:30:33 xqian Exp $
 *
 */
public class Symbol {
  private String _name = null;
  private PlanNode _node = null;
  private Variable _var = null;
  public static final String NULL_VAR = "_NULL_";
  
  /**
   * Construct a symbol that maps a variable in the plan to a variable
   * used in the back-end query.
   * @param name the variable name used in the query sent to back-end database. 
   * @param node the plan node in which the <code>var</code> appears.
   * @param var the corresponding variable in the plan.
   */
  public Symbol(String name, PlanNode node, Variable var) {
    _name = name;
    _node = node;
    _var = var;
  }
  
  public String getName() {
    return _name;
  }
  
  public PlanNode getNode() {
    return _node;
  }
  
  public Variable getVariable() {
    return _var;
  }
}
