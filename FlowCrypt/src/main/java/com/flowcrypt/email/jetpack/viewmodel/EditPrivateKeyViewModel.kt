/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.toPgpKeyRingDetails
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.PGPainless
import org.pgpainless.key.util.UserId

/**
 * @author Denys Bondarenko
 */
class EditPrivateKeyViewModel(val fingerprint: String, application: Application) :
  AccountViewModel(application) {

  private val controlledRunnerForEditingPrivateKey = ControlledRunner<Result<Boolean?>>()
  private val editPrivateKeyMutableStateFlow: MutableStateFlow<Result<Boolean?>> =
    MutableStateFlow(Result.loading())
  val editPrivateKeyStateFlow: StateFlow<Result<Boolean?>> =
    editPrivateKeyMutableStateFlow.asStateFlow()

  fun addUserId(userId: UserId) {
    viewModelScope.launch {
      editPrivateKeyMutableStateFlow.value = Result.loading()
      editPrivateKeyMutableStateFlow.value =
        controlledRunnerForEditingPrivateKey.cancelPreviousThenRun {
          return@cancelPreviousThenRun addUserIdInternal(userId)
        }
    }
  }

  private suspend fun addUserIdInternal(userId: UserId): Result<Boolean?> =
    withContext(Dispatchers.IO) {
      try {
        val keyStore = KeysStorageImpl.getInstance(getApplication())
        val pgpSecretKeyRing = keyStore.getPGPSecretKeyRingByFingerprint(fingerprint)
          ?: throw IllegalStateException("Private key with fingerprint = $fingerprint not found")

        val modifiedPgpSecretKeyRing = PGPainless.modifyKeyRing(pgpSecretKeyRing)
          .addUserId(userId, keyStore.getSecretKeyRingProtector()).done()

        val account =
          keyStore.getActiveAccount() ?: throw IllegalStateException("Account is not defined")
        val entity =
          roomDatabase.keysDao().getKeyByAccountAndFingerprint(account.email, fingerprint)
            ?: throw IllegalStateException("Private key with fingerprint = $fingerprint not found")

        val pgpKeyRingDetails = modifiedPgpSecretKeyRing.toPgpKeyRingDetails()
        val encryptedPrvKey =
          KeyStoreCryptoManager.encryptSuspend(pgpKeyRingDetails.privateKey).toByteArray()
        roomDatabase.keysDao().updateSuspend(entity.copy(privateKey = encryptedPrvKey))
        //update or remove public key

        return@withContext Result.success(true)
      } catch (e: Exception) {
        return@withContext Result.exception(e)
      }
    }
}