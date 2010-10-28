package edu.sdsc.ontoquest.query.executor;
/*
 *  This package is borrowed from mediator's execution engine developed 
 *  by Chris Condit. The class is modified to suit the needs of OntoQuest. 
 */
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.query.Parameter;

/**
 * 
 * This interface should be used as a minimum set of requirements for
 * implementing the variable reservoir. 
 * <p>$Id: Database.java,v 1.1 2010-10-28 06:30:41 xqian Exp $
 */
public interface Database {

  /**
   * Get version information about the data source.
   * @return A string with version information, null if failure
   */
  public abstract String getInfo();

  /**
   * Creates a new table in the variable reservoir
   * @param tableName The name of the table
   * @param columns A vector of Variable object with name and type information
   * @throws OntoquestException
   */
  public abstract void createTable(String tableName, List<? extends Parameter> columns) throws OntoquestException;

  /**
   * Drops the table called tableName from the reservoir
   * @param tableName
   * @throws OntoquestException
   */
  public abstract void deleteTable(String tableName) throws OntoquestException;

  /**
   * @param tableName
   * @return the existence of the table in the database
   */
  public abstract boolean tableExists(String tableName);

  /**
   * @param tableName
   * @return the number of records in a table, <0 if failure
   */
  public abstract int numRecords(String tableName);

  /**
   * Allow the use of arbitrary SQL execution on the database.  This
   * is primarily for debugging purposes.  If this is used in the application,
   * it means that any underlying database will need to implement that functionality
   * where possible.  As such you should add a new method to this interface, forcing
   * source specific implementation.
   * @param sql The <b>source specific</b> SQL query to run
   * @return Results in a ResultSet
   */
  /**
   * @param sql
   * @return
   * @throws SQLException
   */
  public abstract ResultSet runSQL(String sql) throws SQLException;

}

