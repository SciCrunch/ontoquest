package edu.sdsc.ontoquest.db.functions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.functions.GetNeighbors.PropertyType;
import edu.sdsc.ontoquest.rest.ClassNode;
import edu.sdsc.ontoquest.rest.Relationship;
import junit.framework.TestCase;

/**
 * @version $Id: GetNeighborsTest.java,v 1.1 2010-10-28 06:30:06 xqian Exp $
 *
 */
public class GetNeighborsTest extends OntoquestTestAdapter {
  String kbName = "NIF";
//  String kbName = "BIRNLex";
  int kbid = -1;
  public void setUp() {
    super.setUp();
    try {
      // get IDs of terms
      kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  public void testExecute() throws OntoquestException {
    boolean isSynonymIncluded = true;
    String[] names = {"Ammon's horn"};
    String[] includedProperties = new String[]{"subClassOf"};
    String[] excludedProperties = null;
//    run(names, includedProperties, excludedProperties, isSynonymIncluded);
    System.out.println("====================");
    includedProperties = null;
    excludedProperties = new String[]{"disjointWith"};
//    run(names, includedProperties, excludedProperties, isSynonymIncluded);
    System.out.println("====================");
    names = new String[]{"Mouse", "Purkinje Cell"};
//    run(names, includedProperties, excludedProperties, isSynonymIncluded);
    System.out.println("====================");
    names = new String[]{"Regional Part Of Cell"};
    includedProperties = new String[]{"nifID"};
    isSynonymIncluded = true;
    excludedProperties = null;
//    run(names, includedProperties, excludedProperties, isSynonymIncluded, 
//        GetNeighbors.EDGE_OUTGOING, true, false);
    System.out.println("====================");
    names = new String[]{"Caudal anterior cingulate cortex"};
    includedProperties = new String[]{"subClassOf"};
    isSynonymIncluded = true;
    excludedProperties = null;
//    run(names, includedProperties, excludedProperties, isSynonymIncluded, 
//        GetNeighbors.EDGE_OUTGOING, true, false);
    System.out.println("====================");
    names = new String[]{"turn"};
    includedProperties = new String[]{"definition", "description", "externallySourcedDefinition", 
        "birnlexDefinition", "birnlexComment", "comment", "tempDefinition"};
    isSynonymIncluded = true;
    excludedProperties = null;
//    run(names, includedProperties, excludedProperties, isSynonymIncluded, 
//        GetNeighbors.EDGE_OUTGOING, true, false);
    System.out.println("====================");
    names = new String[]{"purkinje cell"};
    includedProperties = new String[]{"prefLabel", "label", "synonym", "abbrev", 
        "hasExactSynonym", "hasRelatedSynonym", "acronym"};
    isSynonymIncluded = true;
    excludedProperties = null;
//    run(names, includedProperties, excludedProperties, isSynonymIncluded, 
//        GetNeighbors.EDGE_OUTGOING, true, false);
    System.out.println("====================");
    names = new String[]{"purkinje cell", "neuron"};
    includedProperties = new String[]{"definition", "description", "externallySourcedDefinition", 
        "birnlexDefinition", "birnlexComment", "comment", "tempDefinition"
        ,"prefLabel", "label", "synonym", "abbrev", 
        "hasExactSynonym", "hasRelatedSynonym", "acronym"};
    isSynonymIncluded = true;
    excludedProperties = null;
//    for (String name : names) {
//      ResourceSet rs = basicFunctions.searchName(name, context, varList3);
      run(130320, 1, new String[]{"has_part"}, null, isSynonymIncluded, 
        GetNeighbors.EDGE_OUTGOING, true, false);
//    }
  }

  private void run(String[] names, String[] includedProperties, String[] excludedProperties,
      boolean isSynonymIncluded) throws OntoquestException {
    OntoquestFunction<ResourceSet> f = new GetNeighbors(
        names, kbid, includedProperties, excludedProperties, 
        isSynonymIncluded, GetNeighbors.EDGE_BOTH, 1, true);
    ResourceSet rs = f.execute(context, varList8);
    while (rs.next()) {
      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
          +", " + rs.getInt(7)+", "+rs.getString(8));
    }
    rs.close();
  }

  private void run(String[] names, String[] includedProperties, String[] excludedProperties,
      boolean isSynonymIncluded, int edgeDirection, boolean excludeHiddenRelationship, boolean isClassOnly) throws OntoquestException {
    OntoquestFunction<ResourceSet> f = new GetNeighbors(
        names, kbid, includedProperties, excludedProperties, 
        isSynonymIncluded, edgeDirection, excludeHiddenRelationship, isClassOnly, 1, false);
    ResourceSet rs = f.execute(context, varList8);
    while (rs.next()) {
      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
          +", " + rs.getInt(7)+", "+rs.getString(8));
    }
    rs.close();
  }

