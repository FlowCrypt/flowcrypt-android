/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.BaseColumns
import android.text.TextUtils
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.BaseDaoSource
import java.util.*
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

/**
 * This class describes the dao source for [GeneralMessageDetails] class.
 *
 * @author DenBond7
 * Date: 20.06.2017
 * Time: 10:49
 * E-mail: DenBond7@gmail.com
 */

class MessageDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_MESSAGES

  /**
   * Mark message as seen in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param isSeen  true if seen
   * @return The count of the updated row or -1 up.
   */
  fun setSeenStatus(context: Context, email: String?, label: String?, uid: Long,
                    isSeen: Boolean = true): Int {
    val resolver = context.contentResolver
    return if (email != null && label != null && resolver != null) {
      val values = ContentValues()
      if (isSeen) {
        values.put(COL_FLAGS, MessageFlag.SEEN.value)
      } else {
        values.put(COL_FLAGS, "")//todo-denbond7 maybe it is not a good idea to erase all flags
      }

      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "
      resolver.update(baseContentUri, values, where, arrayOf(email, label, uid.toString()))
    } else
      -1
  }

  /**
   * Mark messages as old in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @return The count of the updated row or -1 up.
   */
  fun setOldStatus(context: Context, email: String?, label: String?): Int {
    val contentResolver = context.contentResolver
    return if (email != null && label != null && contentResolver != null) {
      val contentValues = ContentValues()
      contentValues.put(COL_IS_NEW, false)
      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ?"
      contentResolver.update(baseContentUri, contentValues, where, arrayOf(email, label))
    } else
      -1
  }

  /**
   * Mark messages as old in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uidList The list of the UIDs.
   * @return The count of the updated row or -1 up.
   */
  fun setOldStatus(context: Context, email: String?, label: String?, uidList: List<String>?): Int {
    val contentResolver = context.contentResolver
    return if (contentResolver != null && email != null && label != null && uidList != null && uidList.isNotEmpty()) {
      val contentValues = ContentValues()
      contentValues.put(COL_IS_NEW, false)

      val args = ArrayList<String>()
      args.add(0, email)
      args.add(1, label)
      args.addAll(uidList)

      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID IN (" + TextUtils.join(",",
          Collections.nCopies(uidList.size, "?")) + ")"

      contentResolver.update(baseContentUri, contentValues, where, args.toTypedArray())
    } else
      -1
  }

  /**
   * Generate a [LocalFolder] object from the current cursor position.
   *
   * @param cursor The [Cursor] which contains information about [LocalFolder].
   * @return A generated [LocalFolder].
   */
  fun getMsgInfo(cursor: Cursor): GeneralMessageDetails {
    val details = GeneralMessageDetails(
        cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
        cursor.getString(cursor.getColumnIndex(COL_FOLDER)),
        cursor.getInt(cursor.getColumnIndex(COL_UID)),
        cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)),
        cursor.getLong(cursor.getColumnIndex(COL_RECEIVED_DATE)),
        cursor.getLong(cursor.getColumnIndex(COL_SENT_DATE)), null, null, null, null,
        cursor.getString(cursor.getColumnIndex(COL_SUBJECT)),
        listOf(*parseFlags(cursor.getString(cursor.getColumnIndex(COL_FLAGS)))),
        !cursor.isNull(cursor.getColumnIndex(COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)),
        cursor.getInt(cursor.getColumnIndex(COL_IS_MESSAGE_HAS_ATTACHMENTS)) == 1,
        cursor.getInt(cursor.getColumnIndex(COL_IS_ENCRYPTED)) == 1,
        MessageState.generate(cursor.getInt(cursor.getColumnIndex(COL_STATE))),
        cursor.getString(cursor.getColumnIndex(COL_ATTACHMENTS_DIRECTORY)),
        cursor.getString(cursor.getColumnIndex(COL_ERROR_MSG))
    )

    try {
      val fromAddresses = cursor.getString(cursor.getColumnIndex(COL_FROM_ADDRESSES))
      details.from = if (TextUtils.isEmpty(fromAddresses)) null else listOf(*InternetAddress.parse(fromAddresses))
    } catch (e: AddressException) {
      e.printStackTrace()
    }

    try {
      val replyToAddresses = cursor.getString(cursor.getColumnIndex(COL_REPLY_TO))
      details.replyTo = if (TextUtils.isEmpty(replyToAddresses)) null else listOf(*InternetAddress
          .parse(replyToAddresses))
    } catch (e: AddressException) {
      e.printStackTrace()
    }

    try {
      val toAddresses = cursor.getString(cursor.getColumnIndex(COL_TO_ADDRESSES))
      details.to = if (TextUtils.isEmpty(toAddresses)) null else listOf(*InternetAddress.parse(toAddresses))
    } catch (e: AddressException) {
      e.printStackTrace()
    }

    try {
      val ccAddresses = cursor.getString(cursor.getColumnIndex(COL_CC_ADDRESSES))
      details.cc = if (TextUtils.isEmpty(ccAddresses)) null else listOf(*InternetAddress.parse(ccAddresses))
    } catch (e: AddressException) {
      e.printStackTrace()
    }

    return details
  }

  /**
   * Get new messages.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return A  list of [GeneralMessageDetails] objects.
   */
  fun getNewMsgs(context: Context, email: String, label: String): List<GeneralMessageDetails> {
    val contentResolver = context.contentResolver

    val orderType: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      "ASC"
    } else {
      "DESC"
    }

    val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_IS_NEW = 1 "
    val selectionArgs = arrayOf(email, label)
    val cursor = contentResolver.query(baseContentUri, null, selection, selectionArgs,
        "$COL_RECEIVED_DATE $orderType")

    val detailsList = ArrayList<GeneralMessageDetails>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        detailsList.add(getMsgInfo(cursor))
      }
      cursor.close()
    }

    return detailsList
  }

  private fun parseFlags(string: String): Array<String> {
    return parseArray(string, "\\s")
  }

  companion object {
    const val TABLE_NAME_MESSAGES = "messages"

    const val COL_EMAIL = "email"
    const val COL_FOLDER = "folder"
    const val COL_UID = "uid"
    const val COL_RECEIVED_DATE = "received_date"
    const val COL_SENT_DATE = "sent_date"
    const val COL_FROM_ADDRESSES = "from_address"
    const val COL_TO_ADDRESSES = "to_address"
    const val COL_CC_ADDRESSES = "cc_address"
    const val COL_SUBJECT = "subject"
    const val COL_FLAGS = "flags"
    const val COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS = "raw_message_without_attachments"
    const val COL_IS_MESSAGE_HAS_ATTACHMENTS = "is_message_has_attachments"
    const val COL_IS_ENCRYPTED = "is_encrypted"
    const val COL_IS_NEW = "is_new"
    const val COL_STATE = "state"
    const val COL_ATTACHMENTS_DIRECTORY = "attachments_directory"
    const val COL_ERROR_MSG = "error_msg"
    const val COL_REPLY_TO = "reply_to"

    const val ENCRYPTED_STATE_UNDEFINED = -1

    private fun parseArray(attributesAsString: String?, regex: String): Array<String> {
      return if (attributesAsString != null && attributesAsString.isNotEmpty()) {
        attributesAsString.split(regex.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      } else {
        arrayOf()
      }
    }
  }
}
