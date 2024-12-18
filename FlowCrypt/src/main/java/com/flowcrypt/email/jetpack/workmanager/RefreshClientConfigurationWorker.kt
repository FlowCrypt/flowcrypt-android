/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.base.Result.Status
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil

/**
 * @author Denys Bondarenko
 */
class RefreshClientConfigurationWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {

  override suspend fun doWork(): Result {
    LogsUtil.d(TAG, "doWork")
    val tempAccount = roomDatabase.accountDao().getActiveAccountSuspend() ?: return Result.success()

    val domain = EmailUtil.getDomain(tempAccount.email)
    val isPublicDomain = domain in EmailUtil.getPublicEmailDomains()

    if (BuildConfig.FLAVOR == Constants.FLAVOR_NAME_ENTERPRISE && isPublicDomain) {
      return Result.success()
    }

    if (!tempAccount.useCustomerFesUrl && !isPublicDomain) {
      //attempting to call fes.domain.com
      val result = ApiClientRepository.FES.checkIfFesIsAvailableAtCustomerFesUrl(
        context = applicationContext,
        domain = domain
      )

      if (result.status == Status.SUCCESS &&
        result.data?.service in ApiClientRepository.FES.ALLOWED_SERVICES
      ) {
        roomDatabase.accountDao().updateAccountSuspend(tempAccount.copy(useCustomerFesUrl = true))
      }
    }

    val account = roomDatabase.accountDao().getActiveAccountSuspend() ?: return Result.success()

    val baseFesUrlPath = GeneralUtil.genBaseFesUrlPath(
      useCustomerFesUrl = if (isPublicDomain) false else account.useCustomerFesUrl,
      domain = domain
    )
    try {
      val idToken = GeneralUtil.getGoogleIdTokenSilently(
        context = applicationContext,
        maxRetryAttemptCount = 5,
        accountEntity = account
      )

      val result = ApiClientRepository.FES.getClientConfiguration(
        context = applicationContext,
        idToken = idToken,
        baseFesUrlPath = baseFesUrlPath,
        domain = domain
      )

      if (result.status == Status.SUCCESS) {
        val fetchedClientConfiguration = result.data?.clientConfiguration
        fetchedClientConfiguration?.let { clientConfiguration ->
          roomDatabase.accountDao()
            .updateSuspend(account.copy(clientConfiguration = clientConfiguration))
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    LogsUtil.d(TAG, "work was finished")
    return Result.success()
  }

  companion object {
    private val TAG = RefreshClientConfigurationWorker::class.java.simpleName
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".REFRESH_CLIENT_CONFIGURATION"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<RefreshClientConfigurationWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.KEEP
      )
    }
  }
}
