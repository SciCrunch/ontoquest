package edu.sdsc.ontoquest.rest;

import org.restlet.resource.ClientResource;

import edu.sdsc.ontoquest.OntoquestTestAdapter;

/**
 * @version $Id: RESTClient.java,v 1.2 2011-07-12 18:00:00 xqian Exp $
 *
 */
public class RESTClient extends OntoquestTestAdapter {
  
  public void run(String url) throws Exception {
    // Define our Restlet client resources.
    ClientResource clientResource = new ClientResource(url);
    clientResource.get();
    if (clientResource.getStatus().isSuccess()
            && clientResource.getResponseEntity().isAvailable()) {
        clientResource.getResponseEntity().write(System.out);
    } else {
      System.out.println(clientResource.getStatus().toString());
    }
    
    
  }
  
  
  public void testClassNode() throws Exception {
//    run("http://localhost:8182/ontoquest/concepts/oid/158065-1");
    run("http://localhost:8182/ontoquest/concepts/sao436474611");
//    run("http://localhost:8182/ontoquest/concepts/term/cerebellum");
//  run("http://localhost:8182/ontoquest/concepts/term/cerebellum?ontology=NIF_old");
//    run("http://localhost:8182/ontoquest/concepts/term/Glial+Cell");
    
  }
  
  public void testDownloadOntology() throws Exception {
    run("http://localhost:8182/ontoquest/ontologies/download/NIF");    
//    run("http://localhost:8182/ontoquest/ontologies/download/NIF+adf");    
  }
  
  public void testLoadSearch() throws Exception {
    for (int i=0; i<100; i++) {
      System.out.println("\ni = " + i);
      run("http://nif-services.neuinfo.org/ontoquest/concepts/search/cerebellum");
//      Thread t = new Thread("t"+i) {
//        public void run() {
//          try {
//            System.out.println("Start thread " + getName());
//            String url = "http://localhost:8182/ontoquest/concepts/search/cerebellum";
//            URL u = new URL(url);
//            System.out.println(u.openConnection().getContent());
////            RESTClient.this.run("http://localhost:8182/ontoquest/concepts/search/cerebellum");
//          } catch (Exception e) {
//            e.printStackTrace();
//          }
//        }     
//      };
//      t.start();
    }
  }
  
  public void testNeighbors() throws Exception {
//    run("http://localhost:8182/ontoquest/graph/all/GO_0046712?level=3");
//  run("http://localhost:8182/ontoquest/graph/parents/GO_0046712?level=2");
  run("http://localhost:8182/ontoquest/graph/children/GO_0046712?level=2");
  }
  
  public void testNIFCards() throws Exception {
//  run("http://localhost:8182/ontoquest/nifcard/list_cell_locations");    
//  run("http://localhost:8182/ontoquest/nifcard/list_cells");    
  run("http://localhost:8182/ontoquest/nifcard/list_anatomical_structures");    
}

  public void testOntologies() throws Exception {
//    run("http://localhost:8182/ontoquest/ontologies");
    run("http://localhost:8182/ontoquest/ontologies/detail/NIF");    
  }
  
  public void testParts() throws Exception {
//    run("http://localhost:8182/ontoquest/graph/parts/oid/178666-1?level=3");
    run("http://localhost:8182/ontoquest/graph/parts/birnlex_1489?level=3");
//    run("http://localhost:8182/ontoquest/graph/parts/term/cerebellum?level=3");    
    run("http://localhost:8182/ontoquest/graph/whole/birnlex_1146");    
  }
  
  public void testSearch() throws Exception {
    run("http://localhost:8182/ontoquest/concepts/search/cerebellum");
//  run("http://localhost:8182/ontoquest/concepts/search/ammon%27s+horn");
//  run("http://localhost:8182/ontoquest/concepts/search/birnlex_276");
//    run("http://localhost:8182/ontoquest/concepts/search/cerebellum?ontology=NIF_old&begin_with=true&max_ed=30&result_limit=10");
  }

  public void testSiblings() throws Exception {
//    run("http://localhost:8182/ontoquest/concepts/siblings/classes/term/cerebellum");
//    run("http://localhost:8182/ontoquest/concepts/siblings/classes/birnlex_1566");
//    run("http://localhost:8182/ontoquest/concepts/siblings/classes/oid/178666-1");
    run("http://localhost:8182/ontoquest/concepts/siblings/classes/birnlex_1566?ontology=NIF_old");
  }
  
  public void testSubclasses() throws Exception {
    run("http://localhost:8182/ontoquest/graph/subclasses/oid/178666-1");
    run("http://localhost:8182/ontoquest/graph/subclasses/oid/178666-1?level=3");
  }
  public void testSuperclasses() throws Exception {
    run("http://localhost:8182/ontoquest/graph/superclasses/birnlex_1146");    
  }
}
