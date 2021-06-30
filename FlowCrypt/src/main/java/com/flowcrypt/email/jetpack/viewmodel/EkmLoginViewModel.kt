/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *  DenBond7
 *  Ivan Pizhenko
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.api.DomainRulesRequest
import com.flowcrypt.email.api.retrofit.request.api.LoginRequest
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.util.exception.EkmNotSupportedException
import com.flowcrypt.email.util.exception.OrgRulesCombinationNotSupportedException
import kotlinx.coroutines.launch

/**
 * This [androidx.lifecycle.ViewModel] can be used to resolve domain rules for enterprise users.
 *
 * @author Denis Bondarenko
 *         Date: 10/23/19
 *         Time: 12:36 PM
 *         E-mail: DenBond7@gmail.com
 */
class EkmLoginViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val repository: ApiRepository = FlowcryptApiRepository()
  val ekmLiveData: MutableLiveData<Result<ApiResponse>?> = MutableLiveData()

  fun fetchPrvKeys(account: String, uuid: String, tokenId: String) {
    ekmLiveData.value = Result.loading()
    val context: Context = getApplication()

    viewModelScope.launch {
      ekmLiveData.value = Result.loading(progressMsg = context.getString(R.string.loading))
      try {
        val loginResult = repository.login(
          context,
          LoginRequest(ApiName.POST_LOGIN, LoginModel(account, uuid), tokenId)
        )

        when (loginResult.status) {
          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            ekmLiveData.value = loginResult
            return@launch
          }

          Result.Status.SUCCESS -> {
            if (loginResult.data?.isVerified == true) {
              ekmLiveData.value = Result.loading(
                progressMsg = context.getString(R.string.loading_domain_rules)
              )
              val domainRulesResult = repository.getDomainRules(
                context,
                DomainRulesRequest(ApiName.POST_GET_DOMAIN_RULES, LoginModel(account, uuid))
              )

              if (domainRulesResult.status == Result.Status.SUCCESS) {
                if (domainRulesResult.data?.orgRules == null) {
                  ekmLiveData.value = Result.exception(
                    IllegalArgumentException("${OrgRules::class.java.simpleName} is not specified")
                  )
                  return@launch
                }

                if (!domainRulesResult.data.orgRules.usesKeyManager() ||
                  !domainRulesResult.data.orgRules.mustAutoImportOrAutoGenPrvWithKeyManager()
                ) {
                  ekmLiveData.value = Result.exception(
                    EkmNotSupportedException(domainRulesResult.data.orgRules)
                  )
                  return@launch
                }

                val notSupportedCombination =
                  checkNotSupportedOrgRulesCombination(domainRulesResult.data.orgRules)

                if (notSupportedCombination.isNotEmpty()) {
                  ekmLiveData.value = Result.exception(
                    OrgRulesCombinationNotSupportedException(
                      orgRules = domainRulesResult.data.orgRules,
                      combination = notSupportedCombination
                    )
                  )
                  return@launch
                }

                val ekmPrivateResult = repository.getPrivateKeysViaEkm(
                  context = context,
                  ekmUrl = domainRulesResult.data.orgRules.keyManagerUrl
                    ?: throw java.lang.IllegalArgumentException("key_manager_url is empty"),
                  tokenId = tokenId
                )

                if (ekmPrivateResult.data?.privateKeys?.isEmpty() != true) {
                  throw java.lang.IllegalStateException(context.getString(R.string.no_prv_keys_ask_admin))
                }

                ekmLiveData.value = ekmPrivateResult
              } else {
                ekmLiveData.value = domainRulesResult
              }
            } else {
              ekmLiveData.value = Result.error(
                LoginResponse(
                  ApiError(
                    -1,
                    context.getString(R.string.user_not_verified)
                  ), false
                )
              )
            }
          }

          else -> {
            ekmLiveData.value = Result.exception(IllegalStateException("Unhandled error"))
          }
        }
      } catch (e: Exception) {
        ekmLiveData.value = Result.exception(e)
      }
    }
  }

  private fun checkNotSupportedOrgRulesCombination(orgRules: OrgRules):
      Map<OrgRules.DomainRule, Boolean> {
    val notSupportedCombination = mutableMapOf(
      OrgRules.DomainRule.PRV_AUTOIMPORT_OR_AUTOGEN to true
    )

    when {
      orgRules.mustAutoGenPassPhraseQuietly() -> {
        notSupportedCombination[OrgRules.DomainRule.PASS_PHRASE_QUIET_AUTOGEN] = true
      }

      !orgRules.forbidStoringPassPhrase() -> {
        notSupportedCombination[OrgRules.DomainRule.FORBID_STORING_PASS_PHRASE] = false
      }

      orgRules.mustSubmitToAttester() -> {
        notSupportedCombination[OrgRules.DomainRule.ENFORCE_ATTESTER_SUBMIT] = true
      }

      !orgRules.forbidCreatingPrivateKey() -> {
        notSupportedCombination[OrgRules.DomainRule.NO_PRV_CREATE] = false
      }

      else -> notSupportedCombination.clear()
    }
    return notSupportedCombination
  }
}
