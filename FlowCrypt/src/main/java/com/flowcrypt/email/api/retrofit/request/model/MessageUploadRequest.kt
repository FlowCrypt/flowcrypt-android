/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model

/**
 * @author Denis Bondarenko
 *         Date: 12/20/21
 *         Time: 12:19 PM
 *         E-mail: DenBond7@gmail.com
 */
data class MessageUploadRequest(
  val associateReplyToken: String,
  val from: String,
  val to: List<String>,
  val cc: List<String> = emptyList(),
  val bcc: List<String> = emptyList()
)
