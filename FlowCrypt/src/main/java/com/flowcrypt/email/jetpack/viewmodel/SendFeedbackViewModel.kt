/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.model.Screenshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 8/8/22
 *         Time: 1:07 PM
 *         E-mail: DenBond7@gmail.com
 */
class SendFeedbackViewModel(application: Application) : BaseAndroidViewModel(application) {
  private val apiClientRepository = ApiClientRepository()
  private val postFeedbackMutableStateFlow: MutableStateFlow<Result<PostHelpFeedbackResponse?>> =
    MutableStateFlow(Result.none())
  val postFeedbackStateFlow: StateFlow<Result<PostHelpFeedbackResponse?>> =
    postFeedbackMutableStateFlow.asStateFlow()

  fun postFeedback(
    account: AccountEntity,
    feedbackMsg: String,
    screenshot: Screenshot? = null
  ) {
    viewModelScope.launch {
      val context: Context = getApplication()
      postFeedbackMutableStateFlow.value =
        Result.loading(progressMsg = context.getString(R.string.sending))
      val screenShotBase64 =
        Base64.encodeToString(screenshot?.byteArray ?: byteArrayOf(), Base64.DEFAULT)

      try {
        postFeedbackMutableStateFlow.value = apiClientRepository.postHelpFeedback(
          context = context,
          PostHelpFeedbackModel(
            email = account.email,
            logs = "",
            screenshot = screenShotBase64,
            msg = "$feedbackMsg\n\nversion: Android ${BuildConfig.VERSION_NAME}"
          )
        )
      } catch (e: Exception) {
        postFeedbackMutableStateFlow.value = Result.exception(e)
      }
    }
  }
}
