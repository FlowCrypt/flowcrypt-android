/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import com.google.gson.annotations.Expose
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class AlternativeContentMsgBlock(
  @Expose override val error: MsgBlockError? = null,
  @Expose val plainBlocks: List<MsgBlock>,
  @Expose val otherBlocks: List<MsgBlock>,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {
  @IgnoredOnParcel
  @Expose
  override val content: String? = null

  @IgnoredOnParcel
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.ALTERNATIVE

  @IgnoredOnParcel
  val allBlocks: List<MsgBlock>
    get() = plainBlocks + otherBlocks
}
