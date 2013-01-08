package edu.sdsc.ontoquest.db.functions;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;

/**
 * @version $Id: EstimateNeighborCountTest.java,v 1.1 2009/03/23 06:42:23 xqian
 *          Exp $
 * 
 */
public class EstimateNeighborCountTest extends OntoquestTestAdapter {
	String kbName = "NIF";

	// String kbName = "BIRNLex";
	int kbid = -1;

	private void run(int rid, int rtid, String[] includedProperties,
			boolean isSynonymIncluded, int edgeDirection,
			boolean excludeHiddenRelationship, int level) throws OntoquestException {
		OntoquestFunction<ResourceSet> f = new EstimateNeighborCount(rid, rtid, kbid,
				includedProperties, edgeDirection,
				excludeHiddenRelationship, level, true);
		ResourceSet rs = f.execute(context, varList1);
		while (rs.next()) {
			System.out.println("neighbor count " + rs.getInt(1));
		}
		rs.close();
	}

	private void run(String[] names, String[] includedProperties,
			boolean isSynonymIncluded, int edgeDirection,
			boolean excludeHiddenRelationship, int level) throws OntoquestException {
		long time1 = System.currentTimeMillis();
		OntoquestFunction<ResourceSet> f = new EstimateNeighborCount(names, kbid,
				includedProperties, isSynonymIncluded, edgeDirection,
				excludeHiddenRelationship, level, true);
		ResourceSet rs = f.execute(context, varList1);
		while (rs.next()) {
			System.out.println("neighbor count " + rs.getInt(1));
		}
		rs.close();
		long time2 = System.currentTimeMillis();
		System.out.println("Total Running Time (ms, printing included): " + (time2-time1));
	}

	@Override
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

	public void testExecute1() throws OntoquestException {
		boolean isSynonymIncluded = true;
		String[] names = { "Hippocampus", "Forebrain" };
		String[] includedProperties = new String[] { "has_part" };
		run(names, includedProperties, isSynonymIncluded,
				EstimateNeighborCount.EDGE_BOTH, true, 0);

	}

	public void testExecute2() throws OntoquestException {
		boolean isSynonymIncluded = true;
		String[] names = { "Cerebellar cortex", "Forebrain" };
		String[] includedProperties = new String[] { "subClassOf" };
		run(names, includedProperties, isSynonymIncluded,
				EstimateNeighborCount.EDGE_BOTH, true, 3);

	}

	public void testExecute3() throws OntoquestException {
		boolean isSynonymIncluded = true;
		int rid = 156159, rtid = 1;
		String[] includedProperties = new String[] { "subClassOf" };
		run(rid, rtid, includedProperties, isSynonymIncluded,
				EstimateNeighborCount.EDGE_OUTGOING, true, 3);

	}
}
