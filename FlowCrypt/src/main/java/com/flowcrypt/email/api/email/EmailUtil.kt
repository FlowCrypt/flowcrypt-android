/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.SparseArray
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.javamail.AttachmentInfoDataSource
import com.flowcrypt.email.api.email.javamail.ForwardedAttachmentInfoDataSource
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.gms.security.ProviderInstaller
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
import jakarta.activation.DataHandler
import jakarta.mail.BodyPart
import jakarta.mail.FetchProfile
import jakarta.mail.Message
import jakarta.mail.MessageRemovedException
import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.UIDFolder
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.search.AndTerm
import jakarta.mail.search.BodyTerm
import jakarta.mail.search.FromStringTerm
import jakarta.mail.search.OrTerm
import jakarta.mail.search.RecipientStringTerm
import jakarta.mail.search.SearchTerm
import jakarta.mail.search.StringTerm
import jakarta.mail.search.SubjectTerm
import jakarta.mail.util.ByteArrayDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.UUID

/**
 * @author Denis Bondarenko
 * Date: 29.09.2017
 * Time: 15:31
 * E-mail: DenBond7@gmail.com
 */
class EmailUtil {
  companion object {
    private const val PATTERN_FORWARDED_DATE = "EEE, MMM d, yyyy HH:mm:ss"
    private const val HTML_EMAIL_INTRO_TEMPLATE_HTM = "html/email_intro.template.htm"

    private val ALLOWED_FILE_NAMES = arrayOf(
      "PGPexch.htm.pgp",
      "PGPMIME version identification",
      "Version.txt",
      "PGPMIME Versions Identification",
      "signature.asc",
      "msg.asc",
      "message",
      "message.asc",
      "encrypted.asc",
      "encrypted.eml.pgp",
      "Message.pgp"
    )

    private val KEYS_EXTENSIONS = arrayOf(
      "asc",
      "key"
    )

    /**
     * Generate an unique content id.
     *
     * @return A generated unique content id.
     */
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
    fun containsNoSelectAttr(folder: IMAPFolder): Boolean {
      return folder.attributes.contains(JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT)
    }

    /**
     * Get a domain of some email.
     *
     * @return The domain of some email.
     */
    fun getDomain(email: String): String {
      return when {
        email.contains("@") -> email.substring(email.indexOf('@') + 1).lowercase()
        else -> throw java.lang.IllegalArgumentException()
      }
    }

    /**
     * Generate [AttachmentInfo] from the requested information from the file uri.
     *
     * @param uri The file [Uri]
     * @return Generated [AttachmentInfo].
     */
    fun getAttInfoFromUri(context: Context?, uri: Uri?): AttachmentInfo? {
      if (context != null && uri != null) {
        val attInfo = AttachmentInfo()
        attInfo.uri = uri
        attInfo.type = GeneralUtil.getFileMimeTypeFromUri(context, uri)
        attInfo.id = generateContentId()

        val cursor = context.contentResolver.query(
          uri, arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
          ), null, null, null
        )
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
              attInfo.name = cursor.getString(nameIndex)
            }

            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1) {
              attInfo.encodedSize = cursor.getLong(sizeIndex)
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
     * @param pgpKeyDetails The key details
     * @return A generated [AttachmentInfo].
     */
    fun genAttInfoFromPubKey(pgpKeyDetails: PgpKeyDetails?, email: String): AttachmentInfo? {
      if (pgpKeyDetails != null) {
        val fileName = "0x" + pgpKeyDetails.fingerprint.uppercase() + ".asc"

        return if (!TextUtils.isEmpty(pgpKeyDetails.publicKey)) {
          val attachmentInfo = AttachmentInfo()

          attachmentInfo.name = fileName
          attachmentInfo.encodedSize = pgpKeyDetails.publicKey.length.toLong()
          attachmentInfo.rawData = pgpKeyDetails.publicKey.toByteArray()
          attachmentInfo.type = Constants.MIME_TYPE_PGP_KEY
          attachmentInfo.email = email
          attachmentInfo.id = generateContentId()
          attachmentInfo.isEncryptionAllowed = false

          attachmentInfo
        } else {
          null
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
    fun genBodyPartWithPrivateKey(account: AccountEntity, armoredPrKey: String): MimeBodyPart {
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
    fun genMsgWithAllPrivateKeys(
      context: Context,
      account: AccountEntity,
      session: Session
    ): Message {
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
    fun genMsgWithPrivateKeys(
      context: Context,
      account: AccountEntity,
      sess: Session,
      bodyPart: MimeBodyPart
    ): Message {
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
    fun getGmailAccountToken(context: Context, accountEntity: AccountEntity): String {
      val account: Account = accountEntity.account

      return GoogleAuthUtil.getToken(
        context,
        account,
        JavaEmailConstants.OAUTH2 + GmailScopes.MAIL_GOOGLE_COM
      )
    }

    /**
     * Check is debug IMAP and SMTP protocols enable.
     *
     * @param context Interface to global information about an application environment;
     * @return true if debug enable, false - otherwise.
     */
    fun hasEnabledDebug(context: Context): Boolean {
      return GeneralUtil.isDebugBuild() && SharedPreferencesHelper.getBoolean(
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext),
        Constants.PREF_KEY_IS_MAIL_DEBUG_ENABLED, BuildConfig.IS_MAIL_DEBUG_ENABLED
      )
    }

    /**
     * Get a private key from [Message], if it exists in.
     *
     * @param msg The original [Message] object.
     * @return <tt>String</tt> A private key.
     * @throws MessagingException
     * @throws IOException
     */
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
    fun genDeleteCandidates(
      localUIDs: Collection<Long>,
      folder: IMAPFolder,
      msgs: Array<Message>
    ): Collection<Long> {
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
     * Generate an array of [jakarta.mail.Message] which contains candidates for insert.
     *
     * @param localUIDs The list of UID of the local messages.
     * @param folder    The remote [IMAPFolder].
     * @param msgs      The array of incoming messages.
     * @return The generated array.
     */
    fun genNewCandidates(
      localUIDs: Collection<Long>,
      folder: IMAPFolder,
      msgs: Array<Message>
    ): Array<Message> {
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
    fun genUpdateCandidates(
      map: Map<Long, String?>,
      folder: IMAPFolder,
      msgs: Array<Message>
    ): Array<Message> {
      val updateCandidates = mutableListOf<Message>()
      try {
        for (msg in msgs) {
          val flags = map[folder.getUID(msg)] ?: ""
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
     * Get updated information about messages in the local database using UIDs.
     *
     * @param folder The folder which contains messages.
     * @param first  The first UID in a range.
     * @param end    The last UID in a range.
     * @return A list of messages which already exist in the local database.
     * @throws MessagingException for other failures.
     */
    fun getUpdatedMsgsByUID(folder: IMAPFolder, first: Long, end: Long, fetchFlags: Boolean = true):
        Array<Message> {
      return if (end <= first && end != UIDFolder.LASTUID) {
        arrayOf()
      } else {
        val msgs = folder.getMessagesByUID(first, end).filterNotNull().toTypedArray()

        if (msgs.isNotEmpty()) {
          val fetchProfile = FetchProfile()
          if (fetchFlags) {
            fetchProfile.add(FetchProfile.Item.FLAGS)
          }
          fetchProfile.add(UIDFolder.FetchProfileItem.UID)
          folder.fetch(msgs, fetchProfile)
        }
        msgs
      }
    }

    /**
     * Get updated information about messages in the local database using UIDs.
     *
     * @param folder The folder which contains messages.
     * @param uids  A list of UID.
     * @return A list of messages which already exist in the local database.
     * @throws MessagingException for other failures.
     */
    fun getUpdatedMsgsByUIDs(
      folder: IMAPFolder,
      uids: LongArray,
      fetchFlags: Boolean = true
    ): Array<Message> {
      return if (uids.isEmpty()) {
        arrayOf()
      } else {
        val msgs = folder.getMessagesByUID(uids).filterNotNull().toTypedArray()

        if (msgs.isNotEmpty()) {
          val fetchProfile = FetchProfile()
          if (fetchFlags) {
            fetchProfile.add(FetchProfile.Item.FLAGS)
          }
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
                hashMap[uid.uid] = hasEncryptedData(rawMsg)
              }
            }
          }
        }

        imapProtocol.notifyResponseHandlers(responses)
        imapProtocol.handleResult(serverResponse)

        hashMap
      } as HashMap<Long, Boolean>
    }

    fun hasEncryptedData(rawMsg: String) = rawMsg.contains("-----BEGIN PGP MESSAGE-----")

    /**
     * Generate a [SearchTerm] for encrypted messages which depends on an input [AccountEntity].
     *
     * @param account An input [AccountEntity]
     * @return A generated [SearchTerm].
     */
    fun genEncryptedMsgsSearchTerm(account: AccountEntity): SearchTerm {
      return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        GmailRawSearchTerm(GmailApiHelper.PATTERN_SEARCH_ENCRYPTED_MESSAGES)
      } else {
        BodyTerm("-----BEGIN PGP MESSAGE-----")
      }
    }

    /**
     * Generate a message(new, reply or forward). Don't call it in the main thread.
     *
     * @param outgoingMsgInfo  The given [OutgoingMessageInfo] which contains information about
     * an outgoing message.
     * @return The generated raw MIME message.
     */
    fun genMessage(
      context: Context, accountEntity: AccountEntity,
      outgoingMsgInfo: OutgoingMessageInfo
    ): Message {
      val session = Session.getInstance(Properties())
      val senderEmail = outgoingMsgInfo.from.address
      var pubKeys: List<String>? = null
      var prvKeys: List<String>? = null
      var ringProtector: SecretKeyRingProtector? = null

      if (outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
        val recipients = outgoingMsgInfo.getAllRecipients().toMutableList()
        pubKeys = mutableListOf()
        pubKeys.addAll(SecurityUtils.getRecipientsUsablePubKeys(context, recipients))
        pubKeys.addAll(SecurityUtils.getSenderPublicKeys(context, senderEmail))
        prvKeys = listOf(
          SecurityUtils.getSenderPgpKeyDetails(context, accountEntity, senderEmail).privateKey
            ?: throw IllegalStateException("Sender private key not found")
        )
        ringProtector = KeysStorageImpl.getInstance(context).getSecretKeyRingProtector()
      }

      return when (outgoingMsgInfo.messageType) {
        MessageType.NEW, MessageType.FORWARD -> {
          prepareNewMsg(session, outgoingMsgInfo, pubKeys, prvKeys, ringProtector)
        }

        MessageType.REPLY, MessageType.REPLY_ALL -> {
          prepareReplyMsg(context, session, outgoingMsgInfo, pubKeys, prvKeys, ringProtector)
        }

        else -> throw IllegalStateException("Unsupported message type")
      }
    }

    /**
     * Get next [UID] value for the outgoing message.
     *
     * @param context Interface to global information about an application environment.
     * @return The next [UID] value for the outgoing message.
     */
    fun genOutboxUID(context: Context): Long {
      var lastUid = SharedPreferencesHelper.getLong(
        PreferenceManager.getDefaultSharedPreferences(context),
        Constants.PREF_KEY_LAST_OUTBOX_UID, 0
      )

      lastUid++

      SharedPreferencesHelper.setLong(
        PreferenceManager.getDefaultSharedPreferences(context),
        Constants.PREF_KEY_LAST_OUTBOX_UID, lastUid
      )

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
    fun getMsgsEncryptionInfo(onlyEncrypted: Boolean?, folder: IMAPFolder, newMsgs: Array<Message>):
        Map<Long, Boolean> {
      val array: HashMap<Long, Boolean> = HashMap()
      return if (onlyEncrypted == true) {
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
          attachmentInfoList.addAll(
            getAttsInfoFromPart(
              bodyPart,
              "$depth${AttachmentInfo.DEPTH_SEPARATOR}$partCount"
            )
          )
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
    fun genReplyContent(msgInfo: IncomingMessageInfo?): String {
      val date =
        if (msgInfo != null) SimpleDateFormat("yyyy-MM-dd' at 'HH:mm").format(msgInfo.getReceiveDate()) else "unknown date"
      val sender = msgInfo?.getFrom()?.firstOrNull()?.toString() ?: "unknown sender"
      val replyText = prepareReplyQuotes(msgInfo?.text)
      return "\n\nOn $date, $sender wrote:\n$replyText"
    }

    fun prepareReplyQuotes(originalText: String?): String {
      return originalText?.replace("(?m)^".toRegex(), "> ") ?: "(unknown content)"
    }

    suspend fun patchingSecurityProviderSuspend(context: Context) = withContext(Dispatchers.IO) {
      patchingSecurityProvider(context)
    }

    /**
     * To update a device's security provider, use the ProviderInstaller class.
     *
     *
     * When you call installIfNeeded(), the ProviderInstaller does the following:
     *  * If the device's Provider is successfully updated (or is already up-to-date), the method returns
     * normally.
     *  * If the device's Google Play services library is out of date, the method throws
     * GooglePlayServicesRepairableException. The app can then catch this exception and show the user an
     * appropriate dialog box to update Google Play services.
     *  * If a non-recoverable error occurs, the method throws GooglePlayServicesNotAvailableException to indicate
     * that it is unable to update the Provider. The app can then catch the exception and choose an appropriate
     * course of action, such as displaying the standard fix-it flow diagram.
     *
     *
     * If installIfNeeded() needs to install a new Provider, this can take anywhere from 30-50 milliseconds (on
     * more recent devices) to 350 ms (on older devices). If the security provider is already up-to-date, the
     * method takes a negligible amount of time.
     *
     *
     * Details here https://developer.android.com/training/articles/security-gms-provider.html#patching
     *
     * @param context Interface to global information about an application environment;
     */
    @WorkerThread
    fun patchingSecurityProvider(context: Context) {
      try {
        ProviderInstaller.installIfNeeded(context)
      } catch (e: GooglePlayServicesRepairableException) {
        e.printStackTrace()
      } catch (e: GooglePlayServicesNotAvailableException) {
        e.printStackTrace()
      }
    }

    /**
     * Generate a [SearchTerm] depend on an input [AccountEntity].
     *
     * @param account An input [AccountEntity]
     * @return A generated [SearchTerm].
     */
    fun generateSearchTerm(account: AccountEntity, localFolder: LocalFolder): SearchTerm {
      val isEncryptedModeEnabled = account.showOnlyEncrypted

      if (isEncryptedModeEnabled == true) {
        val searchTerm = genEncryptedMsgsSearchTerm(account)

        return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(
            account.accountType,
            ignoreCase = true
          )
        ) {
          val stringTerm = searchTerm as StringTerm
          GmailRawSearchTerm(localFolder.searchQuery + " AND (" + stringTerm.pattern + ")")
        } else {
          AndTerm(searchTerm, generateNonGmailSearchTerm(localFolder))
        }
      } else {
        return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(
            account.accountType,
            ignoreCase = true
          )
        ) {
          GmailRawSearchTerm(localFolder.searchQuery)
        } else {
          generateNonGmailSearchTerm(localFolder)
        }
      }
    }

    /**
     * Check is the given [MimeBodyPart] allowed for downloading
     *
     * @param item the given [MimeBodyPart] that will be analysed
     * @return true if the given part is allowed, otherwise - false
     */
    fun isPartAllowed(item: MimeBodyPart): Boolean {
      var result = true
      if (Part.ATTACHMENT.equals(item.disposition, ignoreCase = true)) {
        result = false

        //match allowed files
        if (item.fileName in ALLOWED_FILE_NAMES) {
          result = true
        }

        //match private keys
        if (item.fileName?.matches("(?i)(cryptup|flowcrypt)-backup-[a-z0-9]+\\.(asc|key)".toRegex()) == true) {
          result = true
        }

        //match public keys
        if (item.fileName?.matches("(?i)^(0|0x)?[A-F0-9]{8}([A-F0-9]{8})?.*\\.(asc|key)\$".toRegex()) == true) {
          result = true
        }

        //allow download keys less than 100kb
        if (FilenameUtils.getExtension(item.fileName) in KEYS_EXTENSIONS && item.size < 102400) {
          result = true
        }

        //match signature
        if (item.isMimeType("application/pgp-signature")) {
          result = true
        }
      }

      return result
    }

    /**
     * Get a special string which contains formatted template for the native Gmail search.
     *
     * See details here https://support.google.com/mail/answer/7190?hl=en
     *
     * @param email The account email
     * @return A formatted template for the native Gmail search
     */
    fun getGmailBackupSearchQuery(email: String): String {
      val subjects = listOf(
        "Your FlowCrypt Backup",
        "Your CryptUp Backup",
        "All you need to know about CryptUP (contains a backup)",
        "CryptUP Account Backup"
      )

      val parameters = listOf(
        "from:${email}",
        "to:${email}",
        """(subject:"${subjects.joinToString(separator = """" OR subject: """")}")""",
        "-is:spam"
      )

      return parameters.joinToString(separator = " ")
    }

    fun parseAddresses(fromAddress: String?): List<InternetAddress> {
      return try {
        InternetAddress.parse(fromAddress ?: "").toList()
      } catch (e: AddressException) {
        emptyList()
      }
    }

    fun prepareNewMsg(
      session: Session,
      info: OutgoingMessageInfo,
      pubKeys: List<String>? = null,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null
    ): MimeMessage {
      val msg = FlowCryptMimeMessage(session)
      msg.subject = info.subject
      msg.setFrom(info.from)
      msg.setRecipients(Message.RecipientType.TO, info.toRecipients.toTypedArray())
      msg.setRecipients(Message.RecipientType.CC, info.ccRecipients?.toTypedArray())
      msg.setRecipients(Message.RecipientType.BCC, info.bccRecipients?.toTypedArray())
      msg.setContent(MimeMultipart().apply {
        addBodyPart(prepareBodyPart(info, pubKeys, prvKeys, protector))
      })
      return msg
    }

    fun genReplyMessage(
      replyToMsg: MimeMessage,
      info: OutgoingMessageInfo,
      pubKeys: List<String>? = null,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null
    ): Message {
      val reply = replyToMsg.reply(false)//we use replyToAll == false to use the own logic
      reply.setFrom(info.from)
      reply.setContent(MimeMultipart().apply {
        addBodyPart(prepareBodyPart(info, pubKeys, prvKeys, protector))
      })
      reply.setRecipients(Message.RecipientType.TO, info.toRecipients.toTypedArray())
      reply.setRecipients(Message.RecipientType.CC, info.ccRecipients?.toTypedArray())
      reply.setRecipients(Message.RecipientType.BCC, info.bccRecipients?.toTypedArray())
      return reply
    }

    /**
     * Get public email domains.
     */
    fun getPublicEmailDomains(): Array<String> {
      return arrayOf(
        JavaEmailConstants.EMAIL_PROVIDER_GMAIL,
        JavaEmailConstants.EMAIL_PROVIDER_GOOGLEMAIL,
        JavaEmailConstants.EMAIL_PROVIDER_YAHOO,
        JavaEmailConstants.EMAIL_PROVIDER_OUTLOOK,
        JavaEmailConstants.EMAIL_PROVIDER_LIVE,
      )
    }

    suspend fun createMimeMsg(
      context: Context,
      sess: Session?,
      account: AccountEntity,
      msgEntity: MessageEntity,
      atts: List<AttachmentEntity>
    ): MimeMessage = withContext(Dispatchers.IO) {
      val mimeMsg = MimeMessage(sess, msgEntity.rawMessageWithoutAttachments?.toInputStream())

      //https://tools.ietf.org/html/draft-melnikov-email-user-agent-00#:~:text=User%2DAgent%20and%20X%2DMailer%20are%20common%20Email%20header%20fields,use%20of%20different%20email%20clients.
      mimeMsg.addHeader("User-Agent", "FlowCrypt_Android_" + BuildConfig.VERSION_NAME)

      if (mimeMsg.content is MimeMultipart && atts.isNotEmpty()) {
        val mimeMultipart = mimeMsg.content as MimeMultipart
        val keysStorage = KeysStorageImpl.getInstance(context)
        val secretKeys = PGPSecretKeyRingCollection(keysStorage.getPGPSecretKeyRings())
        val ringProtector = keysStorage.getSecretKeyRingProtector()

        val publicKeys = mutableListOf<String>()
        val senderEmail = msgEntity.from.first().address
        val recipients = msgEntity.allRecipients.toMutableList()
        publicKeys.addAll(SecurityUtils.getRecipientsUsablePubKeys(context, recipients))
        publicKeys.addAll(SecurityUtils.getSenderPublicKeys(context, senderEmail))

        for (att in atts) {
          val attBodyPart = genBodyPartWithAtt(
            context = context,
            att = att,
            shouldBeEncrypted = msgEntity.isEncrypted ?: false,
            publicKeys = publicKeys,
            secretKeys = secretKeys,
            ringProtector = ringProtector
          )
          mimeMultipart.addBodyPart(attBodyPart)
        }

        mimeMsg.setContent(mimeMultipart)
        mimeMsg.saveChanges()
      }

      return@withContext mimeMsg
    }

    private fun genBodyPartWithAtt(
      context: Context,
      att: AttachmentEntity,
      shouldBeEncrypted: Boolean,
      publicKeys: List<String>?,
      secretKeys: PGPSecretKeyRingCollection,
      ringProtector: SecretKeyRingProtector
    ): BodyPart {
      val attBodyPart = MimeBodyPart()
      val attInfo = att.toAttInfo()
      attBodyPart.dataHandler = if (attInfo.isForwarded) {
        DataHandler(
          ForwardedAttachmentInfoDataSource(
            context,
            attInfo,
            shouldBeEncrypted,
            publicKeys,
            secretKeys,
            ringProtector
          )
        )
      } else {
        DataHandler(AttachmentInfoDataSource(context, attInfo))
      }
      attBodyPart.fileName = attInfo.getSafeName()
      attBodyPart.contentID = attInfo.id

      return attBodyPart
    }

    private fun generateNonGmailSearchTerm(localFolder: LocalFolder): SearchTerm {
      return OrTerm(
        arrayOf(
          SubjectTerm(localFolder.searchQuery),
          BodyTerm(localFolder.searchQuery),
          FromStringTerm(localFolder.searchQuery),
          RecipientStringTerm(Message.RecipientType.TO, localFolder.searchQuery),
          RecipientStringTerm(Message.RecipientType.CC, localFolder.searchQuery),
          RecipientStringTerm(Message.RecipientType.BCC, localFolder.searchQuery)
        )
      )
    }

    private fun prepareReplyMsg(
      context: Context,
      session: Session,
      info: OutgoingMessageInfo,
      pubKeys: List<String>?,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null
    ): Message {
      val replyToMessageEntity = info.replyToMsgEntity
        ?: throw IllegalArgumentException("Empty replyTo MessageEntity")
      val msg = if (replyToMessageEntity.rawMessageWithoutAttachments.isNullOrEmpty()) {
        val snapshot = MsgsCacheManager.getMsgSnapshot(replyToMessageEntity.id.toString())
          ?: throw IllegalArgumentException("Snapshot of replyTo message not found")

        val uri = snapshot.getUri(0) ?: throw IllegalArgumentException("Uri not found")
        val input = context.contentResolver?.openInputStream(uri)
          ?: throw IllegalArgumentException("InputStream not found")
        FlowCryptMimeMessage(session, KeyStoreCryptoManager.getCipherInputStream(input))
      } else {
        val input = replyToMessageEntity.rawMessageWithoutAttachments.toInputStream()
        try {
          FlowCryptMimeMessage(session, KeyStoreCryptoManager.getCipherInputStream(input))
        } catch (e: Exception) {
          //added for compatibility to previous versions
          FlowCryptMimeMessage(session, input)
        }
      }

      return genReplyMessage(msg, info, pubKeys, prvKeys, protector)
    }

    private fun prepareBodyPart(
      info: OutgoingMessageInfo,
      pubKeys: List<String>? = null,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null
    ): BodyPart {
      return if (info.encryptionType == MessageEncryptionType.ENCRYPTED) {
        val encryptedContent = PgpEncryptAndOrSign.encryptAndOrSignMsg(
          msg = info.msg ?: "",
          pubKeys = pubKeys ?: emptyList(),
          prvKeys = prvKeys,
          secretKeyRingProtector = protector
        )

        if (info.isPasswordProtected == true) {
          MimeBodyPart().apply {
            val dataSource = ByteArrayDataSource(encryptedContent, "application/pgp-encrypted")
            dataHandler = DataHandler(dataSource)
            description = "OpenPGP encrypted message"
            disposition = Part.INLINE
            fileName = "message.asc"
          }
        } else {
          MimeBodyPart().apply {
            setText(encryptedContent)
          }
        }
      } else {
        MimeBodyPart().apply {
          setText(info.msg ?: "")
        }
      }
    }

    private fun getBodyPartWithBackupText(context: Context): BodyPart {
      val messageBodyPart = MimeBodyPart()
      messageBodyPart.setContent(
        GeneralUtil.removeAllComments(
          IOUtils.toString(
            context.assets
              .open(HTML_EMAIL_INTRO_TEMPLATE_HTM), StandardCharsets.UTF_8
          )
        ), JavaEmailConstants.MIME_TYPE_TEXT_HTML
      )
      return messageBodyPart
    }

    private fun genMsgWithBackupTemplate(
      context: Context,
      account: AccountEntity,
      session: Session
    ): Message {
      val msg = FlowCryptMimeMessage(session)

      msg.setFrom(InternetAddress(account.email))
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(account.email))
      msg.subject = context.getString(R.string.your_key_backup)
      return msg
    }
  }
}
