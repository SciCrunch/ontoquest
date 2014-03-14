package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.db.functions.RunSQL;

public class SearchReconcileBean extends BaseBean {

	public static final String TYPE_STRICT_ANY = "any";
	public static final String TYPE_STRICT_ALL = "all";
	public static final String TYPE_STRICT_SHOULD = "should";

	public static String DEFAULT_TYPE_STRICT = TYPE_STRICT_ANY;
	
	
	public static HashMap<String, List<SearchReconcileBean>> doMultipleQueries(
			JSONObject queries,
			Map<String, Object> attributes, int kbId,
			Context context) throws JSONException, OntoquestException {
		HashMap<String, List<SearchReconcileBean>> results = new HashMap<String, List<SearchReconcileBean>>();
		Iterator iterator = queries.keys();
		while (iterator.hasNext()) {
			String key = iterator.next().toString();
			JSONObject value = queries.getJSONObject(key);
			results.put(key, doSingleQuery(value, attributes, kbId, context));
		}
		return results;
	}

	public static List<SearchReconcileBean> doSingleQuery(JSONObject queryObj,
			Map<String, Object> attributes, int kbId, Context context)
			throws OntoquestException, JSONException {

		String query = queryObj.getString("query");

		int limit = ClassNodeResource.DefaultResultLimit;
		if (queryObj.has("limit")) {
			limit = queryObj.optInt("limit");
		}
		
		JSONArray types = null;

		String typeStr = queryObj.optString("type");
		if (typeStr != null) {
			types = new JSONArray();
			types.put(typeStr);
		} else {
			types = queryObj.optJSONArray("type");
		}
		
		String typeStrict = queryObj.optString("type_restrict", DEFAULT_TYPE_STRICT);
		
		boolean beginWith = false;
		Object bwObj = attributes.get("begin_with");
		if (bwObj != null) {
			try {
				beginWith = Boolean.parseBoolean(bwObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}

		int maxEditDistance = ClassNodeResource.DefaultMaxEditDistance;
		Object medObj = attributes.get("max_ed");
		if (medObj != null) {
			try {
				maxEditDistance = Integer.parseInt(medObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}
		return getTerms(query, limit, beginWith, maxEditDistance, types, typeStrict, kbId, context);

	}
	public static List<SearchReconcileBean> getTerms(String term, int resultLimit,
			boolean beginWith, int maxEditDistance, JSONArray types, String typeStrict,
			int kbId, Context context) throws OntoquestException {
		String beginChar = beginWith ? "" : "%";
		String typeFromStr = "";
		String typeWhereStr = "";
		
		if (!typeStrict.equalsIgnoreCase(TYPE_STRICT_SHOULD) && !typeStrict.equalsIgnoreCase(TYPE_STRICT_ANY)) {
			throw new OntoquestException(OntoquestException.Type.INPUT, "Unsupported type strict : " + typeStrict);
		}
		
		if (types != null && types.length() > 0) {
			StringBuilder st = new StringBuilder();

			for (int i=0; i<types.length(); i++) {
				try {
					String type = types.getString(i).trim();
					if (type.length() == 0) continue;
					st.append('\'').append(DbUtility.formatSQLString(type)).append('\'').append(',');
				} catch (Exception e){}
			}
			if (st.length() > 0) {
				st.deleteCharAt(st.length()-1);
				typeWhereStr = " and gn.rid = tc.cat_rid and gn.rtid = tc.cat_rtid and gn.name in (" + st.toString() + ") ";
				typeFromStr = ", graph_nodes gn ";
			}
		}
		
		String sql = "select term, tid, n.name as cid, category, d from (select * from (select distinct term, tid, category, cat_rid, cat_rtid, editdistance('"
			+ term + "', term) d from TERM_CATEGORY tc " + typeFromStr + " where lower(term) like '"
			+ beginChar
			+ term.toLowerCase()
			+ "%'" + typeWhereStr + ") t1 where d <= "
			+ maxEditDistance
			+ " order by d limit " + resultLimit + ") t, graph_nodes n where cat_rid = n.rid and cat_rtid = n.rtid";
//System.out.println("SQL to search term: " + sql);
		RunSQL f = new RunSQL(sql);
		ResourceSet rs = f.execute(context, BaseBean.getVarList5());
		LinkedList<SearchReconcileBean> matchedTerms = new LinkedList<SearchReconcileBean>();
		//			HashMap<String, Integer> tempMap = new HashMap<String, Integer>();

		while (rs.next()) {
			String t = rs.getString(1);
			if (t.toLowerCase().startsWith("regional part of"))
				continue;
			SearchReconcileBean entry = new SearchReconcileBean(rs.getString(2), t,
					rs.getString(3), rs.getString(4), 1 - rs.getInt(5) / (double) maxEditDistance);
			matchedTerms.add(entry);
		}
		rs.close();

		return matchedTerms;
	}
	// int rid = 0;
	// int rtid = 0;
	double score = 0;

	String name = "";

	String label = "";

	String categoryLabel = "";

	String categoryName = "";
	
	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}
	boolean match = true;

	public SearchReconcileBean(String name, String label,
			String categoryName, String categoryLabel, double score) throws OntoquestException {
		setLabel(label);
		setName(name);
		setCategoryName(categoryName);
		setCategoryLabel(categoryLabel);
		setScore(score);
	}

	public String getCategoryLabel() {
		return categoryLabel;
	}

	// public String getId() {
	// return ClassNode.generateId(rid, rtid);
	// }

	public String getLabel() {
		return label;
	}

	public String getName() {
		return name;
	}

	// public int getRid() {
	// return rid;
	// }
	//
	// public int getRtid() {
	// return rtid;
	// }

	public double getScore() {
		return score;
	}

	public boolean isMatch() {
		return match;
	}



	public void setCategoryLabel(String categoryLabel) {
		this.categoryLabel = categoryLabel;
	}

	// public void setId(String id) throws OntoquestException {
	// int[] ontoId = parseId(id);
	// rid = ontoId[0];
	// rtid = ontoId[1];
	// }

	public void setLabel(String label) {
		this.label = label;
	}

	public void setMatch(boolean match) {
		this.match = match;
	}

	public void setName(String name) {
		this.name = name;
	}

	// public void setRid(int rid) {
	// this.rid = rid;
	// }
	//
	// public void setRtid(int rtid) {
	// this.rtid = rtid;
	// }

	public void setScore(double score) {
		this.score = score;
	}
	@Override
	public Element toXml(Document doc) {
		return null;
	}

}
