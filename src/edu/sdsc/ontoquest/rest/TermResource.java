package edu.sdsc.ontoquest.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.BasicFunctions;
import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.OntoquestFunction;
import edu.sdsc.ontoquest.ResourceSet;
import edu.sdsc.ontoquest.db.DbBasicFunctions;
import edu.sdsc.ontoquest.db.DbConnectionPool;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.db.functions.RunSQL;
import edu.sdsc.ontoquest.rest.BaseBean.InputType;

/**
 * @version $Id: TermResource.java,v 1.1 2010-10-28 06:29:58 xqian Exp $
 *
 */
public class TermResource extends BaseResource {
  List<String> terms = null;
  public static final int DefaultResultLimit = 20;
  public static final int DefaultMaxEditDistance = 40;
  private static BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
  private static Log _logger = LogFactory.getLog(DbConnectionPool.class);

  @Override
  protected void doInit() throws ResourceException {
    try {
      OntoquestApplication application = (OntoquestApplication)getApplication();
      int kbId = application.getKbId();  // default kb id
      Form form = getRequest().getResourceRef().getQueryAsForm();
      for (String key : form.getValuesMap().keySet()) {
        getRequest().getAttributes().put(key, form.getFirstValue(key));
      }
      String kbName = form.getFirstValue("ontology");
      if (kbName != null && kbName.length() > 0) {
        BasicFunctions basicFunctions = DbBasicFunctions.getInstance();
        kbId = basicFunctions.getKnowledgeBaseID(kbName, getOntoquestContext());
      }
      
      String query = (String)getRequest().getAttributes().get("query");
      if (query != null) {
        query = Reference.decode(query);
        terms = getTermsBeginWith(query, getRequestAttributes(), kbId, getOntoquestContext());
//        return;
      }
      
      // advanced option: check if search term must be descendant of some classes
      InputType ancType = InputType.TERM;
      String inputStr = (String) getRequest().getAttributes().get("oids");     
      if (inputStr != null) {
        ancType = InputType.OID;
      } else {
        inputStr = (String)getRequest().getAttributes().get("classIds");
        if (inputStr != null) {
          ancType = InputType.NAME;
        } else {
          inputStr = (String)getRequest().getAttributes().get("terms");
          ancType = InputType.TERM;
        }
      }
      if (inputStr == null || inputStr.length() == 0)
        return; // no ancestor requirement
      
      // now, check ancestor-descendant relationship
      inputStr = Reference.decode(inputStr);
      List<int[]> ancClassIDs = getAncestorClassIDs(inputStr, ancType, kbId, application.getOntoquestContext());
      terms = filterTerms(terms, ancClassIDs, kbId, getOntoquestContext());
    } catch (Throwable oe) {
      setAppException(oe);
      //      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, oe.getMessage(), oe);
    }
    
  }
  
  public static List<int[]> getAncestorClassIDs(String ancStr, InputType ancType, int kbId, Context context) throws OntoquestException {
    List<String> ancStrList = parseAncestorStr(ancStr);
    List<int[]> ancIDs = new ArrayList<int[]>(ancStrList.size());

    if (ancType == InputType.OID) {
      for (String anc : ancStrList) {
        ancIDs.add(ClassNode.parseId(anc));
      }
      return ancIDs;
    }
    
    String[] ancStrArray = new String[ancStrList.size()];
    ancStrList.toArray(ancStrArray);
    boolean useLabel = true;
    if (ancType == InputType.NAME)
      useLabel = false;
    
    ResourceSet rs = basicFunctions.searchAllIDsByName(ancStrArray, new int[]{kbId}, true, 
                        useLabel, context, BaseBean.getVarList3());
    while (rs.next()) {
      ancIDs.add(new int[]{rs.getInt(1), rs.getInt(2)});
    }
    rs.close();
    return ancIDs;
  }
  
  private static List<String> parseAncestorStr(String ancStr) {
    StringTokenizer st = new StringTokenizer(ancStr, ";");
    List<String> results = new LinkedList<String>();
    String term = "";
    while (st.hasMoreTokens()) {
      String tmp = st.nextToken();
      if (term.length() > 0) { // partial term        
        if (tmp.endsWith("\\")){
          term = term + tmp.substring(0, tmp.length()-1);
        } else {
          term = term + tmp.substring(0, tmp.length());
          results.add(term);
          term = "";
        }
      } else {
        if (tmp.endsWith("\\")){
          term = tmp.substring(0, tmp.length()-1);
        } else {
          results.add(tmp);
        }
      }            		
    }
    return results;
  }
  
  public static List<String> getTermsBeginWith(String term, Map<String, Object> attributes, 
      int kbId, Context context) throws OntoquestException {
    try {
      int resultLimit = DefaultResultLimit;
      Object rlObj = attributes.get("result_limit");
      if (rlObj != null) {
        try {
          resultLimit = Integer.parseInt(rlObj.toString());
        } catch (Exception e) {
          // do nothing, use default
        }
      }

      String sql = "select term from (select distinct term, editdistance('"+term
      +"', term) from NIF_TERM where lower(term) like '"+
        term.toLowerCase() +"%' order by editdistance('"+term
        +"', term) limit " + resultLimit + ") t";
//      System.out.println("SQL to search term: " + sql);
      RunSQL f = new RunSQL(sql);
      ResourceSet rs = f.execute(context, BaseBean.getVarList1());
      LinkedList<String> matchedTerms = new LinkedList<String>();
      HashMap<String, Integer> tempMap = new HashMap<String, Integer>();

      while (rs.next()) {
        String t = rs.getString(1);
        if (t.toLowerCase().startsWith("regional part of"))
          continue;
        if (tempMap.get(t.toLowerCase()) == null) {
          matchedTerms.add(t);
          tempMap.put(t.toLowerCase(), 1);
        }
      }
      rs.close();

      return matchedTerms;

    } catch(Exception e) {
      return getTermsBeginWith2(term, attributes, kbId, context);
    }
  }
  
