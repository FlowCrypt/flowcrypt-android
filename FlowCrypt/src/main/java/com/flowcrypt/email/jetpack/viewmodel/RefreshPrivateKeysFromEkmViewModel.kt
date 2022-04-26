/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.EmptyPassphraseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.util.Passphrase

/**
 * @author Denis Bondarenko
 *         Date: 4/25/22
 *         Time: 4:34 PM
 *         E-mail: DenBond7@gmail.com
 */
class RefreshPrivateKeysFromEkmViewModel(application: Application) : EkmViewModel(application) {
  private val controlledRunnerRefreshPrivateKeysFromEkm = ControlledRunner<Result<Boolean?>>()
  private val refreshPrivateKeysFromEkmMutableStateFlow =
    MutableStateFlow<Result<Boolean?>>(Result.none())
  val refreshPrivateKeysFromEkmStateFlow =
    refreshPrivateKeysFromEkmMutableStateFlow.asStateFlow()

  fun refreshPrivateKeys() {
    viewModelScope.launch {
      refreshPrivateKeysFromEkmMutableStateFlow.value = Result.loading()
      val activeAccount = getActiveAccountSuspend() ?: return@launch
      if (activeAccount.clientConfiguration?.usesKeyManager() == true) {
        refreshPrivateKeysFromEkmMutableStateFlow.value =
          controlledRunnerRefreshPrivateKeysFromEkm.cancelPreviousThenRun {
            return@cancelPreviousThenRun try {
              refreshPrivateKeysInternal(activeAccount)
            } catch (e: Exception) {
              e.printStackTrace()
              Result.exception(e)
            }
          }
      } else {
        refreshPrivateKeysFromEkmMutableStateFlow.value = Result.success(true)
      }
    }
  }

  private suspend fun refreshPrivateKeysInternal(activeAccount: AccountEntity): Result<Boolean?> =
    withContext(Dispatchers.IO) {
      val context: Context = getApplication()
      val retryAttempts = 6
      val idToken = GeneralUtil.getGoogleIdToken(
        context = context,
        maxRetryAttemptCount = retryAttempts
      )

      val ekmPrivateResult = repository.getPrivateKeysViaEkm(
        context = context,
        ekmUrl = activeAccount.clientConfiguration?.keyManagerUrl
          ?: throw IllegalArgumentException("key_manager_url is empty"),
        idToken = idToken
      )

      if (ekmPrivateResult.status != Result.Status.SUCCESS) {
        return@withContext when (ekmPrivateResult.status) {
          Result.Status.EXCEPTION -> {
            Result.exception(
              //Thrown network errors can be ignored but not other types of errors
              GeneralUtil.preProcessException(
                context = context,
                causedException = requireNotNull(ekmPrivateResult.exception)
              )
            )
          }

          Result.Status.ERROR -> Result.exception(ApiException(ekmPrivateResult.data?.apiError))

          else -> Result.exception(NullPointerException())
        }
      }

      if (ekmPrivateResult.data?.privateKeys?.isEmpty() == true) {
        return@withContext Result.success(true)
      }

      val pgpKeyDetailsList = mutableListOf<PgpKeyDetails>()
      ekmPrivateResult.data?.privateKeys?.forEach { key ->
        val parsedList = PgpKey.parsePrivateKeys(requireNotNull(key.decryptedPrivateKey))
          .map {
            it.copy(
              passphraseType = KeyEntity.PassphraseType.RAM,
              importSourceType = KeyImportDetails.SourceType.EKM,
              tempPassphrase = null
            )
          }

        if (parsedList.isEmpty()) {
          //ask Tom should we do any things here
          throw IllegalStateException(context.getString(R.string.could_not_parse_one_of_ekm_key))
        } else {
          //ask Tom should we do any things here

          //check that all keys were fully decrypted when we fetched them.
          // If any is encrypted at all, that's an unexpected error, we should throw an exception.
          parsedList.forEach {
            if (!it.isFullyDecrypted) {
              throw IllegalStateException(
                context.getString(
                  R.string.found_not_fully_decrypted_key_ask_admin,
                  it.fingerprint
                )
              )
            }
          }
          pgpKeyDetailsList.addAll(parsedList)
        }
      }

      handleParsedKeys(activeAccount, pgpKeyDetailsList)

      Result.success(true)
    }

