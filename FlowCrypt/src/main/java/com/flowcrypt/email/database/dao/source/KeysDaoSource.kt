/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import com.flowcrypt.email.database.dao.KeysDao
import com.google.android.gms.common.util.CollectionUtils

/**
 * This class describe creating of table which has name [KeysDaoSource.TABLE_NAME_KEYS],
 * add, delete and update rows.
 *
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 12:44
 * E-mail: DenBond7@gmail.com
 */

class KeysDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_KEYS

  /**
   * Add information about a key to the database.
   *
   * @param context Interface to global information about an application environment.
   * @param keysDao The [KeysDao] object which contain information about a key.
   * @return <tt>[Uri]</tt> which contain information about an inserted row or null if the
   * row not inserted.
   */
  fun addRow(context: Context, keysDao: KeysDao?): Uri? {
    val contentResolver = context.contentResolver
    if (keysDao != null && contentResolver != null) {
      val contentValues = ContentValues()
      contentValues.put(COL_LONG_ID, keysDao.longId)
      contentValues.put(COL_SOURCE, keysDao.privateKeySourceType!!.toString())
      contentValues.put(COL_PUBLIC_KEY, keysDao.publicKey)
      contentValues.put(COL_PRIVATE_KEY, keysDao.privateKey)
      contentValues.put(COL_PASSPHRASE, keysDao.passphrase)

      return contentResolver.insert(baseContentUri, contentValues)
    } else
      return null
  }

  /**
   * Check if the key already exists in the database.
   *
   * @param context Interface to global information about an application environment.
   * @param longId  The key longid parameter.
   * @return <tt>[Boolean]</tt> true - if the key already exists in the database, false -
   * otherwise.
   */
  fun hasKey(context: Context, longId: String): Boolean {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(baseContentUri, null, "$COL_LONG_ID = ?", arrayOf(longId), null)

    var result = false

    if (cursor != null) {
      if (cursor.moveToFirst()) {
        result = true
      }
      cursor.close()
    }

    return result
  }

  /**
   * Get a list of of avalible keys longids.
   *
   * @param context Interface to global information about an application environment.
   * @return The list of avalible keys longids.
   */
  fun getAllKeysLongIds(context: Context): List<String> {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(baseContentUri, arrayOf(COL_LONG_ID), null, null, null)

    val longIds = ArrayList<String>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        longIds.add(cursor.getString(cursor.getColumnIndex(COL_LONG_ID)))
      }

      cursor.close()
    }

    return longIds
  }

  /**
   * Delete information about a private by longid.
   *
   * @param context   Interface to global information about an application environment.
   * @param keyLognId The key longid.
   * @return The count of deleted rows. Will be 1 if information about the key was deleted or -1 otherwise.
   */
  fun removeKey(context: Context, keyLognId: String): Int {
    return if (!TextUtils.isEmpty(keyLognId)) {

      val contentResolver = context.contentResolver
      contentResolver?.delete(baseContentUri, "$COL_LONG_ID = ?", arrayOf(keyLognId)) ?: -1
    } else
      -1
  }

  /**
   * This method update information about some private keys.
   *
   * @param context Interface to global information about an application environment.
   * @param keys    The list of [KeysDao] which contains information about the private keys.
   * @return the [ContentProviderResult] array.
   */
  fun updateKeys(context: Context, keys: Collection<KeysDao>): Array<ContentProviderResult> {
    val contentResolver = context.contentResolver
    return if (!CollectionUtils.isEmpty(keys)) {
      val contentProviderOperations = ArrayList<ContentProviderOperation>()
      for ((longId, _, _, privateKey, passphrase) in keys) {
        contentProviderOperations.add(ContentProviderOperation.newUpdate(baseContentUri)
            .withValue(COL_PRIVATE_KEY, privateKey)
            .withValue(COL_PASSPHRASE, passphrase)
            .withSelection("$COL_LONG_ID= ?", arrayOf(longId!!))
            .withYieldAllowed(true)
            .build())
      }
      contentResolver.applyBatch(baseContentUri.authority!!, contentProviderOperations)
    } else {
      emptyArray()
    }
  }

  /**
   * Remove keys with the given longid(s)
   *
   * @param context         Interface to global information about an application environment
   * @param longIds         A list of longid
   * @return the [ContentProviderResult] array.
   */
  fun removeKeys(context: Context, longIds: List<String>): Array<ContentProviderResult> {
    val contentProviderOperations = ArrayList<ContentProviderOperation>()

    for (longId in longIds) {
      contentProviderOperations.add(ContentProviderOperation.newDelete(baseContentUri)
          .withSelection("$COL_LONG_ID= ?", arrayOf(longId))
          .withYieldAllowed(true)
          .build())
    }

    return context.contentResolver.applyBatch(baseContentUri.authority
        ?: "", contentProviderOperations)
  }

  companion object {
    const val TABLE_NAME_KEYS = "keys"

    const val COL_LONG_ID = "long_id"
    const val COL_SOURCE = "source"
    const val COL_PUBLIC_KEY = "public_key"
    const val COL_PRIVATE_KEY = "private_key"
    const val COL_PASSPHRASE = "passphrase"

    const val KEYS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_KEYS + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_LONG_ID + " VARCHAR(16) NOT NULL, " +
        COL_SOURCE + " VARCHAR(20) NOT NULL, " +
        COL_PUBLIC_KEY + " BLOB NOT NULL, " +
        COL_PRIVATE_KEY + " BLOB NOT NULL, " +
        COL_PASSPHRASE + " varchar(100) DEFAULT NULL " + ");"

    const val CREATE_INDEX_LONG_ID_IN_KEYS = UNIQUE_INDEX_PREFIX + COL_LONG_ID + "_in_" +
        TABLE_NAME_KEYS + " ON " + TABLE_NAME_KEYS + " (" + COL_LONG_ID + ")"
  }
}
