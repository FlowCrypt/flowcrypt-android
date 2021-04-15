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
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.WrongPassPhraseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author Denis Bondarenko
 *         Date: 11/12/19
 *         Time: 3:56 PM
 *         E-mail: DenBond7@gmail.com
 */
class CheckPrivateKeysViewModel(application: Application) : BaseAndroidViewModel(application) {
  val checkPrvKeysLiveData: MutableLiveData<Result<List<CheckResult>>> = MutableLiveData()

  fun checkKeys(keys: List<NodeKeyDetails>, passphrase: String) {
    viewModelScope.launch {
      checkPrvKeysLiveData.value = Result.loading()
      if (passphrase.isEmpty()) {
        checkPrvKeysLiveData.value = Result.error(emptyList())
        return@launch
      }
      checkPrvKeysLiveData.value = Result.success(checkKeysInternal(keys, passphrase))
    }
  }

  private suspend fun checkKeysInternal(keys: List<NodeKeyDetails>,
                                        passphrase: String): List<CheckResult> =
      withContext(Dispatchers.IO) {
        val context: Context = getApplication()
        val resultList = mutableListOf<CheckResult>()
        for (keyDetails in keys) {
          val copy = keyDetails.copy()
          var e: Exception? = null
          if (copy.isPrivate) {
            val prvKey = copy.privateKey
            if (prvKey.isNullOrEmpty()) {
              e = IllegalArgumentException("Empty source")
            } else {
              if (copy.isFullyDecrypted == true) {
                copy.passphrase = passphrase
              } else {
                try {
                  PgpKey.decryptKey(prvKey, passphrase)
                  copy.passphrase = passphrase
                } catch (ex: Exception) {
                  //to prevent leak sensitive info we skip printing stack trace for release builds
                  if (GeneralUtil.isDebugBuild()) {
                    ex.printStackTrace()
                  }
                  e = WrongPassPhraseException(
                      message = context.getString(R.string.password_is_incorrect), cause = ex)
                }
              }
            }
          } else {
            e = IllegalArgumentException(context.getString(R.string.not_private_key))
          }

          resultList.add(CheckResult(copy, passphrase, e))
        }
        return@withContext resultList
      }

  data class CheckResult(val nodeKeyDetails: NodeKeyDetails,
                         val passphrase: String, val e: Exception? = null)
}