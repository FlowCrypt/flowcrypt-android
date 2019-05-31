/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.net.Uri
import android.os.RemoteException
import android.provider.BaseColumns
import android.text.TextUtils
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.dao.source.BaseDaoSource
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * This class describes the structure of IMAP labels for different accounts and methods which
 * will be used to manipulate this data.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 15:59
 * E-mail: DenBond7@gmail.com
 */

class ImapLabelsDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_IMAP_LABELS

  /**
   * @param context     Interface to global information about an application environment.
   * @param accountName The account name which are an owner of the folder.
   * @param localFolder The [LocalFolder] object which contains information about
   * [com.sun.mail.imap.IMAPFolder].
   * @return A [Uri] of the created row.
   */
  fun addRow(context: Context, accountName: String, localFolder: LocalFolder?): Uri? {
    val contentResolver = context.contentResolver
    return if (!TextUtils.isEmpty(accountName) && localFolder != null && contentResolver != null) {
      val contentValues = prepareContentValues(accountName, localFolder)
      contentResolver.insert(baseContentUri, contentValues)
    } else
      null
  }

  /**
   * Add information about folders to local the database.
   *
   * @param context      Interface to global information about an application environment.
   * @param accountName  The account name which are an owner of the folder.
   * @param localFolders The folders array.
   * @return @return the number of newly created rows.
   */
  fun addRows(context: Context, accountName: String, localFolders: Collection<LocalFolder>?): Int {
    return if (!CollectionUtils.isEmpty(localFolders)) {
      val contentResolver = context.contentResolver
      val contentValuesArray = arrayOfNulls<ContentValues>(localFolders!!.size)

      val foldersArray = localFolders.toTypedArray()

      for (i in localFolders.indices) {
        val localFolder = foldersArray[i]
        val contentValues = prepareContentValues(accountName, localFolder)
        contentValuesArray[i] = contentValues
      }

      contentResolver.bulkInsert(baseContentUri, contentValuesArray)
    } else
      0
  }

  /**
   * Get all [LocalFolder] objects from the database by an email.
   *
   * @param email The email of the [LocalFolder].
   * @return A  list of [LocalFolder] objects.
   */
  fun getFolders(context: Context, email: String): List<LocalFolder> {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(baseContentUri, null, "$COL_EMAIL = ?", arrayOf(email), null)

    val localFolders = ArrayList<LocalFolder>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        localFolders.add(getFolder(cursor))
      }
      cursor.close()
    }

    return localFolders
  }

  /**
   * Generate a [LocalFolder] object from the current cursor position.
   *
   * @param cursor The [Cursor] which contains information about [LocalFolder].
   * @return A generated [LocalFolder].
   */
  fun getFolder(cursor: Cursor): LocalFolder {
    return LocalFolder(
        cursor.getString(cursor.getColumnIndex(COL_FOLDER_NAME)),
        cursor.getString(cursor.getColumnIndex(COL_FOLDER_ALIAS)),
        parseAttributes(cursor.getString(cursor.getColumnIndex(COL_FOLDER_ATTRIBUTES))),
        cursor.getInt(cursor.getColumnIndex(COL_IS_CUSTOM_LABEL)) == 1,
        cursor.getInt(cursor.getColumnIndex(COL_MESSAGE_COUNT)), null
    )
  }

  /**
   * Get a [LocalFolder] from the database by an email and a name.
   *
   * @param email      The email of the [LocalFolder].
   * @param folderName The folder name.
   * @return [LocalFolder] or null if such folder not found.
   */
  fun getFolder(context: Context, email: String, folderName: String): LocalFolder? {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(baseContentUri, null, COL_EMAIL + " = ?" + " AND " +
        COL_FOLDER_NAME + " = ?", arrayOf(email, folderName), null)

    var localFolder: LocalFolder? = null

    if (cursor != null) {
      while (cursor.moveToNext()) {
        localFolder = getFolder(cursor)
      }
      cursor.close()
    }

    return localFolder
  }

  /**
   * Delete all folders of some email.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email of the [LocalFolder].
   * @return The count of deleted rows. Will be >1 if a folder(s) was deleted or -1 otherwise.
   */
  fun deleteFolders(context: Context, email: String): Int {
    val contentResolver = context.contentResolver
    return contentResolver?.delete(baseContentUri, "$COL_EMAIL = ?", arrayOf(email)) ?: -1
  }

  /**
   * Update a message count of some [LocalFolder].
   *
   * @param context     Interface to global information about an application environment.
   * @param email       The account email.
   * @param folderName  A server folder name. Links to [.COL_FOLDER_NAME]
   * @param newMsgCount A new message count.
   * @return The count of updated rows. Will be 1 if information about [LocalFolder] was
   * updated or -1 otherwise.
   */
  fun updateLabelMsgsCount(context: Context?, email: String, folderName: String, newMsgCount: Int): Int {
    return if (context != null && !TextUtils.isEmpty(folderName)) {
      val contentResolver = context.contentResolver
      if (contentResolver != null) {
        val contentValues = ContentValues()
        contentValues.put(COL_MESSAGE_COUNT, newMsgCount)
        val where = "$COL_EMAIL= ? AND $COL_FOLDER_NAME = ? "
        contentResolver.update(baseContentUri, contentValues, where, arrayOf(email, folderName))
      } else
        -1
    } else
      -1
  }

  /**
   * This method update the local labels info. Here we will remove deleted and create new folders.
   *
   * @param context         Interface to global information about an application environment.
   * @param email           The account email.
   * @param oldLocalFolders The list of old [LocalFolder] object.
   * @param newLocalFolders The list of new [LocalFolder] object.
   * @return the [ContentProviderResult] array.
   */
  @Throws(RemoteException::class, OperationApplicationException::class)
  fun updateLabels(context: Context, email: String?, oldLocalFolders: Collection<LocalFolder>,
                   newLocalFolders: Collection<LocalFolder>): Array<ContentProviderResult> {
    val contentResolver = context.contentResolver
    if (email != null && contentResolver != null) {

      val contentProviderOperations = ArrayList<ContentProviderOperation>()

      val deleteCandidates = ArrayList<LocalFolder>()
      for (oldLocalFolder in oldLocalFolders) {
        var isFolderFound = false
        for ((fullName) in newLocalFolders) {
          if (fullName == oldLocalFolder.fullName) {
            isFolderFound = true
            break
          }
        }

        if (!isFolderFound) {
          deleteCandidates.add(oldLocalFolder)
        }
      }

      val newCandidates = ArrayList<LocalFolder>()
      for (newLocalFolder in newLocalFolders) {
        var isFolderFound = false
        for ((fullName) in oldLocalFolders) {
          if (fullName == newLocalFolder.fullName) {
            isFolderFound = true
            break
          }
        }

        if (!isFolderFound) {
          newCandidates.add(newLocalFolder)
        }
      }

      for ((fullName) in deleteCandidates) {
        val args = arrayOf(email, fullName)

        contentProviderOperations.add(ContentProviderOperation.newDelete(baseContentUri)
            .withSelection("$COL_EMAIL= ? AND $COL_FOLDER_NAME = ? ", args)
            .withYieldAllowed(true)
            .build())
      }

      for (localFolder in newCandidates) {
        contentProviderOperations.add(ContentProviderOperation.newInsert(baseContentUri)
            .withValues(prepareContentValues(email, localFolder))
            .withYieldAllowed(true)
            .build())
      }
      return contentResolver.applyBatch(baseContentUri.authority!!, contentProviderOperations)
    } else {
      return emptyArray()
    }
  }

  private fun prepareContentValues(accountName: String?, localFolder: LocalFolder): ContentValues {
    val contentValues = ContentValues()
    contentValues.put(COL_EMAIL, accountName)
    contentValues.put(COL_FOLDER_NAME, localFolder.fullName)
    contentValues.put(COL_FOLDER_ALIAS, localFolder.folderAlias)
    contentValues.put(COL_MESSAGE_COUNT, localFolder.msgCount)
    contentValues.put(COL_IS_CUSTOM_LABEL, localFolder.isCustom)
    contentValues.put(COL_FOLDER_ATTRIBUTES, prepareAttributesToSaving(localFolder.attributes))
    return contentValues
  }

  private fun prepareAttributesToSaving(attributes: List<String>?): String {
    return if (!CollectionUtils.isEmpty(attributes)) {
      val result = StringBuilder()
      for (attribute in attributes!!) {
        result.append(attribute).append("\t")
      }

      result.toString()
    } else {
      ""
    }
  }

  private fun parseAttributes(attributesAsString: String?): List<String>? {
    return if (attributesAsString != null && attributesAsString.isNotEmpty()) {
      Arrays.asList(*attributesAsString.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    } else {
      null
    }
  }

  companion object {
    const val TABLE_NAME_IMAP_LABELS = "imap_labels"

    const val COL_EMAIL = "email"
    const val COL_FOLDER_NAME = "folder_name"
    const val COL_FOLDER_ALIAS = "folder_alias"
    const val COL_MESSAGE_COUNT = "message_count"
    const val COL_IS_CUSTOM_LABEL = "is_custom_label"
    const val COL_FOLDER_ATTRIBUTES = "folder_attributes"
    const val COL_FOLDER_MESSAGE_COUNT = "folder_message_count"

    const val IMAP_LABELS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_IMAP_LABELS + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_EMAIL + " VARCHAR(100) NOT NULL, " +
        COL_FOLDER_NAME + " VARCHAR(255) NOT NULL, " +
        COL_IS_CUSTOM_LABEL + " INTEGER DEFAULT 0, " +
        COL_FOLDER_ALIAS + " VARCHAR(100) DEFAULT NULL, " +
        COL_MESSAGE_COUNT + " INTEGER DEFAULT 0, " +
        COL_FOLDER_ATTRIBUTES + " TEXT NOT NULL, " +
        COL_FOLDER_MESSAGE_COUNT + " INTEGER DEFAULT 0 " + ");"
  }
}
