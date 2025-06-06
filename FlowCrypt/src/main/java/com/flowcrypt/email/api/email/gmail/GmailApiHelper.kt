/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.gmail

import android.accounts.Account
import android.content.Context
import android.util.Base64
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.api.GMailRawAttachmentFilterInputStream
import com.flowcrypt.email.api.email.gmail.api.GMailRawMIMEMessageFilterInputStream
import com.flowcrypt.email.api.email.gmail.model.GmailThreadInfo
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.disposition
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.getAttachmentInfoList
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.isMimeType
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.toThreadInfo
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.extensions.uid
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.FlavorSettings
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.GmailAPIException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.GenericJson
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.BatchDeleteMessagesRequest
import com.google.api.services.gmail.model.BatchModifyMessagesRequest
import com.google.api.services.gmail.model.Draft
import com.google.api.services.gmail.model.History
import com.google.api.services.gmail.model.Label
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.ListThreadsResponse
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.ModifyThreadRequest
import com.google.api.services.gmail.model.Thread
import jakarta.mail.Flags
import jakarta.mail.MessagingException
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.apache.commons.codec.android.binary.Base64InputStream
import org.eclipse.angus.mail.gimap.GmailRawSearchTerm
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Properties
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLException

/**
 * This class helps to work with Gmail API.
 *
 * @author Denys Bondarenko
 */
class GmailApiHelper {
  companion object {
    const val DEFAULT_USER_ID = "me"
    const val PATTERN_SEARCH_PGP =
      "(\"-----BEGIN PGP MESSAGE-----\" AND \"-----END PGP MESSAGE-----\") " +
          "OR (\"-----BEGIN PGP SIGNED MESSAGE-----\") " +
          "OR filename:({asc pgp gpg key})"

    const val MESSAGE_RESPONSE_FORMAT_METADATA = "metadata"

    const val FOLDER_TYPE_USER = "user"

    const val LABEL_INBOX = JavaEmailConstants.FOLDER_INBOX
    const val LABEL_UNREAD = JavaEmailConstants.FOLDER_UNREAD
    const val LABEL_DRAFT = JavaEmailConstants.FOLDER_DRAFT
    const val LABEL_SENT = JavaEmailConstants.FOLDER_SENT
    const val LABEL_TRASH = JavaEmailConstants.FOLDER_TRASH
    const val LABEL_SPAM = JavaEmailConstants.FOLDER_SPAM

    private val SCOPES = arrayOf(GmailScopes.MAIL_GOOGLE_COM)
    val CATEGORIES = arrayOf(
      "CHAT",
      "CATEGORY_FORUMS",
      "CATEGORY_UPDATES",
      "CATEGORY_PERSONAL",
      "CATEGORY_PROMOTIONS",
      "CATEGORY_SOCIAL"
    )
    private const val COUNT_OF_LOADED_EMAILS_BY_STEP =
      JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP.toLong()
    const val MESSAGE_RESPONSE_FORMAT_RAW = "raw"
    const val RESPONSE_FORMAT_FULL = "full"
    const val RESPONSE_FORMAT_MINIMAL = "minimal"
    const val RESPONSE_FORMAT_METADATA = "metadata"

    private val FULL_INFO_WITHOUT_DATA = listOf(
      "id",
      "threadId",
      "labelIds",
      "snippet",
      "sizeEstimate",
      "historyId",
      "internalDate",
      "payload/partId",
      "payload/mimeType",
      "payload/filename",
      "payload/headers",
      "payload/body",
      "payload/parts(partId,mimeType,filename,headers,body/size,body/attachmentId)"
    )

    val THREAD_BASE_INFO = listOf(
      "id",
      "historyId",
      "messages/id",
      "messages/threadId",
      "messages/labelIds",
    )

    suspend fun <T> executeWithResult(action: suspend () -> Result<T>): Result<T> =
      withContext(Dispatchers.IO) {
        return@withContext try {
          action.invoke()
        } catch (e: Exception) {
          e.printStackTrace()
          when (val exception = processException(e)) {
            is CommonConnectionException -> Result.exception(exception)

            is GmailAPIException -> {
              when {
                GmailAPIException.ENTITY_NOT_FOUND == exception.message
                    && exception.code == HttpURLConnection.HTTP_NOT_FOUND -> {
                  //we handle this error. No ACRA reports
                }

                else -> ExceptionUtil.handleError(exception)
              }
              Result.exception(exception)
            }

            else -> {
              ExceptionUtil.handleError(exception)
              Result.exception(exception)
            }
          }
        }
      }

    /**
     * Generate [Gmail] using incoming [AccountEntity]. [Gmail] class is the main point of
     * Gmail API.
     *
     * @param context   Interface to global information about an application environment.
     * @param account   The [AccountEntity] object which contains information about an account.
     * @return Generated [Gmail].
     */
    fun generateGmailApiService(context: Context, account: AccountEntity?): Gmail {
      requireNotNull(account)
      return generateGmailApiService(context, account.account)
    }

    fun generateGmailApiService(context: Context, account: Account?): Gmail {
      requireNotNull(account)

      val credential = generateGoogleAccountCredential(context, account)

      val transport = NetHttpTransport()
      if (GeneralUtil.isDebugBuild() && EmailUtil.hasEnabledDebug(context)) {
        Logger.getLogger(HttpTransport::class.java.name).apply {
          level = Level.CONFIG
          addHandler(object : ConsoleHandler() {}.apply { level = Level.CONFIG })
        }
      }

      val factory = GsonFactory.getDefaultInstance()
      val appName = context.getString(R.string.app_name)
      val rootUrl = FlavorSettings.getGmailAPIRootUrl()
      val builder = Gmail.Builder(transport, factory, credential).setApplicationName(appName)

      @Suppress("UNNECESSARY_SAFE_CALL", "KotlinRedundantDiagnosticSuppress")
      rootUrl?.let { builder.rootUrl = it }

      if (!FlavorSettings.isGMailAPIHttpRequestInitializerEnabled()) {
        builder.httpRequestInitializer = null
      }
      return builder.build()
    }

    suspend fun <M> doOperationViaStepsSuspend(
      stepValue: Int = 50, list: List<M>,
      block: suspend (list: List<M>) -> List<M>
    ): List<M> = withContext(Dispatchers.IO) {
      val resultList = mutableListOf<M>()
      if (list.isNotEmpty()) {
        if (list.size <= stepValue) {
          resultList.addAll(block(list))
        } else {
          var i = 0
          while (i < list.size) {
            val tempList = if (list.size - i > stepValue) {
              list.subList(i, i + stepValue)
            } else {
              list.subList(i, list.size)
            }
            resultList.addAll(block(tempList))
            i += stepValue
          }
        }
      }

      return@withContext resultList
    }

    suspend fun getWholeMimeMessageInputStream(
      context: Context,
      account: AccountEntity?,
      messageId: String
    ): InputStream = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, account)

