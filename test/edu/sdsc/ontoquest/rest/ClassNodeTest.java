package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.Set;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.rest.BaseBean.SiblingsType;

/**
 * @version $Id: ClassNodeTest.java,v 1.2 2011-07-12 18:00:00 xqian Exp $
 *
 */
public class ClassNodeTest extends OntoquestTestAdapter {
  HashMap<String, Object> attributes = null;
  String kbName = "NIF";
  int kbId = -1;
  
  @Override
	public void setUp() {
    super.setUp();
    attributes = new HashMap<String, Object>();
    try {
    kbId = basicFunctions.getKnowledgeBaseID(kbName, context);
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  public void testGet() throws OntoquestException {
    String id = "316155-1;316057-1;316164-1;357481-1;357480-1";
    Set<ClassNode> classNodes = ClassNode.get(id, context);
    for (ClassNode c : classNodes) {
      System.out.println("class label -- " + c.getLabel());
//      assertEquals(c.getId(), id);
    }
  }

  public void testGetByLabel() throws OntoquestException {
    String name = "Cerebellum";
    Set<ClassNode> classNodes = ClassNode.getByLabel(name, kbId, context);
    for (ClassNode c : classNodes) {
      System.out.println(c.getLabel() + "; " + c.getName());
//      assertEquals(c.getLabel(), name);
    }    
  }
  
  public void testGetByName() throws OntoquestException {
    String name = "sao436474611;GO_0009179;birnlex_1146;birnlex_1566;birnlex_1489;birnlex_1118;birnlex_1567;birnlex_911";
    Set<ClassNode> classNodes = ClassNode.getByName(name, kbId, context);
    for (ClassNode c : classNodes) {
      System.out.println(c.getLabel() + "; " + c.getName());
//      assertEquals(c.getName(), name);
    }    
  }
  
  public void testGetSiblings() throws OntoquestException {
    Set<ClassNode> classNodes = null;
//    classNodes= ClassNode.getSiblings("Cerebellum", SiblingsType.CLASSES, kbId, 
//        attributes, BaseBean.InputType.TERM, context);
    classNodes= ClassNode.getSiblings("birnlex_1566", SiblingsType.CLASSES, kbId, 
        attributes, BaseBean.InputType.NAME, context);
    assertNotNull(classNodes);
    assertTrue(classNodes.size() > 0);
    for (ClassNode c : classNodes) {
      System.out.println(c.getLabel() + "; " + c.getName());
    }
  }

  public void testSearch() throws OntoquestException {
    String term = "Cerebellum";
    Set<ClassNode> classNodes = ClassNode.search(term, attributes, kbId, context);
    for (ClassNode c : classNodes) {
      System.out.println(c.getLabel() + "; " + c.getName());
//      assertEquals(c.getLabel(), name);
    }    
  }
}
