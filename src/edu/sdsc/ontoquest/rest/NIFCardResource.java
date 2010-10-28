package edu.sdsc.ontoquest.rest;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbUtility;
import edu.sdsc.ontoquest.query.Utility;

/**
 * @version $Id: NIFCardResource.java,v 1.1 2010-10-28 06:29:54 xqian Exp $
 *
 */
public class NIFCardResource extends BaseResource {
  
  String resultText = null;
  String[] colNames = null;
  @Override
  protected void doInit() throws ResourceException {
    try {
      OntoquestApplication application = (OntoquestApplication)getApplication();
      Form form = getRequest().getResourceRef().getQueryAsForm();
      for (String key : form.getValuesMap().keySet()) {
        getRequest().getAttributes().put(key, form.getFirstValue(key));
      }
      
      String listType = (String) getRequest().getAttributes().get("listType");
      if (listType != null) {
        listType = Reference.decode(listType);
      }
          
      listData(listType);
    } catch (Throwable oe) {
      setAppException(oe);
      //      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, oe.getMessage(), oe);
    }
  }

  protected void listData(String listType) throws Exception {
    String sql = null;
    if (listType.equalsIgnoreCase("list_anatomical_structures")) {
      sql = "select * from nif_brain_region";
    } else if (listType.equalsIgnoreCase("list_cells")) {
      sql = "select * from nif_cell";
    } else if (listType.equalsIgnoreCase("list_cell_locations")) {
      sql = "select * from nif_brain_cell";
    }
    
    Connection conn = null;
    ResultSet rs = null;
    try {
      Utility.checkBlank(sql, OntoquestException.Type.EXECUTOR, 
          "Invalid statement: "+sql + "; command: " + listType);
      conn = DbUtility.getDBConnection(getOntoquestContext());
      Statement stmt = conn.createStatement();
      rs = stmt.executeQuery(sql);
      ResultSetMetaData rsmd = rs.getMetaData();
      int colCount = rsmd.getColumnCount();
      StringBuilder sb = new StringBuilder();
      while (rs.next()) {
        for (int i = 1; i<=colCount; i++) {
          String s = rs.getString(i);
          if (s == null || s.equalsIgnoreCase("null"))
            s = "";
          if (s.length() != 0) {
            s = s.replace('\t', ' ').replace('\n', ' ').replace("\r", "");
          }
          sb.append(s).append('\t');
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append('\n');
//        System.out.println(sb);
      }
//      System.out.println("AAAAAA");
      resultText = sb.toString();
      colNames = new String[rsmd.getColumnCount()];
      for (int i = 0; i<colCount; i++) {
        colNames[i] = rsmd.getColumnName(i+1);
      }
    } finally {
      try {
        rs.close();
      } catch (Exception e2) {}
      DbUtility.releaseDbConnection(conn, getOntoquestContext());
    }
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
      
      Element columnsElem = d.createElement("columns");
      dataElem.appendChild(columnsElem);
      
      for (String col : colNames) {
        Element columnElem = d.createElement("column");
        columnElem.appendChild(d.createTextNode(col));
        columnsElem.appendChild(columnElem);
      }
      
      Element valueElem = d.createElement("value");
      valueElem.appendChild(d.createTextNode(resultText));
      dataElem.appendChild(valueElem);
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
