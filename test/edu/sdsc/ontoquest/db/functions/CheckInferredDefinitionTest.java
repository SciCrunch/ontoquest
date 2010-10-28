package edu.sdsc.ontoquest.db.functions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;
import junit.framework.TestCase;

/**
 * @version $Id: CheckInferredDefinitionTest.java,v 1.1 2010-10-28 06:30:08 xqian Exp $
 *
 */
public class CheckInferredDefinitionTest extends OntoquestTestAdapter {
  String kbName = "NIF";
  //String kbName = "BIRNLex";
  int kbid = -1;
  String[] terms = {"Gabaergic neuron", "Hippocampal neuron", "Neocortical neuron", 
      "Cerebellum neuron", "hippocampus"};
  int[][] termIDs = null;
  public void setUp() {
    super.setUp();
    try {
      // get IDs of terms
      kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
      ResourceSet rs = basicFunctions.searchAllIDsByName(terms, new int[]{kbid}, 
          true, true, context, varList3);
      LinkedList<int[]> termIDList = new LinkedList<int[]>();
//      termIDs = new int[terms.length][2];
//      int count = 0;
      while (rs.next()) {
        termIDList.add(new int[]{rs.getInt(1), rs.getInt(2)});
//        termIDs[count][0] = rs.getInt(1);
//        termIDs[count][1] = rs.getInt(2);
//        count++;
      }
      rs.close();
      termIDs = new int[termIDList.size()][2];
      termIDList.toArray(termIDs);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  public void testExecuteByTerms() throws OntoquestException {
    OntoquestFunction<ResourceSet> f = new CheckInferredDefinition(terms, kbid, true);
    ResourceSet rs = f.execute(context, varList3);
    int count = 0;
    while (rs.next()) {
      count++;
      System.out.println(rs.getInt(1) + ", " + rs.getInt(2) + ", " + rs.getString(3));
    }
    rs.close();
    assertEquals(count, 4);
  }
  
  public void testExecuteByIDs() throws OntoquestException {
    
    OntoquestFunction<ResourceSet> f = new CheckInferredDefinition(termIDs);
    ResourceSet rs = f.execute(context, varList3);
    int count = 0;
    while (rs.next()) {
      count++;
      System.out.println(rs.getInt(1) + ", " + rs.getInt(2) + ", " + rs.getString(3));
    }
    rs.close();
//    assertEquals(count, 4);
  }

  public void testExpandInferredTerms() throws OntoquestException {
    HashMap<int[], Set<NIFSimpleTerm>> results = expandInferredTerms(termIDs);
    for (int[] key : results.keySet()) {
      System.out.println("key -- " + key[0] + ", " + key[1] + "---------");
      for (NIFSimpleTerm term : results.get(key)) {
        System.out.println("subclass : " + term.getRid() + ", " + term.getRtid() + ", " + term.getLabel());
      }
    }
  }
  
  public HashMap<int[], Set<NIFSimpleTerm>> expandInferredTerms(int[][] termIds) throws OntoquestException {
    HashMap<int[], Set<NIFSimpleTerm>> results = new HashMap<int[], Set<NIFSimpleTerm>>(termIds.length);
    
    OntoquestFunction<ResourceSet> inferredClassFun = new CheckInferredDefinition(termIds); 
    ResourceSet rs = inferredClassFun.execute(context, varList3);
    while (rs.next()) {
      int rid = rs.getInt(1);
      int rtid = rs.getInt(2);
      HashSet<NIFSimpleTerm> subclassSet = new HashSet<NIFSimpleTerm>();
      // add original term
      subclassSet.add(new NIFSimpleTerm(rid, rtid, rs.getString(3)));
      OntoquestFunction<ResourceSet> f2 = new GetSubClasses(rid, rtid);
      ResourceSet rs2 = f2.execute(context, varList8);
      while (rs2.next()) {
        NIFSimpleTerm t = new NIFSimpleTerm(rs2.getInt(1), rs2.getInt(2), rs2.getString(3));
        subclassSet.add(t);
      }
      results.put(new int[]{rid, rtid}, subclassSet);
    }
    
    return results;
  }

}
