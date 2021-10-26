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
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper
import com.flowcrypt.email.api.email.protocol.SmtpProtocolUtil
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyDetails
import com.flowcrypt.email.extensions.org.pgpainless.util.asString
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.service.actionqueue.actions.BackupPrivateKeyToInboxAction
import com.flowcrypt.email.service.actionqueue.actions.RegisterUserPublicKeyAction
import com.flowcrypt.email.service.actionqueue.actions.SendWelcomeTestEmailAction
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.NoPrivateKeysAvailableException
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.PGPainless
import org.pgpainless.key.collection.PGPKeyRingCollection
import org.pgpainless.key.util.UserId
import org.pgpainless.util.Passphrase
import java.util.*

/**
 * This [ViewModel] implementation can be used to fetch details about imported keys.
 *
 * @author Denis Bondarenko
 * Date: 2/14/19
 * Time: 10:50 AM
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysViewModel(application: Application) : AccountViewModel(application) {
  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(getApplication())
  private val apiRepository: ApiRepository = FlowcryptApiRepository()

  val changePassphraseLiveData = MutableLiveData<Result<Boolean>>()
  val saveBackupToInboxLiveData = MutableLiveData<Result<Boolean>>()
  val saveBackupAsFileLiveData = MutableLiveData<Result<Boolean>>()
  val savePrivateKeysLiveData = MutableLiveData<Result<Boolean>>()
  val parseKeysLiveData = MutableLiveData<Result<PgpKey.ParseKeyResult?>>()
  val createPrivateKeyLiveData = MutableLiveData<Result<PgpKeyDetails?>>()
  val deleteKeysLiveData = MutableLiveData<Result<Boolean>>()
  val protectPrivateKeysLiveData = MutableLiveData<Result<List<PgpKeyDetails>>>(Result.none())

  val parseKeysResultLiveData: LiveData<Result<List<PgpKeyDetails>>> =
    keysStorage.secretKeyRingsLiveData.switchMap { list ->
      liveData {
        emit(Result.loading())
        emit(
          try {
            Result.success(list.map { it.toPgpKeyDetails() })
          } catch (e: Exception) {
            Result.exception(e)
          }
        )
      }
    }

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
            publicKey = modifiedPgpKeyDetails.publicKey.toByteArray(),
            storedPassphrase = KeyStoreCryptoManager.encryptSuspend(newPassphrase.asString)
          )
        }

        roomDatabase.keysDao().updateSuspend(updateCandidates)

        //update passphrases in RAM
        rawKeys.filter { it.passphraseType == KeyEntity.PassphraseType.RAM }.forEach { rawKey ->
          keysStorage.putPassphraseToCache(
            fingerprint = rawKey.fingerprint,
            passphrase = newPassphrase,
            validUntil = KeysStorageImpl.calculateLifeTimeForPassphrase(),
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
   * Encrypt sensitive info of [PgpKeyDetails] via AndroidKeyStore and save it in
   * the local database.
   */
  fun encryptAndSaveKeysToDatabase(
    accountEntity: AccountEntity?, keys: List<PgpKeyDetails>,
    sourceType: KeyImportDetails.SourceType, addAccountIfNotExist: Boolean = false
  ) {
    requireNotNull(accountEntity)

    viewModelScope.launch {
      val context: Context = getApplication()
      savePrivateKeysLiveData.value = Result.loading()
      try {
        for (keyDetails in keys) {
          val fingerprint = keyDetails.fingerprint
          if (!keyDetails.isFullyEncrypted) {
            throw IllegalStateException(
              context.getString(R.string.found_not_fully_encrypted_key, fingerprint)
            )
          }

          if (roomDatabase.keysDao().getKeyByAccountAndFingerprintSuspend(
              accountEntity.email.lowercase(Locale.US),
              fingerprint
            ) == null
          ) {
            if (addAccountIfNotExist) {
              val existedAccount = roomDatabase.accountDao()
                .getAccountSuspend(accountEntity.email.lowercase(Locale.US))
              if (existedAccount == null) {
                roomDatabase.accountDao().addAccountSuspend(accountEntity)
              }
            }

            val encryptedPassphrase =
              if (keyDetails.passphraseType == KeyEntity.PassphraseType.DATABASE) {
                KeyStoreCryptoManager.encryptSuspend(
                  String(requireNotNull(keyDetails.tempPassphrase))
                )
              } else null

            val encryptedPrvKey =
              KeyStoreCryptoManager.encryptSuspend(keyDetails.privateKey).toByteArray()

            val keyEntity = keyDetails.toKeyEntity(accountEntity).copy(
              source = sourceType.toPrivateKeySourceTypeString(),
              privateKey = encryptedPrvKey,
              storedPassphrase = encryptedPassphrase
            )
            val isAdded = roomDatabase.keysDao().insertSuspend(keyEntity) > 0

            if (isAdded) {
              if (keyDetails.passphraseType == KeyEntity.PassphraseType.RAM) {
                keysStorage.putPassphraseToCache(
                  fingerprint = fingerprint,
                  passphrase = Passphrase(keyDetails.tempPassphrase),
                  validUntil = KeysStorageImpl.calculateLifeTimeForPassphrase(),
                  passphraseType = KeyEntity.PassphraseType.RAM
                )
              }
              //update contacts table
              val contactsDao = roomDatabase.recipientDao()
              for (pgpContact in keyDetails.pgpContacts) {
                pgpContact.pubkey = keyDetails.publicKey
                val temp = contactsDao.getContactByEmailSuspend(pgpContact.email)
                if (temp == null && GeneralUtil.isEmailValid(pgpContact.email)) {
                  contactsDao.insertWithReplaceSuspend(pgpContact.toContactEntity())
                  //todo-DenBond7 Need to resolve a situation with different public keys. For example
                  // we can have a situation when we have to different public keys with the same email
                }
              }
            }
          }
        }
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
            parseKeyResult = PgpKey.parseKeys(source, false)
          }

          KeyImportDetails.SourceType.CLIPBOARD, KeyImportDetails.SourceType.EMAIL, KeyImportDetails.SourceType.MANUAL_ENTERING -> {
            val source = keyImportModel.keyString
              ?: throw IllegalStateException(sourceNotAvailableMsg)
            parseKeyResult = PgpKey.parseKeys(source, false)
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

  fun createPrivateKey(
    accountEntity: AccountEntity, passphrase: String,
    passphraseType: KeyEntity.PassphraseType
  ) {
    viewModelScope.launch {
      createPrivateKeyLiveData.value = Result.loading()
      var pgpKeyDetails: PgpKeyDetails? = null
      try {
        pgpKeyDetails = PGPainless.generateKeyRing().simpleEcKeyRing(
          UserId.nameAndEmail(
            accountEntity.displayName
              ?: accountEntity.email, accountEntity.email
          ), passphrase
        ).toPgpKeyDetails().copy(passphraseType = passphraseType)

        val existedAccount =
          roomDatabase.accountDao().getAccountSuspend(accountEntity.email.lowercase(Locale.US))
        if (existedAccount == null) {
          roomDatabase.accountDao().addAccountSuspend(accountEntity)
        }

        savePrivateKeyToDatabase(accountEntity, pgpKeyDetails, passphrase)
        doAdditionalOperationsAfterKeyCreation(accountEntity, pgpKeyDetails)
        createPrivateKeyLiveData.value = Result.success(pgpKeyDetails)
      } catch (e: Exception) {
        e.printStackTrace()
        pgpKeyDetails?.fingerprint?.let {
          roomDatabase.keysDao().deleteByAccountAndFingerprintSuspend(accountEntity.email, it)
        }
        createPrivateKeyLiveData.value = Result.exception(e)
        ExceptionUtil.handleError(e)
      }
    }
  }

  fun deleteKeys(accountEntity: AccountEntity, keys: List<PgpKeyDetails>) {
    viewModelScope.launch {
      deleteKeysLiveData.value = Result.loading()
      try {
        val context: Context = getApplication()
        val allKeyEntitiesOfAccount =
          roomDatabase.keysDao().getAllKeysByAccountSuspend(accountEntity.email)
        val fingerprintListOfDeleteCandidates = keys.map {
          it.fingerprint.lowercase(Locale.US)
        }
        val deleteCandidates = allKeyEntitiesOfAccount.filter {
          fingerprintListOfDeleteCandidates.contains(it.fingerprint.lowercase(Locale.US))
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

  fun protectPrivateKeys(privateKeys: List<PgpKeyDetails>, passphrase: Passphrase) {
    viewModelScope.launch {
      protectPrivateKeysLiveData.value = Result.loading()
      try {
        val encryptedKeysSource = privateKeys.map { pgpKeyDetails ->
          PgpKey.encryptKeySuspend(requireNotNull(pgpKeyDetails.privateKey), passphrase)
        }.joinToString(separator = "\n")

        protectPrivateKeysLiveData.value =
          Result.success(PgpKey.parsePrivateKeys(encryptedKeysSource)
            .map { it.copy(tempPassphrase = passphrase.chars) })
      } catch (e: Exception) {
        e.printStackTrace()
        protectPrivateKeysLiveData.value = Result.exception(e)
      }
    }
  }

  private suspend fun savePrivateKeyToDatabase(
    accountEntity: AccountEntity,
    pgpKeyDetails: PgpKeyDetails,
    passphrase: String
  ) {
    val keyEntity = pgpKeyDetails.toKeyEntity(accountEntity).copy(
      source = KeyImportDetails.SourceType.NEW.toPrivateKeySourceTypeString(),
      privateKey = KeyStoreCryptoManager.encryptSuspend(pgpKeyDetails.privateKey).toByteArray(),
      storedPassphrase = KeyStoreCryptoManager.encryptSuspend(passphrase)
    )

    if (roomDatabase.keysDao().insertSuspend(keyEntity) == -1L) {
      throw NullPointerException("Cannot save a generated private key")
    }
  }

  private suspend fun doAdditionalOperationsAfterKeyCreation(
    accountEntity: AccountEntity,
    pgpKeyDetails: PgpKeyDetails
  ) {
    if (accountEntity.isRuleExist(OrgRules.DomainRule.ENFORCE_ATTESTER_SUBMIT)) {
      val model = InitialLegacySubmitModel(accountEntity.email, pgpKeyDetails.publicKey)
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

      if (!accountEntity.isRuleExist(OrgRules.DomainRule.NO_PRV_BACKUP)) {
        if (!saveCreatedPrivateKeyAsBackupToInbox(accountEntity, pgpKeyDetails)) {
          val backupAction = ActionQueueEntity.fromAction(
            BackupPrivateKeyToInboxAction(
              0,
              accountEntity.email, 0, pgpKeyDetails.fingerprint
            )
          )
          backupAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
        }
      }
    } else {
      if (!accountEntity.isRuleExist(OrgRules.DomainRule.NO_PRV_BACKUP)) {
        if (!saveCreatedPrivateKeyAsBackupToInbox(accountEntity, pgpKeyDetails)) {
          val backupAction = ActionQueueEntity.fromAction(
            BackupPrivateKeyToInboxAction(
              0,
              accountEntity.email, 0, pgpKeyDetails.fingerprint
            )
          )
          backupAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
        }
      }

      if (!registerUserPublicKey(accountEntity, pgpKeyDetails)) {
        val registerAction = ActionQueueEntity.fromAction(
          RegisterUserPublicKeyAction(
            0,
            accountEntity.email, 0, pgpKeyDetails.publicKey
          )
        )
        registerAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
      }
    }

    if (!requestingTestMsgWithNewPublicKey(accountEntity, pgpKeyDetails)) {
      val welcomeEmailAction = ActionQueueEntity.fromAction(
        SendWelcomeTestEmailAction(
          0,
          accountEntity.email, 0, pgpKeyDetails.publicKey
        )
      )
      welcomeEmailAction?.let { action -> roomDatabase.actionQueueDao().insertSuspend(action) }
    }
  }

  private suspend fun getModifiedPgpKeyDetails(
    oldPassphrase: Passphrase,
    newPassphrase: Passphrase,
    originalPrivateKey: String,
    fingerprint: String
  ): PgpKeyDetails = withContext(Dispatchers.IO) {
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
      PgpKey.parseKeys(keyEncryptedWithNewPassphrase.toByteArray()).pgpKeyDetailsList
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
    keyDetails: PgpKeyDetails
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

  private suspend fun genContacts(accountEntity: AccountEntity): List<PgpContact> =
    withContext(Dispatchers.IO) {
      val pgpContactMain = PgpContact(accountEntity.email, accountEntity.displayName)
      val contacts = ArrayList<PgpContact>()

      when (accountEntity.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          contacts.add(pgpContactMain)
          val gmail = GmailApiHelper.generateGmailApiService(getApplication(), accountEntity)
          val aliases =
            gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
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
  private suspend fun registerUserPublicKey(
    accountEntity: AccountEntity,
    keyDetails: PgpKeyDetails
  ): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
      val model = InitialLegacySubmitModel(accountEntity.email, keyDetails.publicKey)
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
  private suspend fun requestingTestMsgWithNewPublicKey(
    accountEntity: AccountEntity,
    keyDetails: PgpKeyDetails
  ): Boolean =
    withContext(Dispatchers.IO) {
      return@withContext try {
        val model = TestWelcomeModel(accountEntity.email, keyDetails.publicKey)
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

  private suspend fun saveBackupsToInboxInternal() = withContext(Dispatchers.IO) {
    val account = requireNotNull(
      getAccountEntityWithDecryptedInfo(
        roomDatabase.accountDao().getActiveAccountSuspend()
      )
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
