/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class describe concurrent database access.
 * <p>
 * See http://www.dmytrodanylyk.com/concurrent-database-access/
 *
 * @author Denis Bondarenko
 * Date: 13.05.2017
 * Time: 11:23
 * E-mail: DenBond7@gmail.com
 */
public class FlowCryptDatabaseManager {

  private static FlowCryptDatabaseManager instance;
  private static SQLiteOpenHelper sqLiteOpenHelper;
  private AtomicInteger openCounter = new AtomicInteger();

  public static synchronized void initializeInstance(SQLiteOpenHelper helper) {
    if (instance == null) {
      instance = new FlowCryptDatabaseManager();
      sqLiteOpenHelper = helper;
    }
  }

  public static synchronized FlowCryptDatabaseManager getInstance() {
    if (instance == null) {
      throw new IllegalStateException(FlowCryptDatabaseManager.class.getSimpleName() +
          " is not initialized, call initializeInstance(..) method first.");
    }
    return instance;
  }

  public static SQLiteOpenHelper getSqLiteOpenHelper() {
    return sqLiteOpenHelper;
  }

  public synchronized int registerWorkWithDatabase() {
    return openCounter.incrementAndGet();
  }

  public synchronized int unregisterWorkWithDatabase() {
    return openCounter.get() != 0 ? openCounter.decrementAndGet() : 0;
  }

  public synchronized SQLiteDatabase getWritableDatabase() {
    return sqLiteOpenHelper.getWritableDatabase();
  }

  public synchronized SQLiteDatabase getReadableDatabase() {
    return getWritableDatabase();
  }

  public synchronized void close() {
    if (openCounter.decrementAndGet() == 0) {
      sqLiteOpenHelper.close();
    }
  }

  public AtomicInteger getOpenCounter() {
    return openCounter;
  }
}

