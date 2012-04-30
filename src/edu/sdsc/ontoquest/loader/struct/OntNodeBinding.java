package edu.sdsc.ontoquest.loader.struct;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

import edu.sdsc.ontoquest.loader.struct.OntNode.NodeType;

/**
 * BDB binding for OntNode
 * 
 * @version $Id: OntNodeBinding.java,v 1.1 2012-04-30 22:43:26 xqian Exp $
 * @author xqian
 * 
 */
public class OntNodeBinding extends TupleBinding<OntNode> {

	@Override
	public OntNode entryToObject(TupleInput ti) {
		NodeType type = NodeType.getType(ti.readInt());
		int rid = ti.readInt();
		String iri = ti.readString();
		String browserText = ti.readString();
		return new OntNode(iri, type, rid, browserText);
	}

	@Override
	public void objectToEntry(OntNode node, TupleOutput to) {
		to.writeInt(node.getType().getRtid());
		to.writeInt(node.getRid());
		to.writeString(node.getName());
		to.writeString(node.getBrowserText());
	}

}
