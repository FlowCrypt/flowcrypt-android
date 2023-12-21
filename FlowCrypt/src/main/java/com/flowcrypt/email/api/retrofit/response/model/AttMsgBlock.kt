/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.AttachmentInfo

interface AttMsgBlock : MsgBlock {
  val attMeta: AttMeta

  fun toAttachmentInfo(): AttachmentInfo {
    return AttachmentInfo.Builder(
      rawData = attMeta.data,
      type = attMeta.type ?: Constants.MIME_TYPE_BINARY_DATA,
      name = attMeta.name?.take(255),
      encodedSize = attMeta.length,
      id = EmailUtil.generateContentId(),
      isDecrypted = true
    ).build()
  }
}