      val message = gmailApiService
        .users()
        .messages()
        .get(DEFAULT_USER_ID, messageId)
        .setFormat(MESSAGE_RESPONSE_FORMAT_RAW)
      message.fields = "raw"

      return@withContext Base64InputStream(GMailRawMIMEMessageFilterInputStream(message.executeAsInputStream()))
    }

    suspend fun loadThreads(
      context: Context,
      accountEntity: AccountEntity,
      localFolder: LocalFolder,
      maxResult: Long = COUNT_OF_LOADED_EMAILS_BY_STEP,
      fields: List<String>? = null,
      nextPageToken: String? = null
    ): ListThreadsResponse = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val request = gmailApiService
        .users()
        .threads()
        .list(DEFAULT_USER_ID)
        .setPageToken(nextPageToken)
        .setMaxResults(maxResult)

      fields?.let { fields ->
        request.fields = fields.joinToString(separator = ",")
      }

      if (!localFolder.isAll) {
        request.labelIds = listOf(localFolder.fullName)
      }

      if (localFolder.searchQuery.isNullOrEmpty()) {
        if (accountEntity.showOnlyEncrypted == true) {
          request.q =
            (EmailUtil.genPgpThingsSearchTerm(accountEntity) as? GmailRawSearchTerm)?.pattern
        }
      } else {
        request.q = (EmailUtil.generateSearchTerm(
          accountEntity,
          localFolder
        ) as? GmailRawSearchTerm)?.pattern
      }

      return@withContext request.execute()
    }

    suspend fun loadMsgsBaseInfo(
      context: Context,
      accountEntity: AccountEntity,
      localFolder: LocalFolder,
      maxResult: Long = COUNT_OF_LOADED_EMAILS_BY_STEP,
      fields: List<String>? = null,
      nextPageToken: String? = null
    ): GenericJson? = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val request = if (localFolder.isDrafts) {
        gmailApiService
          .users()
          .drafts()
          .list(DEFAULT_USER_ID)
          .setPageToken(nextPageToken)
          .setMaxResults(maxResult)
      } else {
        gmailApiService
          .users()
          .messages()
          .list(DEFAULT_USER_ID)
          .setPageToken(nextPageToken)
          .setMaxResults(maxResult)
      }

      fields?.let { fields ->
        request.fields = fields.joinToString(separator = ",")
      }

      if (request is Gmail.Users.Messages.List) {
        if (!localFolder.isAll) {
          request.labelIds = listOf(localFolder.fullName)
        }
        if (accountEntity.showOnlyEncrypted == true) {
          request.q =
            (EmailUtil.genPgpThingsSearchTerm(accountEntity) as? GmailRawSearchTerm)?.pattern
        }
      }

      return@withContext request.execute()
    }

    /**
     * This method is responsible for loading messages. If the input list of Message large than the
     * twice value of <code>stepValue</code> we will use parallel requests to minimize latency
     */
    suspend fun loadMsgsInParallel(
      context: Context,
      accountEntity: AccountEntity,
      messages: List<Message>,
      localFolder: LocalFolder,
      format: String = RESPONSE_FORMAT_FULL,
      stepValue: Int = 10
    ): List<Message> = withContext(Dispatchers.IO)
    {
      return@withContext useParallel(list = messages, stepValue = stepValue) { list ->
        loadMsgs(context, accountEntity, list, localFolder, format)
      }
    }

    suspend fun loadGmailThreadInfoInParallel(
      context: Context,
      accountEntity: AccountEntity,
      localFolder: LocalFolder? = null,
      threads: List<Thread>,
      format: String = RESPONSE_FORMAT_FULL,
      fields: List<String>? = null,
      stepValue: Int = 10
    ): List<GmailThreadInfo> = withContext(Dispatchers.IO)
    {
      return@withContext useParallel(list = threads, stepValue = stepValue) { list ->
        loadThreadsInfo(
          context = context,
          accountEntity = accountEntity,
          localFolder = localFolder,
          threads = list,
          format = format,
          fields = fields
        )
      }
    }

    suspend fun loadThreadsInfo(
      context: Context,
      accountEntity: AccountEntity,
      localFolder: LocalFolder? = null,
      threads: Collection<Thread>,
      format: String = RESPONSE_FORMAT_FULL,
      metadataHeaders: List<String>? = null,
      fields: List<String>? = null
    ): List<GmailThreadInfo> = withContext(Dispatchers.IO)
    {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val batch = gmailApiService.batch()

      val listResult = mutableListOf<GmailThreadInfo>()

      for (thread in threads) {
        val request = gmailApiService
          .users()
          .threads()
          .get(DEFAULT_USER_ID, thread.id)
          .setFormat(format)

        metadataHeaders?.let { metadataHeaders ->
          request.metadataHeaders = metadataHeaders
        }

        fields?.let { fields ->
          request.fields = fields.joinToString(separator = ",")
        }

        request.queue(
          batch,
          object : JsonBatchCallback<Thread>() {
            override fun onSuccess(
              t: Thread?,
              responseHeaders: HttpHeaders?
            ) {
              t.toThreadInfo(context, accountEntity, localFolder)?.let { threadInfo ->
                listResult.add(threadInfo)
              }
            }

            override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
              IllegalStateException(e?.toPrettyString()).printStackTraceIfDebugOnly()
            }
          })
      }

      batch.execute()

      return@withContext listResult
    }

    suspend fun loadThreadInfo(
      context: Context,
      accountEntity: AccountEntity,
      localFolder: LocalFolder,
      threadId: String,
      format: String = RESPONSE_FORMAT_FULL,
      metadataHeaders: List<String>? = null,
      fields: List<String>? = null
    ): GmailThreadInfo? = withContext(Dispatchers.IO)
    {
      return@withContext getThread(
        context = context,
        accountEntity = accountEntity,
        threadId = threadId,
        format = format,
        metadataHeaders = metadataHeaders,
        fields = fields
      ).toThreadInfo(context, accountEntity, localFolder)
    }

    suspend fun getThread(
      context: Context,
      accountEntity: AccountEntity,
      threadId: String,
      format: String = RESPONSE_FORMAT_FULL,
      metadataHeaders: List<String>? = null,
      fields: List<String>? = null
    ): Thread? = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)

      val request = gmailApiService
        .users()
        .threads()
        .get(DEFAULT_USER_ID, threadId)
        .setFormat(format)

      metadataHeaders?.let { metadataHeaders ->
        request.metadataHeaders = metadataHeaders
      }

      fields?.let { fields ->
        request.fields = fields.joinToString(separator = ",")
      }

      return@withContext request.execute()
    }

    suspend fun loadMsgs(
      context: Context, accountEntity: AccountEntity, messages: Collection<Message>,
      localFolder: LocalFolder, format: String = RESPONSE_FORMAT_FULL,
      metadataHeaders: List<String>? = null, fields: List<String>? = FULL_INFO_WITHOUT_DATA
    ): List<Message> = withContext(Dispatchers.IO)
    {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val batch = gmailApiService.batch()

      val listResult = mutableListOf<Message>()
      val isTrash = localFolder.fullName.equals(LABEL_TRASH, true)

      for (message in messages) {
        val request = gmailApiService
          .users()
          .messages()
          .get(DEFAULT_USER_ID, message.id)
          .setFormat(format)

        metadataHeaders?.let { metadataHeaders ->
          request.metadataHeaders = metadataHeaders
        }

        fields?.let { fields ->
          request.fields = fields.joinToString(separator = ",")
        }

        request.queue(batch, object : JsonBatchCallback<Message>() {
          override fun onSuccess(t: Message?, responseHeaders: HttpHeaders?) {
            t?.let {
              if (isTrash || it.labelIds?.contains(LABEL_TRASH) != true) {
                listResult.add(it)
              }
            } ?: throw java.lang.NullPointerException()
          }

          override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {

          }
        })
      }

      batch.execute()

      return@withContext listResult
    }

    suspend fun getLabels(context: Context, account: AccountEntity?): List<Label> =
      withContext(Dispatchers.IO) {
        val gmailApiService = generateGmailApiService(context, account)

        val response = gmailApiService
          .users()
          .labels()
          .list(DEFAULT_USER_ID)
          .execute()

        return@withContext response.labels ?: emptyList()
      }

    suspend fun changeLabels(
      context: Context, accountEntity: AccountEntity,
      ids: Collection<String>,
      addLabelIds: List<String>? = null,
      removeLabelIds: List<String>? = null
    ) = withContext(Dispatchers.IO) {
      if (addLabelIds == null && removeLabelIds == null) return@withContext
      val gmailApiService = generateGmailApiService(context, accountEntity)

      gmailApiService
        .users()
        .messages()
        .batchModify(DEFAULT_USER_ID, BatchModifyMessagesRequest().apply {
          this.ids = ids.toList()
          this.addLabelIds = addLabelIds
          this.removeLabelIds = removeLabelIds
        })
        .execute()
    }

    suspend fun changeLabelsForThreads(
      context: Context,
      accountEntity: AccountEntity,
      threadIdList: Collection<String>,
      addLabelIds: List<String>? = null,
      removeLabelIds: List<String>? = null
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
      if (addLabelIds == null && removeLabelIds == null) {
        return@withContext emptyMap<String, Boolean>()
      }

      val gmailApiService = generateGmailApiService(context, accountEntity)
      val batch = gmailApiService.batch()

      val resultMap = mutableMapOf<String, Boolean>()
      threadIdList.forEach { threadId ->
        val request = gmailApiService
          .users()
          .threads()
          .modify(DEFAULT_USER_ID, threadId, ModifyThreadRequest().apply {
            this.addLabelIds = addLabelIds
            this.removeLabelIds = removeLabelIds
          })

        request.queue(batch, object : JsonBatchCallback<Thread>() {
          override fun onSuccess(t: Thread?, responseHeaders: HttpHeaders?) {
            resultMap[threadId] = true
          }

          override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
            resultMap[threadId] = true
          }
        })
      }

      batch.execute()

      return@withContext resultMap
    }

    suspend fun deleteMsgsPermanently(
      context: Context, accountEntity: AccountEntity,
      ids: List<String>
    ) {
      withContext(Dispatchers.IO) {
        val gmailApiService = generateGmailApiService(context, accountEntity)
        gmailApiService
          .users()
          .messages()
          .batchDelete(DEFAULT_USER_ID, BatchDeleteMessagesRequest().apply {
            this.ids = ids
          })
          .execute()
      }
    }

    suspend fun deleteThreadsPermanently(
      context: Context, accountEntity: AccountEntity,
      ids: Collection<String>
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val batch = gmailApiService.batch()
      val resultMap = mutableMapOf<String, Boolean>()
      ids.forEach { id ->
        val request = gmailApiService
          .users()
          .threads()
          .delete(DEFAULT_USER_ID, id)
        request.queue(batch, object : JsonBatchCallback<Void>() {
          override fun onSuccess(t: Void?, responseHeaders: HttpHeaders?) {
            resultMap[id] = true
          }

          override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
            resultMap[id] = false
          }
        })
      }

      batch.execute()
      return@withContext resultMap
    }

    suspend fun deleteDrafts(
      context: Context,
      accountEntity: AccountEntity,
      ids: List<String>,
      stepValue: Int = 20
    ) = withContext(Dispatchers.IO) {
      useParallel(list = ids, stepValue = stepValue) { list ->
        val gmailApiService = generateGmailApiService(context, accountEntity)
        val batch = gmailApiService.batch()

        val result = mutableListOf<Pair<String, Boolean>>()
        for (id in list) {
          val request = gmailApiService
            .users()
            .drafts()
            .delete(DEFAULT_USER_ID, id)

          request.queue(batch, object : JsonBatchCallback<Void>() {
            override fun onSuccess(t: Void?, responseHeaders: HttpHeaders?) {
              result.add(Pair(id, true))
            }

            override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
              result.add(Pair(id, false))
            }
          })
        }

        batch.execute()
        return@useParallel result
      }.associateBy({ it.first }, { it.second })
    }

    suspend fun moveToTrash(
      context: Context,
      accountEntity: AccountEntity,
      ids: List<String>
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val batch = gmailApiService.batch()
      val resultMap = mutableMapOf<String, Boolean>()

      ids.forEach { id ->
        val request = gmailApiService
          .users()
          .messages()
          .trash(DEFAULT_USER_ID, id)
        request.queue(batch, object : JsonBatchCallback<Message>() {
          override fun onSuccess(t: Message?, responseHeaders: HttpHeaders?) {
            resultMap[id] = true
          }

          override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
            resultMap[id] = false
          }
        })
      }

      batch.execute()
      return@withContext resultMap
    }

    suspend fun moveThreadsToTrash(
      context: Context,
      accountEntity: AccountEntity,
      ids: Collection<String>
    ): Map<String, Boolean> = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val batch = gmailApiService.batch()

      val resultMap = mutableMapOf<String, Boolean>()
      ids.forEach { id ->
        val request = gmailApiService
          .users()
          .threads()
          .trash(DEFAULT_USER_ID, id)
        request.queue(batch, object : JsonBatchCallback<Thread>() {
          override fun onSuccess(t: Thread?, responseHeaders: HttpHeaders?) {
            resultMap[id] = true
          }

          override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
            resultMap[id] = false
          }
        })
      }

      batch.execute()
      return@withContext resultMap
    }

    suspend fun loadTrashMsgs(context: Context, accountEntity: AccountEntity): List<Message> =
      withContext(Dispatchers.IO) {
        val gmailApiService = generateGmailApiService(context, accountEntity)

        var response = gmailApiService
          .users()
          .messages()
          .list(DEFAULT_USER_ID)
          .setLabelIds(listOf(LABEL_TRASH))
          .execute()

        val msgs = mutableListOf<Message>()

        //Try to load all messages. Only base info
        while (response.messages != null) {
          msgs.addAll(response.messages)
          if (response.nextPageToken != null) {
            response = gmailApiService
              .users()
              .messages()
              .list(DEFAULT_USER_ID)
              .setPageToken(response.nextPageToken)
              .execute()
          } else {
            break
          }
        }

        return@withContext msgs
      }

    suspend fun loadHistoryInfo(
      context: Context, accountEntity: AccountEntity,
      localFolder: LocalFolder, historyId: BigInteger
    ): List<History> = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      var response = gmailApiService
        .users()
        .history()
        .list(DEFAULT_USER_ID)
        .setStartHistoryId(historyId)
        .apply {
          if (!localFolder.isAll) {
            labelId = localFolder.fullName
          }
        }
        .execute()

      val historyList = mutableListOf<History>()
      response.history?.let { historyList.addAll(it) }

      //Try to load all history
      while (response.nextPageToken?.isNotEmpty() == true) {
        response = gmailApiService
          .users()
          .history()
          .list(DEFAULT_USER_ID)
          .setStartHistoryId(historyId)
          .apply {
            if (!localFolder.isAll) {
              labelId = localFolder.fullName
            }
          }
          .setPageToken(response.nextPageToken)
          .execute()

        response.history?.let { historyList.addAll(it) }
      }

      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      val labelEntity = roomDatabase.labelDao()
        .getLabelSuspend(accountEntity.email, accountEntity.accountType, localFolder.fullName)
      labelEntity?.let { folder ->
        roomDatabase.labelDao()
          .updateSuspend(folder.copy(historyId = response?.historyId?.toString()))
      }

      return@withContext historyList
    }

    suspend fun identifyAttachments(
      msgEntities: List<MessageEntity>, msgs: List<Message>,
      account: AccountEntity, localFolder: LocalFolder, roomDatabase: FlowCryptRoomDatabase
    ) = withContext(Dispatchers.IO) {
      val savedMsgUIDsSet = msgEntities.map { it.uid }.toSet()
      val attachments = mutableListOf<AttachmentEntity>()
      for (msg in msgs) {
        try {
          if (msg.uid in savedMsgUIDsSet) {
            attachments.addAll(msg.payload.getAttachmentInfoList().mapNotNull { attachmentInfo ->
              AttachmentEntity.fromAttInfo(
                attachmentInfo = attachmentInfo.copy(
                  email = account.email,
                  folder = localFolder.fullName,
                  uid = msg.uid
                ),
                accountType = account.accountType
              )
            })
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
        }
      }
      roomDatabase.attachmentDao().insertWithReplaceSuspend(attachments)
    }

    suspend fun loadMsgsBaseInfoUsingSearch(
      context: Context, accountEntity: AccountEntity,
      localFolder: LocalFolder, nextPageToken: String? = null
    ):
        ListMessagesResponse = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val list = gmailApiService
        .users()
        .messages()
        .list(DEFAULT_USER_ID)
        .setQ(
          (EmailUtil.generateSearchTerm(
            accountEntity,
            localFolder
          ) as? GmailRawSearchTerm)?.pattern
        )
        .setPageToken(nextPageToken)
        .setMaxResults(COUNT_OF_LOADED_EMAILS_BY_STEP)
      return@withContext list.execute()
    }

    /**
     * Retrieve a Gmail message thread id.
     *
     * @param service          A [Gmail] reference.
     * @param rfc822msgidValue An rfc822 Message-Id value of the input message.
     * @return The input message thread id.
     * @throws IOException
     */
    suspend fun getGmailMsgThreadID(service: Gmail, rfc822msgidValue: String): String? =
      withContext(Dispatchers.IO) {
        val response = service
          .users()
          .messages()
          .list(DEFAULT_USER_ID)
          .setQ("rfc822msgid:$rfc822msgidValue")
          .execute()

        return@withContext response?.messages?.firstOrNull()?.threadId
      }

    suspend fun sendMsg(
      context: Context,
      account: AccountEntity,
      mimeMessage: jakarta.mail.Message
    ): Boolean = withContext(Dispatchers.IO) {
      val gmail = generateGmailApiService(context, account)
      val outputStream = ByteArrayOutputStream()
      mimeMessage.writeTo(outputStream)

      val sentMsg = Message().apply {
        raw = Base64.encodeToString(
          outputStream.toByteArray(),
          Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
      }

      gmail
        .users()
        .messages()
        .send(DEFAULT_USER_ID, sentMsg)
        .execute()

      return@withContext sentMsg.id == null
    }

    suspend fun loadMsgInfoSuspend(
      context: Context,
      accountEntity: AccountEntity,
      msgId:
      String,
      fields: List<String>? = FULL_INFO_WITHOUT_DATA,
      format: String
    ): Message = withContext(Dispatchers.IO) {
      return@withContext loadMsgInfo(
        context = context,
        accountEntity = accountEntity,
        msgId = msgId,
        fields = fields,
        format = format
      )
    }

    /**
     * Get a list of [PgpKeyRingDetails] using the **Gmail API**
     *
     * @param context context Interface to global information about an application environment;
     * @param account An [AccountEntity] object.
     * @return A list of [PgpKeyRingDetails]
     * @throws MessagingException
     * @throws IOException
     */
    suspend fun getPrivateKeyBackups(
      context: Context,
      account: AccountEntity
    ): List<PgpKeyRingDetails> = withContext(Dispatchers.IO) {
      try {
        val list = mutableListOf<PgpKeyRingDetails>()

        val searchQuery = EmailUtil.getGmailBackupSearchQuery(account.email)
        val gmailApiService = generateGmailApiService(context, account)

        var response = gmailApiService
          .users()
          .messages()
          .list(DEFAULT_USER_ID)
          .setQ(searchQuery)
          .execute()

        val msgs = mutableListOf<Message>()

        //Try to load all backups
        while (response.messages != null) {
          msgs.addAll(response.messages)
          if (response.nextPageToken != null) {
            response = gmailApiService
              .users()
              .messages()
              .list(DEFAULT_USER_ID)
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
            .get(DEFAULT_USER_ID, origMsg.id)
            .setFormat(MESSAGE_RESPONSE_FORMAT_RAW)
            .execute()

          val stream = ByteArrayInputStream(Base64.decode(message.raw, Base64.URL_SAFE))
          val msg = MimeMessage(Session.getInstance(Properties()), stream)
          val backup = EmailUtil.getKeyFromMimeMsg(msg).takeIf { it.isNotEmpty() } ?: continue

          list.addAll(PgpKey.parseKeys(source = backup).pgpKeyDetailsList.map {
            it.copy(
              importInfo = (it.importInfo ?: PgpKeyRingDetails.ImportInfo()).copy(
                importSourceType = KeyImportDetails.SourceType.EMAIL
              )
            )
          })
        }

        return@withContext list
      } catch (e: UserRecoverableAuthIOException) {
        ErrorNotificationManager(context).notifyUserAboutAuthFailure(account, e.intent)
        throw e
      } catch (e: UserRecoverableAuthException) {
        ErrorNotificationManager(context).notifyUserAboutAuthFailure(account, e.intent)
        throw e
      }
    }

    fun loadMsgInfo(
      context: Context,
      accountEntity: AccountEntity,
      msgId: String,
      fields: List<String>? = FULL_INFO_WITHOUT_DATA,
      format: String
    ): Message {
      val gmailApiService = generateGmailApiService(context, accountEntity)

      return gmailApiService
        .users()
        .messages()
        .get(DEFAULT_USER_ID, msgId)
        .setFormat(format)
        .setFields(fields?.joinToString(separator = ","))
        .execute()
    }

    /**
     * Get [Part] which has an attachment with the given attachment path.
     *
     * @param part         The parent part.
     * @param currentPath  The current path of MIME hierarchy.
     * @param neededPath   The path where the needed attachment exists.
     * @return [Part] which has attachment or null if message doesn't have such attachment.
     */
    fun getAttPartByPath(
      part: MessagePart,
      currentPath: String = "0/",
      neededPath: String
    ): MessagePart? {
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val neededParentPath =
          neededPath.substringBeforeLast(AttachmentInfo.DEPTH_SEPARATOR) + AttachmentInfo.DEPTH_SEPARATOR
        val partsCount = (part.parts ?: emptyList()).size

        if (currentPath == neededParentPath) {
          val position = neededPath.substringAfterLast(AttachmentInfo.DEPTH_SEPARATOR).toInt()

          if (partsCount > position) {
            val bodyPart = part.parts[position]
            if (Part.ATTACHMENT.equals(bodyPart.disposition(), ignoreCase = true)) {
              return bodyPart
            }
          }
        } else {
          val nextDepth = neededParentPath
            .replaceFirst(currentPath, "")
            .split(AttachmentInfo.DEPTH_SEPARATOR).first().toInt()
          val bodyPart = part.parts[nextDepth]
          return getAttPartByPath(
            bodyPart,
            currentPath + nextDepth + AttachmentInfo.DEPTH_SEPARATOR,
            neededPath
          )
        }
        return null
      } else {
        return null
      }
    }

    fun getAttInputStream(
      context: Context,
      accountEntity: AccountEntity,
      msgId: String,
      attId: String,
      decodeBase64: Boolean = true
    ): InputStream {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val request = gmailApiService
        .users()
        .messages()
        .attachments()
        .get(DEFAULT_USER_ID, msgId, attId)
        .setPrettyPrint(false)
        .setFields("data")

      return if (decodeBase64) {
        Base64InputStream(GMailRawAttachmentFilterInputStream(request.executeAsInputStream()))
      } else {
        GMailRawAttachmentFilterInputStream(request.executeAsInputStream())
      }
    }

    suspend fun uploadDraft(
      context: Context,
      account: AccountEntity,
      mimeMessage: jakarta.mail.Message,
      draftId: String? = null,
      threadId: String? = null
    ): Draft = withContext(Dispatchers.IO) {
      val gmail = generateGmailApiService(context, account)
      val outputStream = ByteArrayOutputStream()
      mimeMessage.writeTo(outputStream)

      val draftMsg = outputStream.use {
        Message().apply {
          raw = Base64.encodeToString(
            it.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
          )

          setThreadId(threadId)
        }
      }

      val draft = Draft().apply { message = draftMsg }
      val gmailUsersDrafts = gmail
        .users()
        .drafts()
      return@withContext if (draftId == null) {
        gmailUsersDrafts
          .create(DEFAULT_USER_ID, draft)
          .execute()
      } else {
        gmailUsersDrafts
          .update(DEFAULT_USER_ID, draftId, draft)
          .execute()
      }
    }

    suspend fun getDraftByMsgId(
      context: Context,
      account: AccountEntity,
      msgId: String
    ): Draft? =
      withContext(Dispatchers.IO) {
        val gmail = generateGmailApiService(context, account)

        val rfc822msgid =
          getHeaderValueByMessageId(context, account, msgId, JavaEmailConstants.HEADER_MESSAGE_ID)

        val response = gmail
          .users()
          .drafts()
          .list(DEFAULT_USER_ID)
          .setQ("rfc822msgid:${rfc822msgid}")
          .execute()

        return@withContext response?.drafts?.firstOrNull()
      }

    suspend fun loadBaseDraftInfoInParallel(
      context: Context,
      accountEntity: AccountEntity,
      messages: List<Message>,
      stepValue: Int = 10
    ): List<Draft> = withContext(Dispatchers.IO)
    {
      return@withContext useParallel(list = messages, stepValue = stepValue) { list ->
        loadDrafts(context, accountEntity, list)
      }
    }

    private suspend fun <T, V> useParallel(
      list: List<T>,
      stepValue: Int = 10,
      action: suspend (subList: List<T>) -> List<V>
    ): List<V> = withContext(Dispatchers.IO)
    {
      val useParallel = list.size > stepValue * 2
      val steps = mutableListOf<Deferred<List<V>>>()

      if (list.isNotEmpty()) {
        if (list.size <= stepValue && !useParallel) {
          steps.add(async { action.invoke(list) })
        } else {
          var i = 0
          while (i < list.size) {
            val tempList = if (list.size - i > stepValue) {
              list.subList(i, i + stepValue)
            } else {
              list.subList(i, list.size)
            }
            steps.add(async { action.invoke(tempList) })
            i += stepValue
          }
        }
      }

      return@withContext awaitAll(*steps.toTypedArray()).flatten()
    }

    private suspend fun loadDrafts(
      context: Context,
      accountEntity: AccountEntity,
      messages: Collection<Message>,
      fields: List<String>? = listOf("drafts/id", "drafts/message/id")
    ): List<Draft> = withContext(Dispatchers.IO)
    {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val request = gmailApiService
        .users()
        .drafts()
        .list(DEFAULT_USER_ID)
        .setQ(messages.joinToString(separator = " OR ") { item ->
          "rfc822msgid:${
            item.payload.headers.first {
              JavaEmailConstants.HEADER_MESSAGE_ID.equals(it.name, true)
            }.value
          }"
        })

      fields?.let { fields ->
        request.fields = fields.joinToString(separator = ",")
      }

      return@withContext request.execute()?.drafts ?: emptyList()
    }

    /**
     * Generate [GoogleAccountCredential] which will be used with Gmail API.
     *
     * @param context Interface to global information about an application environment.
     * @param account The Gmail account.
     * @return Generated [GoogleAccountCredential].
     */
    private fun generateGoogleAccountCredential(
      context: Context,
      account: Account?
    ): GoogleAccountCredential {
      return GoogleAccountCredential.usingOAuth2(context, listOf(*SCOPES))
        .setSelectedAccount(account)
    }

    fun labelsToImapFlags(labelIds: Collection<String>): Flags {
      val flags = Flags()
      labelIds.forEach {
        when (it) {
          LABEL_DRAFT -> flags.add(Flags.Flag.DRAFT)
          LABEL_UNREAD -> {}//do nothing
          else -> flags.add(it)
        }
      }

      if (!labelIds.contains(LABEL_UNREAD)) {
        flags.add(Flags.Flag.SEEN)
      }

      return flags
    }

    private suspend fun getHeaderValueByMessageId(
      context: Context,
      account: AccountEntity?,
      msgId: String,
      headerName: String
    ): String? = withContext(Dispatchers.IO) {
      val gmailApiService = generateGmailApiService(context, account)

      val message = gmailApiService
        .users()
        .messages()
        .get(DEFAULT_USER_ID, msgId)
        .setFormat(MESSAGE_RESPONSE_FORMAT_METADATA)
      message.metadataHeaders = listOf(headerName)

      return@withContext message.execute()?.payload?.headers?.firstOrNull {
        headerName.equals(it.name, true)
      }?.value
    }

    private fun processException(e: Throwable): Throwable {
      return when (e) {
        is GoogleJsonResponseException -> {
          GmailAPIException(e)
        }

        is ProtocolException, is SSLException, is SocketTimeoutException, is UnknownHostException -> {
          CommonConnectionException(e)
        }

        else -> e.cause?.let {
          processException(it)
        } ?: e
      }
    }
  }
}
