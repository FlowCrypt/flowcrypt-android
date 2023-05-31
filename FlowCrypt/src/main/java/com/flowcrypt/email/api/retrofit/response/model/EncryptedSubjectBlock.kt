/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class EncryptedSubjectBlock(override val content: String) : MsgBlock {
  @IgnoredOnParcel
  override val type: MsgBlock.Type = MsgBlock.Type.ENCRYPTED_SUBJECT

  @IgnoredOnParcel
  override val error: MsgBlockError? = null

  @IgnoredOnParcel
  override val isOpenPGPMimeSigned: Boolean = false
}
