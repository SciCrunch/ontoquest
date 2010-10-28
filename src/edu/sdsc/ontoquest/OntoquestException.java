package edu.sdsc.ontoquest;

/**
 * Exception used by ontoquest classes
 * @version $Id: OntoquestException.java,v 1.1 2010-10-28 06:30:00 xqian Exp $
 *
 */
public class OntoquestException extends Exception {

  private static final long serialVersionUID = -8262580154459060699L;
  
  public enum Type {UNDEFINED, QUERY, PLANNER, PARSER, BACKEND, EXECUTOR, INPUT, SETTING};
  
  private Type _type = Type.UNDEFINED;
  
  public OntoquestException() {
    super();
  }
  
  public OntoquestException(String message) {
    this(Type.UNDEFINED, message);
  }
  
  public OntoquestException(Type type, String message) {
    super(message);
    this._type = type;
  }
  
  public OntoquestException(Type type, Throwable t) {
    super(t.getMessage(), t);
    _type = type;
  }
  
  public OntoquestException(Type type, String message, Throwable t) {
    super((message==null)?t.getMessage():(message+" "+t.getMessage()), t);
    _type = type;
  }
  
  public Type getType() {
    return _type;
  }
}
