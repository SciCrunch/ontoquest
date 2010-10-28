package edu.sdsc.ontoquest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages resource set. Close and remove the 
 * resource set when it is expired.
 * 
 * @version $Id: ResourceSetManager.java,v 1.1 2010-10-28 06:30:00 xqian Exp $
 */
public class ResourceSetManager {
  private static ResourceSetManager _instance = null;
  private HashMap<Integer, ResourceSet> _rsMap = null;
  private static Log _logger = LogFactory.getLog(ResourceSetManager.class);
  private final ScheduledExecutorService _cleanerScheduler = 
    Executors.newScheduledThreadPool(1);
  private ScheduledFuture<?> _rsCleanerHandler = null;
  private long _cleanerInitialDelay = 60; // 60 seconds
  private long _cleanerPeriod = 120; // 120 seconds
  
  private ResourceSetManager() {
    _rsMap = new HashMap<Integer, ResourceSet>();
    
    // schedule cleaner to start after _cleanerInitialDelay and
    // re-run periodically.
    final Runnable rsCleaner = new Runnable() {
      public void run() { 
        ResourceSetManager.this.removedExpiredResourceSets();
      }
    };
    _rsCleanerHandler = _cleanerScheduler.scheduleAtFixedRate(rsCleaner, _cleanerInitialDelay, 
        _cleanerPeriod, TimeUnit.SECONDS);
  }
  
  public synchronized static ResourceSetManager getInstance() {
    if (_instance == null)
      _instance = new ResourceSetManager();
    return _instance;
  }
  
  public synchronized void addResourceSet(ResourceSet rs) {
    if (rs == null) return;
    _rsMap.put(rs.getID(), rs);
  }
  
  /**
   * Removes the resource set and close it. The removed resource set can
   * not be used anymore.
   * @param rsId
   */
  public synchronized void removeResourceSet(int rsId) {
    ResourceSet rs2 = _rsMap.remove(rsId);
    if (rs2 == null) return;
    try {
      rs2.close();
    } catch (Exception e) {
    }
  }
  
  /**
   * Removes the resource set, but does not close it.
   * @param rsId
   * @return
   */
  public synchronized ResourceSet removeResourceSetWithoutClose(int rsId) {
    return _rsMap.remove(rsId);
  }
  
  public void removedExpiredResourceSets() {
    Collection<ResourceSet> rsValues = _rsMap.values();
    synchronized (_rsMap) {
      Iterator<ResourceSet> it = rsValues.iterator();
      while (it.hasNext()) {
        ResourceSet rs = it.next();
        if (rs.isExpired()) {
          // remove the resource set from the map
          it.remove(); 
          // close the result set if possible.
          try {
            rs.close();
          } catch (Exception e) {
            _logger.warn("Unable to close resource set, id = " + rs.getID(), e);
          }
        }
      }
    }
  }
  
  public synchronized void terminateResourceSetCleaner() {
    if (_rsCleanerHandler.isCancelled() || _rsCleanerHandler.isDone())
      return;
    
    _cleanerScheduler.schedule(new Runnable() {
      public void run() {
        _rsCleanerHandler.cancel(true);
      }
    }, 1, TimeUnit.SECONDS);
  }
}
