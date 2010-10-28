package edu.sdsc.ontoquest.db.functions;

import java.util.LinkedList;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;

/**
 * @version $Id: GetOntologyURLTest.java,v 1.1 2010-10-28 06:30:05 xqian Exp $
 *
 */
public class GetOntologyURLTest extends OntoquestTestAdapter {
  String kbName = "NIF"; //"pizza";
  String names[] = {"Cerebellum", "Ammon's horn", "brain", "neuron", "nosuchterm"};
  
  public void testURL() throws OntoquestException {
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    System.out.println("======== Get ID of terms ");
    ResourceSet rs = basicFunctions.searchAllIDsByName(names, new int[]{kbid}, true, false, context, varList3);
    LinkedList<int[]> idList = new LinkedList<int[]>();
    while (rs.next()) {
      idList.add(new int[]{rs.getInt(1), rs.getInt(2)});
    }
    rs.close();

    int[][] idArray = new int[idList.size()][2];
    idList.toArray(idArray);
    
    GetOntologyURL f = new GetOntologyURL(idArray);
    ResourceSet rs2 = f.execute(context, varList3);
    while (rs2.next()) {
      System.out.println(rs2.getInt(1)+", " + rs2.getInt(2)+", "+rs2.getString(3));
    }
  }
}
