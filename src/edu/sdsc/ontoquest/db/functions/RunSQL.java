package edu.sdsc.ontoquest.db.functions;

import java.util.List;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Variable;

/**
 * A SQL wrapper which provides a way for external application to run a select SQL directly 
 * against the database.
 * 
 * @version $Id: RunSQL.java,v 1.2 2012-04-30 22:42:03 xqian Exp $
 *
 */
public class RunSQL implements OntoquestFunction<ResourceSet> {

  private String sql;
  
  private int resultLimit ;
  
  public RunSQL(String sql) {
    this.sql = sql;
    resultLimit = -1;
  }
  
  /**
   * @see edu.sdsc.ontoquest.OntoquestFunction#execute(edu.sdsc.ontoquest.Context, java.util.List)
   */
  @Override
  public ResourceSet execute(Context context, List<Variable> varList)
      throws OntoquestException {
    return DbUtility.executeSQLQuery(sql, context, varList, new String[]{}, 
        "Failed to run user-supplied SQL query.", resultLimit);
  }

}
