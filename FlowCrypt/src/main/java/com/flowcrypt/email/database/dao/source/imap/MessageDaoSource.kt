/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.os.Build
import android.os.RemoteException
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.LongSparseArray
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.GeneralMessageDetails
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.BaseDaoSource
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.google.android.gms.common.util.CollectionUtils
import com.sun.mail.imap.IMAPFolder
import java.io.File
import java.io.IOException
import java.util.*
import javax.mail.Flags
import javax.mail.Message
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
   * This method update cached messages.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param folder  The [IMAPFolder] object which contains information about a
   * remote folder.
   * @param msgs    The messages array.
   * @return the [ContentProviderResult] array.
   */
  fun updateMsgsByUID(context: Context, email: String?, label: String?,
                      folder: IMAPFolder, msgs: Array<Message>?): Array<ContentProviderResult> {
    val contentResolver = context.contentResolver
    if (email != null && label != null && contentResolver != null && msgs != null && msgs.isNotEmpty()) {

      val ops = ArrayList<ContentProviderOperation>()
      for (message in msgs) {
        val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "

        val builder = ContentProviderOperation.newUpdate(baseContentUri)
            .withValue(COL_FLAGS, message.flags.toString().toUpperCase(Locale.getDefault()))
            .withSelection(selection, arrayOf(email, label, folder.getUID(message).toString()))
            .withYieldAllowed(true)

        if (message.flags.contains(Flags.Flag.SEEN)) {
          builder.withValue(COL_IS_NEW, false)
        }

        ops.add(builder.build())
      }
      return contentResolver.applyBatch(baseContentUri.authority!!, ops)
    } else
      return emptyArray()
  }

  /**
   * Update some message by the given parameters.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param values  The [ContentValues] which contains new information.
   * @return The count of the updated rows or -1 up.
   */
  fun updateMsg(context: Context, email: String?, label: String?, uid: Long, values: ContentValues): Int {
    val resolver = context.contentResolver
    return if (email != null && label != null && resolver != null) {
      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "
      resolver.update(baseContentUri, values, where, arrayOf(email, label, uid.toString()))
    } else
      -1
  }

  /**
   * Switch [MessageState] for messages of the given folder of the given account
   *
   * @param context    Interface to global information about an application environment.
   * @param email      The email that the message linked.
   * @param label      The folder label.
   * @param oldValue   The old value.
   * @param newValues  The new value.
   * @return The count of the changed rows or -1 up.
   */
  fun changeMsgsState(context: Context, email: String?, label: String?, oldValue: MessageState,
                      newValues: MessageState): Int {
    val resolver = context.contentResolver

    val values = ContentValues()
    values.put(COL_STATE, newValues.value)

    return if (email != null && label != null && resolver != null) {
      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_STATE = ? "
      resolver.update(baseContentUri, values, where, arrayOf(email, label, oldValue.value.toString()))
    } else
      -1
  }

  /**
   * Update a state of some message.
   *
   * @param context      Interface to global information about an application environment
   * @param email        The email that the message linked
   * @param label        The folder label
   * @param uid          The message UID
   * @param messageState A new message state.
   * @return The count of the updated row or -1 up.
   */
  fun updateMsgState(context: Context, email: String, label: String, uid: Long, messageState: MessageState): Int {
    val contentValues = ContentValues()
    contentValues.put(COL_STATE, messageState.value)

    return updateMsg(context, email, label, uid, contentValues)
  }

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
   * Update the message flags in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param flags   The message flags.
   * @return The count of the updated row or -1 up.
   */
  fun updateLocalMsgFlags(context: Context, email: String?, label: String?, uid: Long, flags: Flags): Int {
    val resolver = context.contentResolver
    return if (email != null && label != null && resolver != null) {
      val values = ContentValues()
      values.put(COL_FLAGS, flags.toString().toUpperCase(Locale.getDefault()))
      if (flags.contains(Flags.Flag.SEEN)) {
        values.put(COL_IS_NEW, false)
      }
      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "
      resolver.update(baseContentUri, values, where, arrayOf(email, label, uid.toString()))
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
   * Get messages of some folder by the given state.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email of the [LocalFolder].
   * @param state   The message state.
   * @return A  list of [GeneralMessageDetails] objects.
   */
  fun getMsgsWithState(context: Context, email: String, state: MessageState):
      List<GeneralMessageDetails> {
    val contentResolver = context.contentResolver
    val selection = "$COL_EMAIL= ? AND $COL_STATE = ?"
    val cursor = contentResolver.query(baseContentUri,
        null, selection, arrayOf(email, state.value.toString()), null)

    val generalMsgDetailsList = mutableListOf<GeneralMessageDetails>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        generalMsgDetailsList.add(getMsgInfo(cursor))
      }
      cursor.close()
    }

    return generalMsgDetailsList
  }

  /**
   * Get all messages of the outbox folder.
   *
   * @param context  Interface to global information about an application environment.
   * @param email    The email of the [LocalFolder].
   * @param msgState The message state which will be used for filter results.
   * @return A  list of [GeneralMessageDetails] objects.
   */
  fun getOutboxMsgs(context: Context, email: String, msgState: MessageState): List<GeneralMessageDetails> {
    val contentResolver = context.contentResolver
    val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_STATE = ?"
    val folder = JavaEmailConstants.FOLDER_OUTBOX
    val selectionArgs = arrayOf(email, folder, msgState.value.toString())
    val cursor = contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    val generalMsgDetailsList = ArrayList<GeneralMessageDetails>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        generalMsgDetailsList.add(getMsgInfo(cursor))
      }
      cursor.close()
    }

    return generalMsgDetailsList
  }

  /**
   * Get all messages of the outbox folder which are not sent.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email of the [LocalFolder].
   * @return A  list of [GeneralMessageDetails] objects.
   */
  fun getOutboxMsgs(context: Context, email: String): List<GeneralMessageDetails> {
    val contentResolver = context.contentResolver
    val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_STATE NOT IN (?, ?)"
    val selectionArgs = arrayOf(email, JavaEmailConstants.FOLDER_OUTBOX, MessageState.SENT
        .value.toString(), MessageState.SENT_WITHOUT_LOCAL_COPY.value.toString())
    val cursor = contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    val generalMsgDetailsList = ArrayList<GeneralMessageDetails>()

    if (cursor != null) {
      while (cursor.moveToNext()) {
        generalMsgDetailsList.add(getMsgInfo(cursor))
      }
      cursor.close()
    }

    return generalMsgDetailsList
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

  /**
   * Get all [LocalFolder] objects from the database by an email.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @param uid     The uid of the message.
   * @return [GeneralMessageDetails] if the information about a message is exists.
   */
  fun getMsg(context: Context, email: String, label: String, uid: Long): GeneralMessageDetails? {
    val contentResolver = context.contentResolver
    val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "
    val selectionArgs = arrayOf(email, label, uid.toString())
    val cursor = contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    var details: GeneralMessageDetails? = null

    if (cursor != null) {
      if (cursor.moveToFirst()) {
        details = getMsgInfo(cursor)
      }
      cursor.close()
    }

    return details
  }

  /**
   * Get the raw MIME content of some message.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @param uid     The uid of the message.
   * @return the raw MIME message
   */
  fun getRawMIME(context: Context, email: String, label: String, uid: Int): String? {
    val contentResolver = context.contentResolver
    val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "
    val selectionArgs = arrayOf(email, label, uid.toString())
    val cursor = contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    var rawMimeMsg: String? = null

    if (cursor != null) {
      if (cursor.moveToFirst()) {
        rawMimeMsg = cursor.getString(cursor.getColumnIndex(COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS))

      }
      cursor.close()
    }

    return rawMimeMsg
  }

  /**
   * Get the last UID of a message in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The last UID for the current label or -1 if it not exists.
   */
  fun getLastUIDOfMsgInLabel(context: Context, email: String, label: String): Int {
    val contentResolver = context.contentResolver

    val projection = arrayOf("max($COL_UID)")
    val selection = "$COL_EMAIL = ? AND $COL_FOLDER = ?"
    val selectionArgs = arrayOf(email, label)

    val cursor = contentResolver.query(baseContentUri, projection, selection, selectionArgs, null)

    if (cursor != null && cursor.moveToFirst()) {
      val uid = cursor.getInt(0)
      cursor.close()
      return uid
    }

    return -1
  }

  /**
   * Get the oldest UID of a message in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The last UID for the current label or -1 if it not exists.
   */
  fun getOldestUIDOfMsgInLabel(context: Context, email: String, label: String): Int {
    val contentResolver = context.contentResolver

    val projection = arrayOf("min($COL_UID)")
    val selection = "$COL_EMAIL = ? AND $COL_FOLDER = ?"
    val selectionArgs = arrayOf(email, label)

    val cursor = contentResolver.query(baseContentUri, projection, selection, selectionArgs, null)

    if (cursor != null && cursor.moveToFirst()) {
      val uid = cursor.getInt(0)
      cursor.close()
      return uid
    }

    return -1
  }

  /**
   * Get the list of UID of all messages in the database which were not checked to encryption.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The list of UID of selected messages in the database for some label.
   */
  fun getNotCheckedUIDs(context: Context, email: String, label: String): List<Long> {
    val contentResolver = context.contentResolver
    val uidList = ArrayList<Long>()

    val projection = arrayOf(COL_UID)
    val selection = COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?" + " AND " + COL_IS_ENCRYPTED + " = " +
        ENCRYPTED_STATE_UNDEFINED
    val selectionArgs = arrayOf(email, label)

    val cursor = contentResolver.query(baseContentUri, projection, selection, selectionArgs, null)

    if (cursor != null) {
      while (cursor.moveToNext()) {
        uidList.add(cursor.getLong(cursor.getColumnIndex(COL_UID)))
      }
      cursor.close()
    }

    return uidList
  }

  /**
   * Get a map of UID and flags of all messages in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The map of UID and flags of all messages in the database for some label.
   */
  @SuppressLint("UseSparseArrays")
  fun getMapOfUIDAndMsgFlags(context: Context, email: String, label: String): Map<Long, String> {
    val contentResolver = context.contentResolver
    val uidList = HashMap<Long, String>()
    val projection = arrayOf(COL_UID, COL_FLAGS)
    val selection = "$COL_EMAIL = ? AND $COL_FOLDER = ?"
    val selectionArgs = arrayOf(email, label)

    val cursor = contentResolver.query(baseContentUri, projection, selection, selectionArgs, null)

    if (cursor != null) {
      while (cursor.moveToNext()) {
        uidList[cursor.getLong(cursor.getColumnIndex(COL_UID))] = cursor.getString(cursor.getColumnIndex(COL_FLAGS))
      }
      cursor.close()
    }

    return uidList
  }

  /**
   * Get a list of UID and flags of all unseen messages in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The list of UID and flags of all unseen messages in the database for some label.
   */
  @SuppressLint("UseSparseArrays")
  fun getUIDOfUnseenMsgs(context: Context, email: String, label: String): List<Int> {
    val contentResolver = context.contentResolver

    val projection = arrayOf(COL_UID)
    val selection = (COL_EMAIL + " = ? AND " + COL_FOLDER + " = ? AND " + COL_FLAGS + " NOT LIKE '%"
        + MessageFlag.SEEN.value + "%'")
    val selectionArgs = arrayOf(email, label)

    val cursor = contentResolver.query(baseContentUri, projection, selection, selectionArgs, null)

    val uidList = ArrayList<Int>()
    if (cursor != null) {
      while (cursor.moveToNext()) {
        uidList.add(cursor.getInt(cursor.getColumnIndex(COL_UID)))
      }
      cursor.close()
    }

    return uidList
  }

  /**
   * Get the count of messages in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The count of messages for the current label.
   */
  fun getLabelMsgsCount(context: Context, email: String, label: String): Int {
    val contentResolver = context.contentResolver

    val projection = arrayOf(FlowCryptRoomDatabase.COLUMN_NAME_COUNT)
    val selection = "$COL_EMAIL = ? AND $COL_FOLDER = ?"
    val selectionArgs = arrayOf(email, label)

    val cursor = contentResolver.query(baseContentUri, projection, selection, selectionArgs, null)

    if (cursor != null && cursor.moveToFirst()) {
      val uid = cursor.getInt(cursor.getColumnIndex(FlowCryptRoomDatabase
          .COLUMN_NAME_COUNT))
      cursor.close()
      return uid
    }

    return 0
  }

  /**
   * Delete a message from a some folder in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @return The number of rows deleted.
   */
  fun deleteMsg(context: Context, email: String?, label: String?, uid: Long): Int {
    val contentResolver = context.contentResolver
    return if (email != null && label != null && contentResolver != null) {
      val where = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "
      val selectionArgs = arrayOf(email, label, uid.toString())
      contentResolver.delete(baseContentUri, where, selectionArgs)
    } else
      -1
  }

  /**
   * @param context              Interface to global information about an application environment.
   * @param email                The email that the message linked.
   * @param label                The folder label.
   * @param msgsEncryptionStates The array which contains information about an encrypted state of some messages
   * @return the [ContentProviderResult] array.
   * @throws RemoteException
   * @throws OperationApplicationException
   */
  fun updateEncryptionStates(context: Context, email: String, label: String,
                             msgsEncryptionStates: LongSparseArray<Boolean>?): Array<ContentProviderResult> {
    val contentResolver = context.contentResolver

    if (msgsEncryptionStates != null && msgsEncryptionStates.size() > 0) {
      val ops = ArrayList<ContentProviderOperation>()
      var i = 0
      val arraySize = msgsEncryptionStates.size()
      while (i < arraySize) {
        val uid = msgsEncryptionStates.keyAt(i)
        val b = msgsEncryptionStates.get(uid)
        val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_UID = ? "

        ops.add(ContentProviderOperation.newUpdate(baseContentUri)
            .withValue(COL_IS_ENCRYPTED, b)
            .withSelection(selection, arrayOf(email, label, uid.toString()))
            .withYieldAllowed(true)
            .build())
        i++
      }
      return contentResolver.applyBatch(baseContentUri.authority!!, ops)
    } else
      return emptyArray()
  }

  /**
   * Delete an outgoing message.
   *
   * @param context Interface to global information about an application environment.
   * @param details Input details about the outgoing message.
   * @return The number of rows deleted.
   */
  fun deleteOutgoingMsg(context: Context, details: GeneralMessageDetails): Int {
    var deletedRows = -1
    val contentResolver = context.contentResolver

    if (contentResolver != null) {
      val where = (COL_EMAIL + "= ? AND "
          + COL_FOLDER + " = ? AND "
          + COL_UID + " = ? AND "
          + COL_STATE + " != " + MessageState.SENDING.value + " AND "
          + COL_STATE + " != " + MessageState.SENT_WITHOUT_LOCAL_COPY.value)
      val selectionArgs = arrayOf(details.email, details.label, details.uid.toString())
      deletedRows = contentResolver.delete(baseContentUri, where, selectionArgs)
    }

    if (deletedRows > 0) {
      ImapLabelsDaoSource().updateLabelMsgsCount(context, details.email,
          JavaEmailConstants.FOLDER_OUTBOX, MessageDaoSource().getOutboxMsgs(context,
          details.email).size)

      if (details.hasAtts) {
        val attDaoSource = AttachmentDaoSource()

        val attachmentInfoList = attDaoSource.getAttInfoList(context, details
            .email, JavaEmailConstants.FOLDER_OUTBOX, details.uid.toLong())

        if (!CollectionUtils.isEmpty(attachmentInfoList)) {
          attDaoSource.deleteAtts(context, details.email,
              details.label, details.uid.toLong())

          if (!TextUtils.isEmpty(details.attsDir)) {
            try {
              val parentDirName = details.attsDir
              val dir = File(File(context.cacheDir, Constants.ATTACHMENTS_CACHE_DIR), parentDirName)
              FileAndDirectoryUtils.deleteDir(dir)
            } catch (e: IOException) {
              e.printStackTrace()
            }
          }
        }
      }
    }

    return deletedRows
  }

  /**
   * Add the messages which have a current state equal [MessageState.SENDING] to the sending queue again.
   *
   * @param context Interface to global information about an application environment
   * @param email   The email that the message linked
   * @return The count of the updated row or -1 up.
   */
  fun resetMsgsWithSendingState(context: Context, email: String?): Int {
    val contentValues = ContentValues()
    contentValues.put(COL_STATE, MessageState.QUEUED.value)

    val contentResolver = context.contentResolver
    return if (email != null && contentResolver != null) {
      val selection = "$COL_EMAIL= ? AND $COL_FOLDER = ? AND $COL_STATE = ? "
      val selectionArgs = arrayOf(email, JavaEmailConstants.FOLDER_OUTBOX, MessageState.SENDING.value.toString())
      contentResolver.update(baseContentUri, contentValues, selection, selectionArgs)
    } else
      -1
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
