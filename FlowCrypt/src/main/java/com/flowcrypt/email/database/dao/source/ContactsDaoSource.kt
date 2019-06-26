/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import com.flowcrypt.email.model.EmailAndNamePair
import com.flowcrypt.email.model.PgpContact
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * This class describe creating of table which has name
 * [ContactsDaoSource.TABLE_NAME_CONTACTS], add, delete and update rows.
 *
 * @author DenBond7
 * Date: 17.05.2017
 * Time: 12:22
 * E-mail: DenBond7@gmail.com
 */

class ContactsDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_CONTACTS

  fun addRow(context: Context, pgpContact: PgpContact?): Uri? {
    val contentResolver = context.contentResolver
    return if (pgpContact != null && contentResolver != null) {
      val contentValues = prepareContentValues(pgpContact)

      contentResolver.insert(baseContentUri, contentValues)
    } else
      null
  }

  /**
   * This method add rows per single transaction. This method must be called in the non-UI thread.
   *
   * @param context Interface to global information about an application environment.
   * @param pairs   A list of [EmailAndNamePair] objects which will be wrote to the database.
   * @return the number of newly created rows.
   */
  fun addRows(context: Context, pairs: Collection<EmailAndNamePair>): Int {
    if (!CollectionUtils.isEmpty(pairs)) {
      val contentResolver = context.contentResolver
      val contentValuesArray = arrayOfNulls<ContentValues>(pairs.size)

      var i = 0
      for ((email, name) in pairs) {
        val contentValues = ContentValues()
        contentValues.put(COL_EMAIL, email!!.toLowerCase())
        contentValues.put(COL_NAME, name)
        contentValues.put(COL_HAS_PGP, false)
        contentValuesArray[i] = contentValues
        i++
      }

      return contentResolver.bulkInsert(baseContentUri, contentValuesArray)
    } else
      return 0
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context        Interface to global information about an application environment.
   * @param pgpContactList A list of [PgpContact] objects which will be wrote to the database.
   * @return the number of newly created rows.
   */
  fun addRows(context: Context, pgpContactList: List<PgpContact>): Int {
    return if (!CollectionUtils.isEmpty(pgpContactList)) {
      val contentResolver = context.contentResolver
      val contentValuesArray = arrayOfNulls<ContentValues>(pgpContactList.size)

      for (i in pgpContactList.indices) {
        contentValuesArray[i] = prepareContentValues(pgpContactList[i])
      }

      contentResolver.bulkInsert(baseContentUri, contentValuesArray)
    } else
      0
  }

  /**
   * This method add rows per single transaction using [android.content.ContentProvider.applyBatch]
   *
   * @param context  Interface to global information about an application environment.
   * @param contacts A list of [PgpContact] objects.
   * @return the [ContentProviderResult] array.
   */
  fun addRowsUsingApplyBatch(context: Context?, contacts: List<PgpContact>): Array<ContentProviderResult>? {
    if (context == null) {
      return null
    }

    val contentResolver = context.contentResolver
    if (!CollectionUtils.isEmpty(contacts)) {
      val contentValuesArray = arrayOfNulls<ContentValues>(contacts.size)

      for (i in contacts.indices) {
        contentValuesArray[i] = prepareContentValues(contacts[i])
      }

      val contentProviderOperationList = ArrayList<ContentProviderOperation>()
      for (contentValues in contentValuesArray) {
        contentProviderOperationList.add(ContentProviderOperation.newInsert(baseContentUri)
            .withValues(contentValues)
            .withYieldAllowed(true)
            .build())
      }
      return contentResolver.applyBatch(baseContentUri.authority!!, contentProviderOperationList)
    } else
      return emptyArray()
  }

  /**
   * Generate a [PgpContact] object from the current cursor position.
   *
   * @param cursor The [Cursor] which contains information about [PgpContact].
   * @return A generated [PgpContact].
   */
  fun getCurrentPgpContact(cursor: Cursor): PgpContact {
    return PgpContact(
        cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
        cursor.getString(cursor.getColumnIndex(COL_NAME)),
        cursor.getString(cursor.getColumnIndex(COL_PUBLIC_KEY)),
        cursor.getInt(cursor.getColumnIndex(COL_HAS_PGP)) == 1,
        cursor.getString(cursor.getColumnIndex(COL_CLIENT)),
        cursor.getString(cursor.getColumnIndex(COL_FINGERPRINT)),
        cursor.getString(cursor.getColumnIndex(COL_LONG_ID)),
        cursor.getString(cursor.getColumnIndex(COL_KEYWORDS)),
        cursor.getInt(cursor.getColumnIndex(COL_LAST_USE))
    )
  }

  /**
   * Get a [PgpContact] object from the database by an email.
   *
   * @param email The email of the [PgpContact].
   * @return A [PgpContact] object.
   */
  fun getPgpContact(context: Context, email: String?): PgpContact? {
    if (email == null) {
      return null
    }

    val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email.toLowerCase()

    val contentResolver = context.contentResolver
    val selection = "$COL_EMAIL = ?"
    val cursor = contentResolver.query(baseContentUri, null, selection, arrayOf(emailInLowerCase), null)

    var pgpContact: PgpContact? = null

    if (cursor != null) {
      if (cursor.moveToFirst()) {
        pgpContact = getCurrentPgpContact(cursor)
      }
      cursor.close()
    }

    return pgpContact
  }

  /**
   * Get all [PgpContact]s from the database.
   *
   * @return A list of [PgpContact] objects.
   */
  fun getAllPgpContacts(context: Context): List<PgpContact> {
    val pgpContacts = ArrayList<PgpContact>()

    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(baseContentUri, null, null, null, null)

    if (cursor != null) {
      while (cursor.moveToNext()) {
        pgpContacts.add(getCurrentPgpContact(cursor))
      }
      cursor.close()
    }

    return pgpContacts
  }

  /**
   * Get a list of [PgpContact] objects from the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param emails  A list of emails.
   * @return <tt>List<PgpContact></PgpContact></tt> Return a list of existed(created) [PgpContact]
   * objects from the search by emails.
   */
  fun getPgpContacts(context: Context, emails: MutableList<String>): List<PgpContact> {
    val iterator = emails.listIterator()
    while (iterator.hasNext()) {
      iterator.set(iterator.next().toLowerCase())
    }

    val selection = COL_EMAIL + " IN " + prepareSelection(emails)
    val selectionArgs = emails.toTypedArray()
    val cursor = context.contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    val pgpContacts = ArrayList<PgpContact>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        pgpContacts.add(getCurrentPgpContact(cursor))
      }
    }

    cursor?.close()

    return pgpContacts
  }

  /**
   * Update information about some [PgpContact].
   *
   * @param context    Interface to global information about an application environment.
   * @param pgpContact A new information of [PgpContact] in the database.
   * @return The count of updated rows. Will be 1 if information about [PgpContact] was
   * updated or -1 otherwise.
   */
  fun updatePgpContact(context: Context, pgpContact: PgpContact?): Int {
    val contentResolver = context.contentResolver
    return if (pgpContact != null && contentResolver != null) {
      val contentValues = ContentValues()
      contentValues.put(COL_NAME, pgpContact.name)
      contentValues.put(COL_PUBLIC_KEY, pgpContact.pubkey)
      contentValues.put(COL_HAS_PGP, pgpContact.hasPgp)
      contentValues.put(COL_CLIENT, pgpContact.client)
      contentValues.put(COL_FINGERPRINT, pgpContact.fingerprint)
      contentValues.put(COL_LONG_ID, pgpContact.longid)
      contentValues.put(COL_KEYWORDS, pgpContact.keywords)

      val selection = "$COL_EMAIL = ?"
      val selectionArgs = arrayOf(pgpContact.email.toLowerCase())
      contentResolver.update(baseContentUri, contentValues, selection, selectionArgs)
    } else
      -1
  }

  /**
   * Update information about the given [PgpContact] in the local database.
   *
   * @param context       Interface to global information about an application environment.
   * @param localContact  A local copy of [PgpContact] in the database.
   * @param remoteContact A new information of [PgpContact] from the attester sever.
   * @return The count of updated rows. Will be 1 if information about [PgpContact] was
   * updated or -1 otherwise.
   */
  fun updatePgpContact(context: Context, localContact: PgpContact?, remoteContact: PgpContact?): Int {
    val contentResolver = context.contentResolver
    if (localContact != null && remoteContact != null && contentResolver != null) {
      val contentValues = ContentValues()
      if (TextUtils.isEmpty(localContact.name)) {
        if (localContact.email.equals(remoteContact.email, ignoreCase = true)) {
          contentValues.put(COL_NAME, remoteContact.name)
        }
      }

      contentValues.put(COL_PUBLIC_KEY, remoteContact.pubkey)
      contentValues.put(COL_HAS_PGP, remoteContact.hasPgp)
      contentValues.put(COL_CLIENT, remoteContact.client)
      contentValues.put(COL_FINGERPRINT, remoteContact.fingerprint)
      contentValues.put(COL_LONG_ID, remoteContact.longid)
      contentValues.put(COL_KEYWORDS, remoteContact.keywords)

      val selection = "$COL_EMAIL = ?"
      val selectionArgs = arrayOf(localContact.email.toLowerCase())
      return contentResolver.update(baseContentUri, contentValues, selection, selectionArgs)
    } else
      return -1
  }

  /**
   * This method update cached contacts.
   *
   * @param context Interface to global information about an application environment.
   * @param pairs   A list of [EmailAndNamePair] objects.
   * @return the [ContentProviderResult] array.
   */
  fun updatePgpContacts(context: Context, pairs: Collection<EmailAndNamePair>): Array<ContentProviderResult> {
    val contentResolver = context.contentResolver
    return if (!CollectionUtils.isEmpty(pairs)) {
      val contentProviderOperationList = ArrayList<ContentProviderOperation>()
      for ((email, name) in pairs) {
        contentProviderOperationList.add(ContentProviderOperation.newUpdate(baseContentUri)
            .withValue(COL_NAME, name)
            .withSelection("$COL_EMAIL= ?", arrayOf(email!!.toLowerCase()))
            .withYieldAllowed(true)
            .build())
      }
      contentResolver.applyBatch(baseContentUri.authority!!, contentProviderOperationList)
    } else {
      emptyArray()
    }
  }

  /**
   * This method update cached contacts.
   *
   * @param context        Interface to global information about an application environment.
   * @param pgpContactList A list of [PgpContact] objects.
   * @return the [ContentProviderResult] array.
   */
  fun updatePgpContacts(context: Context, pgpContactList: List<PgpContact>): Array<ContentProviderResult> {
    val contentResolver = context.contentResolver
    if (!CollectionUtils.isEmpty(pgpContactList)) {
      val list = ArrayList<ContentProviderOperation>()
      for ((email, name, pubkey, hasPgp, client, fingerprint, longid, keywords, lastUse) in pgpContactList) {
        list.add(ContentProviderOperation.newUpdate(baseContentUri)
            .withValue(COL_NAME, name)
            .withValue(COL_PUBLIC_KEY, pubkey)
            .withValue(COL_HAS_PGP, hasPgp)
            .withValue(COL_CLIENT, client)
            .withValue(COL_FINGERPRINT, fingerprint)
            .withValue(COL_LONG_ID, longid)
            .withValue(COL_KEYWORDS, keywords)
            .withValue(COL_LAST_USE, lastUse)
            .withSelection("$COL_EMAIL= ?", arrayOf(email.toLowerCase()))
            .withYieldAllowed(true)
            .build())
      }
      return contentResolver.applyBatch(baseContentUri.authority!!, list)
    } else {
      return emptyArray()
    }
  }

  /**
   * Update a last use entry of [PgpContact].
   *
   * @param context Interface to global information about an application environment.
   * @param contact A contact email in the database.
   * @return The count of updated rows. Will be 1 if information about [PgpContact] was
   * updated or -1 otherwise.
   */
  fun updateLastUse(context: Context, contact: String): Int {
    val contentResolver = context.contentResolver
    return if (!TextUtils.isEmpty(contact) && contentResolver != null) {
      val contentValues = ContentValues()
      contentValues.put(COL_LAST_USE, System.currentTimeMillis())

      val where = "$COL_EMAIL = ?"
      val selectionArgs = arrayOf(contact.toLowerCase())
      contentResolver.update(baseContentUri, contentValues, where, selectionArgs)
    } else {
      -1
    }
  }

  /**
   * Update a name of the email entry in the database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email in the database.
   * @param name    A new information about name of the email.
   * @return The count of updated rows. Will be 1 if information was updated or -1 otherwise.
   */
  fun updateNameOfPgpContact(context: Context, email: String, name: String?): Int {
    val contentResolver = context.contentResolver
    return if (contentResolver != null) {
      val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email.toLowerCase()

      val contentValues = ContentValues()
      contentValues.put(COL_NAME, name)

      val where = "$COL_EMAIL = ?"
      contentResolver.update(baseContentUri, contentValues, where, arrayOf(emailInLowerCase))
    } else {
      -1
    }
  }

  /**
   * Delete a [PgpContact] object from the database by an email.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email of the [PgpContact].
   * @return The count of deleted rows. Will be 1 if a contact was deleted or -1 otherwise.
   */
  fun deletePgpContact(context: Context, email: String): Int {
    val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email.toLowerCase()

    val contentResolver = context.contentResolver
    return contentResolver?.delete(baseContentUri, "$COL_EMAIL = ?", arrayOf(emailInLowerCase)) ?: -1
  }

  private fun prepareContentValues(pgpContact: PgpContact): ContentValues {
    val contentValues = ContentValues()
    contentValues.put(COL_EMAIL, pgpContact.email.toLowerCase())
    contentValues.put(COL_NAME, pgpContact.name)
    contentValues.put(COL_PUBLIC_KEY, pgpContact.pubkey)
    contentValues.put(COL_HAS_PGP, pgpContact.hasPgp)
    contentValues.put(COL_CLIENT, pgpContact.client)
    contentValues.put(COL_FINGERPRINT, pgpContact.fingerprint)
    contentValues.put(COL_LONG_ID, pgpContact.longid)
    contentValues.put(COL_KEYWORDS, pgpContact.keywords)
    contentValues.put(COL_LAST_USE, pgpContact.lastUse)
    return contentValues
  }

  companion object {
    const val CLIENT_FLOWCRYPT = "flowcrypt"
    const val CLIENT_PGP = "pgp"

    const val TABLE_NAME_CONTACTS = "contacts"

    const val COL_EMAIL = "email"
    const val COL_NAME = "name"
    const val COL_PUBLIC_KEY = "public_key"
    const val COL_HAS_PGP = "has_pgp"
    const val COL_CLIENT = "client"
    const val COL_ATTESTED = "attested"
    const val COL_FINGERPRINT = "fingerprint"
    const val COL_LONG_ID = "long_id"
    const val COL_KEYWORDS = "keywords"
    const val COL_LAST_USE = "last_use"

    const val CONTACTS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_CONTACTS + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_EMAIL + " VARCHAR(100) NOT NULL, " +
        COL_NAME + " VARCHAR(50) DEFAULT NULL, " +
        COL_PUBLIC_KEY + " BLOB DEFAULT NULL, " +
        COL_HAS_PGP + " BOOLEAN NOT NULL, " +
        COL_CLIENT + " VARCHAR(20) DEFAULT NULL, " +
        COL_ATTESTED + " BOOLEAN DEFAULT NULL, " +
        COL_FINGERPRINT + " VARCHAR(40) DEFAULT NULL, " +
        COL_LONG_ID + " VARCHAR(16) DEFAULT NULL, " +
        COL_KEYWORDS + " VARCHAR(100) DEFAULT NULL, " +
        COL_LAST_USE + " INTEGER DEFAULT 0 " + ");"

    const val CREATE_UNIQUE_INDEX_EMAIL_IN_CONTACT = UNIQUE_INDEX_PREFIX + COL_EMAIL + "_in_" +
        TABLE_NAME_CONTACTS + " ON " + TABLE_NAME_CONTACTS + " (" + COL_EMAIL + ")"

    const val CREATE_INDEX_NAME_IN_CONTACT = INDEX_PREFIX + COL_NAME + "_in_" + TABLE_NAME_CONTACTS +
        " ON " + TABLE_NAME_CONTACTS + " (" + COL_NAME + ")"

    const val CREATE_INDEX_HAS_PGP_IN_CONTACT = INDEX_PREFIX + COL_HAS_PGP + "_in_" +
        TABLE_NAME_CONTACTS + " ON " + TABLE_NAME_CONTACTS + " (" + COL_HAS_PGP + ")"

    const val CREATE_INDEX_LONG_ID_IN_CONTACT = INDEX_PREFIX + COL_LONG_ID + "_in_" +
        TABLE_NAME_CONTACTS + " ON " + TABLE_NAME_CONTACTS + " (" + COL_LONG_ID + ")"

    const val CREATE_INDEX_LAST_USE_IN_CONTACT = INDEX_PREFIX + COL_LAST_USE + "_in_" +
        TABLE_NAME_CONTACTS + " ON " + TABLE_NAME_CONTACTS + " (" + COL_LAST_USE + ")"
  }
}
