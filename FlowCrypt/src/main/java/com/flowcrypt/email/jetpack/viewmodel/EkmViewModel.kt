/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration.ConfigurationProperty
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.EkmNotSupportedException
import com.flowcrypt.email.util.exception.UnsupportedClientConfigurationException
import kotlinx.coroutines.launch

/**
 * This [ViewModel] can be used to apply EKM functionality for enterprise users.
 *
 * @author Denys Bondarenko
 */
class EkmViewModel(application: Application) : BaseAndroidViewModel(application) {
  val ekmLiveData: MutableLiveData<Result<EkmPrivateKeysResponse>> = MutableLiveData(Result.none())

  fun fetchPrvKeys(clientConfiguration: ClientConfiguration, idToken: String) {
    viewModelScope.launch {
      val context: Context = getApplication()
      ekmLiveData.value = Result.loading(progressMsg = context.getString(R.string.fetching_keys))
      try {
        if (!clientConfiguration.usesKeyManager() || !clientConfiguration.mustAutoImportOrAutoGenPrvWithKeyManager()) {
          ekmLiveData.value = Result.exception(
            EkmNotSupportedException(clientConfiguration)
          )
          return@launch
        }

        val unsupportedClientConfigurationException =
          checkForUnsupportedClientConfigurationCombination(clientConfiguration)
        if (unsupportedClientConfigurationException != null) {
          ekmLiveData.value = Result.exception(unsupportedClientConfigurationException)
          return@launch
        }

        val ekmPrivateResult = ApiClientRepository.EKM.getPrivateKeys(
          context = context,
          ekmUrl = clientConfiguration.keyManagerUrl
            ?: throw IllegalArgumentException("key_manager_url is empty"),
          idToken = idToken
        )

        if (ekmPrivateResult.status != Result.Status.SUCCESS) {
          ekmLiveData.value = ekmPrivateResult
          return@launch
        }

        if (ekmPrivateResult.data?.privateKeys?.isEmpty() == true) {
          throw IllegalStateException(context.getString(R.string.no_prv_keys_ask_admin))
        }

        val pgpKeyDetailsList = mutableListOf<PgpKeyDetails>()
        ekmPrivateResult.data?.privateKeys?.forEach { key ->
          val parsedList = PgpKey.parsePrivateKeys(requireNotNull(key.decryptedPrivateKey))
            .map { it.copy(passphraseType = KeyEntity.PassphraseType.RAM) }

          if (parsedList.isEmpty()) {
            throw IllegalStateException(context.getString(R.string.could_not_parse_one_of_ekm_key))
          } else {
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
            pgpKeyDetailsList.addAll(parsedList.map {
              it.copy(importSourceType = KeyImportDetails.SourceType.EKM)
            })
          }
        }

        ekmLiveData.value = ekmPrivateResult.copy(
          data = ekmPrivateResult.data?.copy(pgpKeyDetailsList = pgpKeyDetailsList)
        )
      } catch (e: Exception) {
        ekmLiveData.value = Result.exception(e)
      }
    }
  }

  private fun checkForUnsupportedClientConfigurationCombination(clientConfiguration: ClientConfiguration):
      UnsupportedClientConfigurationException? {
    if (clientConfiguration.hasProperty(ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN)) {
      if (clientConfiguration.hasProperty(ConfigurationProperty.PASS_PHRASE_QUIET_AUTOGEN)) {
        return UnsupportedClientConfigurationException(
          ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + " + ConfigurationProperty.PASS_PHRASE_QUIET_AUTOGEN
        )
      }

      if (!clientConfiguration.hasProperty(ConfigurationProperty.FORBID_STORING_PASS_PHRASE)) {
        return UnsupportedClientConfigurationException(
          ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing " +
              ConfigurationProperty.FORBID_STORING_PASS_PHRASE
        )
      }

      if (clientConfiguration.hasProperty(ConfigurationProperty.ENFORCE_ATTESTER_SUBMIT)) {
        return UnsupportedClientConfigurationException(
          ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + " + ConfigurationProperty.ENFORCE_ATTESTER_SUBMIT
        )
      }

      if (!clientConfiguration.hasProperty(ConfigurationProperty.NO_PRV_CREATE)) {
        return UnsupportedClientConfigurationException(
          ConfigurationProperty.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing " + ConfigurationProperty.NO_PRV_CREATE
        )
      }
    }
    return null
  }
}
