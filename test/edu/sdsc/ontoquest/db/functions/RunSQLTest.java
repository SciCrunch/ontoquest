package edu.sdsc.ontoquest.db.functions;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;

/**
 * @version $Id: RunSQLTest.java,v 1.1 2010-10-28 06:30:06 xqian Exp $
 *
 */
public class RunSQLTest extends OntoquestTestAdapter {
  public void testExecute() throws OntoquestException {
    OntoquestFunction<ResourceSet> f = new RunSQL("select id, 1 as rtid, name from primitiveclass limit 10");
    ResourceSet rs = f.execute(context, varList3);
    while (rs.next()) {
      System.out.println(rs.getInt(1) + ", " + rs.getInt(2) + ", " + rs.getString(3));
    }
    rs.close();
  }
}
