/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node

import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.model.MessageEncryptionType
import com.google.gson.annotations.Expose

/**
 * Using this class we can create a request to create a raw MIME message(encrypted or plain).
 *
 * @author Denis Bondarenko
 * Date: 3/27/19
 * Time: 3:00 PM
 * E-mail: DenBond7@gmail.com
 */
class ComposeEmailRequest(info: OutgoingMessageInfo?,
                          @field:Expose val pubKeys: List<String>?) : BaseNodeRequest() {

  @Expose
  private var format: String? = null

  @Expose
  private var text: String? = null

  @Expose
  private var to: List<String>? = null

  @Expose
  private var cc: List<String>? = null

  @Expose
  private var bcc: List<String>? = null

  @Expose
  private var from: String? = null

  @Expose
  private var subject: String? = null

  @Expose
  private var replyToMimeMsg: String? = null
  //todo-tomholub Maybe we have to rename this field for better understanding. It contains only headers (not whole MIME)

  override val endpoint: String = "composeEmail"

  override val data: ByteArray
    get() = ByteArray(0)

  init {
    if (info != null) {
      format = if (info.encryptionType === MessageEncryptionType.ENCRYPTED) FORMAT_ENCRYPT_INLINE else FORMAT_PLAIN
      text = info.msg
      to = info.toRecipients
      cc = info.ccRecipients
      bcc = info.bccRecipients
      from = info.from
      subject = info.subject
      replyToMimeMsg = info.origMsgHeaders
    }
  }

  companion object {
    private const val FORMAT_ENCRYPT_INLINE = "encrypt-inline"
    private const val FORMAT_PLAIN = "plain"
  }
}
