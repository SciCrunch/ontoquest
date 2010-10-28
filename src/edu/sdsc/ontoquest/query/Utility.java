package edu.sdsc.ontoquest.query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;

import edu.sdsc.ontoquest.AllConfiguration;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbConnectionPool;

/**
 * @version $Id: Utility.java,v 1.1 2010-10-28 06:30:33 xqian Exp $
 *
 */
public class Utility {
  private static int _varIdSerial = 0;
  private static HashMap<Integer, String> _rtNameMap = new HashMap<Integer, String>();
  private static HashMap<Character, Integer> _rtIdMap = new HashMap<Character, Integer>();
  
  public static synchronized String generateVarID() {
    return "_v"+(++_varIdSerial);
  }

    /**
     * Check whether a string is null or empty.
     * @param s the string to be checked
     * @return true if the string is blank.
     */
    public static boolean isBlank(String s) {
      return (s == null || s.trim().length() == 0);
    }

    /**
     * Checks whether or not a collection is null or empty.
     * @param c
     * @return true if the collection if null or empty.
     */
    public static boolean isBlank(Collection<?> c) {
      return (c == null || c.size() == 0);
    }
    
    /**
     * Check whether a string is null or empty. If empty, throw an exception.
     * @param s the string to check
     * @param errorCode the message type
     * @throws MediatorException
     * @see net.nbirn.mediator.utils.MessageType
     */
    public static void checkBlank(String s, OntoquestException.Type t) 
        throws OntoquestException {
      if (isBlank(s))
        throw new OntoquestException(t, "The string is empty!");
    }
    
    /**
     * Check whether a string is null or empty. If empty, throw an exception.
     * @param s the string to be checked
     * @param t the message type
     * @param errorMsg the custom error message
     * @throws OntoquestException when the string is empty
     * @see net.nbirn.mediator.utils.MessageType
     */
    public static void checkBlank(String s, OntoquestException.Type t, String errorMsg) 
        throws OntoquestException {
      if (isBlank(s))
        throw new OntoquestException(t, errorMsg);
    }

    /**
     * Check whether a collection is null or empty. If empty, throw an exception.
     * @param c the collection to be checked
     * @param t the message type
     * @throws OntoquestException when the collection is empty
     * @see net.nbirn.mediator.utils.MessageType
     */
    public static void checkBlank(Collection<?> c, OntoquestException.Type t) 
        throws OntoquestException {
      checkBlank(c, t, "The collection is empty");
    }
    
    /**
     * Check whether a collection is null or empty. If empty, throw an exception.
     * @param c the collection to be checked
     * @param t the message type
     * @param errorMsg the custom error message
     * @throws OntoquestException when the collection is empty
     * @see net.nbirn.mediator.utils.MessageType
     */
    public static void checkBlank(Collection<?> c, OntoquestException.Type t, String errorMsg)
        throws OntoquestException {
      if (isBlank(c))
        throw new OntoquestException(t, errorMsg);
    }
    
    /**
     * Check whether the object <code>o</code> is null. If so, throw an exception.
     * @param o the object to be checked
     * @param t the message type
     * @param errorMsg the custom error message
     * @throws OntoquestException if the object is null
     */
    public static void checkNull(Object o, OntoquestException.Type t, String errorMsg)
        throws OntoquestException {
      if (o == null)
        throw new OntoquestException(t, errorMsg);
    }
    
    /**
     * Check whether the object <code>o</code> is null. If so, throw an exception.
     * @param o the object to be checked
     * @param t the message type
     * @throws OntoquestException if the object is null
     */
    public static void checkNull(Object o, OntoquestException.Type t)
        throws OntoquestException {
      if (o == null)
        throw new OntoquestException(t, "Null Object");
    }
    
    public static <T> void checkClass(Object o, Class<T> expectedClass, OntoquestException.Type t) 
        throws OntoquestException {
      
      if (!expectedClass.isInstance(o))
        throw new OntoquestException(t, "Encountered object " + o.getClass().getName() + 
             " while expecting an instance of class "+expectedClass.getName());
    }
    
    public static String getResourceTypeName(int rtid, DbConnectionPool conPool) throws OntoquestException {
      String rtName = _rtNameMap.get(rtid);
      if (rtName == null) {
        // load _rtNameMap
        loadResourceTypes(conPool);
        rtName = _rtNameMap.get(rtid);
      }
      Utility.checkNull(rtName, OntoquestException.Type.EXECUTOR, 
          "Unable to find name for resource type id "+rtid);
      return rtName;
    }
    
    public static int getResourceTypeID(char rtSymbol, DbConnectionPool conPool) throws OntoquestException {
      Integer rtid = _rtIdMap.get(rtSymbol);
      if (rtid == null) {
        // load _rtIdMap
        loadResourceTypes(conPool);
        rtid = _rtIdMap.get(rtSymbol);
      }
      Utility.checkNull(rtid, OntoquestException.Type.EXECUTOR, 
          "Unable to find id for resource type symbol "+rtSymbol);
      return rtid;
    }
    
//    public static boolean isClassType(int rtid, DbConnectionPool conPool) throws OntoquestException {
//      String rtName = getResourceTypeName(rtid, conPool);
//      if (rtName.equalsIgnoreCase("primitiveclass"))
//    }
    
    private synchronized static void loadResourceTypes(DbConnectionPool conPool)
        throws OntoquestException {
      _rtNameMap.clear();
      _rtIdMap.clear();
      
      Connection conn = null;
      ResultSet rs = null;
      try {
        conn = conPool.getConnection(1000);
        String sql = AllConfiguration.getConfig().getString(
            "query.getResourceTypes");
        Utility.checkBlank(sql, OntoquestException.Type.BACKEND);
        Statement stmt = conn.createStatement();
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
          int rtid = rs.getInt(1);
          String rtName = rs.getString(2);
          char rtSymbol = rs.getString(3).charAt(0);
          _rtNameMap.put(rtid, rtName);
          _rtIdMap.put(rtSymbol, rtid);
        }
        rs.close();
      } catch (Exception e) {
        if (!(e instanceof OntoquestException))
          throw new OntoquestException(OntoquestException.Type.BACKEND,
              "Failed to retrieve resource type names from backend database", e);
        else
          throw (OntoquestException) e; // throw ontoquest exception up
      } finally {
        try {
          rs.close();
        } catch (Exception e2) {
        }          
        conPool.releaseConnection(conn);
      }
    }
  }
