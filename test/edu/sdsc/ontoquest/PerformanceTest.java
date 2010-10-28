package edu.sdsc.ontoquest;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import edu.sdsc.ontoquest.db.functions.CheckInferredDefinition;
import edu.sdsc.ontoquest.db.functions.GetNeighbors;

/**
 * @version $Id: PerformanceTest.java,v 1.1 2010-10-28 06:29:52 xqian Exp $
 *
 */
public class PerformanceTest extends OntoquestTestAdapter {
  String kbName = "NIF"; //"pizza";

  public void testPerformance() throws Exception {
    TimingSummary summary = null;
    
//    summary = searchTerm("cere", 20);
//    System.out.println(summary);
    
//    summary = getParts("Brain", 2, 20);
//    System.out.println(summary);
//
//    summary = getParts("Brain", 3, 20);
//    System.out.println(summary);
//
//    summary = getParts("Brain", 4, 20);
//    System.out.println(summary);
    
//    summary = getSubclasses("Neuron", 2, 20);
//    System.out.println(summary);
//
//    summary = getSubclasses("Neuron", 3, 20);
//    System.out.println(summary);
//
//    summary = getSubclasses("Neuron", 4, 20);
//    System.out.println(summary);
    
    summary = getNeighbors("Neuron", 3, 20);
    System.out.println(summary);
    
    summary = checkInferredDef(new String[]{"Gabaergic neuron"}, 20);
    System.out.println(summary);
  }
  
  public TimingSummary getParts(String term, int level, int iterations) throws Exception {
    TimingSummary summary = new TimingSummary();
    summary.iterations = iterations;
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    String[] includedProperties = {"has_part"};
    for (int k=0; k<iterations; k++) {
      long start = System.currentTimeMillis();            
      OntoquestFunction<ResourceSet> f = new GetNeighbors(
          term, kbid, includedProperties, null, 
          true, GetNeighbors.EDGE_OUTGOING, level, true);
      ResourceSet rs = f.execute(context, varList8);
      int count = 0;
      while (rs.next()) {
        count++;
      }
      rs.close();
      summary.addValue(System.currentTimeMillis() - start);
      summary.setResultSize(count);
    }
    return summary;
  }
  
  public TimingSummary getSubclasses(String term, int level, int iterations) throws Exception {
    TimingSummary summary = new TimingSummary();
    summary.iterations = iterations;
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    String[] includedProperties = {"subClassOf"};
    for (int k=0; k<iterations; k++) {
      long start = System.currentTimeMillis();            
      OntoquestFunction<ResourceSet> f = new GetNeighbors(
          term, kbid, includedProperties, null, 
          true, GetNeighbors.EDGE_INCOMING, level, true);
      ResourceSet rs = f.execute(context, varList8);
      int count = 0;
      while (rs.next()) {
        count++;
      }
      rs.close();
      summary.addValue(System.currentTimeMillis() - start);
      summary.setResultSize(count);
    }
    return summary;
  }
  
  public TimingSummary getNeighbors(String term, int level, int iterations) throws Exception {
    TimingSummary summary = new TimingSummary();
    summary.iterations = iterations;
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    String[] includedProperties = null;
    for (int k=0; k<iterations; k++) {
      long start = System.currentTimeMillis();            
      OntoquestFunction<ResourceSet> f = new GetNeighbors(
          term, kbid, includedProperties, null, 
          true, GetNeighbors.EDGE_BOTH, level, true);
      ResourceSet rs = f.execute(context, varList8);
      int count = 0;
      while (rs.next()) {
        count++;
      }
      rs.close();
      summary.addValue(System.currentTimeMillis() - start);
      summary.setResultSize(count);
    }
    return summary;
  }

  public TimingSummary searchTerm(String term, int iterations) throws Exception {
    TimingSummary summary = new TimingSummary();
    summary.iterations = iterations;
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    for (int k=0; k<iterations; k++) {
      int count = 0, resultLimit = 40;
      long start = System.currentTimeMillis();            
      ResourceSet rs = basicFunctions.searchTerm(term, new int[]{kbid}, context, varList1, resultLimit, false, 40, true);
      while (rs.next()) {
        count++;
      }
      rs.close();
      summary.addValue(System.currentTimeMillis() - start);
      summary.setResultSize(count);
    }
    return summary;
  }
  
  public TimingSummary checkInferredDef(String[] terms, int iterations) throws Exception {
    TimingSummary summary = new TimingSummary();
    summary.iterations = iterations;
    int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
    for (int k=0; k<iterations; k++) {
      int count = 0;
      long start = System.currentTimeMillis();            
      OntoquestFunction<ResourceSet> f = new CheckInferredDefinition(terms, kbid, true);
      ResourceSet rs = f.execute(context, varList3);
      while (rs.next()) {
        count++;
      }
      rs.close();
      summary.addValue(System.currentTimeMillis() - start);
      summary.setResultSize(count);
    }
    return summary;
  }
  
  public class TimingSummary {
    public final Mean mean;
    public final StandardDeviation stdev;
    public int iterations;
    public int resultSize;

    public TimingSummary() {
      mean = new Mean();
      stdev = new StandardDeviation();
    }

    public void addValue(long value) {
      mean.increment(value);
      stdev.increment(value);
    }
    
    

    @Override
    public String toString() {
      return "Result Size: " + getResultSize() + "; Over " + iterations + " iterations averaged " + mean.getResult() + "ms with stdev " + stdev.getResult();
    }

    /**
     * @return the resultSize
     */
    public int getResultSize() {
      return resultSize;
    }

    /**
     * @param resultSize the resultSize to set
     */
    public void setResultSize(int resultSize) {
      this.resultSize = resultSize;
    }   

  }

}