  private void run(int rid, int rtid, 
      String[] includedProperties, String[] excludedProperties,
      boolean isSynonymIncluded, int edgeDirection, boolean excludeHiddenRelationship, 
      boolean isClassOnly) throws OntoquestException {
    long time1 = System.currentTimeMillis();
    OntoquestFunction<ResourceSet> f = new GetNeighbors(rid, rtid, kbid, 
        includedProperties, excludedProperties, edgeDirection,
        excludeHiddenRelationship, isClassOnly, 0, true);
    ResourceSet rs = f.execute(context, varList8);
    while (rs.next()) {
      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
          +", " + rs.getInt(7)+", "+rs.getString(8));
    }
    rs.close();
    long time2 = System.currentTimeMillis();
    System.out.println("Time (ms): " + (time2-time1));
    
  }
 
  public void testOntology() throws OntoquestException {
    run(442, 18, null, null, true, GetNeighbors.EDGE_OUTGOING, true, false);
  }
  
  public void testExecute2() throws OntoquestException {
//    String[] terms = new String[]{"CA3"};
    String[] includedProperties = {"has_part"};
    boolean isSynonymIncluded = true;
    String[] excludedProperties = {"disjointWith"};
    OntoquestFunction<ResourceSet> f = new GetNeighbors(
        "CA1", kbid, includedProperties, null, 
        true, GetNeighbors.EDGE_OUTGOING, 1, true);
    ResourceSet rs = f.execute(context, varList8);
    int count = 0;
    while (rs.next()) {
      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
          +", " + rs.getInt(7)+", "+rs.getString(8));
      count++;
    }
    rs.close();
    System.out.println("====================");
  }
  
  public void testGetNeighborsByIDList() throws OntoquestException {
    long time1 = System.currentTimeMillis();
    String[] includedProperties = new String[]{"definition", "description", "externallySourcedDefinition", 
        "birnlexDefinition", "birnlexComment", "comment", "tempDefinition"
        ,"prefLabel", "label", "synonym", "abbrev", 
        "hasExactSynonym", "hasRelatedSynonym", "acronym"};
    String[] excludedProperties = null;
    int[][] idArray = new int[][]{{35871, 1}, {37391, 1}, {34988, 1}};
    OntoquestFunction<ResourceSet> f = new GetNeighbors(idArray, kbid, 
        includedProperties, excludedProperties, GetNeighbors.EDGE_BOTH,
        true, false, 1, false);
    ResourceSet rs = f.execute(context, varList8);
    while (rs.next()) {
      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
          +", " + rs.getInt(7)+", "+rs.getString(8));
    }
    rs.close();
    long time2 = System.currentTimeMillis();
    System.out.println("Time (ms): " + (time2-time1));  
  }

  public void testGetMultiLevelNeighborsByLabel() throws OntoquestException {
    long time1 = System.currentTimeMillis();
    String[] includedProperties = new String[]{"subClassOf", "has_part"};
    String[] terms = {"Neurons which activate muscle cells (MSH)."};
//    OntoquestFunction<ResourceSet> f = new GetNeighbors(terms, kbid, 
//        null, null, true, GetNeighbors.EDGE_BOTH,
//        true, true, 1, false);
    OntoquestFunction<ResourceSet> f = new GetNeighbors(terms, kbid, 
        includedProperties, null, true, GetNeighbors.EDGE_BOTH,
        true, true, 1, true);
    ResourceSet rs = f.execute(context, varList8);
    int count = 0;
    while (rs.next()) {
      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
          +", " + rs.getInt(7)+", "+rs.getString(8));
      count++;
    }
    rs.close();
    long time2 = System.currentTimeMillis();
    System.out.println("Count : " + count);
    System.out.println("Time (ms): " + (time2-time1));  
  }

