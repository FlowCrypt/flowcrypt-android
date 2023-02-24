/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * The request model for the https://flowcrypt.com/api/message/reply API.
 *
 * @author Denys Bondarenko
 */
data class MessageReplyModel(
  @SerializedName("short") @Expose val shortValue: String,
  @SerializedName("token") @Expose val token: String,
  @SerializedName("message") @Expose val message: String,
  @SerializedName("subject") @Expose val subject: String,
  @SerializedName("from") @Expose val from: String,
  @SerializedName("to") @Expose val to: String
) : RequestModel
