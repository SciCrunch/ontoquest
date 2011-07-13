package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.functions.RunSQL;

public class SearchReconcileBean extends BaseBean {

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

		int limit = ClassNode.DefaultResultLimit;
		if (queryObj.has("limit")) {
			limit = queryObj.optInt("limit");
		}

		boolean beginWith = false;
		Object bwObj = attributes.get("begin_with");
		if (bwObj != null) {
			try {
				beginWith = Boolean.parseBoolean(bwObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}

		int maxEditDistance = ClassNode.DefaultMaxEditDistance;
		Object medObj = attributes.get("max_ed");
		if (medObj != null) {
			try {
				maxEditDistance = Integer.parseInt(medObj.toString());
			} catch (Exception e) {
				// do nothing, use default
			}
		}
		return getTerms(query, limit, beginWith, maxEditDistance, kbId, context);

	}
	public static List<SearchReconcileBean> getTerms(String term, int resultLimit,
			boolean beginWith, int maxEditDistance,
			int kbId, Context context) throws OntoquestException {
		String beginChar = beginWith ? "" : "%";

		String sql = "select term, tid, category, d from (select * from (select distinct term, tid, category, editdistance('"
			+ term + "', term) d from TERM_CATEGORY where lower(term) like '"
			+ beginChar
			+ term.toLowerCase()
			+ "%') t1 where d <= "
			+ maxEditDistance
			+ " order by d limit " + resultLimit + ") t";
		// System.out.println("SQL to search term: " + sql);
		RunSQL f = new RunSQL(sql);
		ResourceSet rs = f.execute(context, BaseBean.getVarList4());
		LinkedList<SearchReconcileBean> matchedTerms = new LinkedList<SearchReconcileBean>();
		//			HashMap<String, Integer> tempMap = new HashMap<String, Integer>();

		while (rs.next()) {
			String t = rs.getString(1);
			if (t.toLowerCase().startsWith("regional part of"))
				continue;
			SearchReconcileBean entry = new SearchReconcileBean(rs.getString(2), t,
					rs.getString(3), 1 - rs.getInt(4) / (double) maxEditDistance);
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

	String category = "";

	boolean match = true;

	public SearchReconcileBean(String name, String label,
			String category, double score) throws OntoquestException {
		setLabel(label);
		setName(name);
		setCategory(category);
		setScore(score);
	}

	public String getCategory() {
		return category;
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



	public void setCategory(String category) {
		this.category = category;
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