//  public void testGetNeighborsByIDArray2() throws OntoquestException {
//    long time1 = System.currentTimeMillis();
//    String[] includedProperties = new String[]{"subClassOf", "has_part"};
//    String[] excludedProperties = new String[]{"subClassOf", "disjointWith"};
//    int[][] idArray = new int[][]{{35871, 1}, {37391, 1}, {34988, 1}};
//    OntoquestFunction<ResourceSet> f = new GetNeighbors(idArray, kbid, 
//        null, excludedProperties, GetNeighbors.EDGE_INCOMING, true,
//        true, 1, true);
//    ResourceSet rs = f.execute(context, varList8);
//    int count = 0;
//    while (rs.next()) {
//      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
//          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
//          +", " + rs.getInt(7)+", "+rs.getString(8));
//      count++;
//    }
//    rs.close();
//    long time2 = System.currentTimeMillis();
//    System.out.println("Count : " + count);
//    System.out.println("Time (ms): " + (time2-time1)); 
//    
//  }

  public void testGetNeighborsWithPropType() throws OntoquestException {
    String[] includedProperties = {};
    boolean isSynonymIncluded = true;
    String[] excludedProperties = {"disjointWith"};
    OntoquestFunction<ResourceSet> f = new GetNeighbors(
        new String[]{"Cerebellum"}, kbid, includedProperties, null, 
        true, GetNeighbors.EDGE_OUTGOING, true, false, 1, false, 
        new GetNeighbors.PropertyType[]{PropertyType.DATATYPE, PropertyType.OBJECT});
    ResourceSet rs = f.execute(context, varList8);
    int count = 0;
    while (rs.next()) {
      System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
          +", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
          +", " + rs.getInt(7)+", "+rs.getString(8));
      count++;
    }
    rs.close();
    System.out.println("====================");
  }

  public void testProteinSubclasses() throws OntoquestException, IOException {
//    String name = "nlx_cell_100601" ; 
    String name = "PRO_000000001";
    String nodeFileName = "tmp/node.csv";
    HashMap<String, Node> nodeMap = new HashMap<String, Node>();
//    HashSet<Node> nodeSet = new HashSet<Node>();
    Queue<Node> nodeQueue = new LinkedList<Node>();
    
    // get the id of initial node
    ResourceSet rs0 = basicFunctions.searchAllIDsByName(name, new int[]{kbid}, true, context, varList3);
    while (rs0.next()) {
      nodeQueue.add(new Node(rs0.getInt(1), rs0.getInt(2)));
    }
    rs0.close();

    while (!nodeQueue.isEmpty()) {
      Node n = nodeQueue.poll();

      OntoquestFunction<ResourceSet> f = new GetSubClasses(n.rid, n.rtid, kbid,
          1, true);
      ResourceSet rs = f.execute(context, varList8);

      // fetch all subclasses
      while (rs.next()) {
        int[] id1 = new int[] { rs.getInt(1), rs.getInt(2) };
        String idStr = ClassNode.generateId(id1[0], id1[1]);
        Node n1 = new Node(id1[0], id1[1]);
        if (!nodeMap.containsKey(idStr)) {
          nodeMap.put(idStr, n1);
        }

        int[] id2 = new int[] { rs.getInt(4), rs.getInt(5) };
        String idStr2 = ClassNode.generateId(id2[0], id2[1]);
        if (!nodeMap.containsKey(idStr2)) {
          nodeMap.put(idStr2, new Node(id2[0], id2[1]));
        }

        n1.parentRid = id2[0];
        n1.parentRtid = id2[1];
        n1.parentName = rs.getString(6);

        nodeQueue.add(n1);
      }

      rs.close();
    }
    
    // fetch node information
    String[] nodeProperties = {"label", "hasExactSynonym", "hasRelatedSynonym", "definition", "comment"};
    int count = 0;
    for (Node n : nodeMap.values()) {
      OntoquestFunction<ResourceSet> f = new GetNeighbors(n.rid, n.rtid, kbid, nodeProperties, null, GetNeighbors.EDGE_OUTGOING, true, false, 1, false);
      ResourceSet rs2 = f.execute(context, varList8);
      while (rs2.next()) {
        if (rs2.getString(8).equals("label")) {
          n.label = rs2.getString(6);
        } else if (rs2.getString(8).equals("hasExactSynonym")) {
          n.exactSynonym = rs2.getString(6);
        } else if (rs2.getString(8).equals("hasRelatedSynonym")) {
          n.relatedSynonym = rs2.getString(6);
        } else if (rs2.getString(8).equals("definition")) {
          n.definition = rs2.getString(6);
        } else if (rs2.getString(8).equals("comment")) {
          n.comment = rs2.getString(6);
        }
      }
      rs2.close();
      n.name = basicFunctions.getName(n.rtid, n.rid, context);
      
//      nodeSet.add(n);
      if (++count % 500 == 0) {
        System.out.println("Got node count -- "  + count);
      }
    }
    
    
    FileWriter nodeWriter = new FileWriter(new File(nodeFileName));
    nodeWriter.write("ontoquestID,NIF_ID,label,parentID,parentLabel,exactSynonym,relatedSynonym,definition,comment\n");
    for (Node n : nodeMap.values()) {
      nodeWriter.write(""+n.rid+'-'+n.rtid+','+'"'+n.name+'"'+','+
          '"'+n.label+'"'+',');
      if (n.parentRtid > 0) 
        nodeWriter.write(""+n.parentRid+'-'+n.parentRtid);
      nodeWriter.write(',');
      if (n.parentName != null)
        nodeWriter.write('"'+n.parentName+'"');
      nodeWriter.write(',');
      if (n.exactSynonym != null)
        nodeWriter.write('"'+n.exactSynonym+'"');
      nodeWriter.write(',');
      if (n.relatedSynonym != null)
        nodeWriter.write('"'+n.relatedSynonym+'"');
      nodeWriter.write(',');
      if (n.definition != null)
        nodeWriter.write('"'+ n.definition+'"');
      nodeWriter.write(',');
      if (n.comment != null)
        nodeWriter.write('"'+n.comment+'"');
      nodeWriter.write('\n');
    }
    nodeWriter.close();
  }
  
  class Node {
     int rid, rtid;
     int parentRid, parentRtid = -1;
     String parentName = null;
     String name = null;
     String label = null;
     String comment = null;
     String exactSynonym = null;
     String definition = null;
     String relatedSynonym = null;
     
     public Node(int rid, int rtid) {
       this.rid = rid;
       this.rtid = rtid;
     }
  }
  
}
