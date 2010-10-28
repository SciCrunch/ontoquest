package edu.sdsc.ontoquest.db.functions;

import java.util.ArrayList;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;
import junit.framework.TestCase;

/**
 * @version $Id: GetSuperClassesTest.java,v 1.1 2010-10-28 06:30:03 xqian Exp $
 *
 */
public class GetSuperClassesTest extends OntoquestTestAdapter {
//  String kbName = "pizza";
//  String[] names = {"Pizza", "PineKernels", "Rosa"};
  String kbName = "NIF";
  int kbid;
  String[] names = {"Cerebellar cortex", "purkinje Cell"};
  ArrayList<int[]> nodeIDs = new ArrayList<int[]>();
  
  public void setUp() {
    super.setUp();
    try {
      // get IDs of terms
      kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
      for (int i = 0; i<names.length; i++) {
        ResourceSet rs = basicFunctions.searchAllIDsByName(
            names[i], new int[]{kbid}, context, varList3);
        while (rs.next()) {
          nodeIDs.add(new int[]{rs.getInt(1), rs.getInt(2)});
        }
        rs.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
  public void testExecute() throws OntoquestException {
    nodeIDs.add(new int[]{649226, 1});
    for (int[] nodeID : nodeIDs) {
      OntoquestFunction<ResourceSet> f = new GetSuperClasses(nodeID[0], nodeID[1], kbid, 0, false);
      System.out.println("Find superclasses of node (rid, rtid): " + nodeID[0] + ", " + nodeID[1]);
      ResourceSet rs = f.execute(context, varList8);
      while (rs.next()) {
        System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
            +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
            +", " + rs.getInt(7)+", "+rs.getString(8));
      }
      rs.close();
    }
  }
}
