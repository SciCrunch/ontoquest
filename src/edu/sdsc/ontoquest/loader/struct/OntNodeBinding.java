package edu.sdsc.ontoquest.loader.struct;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

import edu.sdsc.ontoquest.loader.struct.OntNode.NodeType;

/**
 * BDB binding for OntNode
 * 
 * @version $Id: OntNodeBinding.java,v 1.2 2012-06-23 17:19:27 xqian Exp $
 * @author xqian
 * 
 */
public class OntNodeBinding extends TupleBinding<OntNode> {

	@Override
	public OntNode entryToObject(TupleInput ti) {
		int typeInt = -1;
		try {
			typeInt = ti.readInt();
			NodeType type = NodeType.getType(typeInt);
			int rid = ti.readInt();
			String iri = ti.readString();
			String browserText = ti.readString();
			return new OntNode(iri, type, rid, browserText);
		} catch (Exception e) {
			System.out.println("type int: " + typeInt);
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void objectToEntry(OntNode node, TupleOutput to) {
		to.writeInt(node.getType().getRtid());
		to.writeInt(node.getRid());
		to.writeString(node.getName());
		to.writeString(node.getBrowserText());
	}

}
