/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The model of [PostHelpFeedbackRequest].
 *
 * @author Denys Bondarenko
 */
data class PostHelpFeedbackModel(
  @Expose val email: String,
  @Expose val logs: String? = null,
  @Expose val screenshot: String? = null,
  @SerializedName("message") @Expose val msg: String
) : RequestModel
