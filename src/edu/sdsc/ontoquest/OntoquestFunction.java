package edu.sdsc.ontoquest;

import edu.sdsc.ontoquest.query.Variable;

/**
 * An abstract function as the extension point of customized functions.
 * @version $Id: OntoquestFunction.java,v 1.1 2010-10-28 06:30:01 xqian Exp $
 *
 */
public interface OntoquestFunction<T> {
  
  public T execute(Context context, java.util.List<Variable> varList) throws OntoquestException;
  
}
