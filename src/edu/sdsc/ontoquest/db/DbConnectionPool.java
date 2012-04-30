package edu.sdsc.ontoquest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple database connection pool.
 * @version $Id: DbConnectionPool.java,v 1.2 2012-04-30 22:43:04 xqian Exp $
 */
public class DbConnectionPool {

    boolean _initialized = false;
    private int _checkedOut = 0;
    private String _driver, _url, _usr, _passwd;
    private ArrayList<Connection> _freeConnections = null;
    public static final int DEFAULT_POOL_SIZE = 0; // unlimited connection
    private int _poolSize = DEFAULT_POOL_SIZE;
    private static Log _logger = LogFactory.getLog(DbConnectionPool.class);
    
    public DbConnectionPool(String driver, String url,
        String usr, String passwd) {
      this(driver, url, usr, passwd, DEFAULT_POOL_SIZE);
    }
    
    public DbConnectionPool(String driver, String url,
                            String usr, String passwd, int poolSize) {

      _driver = driver;
      _url = url;
      _usr = usr;
      _passwd = passwd;
      _poolSize = poolSize;
      _freeConnections = new ArrayList<Connection>();
//      init();
    }

    private void init() throws SQLException {
      try {
        if (!_initialized) {
          Class.forName(_driver);
          _initialized = true;
        }
      } catch (ClassNotFoundException cnfe) {
        throw new SQLException("Driver class not found: " + _driver);
      }
    }

    /**
     * Checks out a connection from the pool. If no free connection
     * is available, a new connection is created unless the max
     * number of connections has been reached. If a free connection
     * has been closed by the database, it's removed from the pool
     * and this method is called again recursively. If no more conncetion
     * is available, return null. This method should be used with caution,
     * because null may be returned.
     */
    protected synchronized Connection getConnection()
        throws SQLException {
      init();
      Connection con = null;
      if (_freeConnections.size() > 0) {
        // pick up the first element in the pool
        con = _freeConnections.get(0);
        _freeConnections.remove(0);
        try {
          if (con == null || con.isClosed()) {
            con = getConnection();
          } else {
            // test connection
            Statement stmt = con.createStatement();
            stmt.executeQuery("select 1+2");
            stmt.close();
//            con.getMetaData().getTableTypes(); // test connection
          }
        } catch (Throwable sqle) {
          con = getConnection();
        }
      } else if (_poolSize == 0 || _checkedOut < _poolSize) {
        con = newConnection();
      }
      if (con != null)
        _checkedOut++;
      return con;
    }

    public synchronized Connection getConnection(long timeout)
        throws SQLException {
//      long startTime = System.currentTimeMillis();
      Connection con;
      long waitedTime = 0;
      long waitInterval = 500; // wake up every half second
      while ((con = getConnection()) == null && waitedTime < timeout) {
        try {
          waitedTime += waitInterval;
          wait(waitInterval);
        } catch (InterruptedException e) {}
          if (waitedTime >= timeout) {
           // Timeout has expired
             throw new SQLException("No connection is available in connection "
                 +"pool. Consider to close those connections not in use, or increase pool size.");
          }
        }
        return con;
    }

    public synchronized void releaseConnection(Connection con) {
      if (con != null) {
        _freeConnections.add(con);
        _checkedOut--;
//        _logger.debug("Connection released. # of free connections : " + _freeConnections.size()
//            + "; # of checked-out connections: " + _checkedOut);
        notifyAll();
      }
    }

    private Connection newConnection() throws SQLException {
      Connection con = null;
      if (_usr == null) {
        con = DriverManager.getConnection(_url);
      }
      else {
        con = DriverManager.getConnection(_url, _usr, _passwd);
      }
      return con;
    }
    
    public String getDbURL() { return _url; }
  }
