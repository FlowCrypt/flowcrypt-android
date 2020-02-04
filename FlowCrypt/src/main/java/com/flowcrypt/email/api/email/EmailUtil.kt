/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import android.util.SparseArray
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.node.NodeRetrofitHelper
import com.flowcrypt.email.api.retrofit.node.NodeService
import com.flowcrypt.email.api.retrofit.request.node.ComposeEmailRequest
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.broadcastreceivers.UserRecoverableAuthExceptionBroadcastReceiver
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NodeEncryptException
import com.flowcrypt.email.util.exception.NodeException
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.common.util.CollectionUtils
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.gmail.GmailScopes
import com.sun.mail.gimap.GmailRawSearchTerm
import com.sun.mail.iap.Argument
import com.sun.mail.imap.IMAPBodyPart
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.protocol.BODY
import com.sun.mail.imap.protocol.FetchResponse
import com.sun.mail.imap.protocol.UID
import com.sun.mail.imap.protocol.UIDSet
import com.sun.mail.util.ASCIIUtility
import org.apache.commons.io.IOUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.activation.DataHandler
import javax.mail.BodyPart
import javax.mail.FetchProfile
import javax.mail.Message
import javax.mail.MessageRemovedException
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.UIDFolder
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.search.BodyTerm
import javax.mail.search.SearchTerm
import javax.mail.util.ByteArrayDataSource
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * @author Denis Bondarenko
 * Date: 29.09.2017
 * Time: 15:31
 * E-mail: DenBond7@gmail.com
 */

