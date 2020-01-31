/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
import android.util.Pair
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.util.GeneralUtil
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * This class describe table [UserIdEmailsKeysDaoSource.TABLE_NAME_USER_ID_EMAILS_AND_KEYS] and operations with it
 *
 * @author Denis Bondarenko
 * Date: 30.07.2018
 * Time: 10:16
 * E-mail: DenBond7@gmail.com
 */
class UserIdEmailsKeysDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_USER_ID_EMAILS_AND_KEYS

  /**
   * Add information about a combination of `longId` and `email` to the database.
   *
   * @param context Interface to global information about an application environment.
   * @param longId  A `longId` value of some private key.
   * @param email   An email of some `uid` of some key.
   * @return <tt>[Uri]</tt> which contain information about an inserted row or null if the
   * row not inserted.
   */
  fun addRow(context: Context, longId: String, email: String): Uri? {
    val contentResolver = context.contentResolver
    return if (!TextUtils.isEmpty(longId) && !TextUtils.isEmpty(email) && contentResolver != null) {
      val contentValues = ContentValues()
      contentValues.put(COL_LONG_ID, longId)
      contentValues.put(COL_USER_ID_EMAIL, email.toLowerCase(Locale.getDefault()))
      contentResolver.insert(baseContentUri, contentValues)
    } else {
      null
    }
  }

  /**
   * This method add rows per single transaction. This method must be called in the non-UI thread.
   *
   * @param context Interface to global information about an application environment.
   * @param pairs   A list of [Pair], which contains information about a combination of `longId` and
   * `email`.
   * @return the number of newly created rows.
   */
  fun addRows(context: Context, pairs: List<Pair<String, String>>): Int {
    return if (!CollectionUtils.isEmpty(pairs)) {
      val contentResolver = context.contentResolver
      val contentValuesArray = arrayOfNulls<ContentValues>(pairs.size)

      for (i in pairs.indices) {
        val pair = pairs[i]
        val contentValues = ContentValues()
        contentValues.put(COL_LONG_ID, pair.first)
        contentValues.put(COL_USER_ID_EMAIL, pair.second.toLowerCase(Locale.getDefault()))
        contentValuesArray[i] = contentValues
      }

      contentResolver.bulkInsert(baseContentUri, contentValuesArray)
    } else {
      0
    }
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
    } else {
      -1
    }
  }

  /**
   * Get a list of longId using a given email.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email which will be used for searching.
   * @return A list of found longId.
   */
  fun getLongIdsByEmail(context: Context?, email: String?): List<String> {
    val longIdsList = ArrayList<String>()
    if (!TextUtils.isEmpty(email)) {
      val selection = "$COL_USER_ID_EMAIL = ?"
      val selectionArgs = arrayOf(email?.toLowerCase(Locale.getDefault()))
      val cursor = context?.contentResolver?.query(baseContentUri, null, selection, selectionArgs,
          null)

      if (cursor != null) {
        while (cursor.moveToNext()) {
          longIdsList.add(cursor.getString(cursor.getColumnIndex(COL_LONG_ID)))
        }
      }

      cursor?.close()
    }

    return longIdsList
  }

  /**
   * Remove pairs for the given keys
   *
   * @param context         Interface to global information about an application environment
   * @param keys            A list of keys
   * @return the [ContentProviderResult] array.
   */
  fun removePairs(context: Context, keys: List<NodeKeyDetails>): Array<ContentProviderResult> {
    val contentProviderOperations = ArrayList<ContentProviderOperation>()

    for (key in keys) {
      val pairs = ArrayList<Pair<String, String>>()
      for (pgpContact in key.pgpContacts) {
        pairs.add(Pair.create(key.longId, pgpContact.email.toLowerCase(Locale.getDefault())))
      }

      for (pair in pairs) {
        contentProviderOperations.add(ContentProviderOperation.newDelete(baseContentUri)
            .withSelection("$COL_LONG_ID = ? AND $COL_USER_ID_EMAIL = ? ",
                arrayOf(pair.first, pair.second))
            .withYieldAllowed(true)
            .build())
      }
    }

    return context.contentResolver.applyBatch(baseContentUri.authority
        ?: "", contentProviderOperations)
  }

  companion object {
    const val TABLE_NAME_USER_ID_EMAILS_AND_KEYS = "user_id_emails_and_keys"

    const val COL_LONG_ID = "long_id"
    const val COL_USER_ID_EMAIL = "user_id_email"

    const val SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_USER_ID_EMAILS_AND_KEYS + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_LONG_ID + " VARCHAR(16) NOT NULL, " +
        COL_USER_ID_EMAIL + " VARCHAR(20) NOT NULL " + ");"

    const val INDEX_LONG_ID_USER_ID_EMAIL = (UNIQUE_INDEX_PREFIX + COL_LONG_ID + "_" +
        COL_USER_ID_EMAIL + "_in_" + TABLE_NAME_USER_ID_EMAILS_AND_KEYS + " ON " + TABLE_NAME_USER_ID_EMAILS_AND_KEYS
        + " (" + COL_LONG_ID + ", " + COL_USER_ID_EMAIL + ")")

    fun genPairs(context: Context, keyDetails: NodeKeyDetails, contacts: List<PgpContact>,
                 daoSource: ContactsDaoSource): List<Pair<String, String>> {
      val pairs = mutableListOf<Pair<String, String>>()
      for (pgpContact in contacts) {
        pgpContact.pubkey = keyDetails.publicKey
        val temp = daoSource.getPgpContact(context, pgpContact.email)
        if (GeneralUtil.isEmailValid(pgpContact.email) && temp == null) {
          ContactsDaoSource().addRow(context, pgpContact)
          //todo-DenBond7 Need to resolve a situation with different public keys.
          //For example we can have a situation when we have to different public
          // keys with the same email
        }

        pairs.add(Pair.create(keyDetails.longId, pgpContact.email))
      }
      return pairs
    }
  }
}
