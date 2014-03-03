package edu.sdsc.ontoquest.rest;

import java.util.HashMap;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;
import junit.framework.TestCase;

/**
 * @version $Id: OntGraphTest.java,v 1.1 2010-10-28 06:30:37 xqian Exp $
 *
 */
public class OntGraphTest extends OntoquestTestAdapter {
  
  HashMap<String, Object> attributes = null;
  int kbid = 0;
  String kbName = "NIF";
  
  public void setUp() {
    super.setUp();
    attributes = new HashMap<String, Object>();
    try {
    kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    } catch (Exception e) {
      // do nothing
    }
  }
  
  public void testGetSubclasses() throws OntoquestException {
    String id = "316108-1";
    attributes.put("level", 3);
    OntGraph g = OntGraph.get(id, OntGraph.NeighborType.SUBCLASSES, kbid, attributes, OntGraph.InputType.OID,context,1000,true);
//    ClassNode node = ClassNode.get(id, context);
//    assertTrue(g.getNodes().size() > 0);
    assertTrue(g.getEdges().size() > 0); 
    for (Relationship r : g.getEdges()) {
      System.out.println(r.getLabel1() + " " + r.getPname() + " " + r.getLabel2());
    }
  }

  public void testGetSubclasses2() throws OntoquestException {
//    String name = "CHEBI_23367";
    String name = "PRO_000000001";
    attributes.put("level", 0);
    long time1 = System.currentTimeMillis();
    OntGraph g = OntGraph.get(name, OntGraph.NeighborType.SUBCLASSES, kbid, attributes, OntGraph.InputType.NAME,context,100,true);
//    ClassNode node = ClassNode.get(id, context);
//    assertTrue(g.getNodes().size() > 0);
    assertTrue(g.getEdges().size() > 0); 
    for (Relationship r : g.getEdges()) {
      System.out.println('"'+r.getLabel1()+'"' + "," + r.getPname() + "," + '"'+r.getLabel2()+'"');
    }
    long time2 = System.currentTimeMillis();
    System.out.println("Total time (sec): " + (time2-time1)/1000);
  }

  public void testGetSuperclasses() throws OntoquestException {
    attributes.put("level", 3);
    OntGraph g = OntGraph.get("cerebellum", OntGraph.NeighborType.SUPERCLASSES, kbid, attributes, OntGraph.InputType.TERM,context,1000,true);
//    ClassNode node = ClassNode.get(id, context);
//    assertTrue(g.getNodes().size() > 0);
    assertTrue(g.getEdges().size() > 0); 
    for (Relationship r : g.getEdges()) {
      System.out.println(r.getLabel1() + " " + r.getPname() + " " + r.getLabel2());
    }
  }

  public void testGetParts() throws OntoquestException {
    String name = "birnlex_1489";
    attributes.put("level", 1);
    OntGraph g = OntGraph.get(name, OntGraph.NeighborType.PARTS, kbid, attributes, OntGraph.InputType.NAME, context,1000,true);
//    assertTrue(g.getNodes().size() > 0);
    assertTrue(g.getEdges().size() > 0);
    for (Relationship r : g.getEdges()) {
      System.out.println(r.getLabel1() + " " + r.getPname() + " " + r.getLabel2());
    }
  }
  
  public void testGetParentsByID() throws OntoquestException {
//    String name = "birnlex_1489";
    String name = "sao436474611";
    attributes.put("level", 3);
    ResourceSet rs = basicFunctions.searchAllIDsByName(name, new int[]{kbid}, context, varList3);
    while (rs.next()) {
      int rid = rs.getInt(1);
      int rtid = rs.getInt(2);
      String id = rid + "-" + rtid;
      OntGraph g = OntGraph.get(id, OntGraph.NeighborType.PARENTS, kbid, attributes, 
                                OntGraph.InputType.OID, context,1000,true);
//      assertTrue(g.getNodes().size() > 0);
      for (Relationship r : g.getEdges()) {
        System.out.println(r.getLabel1() + " " + r.getPname() + " " + r.getLabel2());
      }
    }
    rs.close();
  }

  public void testGetWhole() throws OntoquestException {
    String name = "birnlex_1146";
    attributes.put("level", 3);
    OntGraph g = OntGraph.get(name, OntGraph.NeighborType.WHOLE, kbid, attributes, OntGraph.InputType.NAME, context,1000,true);
//    assertTrue(g.getNodes().size() > 0);
    assertTrue(g.getEdges().size() > 0);
    for (Relationship r : g.getEdges()) {
      System.out.println(r.getLabel1() + " " + r.getPname() + " " + r.getLabel2());
    }
  }

}