  private suspend fun handleParsedKeys(
    activeAccount: AccountEntity,
    fetchedPgpKeyDetailsList: MutableList<PgpKeyDetails>
  ) = withContext(Dispatchers.IO) {
    val context: Context = getApplication()
    val keysStorage = KeysStorageImpl.getInstance(context)
    val existingKeyEntities = keysStorage.getRawKeys()
    val existingPgpKeyDetailsList = keysStorage.getPgpKeyDetailsList()
    var passphrase: Passphrase? = null

    for (fetchedPgpKeyDetails in fetchedPgpKeyDetailsList) {
      if (!fetchedPgpKeyDetails.usableForEncryption) {
        //ask Tom
        //we skip a key that is not usable for encryption
        continue
      }

      val existingPgpKeyDetails =
        existingPgpKeyDetailsList.firstOrNull { it.fingerprint == fetchedPgpKeyDetails.fingerprint }

      if (existingPgpKeyDetails != null) {
        if (fetchedPgpKeyDetails.isNewerThan(existingPgpKeyDetails)) {
          if (passphrase == null) {
            passphrase = getUsablePassphraseFromCache()
          }

          val existingKeyEntity = existingKeyEntities.first {
            it.fingerprint == existingPgpKeyDetails.fingerprint
          }

          val safeVersionOfPrvKey = protectAndEncryptInternally(passphrase, fetchedPgpKeyDetails)
          roomDatabase.keysDao().updateSuspend(
            existingKeyEntity.copy(
              privateKey = safeVersionOfPrvKey,
              publicKey = fetchedPgpKeyDetails.publicKey.toByteArray()
            )
          )
        }
      } else {
        if (passphrase == null) {
          passphrase = getUsablePassphraseFromCache()
        }

        val safeVersionOfPrvKey = protectAndEncryptInternally(passphrase, fetchedPgpKeyDetails)
        val keyEntity = fetchedPgpKeyDetails.toKeyEntity(activeAccount).copy(
          privateKey = safeVersionOfPrvKey,
          storedPassphrase = null
        )
        roomDatabase.keysDao().insertSuspend(keyEntity)
      }
    }
  }

  /**
   * We receive a private key from EKM in decrypted format. Before saving it to the local database
   * we have to protect it with a provided [Passphrase] and encrypt it with [AndroidKeyStore]
   */
  private suspend fun protectAndEncryptInternally(
    passphrase: Passphrase,
    pgpKeyDetails: PgpKeyDetails
  ): ByteArray = withContext(Dispatchers.IO) {
    val protectedPrvKey = PgpKey.encryptKeySuspend(
      armored = requireNotNull(pgpKeyDetails.privateKey),
      passphrase = passphrase
    )
    val encryptedPrvKeyInternally = KeyStoreCryptoManager.encryptSuspend(protectedPrvKey)
    return@withContext encryptedPrvKeyInternally.toByteArray()
  }

  /**
   * Get the first non-null pass phrase for any of the existing keys already in storage
   * or throw [EmptyPassphraseException] if it is not exist.
   */
  private fun getUsablePassphraseFromCache(): Passphrase {
    val context: Context = getApplication()
    val keysStorage = KeysStorageImpl.getInstance(context)
    val fingerprintOfKeyWithNotEmptyPassphrase = keysStorage.getRawKeys()
      .map { it.fingerprint }
      .firstOrNull {
        keysStorage.getPassphraseByFingerprint(it)?.isEmpty == false
      } ?: throw EmptyPassphraseException(
      fingerprints = keysStorage.getFingerprintsWithEmptyPassphrase(),
      message = context.getString(R.string.empty_pass_phrase)
    )
    return requireNotNull(
      keysStorage.getPassphraseByFingerprint(
        fingerprintOfKeyWithNotEmptyPassphrase
      )
    )
  }
}
