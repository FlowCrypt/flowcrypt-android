/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The model of [PostHelpFeedbackRequest].
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 12:42
 * E-mail: DenBond7@gmail.com
 */

data class PostHelpFeedbackModel(@Expose val email: String,
                                 @Expose val logs: String? = null,
                                 @Expose val screenshot: String? = null,
                                 @SerializedName("message") @Expose val msg: String) : RequestModel
