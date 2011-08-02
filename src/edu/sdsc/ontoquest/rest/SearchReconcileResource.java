package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbBasicFunctions;

/**
 * Google Refine Reconcile Services
 * <p>
 * $Id: SearchReconcileResource.java,v 1.2 2011-08-02 17:33:16 xqian Exp $
 * 
 */
public class SearchReconcileResource extends BaseResource {
	protected final String KEY_IS_SINGLE = "is_single";
	private List<SearchReconcileBean> singleSearchResult = null;
	private HashMap<String, List<SearchReconcileBean>> multipleSearchResult = null;
	private SearchReconcileMetaData metaData = null;
	private String callback = null;

	private JSONObject convertMetaData(SearchReconcileMetaData metaData) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", metaData.getName());
		result.put("identifierSpace", metaData.getIdentifierSpace());
		result.put("schemaSpace", metaData.getSchemaSpace());

		JSONObject view = new JSONObject().put("url", metaData.getViewURL());
		result.put("view", view);
		return result;
	}

	private JSONObject convertMultipleResults(
			Map<String, List<SearchReconcileBean>> beanMap) throws JSONException {
		JSONObject result = new JSONObject();
		for (String key : beanMap.keySet()) {
			JSONObject o = convertSingleResult(beanMap.get(key));
			result.put(key, o);
		}
		return result;
	}

	private JSONObject convertSingleResult(List<SearchReconcileBean> beans)
	throws JSONException {
		JSONArray array = new JSONArray();
		for (SearchReconcileBean item : beans) {
			JSONObject obj = new JSONObject();
			obj.put("id", item.getName()).put("name", item.getLabel())
			.put("type", item.getCategory()).put("score", item.getScore())
			.put("match", item.getScore() > 0.5);
			array.put(obj);
		}
		JSONObject result = new JSONObject().put("result", array);
		return result;
	}

	@Override
	protected void doInit() throws ResourceException {
		try {
			OntoquestApplication application = (OntoquestApplication) getApplication();
			int kbId = application.getKbId(); // default kb id
			Form form = getRequest().getResourceRef().getQueryAsForm();
			for (String key : form.getValuesMap().keySet()) {
				getRequest().getAttributes().put(key, form.getFirstValue(key));
			}
			String kbName = form.getFirstValue("ontology");
			if (kbName != null && kbName.length() > 0) {
				BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
				kbId = basicFunctions.getKnowledgeBaseID(kbName, getOntoquestContext());
			}

			String callback = (String) getRequest().getAttributes().get("callback");
			if (callback != null) {
				metaData = new SearchReconcileMetaData();
				this.callback = callback;
				return;
			}

			String query = (String) getRequest().getAttributes().get("query");
			if (query != null) {
				query = Reference.decode(query);
				JSONObject queryObj = parseQuery(query);
				search(queryObj, kbId);
				return;
			}

			// default action
			metaData = new SearchReconcileMetaData();
		} catch (Throwable oe) {
			setAppException(oe);
			// throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
			// oe.getMessage(), oe);
		}
	}

	protected JSONObject parseQuery(String query) throws OntoquestException,
	JSONException {
		if (query == null || query.length() == 0)
			throw new OntoquestException("Input query is empty!");

		if (query.charAt(0) != '{' || query.charAt(query.length() - 1) != '}') {
			// http://foo.com/bar/reconcile?query=...string...
			JSONObject obj = new JSONObject();
			obj.put("query", query);
			obj.put(KEY_IS_SINGLE, true);
			return obj;
		} else {
			JSONObject obj = new JSONObject(query); // single or multiple
			if (obj.has("query")) {
				obj.put(KEY_IS_SINGLE, true); // single
			}
			return obj;
		}
	}

	protected void search(JSONObject queryObj, int kbId)
	throws JSONException,
	OntoquestException {
		if (queryObj.has(KEY_IS_SINGLE)) { // single
			singleSearchResult = SearchReconcileBean.doSingleQuery(queryObj,
					getRequestAttributes(), kbId, getOntoquestContext());
		} else { // multiple
			multipleSearchResult = SearchReconcileBean.doMultipleQueries(queryObj,
					getRequestAttributes(), kbId, getOntoquestContext());
		}
	}

	@Get("json")
	public Representation toJSON() {
		if (getAppException() != null)
			return toErrorJSON();

		try {

			JSONObject result = null;
			if (metaData != null) {
				result = convertMetaData(metaData);
				String jsonText = result.toString();
				StringBuilder finalResponse = new StringBuilder();
				if (callback != null)
					finalResponse.append(callback).append("(");
				finalResponse.append(jsonText);
				if (callback != null)
					finalResponse.append(')');
				return new StringRepresentation(finalResponse.toString());
			} else if (singleSearchResult != null)
				result = convertSingleResult(singleSearchResult);
			else if (multipleSearchResult != null)
				result = convertMultipleResults(multipleSearchResult);
			else {
				result = new JSONObject();
			}
			return new JsonRepresentation(result);
		} catch (Throwable e) {
			e.printStackTrace();
			setAppException(e);
			return toErrorJSON();
		}
	}

}