class EmailUtil {
  companion object {
    const val PATTERN_FORWARDED_DATE = "EEE, MMM d, yyyy HH:mm:ss"
    private const val HTML_EMAIL_INTRO_TEMPLATE_HTM = "html/email_intro.template.htm"

    /**
     * Generate an unique content id.
     *
     * @return A generated unique content id.
     */
    @JvmStatic
    fun generateContentId(prefix: String = ""): String {
      return "<$prefix" + UUID.randomUUID().toString() + "@flowcrypt" + ">"
    }

    /**
     * Check if current folder has [JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT]. If the
     * folder contains it attribute we will not show this folder in the list.
     *
     * @param folder The [IMAPFolder] object.
     * @return true if current folder contains attribute
     * [JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT], false otherwise.
     * @throws MessagingException
     */
    @JvmStatic
    fun containsNoSelectAttr(folder: IMAPFolder): Boolean {
      return folder.attributes.contains(JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT)
    }

    /**
     * Get a domain of some email.
     *
     * @return The domain of some email.
     */
    @JvmStatic
    fun getDomain(email: String): String {
      return when {
        TextUtils.isEmpty(email) -> ""
        email.contains("@") -> email.substring(email.indexOf('@') + 1)
        else -> ""
      }
    }

    /**
     * Generate [AttachmentInfo] from the requested information from the file uri.
     *
     * @param uri The file [Uri]
     * @return Generated [AttachmentInfo].
     */
    @JvmStatic
    fun getAttInfoFromUri(context: Context?, uri: Uri?): AttachmentInfo? {
      if (context != null && uri != null) {
        val attInfo = AttachmentInfo()
        attInfo.uri = uri
        attInfo.type = GeneralUtil.getFileMimeTypeFromUri(context, uri)
        attInfo.id = generateContentId()

        val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE), null, null, null)
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
              attInfo.name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }

            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1) {
              attInfo.encodedSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
            }
          }
          cursor.close()
        } else if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
          attInfo.name = GeneralUtil.getFileNameFromUri(context, uri)
          attInfo.encodedSize = GeneralUtil.getFileSizeFromUri(context, uri)
        }

        return attInfo
      } else
        return null
    }

    /**
     * Generate [AttachmentInfo] using the given key details.
     *
     * @param nodeKeyDetails The key details
     * @return A generated [AttachmentInfo].
     */
    @JvmStatic
    fun genAttInfoFromPubKey(nodeKeyDetails: NodeKeyDetails?): AttachmentInfo? {
      if (nodeKeyDetails != null) {
        val fileName = "0x" + nodeKeyDetails.longId!!.toUpperCase(Locale.getDefault()) + ".asc"

        if (!TextUtils.isEmpty(nodeKeyDetails.publicKey)) {
          val attachmentInfo = AttachmentInfo()

          attachmentInfo.name = fileName
          attachmentInfo.encodedSize = nodeKeyDetails.publicKey!!.length.toLong()
          attachmentInfo.rawData = nodeKeyDetails.publicKey
          attachmentInfo.type = Constants.MIME_TYPE_PGP_KEY
          attachmentInfo.email = nodeKeyDetails.primaryPgpContact.email
          attachmentInfo.id = generateContentId()
          attachmentInfo.isEncryptionAllowed = false

          return attachmentInfo
        } else {
          return null
        }
      } else {
        return null
      }
    }

    /**
     * Generate a [BodyPart] with a private key as an attachment.
     *
     * @param account      The given account;
     * @param armoredPrKey The armored private key.
     * @return [BodyPart] with private key as an attachment.
     * @throws Exception will occur when generate this [BodyPart].
     */
    @JvmStatic
    fun genBodyPartWithPrivateKey(account: AccountDao, armoredPrKey: String): MimeBodyPart {
      val part = MimeBodyPart()
      val dataSource = ByteArrayDataSource(armoredPrKey, JavaEmailConstants.MIME_TYPE_TEXT_PLAIN)
      part.dataHandler = DataHandler(dataSource)
      part.fileName = SecurityUtils.genPrivateKeyName(account.email)
      return part
    }

    /**
     * Generate a message with the html pattern and the private key(s) as an attachment.
     *
     * @param context Interface to global information about an application environment;
     * @param account The given account;
     * @param session The current sess.
     * @return Generated [Message] object.
     * @throws Exception will occur when generate this message.
     */
    @JvmStatic
    fun genMsgWithAllPrivateKeys(context: Context, account: AccountDao, session: Session): Message {
      val keys = SecurityUtils.genPrivateKeysBackup(context, account)

      val multipart = MimeMultipart()
      multipart.addBodyPart(getBodyPartWithBackupText(context))

      val attsPart = genBodyPartWithPrivateKey(account, keys)
      attsPart.contentID = generateContentId()
      multipart.addBodyPart(attsPart)

      val msg = genMsgWithBackupTemplate(context, account, session)
      msg.setContent(multipart)
      return msg
    }

    /**
     * Generate a message with the html pattern and the private key as an attachment.
     *
     * @param context Interface to global information about an application environment;
     * @param account The given account;
     * @param sess The current sess.
     * @return Generated [Message] object.
     * @throws Exception will occur when generate this message.
     */
    @JvmStatic
    fun genMsgWithPrivateKeys(context: Context, account: AccountDao, sess: Session, bodyPart: MimeBodyPart): Message {
      val multipart = MimeMultipart()
      multipart.addBodyPart(getBodyPartWithBackupText(context))
      bodyPart.contentID = generateContentId()
      multipart.addBodyPart(bodyPart)

      val msg = genMsgWithBackupTemplate(context, account, sess)
      msg.setContent(multipart)
      return msg
    }

    /**
     * Get a valid OAuth2 token for some [Account]. Must be called on the non-UI thread.
     *
     * @return A new valid OAuth2 token;
     * @throws IOException         Signaling a transient error (typically network related). It is left to clients to
     * implement a backoff/abandonment strategy appropriate to their latency requirements.
     * @throws GoogleAuthException Signaling an unrecoverable authentication error. These errors will typically
     * result from client errors (e.g. providing an invalid scope).
     */
    @JvmStatic
    fun getGmailAccountToken(context: Context, accountDao: AccountDao?): String {
      try {
        val account: Account? = accountDao?.account
            ?: throw NullPointerException("Account can't be a null!")

        return GoogleAuthUtil.getToken(context, account, JavaEmailConstants.OAUTH2 + GmailScopes.MAIL_GOOGLE_COM)
      } catch (e: UserRecoverableAuthException) {
        AccountDaoSource().updateAccountInformation(context, accountDao, ContentValues().apply { put(AccountDaoSource.COL_IS_RESTORE_ACCESS_REQUIRED, true) })
        context.sendBroadcast(UserRecoverableAuthExceptionBroadcastReceiver.newIntent(context, e.intent))
        throw e
      }
    }

    /**
     * Check is debug IMAP and SMTP protocols enable.
     *
     * @param context Interface to global information about an application environment;
     * @return true if debug enable, false - otherwise.
     */
    @JvmStatic
    fun hasEnabledDebug(context: Context): Boolean {
      return GeneralUtil.isDebugBuild() && SharedPreferencesHelper.getBoolean(
          PreferenceManager.getDefaultSharedPreferences(context.applicationContext),
          Constants.PREF_KEY_IS_MAIL_DEBUG_ENABLED, BuildConfig.IS_MAIL_DEBUG_ENABLED)
    }

    /**
     * Get a list of [NodeKeyDetails] using the **Gmail API**
     *
     * @param context context Interface to global information about an application environment;
     * @param account An [AccountDao] object.
     * @param sess A [Session] object.
     * @return A list of [NodeKeyDetails]
     * @throws MessagingException
     * @throws IOException
     */
    @JvmStatic
    fun getPrivateKeyBackupsViaGmailAPI(context: Context, account: AccountDao, sess: Session):
        Collection<NodeKeyDetails> {
      try {
        val list = mutableListOf<NodeKeyDetails>()

        val searchQuery = NodeCallsExecutor.getGmailBackupSearch(account.email)
        val gmailApiService = GmailApiHelper.generateGmailApiService(context, account)

        var response = gmailApiService
            .users()
            .messages()
            .list(GmailApiHelper.DEFAULT_USER_ID)
            .setQ(searchQuery)
            .execute()

        val msgs = mutableListOf<com.google.api.services.gmail.model.Message>()

        //Try to load all backups
        while (response.messages != null) {
          msgs.addAll(response.messages)
          if (response.nextPageToken != null) {
            response = gmailApiService
                .users()
                .messages()
                .list(GmailApiHelper.DEFAULT_USER_ID)
                .setQ(searchQuery)
                .setPageToken(response.nextPageToken)
                .execute()
          } else {
            break
          }
        }

        for (origMsg in msgs) {
          val message = gmailApiService
              .users()
              .messages()
              .get(GmailApiHelper.DEFAULT_USER_ID, origMsg.id)
              .setFormat(GmailApiHelper.MESSAGE_RESPONSE_FORMAT_RAW)
              .execute()

          val stream = ByteArrayInputStream(Base64.decode(message.raw, Base64.URL_SAFE))
          val msg = MimeMessage(sess, stream)
          val backup = getKeyFromMimeMsg(msg)

          if (TextUtils.isEmpty(backup)) {
            continue
          }

          try {
            list.addAll(NodeCallsExecutor.parseKeys(backup))
          } catch (e: NodeException) {
            e.printStackTrace()
            ExceptionUtil.handleError(e)
          }

        }

        return list
      } catch (e: UserRecoverableAuthIOException) {
        AccountDaoSource().updateAccountInformation(context, account, ContentValues().apply { put(AccountDaoSource.COL_IS_RESTORE_ACCESS_REQUIRED, true) })
        context.sendBroadcast(UserRecoverableAuthExceptionBroadcastReceiver.newIntent(context, e.intent))
        throw e
      } catch (e: UserRecoverableAuthException) {
        AccountDaoSource().updateAccountInformation(context, account, ContentValues().apply { put(AccountDaoSource.COL_IS_RESTORE_ACCESS_REQUIRED, true) })
        context.sendBroadcast(UserRecoverableAuthExceptionBroadcastReceiver.newIntent(context, e.intent))
        throw e
      }
    }

    /**
     * Get a private key from [Message], if it exists in.
     *
     * @param msg The original [Message] object.
     * @return <tt>String</tt> A private key.
     * @throws MessagingException
     * @throws IOException
     */
    @JvmStatic
    fun getKeyFromMimeMsg(msg: Message): String {
      if (msg.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val multipart = msg.content as Multipart
        val partsCount = multipart.count
        for (partCount in 0 until partsCount) {
          val part = multipart.getBodyPart(partCount)
          if (part is MimeBodyPart) {
            if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) {
              return IOUtils.toString(part.inputStream, StandardCharsets.UTF_8)
            }
          }
        }
      }

      return ""
    }

    /**
     * Prepare a formatted date string for a forwarded message. For example `Tue, Apr 3, 2018 at 3:07 PM.`
     *
     * @return A generated formatted date string.
     */
    @JvmStatic
    fun genForwardedMsgDate(date: Date?): String {
      if (date == null) {
        return ""
      }

      val format = SimpleDateFormat(PATTERN_FORWARDED_DATE, Locale.US)
      return format.format(date)
    }

    /**
     * Generated a list of UID of the local messages which will be removed.
     *
     * @param localUIDs The list of UID of the local messages.
     * @param folder    The remote [IMAPFolder].
     * @param msgs      The array of incoming messages.
     * @return A list of UID of the local messages which will be removed.
     */
    @JvmStatic
    fun genDeleteCandidates(localUIDs: Collection<Long>, folder: IMAPFolder, msgs: Array<Message>): Collection<Long> {
      val uidListDeleteCandidates = HashSet(localUIDs)
      val uidList = HashSet<Long>()
      try {
        for (msg in msgs) {
          uidList.add(folder.getUID(msg))
        }
      } catch (e: MessagingException) {
        e.printStackTrace()
        if (e !is MessageRemovedException) {
          ExceptionUtil.handleError(e)
        }
      }

      uidListDeleteCandidates.removeAll(uidList)
      return uidListDeleteCandidates
    }

    /**
     * Generate an array of [javax.mail.Message] which contains candidates for insert.
     *
     * @param localUIDs The list of UID of the local messages.
     * @param folder    The remote [IMAPFolder].
     * @param msgs      The array of incoming messages.
     * @return The generated array.
     */
    @JvmStatic
    fun genNewCandidates(localUIDs: Collection<Long>, folder: IMAPFolder, msgs: Array<Message>): Array<Message> {
      val newCandidates = mutableListOf<Message>()
      try {
        for (msg in msgs) {
          if (!localUIDs.contains(folder.getUID(msg))) {
            newCandidates.add(msg)
          }
        }
      } catch (e: MessagingException) {
        e.printStackTrace()
        if (e !is MessageRemovedException) {
          ExceptionUtil.handleError(e)
        }
      }

      return newCandidates.toTypedArray()
    }

    /**
     * Generate an array of the messages which will be updated.
     *
     * @param map    The map of UID and flags of the local messages.
     * @param folder The remote [IMAPFolder].
     * @param msgs   The array of incoming messages.
     * @return An array of the messages which are candidates for updating iin the local database.
     */
    @JvmStatic
    fun genUpdateCandidates(map: Map<Long, String?>, folder: IMAPFolder, msgs: Array<Message>):
        Array<Message> {
      val updateCandidates = mutableListOf<Message>()
      try {
        for (msg in msgs) {
          var flags = map[folder.getUID(msg)]
          if (flags == null) {
            flags = ""
          }

          if (!flags.equals(msg.flags.toString(), ignoreCase = true)) {
            updateCandidates.add(msg)
          }
        }
      } catch (e: MessagingException) {
        e.printStackTrace()
        if (e !is MessageRemovedException) {
          ExceptionUtil.handleError(e)
        }
      }

      return updateCandidates.toTypedArray()
    }

    /**
     * Get the personal name of the first address from an array. If the given name is null we will return the email
     * address.
     *
     * @param addresses An array of [InternetAddress]
     * @return The first address as a human readable string or email.
     */
    @JvmStatic
    fun getFirstAddressString(addresses: List<InternetAddress>?): String {
      if (addresses == null || addresses.isEmpty()) {
        return ""
      }

      return if (TextUtils.isEmpty(addresses[0].personal)) {
        addresses[0].address
      } else {
        addresses[0].personal
      }
    }

    /**
     * Get updated information about messages in the local database.
     *
     * @param folder          The folder which contains messages.
     * @param loadedMsgsCount The count of already loaded messages.
     * @param newMsgsCount    The count of new messages (offset value).
     * @return A list of messages which already exist in the local database.
     * @throws MessagingException for other failures.
     */
    @JvmStatic
    fun getUpdatedMsgs(folder: IMAPFolder, loadedMsgsCount: Int, newMsgsCount: Int): Array<Message> {
      val end = folder.messageCount - newMsgsCount
      var start = end - loadedMsgsCount + 1

      if (end < 1) {
        return arrayOf()
      } else {
        if (start < 1) {
          start = 1
        }

        val msgs = folder.getMessages(start, end)

        if (msgs.isNotEmpty()) {
          val fetchProfile = FetchProfile()
          fetchProfile.add(FetchProfile.Item.FLAGS)
          fetchProfile.add(UIDFolder.FetchProfileItem.UID)
          folder.fetch(msgs, fetchProfile)
        }
        return msgs
      }
    }

    /**
     * Get updated information about messages in the local database using UIDs.
     *
     * @param folder The folder which contains messages.
     * @param first  The first UID in a range.
     * @param end    The last UID in a range.
     * @return A list of messages which already exist in the local database.
     * @throws MessagingException for other failures.
     */
    @JvmStatic
    fun getUpdatedMsgsByUID(folder: IMAPFolder, first: Long, end: Long): Array<Message> {
      return if (end <= first) {
        arrayOf()
      } else {
        val msgs = folder.getMessagesByUID(first, end)

        if (msgs.isNotEmpty()) {
          val fetchProfile = FetchProfile()
          fetchProfile.add(FetchProfile.Item.FLAGS)
          fetchProfile.add(UIDFolder.FetchProfileItem.UID)
          folder.fetch(msgs, fetchProfile)
        }
        msgs
      }
    }

    /**
     * Load messages info.
     *
     * @param folder The folder which contains messages.
     * @param msgs   The array of [Message].
     * @return New messages from a server which not exist in a local database.
     * @throws MessagingException for other failures.
     */
    @JvmStatic
    fun fetchMsgs(folder: IMAPFolder, msgs: Array<Message>): Array<Message> {
      if (msgs.isNotEmpty()) {
        val fetchProfile = FetchProfile()
        fetchProfile.add(FetchProfile.Item.ENVELOPE)
        fetchProfile.add(FetchProfile.Item.FLAGS)
        fetchProfile.add(FetchProfile.Item.CONTENT_INFO)
        fetchProfile.add(UIDFolder.FetchProfileItem.UID)

        folder.fetch(msgs, fetchProfile)
      }

      return msgs
    }

    /**
     * Check is input messages are encrypted.
     *
     * @param folder  The folder which contains messages which will be checked.
     * @param uidList The array of messages [UID] values.
     * @return [SparseArray] as results of the checking.
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun getMsgsEncryptionStates(folder: IMAPFolder, uidList: List<Long>): Map<Long, Boolean> {
      if (CollectionUtils.isEmpty(uidList)) {
        return HashMap()
      }
      val uidArray = LongArray(uidList.size)

      for (i in uidList.indices) {
        uidArray[i] = uidList[i]
      }

      val uidSets = UIDSet.createUIDSets(uidArray)

      return if (uidSets == null || uidSets.isEmpty()) {
        HashMap()
      } else folder.doCommand { imapProtocol ->
        val hashMap: HashMap<Long, Boolean> = HashMap()

        val args = Argument()
        val list = Argument()
        list.writeString("UID")
        list.writeString("BODY.PEEK[TEXT]<0.2048>")
        args.writeArgument(list)

        val responses = imapProtocol.command("UID FETCH " + UIDSet.toString(uidSets), args)
        val serverResponse = responses[responses.size - 1]

        if (serverResponse.isOK) {
          for (response in responses) {
            if (response !is FetchResponse) {
              continue
            }

            val uid = response.getItem(UID::class.java)
            if (uid != null && uid.uid != 0L) {
              val body = response.getItem(BODY::class.java)
              if (body != null && body.byteArrayInputStream != null) {
                val rawMsg = ASCIIUtility.toString(body.byteArrayInputStream)
                hashMap[uid.uid] = rawMsg.contains("-----BEGIN PGP MESSAGE-----")
              }
            }
          }
        }

        imapProtocol.notifyResponseHandlers(responses)
        imapProtocol.handleResult(serverResponse)

        hashMap
      } as HashMap<Long, Boolean>
    }

    /**
     * Generate a [SearchTerm] for encrypted messages which depends on an input [AccountDao].
     *
     * @param account An input [AccountDao]
     * @return A generated [SearchTerm].
     */
    @JvmStatic
    fun genEncryptedMsgsSearchTerm(account: AccountDao): SearchTerm {
      return if (AccountDao.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        GmailRawSearchTerm(
            "PGP OR GPG OR OpenPGP OR filename:asc OR filename:message OR filename:pgp OR filename:gpg")
      } else {
        BodyTerm("-----BEGIN PGP MESSAGE-----")
      }
    }

    /**
     * Generate a raw MIME message. Don't call it in the main thread.
     *
     * @param info    The given [OutgoingMessageInfo] which contains information about an outgoing
     * message.
     * @param pubKeys The public keys which will be used to generate an encrypted part.
     * @return The generated raw MIME message.
     */
    @JvmStatic
    fun genRawMsgWithoutAtts(info: OutgoingMessageInfo, pubKeys: List<String>?): String {

      val retrofit = NodeRetrofitHelper.getRetrofit() ?: return ""

      val nodeService = retrofit.create(NodeService::class.java)
      val request = ComposeEmailRequest(info, pubKeys)

      val response = nodeService.composeEmail(request).execute()
      val result = response.body() ?: throw NullPointerException("ComposeEmailResult == null")

      if (result.apiError != null) {
        throw NodeEncryptException(result.apiError)
      }

      return result.mimeMsg
    }

    /**
     * Get next [UID] value for the outgoing message.
     *
     * @param context Interface to global information about an application environment.
     * @return The next [UID] value for the outgoing message.
     */
    fun genOutboxUID(context: Context?): Long {
      var lastUid = SharedPreferencesHelper.getLong(PreferenceManager.getDefaultSharedPreferences(context),
          Constants.PREF_KEY_LAST_OUTBOX_UID, 0)

      lastUid++

      SharedPreferencesHelper.setLong(PreferenceManager.getDefaultSharedPreferences(context),
          Constants.PREF_KEY_LAST_OUTBOX_UID, lastUid)

      return lastUid
    }

    /**
     * Get information about the encryption state for the given messages.
     *
     * @param onlyEncrypted If true we show only encrypted messages
     * @param folder        The folder which contains input messages
     * @param newMsgs       The input messages
     * @return An array which contains information about the encryption state of the given messages.
     * @throws MessagingException
     */
    @JvmStatic
    fun getMsgsEncryptionInfo(onlyEncrypted: Boolean, folder: IMAPFolder, newMsgs: Array<Message>):
        Map<Long, Boolean> {
      val array: HashMap<Long, Boolean> = HashMap()
      return if (onlyEncrypted) {
        for (msg in newMsgs) {
          array[folder.getUID(msg)] = true
        }

        array
      } else {
        val uidList = mutableListOf<Long>()

        for (msg in newMsgs) {
          uidList.add(folder.getUID(msg))
        }

        getMsgsEncryptionStates(folder, uidList)
      }
    }

    @JvmStatic
    private fun getBodyPartWithBackupText(context: Context): BodyPart {
      val messageBodyPart = MimeBodyPart()
      messageBodyPart.setContent(GeneralUtil.removeAllComments(IOUtils.toString(context.assets
          .open(HTML_EMAIL_INTRO_TEMPLATE_HTM), StandardCharsets.UTF_8)), JavaEmailConstants.MIME_TYPE_TEXT_HTML)
      return messageBodyPart
    }

    @JvmStatic
    private fun genMsgWithBackupTemplate(context: Context, account: AccountDao, session: Session): Message {
      val msg = MimeMessage(session)

      msg.setFrom(InternetAddress(account.email))
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(account.email))
      msg.subject = context.getString(R.string.your_key_backup)
      return msg
    }

    /**
     * Get only headers from the raw MIME
     */
    fun getHeadersFromRawMIME(rawMime: String?): String {
      // we don't know if the message is \n or \r\n delimited
      if (rawMime == null) {
        return ""
      }

      val headersByDoubleNl = rawMime.trim().substringBefore("\n\n")
      val headersByDoubleCrNl = rawMime.trim().substringBefore("\r\n\r\n")

      return if (headersByDoubleCrNl.length < headersByDoubleNl.length) { // therefore we choose smaller result
        headersByDoubleCrNl
      } else {
        headersByDoubleNl
      }
    }

    /**
     * Get information about attachments from the given [Part]
     *
     * @param depth          The depth of the given [Part]
     * @param part           The given [Part]
     * @return a list of found attachments
     */
    fun getAttsInfoFromPart(part: Part, depth: String = "0"): MutableList<AttachmentInfo> {
      val attachmentInfoList = mutableListOf<AttachmentInfo>()
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val multiPart = part.content as Multipart
        val partsNumber = multiPart.count
        for (partCount in 0 until partsNumber) {
          val bodyPart = multiPart.getBodyPart(partCount)
          attachmentInfoList.addAll(getAttsInfoFromPart(bodyPart, "$depth${AttachmentInfo.DEPTH_SEPARATOR}$partCount"))
        }
      } else if (Part.ATTACHMENT.equals(part.disposition, ignoreCase = true)) {
        val attachmentInfo = AttachmentInfo()
        attachmentInfo.name = part.fileName ?: depth
        attachmentInfo.encodedSize = part.size.toLong()
        attachmentInfo.type = part.contentType ?: ""
        attachmentInfo.id = (part as? IMAPBodyPart)?.contentID
            ?: generateContentId(AttachmentInfo.INNER_ATTACHMENT_PREFIX)
        attachmentInfo.path = depth
        attachmentInfoList.add(attachmentInfo)
      }

      return attachmentInfoList
    }

    /**
     * Check is [Part] has attachment.
     *
     *
     * If the part contains a wrong MIME structure we will receive the exception "Unable to load BODYSTRUCTURE" when
     * calling [Part.isMimeType]
     *
     * @param part The parent part.
     * @return <tt>boolean</tt> true if [Part] has attachment, false otherwise or if an error has occurred.
     */
    fun hasAtt(part: Part): Boolean {
      try {
        if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
          val multiPart = part.content as Multipart
          val partsNumber = multiPart.count
          for (partCount in 0 until partsNumber) {
            val bodyPart = multiPart.getBodyPart(partCount)
            if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
              val hasAtt = hasAtt(bodyPart)
              if (hasAtt) {
                return true
              }
            } else if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true)) {
              return true
            }
          }
          return false
        } else {
          return false
        }
      } catch (e: MessagingException) {
        e.printStackTrace()
        return false
      } catch (e: IOException) {
        e.printStackTrace()
        return false
      }
    }

    /**
     * Prepare a reply quotes text
     *
     * @return The reply quotes text
     */
    @SuppressLint("SimpleDateFormat") // for now we use iso format, regardles of locality
    fun prepareReplyQuotes(msgInfo: IncomingMessageInfo?): String {
      val date = if (msgInfo != null) SimpleDateFormat("yyyy-MM-dd' at 'HH:mm").format(msgInfo.getReceiveDate()) else "unknown date"
      val sender = msgInfo?.getFrom()?.firstOrNull()?.toString() ?: "unknown sender"
      val replyText = msgInfo?.text?.replace("(?m)^".toRegex(), "> ") ?: "(unknown content)"
      return "\n\nOn $date, $sender wrote:\n$replyText"
    }
  }
}
