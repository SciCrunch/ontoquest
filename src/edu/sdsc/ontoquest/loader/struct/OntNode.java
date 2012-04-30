package edu.sdsc.ontoquest.loader.struct;

/**
 * Intermediate node structure used in BDB cache.
 * 
 * @version $Id: OntNode.java,v 1.1 2012-04-30 22:43:25 xqian Exp $
 * @author xqian
 * 
 */
public class OntNode {
	public enum NodeType {
		AllDifferentIndividual(17), AllValuesFrom(2), Cardinality(4), ComplementClass(
				7), DataRange(16), Datatype(14), DatatypeRestriction(21), HasSelf(20), HasValue(
				10), Individual(
						12), IntersectionClass(
								8), Literal(13), MinCardinality(5), MaxCardinality(6), NamedClass(1), OneOf(
										11), OntologyURI(18), Property(15), RDFList(19), SomeValuesFrom(3), UnionClass(
												9);

		public static NodeType getType(int rtid) {
			switch (rtid) {
			case 2:
				return AllValuesFrom;
			case 4:
				return Cardinality;
			case 1:
				return NamedClass;
			case 14:
				return Datatype;
			case 21:
				return DatatypeRestriction;
			case 10:
				return HasValue;
			case 12:
				return Individual;
			case 18:
				return OntologyURI;
			case 13:
				return Literal;
			case 5:
				return MinCardinality;
			case 6:
				return MaxCardinality;
			case 11:
				return OneOf;
			case 15:
				return Property;
			case 16:
				return DataRange;
			case 19:
				return RDFList;
			case 7:
				return ComplementClass;
			case 9:
				return UnionClass;
			case 3:
				return SomeValuesFrom;
			case 8:
				return IntersectionClass;
			case 17:
				return AllDifferentIndividual;
			case 20:
				return HasSelf;
			default:
				return null;
			}
		}

		private int rtid;

		NodeType(int rtid) {
			this.setRtid(rtid);
		}

		public int getRtid() {
			return rtid;
		}

		public void setRtid(int rtid) {
			this.rtid = rtid;
		}
	}

	private String name; // unique identifer
	private NodeType type;
	private int rid; // resource id in database
	private String browserText;

	public OntNode(String name, NodeType type, int rid, String browserText) {
		this.name = name;
		this.type = type;
		this.rid = rid;
		this.browserText = browserText;
	}

	public String getBrowserText() {
		return browserText;
	}

	public String getName() {
		return name;
	}

	public int getRid() {
		return rid;
	}

	public NodeType getType() {
		return type;
	}

	public void set(String name, NodeType type, int rid, String browserText) {
		setName(name);
		setType(type);
		setRid(rid);
		setBrowserText(browserText);
	}

	public void setBrowserText(String browserText) {
		this.browserText = browserText;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRid(int rid) {
		this.rid = rid;
	}

	public void setType(NodeType type) {
		this.type = type;
	}
}
