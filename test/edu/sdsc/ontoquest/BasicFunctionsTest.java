package edu.sdsc.ontoquest;

/**
 * @version $Id: BasicFunctionsTest.java,v 1.2 2011-07-13 12:15:44 xqian Exp $
 *
 */
public class BasicFunctionsTest extends OntoquestTestAdapter {

	String kbName = "NIF"; //"pizza";
	//  String kbName = "pizza";
	int kbid = -1;
	String strToSearch[] = {"Bil", "Zz", "zz_ ", "%", "'", "Cla", "zz|^An"};
	//  String names[] = {"Pizza", "PineKernels", "Rosa"};
	String names[] = {"Cerebellum", "Ammon's horn", "brain", "neuron", "nosuchterm"};

	String pname = "label";

	public BasicFunctionsTest(String name) {
		super(name);
	}

	@Override
	public void setUp() {
		super.setUp();
		try {
			kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failure during set up");
		}
	}

	public void testGetID() throws OntoquestException {
		String nodeName = "protein";
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		ResourceSet rs = basicFunctions.searchAllIDsByName(nodeName, new int[]{kbid}, context, varList3);
		while (rs.next()) {
			String name = basicFunctions
					.getLabel(rs.getInt(2), rs.getInt(1), context);
			assertEquals(nodeName, name);
		}
	}

