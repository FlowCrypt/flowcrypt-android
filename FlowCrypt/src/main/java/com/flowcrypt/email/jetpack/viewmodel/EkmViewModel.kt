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

  fun fetchPrvKeys(orgRules: OrgRules, tokenId: String) {
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
          tokenId = tokenId
        )

        if (ekmPrivateResult.data?.privateKeys?.isEmpty() == true) {
          throw IllegalStateException(context.getString(R.string.no_prv_keys_ask_admin))
        }

        ekmLiveData.value = ekmPrivateResult
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
          DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing" +
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
          DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN.name + " + missing" + DomainRule.NO_PRV_CREATE
        )
      }
    }
    return null
  }
}
