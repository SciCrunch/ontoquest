package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.List;

import edu.sdsc.ontoquest.OntoquestTestAdapter;

/**
 * @version $Id: TermResourceTest.java,v 1.2 2012-04-30 22:45:03 xqian Exp $
 *
 */
public class TermResourceTest extends OntoquestTestAdapter {
  HashMap<String, Object> attributes = null;
  int kbid = 0;
  String kbName = "NIF";
  
  @Override
	public void setUp() {
    super.setUp();
    attributes = new HashMap<String, Object>();
    try {
    kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    } catch (Exception e) {
      // do nothing
    }
  }

  public void testGetTermsBeginWith() throws Exception {
    String term = "cereb";
    long time1 = System.currentTimeMillis();
    List<String> results1 = TermResource.getTermsBeginWith(term, attributes, kbid, context);
    long time2 = System.currentTimeMillis();
    System.out.println("Time for GetTermsBeginWith call (ms): " + (time2-time1));
    
    long time3 = System.currentTimeMillis();
    List<String> results2 = TermResource.getTermsBeginWith2(term, attributes, kbid, context);
    long time4 = System.currentTimeMillis();
    System.out.println("Time for GetTermsBeginWith2 call (ms): " + (time4-time3));
    
//    assertEquals(results1.size(), results2.size());
    System.out.println("======================= results1 size = " + results1.size());
    for (String s : results1) {
      System.out.println(s);
    }
    
    System.out.println("======================= results2 size = " + results2.size());
    for (String s : results2) {
      System.out.println(s);
    }

  }
}
