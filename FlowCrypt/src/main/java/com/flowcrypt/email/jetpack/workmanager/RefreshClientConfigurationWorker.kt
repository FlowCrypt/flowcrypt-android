/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.model.LoginModel
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.DomainOrgRulesResponse
import com.flowcrypt.email.api.retrofit.response.base.Result.Status
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil

/**
 * @author Denis Bondarenko
 *         Date: 11/24/21
 *         Time: 4:31 PM
 *         E-mail: DenBond7@gmail.com
 */
class RefreshClientConfigurationWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params) {

  override suspend fun doWork(): Result {
    LogsUtil.d(TAG, "doWork")
    val repository = FlowcryptApiRepository()
    val publicEmailDomains = EmailUtil.getPublicEmailDomains()
    val account = roomDatabase.accountDao().getActiveAccount() ?: return Result.success()

    val domain = EmailUtil.getDomain(account.email)
    if (domain in publicEmailDomains) {
      return Result.success()
    }

    val fesUrl = GeneralUtil.generateFesUrl(domain)
    try {
      val retryAttempts = 5
      val idToken = GeneralUtil.getGoogleIdToken(
        context = applicationContext,
        maxRetryAttemptCount = retryAttempts
      )

      val result = repository.getDomainOrgRules(
        context = applicationContext,
        loginModel = LoginModel(account.email, idToken),
        fesUrl = fesUrl
      )

      if (result.status == Status.SUCCESS) {
        val fetchedOrgRules = (result.data as? DomainOrgRulesResponse)?.orgRules
          ?: (result.data as? ClientConfigurationResponse)?.orgRules
        fetchedOrgRules?.let { orgRules ->
          roomDatabase.accountDao().updateSuspend(account.copy(clientConfiguration = orgRules))
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
    val NAME = RefreshClientConfigurationWorker::class.java.simpleName

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          NAME,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<RefreshClientConfigurationWorker>()
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
