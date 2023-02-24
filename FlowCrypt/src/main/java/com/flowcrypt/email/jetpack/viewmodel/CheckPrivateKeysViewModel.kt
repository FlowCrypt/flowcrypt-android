/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.org.pgpainless.util.asString
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.WrongPassPhraseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.exception.KeyIntegrityException
import org.pgpainless.util.Passphrase

/**
 * @author Denys Bondarenko
 */
class CheckPrivateKeysViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val controlledRunnerForChecking = ControlledRunner<Result<List<CheckResult>>>()
  val checkPrvKeysLiveData: MutableLiveData<Result<List<CheckResult>>> = MutableLiveData()

  fun checkKeys(keys: List<PgpKeyDetails>, passphrase: Passphrase) {
    viewModelScope.launch {
      checkPrvKeysLiveData.value = Result.loading()
      if (passphrase.isEmpty) {
        checkPrvKeysLiveData.value = Result.error(emptyList())
        return@launch
      }
      checkPrvKeysLiveData.value = controlledRunnerForChecking.cancelPreviousThenRun {
        return@cancelPreviousThenRun Result.success(checkKeysInternal(keys, passphrase))
      }
    }
  }

  private suspend fun checkKeysInternal(keys: List<PgpKeyDetails>, passphrase: Passphrase):
      List<CheckResult> =
    withContext(Dispatchers.IO) {
      val context: Context = getApplication()
      val resultList = mutableListOf<CheckResult>()
      for (keyDetails in keys) {
        val copy = keyDetails.copy()
        var e: Throwable? = null
        if (copy.isPrivate) {
          if (copy.passphraseType == null) {
            e = IllegalArgumentException(context.getString(R.string.passphrase_type_undefined))
          } else {
            val prvKey = copy.privateKey
            if (prvKey.isNullOrEmpty()) {
              e = IllegalArgumentException("Empty source")
            } else {
              if (copy.isFullyDecrypted) {
                copy.tempPassphrase = passphrase.chars
              } else {
                try {
                  PgpKey.decryptKey(prvKey, passphrase)
                  //https://github.com/FlowCrypt/flowcrypt-android/issues/1669
                  PgpKey.checkSecretKeyIntegrity(prvKey, passphrase)
                  copy.tempPassphrase = passphrase.chars
                } catch (ex: Throwable) {
                  //to prevent leak sensitive info we skip printing stack trace for release builds
                  if (GeneralUtil.isDebugBuild()) {
                    ex.printStackTrace()
                  }

                  e = when (ex) {
                    is KeyIntegrityException -> ex

                    else -> WrongPassPhraseException(
                      message = context.getString(R.string.password_is_incorrect), cause = ex
                    )
                  }
                }
              }
            }
          }
        } else {
          e = IllegalArgumentException(context.getString(R.string.not_private_key))
        }

        resultList.add(
          CheckResult(
            pgpKeyDetails = copy,
            passphrase = passphrase.asString ?: throw IllegalArgumentException(),
            e = e
          )
        )
      }
      return@withContext resultList
    }

  data class CheckResult(
    val pgpKeyDetails: PgpKeyDetails,
    val passphrase: String, val e: Throwable? = null
  )
}
