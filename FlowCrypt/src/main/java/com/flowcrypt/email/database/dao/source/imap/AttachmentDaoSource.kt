/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.database.dao.source.BaseDaoSource
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * @author Denis Bondarenko
 * Date: 08.08.2017
 * Time: 10:41
 * E-mail: DenBond7@gmail.com
 */

class AttachmentDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_ATTACHMENT

  /**
   * Add a new attachment details to the database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label where exists message which contains a current attachment.
   * @param uid     The message UID.
   * @param attInfo The attachment details which will be added to the database.
   * @return A [Uri] of the created row.
   */
  fun addRow(context: Context, email: String, label: String?, uid: Long, attInfo: AttachmentInfo?): Uri? {
    val contentResolver = context.contentResolver
    return if (attInfo != null && label != null && contentResolver != null) {
      val contentValues = prepareContentValues(email, label, uid, attInfo)
      contentResolver.insert(baseContentUri, contentValues)
    } else
      null
  }

  /**
   * Add a new attachment details to the database.
   *
   * @param context Interface to global information about an application environment.
   * @param attInfo The attachment details which will be added to the database.
   * @return A [Uri] of the created row.
   */
  fun addRow(context: Context, attInfo: AttachmentInfo?): Uri? {
    val contentResolver = context.contentResolver
    if (attInfo != null && contentResolver != null) {
      val contentValues = prepareContentValuesFromAttInfo(attInfo)
      return contentResolver.insert(baseContentUri, contentValues)
    } else
      return null
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context     Interface to global information about an application environment.
   * @param email       The email that the message linked.
   * @param label       The folder label where exists a message which contains the current attachments.
   * @param uid         The message UID.
   * @param attInfoList The attachments list.
   * @return the number of newly created rows.
   */
  fun addRows(context: Context, email: String, label: String, uid: Long, attInfoList: List<AttachmentInfo>?): Int {
    return if (!CollectionUtils.isEmpty(attInfoList)) {
      val contentResolver = context.contentResolver
      val contentValuesArray = arrayOfNulls<ContentValues>(attInfoList!!.size)

      for (i in attInfoList.indices) {
        val attachmentInfo = attInfoList[i]
        val contentValues = prepareContentValues(email, label, uid, attachmentInfo)

        contentValuesArray[i] = contentValues
      }

      contentResolver.bulkInsert(baseContentUri, contentValuesArray)
    } else
      0
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context       Interface to global information about an application environment.
   * @param contentValues The array of prepared [ContentValues].
   * @return the number of newly created rows.
   */
  fun addRows(context: Context, contentValues: Array<ContentValues>?): Int {
    return if (contentValues != null) {
      val contentResolver = context.contentResolver
      contentResolver.bulkInsert(baseContentUri, contentValues)
    } else
      0
  }

  /**
   * Update some attachment by the given parameters.
   *
   * @param context       Interface to global information about an application environment.
   * @param email         The email that the attachment linked.
   * @param label         The folder that the attachment linked.
   * @param uid           The message UID that the attachment linked.
   * @param attId         The unique attachment id.
   * @param contentValues The [ContentValues] which contains new information.
   * @return The count of the updated row or -1 up.
   */
  fun update(context: Context, email: String?, label: String?, uid: Long, attId: String, contentValues: ContentValues): Int {
    val contentResolver = context.contentResolver
    return if (email != null && label != null && contentResolver != null) {
      val where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? AND " + COL_ATTACHMENT_ID +
          " = ? "
      val selectionArgs = arrayOf(email, label, uid.toString(), attId)
      contentResolver.update(baseContentUri, contentValues, where, selectionArgs)
    } else
      -1
  }

  /**
   * Get all [AttachmentInfo] objects from the database for some message.
   *
   * @param email The email that the message linked.
   * @param label The folder label.
   * @param uid   The message UID.
   * @return A  list of [AttachmentInfo] objects.
   */
  fun getAttInfoList(context: Context, email: String, label: String, uid: Long): ArrayList<AttachmentInfo> {
    val contentResolver = context.contentResolver
    val selection = "$COL_EMAIL = ? AND $COL_FOLDER = ? AND $COL_UID = ?"
    val selectionArgs = arrayOf(email, label, uid.toString())
    val cursor = contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    val attInfoList = ArrayList<AttachmentInfo>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        attInfoList.add(getAttInfo(cursor))
      }
      cursor.close()
    }

    return attInfoList
  }

  /**
   * Delete cached attachments info for some email and some folder.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @return The number of deleted rows.
   */
  fun deleteCachedAttInfo(context: Context, email: String?, label: String?): Int {
    val contentResolver = context.contentResolver
    return if (email != null && label != null && contentResolver != null) {
      val where = "$COL_EMAIL = ? AND $COL_FOLDER = ?"
      contentResolver.delete(baseContentUri, where, arrayOf(email, label))
    } else
      -1
  }

  /**
   * Delete attachments of some message in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @return The number of rows deleted.
   */
  fun deleteAtts(context: Context, email: String?, label: String?, uid: Long): Int {
    val contentResolver = context.contentResolver
    return if (email != null && label != null && contentResolver != null) {
      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "
      contentResolver.delete(baseContentUri, where, arrayOf(email, label, uid.toString()))
    } else
      -1
  }

  companion object {
    const val TABLE_NAME_ATTACHMENT = "attachment"

    const val COL_EMAIL = "email"
    const val COL_FOLDER = "folder"
    const val COL_UID = "uid"
    const val COL_NAME = "name"
    const val COL_ENCODED_SIZE_IN_BYTES = "encodedSize"
    const val COL_TYPE = "type"
    const val COL_ATTACHMENT_ID = "attachment_id"
    const val COL_FILE_URI = "file_uri"
    const val COL_FORWARDED_FOLDER = "forwarded_folder"
    const val COL_FORWARDED_UID = "forwarded_uid"

    const val ATTACHMENT_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_ATTACHMENT + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_EMAIL + " VARCHAR(100) NOT NULL, " +
        COL_FOLDER + " TEXT NOT NULL, " +
        COL_UID + " INTEGER NOT NULL, " +
        COL_NAME + " TEXT NOT NULL, " +
        COL_ENCODED_SIZE_IN_BYTES + " INTEGER DEFAULT 0, " +
        COL_TYPE + " VARCHAR(100) NOT NULL, " +
        COL_ATTACHMENT_ID + " TEXT NOT NULL, " +
        COL_FILE_URI + " TEXT, " +
        COL_FORWARDED_FOLDER + " TEXT, " +
        COL_FORWARDED_UID + " INTEGER DEFAULT -1 " + ");"

    const val CREATE_UNIQUE_INDEX_EMAIL_UID_FOLDER_ATTACHMENT_IN_ATTACHMENT = (
        UNIQUE_INDEX_PREFIX + COL_EMAIL + "_" + COL_UID + "_" + COL_FOLDER + "_" + COL_ATTACHMENT_ID + "_in_"
            + TABLE_NAME_ATTACHMENT + " ON " + TABLE_NAME_ATTACHMENT + " (" + COL_EMAIL + ", " + COL_UID
            + ", " + COL_FOLDER + ", " + COL_ATTACHMENT_ID + ")")

    /**
     * Prepare content values for insert to the database.
     *
     * @param email   The email that the message linked.
     * @param label   The folder label.
     * @param uid     The message UID.
     * @param attInfo The attachment info which will be added to the database.
     * @return generated [ContentValues]
     */
    fun prepareContentValues(email: String, label: String, uid: Long, attInfo: AttachmentInfo): ContentValues {
      val contentValues = prepareContentValuesFromAttInfo(attInfo)
      contentValues.put(COL_EMAIL, email)
      contentValues.put(COL_FOLDER, label)
      contentValues.put(COL_UID, uid)

      return contentValues
    }

    /**
     * Prepare content values for insert to the database.
     *
     * @param attInfo The attachment info which will be added to the database.
     * @return generated [ContentValues]
     */
    fun prepareContentValuesFromAttInfo(attInfo: AttachmentInfo): ContentValues {
      val contentValues = ContentValues()

      if (!TextUtils.isEmpty(attInfo.email)) {
        contentValues.put(COL_EMAIL, attInfo.email)
      }

      if (!TextUtils.isEmpty(attInfo.folder)) {
        contentValues.put(COL_FOLDER, attInfo.folder)
      }

      if (attInfo.uid != 0) {
        contentValues.put(COL_UID, attInfo.uid)
      }

      contentValues.put(COL_NAME, attInfo.name)
      contentValues.put(COL_ENCODED_SIZE_IN_BYTES, attInfo.encodedSize)
      contentValues.put(COL_TYPE, attInfo.type)
      contentValues.put(COL_ATTACHMENT_ID, attInfo.id)
      if (attInfo.uri != null) {
        contentValues.put(COL_FILE_URI, attInfo.uri!!.toString())
      }
      contentValues.put(COL_FORWARDED_FOLDER, attInfo.fwdFolder)
      contentValues.put(COL_FORWARDED_UID, attInfo.fwdUid)

      return contentValues
    }

    /**
     * Generate an [AttachmentInfo] object from the current cursor position.
     *
     * @param cursor The [Cursor] which contains information about an
     * [AttachmentInfo] object.
     * @return A generated [AttachmentInfo].
     */
    @JvmStatic
    fun getAttInfo(cursor: Cursor): AttachmentInfo {
      val uriString = cursor.getString(cursor.getColumnIndex(COL_FILE_URI))
      val uri = if (uriString == null) {
        null
      } else {
        Uri.parse(uriString)
      }

      return AttachmentInfo(null,
          cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
          cursor.getString(cursor.getColumnIndex(COL_FOLDER)),
          cursor.getInt(cursor.getColumnIndex(COL_UID)),
          cursor.getString(cursor.getColumnIndex(COL_FORWARDED_FOLDER)),
          cursor.getInt(cursor.getColumnIndex(COL_FORWARDED_UID)),
          cursor.getString(cursor.getColumnIndex(COL_NAME)),
          cursor.getLong(cursor.getColumnIndex(COL_ENCODED_SIZE_IN_BYTES)),
          cursor.getString(cursor.getColumnIndex(COL_TYPE)),
          cursor.getString(cursor.getColumnIndex(COL_ATTACHMENT_ID)),
          uri,
          false,
          !cursor.isNull(cursor.getColumnIndex(COL_FORWARDED_FOLDER)) && cursor.getInt(cursor.getColumnIndex(COL_FORWARDED_UID)) > 0, 0
      )
    }
  }
}
