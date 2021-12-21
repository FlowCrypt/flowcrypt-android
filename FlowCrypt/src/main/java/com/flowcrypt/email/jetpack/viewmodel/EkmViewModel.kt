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
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.response.api.EkmPrivateKeysResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.api.retrofit.response.model.OrgRules.DomainRule
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.EkmNotSupportedException
import com.flowcrypt.email.util.exception.UnsupportedOrgRulesException
import kotlinx.coroutines.launch

/**
 * This [ViewModel] can be used to apply EKM functionality for enterprise users.
 *
 * @author Denis Bondarenko
 *         Date: 6/30/21
 *         Time: 6:56 PM
 *         E-mail: DenBond7@gmail.com
 */
class EkmViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val repository = FlowcryptApiRepository()
  val ekmLiveData: MutableLiveData<Result<EkmPrivateKeysResponse>> = MutableLiveData(Result.none())

  fun fetchPrvKeys(orgRules: OrgRules, idToken: String) {
    viewModelScope.launch {
      val context: Context = getApplication()
      ekmLiveData.value = Result.loading(progressMsg = context.getString(R.string.fetching_keys))
      try {
        if (!orgRules.usesKeyManager() || !orgRules.mustAutoImportOrAutoGenPrvWithKeyManager()) {
          ekmLiveData.value = Result.exception(
            EkmNotSupportedException(orgRules)
          )
          return@launch
        }

        val unsupportedOrgRulesException = checkForUnsupportedOrgRulesCombination(orgRules)
        if (unsupportedOrgRulesException != null) {
          ekmLiveData.value = Result.exception(unsupportedOrgRulesException)
          return@launch
        }

        val ekmPrivateResult = repository.getPrivateKeysViaEkm(
          context = context,
          ekmUrl = orgRules.keyManagerUrl
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
            pgpKeyDetailsList.addAll(parsedList)
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

  private fun checkForUnsupportedOrgRulesCombination(orgRules: OrgRules):
      UnsupportedOrgRulesException? {
    if (orgRules.hasRule(DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN)) {
      if (orgRules.hasRule(DomainRule.PASS_PHRASE_QUIET_AUTOGEN)) {
        return UnsupportedOrgRulesException(
          DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + " + DomainRule.PASS_PHRASE_QUIET_AUTOGEN
        )
      }

      if (!orgRules.hasRule(DomainRule.FORBID_STORING_PASS_PHRASE)) {
        return UnsupportedOrgRulesException(
          DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing " +
              DomainRule.FORBID_STORING_PASS_PHRASE
        )
      }

      if (orgRules.hasRule(DomainRule.ENFORCE_ATTESTER_SUBMIT)) {
        return UnsupportedOrgRulesException(
          DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + " + DomainRule.ENFORCE_ATTESTER_SUBMIT
        )
      }

      if (!orgRules.hasRule(DomainRule.NO_PRV_CREATE)) {
        return UnsupportedOrgRulesException(
          DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing " + DomainRule.NO_PRV_CREATE
        )
      }
    }
    return null
  }
}
