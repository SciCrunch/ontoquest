package edu.sdsc.ontoquest.db.functions;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;

/**
 * @version $Id: GetDagPathTest.java,v 1.1 2010-10-28 06:30:04 xqian Exp $
 */
public class GetDagPathTest extends OntoquestTestAdapter {
  OntoquestFunction<ResourceSet> f = null;
  int rid1=8156, rtid1=1, rid2=8209, rtid2=1, pid = 2814;
  
  public void testExecute() throws OntoquestException {
    OntoquestFunction<ResourceSet> f = new GetDagPath(rid1, rtid1, rid2, rtid2, pid, true, true);
    ResourceSet rs = f.execute(context, varList6);
    while (rs.next()) {
      System.out.println(rs.getInt(1)+" "+rs.getInt(2)+" "+rs.getString(3)+
          " "+rs.getInt(4)+" "+rs.getInt(5)+" "+rs.getString(6));
    }
    rs.close();
  }
}
