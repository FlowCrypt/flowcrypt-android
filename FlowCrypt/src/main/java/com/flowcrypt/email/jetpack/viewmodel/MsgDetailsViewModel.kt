/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.IncomingMessageInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.PublicKeyMsgBlock
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.cache.DiskLruCache
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.util.ASCIIUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import org.spongycastle.bcpg.ArmoredInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.MimeMessage

/**
 * @author Denis Bondarenko
 *         Date: 12/26/19
 *         Time: 4:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class MsgDetailsViewModel(val localFolder: LocalFolder, val msgEntity: MessageEntity,
                          application: Application) : BaseAndroidViewModel(application) {
  private val roomDatabase = FlowCryptRoomDatabase.getDatabase(application)
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(application)
  private val apiRepository: PgpApiRepository = NodeRepository()

  val msgStatesLiveData = MutableLiveData<MessageState>()
  val attsLiveData = roomDatabase.attachmentDao().getAttachmentsLD(
      account = msgEntity.email,
      label = msgEntity.folder,
      uid = msgEntity.uid
  )

  private val msgLiveData: LiveData<MessageEntity?> = roomDatabase.msgDao().getMsgLiveData(
      account = msgEntity.email,
      folder = msgEntity.folder,
      uid = msgEntity.uid
  )
  private val processMsgLiveData: MediatorLiveData<Result<ParseDecryptedMsgResult?>> = MediatorLiveData()
  private val processOutgoingMsgLiveData: LiveData<Result<ParseDecryptedMsgResult?>> = Transformations.switchMap(msgLiveData) { messageEntity ->
    liveData {
      if (messageEntity?.isOutboxMsg() == true) {
        val byteArray = messageEntity.rawMessageWithoutAttachments?.toByteArray()

        val result = if (byteArray == null) {
          val context: Context = getApplication()
          Result.exception(throwable = IllegalArgumentException(context.getString(R.string.unknown_error)))
        } else {
          emit(Result.loading())

          val result = apiRepository.parseDecryptMsg(
              request = ParseDecryptMsgRequest(
                  data = byteArray,
                  keyEntities = keysStorage.getLatestAllPgpPrivateKeys(),
                  isEmail = true
              ))

          modifyMsgBlocksIfNeeded(result)
          result
        }

        emit(result)
      }
    }
  }

  val incomingMessageInfoLiveData: LiveData<Result<IncomingMessageInfo>> = Transformations.switchMap(processMsgLiveData) {
    liveData {
      val context: Context = getApplication()
      val result = when (it.status) {
        Result.Status.LOADING -> {
          Result.loading(requestCode = it.requestCode, progressMsg = it.progressMsg)
        }

        Result.Status.SUCCESS -> {
          val parseDecryptedMsgResult = it.data
          if (parseDecryptedMsgResult != null) {
            val msgInfo = IncomingMessageInfo(
                msgEntity = msgEntity,
                text = parseDecryptedMsgResult.text,
                subject = parseDecryptedMsgResult.subject,
                msgBlocks = parseDecryptedMsgResult.msgBlocks ?: emptyList(),
                origMsgHeaders = null,
                encryptionType = parseDecryptedMsgResult.getMsgEncryptionType()
            )
            Result.success(requestCode = it.requestCode, data = msgInfo)
          } else {
            Result.exception(requestCode = it.requestCode, throwable = IllegalStateException(context.getString(R.string.unknown_error)))
          }
        }

        Result.Status.ERROR -> {
          Result.exception(requestCode = it.requestCode, throwable = ApiException(it.data?.apiError))
        }

        Result.Status.EXCEPTION -> {
          Result.exception(requestCode = it.requestCode, throwable = it.exception
              ?: IllegalStateException(context.getString(R.string.unknown_error)))
        }

        Result.Status.NONE -> {
          Result.none()
        }
      }

      emit(result)
    }
  }

  init {
    processMsgLiveData.addSource(processOutgoingMsgLiveData) { processMsgLiveData.value = it }
  }

  fun setSeenStatus(isSeen: Boolean) {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().updateSuspend(msgEntity.copy(flags = if (isSeen) {
          if (msgEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
            msgEntity.flags
          } else {
            msgEntity.flags?.plus("${MessageFlag.SEEN.value} ")
          }
        } else {
          msgEntity.flags?.replace(MessageFlag.SEEN.value, "")
        }))
      }
    }
  }

  fun changeMsgState(newMsgState: MessageState) {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        val candidate: MessageEntity = when (newMsgState) {
          MessageState.PENDING_MARK_READ -> {
            msgEntity.copy(
                state = newMsgState.value,
                flags = if (msgEntity.flags?.contains(MessageFlag.SEEN.value) == true) {
                  msgEntity.flags
                } else {
                  msgEntity.flags?.plus("${MessageFlag.SEEN.value} ")
                })
          }

          MessageState.PENDING_MARK_UNREAD -> {
            msgEntity.copy(
                state = newMsgState.value,
                flags = msgEntity.flags?.replace(MessageFlag.SEEN.value, ""))
          }

          else -> {
            msgEntity.copy(state = newMsgState.value)
          }
        }

        roomDatabase.msgDao().updateSuspend(candidate)
        msgStatesLiveData.postValue(newMsgState)
      }
    }
  }

  fun deleteMsg() {
    val freshMsgEntity = msgLiveData.value
    freshMsgEntity?.let { msgEntity ->
      viewModelScope.launch {
        roomDatabase.msgDao().deleteSuspend(msgEntity)

        if (JavaEmailConstants.FOLDER_OUTBOX.equals(localFolder.fullName, ignoreCase = true)) {
          val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsSuspend(msgEntity.email).size
          val outboxLabel = roomDatabase.labelDao().getLabelSuspend(msgEntity.email,
              JavaEmailConstants.FOLDER_OUTBOX)

          outboxLabel?.let {
            roomDatabase.labelDao().updateSuspend(it.copy(msgsCount = outgoingMsgCount))
          }
        }
      }
    }
  }

  fun decryptMessage(rawMimeBytes: ByteArray) {
    viewModelScope.launch {
      processMsgLiveData.value = Result.loading()
      /*ByteArrayInputStream(rawMimeBytes).use {
        headersLiveData.postValue(getHeaders(it))
      }*/
      val list = keysStorage.getLatestAllPgpPrivateKeys()
      val result = apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(data = rawMimeBytes, keyEntities = list, isEmail = true))

      modifyMsgBlocksIfNeeded(result)
      processMsgLiveData.value = result
    }
  }

  fun decryptMessage(context: Context, msgSnapshot: DiskLruCache.Snapshot) {
    viewModelScope.launch {
      processMsgLiveData.value = Result.loading()
      val uri = msgSnapshot.getUri(0)
      if (uri != null) {
        /*withContext(Dispatchers.IO) {
          context.contentResolver.openInputStream(uri)?.use { uriInputStream ->
            headersLiveData.postValue(getHeaders(uriInputStream, true))
          }
        }*/

        val list = keysStorage.getLatestAllPgpPrivateKeys()
        val largerThan1Mb = msgSnapshot.getLength(0) > 1024 * 1000
        val result = if (largerThan1Mb) {
          parseMimeAndDecrypt(context, uri, list)
        } else {
          apiRepository.parseDecryptMsg(
              request = ParseDecryptMsgRequest(context = context, uri = uri, keyEntities = list,
                  isEmail = true, hasEncryptedDataInUri = true))
        }

        modifyMsgBlocksIfNeeded(result)
        processMsgLiveData.value = result
      } else {
        //val byteArray = msgSnapshot.getByteArray(0)
        //decryptMessage(byteArray)
      }
    }
  }

  private suspend fun modifyMsgBlocksIfNeeded(result: Result<ParseDecryptedMsgResult?>) {
    result.data?.let { parseDecryptMsgResult ->
      for (block in parseDecryptMsgResult.msgBlocks ?: mutableListOf()) {
        if (block is PublicKeyMsgBlock) {
          val keyDetails = block.keyDetails ?: continue
          val pgpContact = keyDetails.primaryPgpContact
          val contactEntity = roomDatabase.contactsDao().getContactByEmailSuspend(pgpContact.email)
          block.existingPgpContact = contactEntity?.toPgpContact()
        }
      }
    }
  }

  private suspend fun parseMimeAndDecrypt(context: Context, uri: Uri, list: List<KeyEntity>):
      Result<ParseDecryptedMsgResult?> {
    val uriOfEncryptedPart = getUriOfEncryptedPart(context, uri)
    return if (uriOfEncryptedPart != null) {
      apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(context = context, uri = uriOfEncryptedPart, keyEntities = list, isEmail = false))
    } else {
      apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(context = context, uri = uri, keyEntities = list, isEmail = true, hasEncryptedDataInUri = true))
    }
  }

  private suspend fun getMimeMessageFromInputStream(context: Context, uri: Uri) =
      withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
          MimeMessage(null, KeyStoreCryptoManager.getCipherInputStream(inputStream))
        } else throw NullPointerException("Stream is empty")
      }

  private suspend fun getUriOfEncryptedPart(context: Context, uri: Uri): Uri? {
    val mimeMessage: MimeMessage = getMimeMessageFromInputStream(context, uri)
    return findEncryptedPart(mimeMessage)
  }

  private suspend fun findEncryptedPart(part: Part): Uri? = withContext(Dispatchers.Default) {
    try {
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val multiPart = part.content as Multipart
        val partsNumber = multiPart.count
        for (partCount in 0 until partsNumber) {
          val bodyPart = multiPart.getBodyPart(partCount)
          if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            val encryptedPart = findEncryptedPart(bodyPart)
            if (encryptedPart != null) {
              return@withContext encryptedPart
            }
          } else if (bodyPart?.disposition?.toLowerCase(Locale.getDefault()) in listOf(Part.ATTACHMENT, Part.INLINE)) {
            val fileName = bodyPart.fileName?.toLowerCase(Locale.getDefault()) ?: ""
            if (fileName in listOf("message", "msg.asc", "message.asc", "encrypted.asc", "encrypted.eml.pgp", "Message.pgp", "")) {
              val file = prepareTempFile(bodyPart)
              return@withContext Uri.fromFile(file)
            }

            val contentType = bodyPart.contentType?.toLowerCase(Locale.getDefault()) ?: ""
            if (contentType in listOf("application/octet-stream", "application/pgp-encrypted")) {
              val file = prepareTempFile(bodyPart)
              return@withContext Uri.fromFile(file)
            }
          }
        }
        return@withContext null
      } else {
        return@withContext null
      }
    } catch (e: MessagingException) {
      e.printStackTrace()
      return@withContext null
    } catch (e: IOException) {
      e.printStackTrace()
      return@withContext null
    }
  }

  private suspend fun prepareTempFile(bodyPart: BodyPart): File = withContext(Dispatchers.IO) {
    val tempDir = CacheManager.getCurrentMsgTempDir()
    val file = File(tempDir, FILE_NAME_ENCRYPTED_MESSAGE)
    IOUtils.copy(ArmoredInputStream(bodyPart.inputStream), FileOutputStream(file))
    return@withContext file
  }

  /**
   * We fetch the first 50Kb from the given input stream and extract headers.
   */
  private suspend fun getHeaders(inputStream: InputStream?,
                                 isDataEncrypted: Boolean = false): String = withContext(Dispatchers.IO) {
    inputStream ?: return@withContext ""
    val d = ByteArray(50000)
    try {
      if (isDataEncrypted) {
        IOUtils.read(KeyStoreCryptoManager.getCipherInputStream(inputStream), d)
      } else {
        IOUtils.read(inputStream, d)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
    EmailUtil.getHeadersFromRawMIME(ASCIIUtility.toString(d))
  }

  companion object {
    private const val FILE_NAME_ENCRYPTED_MESSAGE = "temp_encrypted_msg.asc"
  }
}