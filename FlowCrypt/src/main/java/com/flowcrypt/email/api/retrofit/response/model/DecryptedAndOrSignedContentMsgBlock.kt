/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import com.google.gson.annotations.Expose
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.pgpainless.decryption_verification.OpenPgpMetadata

/**
 * @author Denis Bondarenko
 *         Date: 12/8/21
 *         Time: 6:55 PM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class DecryptedAndOrSignedContentMsgBlock(
  @Expose override val error: MsgBlockError? = null,
  @Expose val blocks: List<MsgBlock> = listOf(),
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {
  @IgnoredOnParcel
  @Expose
  override val content: String? = null

  @IgnoredOnParcel
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.DECRYPTED_AND_OR_SIGNED_CONTENT

  @IgnoredOnParcel
  var openPgpMetadata: OpenPgpMetadata? = null
}
