package edu.sdsc.ontoquest.db;

import edu.sdsc.ontoquest.Context;

/**
 * @version $Id: DbContext.java,v 1.1 2010-10-28 06:30:08 xqian Exp $
 *
 */
public class DbContext extends Context {
  private DbConnectionPool conPool = null;

  public DbContext(DbConnectionPool conPool) {
    this.conPool = conPool;
  }
  
  /**
   * @return the conPool
   */
  public DbConnectionPool getConPool() {
    return conPool;
  }

  /**
   * @param conPool the conPool to set
   */
  public void setConPool(DbConnectionPool conPool) {
    this.conPool = conPool;
  }
  
  
}