  public static List<String> getTermsBeginWith2(String term, Map<String, Object> attributes, 
      int kbId, Context context) throws OntoquestException {
    int resultLimit = DefaultResultLimit;
    Object rlObj = attributes.get("result_limit");
    if (rlObj != null) {
      try {
        resultLimit = Integer.parseInt(rlObj.toString());
      } catch (Exception e) {
        // do nothing, use default
      }
    }

    ResourceSet rs = BaseBean.getBasicFunctions().searchTerm(term, new int[]{kbId}, context, 
        BaseBean.getVarList1(), resultLimit, false, DefaultMaxEditDistance, true);
    LinkedList<String> matchedTerms = new LinkedList<String>();
//    boolean hasMatch = false;
    HashMap<String, Integer> tempMap = new HashMap<String, Integer>();

    while (rs.next()) {
//      hasMatch = true;
      String t = rs.getString(1);
      if (t.toLowerCase().startsWith("regional part of"))
        continue;
      if (tempMap.get(t.toLowerCase()) == null) {
//        NIFTerm nt = new NIFTerm();
//        if (t.contains(" "))      
//          nt.setTerm("\"" + t + "\"");                
//        else
//          nt.setTerm(t);
        matchedTerms.add(t);
        tempMap.put(t.toLowerCase(), 1);
      }
    }
    rs.close();

    return matchedTerms;
  }

  // iterate terms, keep those that are descendants of classes in ancestor list.
  public static List<String> filterTerms(List<String> terms, List<int[]> ancestors, int kbId, Context context) 
    throws OntoquestException {
    
    // get matched terms' IDs
    String[] termArray = new String[terms.size()];
    terms.toArray(termArray);
    
    HashMap<int[], String> termIDs = new HashMap<int[], String>(terms.size());
    
    ResourceSet rs = basicFunctions.searchAllIDsByName(termArray, new int[]{kbId}, true, 
                        true, context, BaseBean.getVarList3());
    while (rs.next()) {
      termIDs.put(new int[]{rs.getInt(1), rs.getInt(2)}, rs.getString(3));
    }
    rs.close();
    
    // get ID for subClassOf property
    int pid = -1;
    ResourceSet rs2 = basicFunctions.searchAllIDsByName("subClassOf", 
        new int[]{kbId}, context, BaseBean.getVarList3());
    while (rs2.next()) {
      pid = rs2.getInt(1);
    }
    rs2.close();
    
    List<String> results = new LinkedList<String>();
    for (int[] termID : termIDs.keySet()) {
      for (int[] ancID : ancestors) {
        if (isAncestor(termID, ancID, pid, context)) {
          results.add(termIDs.get(termID));
        }
      }
    }
    return results;
  }

  /**
   * If we know ancestors belong to a list of pre-defined concepts, this function
   * should run faster.
   * @param terms
   * @param ancestors
   * @param kbId
   * @param context
   * @return
   * @throws OntoquestException
   */
  public static List<String> filterTerms2(List<String> terms, List<int[]> ancestors, int kbId, Context context) 
    throws OntoquestException {
    String[] termArray = new String[terms.size()];
    terms.toArray(termArray);
    String serializedTerms = DbUtility.formatArrayToSingleQuotedStr(termArray);
    String serializedAncIDs = DbUtility.formatIDsToSingleQuotedStr(ancestors);
    
    String sql = "select term from term_category where term in (" + serializedTerms
                  + ") and (cat_rid, cat_rtid) in ("+serializedAncIDs+")";
    OntoquestFunction<ResourceSet> f = new RunSQL(sql);
    ResourceSet rs = f.execute(context, BaseBean.getVarList1());
    List<String> result = new LinkedList<String>();
    if (rs.next()) {
      result.add(rs.getString(1));
    }
    rs.close();
    return result;
    
  }
  
  /**
   * if term2 is an ancestor of term1, return true
   * @param term
   * @param ancestor
   * @return
   * @throws OntoquestException
   */
  private static boolean isAncestor(int[] term1, int[] term2, int pid, Context context) 
        throws OntoquestException {
    String sql = "select is_ancestor("+term1[0]+','+term1[1]+','+term2[0]
                 +','+term2[1]+','+pid+')';
    OntoquestFunction<ResourceSet> f = new RunSQL(sql);
    ResourceSet rs = f.execute(context, BaseBean.getVarList1());
    boolean result = false;
    if (rs.next()) {
      result = rs.getBoolean(1);
    }
    rs.close();
    return result;
  }
  
  @Get("xml")
  public Representation toXml() {
    if (getAppException() != null)
      return toErrorXml();
    try {
      DomRepresentation representation = new DomRepresentation(
              MediaType.TEXT_XML);
      // Generate a DOM document representing the item.
      Document d = representation.getDocument();
      Element succElem = d.createElement("success");
      d.appendChild(succElem);
      Element dataElem = d.createElement("data");
      succElem.appendChild(dataElem);
      
      for (String term : terms) {
        Element termElem = d.createElement("term");        
        dataElem.appendChild(termElem);
        termElem.appendChild(d.createTextNode(term));
      }
      d.normalizeDocument();

      // Returns the XML representation of this document.
      return representation;
    } catch (Throwable e) {
      e.printStackTrace();
      setAppException(e);
      return toErrorXml();
    }
  }
}
