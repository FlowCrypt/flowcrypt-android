/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
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
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.jakarta.mail.isAttachment
import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.api.services.gmail.GmailScopes
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
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.search.AndTerm
import jakarta.mail.search.BodyTerm
import jakarta.mail.search.FromStringTerm
import jakarta.mail.search.HeaderTerm
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
import org.eclipse.angus.mail.gimap.GmailRawSearchTerm
import org.eclipse.angus.mail.iap.Argument
import org.eclipse.angus.mail.imap.IMAPBodyPart
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.protocol.BODY
import org.eclipse.angus.mail.imap.protocol.FetchResponse
import org.eclipse.angus.mail.imap.protocol.UID
import org.eclipse.angus.mail.imap.protocol.UIDSet
import org.eclipse.angus.mail.util.ASCIIUtility
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.UUID

/**
 * @author Denys Bondarenko
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
        val attInfoBuilder = AttachmentInfo.Builder()
        attInfoBuilder.uri = uri
        attInfoBuilder.type = GeneralUtil.getFileMimeTypeFromUri(context, uri)
        attInfoBuilder.id = generateContentId()

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
              attInfoBuilder.name = cursor.getString(nameIndex)
            }

            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1) {
              attInfoBuilder.encodedSize = cursor.getLong(sizeIndex)
            }
          }
          cursor.close()
        } else if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
          attInfoBuilder.name = GeneralUtil.getFileNameFromUri(context, uri)
          attInfoBuilder.encodedSize = GeneralUtil.getFileSizeFromUri(context, uri)
        }

        return attInfoBuilder.build()
      } else
        return null
    }

    /**
     * Generate [AttachmentInfo] using the given key details.
     *
     * @param pgpKeyRingDetails The key details
     * @return A generated [AttachmentInfo].
     */
    fun genAttInfoFromPubKey(
      pgpKeyRingDetails: PgpKeyRingDetails?,
      email: String
    ): AttachmentInfo? {
      if (pgpKeyRingDetails != null) {
        val fileName = "0x" + pgpKeyRingDetails.fingerprint.uppercase() + ".asc"

        return if (!TextUtils.isEmpty(pgpKeyRingDetails.publicKey)) {
          val attachmentInfoBuilder = AttachmentInfo.Builder()

          attachmentInfoBuilder.name = fileName
          attachmentInfoBuilder.encodedSize = pgpKeyRingDetails.publicKey.length.toLong()
          attachmentInfoBuilder.rawData = pgpKeyRingDetails.publicKey.toByteArray()
          attachmentInfoBuilder.type = Constants.MIME_TYPE_PGP_KEY
          attachmentInfoBuilder.email = email
          attachmentInfoBuilder.id = generateContentId()
          attachmentInfoBuilder.isEncryptionAllowed = false

          attachmentInfoBuilder.build()
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
            if (part.isAttachment()) {
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
      return addresses?.firstOrNull()?.let { it.personal?.ifEmpty { it.address } ?: it.address }
        ?: ""
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
      if (uidList.isEmpty()) {
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

    fun hasEncryptedData(rawMsg: String?): Boolean {
      return "^-----BEGIN PGP MESSAGE-----".toRegex(RegexOption.MULTILINE)
        .containsMatchIn(rawMsg ?: "")
    }

    fun hasSignedData(rawMsg: String?): Boolean {
      return "^-----BEGIN PGP SIGNED MESSAGE-----".toRegex(RegexOption.MULTILINE)
        .containsMatchIn(rawMsg ?: "")
    }

    fun genPgpThingsSearchTerm(account: AccountEntity): SearchTerm {
      return if (AccountEntity.ACCOUNT_TYPE_GOOGLE.equals(account.accountType, ignoreCase = true)) {
        GmailRawSearchTerm(GmailApiHelper.PATTERN_SEARCH_PGP)
      } else {
        OrTerm(
          arrayOf(
            AndTerm(
              arrayOf(
                BodyTerm("-----BEGIN PGP MESSAGE-----"),
                BodyTerm("-----END PGP MESSAGE-----"),
              )
            ),
            BodyTerm("-----BEGIN PGP SIGNED MESSAGE-----"),
            AndTerm(
              arrayOf(
                HeaderTerm("Content-Disposition", ".asc"),
                HeaderTerm("Content-Disposition", ".pgp"),
                HeaderTerm("Content-Disposition", ".gpg"),
                HeaderTerm("Content-Disposition", ".key"),
              )
            )
          )
        )
      }
    }

    /**
     * Generate a message(new, reply or forward). Don't call it in the main thread.
     *
     * @param outgoingMsgInfo  The given [OutgoingMessageInfo] which contains information about
     * an outgoing message.
     * @return The generated raw MIME message.
     */
    suspend fun genMessage(
      context: Context,
      accountEntity: AccountEntity,
      outgoingMsgInfo: OutgoingMessageInfo,
      signingRequired: Boolean = true,
      hideArmorMeta: Boolean = false,
    ): Message = withContext(Dispatchers.IO) {
      val session = Session.getInstance(Properties())
      val senderEmail = requireNotNull(outgoingMsgInfo.from?.address)
      var pubKeys: List<String>? = null
      var protectedPubKeys: List<String>? = null
      var prvKeys: List<String>? = null
      var ringProtector: SecretKeyRingProtector? = null

      if (outgoingMsgInfo.encryptionType === MessageEncryptionType.ENCRYPTED) {
        val publicRecipients = outgoingMsgInfo.getPublicRecipients().toMutableList()
        pubKeys =
          SecurityUtils.getRecipientsUsablePubKeys(context, publicRecipients).toMutableList()
        pubKeys.addAll(SecurityUtils.getSenderPublicKeys(context, senderEmail))

        val protectedRecipients = outgoingMsgInfo.getProtectedRecipients().toMutableList()
        protectedPubKeys = SecurityUtils.getRecipientsUsablePubKeys(context, protectedRecipients)

        if (signingRequired) {
          prvKeys = listOf(
            SecurityUtils.getSenderPgpKeyDetails(context, accountEntity, senderEmail).privateKey
              ?: throw IllegalStateException("Sender private key not found")
          )

          ringProtector =
            KeysStorageImpl.getInstance(context).getSecretKeyRingProtector()
        }
      }

      return@withContext when (outgoingMsgInfo.messageType) {
        MessageType.NEW, MessageType.FORWARD, MessageType.DRAFT -> {
          prepareNewMsg(
            session = session,
            info = outgoingMsgInfo,
            pubKeys = pubKeys,
            protectedPubKeys = protectedPubKeys,
            prvKeys = prvKeys,
            protector = ringProtector,
            hideArmorMeta = hideArmorMeta
          )
        }

        MessageType.REPLY, MessageType.REPLY_ALL -> {
          prepareReplyMsg(
            context = context,
            accountEntity = accountEntity,
            session = session,
            info = outgoingMsgInfo,
            pubKeys = pubKeys,
            protectedPubKeys = protectedPubKeys,
            prvKeys = prvKeys,
            protector = ringProtector,
            hideArmorMeta = hideArmorMeta
          )
        }

        else -> throw IllegalStateException("Unsupported message type")
      }
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
      } else if (part.isAttachment()) {
        val attachmentInfoBuilder = AttachmentInfo.Builder()
        attachmentInfoBuilder.name = part.fileName ?: depth
        attachmentInfoBuilder.encodedSize = part.size.toLong()
        attachmentInfoBuilder.type = part.contentType ?: ""
        attachmentInfoBuilder.id = (part as? IMAPBodyPart)?.contentID
          ?: generateContentId(AttachmentInfo.INNER_ATTACHMENT_PREFIX)
        attachmentInfoBuilder.path = depth
        attachmentInfoList.add(attachmentInfoBuilder.build())
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
            } else if (bodyPart.isAttachment()) {
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
      val date = if (msgInfo != null) {
        SimpleDateFormat("yyyy-MM-dd' at 'HH:mm").format(msgInfo.getReceiveDate())
      } else {
        "unknown date"
      }
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
      val isOnlyPgpModeEnabled = account.showOnlyEncrypted

      if (isOnlyPgpModeEnabled == true) {
        val searchTerm = genPgpThingsSearchTerm(account)

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
      val isAttachment = item.isAttachment()
      val fileName = try {
        item.fileName
      } catch (e: Exception) {
        //return empty file name if we can not recognize it
        ""
      }
      val backupsPattern = "(?i)(cryptup|flowcrypt)-backup-[a-z0-9]+\\.(asc|key)".toRegex()
      val pgpKeysPattern = "(?i)^(0|0x)?[A-F0-9]{8}([A-F0-9]{8})?.*\\.(asc|key)\$".toRegex()

      return when {
        isAttachment && (
            //match allowed files
            fileName in ALLOWED_FILE_NAMES ||
                //match private keys(backups)
                fileName?.matches(backupsPattern) == true ||
                //match PGP keys by name and extension
                fileName?.matches(pgpKeysPattern) == true ||
                //allow download keys less than 100kb
                FilenameUtils.getExtension(fileName) in KEYS_EXTENSIONS && item.size < 10240 ||
                //match signature
                item.isMimeType("application/pgp-signature") ||
                //match PGP/MIME version identification
                item.isMimeType("application/pgp-encrypted")
                && item.description.equals("PGP/MIME version identification", false)
            ) -> true

        isAttachment -> false

        else -> true
      }
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
        "-is:spam",
        "-is:trash",
      )

      return parameters.joinToString(separator = " ")
    }

    suspend fun prepareNewMsg(
      session: Session,
      info: OutgoingMessageInfo,
      pubKeys: List<String>? = null,
      protectedPubKeys: List<String>? = null,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null,
      hideArmorMeta: Boolean = false,
    ): MimeMessage = withContext(Dispatchers.IO) {
      val msg = FlowCryptMimeMessage(session)
      msg.subject = info.subject
      msg.setFrom(info.from)
      msg.setRecipients(Message.RecipientType.TO, info.toRecipients?.toTypedArray())
      msg.setRecipients(Message.RecipientType.CC, info.ccRecipients?.toTypedArray())
      msg.setRecipients(Message.RecipientType.BCC, info.bccRecipients?.toTypedArray())
      msg.setContent(MimeMultipart().apply {
        addBodyPart(
          prepareBodyPart(
            info = info,
            pubKeys = pubKeys,
            protectedPubKeys = protectedPubKeys,
            prvKeys = prvKeys,
            protector = protector,
            hideArmorMeta = hideArmorMeta
          )
        )
      })
      return@withContext msg
    }

    fun genReplyMessage(
      replyToMsg: MimeMessage,
      info: OutgoingMessageInfo,
      pubKeys: List<String>? = null,
      protectedPubKeys: List<String>? = null,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null,
      hideArmorMeta: Boolean = false,
    ): Message {
      val reply = replyToMsg.reply(false)//we use replyToAll == false to use the own logic
      reply.setFrom(info.from)
      reply.setContent(MimeMultipart().apply {
        addBodyPart(
          prepareBodyPart(
            info = info,
            pubKeys = pubKeys,
            protectedPubKeys = protectedPubKeys,
            prvKeys = prvKeys,
            protector = protector,
            hideArmorMeta = hideArmorMeta
          )
        )
      })
      reply.setRecipients(Message.RecipientType.TO, info.toRecipients?.toTypedArray())
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
      msgEntity: MessageEntity,
      atts: List<AttachmentEntity>
    ): MimeMessage = withContext(Dispatchers.IO) {
      val mimeMsg = MimeMessage(
        sess,
        OutgoingMessagesManager.getOutgoingMessageFromFile(
          context, requireNotNull(msgEntity.id)
        )?.inputStream()
      )

      val account =
        FlowCryptRoomDatabase.getDatabase(context).accountDao().getActiveAccountSuspend()

      if (account?.clientConfiguration?.shouldHideArmorMeta() == false) {
        //https://tools.ietf.org/html/draft-melnikov-email-user-agent-00#:~:text=User%2DAgent%20and%20X%2DMailer%20are%20common%20Email%20header%20fields,use%20of%20different%20email%20clients.
        mimeMsg.addHeader("User-Agent", "FlowCrypt_Android_" + BuildConfig.VERSION_NAME)
      }

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
      attBodyPart.dataHandler = if (attInfo.isLazyForwarded) {
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

    private suspend fun prepareReplyMsg(
      context: Context,
      accountEntity: AccountEntity,
      session: Session,
      info: OutgoingMessageInfo,
      pubKeys: List<String>?,
      protectedPubKeys: List<String>? = null,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null,
      hideArmorMeta: Boolean = false,
    ): Message = withContext(Dispatchers.IO) {
      val replyToMessageEntityId = info.replyToMessageEntityId
        ?: throw IllegalArgumentException("replyToMessageEntityId is null")

      val keys = PGPainless.readKeyRing()
        .secretKeyRingCollection(accountEntity.servicePgpPrivateKey)

      val replyToMessageEntity =
        FlowCryptRoomDatabase.getDatabase(context).msgDao().getMsgById(replyToMessageEntityId)
          ?: throw IllegalArgumentException("Empty replyTo MessageEntity")

      val snapshot = MsgsCacheManager.getMsgSnapshot(replyToMessageEntity.id.toString())
        ?: throw IllegalArgumentException("Snapshot of replyTo message not found")

      val uri = snapshot.getUri(0) ?: throw IllegalArgumentException("Uri not found")
      val inputStream = context.contentResolver?.openInputStream(uri)
        ?: throw IllegalArgumentException("InputStream not found")
      val decryptionStream = PgpDecryptAndOrVerify.genDecryptionStream(
        srcInputStream = inputStream,
        secretKeys = keys,
        protector = PasswordBasedSecretKeyRingProtector.forKey(
          keys.first(),
          Passphrase.fromPassword(accountEntity.servicePgpPassphrase)
        )
      )

      return@withContext genReplyMessage(
        replyToMsg = FlowCryptMimeMessage(session, decryptionStream),
        info = info,
        pubKeys = pubKeys,
        protectedPubKeys = protectedPubKeys,
        prvKeys = prvKeys,
        protector = protector,
        hideArmorMeta = hideArmorMeta
      )
    }

    private fun prepareBodyPart(
      info: OutgoingMessageInfo,
      pubKeys: List<String>? = null,
      protectedPubKeys: List<String>? = null,
      prvKeys: List<String>? = null,
      protector: SecretKeyRingProtector? = null,
      hideArmorMeta: Boolean = false,
    ): BodyPart {
      return if (info.encryptionType == MessageEncryptionType.ENCRYPTED) {
        val encryptedContent = PgpEncryptAndOrSign.encryptAndOrSignMsg(
          msg = info.msg ?: "",
          pubKeys = pubKeys ?: emptyList(),
          protectedPubKeys = protectedPubKeys,
          prvKeys = prvKeys,
          secretKeyRingProtector = protector,
          hideArmorMeta = hideArmorMeta,
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
      msg.setRecipients(Message.RecipientType.TO, account.email.asInternetAddresses())
      msg.subject =
        context.getString(R.string.your_key_backup, context.getString(R.string.app_name))
      return msg
    }
  }
}
