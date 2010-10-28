package edu.sdsc.ontoquest.db.functions;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;
import junit.framework.TestCase;

/**
 * @version $Id: GetDagLCATest.java,v 1.1 2010-10-28 06:30:04 xqian Exp $
 *
 */
public class GetDagLCATest extends OntoquestTestAdapter {
  OntoquestFunction<ResourceSet> f = null;
  String kb = "NIF";
  
  public void testExecute() throws OntoquestException {
    String[] terms = {};
    String property = "subClassOf";
    // NOTE: MUST CHECK DATABASE TO CHOOSE nodeIDs and pid for testing!!
    int[][] nodeIDs = new int[terms.length][2];
    int kbid = basicFunctions.getKnowledgeBaseID(kb, context);
    ResourceSet rs1 = basicFunctions.searchAllIDsByName(
                      property, new int[]{kbid}, context, varList3);
    int pid = 0;
    if (rs1.next()) {
      pid = rs1.getInt(1);
    } else {
      fail("Property '" + property + "' not found in ontology " + kb);
    }
    
    int count = 0;
    for (String term : terms) {
      rs1 = basicFunctions.searchAllIDsByName(term, new int[]{kbid}, context, varList2);
      if (rs1.next()) {
        nodeIDs[count++] = new int[]{rs1.getInt(1), rs1.getInt(2)};
      } else {
        fail("Term '" + term + "' not found in ontology " + kb);
      }
    }
    
    OntoquestFunction<ResourceSet> f = new GetDagLCA(nodeIDs, pid, true, true);
    ResourceSet rs = f.execute(context, varList2);
    while (rs.next()) {
      System.out.println(rs.getInt(1)+" "+rs.getInt(2));
    }
    rs.close();
  }
  
}
