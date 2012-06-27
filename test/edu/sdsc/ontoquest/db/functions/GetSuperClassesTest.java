package edu.sdsc.ontoquest.db.functions;

import java.util.ArrayList;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;

/**
 * @version $Id: GetSuperClassesTest.java,v 1.3 2012-06-27 20:08:17 xqian Exp $
 *
 */
public class GetSuperClassesTest extends OntoquestTestAdapter {
	//  String kbName = "pizza";
	//  String[] names = {"Pizza", "PineKernels", "Rosa"};
	String kbName = "NIF";
	int kbid;
	String[] names = {"Cerebellar cortex", "purkinje Cell"};
	ArrayList<int[]> nodeIDs = new ArrayList<int[]>();

	@Override
	public void setUp() {
		super.setUp();
		try {
			// get IDs of terms
			kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
			for (String name : names) {
				ResourceSet rs = basicFunctions.searchAllIDsByName(
						name, new int[]{kbid}, context, varList3);
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
		//    nodeIDs.add(new int[]{649226, 1});
		long time1 = System.currentTimeMillis();
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
		long time2 = System.currentTimeMillis();
		System.out.println("Total Running Time including printing (ms): "
				+ (time2 - time1));
	}
}
