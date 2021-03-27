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
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 11/12/19
 *         Time: 3:56 PM
 *         E-mail: DenBond7@gmail.com
 */
class CheckPrivateKeysViewModel(application: Application) : BaseAndroidViewModel(application) {
  val liveData: MutableLiveData<Result<List<NodeKeyDetails>>> = MutableLiveData()

  fun checkKeys(keys: List<NodeKeyDetails>, passphrase: String) {
    liveData.value = Result.loading()

    if (passphrase.isEmpty()) {
      liveData.value = Result.error(emptyList())
      return
    }

    val context: Context = getApplication()

    viewModelScope.launch {
      val resultList = mutableListOf<NodeKeyDetails>()
      for (keyDetails in keys) {
        val copy = keyDetails.copy()
        if (copy.isPrivate) {
          val prvKey = copy.privateKey ?: continue
          val decryptedKey = if (copy.isFullyDecrypted == true) {
            prvKey
          } else {
            try {
              PgpKey.decryptKey(prvKey, passphrase)
            } catch (e: Exception) {
              e.printStackTrace()
              copy.errorMsg = e.message
              ""
            }
          }

          if (decryptedKey.isNotEmpty()) {
            copy.passphrase = passphrase
          } else {
            copy.errorMsg = context.getString(R.string.password_is_incorrect)
          }
        } else {
          copy.errorMsg = context.getString(R.string.not_private_key)
        }

        resultList.add(copy)
      }

      liveData.value = Result.success(resultList)
    }
  }
}