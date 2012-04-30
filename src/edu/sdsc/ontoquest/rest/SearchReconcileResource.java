package edu.sdsc.ontoquest.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbBasicFunctions;

/**
 * Google Refine Reconcile Services
 * <p>
 * $Id: SearchReconcileResource.java,v 1.3 2012-04-30 22:44:12 xqian Exp $
 * 
 */
public class SearchReconcileResource extends BaseResource {
	protected final String KEY_IS_SINGLE = "is_single";
	private List<SearchReconcileBean> singleSearchResult = null;
	private HashMap<String, List<SearchReconcileBean>> multipleSearchResult = null;
	private long queryDurationMillis = 0;
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
			JSONArray typeArray = new JSONArray();
			JSONObject typeObj = new JSONObject().put("id", item.getCategoryName()).put("name", item.getCategoryLabel());
			typeArray.put(typeObj);
			
			JSONObject obj = new JSONObject();
			obj.put("id", item.getName()).put("name", item.getLabel() + "|" + item.getName())
			.put("type", typeArray).put("score", item.getScore())
			.put("match", item.getScore() > 0.5);
			array.put(obj);
		}
		JSONObject result = new JSONObject().put("result", array);
		
		return result;
	}

//	@Override
//	protected void doInit() throws ResourceException {
//		Form form = getRequest().getResourceRef().getQueryAsForm();
////		Map<String, Object> requestAttributes = getRequest().getAttributes();
////		for (String key : getRequest().getAttributes().keySet()) {
////			Object value = requestAttributes.get(key);
////			if (value instanceof String)
////				form.add(key, (String)value);
////		}
//		doQuery(form);
//		
////		try {;
////			OntoquestApplication application = (OntoquestApplication) getApplication();
////			int kbId = application.getKbId(); // default kb id
////			Form form = getRequest().getResourceRef().getQueryAsForm();
////			for (String key : form.getValuesMap().keySet()) {
////				getRequest().getAttributes().put(key, form.getFirstValue(key));
////			}
////			String kbName = form.getFirstValue("ontology");
////			if (kbName != null && kbName.length() > 0) {
////				BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
////				kbId = basicFunctions.getKnowledgeBaseID(kbName, getOntoquestContext());
////			}
////
////			String callback = (String) getRequest().getAttributes().get("callback");
////			if (callback != null) {
//////				metaData = new SearchReconcileMetaData();
////				this.callback = callback;
////			}
////
////			String query = (String) getRequest().getAttributes().get("query");
////			if (query != null) {
////				query = Reference.decode(query);
////				JSONObject queryObj = parseQuery(query);
////				search(queryObj, kbId);
////			} else {
////
////				// default action
////				metaData = new SearchReconcileMetaData();
////			}
////		} catch (Throwable oe) {
////			setAppException(oe);
////			oe.printStackTrace();
////			// throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
////			// oe.getMessage(), oe);
////		}
//	}

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
		long time1 = System.currentTimeMillis();
		if (queryObj.has(KEY_IS_SINGLE)) { // single
			singleSearchResult = SearchReconcileBean.doSingleQuery(queryObj,
					getRequestAttributes(), kbId, getOntoquestContext());
		} else { // multiple
			multipleSearchResult = SearchReconcileBean.doMultipleQueries(queryObj,
					getRequestAttributes(), kbId, getOntoquestContext());
		}
		long time2 = System.currentTimeMillis();
		queryDurationMillis = time2-time1;
	}

	/**
	 * Allow a POST http request
	 * 
	 * @return
	 */
	public boolean allowPost() {
	 return true;
	}

	/**
	 * Allow a GET http request
	 * 
	 * @return
	 */
	public boolean allowGet() {
	 return true;
	}
	
	@Post
	public Representation accept(Representation entity) {
		Form form = new Form(entity);  
		doQuery(form);
		return represent();
	}
	
	@Get
	public Representation accept() {
		Form form = getRequest().getResourceRef().getQueryAsForm();
		Map<String, Object> requestAttributes = getRequest().getAttributes();
		for (String key : getRequest().getAttributes().keySet()) {
			Object value = requestAttributes.get(key);
			if (value instanceof String)
				form.add(key, (String)value);
		}
		doQuery(form);
		return represent();
	}
	
	public void doQuery(Form form) {
		try {
			
//			Iterator<Parameter> it = form.iterator();
//			while (it.hasNext()) {
//				Parameter p = it.next();
//				System.out.println(p.getName() + ": " + p.getFirst() + ", " + p.getValue());
//			}
			
			OntoquestApplication application = (OntoquestApplication) getApplication();
			int kbId = application.getKbId(); // default kb id
			String kbName = form.getFirstValue("ontology");
			if (kbName != null && kbName.length() > 0) {
				BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
				kbId = basicFunctions.getKnowledgeBaseID(kbName, getOntoquestContext());
			}

			String callback = form.getFirstValue("callback");
			if (callback != null) {
//				metaData = new SearchReconcileMetaData();
				this.callback = callback;
			}

			String query = form.getFirstValue("query");
			if (query == null) {
				query = form.getFirstValue("queries");
			}
			
			if (query != null) {
				query = Reference.decode(query);
				JSONObject queryObj = parseQuery(query);
				search(queryObj, kbId);
			} else {
				
				// default action
				metaData = new SearchReconcileMetaData();
			}
			
		} catch (Throwable oe) {
			setAppException(oe);
//			oe.printStackTrace();
		}
	}
	
//	@Get("json")
	public Representation represent() {
		if (getAppException() != null)
			return toErrorJSON();

		try {

			JSONObject result = null;
			if (metaData != null) {
				result = convertMetaData(metaData);
				return toStringRepresentation(result);
			} else if (singleSearchResult != null) {
				result = convertSingleResult(singleSearchResult);
				return toStringRepresentation(result);
			} else if (multipleSearchResult != null) {
				result = convertMultipleResults(multipleSearchResult);
				return toStringRepresentation(result);
			} else {
				result = new JSONObject();
			}
			return new JsonRepresentation(result);
		} catch (Throwable e) { 
			e.printStackTrace();
			setAppException(e);
			return toErrorJSON();
		}
	}

	private StringRepresentation toStringRepresentation(JSONObject result) throws JSONException {
		result.put("labs", false);
		result.put("duration", queryDurationMillis);
		String jsonText = result.toString(4);
		StringBuilder finalResponse = new StringBuilder();
		if (callback != null)
			finalResponse.append(callback).append("(");
		finalResponse.append(jsonText);
		if (callback != null)
			finalResponse.append(')');
		return new StringRepresentation(finalResponse.toString());
	}
}
