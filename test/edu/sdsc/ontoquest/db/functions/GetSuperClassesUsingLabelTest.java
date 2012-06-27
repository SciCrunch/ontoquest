package edu.sdsc.ontoquest.db.functions;

import java.util.ArrayList;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.OntoquestTestAdapter;
import edu.sdsc.ontoquest.ResourceSet;
import junit.framework.TestCase;

/**
 * @version $Id: GetSuperClassesUsingLabelTest.java,v 1.2 2012-06-27 20:08:17 xqian Exp $
 *
 */
public class GetSuperClassesUsingLabelTest extends OntoquestTestAdapter {
	String kbName = "NIF";
	int kbid = -1;
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

	public void testExecute() throws OntoquestException {
		long time1 = System.currentTimeMillis();
		OntoquestFunction<ResourceSet> f = new GetSuperClasses("Purkinje cell", kbid);
		ResourceSet rs = f.execute(context, varList8);
		while (rs.next()) {
			System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
					+", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
					+", " + rs.getInt(7)+", "+rs.getString(8));
		}
		rs.close();
		System.out.println("------------------------------");
		f = new GetSuperClasses(new String[]{"Purkinje cell"}, kbid, 2, true);
		rs = f.execute(context, varList8);
		while (rs.next()) {
			System.out.println(rs.getInt(1)+", "+rs.getInt(2)+", "+rs.getString(3)
					+", " + rs.getInt(4)+", "+rs.getInt(5)+", "+rs.getString(6)
					+", " + rs.getInt(7)+", "+rs.getString(8));
		}
		rs.close();
		long time2 = System.currentTimeMillis();
		System.out.println("Total Running Time including printing (ms): "
				+ (time2 - time1));
	}
}
