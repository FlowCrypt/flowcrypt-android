/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.request.model.WelcomeMessageModel
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.extensions.org.pgpainless.util.asString
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.service.actionqueue.actions.BackupPrivateKeyToInboxAction
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.key.collection.PGPKeyRingCollection
import org.pgpainless.key.info.KeyRingInfo
import org.pgpainless.util.Passphrase

/**
 * This [ViewModel] implementation can be used to fetch details about imported keys.
 *
 * @author Denys Bondarenko
 */
class PrivateKeysViewModel(application: Application) : AccountViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(getApplication())
  private val controlledRunnerForAdditionalOperationsForPrivateKeys =
    ControlledRunner<Result<Pair<AccountEntity, List<PgpKeyRingDetails>>>>()

  val changePassphraseLiveData = MutableLiveData<Result<Boolean>>()
  val saveBackupToInboxLiveData = MutableLiveData<Result<Boolean>>()
  val saveBackupAsFileLiveData = MutableLiveData<Result<Boolean>>()
  val savePrivateKeysLiveData =
    MutableLiveData<Result<Pair<AccountEntity, List<PgpKeyRingDetails>>>?>()
  val parseKeysLiveData = MutableLiveData<Result<PgpKey.ParseKeyResult?>>()
  val additionalActionsAfterPrivateKeyCreationLiveData =
    MutableLiveData<Result<Pair<AccountEntity, List<PgpKeyRingDetails>>>?>()
  val additionalActionsAfterPrivateKeysImportingLiveData =
    MutableLiveData<Result<Pair<AccountEntity, List<PgpKeyRingDetails>>>?>()
  val deleteKeysLiveData = MutableLiveData<Result<Boolean>>()
  val protectPrivateKeysLiveData = MutableLiveData<Result<List<PgpKeyRingDetails>>>(Result.none())

  val parseKeysResultLiveData: LiveData<Result<List<PgpKeyRingDetails>>> =
    keysStorage.secretKeyRingsLiveData.switchMap { list ->
      liveData {
        emit(Result.loading())
        val account = getActiveAccountSuspend()
        emit(
          try {
            Result.success(list.map {
              it.toPgpKeyRingDetails(
                account?.clientConfiguration?.shouldHideArmorMeta() ?: false
              )
            })
          } catch (e: Exception) {
            Result.exception(e)
          }
        )
      }
    }

  fun getActiveAccount(): AccountEntity? = keysStorage.getActiveAccount()

  @ExperimentalCoroutinesApi
  val secretKeyRingsInfoStateFlow: StateFlow<Result<List<KeyRingInfo>?>> =
    keysStorage.secretKeyRingsLiveData.asFlow().flatMapLatest {
      flow {
        emit(Result.loading())
        try {
          emit(Result.success(it.map { pgpSecretKeyRing -> KeyRingInfo(pgpSecretKeyRing) }))
        } catch (e: Exception) {
          emit(Result.exception(e))
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = Result.none()
    )

  fun changePassphrase(newPassphrase: Passphrase) {
    viewModelScope.launch {
      changePassphraseLiveData.value = Result.loading()
      try {
        if (newPassphrase.isEmpty) {
          throw IllegalStateException("new Passphrase can't be empty")
        }

        val account = requireNotNull(roomDatabase.accountDao().getActiveAccountSuspend())
        val rawKeys = keysStorage.getRawKeys()

        if (rawKeys.isEmpty()) {
          throw NoPrivateKeysAvailableException(getApplication(), account.email)
        }

        val updateCandidates = rawKeys.map { keyEntity ->
          val fingerprint = keyEntity.fingerprint
          val oldPassphrase = keysStorage.getPassphraseByFingerprint(fingerprint)
            ?: throw IllegalStateException(
              "Passphrase for key with fingerprint $fingerprint not defined"
            )

          val modifiedPgpKeyDetails = getModifiedPgpKeyDetails(
            oldPassphrase = oldPassphrase,
            newPassphrase = newPassphrase,
            originalPrivateKey = keyEntity.privateKeyAsString,
            fingerprint = fingerprint
          )

          if (modifiedPgpKeyDetails.isFullyDecrypted) {
            throw IllegalArgumentException("Error. The key is decrypted!")
          }

          keyEntity.copy(
            privateKey = KeyStoreCryptoManager.encryptSuspend(modifiedPgpKeyDetails.privateKey)
              .toByteArray(),
            storedPassphrase = KeyStoreCryptoManager.encryptSuspend(newPassphrase.asString)
          )
        }

        roomDatabase.keysDao().updateSuspend(updateCandidates)

        //update passphrases in RAM
        rawKeys.filter { it.passphraseType == KeyEntity.PassphraseType.RAM }.forEach { rawKey ->
          keysStorage.putPassphraseToCache(
            fingerprint = rawKey.fingerprint,
            passphrase = newPassphrase,
            validUntil = keysStorage.calculateLifeTimeForPassphrase(),
            passphraseType = KeyEntity.PassphraseType.RAM
          )
        }

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
      try {
        saveBackupsToInboxInternal()
        saveBackupToInboxLiveData.value = Result.success(true)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        saveBackupToInboxLiveData.value = Result.exception(e)
      }
    }
  }

  fun saveBackupsAsFile(destinationUri: Uri) {
    viewModelScope.launch {
      saveBackupAsFileLiveData.value = Result.loading()
      try {
        val result = saveBackupsAsFileInternal(destinationUri)
        saveBackupAsFileLiveData.value = Result.success(result)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        saveBackupAsFileLiveData.value = Result.exception(e)
      }
    }
  }

  /**
   * Encrypt sensitive info of [PgpKeyRingDetails] via AndroidKeyStore and save it in
   * the local database.
   */
  fun encryptAndSaveKeysToDatabase(
    accountEntity: AccountEntity?,
    keys: List<PgpKeyRingDetails>
  ) {
    if (accountEntity == null) {
      savePrivateKeysLiveData.value = Result.exception(NullPointerException("account == null"))
      return
    }

    viewModelScope.launch {
      val context: Context = getApplication()
      savePrivateKeysLiveData.value = Result.loading()
      try {
        for (pgpKeyRingDetails in keys) {
          val fingerprint = pgpKeyRingDetails.fingerprint
          if (!pgpKeyRingDetails.isFullyEncrypted) {
            throw IllegalStateException(
              context.getString(R.string.found_not_fully_encrypted_key, fingerprint)
            )
          }

          val existingKeyEntity = roomDatabase.keysDao().getKeyByAccountAndFingerprintSuspend(
            accountEntity.email.lowercase(), fingerprint
          )

          if (existingKeyEntity != null) {
            val decryptedPrivateKey =
              KeyStoreCryptoManager.decryptSuspend(String(existingKeyEntity.privateKey))
            val existingPgpKeyDetails =
              PgpKey.parseKeys(decryptedPrivateKey).pgpKeyDetailsList.firstOrNull()

            if (existingPgpKeyDetails?.isNewerThan(pgpKeyRingDetails) == true) {
              continue
            }
          }

          val encryptedPassphrase =
            if (pgpKeyRingDetails.passphraseType == KeyEntity.PassphraseType.DATABASE) {
              KeyStoreCryptoManager.encryptSuspend(
                String(requireNotNull(pgpKeyRingDetails.tempPassphrase))
              )
            } else null

          val encryptedPrvKey =
            KeyStoreCryptoManager.encryptSuspend(pgpKeyRingDetails.privateKey).toByteArray()

          val keyEntity = (existingKeyEntity ?: pgpKeyRingDetails.toKeyEntity(accountEntity)).copy(
            source = requireNotNull(pgpKeyRingDetails.importSourceType?.toPrivateKeySourceTypeString()),
            privateKey = encryptedPrvKey,
            storedPassphrase = encryptedPassphrase
          )
          val isAddedOrUpdated = if (existingKeyEntity != null) {
            roomDatabase.keysDao().updateSuspend(keyEntity) > 0
          } else {
            roomDatabase.keysDao().insertSuspend(keyEntity) > 0
          }

          if (isAddedOrUpdated) {
            if (pgpKeyRingDetails.passphraseType == KeyEntity.PassphraseType.RAM) {
              keysStorage.putPassphraseToCache(
                fingerprint = fingerprint,
                passphrase = Passphrase(pgpKeyRingDetails.tempPassphrase),
                validUntil = keysStorage.calculateLifeTimeForPassphrase(),
                passphraseType = KeyEntity.PassphraseType.RAM
              )
            }

            //update pub keys
            val recipientDao = roomDatabase.recipientDao()
            val pubKeysDao = roomDatabase.pubKeyDao()
            for (mimeAddress in pgpKeyRingDetails.mimeAddresses) {
              val address = mimeAddress.address.lowercase()
              val name = mimeAddress.personal

              val existingRecipientWithPubKeys =
                recipientDao.getRecipientWithPubKeysByEmailSuspend(address)
              if (existingRecipientWithPubKeys == null) {
                recipientDao.insertSuspend(RecipientEntity(email = address, name = name))
              }

              val existingPubKeyEntity =
                pubKeysDao.getPublicKeyByRecipientAndFingerprint(
                  address,
                  pgpKeyRingDetails.fingerprint
                )
              if (existingPubKeyEntity == null) {
                pubKeysDao.insertSuspend(pgpKeyRingDetails.toPublicKeyEntity(address))
              } else if (existingKeyEntity != null) {
                pubKeysDao.updateSuspend(
                  existingPubKeyEntity.copy(
                    publicKey = pgpKeyRingDetails.publicKey.toByteArray()
                  )
                )
              }
            }
          }
        }
        savePrivateKeysLiveData.value = Result.success(Pair(accountEntity, keys))
      } catch (e: Exception) {
        e.printStackTrace()
        savePrivateKeysLiveData.value = Result.exception(SavePrivateKeyToDatabaseException(keys, e))
      }
    }
  }

  /**
   * Parse keys from the given resource (string or file).
   */
  fun parseKeys(
    keyImportModel: KeyImportModel?, isCheckSizeEnabled: Boolean,
    filterOnlyPrivate: Boolean = false
  ) {
    viewModelScope.launch {
      val context: Context = getApplication()
      try {
        parseKeysLiveData.value = Result.loading()

        if (keyImportModel == null) {
          parseKeysLiveData.value = Result.exception(IllegalArgumentException("Unknown format"))
          return@launch
        }

        var parseKeyResult: PgpKey.ParseKeyResult
        val sourceNotAvailableMsg = context.getString(R.string.source_is_empty_or_not_available)
        when (keyImportModel.sourceType) {
          KeyImportDetails.SourceType.FILE -> {
            if (isCheckSizeEnabled && isKeyTooBig(keyImportModel.fileUri)) {
              throw IllegalArgumentException(context.getString(R.string.file_is_too_big))
            }

            if (keyImportModel.fileUri == null) {
              throw NullPointerException("Uri is null!")
            }

            val source = context.contentResolver.openInputStream(keyImportModel.fileUri)
              ?: throw java.lang.IllegalStateException(sourceNotAvailableMsg)
            parseKeyResult = PgpKey.parseKeys(
              source = source,
              throwExceptionIfUnknownSource = false
            )
          }

          KeyImportDetails.SourceType.CLIPBOARD, KeyImportDetails.SourceType.EMAIL, KeyImportDetails.SourceType.MANUAL_ENTERING -> {
            val source = keyImportModel.keyString
              ?: throw IllegalStateException(sourceNotAvailableMsg)
            parseKeyResult = PgpKey.parseKeys(
              source = source,
              throwExceptionIfUnknownSource = false
            )
          }

          else -> throw IllegalStateException("Unsupported : ${keyImportModel.sourceType}")
        }

        if (filterOnlyPrivate) {
          parseKeyResult = PgpKey.ParseKeyResult(
            PGPKeyRingCollection(
              parseKeyResult.pgpKeyRingCollection
                .pgpSecretKeyRingCollection.keyRings.asSequence().toList(), true
            )
          )
        }

        parseKeysLiveData.value = Result.success(parseKeyResult)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        parseKeysLiveData.value = Result.exception(e)
      }
    }
  }

  fun doAdditionalActionsAfterPrivateKeyCreation(
    accountEntity: AccountEntity,
    keys: List<PgpKeyRingDetails>,
    idToken: String? = null,
  ) {
    processPrivateKeysInternally(
      accountEntity = accountEntity,
      keys = keys,
      idToken = idToken,
      mutableLiveData = additionalActionsAfterPrivateKeyCreationLiveData
    )
  }

  fun doAdditionalActionsAfterPrivateKeysImporting(
    accountEntity: AccountEntity,
    keys: List<PgpKeyRingDetails>,
    idToken: String? = null,
  ) {
    processPrivateKeysInternally(
      accountEntity = accountEntity,
      keys = keys,
      idToken = idToken,
      mutableLiveData = additionalActionsAfterPrivateKeysImportingLiveData
    )
  }

  private fun processPrivateKeysInternally(
    accountEntity: AccountEntity,
    keys: List<PgpKeyRingDetails>,
    idToken: String?,
    mutableLiveData: MutableLiveData<Result<Pair<AccountEntity, List<PgpKeyRingDetails>>>?>
  ) {
    viewModelScope.launch {
      mutableLiveData.value = Result.loading()
      try {
        val pgpKeyRingDetails =
          keys.firstOrNull() ?: throw java.lang.IllegalStateException("No keys")
        val result = controlledRunnerForAdditionalOperationsForPrivateKeys.joinPreviousOrRun {
          return@joinPreviousOrRun try {
            doAdditionalOperationsForPrivateKey(
              accountEntity = accountEntity,
              pgpKeyRingDetails = pgpKeyRingDetails,
              idToken = idToken,
            )
            Result.success(Pair(accountEntity, keys))
          } catch (e: Exception) {
            //we can skip the exception here as we will handle it below
            Result.exception(e)
          }
        }

        if (result.exception != null) {
          throw result.exception
        } else {
          mutableLiveData.value = result
        }
      } catch (e: Exception) {
        e.printStackTrace()
        for (pgpKeyRingDetails in keys) {
          pgpKeyRingDetails.fingerprint.let {
            roomDatabase.keysDao().deleteByAccountAndFingerprintSuspend(accountEntity.email, it)
          }
        }
        mutableLiveData.value = Result.exception(e)
        ExceptionUtil.handleError(e)
      }
    }
  }

  fun deleteKeys(accountEntity: AccountEntity, keys: List<PgpKeyRingDetails>) {
    viewModelScope.launch {
      deleteKeysLiveData.value = Result.loading()
      try {
        val context: Context = getApplication()
        val allKeyEntitiesOfAccount =
          roomDatabase.keysDao().getAllKeysByAccountSuspend(accountEntity.email)
        val fingerprintListOfDeleteCandidates = keys.map {
          it.fingerprint.lowercase()
        }
        val deleteCandidates = allKeyEntitiesOfAccount.filter {
          fingerprintListOfDeleteCandidates.contains(it.fingerprint.lowercase())
        }

        if (keys.size == allKeyEntitiesOfAccount.size) {
          throw IllegalArgumentException(context.getString(R.string.please_leave_at_least_one_key))
        }

        roomDatabase.keysDao().deleteSuspend(deleteCandidates)

        deleteKeysLiveData.value = Result.success(true)
      } catch (e: Exception) {
        e.printStackTrace()
        deleteKeysLiveData.value = Result.exception(e)
        ExceptionUtil.handleError(e)
      }
    }
  }

  fun protectPrivateKeys(privateKeys: List<PgpKeyRingDetails>, passphrase: Passphrase) {
    viewModelScope.launch {
      protectPrivateKeysLiveData.value = Result.loading()
      val sourceTypeInfo = privateKeys.associateBy({ it.fingerprint }, { it.importSourceType })
      try {
        val encryptedKeysSource = privateKeys.map { pgpKeyRingDetails ->
          PgpKey.encryptKeySuspend(requireNotNull(pgpKeyRingDetails.privateKey), passphrase)
        }.joinToString(separator = "\n")

        protectPrivateKeysLiveData.value =
          Result.success(PgpKey.parsePrivateKeys(encryptedKeysSource).map { key ->
            key.copy(
              tempPassphrase = passphrase.chars,
              importSourceType = sourceTypeInfo[key.fingerprint]
            )
          })
      } catch (e: Exception) {
        e.printStackTrace()
        protectPrivateKeysLiveData.value = Result.exception(e)
      }
    }
  }

  private suspend fun doAdditionalOperationsForPrivateKey(
    accountEntity: AccountEntity,
    pgpKeyRingDetails: PgpKeyRingDetails,
    idToken: String? = null,
  ) {
    if (accountEntity.hasClientConfigurationProperty(ClientConfiguration.ConfigurationProperty.ENFORCE_ATTESTER_SUBMIT)) {
      registerUserPublicKey(accountEntity, pgpKeyRingDetails, idToken, false)

      if (!accountEntity.hasClientConfigurationProperty(ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP)) {
        if (!saveCreatedPrivateKeyAsBackupToInbox(accountEntity, pgpKeyRingDetails)) {
          val backupAction = ActionQueueEntity.fromAction(
            BackupPrivateKeyToInboxAction(
              0,
              accountEntity.email, 0, pgpKeyRingDetails.fingerprint
            )
          )
          backupAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
        }
      }
    } else {
      if (!accountEntity.hasClientConfigurationProperty(ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP)) {
        if (!saveCreatedPrivateKeyAsBackupToInbox(accountEntity, pgpKeyRingDetails)) {
          val backupAction = ActionQueueEntity.fromAction(
            BackupPrivateKeyToInboxAction(
              0,
              accountEntity.email, 0, pgpKeyRingDetails.fingerprint
            )
          )
          backupAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
        }
      }

      registerUserPublicKey(accountEntity, pgpKeyRingDetails, idToken)
    }

    idToken?.let { postWelcomeMessage(accountEntity, pgpKeyRingDetails, it) }
  }

  private suspend fun getModifiedPgpKeyDetails(
    oldPassphrase: Passphrase,
    newPassphrase: Passphrase,
    originalPrivateKey: String,
    fingerprint: String
  ): PgpKeyRingDetails = withContext(Dispatchers.IO) {
    val keyEncryptedWithNewPassphrase = try {
      PgpKey.changeKeyPassphrase(
        armored = originalPrivateKey,
        oldPassphrase = oldPassphrase,
        newPassphrase = newPassphrase
      )
    } catch (e: Exception) {
      throw IllegalStateException(
        "Can't change passphrase for the key with fingerprint $fingerprint", e
      )
    }

    val modifiedPgpKeyDetailsList =
      PgpKey.parseKeys(source = keyEncryptedWithNewPassphrase.toByteArray()).pgpKeyDetailsList
    if (modifiedPgpKeyDetailsList.isEmpty() || modifiedPgpKeyDetailsList.size != 1) {
      throw IllegalStateException("Parse keys error")
    }

    return@withContext modifiedPgpKeyDetailsList.first()
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
  private suspend fun saveCreatedPrivateKeyAsBackupToInbox(
    accountEntity: AccountEntity,
    keyDetails: PgpKeyRingDetails
  ): Boolean =
    withContext(Dispatchers.IO) {
      try {
        val context: Context = getApplication()
        val session = OpenStoreHelper.getAccountSess(context, accountEntity)
        val transport = SmtpProtocolUtil.prepareSmtpTransport(context, session, accountEntity)
        val msg = EmailUtil.genMsgWithPrivateKeys(
          context, accountEntity, session,
          EmailUtil.genBodyPartWithPrivateKey(accountEntity, keyDetails.privateKey!!)
        )
        transport.sendMessage(msg, msg.allRecipients)
        return@withContext true
      } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
      }
    }

  /**
   * Set or replace public key. If idToken != null an auth mechanism will be used to upload
   * the given pub key. Otherwise will be used a request to replace public key that will
   * be verified by clicking email.
   *
   * More details can be found here
   * https://github.com/FlowCrypt/flowcrypt-browser/pull/4704#issuecomment-1253847852
   *
   * @param accountEntity [AccountEntity] which will be used for registration.
   * @param keyDetails Details of the created key.
   * @param idToken JSON Web Token signed by Google that can be used to identify a user to a backend.
   * @param isSilent If true - skip errors or exceptions.
   */
  private suspend fun registerUserPublicKey(
    accountEntity: AccountEntity,
    keyDetails: PgpKeyRingDetails,
    idToken: String? = null,
    isSilent: Boolean = true,
  ): Boolean = withContext(Dispatchers.IO) {
    val submitPubKeyResult = if (idToken != null) {
      ApiClientRepository.Attester.submitPrimaryEmailPubKey(
        context = getApplication(),
        idToken = idToken,
        email = accountEntity.email,
        pubKey = keyDetails.publicKey,
        clientConfiguration = accountEntity.clientConfiguration,
      )
    } else {
      ApiClientRepository.Attester.submitPubKeyWithConditionalEmailVerification(
        context = getApplication(),
        email = accountEntity.email,
        pubKey = keyDetails.publicKey,
        clientConfiguration = accountEntity.clientConfiguration,
      )
    }

    when (submitPubKeyResult.status) {
      Result.Status.SUCCESS -> {
        when {
          isSilent -> {
            submitPubKeyResult.apiError?.code?.let { it !in 400..499 } ?: true
          }

          submitPubKeyResult.apiError != null -> {
            throw IllegalStateException(ApiException(submitPubKeyResult.apiError))
          }

          else -> submitPubKeyResult.data?.isSent == true
        }
      }

      else -> if (isSilent) {
        false
      } else when (submitPubKeyResult.status) {
        Result.Status.EXCEPTION -> {
          submitPubKeyResult.exception?.let { exception -> throw exception }
            ?: throw IllegalStateException("Unknown exception")
        }

        Result.Status.ERROR -> {
          submitPubKeyResult.apiError?.let { apiError -> throw ApiException(apiError) }
            ?: throw IllegalStateException("Unknown API error")
        }

        else -> {
          throw IllegalStateException("Undefined case")
        }
      }
    }
  }

  /**
   * Send a welcome message.
   *
   * @param keyDetails Details of the created key.
   * @return true if no errors.
   */
  private suspend fun postWelcomeMessage(
    accountEntity: AccountEntity,
    keyDetails: PgpKeyRingDetails,
    idToken: String,
  ): Boolean =
    withContext(Dispatchers.IO) {
      return@withContext try {
        val model = WelcomeMessageModel(accountEntity.email, keyDetails.publicKey)
        val result = ApiClientRepository.Attester.postWelcomeMessage(
          context = getApplication(),
          idToken = idToken,
          model = model
        )

        result.status == Result.Status.SUCCESS
      } catch (e: Exception) {
        e.printStackTrace()
        false
      }
    }

  private suspend fun saveBackupsToInboxInternal() = withContext(Dispatchers.IO) {
    val account = requireNotNull(
      roomDatabase.accountDao().getActiveAccountSuspend()?.withDecryptedInfo()
    )

    val sess = OpenStoreHelper.getAccountSess(getApplication(), account)
    val transport = SmtpProtocolUtil.prepareSmtpTransport(getApplication(), sess, account)
    val msg = EmailUtil.genMsgWithAllPrivateKeys(getApplication(), account, sess)
    transport.sendMessage(msg, msg.allRecipients)
  }

  private suspend fun saveBackupsAsFileInternal(destinationUri: Uri) =
    withContext(Dispatchers.IO) {
      val account = roomDatabase.accountDao().getActiveAccountSuspend()
      requireNotNull(account)

      val backup = SecurityUtils.genPrivateKeysBackup(getApplication(), account)
      return@withContext GeneralUtil.writeFileFromStringToUri(
        getApplication(),
        destinationUri,
        backup
      ) > 0
    }

  companion object {
    /**
     * Max size of a key is 256k.
     */
    const val MAX_SIZE_IN_BYTES = 256 * 1024
  }
}