	public void testGetKnowledgeBaseID() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		System.out.println("kbid = " + kbid);
	}

	public void testGetPropertyValue() throws OntoquestException {
		for (String name : names) {
			ResourceSet rs = basicFunctions.searchAllIDsByName(name, new int[]{kbid}, context, varList3);
			while (rs.next()) {
				int rid = rs.getInt(1);
				int rtid = rs.getInt(2);
				ResourceSet rs2 = basicFunctions.getPropertyValue(rid, rtid, pname, context, varList1);
				while (rs2.next()) {
					System.out.println("Node (name="+name+", rid="+rid+", rtid="+rtid
							+"); Property = "+pname + "; value = "+rs2.getString(1));
				}
				rs2.close();
			}
			rs.close();
		}
	}

	public void testListKnowledgeBases() throws OntoquestException {
		ResourceSet rs = basicFunctions.listKnowledgeBases(context, varList2);
		System.out.println("========= BEGIN List Knowledge Bases =========");
		while (rs.next()) {
			System.out.println("list KB -- id = " + rs.getInt(1) + "; name = "+rs.getString(2));
		}
		rs.close();
		System.out.println("========= END List Knowledge Bases =========");
	}

	public void testListRootResources() throws OntoquestException {
		System.out.println("========= BEGIN List Root Resources =========");
		System.out.println("-------- list roots excluding owl:Thing, prefer label -------");
		ResourceSet rs = basicFunctions.listRootResources(kbid, true, true, context, varList3);
		while (rs.next()) {
			System.out.println("Root (rid, rtid, label) : " + rs.getInt(1)
					+ ", " + rs.getInt(2) + ", " + rs.getString(3));
		}
		rs.close();

		System.out.println("-------- list roots including owl:Thing, not prefer label -------");
		rs = basicFunctions.listRootResources(kbid, false, false, context, varList3);
		while (rs.next()) {
			System.out.println("Root (rid, rtid, label) : " + rs.getInt(1)
					+ ", " + rs.getInt(2) + ", " + rs.getString(3));
		}
		rs.close();

		System.out.println("-------- list roots excluding owl:Thing, not prefer label -------");
		rs = basicFunctions.listRootResources(kbid, true, false, context, varList3);
		while (rs.next()) {
			System.out.println("Root (rid, rtid, label) : " + rs.getInt(1)
					+ ", " + rs.getInt(2) + ", " + rs.getString(3));
		}
		rs.close();

		System.out.println("========= END List Root Resources =========");
	}

	public void testScanAllRelationships() throws OntoquestException {
		String kbName = "NIF";
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		ResourceSet rs = basicFunctions.scanAllRelationships(new int[]{kbid}, context, varList5, false);
		//    System.out.println("========== scan all relationships ==========");
		int count = 0;
		while (rs.next()) {
			int rid1 = rs.getInt(1);
			int rtid1 = rs.getInt(2);
			int rid2 = rs.getInt(3);
			int rtid2 = rs.getInt(4);
			int pid = rs.getInt(5);
			//      System.out.println(rid1+"  "+rtid1+"  "+rid2+"  "+rtid2+"  "+pid);
			count++;
		}
		rs.close();
		//    assertEquals(expectedCountOfAllRel, count);
		System.out.println("===== count of all relationship: "+count+" =====");
	}

	public void testScanAllRelationshipsWithWeight() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID("Pizza", context);
		ResourceSet rs = basicFunctions.scanAllRelationships(new int[]{kbid}, context, varList6, true);
		//    System.out.println("========== scan all relationships ==========");
		int count = 0;
		while (rs.next()) {
			int rid1 = rs.getInt(1);
			int rtid1 = rs.getInt(2);
			int rid2 = rs.getInt(3);
			int rtid2 = rs.getInt(4);
			int pid = rs.getInt(5);
			float weight = rs.getFloat(6);
			System.out.println(rid1+"  "+rtid1+"  "+rid2+"  "+rtid2+"  "+pid+"  "+weight);
			count++;
		}
		rs.close();
		//    assertEquals(expectedCountOfAllRel, count);
		System.out.println("===== count of all relationship: "+count+" =====");
	}

	public void testScanClassRelationships() throws OntoquestException {
		ResourceSet rs = basicFunctions.scanClassRelationships(kbid, context, varList5);
		//    System.out.println("========== scan class relationships ==========");
		int count = 0;
		while (rs.next()) {
			int rid1 = rs.getInt(1);
			int rtid1 = rs.getInt(2);
			int rid2 = rs.getInt(3);
			int rtid2 = rs.getInt(4);
			int pid = rs.getInt(5);
			//      System.out.println(rid1+"  "+rtid1+"  "+rid2+"  "+rtid2+"  "+pid);
			count++;
		}
		rs.close();
		//    assertEquals(expectedCountOfClassRel, count);
		System.out.println("===== count of class relationship: "+count+" =====");
	}

	public void testScanPropertyRelationships() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		ResourceSet rs = basicFunctions.scanPropertyRelationships(kbid, context, varList5);
		//    System.out.println("========== scan property relationships ==========");
		int count = 0;
		while (rs.next()) {
			int rid1 = rs.getInt(1);
			int rtid1 = rs.getInt(2);
			int rid2 = rs.getInt(3);
			int rtid2 = rs.getInt(4);
			int pid = rs.getInt(5);
			//      System.out.println(rid1+"  "+rtid1+"  "+rid2+"  "+rtid2+"  "+pid);
			count++;
		}
		rs.close();
		//    assertEquals(expectedCountOfPropertyRel, count);
		System.out.println("===== count of property relationship: "+count+" =====");
	}

	public void testSearchAllIDsByName() throws OntoquestException {
		//    String kbName = "NIF";
		String[] names = {"hippocampal neuron", "Hippocampus CA2 basket cell narrow"};
		//    String[] names = {"Ammon's horn", "cerebellar cortex", "subPropertyOf", "part_of", "mouse"};
		//    String[] names = {"mlaspepkglvpftkesfelikqhiakthnedheeedlkpnpdlevgkklpfiygnlsqgmvsepledvdpyyykkkntfivlnknrtifrfnaasilctlspfncirrttikvlvhpffqlfilisvlidcvfmsltnlpkwrpvlentllgiytfeilvklfargvwagsfsflgdpwnwldfsvtvfeviiryspldfiptlqtartlrilkiiplnqglkslvgvlihclkqligviiltlfflsifsligmglfmgnlkhkcfrwpqenenetlhnrtgnpyyiretenfyylegeryallcgnrtdagqcpegyvcvkaginpdqgftnfdsfgwalfalfrlmaqdypevlyhlilyasgkvymiffvvvsflfsfymaslflgilamayeeekqrvgeiskkiepkfqqtgkelqegnetdeaktiqiemkkrspistdtsldvledatlrhkeelekskkicplywykfaktfliwncspcwlklkefvhriimapftdlfliiciilnvcfltlehypmskqtntllnignlvfigiftaemifkiiamhpygyfqvgwnifdsmivfhglielclanvagmallrlfrmlrifklgkywptfqilmwslsnswvalkdlvlllftfiffsaafgmklfgknyeefvchidkdcqlprwhmhdffhsflnvfrilcgewvetlwdcmevagqswcipfylmvilignllvlylflalvssfssckdvtaeenneaknlqlavarikkginyvllkilcktqnvpkdtmdhvnevyvkedisdhtlselsntqdflkdkekssgteknatenesqslipspsvsetvpiasgesdienldnkeiqsksgdggskekikqssssecstvdiaiseeeemfyggerskhlkngcrrgsslgqisgaskkgkiwqnirktcckivennwfkcfiglvtllstgtlafediyidqrktikilleyadmiftyifilemllkwmaygfkayfsngwyrldfvvvivfclsligktreelkplismkflrplrvlsqfermkvvvralikttlptlnvflvclmiwlifsimgvdlfagrfyecidptsgerfpssevmnksrcesllfnesmlwenakmnfdnvgngflsllqvatfngwitimnsaidsvavniqphfevniymycyfinfiifgvflplsmlitviidnfnkhkiklggsnifitvkqrkqyrrlkklmyedsqrpvprplnklqgfifdvvtsqafnvivmvlicfqaiammidtdvqslqmsialywinsifvmlytmecilkliafrcfyftiawnifdfmvvifsitglclpmtvgsylvppslvqlillsriihmlrlgkgpkvfhnlmlplmlslpallniilliflvmfiyavfgmynfayvkkeagindvsnfetfgnsmlclfqvaifagwdgmldaifnskwsdcdpdkinpgtqvrgdcgnpsvgifyfvsyiliswliivnmyivvvmeflniaskkknktlseddfrkffqvwkrfdpdrtqyidssklsdfaaaldpplfmakpnkgqlialdlpmavgdrihcldillaftkrvmgqdvrmekvvseiesgfllanpfkitcepitttlkrkqeavsatiiqrayknyrlrrndkntsdihmidgdrdvhatkegayfdkakekspiqsqi"};
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		for (String name : names) {
			System.out.println("======== Get ID of term " + name);
			ResourceSet rs = basicFunctions.searchAllIDsByName(name, new int[]{kbid}, context, varList3);
			while (rs.next()) {
				System.out.println("(rid, rtid, name) = (" + rs.getInt(1) + ", " + rs.getInt(2)+ ", " + rs.getString(3) + ")");
			}
			rs.close();
		}

		//    try {
		//      basicFunctions.searchAllIDsByName(null, new int[]{kbid}, context, varList2);
		//      fail("Search term null. An exception should have been thrown!");
		//    } catch (OntoquestException oe) {
		//      System.out.println("Search term null. Got a proper exception.");
		//    }
	}

	public void testSearchAllIDsByName2() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		System.out.println("======== Get ID of terms ");
		ResourceSet rs = basicFunctions.searchAllIDsByName(names, new int[]{kbid}, true, false, context, varList3);
		while (rs.next()) {
			System.out.println("(rid, rtid, name) = (" + rs.getInt(1) + ", " + rs.getInt(2)+ ", " + rs.getString(3) + ")");
		}
		rs.close();
	}

	//  public void testGetName() throws OntoquestException {
	//    System.out.println(" rtid     rid      name");
	//    for (int i=0; i<cids.length; i++) {
	//      String name = basicFunctions.getName(cids[i][0], cids[i][1], context);
	//      System.out.println(cids[i][0]+"     "+cids[i][1]+"    "+name);
	//    }
	//    try {
	//      basicFunctions.getName(25, 1, context); // invalid rtid
	//      fail("Invalid rtid. An error should have been thrown by now.");
	//    } catch (Exception e) {
	//      System.out.println("Exception is properly thrown. Message: "+e.getMessage());
	//    }
	//
	//    try {
	//      basicFunctions.getName(1, 6, context); // invalid rid
	//      fail("Invalid rid. An error should have been thrown by now.");
	//    } catch (Exception e) {
	//      System.out.println("Exception is properly thrown. Message: "+e.getMessage());
	//    }
	//  
	//    try {
	//      basicFunctions.getName(11, 6, context); // anonymous class with invalid rid
	//      fail("Anonymous class with invalid rid. An error should have been thrown by now.");
	//    } catch (Exception e) {
	//      System.out.println("Exception is properly thrown. Message: "+e.getMessage());
	//    }
	//  }

	public void testSearchName() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		int count = 0, resultLimit = 5;
		for (String element : strToSearch) {
			count = 0;
			ResourceSet rs = basicFunctions.searchName(element, new int[]{kbid}, context, varList1, resultLimit, false, BasicFunctions.MASK_SEARCH_ALL, 0);
			while (rs.next()) {
				count++;
				System.out.println(element + "   " +rs.getString(1));
			}
			rs.close();
			System.out.println("search string: '" + element +"' in kb " + kbName + "; count = "+count);

			count = 0;
			ResourceSet rs2 = basicFunctions.searchName(element, null, context, varList1, resultLimit, false, BasicFunctions.MASK_SEARCH_ALL, 0);
			while (rs2.next()) {
				count++;
				System.out.println(element + "   " + rs2.getString(1));
			}
			System.out.println("search string '" + element +"' in all kb; count2 = "+count);
			rs2.close();

			count = 0;
			int searchType = BasicFunctions.MASK_SEARCH_CLASS | BasicFunctions.MASK_SEARCH_INSTANCE;
			ResourceSet rs3 = basicFunctions.searchName(element, new int[]{kbid}, context, varList1, resultLimit, false, searchType, 0);
			while (rs3.next()) {
				count++;
				System.out.println(element + "   " +rs3.getString(1));
			}
			rs3.close();
			System.out.println("search string: '" + element +"' in kb " + kbName + "; search classes and instances only. count = "+count);
		}
	}

	public void testSearchName2() throws OntoquestException {
		//    String kbName = "NIF";
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		int count = 0, resultLimit = 5;
		String[] strToSearch = {"purkinje", "ion channel", "topping"};
		for (String element : strToSearch) {
			count = 0;
			ResourceSet rs = basicFunctions.searchName(element, new int[]{kbid}, context, varList1, resultLimit, false, BasicFunctions.MASK_SEARCH_ALL, 20);
			while (rs.next()) {
				count++;
				System.out.println(element + "   " +rs.getString(1));
			}
			rs.close();
			System.out.println("search string: '" + element +"' in kb " + kbName + "; count = "+count);

			count = 0;
			ResourceSet rs2 = basicFunctions.searchName(element, null, context, varList1, resultLimit, false, BasicFunctions.MASK_SEARCH_ALL, 0);
			while (rs2.next()) {
				count++;
				System.out.println(element + "   " + rs2.getString(1));
			}
			System.out.println("search string '" + element +"' in all kb; count2 = "+count);
			rs2.close();

			count = 0;
			int searchType = BasicFunctions.MASK_SEARCH_CLASS | BasicFunctions.MASK_SEARCH_INSTANCE;
			ResourceSet rs3 = basicFunctions.searchName(element, new int[]{kbid}, context, varList1, resultLimit, false, searchType, 0);
			while (rs3.next()) {
				count++;
				System.out.println(element + "   " +rs3.getString(1));
			}
			rs3.close();
			System.out.println("search string: '" + element +"' in kb " + kbName + "; search classes and instances only. count = "+count);

			count = 0;
			ResourceSet rs4 = basicFunctions.searchNameStartWith(element, new int[]{kbid}, context, varList1, resultLimit, false, BasicFunctions.MASK_SEARCH_ALL, 0);
			while (rs4.next()) {
				count++;
				System.out.println(element + "   " +rs4.getString(1));
			}
			rs4.close();
			System.out.println("search string start with: '" + element +"' in kb " + kbName + "; search all types. count = "+count);

		}
	}

	public void testSearchNameLike() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		int count = 0, resultLimit = 5;
		for (String element : strToSearch) {
			count = 0;
			ResourceSet rs = basicFunctions.searchNameLike(element, kbid, true, basicFunctions.MASK_SEARCH_ALL, context, varList3);
			while (rs.next()) {
				count++;
				System.out.println(element + "   " +rs.getString(1));
			}
			rs.close();
			System.out.println("1. search string like: '" + element +"' in kb " + kbName + "; count = "+count);

			count = 0;
			ResourceSet rs2 = basicFunctions.searchNameLike(element, 0, true, basicFunctions.MASK_SEARCH_ALL, context, varList3);
			while (rs2.next()) {
				count++;
				System.out.println(element + "   " + rs2.getString(1));
			}
			System.out.println("2. search string like '" + element +"' in all kb; count2 = "+count);
			rs2.close();

			count = 0;
			int searchType = BasicFunctions.MASK_SEARCH_CLASS | BasicFunctions.MASK_SEARCH_INSTANCE;
			ResourceSet rs3 = basicFunctions.searchNameLike(element, kbid, true, searchType, context, varList3);
			while (rs3.next()) {
				count++;
				System.out.println(element + "   " +rs3.getString(1));
			}
			rs3.close();
			System.out.println("3. search string like '" + element +"' in kb " + kbName + "; search classes and instances only. count = "+count);
		}    
	}

	public void testSearchNameRegex() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		int count = 0;
		for (String element : strToSearch) {
			count = 0;
			ResourceSet rs = basicFunctions.searchNameRegex(element, new int[]{kbid}, context, varList1, 10, false, BasicFunctions.MASK_SEARCH_ALL);
			while (rs.next()) {
				count++;
				System.out.println(element + "   " +rs.getString(1));
			}
			rs.close();
			System.out.println("search regex string: '" + element +"' in kb " + kbName + "; count = "+count);

			count = 0;
			ResourceSet rs2 = basicFunctions.searchNameRegex(element, null, context, varList1, 10, false, BasicFunctions.MASK_SEARCH_ALL);
			while (rs2.next()) {
				count++;
				System.out.println(element + "   " + rs2.getString(1));
			}
			System.out.println("search regex string '" + element +"' in all kb; count2 = "+count);
			rs2.close();

			count = 0;
			int searchType = BasicFunctions.MASK_SEARCH_CLASS | BasicFunctions.MASK_SEARCH_INSTANCE;
			ResourceSet rs3 = basicFunctions.searchNameRegex(element, new int[]{kbid}, context, varList1, 10, false, searchType);
			while (rs3.next()) {
				count++;
				System.out.println(element + "   " +rs3.getString(1));
			}
			rs3.close();
			System.out.println("search regex string: '" + element +"' in kb " + kbName + "; search classes and instances only. count = "+count);
		}
	}

	public void testSearchTerm() throws OntoquestException {
		int kbid = basicFunctions.getKnowledgeBaseID(kbName, context);
		int count = 0, resultLimit = 40;
		String[] strToSearch = {"Loli"};
		for (String element : strToSearch) {
			count = 0;
			ResourceSet rs = basicFunctions.searchTerm(element, new int[]{kbid}, context, varList1, resultLimit, false, 40, true);
			while (rs.next()) {
				count++;
				System.out.println(element + "   " +rs.getString(1));
			}
			rs.close();
			System.out.println("search string: '" + element +"' in kb " + kbName + "; count = "+count);

		}
	}
}
