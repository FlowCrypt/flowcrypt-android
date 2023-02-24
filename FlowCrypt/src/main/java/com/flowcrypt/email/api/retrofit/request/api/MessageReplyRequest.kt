/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.api

import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.model.MessageReplyModel

/**
 * This class describes a request to the https://flowcrypt.com/api/message/reply API.
 *
 *
 * `POST /message/reply  {
 * "short" (<type></type>'str'>)  # original message short id
 * "token" (<type></type>'str'>)  # message token
 * "message" (<type></type>'str'>)  # encrypted message
 * "subject" (<type></type>'str'>)  # subject of sent email
 * "from" (<type></type>'str'>)  # sender (user of the web app)
 * "to" (<type></type>'str'>)  # recipient (CryptUp user and the sender of the original message)
 * }`
 *
 * @author Denys Bondarenko
 */
//todo-denbond7 this class is redundant. We can refactor code and delete this
class MessageReplyRequest(
  override val apiName: ApiName = ApiName.POST_MESSAGE_REPLY,
  override val requestModel: MessageReplyModel
) : BaseRequest<MessageReplyModel>
