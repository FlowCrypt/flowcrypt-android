/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult
import com.flowcrypt.email.database.dao.UserIdEmailsKeysDao
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.database.entity.UserIdEmailsKeysEntity
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.service.actionqueue.actions.BackupPrivateKeyToInboxAction
import com.flowcrypt.email.service.actionqueue.actions.RegisterUserPublicKeyAction
import com.flowcrypt.email.service.actionqueue.actions.SendWelcomeTestEmailAction
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.gms.common.util.CollectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * This [ViewModel] implementation can be used to fetch details about imported keys.
 *
 * @author Denis Bondarenko
 * Date: 2/14/19
 * Time: 10:50 AM
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysViewModel(application: Application) : BaseNodeApiViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(getApplication())
  private val nodeRepository: PgpApiRepository = NodeRepository()
  private val apiRepository: ApiRepository = FlowcryptApiRepository()

  val changePassphraseLiveData = MutableLiveData<Result<Boolean>>()
  val saveBackupToInboxLiveData = MutableLiveData<Result<Boolean>>()
  val saveBackupAsFileLiveData = MutableLiveData<Result<Boolean>>()
  val savePrivateKeysLiveData = MutableLiveData<Result<Boolean>>()
  val parseKeysLiveData = MutableLiveData<Result<ParseKeysResult?>>()
  val createPrivateKeyLiveData = MutableLiveData<Result<NodeKeyDetails?>>()

  val longIdsOfCurrentAccountLiveData: LiveData<List<String>> = Transformations.switchMap(activeAccountLiveData) {
    roomDatabase.userIdEmailsKeysDao().getLongIdsByEmailLD(it?.email ?: "")
  }
  val userIdEmailsKeysLiveData = roomDatabase.userIdEmailsKeysDao().getAllLD()
  val privateKeyDetailsLiveData: LiveData<Result<ParseKeysResult?>> =
      Transformations.switchMap(keysStorage.keysLiveData) { keyEntities ->
        liveData {
          emit(Result.loading())
          val request = if (keyEntities.isNotEmpty()) {
            ParseKeysRequest(keyEntities.joinToString { it.privateKeyAsString + "\n" })
          } else {
            ParseKeysRequest(null)
          }
          emit(nodeRepository.fetchKeyDetails(request))
        }
      }

  fun changePassphrase(newPassphrase: String) {
    viewModelScope.launch {
      try {
        changePassphraseLiveData.value = Result.loading()
        val account = roomDatabase.accountDao().getActiveAccountSuspend()
        requireNotNull(account)

        val longIds = roomDatabase.userIdEmailsKeysDao().getLongIdsByEmailSuspend(account.email)
        val list = keysStorage.getFilteredPgpPrivateKeys(longIds.toTypedArray())

        if (CollectionUtils.isEmpty(list)) {
          throw NoPrivateKeysAvailableException(getApplication(), account.email)
        }

        roomDatabase.keysDao().updateSuspend(list.map { keyEntity ->
          with(getModifiedNodeKeyDetails(keyEntity.passphrase, newPassphrase, keyEntity.privateKeyAsString)) {
            if (isFullyDecrypted == true) {
              throw IllegalArgumentException("Error. The key is decrypted!")
            }

            keyEntity.copy(
                privateKey = KeyStoreCryptoManager.encryptSuspend(privateKey).toByteArray(),
                publicKey = publicKey?.toByteArray()
                    ?: throw NullPointerException("NodeKeyDetails.publicKey == null"),
                passphrase = KeyStoreCryptoManager.encryptSuspend(newPassphrase)
            )
          }
        })
        changePassphraseLiveData.value = Result.success(true)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        changePassphraseLiveData.value = Result.exception(e)
      }
    }
  }

  fun saveBackupsToInbox() {
    viewModelScope.launch {
      saveBackupToInboxLiveData.value = Result.loading()
      withContext(Dispatchers.IO) {
        try {
          val account = getAccountEntityWithDecryptedInfo(roomDatabase.accountDao().getActiveAccountSuspend())
          requireNotNull(account)

          val sess = OpenStoreHelper.getAccountSess(getApplication(), account)
          val transport = SmtpProtocolUtil.prepareSmtpTransport(getApplication(), sess, account)
          val msg = EmailUtil.genMsgWithAllPrivateKeys(getApplication(), account, sess)
          transport.sendMessage(msg, msg.allRecipients)
          saveBackupToInboxLiveData.postValue(Result.success(true))
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          saveBackupToInboxLiveData.postValue(Result.exception(e))
        }
      }
    }
  }

  fun saveBackupsAsFile(destinationUri: Uri) {
    viewModelScope.launch {
      saveBackupAsFileLiveData.value = Result.loading()
      withContext(Dispatchers.IO) {
        try {
          val account = roomDatabase.accountDao().getActiveAccountSuspend()
          requireNotNull(account)

          val backup = SecurityUtils.genPrivateKeysBackup(getApplication(), account)
          val result = GeneralUtil.writeFileFromStringToUri(getApplication(), destinationUri, backup) > 0

          saveBackupAsFileLiveData.postValue(Result.success(result))
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          saveBackupAsFileLiveData.postValue(Result.exception(e))
        }
      }
    }
  }

  fun encryptAndSaveKeysToDatabase(accountEntity: AccountEntity?, keys: List<NodeKeyDetails>, type: KeyDetails.Type) {
    requireNotNull(accountEntity)

    viewModelScope.launch {
      savePrivateKeysLiveData.value = Result.loading()
      try {
        val context: Context = getApplication()
        val totalList = mutableListOf<UserIdEmailsKeysEntity>()
        for (keyDetails in keys) {
          val longId = keyDetails.longId
          requireNotNull(longId)
          if (roomDatabase.keysDao().getKeyByLongIdSuspend(longId) == null) {
            val passphrase = if (keyDetails.isFullyDecrypted == true) "" else keyDetails.passphrase
                ?: ""
            val keyEntity = keyDetails.toKeyEntity(accountEntity.email.toLowerCase(Locale.US))
                .copy(source = type.toPrivateKeySourceTypeString(),
                    privateKey = KeyStoreCryptoManager.encryptSuspend(keyDetails.privateKey).toByteArray(),
                    passphrase = KeyStoreCryptoManager.encryptSuspend(passphrase))
            val isAdded = roomDatabase.keysDao().insertSuspend(keyEntity) > 0

            if (isAdded) {
              totalList.addAll(UserIdEmailsKeysDao.genEntities(context, keyDetails, keyDetails.pgpContacts))
            }
          }
        }
        roomDatabase.userIdEmailsKeysDao().insertWithReplaceSuspend(totalList)
        savePrivateKeysLiveData.value = Result.success(true)
      } catch (e: Exception) {
        e.printStackTrace()
        savePrivateKeysLiveData.value = Result.exception(SavePrivateKeyToDatabaseException(keys, e))
      }
    }
  }

  /**
   * Parse keys from the given resource (string or file).
   */
  fun parseKeys(keyImportModel: KeyImportModel?, isCheckSizeEnabled: Boolean) {
    viewModelScope.launch {
      val context: Context = getApplication()
      try {
        parseKeysLiveData.value = Result.loading()

        if (keyImportModel == null) {
          parseKeysLiveData.value = Result.success(ParseKeysResult(format = "unknown"))
          return@launch
        }

        val armoredSource: String
        when (keyImportModel.type) {
          KeyDetails.Type.FILE -> {
            if (isCheckSizeEnabled && isKeyTooBig(keyImportModel.fileUri)) {
              throw IllegalArgumentException(context.getString(R.string.file_is_too_big))
            }

            if (keyImportModel.fileUri == null) {
              throw NullPointerException("Uri is null!")
            }

            armoredSource = GeneralUtil.readFileFromUriToString(context, keyImportModel.fileUri)
                ?: throw NullPointerException(context.getString(R.string.source_is_empty_or_not_available))
          }

          KeyDetails.Type.CLIPBOARD, KeyDetails.Type.EMAIL, KeyDetails.Type.MANUAL_ENTERING -> {
            armoredSource = keyImportModel.keyString
                ?: throw NullPointerException(context.getString(R.string.source_is_empty_or_not_available))
          }
          else -> throw IllegalStateException("Unsupported : ${keyImportModel.type}")
        }

        parseKeysLiveData.value = nodeRepository.fetchKeyDetails(ParseKeysRequest(armoredSource))
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        parseKeysLiveData.value = Result.exception(e)
      }
    }
  }

  fun createPrivateKey(accountEntity: AccountEntity, passphrase: String) {
    viewModelScope.launch {
      createPrivateKeyLiveData.value = Result.loading()
      var nodeKeyDetails: NodeKeyDetails? = null
      try {
        nodeKeyDetails = genPrivateKeyViaNode(passphrase, accountEntity)
        requireNotNull(nodeKeyDetails)

        savePrivateKeyToDatabase(accountEntity, nodeKeyDetails, passphrase)
        doAdditionalOperationsAfterKeyCreation(accountEntity, nodeKeyDetails)
        createPrivateKeyLiveData.value = Result.success(nodeKeyDetails)
      } catch (e: Exception) {
        e.printStackTrace()
        nodeKeyDetails?.longId?.let {
          roomDatabase.keysDao().deleteByLongIdSuspend(it)
          roomDatabase.userIdEmailsKeysDao().deleteByLongIdSuspend(it)
        }
        createPrivateKeyLiveData.value = Result.exception(e)
        ExceptionUtil.handleError(e)
      }
    }
  }

  private suspend fun genPrivateKeyViaNode(passphrase: String, accountEntity: AccountEntity): NodeKeyDetails? {
    val generateKeyResult = nodeRepository.createPrivateKey(getApplication(), passphrase, genContacts(accountEntity))
    when (generateKeyResult.status) {
      Result.Status.SUCCESS -> {
        return generateKeyResult.data?.key
      }

      Result.Status.EXCEPTION -> {
        generateKeyResult.exception?.let { exception -> throw exception }
      }

      Result.Status.ERROR -> {
        generateKeyResult.data?.apiError?.let { apiError -> throw ApiException(apiError) }
      }

      else -> {
        // all looks well
      }
    }
    return null
  }

  private suspend fun savePrivateKeyToDatabase(accountEntity: AccountEntity, nodeKeyDetails: NodeKeyDetails, passphrase: String) {
    val keyEntity = nodeKeyDetails.toKeyEntity(accountEntity.email.toLowerCase(Locale.US)).copy(
        source = KeyDetails.Type.NEW.toPrivateKeySourceTypeString(),
        privateKey = KeyStoreCryptoManager.encryptSuspend(nodeKeyDetails.privateKey).toByteArray(),
        passphrase = KeyStoreCryptoManager.encryptSuspend(passphrase))

    if (roomDatabase.keysDao().insertSuspend(keyEntity) == -1L) {
      throw NullPointerException("Cannot save a generated private key")
    }

    //update "user_id_emails_and_keys" table
    nodeKeyDetails.longId?.let {
      roomDatabase.userIdEmailsKeysDao().insertWithReplaceSuspend(
          UserIdEmailsKeysEntity(longId = it, userIdEmail = nodeKeyDetails.primaryPgpContact.email))
    }
  }

  private suspend fun doAdditionalOperationsAfterKeyCreation(accountEntity: AccountEntity, nodeKeyDetails: NodeKeyDetails) {
    if (accountEntity.isRuleExist(AccountEntity.DomainRule.ENFORCE_ATTESTER_SUBMIT)) {
      val model = InitialLegacySubmitModel(accountEntity.email, nodeKeyDetails.publicKey!!)
      val initialLegacySubmitResult = apiRepository.postInitialLegacySubmit(getApplication(), model)

      when (initialLegacySubmitResult.status) {
        Result.Status.EXCEPTION -> {
          initialLegacySubmitResult.exception?.let { exception -> throw exception }
        }

        Result.Status.ERROR -> {
          initialLegacySubmitResult.data?.apiError?.let { apiError -> throw ApiException(apiError) }
        }

        else -> {
          // all looks well
        }
      }

      if (!accountEntity.isRuleExist(AccountEntity.DomainRule.NO_PRV_BACKUP)) {
        if (!saveCreatedPrivateKeyAsBackupToInbox(accountEntity, nodeKeyDetails)) {
          val backupAction = ActionQueueEntity.fromAction(BackupPrivateKeyToInboxAction(0,
              accountEntity.email, 0, nodeKeyDetails.longId!!))
          backupAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
        }
      }
    } else {
      if (!accountEntity.isRuleExist(AccountEntity.DomainRule.NO_PRV_BACKUP)) {
        if (!saveCreatedPrivateKeyAsBackupToInbox(accountEntity, nodeKeyDetails)) {
          val backupAction = ActionQueueEntity.fromAction(BackupPrivateKeyToInboxAction(0,
              accountEntity.email, 0, nodeKeyDetails.longId!!))
          backupAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
        }
      }

      if (!registerUserPublicKey(accountEntity, nodeKeyDetails)) {
        val registerAction = ActionQueueEntity.fromAction(RegisterUserPublicKeyAction(0,
            accountEntity.email, 0, nodeKeyDetails.publicKey!!))
        registerAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
      }
    }

    if (!requestingTestMsgWithNewPublicKey(accountEntity, nodeKeyDetails)) {
      val welcomeEmailAction = ActionQueueEntity.fromAction(SendWelcomeTestEmailAction(0,
          accountEntity.email, 0, nodeKeyDetails.publicKey!!))
      welcomeEmailAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
    }
  }

  private suspend fun getModifiedNodeKeyDetails(oldPassphrase: String?,
                                                newPassphrase: String,
                                                originalPrivateKey: String?): NodeKeyDetails =
      withContext(Dispatchers.IO) {
        val keyDetailsList = NodeCallsExecutor.parseKeys(originalPrivateKey!!)
        if (CollectionUtils.isEmpty(keyDetailsList) || keyDetailsList.size != 1) {
          throw IllegalStateException("Parse keys error")
        }

        val nodeKeyDetails = keyDetailsList[0]
        val longId = nodeKeyDetails.longId

        if (TextUtils.isEmpty(oldPassphrase)) {
          throw IllegalStateException("Passphrase for key with longid $longId not found")
        }

        val (decryptedKey) = NodeCallsExecutor.decryptKey(nodeKeyDetails.privateKey!!, oldPassphrase!!)

        if (TextUtils.isEmpty(decryptedKey)) {
          throw IllegalStateException("Can't decrypt key with longid " + longId!!)
        }

        val (encryptedKey) = NodeCallsExecutor.encryptKey(decryptedKey!!, newPassphrase)

        if (TextUtils.isEmpty(encryptedKey)) {
          throw IllegalStateException("Can't encrypt key with longid " + longId!!)
        }

        val modifiedKeyDetailsList = NodeCallsExecutor.parseKeys(encryptedKey!!)
        if (CollectionUtils.isEmpty(modifiedKeyDetailsList) || modifiedKeyDetailsList.size != 1) {
          throw IllegalStateException("Parse keys error")
        }

        modifiedKeyDetailsList[0]
      }

  /**
   * Check that the key size not bigger then [.MAX_SIZE_IN_BYTES].
   *
   * @param fileUri The [Uri] of the selected file.
   * @return true if the key size not bigger then [.MAX_SIZE_IN_BYTES], otherwise false
   */
  private fun isKeyTooBig(fileUri: Uri?): Boolean {
    return GeneralUtil.getFileSizeFromUri(getApplication(), fileUri) > MAX_SIZE_IN_BYTES
  }

  /**
   * Perform a backup of the armored key in INBOX.
   *
   * @return true if message was send.
   */
  private suspend fun saveCreatedPrivateKeyAsBackupToInbox(accountEntity: AccountEntity, keyDetails: NodeKeyDetails): Boolean =
      withContext(Dispatchers.IO) {
        try {
          val context: Context = getApplication()
          val session = OpenStoreHelper.getAccountSess(context, accountEntity)
          val transport = SmtpProtocolUtil.prepareSmtpTransport(context, session, accountEntity)
          val msg = EmailUtil.genMsgWithPrivateKeys(context, accountEntity, session,
              EmailUtil.genBodyPartWithPrivateKey(accountEntity, keyDetails.privateKey!!))
          transport.sendMessage(msg, msg.allRecipients)
        } catch (e: Exception) {
          e.printStackTrace()
          return@withContext false
        }

        return@withContext true
      }

  private suspend fun genContacts(accountEntity: AccountEntity): List<PgpContact> = withContext(Dispatchers.IO) {
    val pgpContactMain = PgpContact(accountEntity.email, accountEntity.displayName)
    val contacts = ArrayList<PgpContact>()

    when (accountEntity.accountType) {
      AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
        contacts.add(pgpContactMain)
        val gmail = GmailApiHelper.generateGmailApiService(getApplication(), accountEntity)
        val aliases = gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
        for (alias in aliases.sendAs) {
          if (alias.verificationStatus != null) {
            contacts.add(PgpContact(alias.sendAsEmail, alias.displayName))
          }
        }
      }

      else -> contacts.add(pgpContactMain)
    }

    return@withContext contacts
  }

  /**
   * Registering a key with attester API.
   * Note: this will only be successful if it's the first time submitting a key for this email address, or if the
   * key being submitted has the same fingerprint as the one already recorded. If it's an error due to key
   * conflict, ignore the error.
   *
   * @param accountEntity [AccountEntity] which will be used for registration.
   * @param keyDetails Details of the created key.
   * @return true if no errors.
   */
  private suspend fun registerUserPublicKey(accountEntity: AccountEntity, keyDetails: NodeKeyDetails): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
      val model = InitialLegacySubmitModel(accountEntity.email, keyDetails.publicKey!!)
      val initialLegacySubmitResult = apiRepository.postInitialLegacySubmit(getApplication(), model)
      when (initialLegacySubmitResult.status) {
        Result.Status.SUCCESS -> {
          val body = initialLegacySubmitResult.data
          body != null && (body.apiError == null || body.apiError.code !in 400..499)
        }

        else -> {
          false
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Request a test email from FlowCrypt.
   *
   * @param keyDetails Details of the created key.
   * @return true if no errors.
   */
  private suspend fun requestingTestMsgWithNewPublicKey(accountEntity: AccountEntity, keyDetails: NodeKeyDetails): Boolean =
      withContext(Dispatchers.IO) {
        return@withContext try {
          val model = TestWelcomeModel(accountEntity.email, keyDetails.publicKey!!)
          val testWelcomeResult = apiRepository.postTestWelcome(getApplication(), model)
          when (testWelcomeResult.status) {
            Result.Status.SUCCESS -> {
              val testWelcomeResponse = testWelcomeResult.data
              testWelcomeResponse != null && testWelcomeResponse.isSent
            }

            else -> {
              false
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
          false
        }
      }

  companion object {
    /**
     * Max size of a key is 256k.
     */
    const val MAX_SIZE_IN_BYTES = 256 * 1024
  }
}
