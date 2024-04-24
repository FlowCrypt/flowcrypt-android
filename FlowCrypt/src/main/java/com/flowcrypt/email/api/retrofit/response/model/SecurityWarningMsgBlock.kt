/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
class SecurityWarningMsgBlock(
  @Expose val warningType: WarningType,
  @Expose override val content: String? = null
) : MsgBlock {
  override val type: MsgBlock.Type = MsgBlock.Type.SECURITY_WARNING
  override val error: MsgBlockError? = null
  override val isOpenPGPMimeSigned: Boolean = false

  constructor(source: Parcel) : this(
    requireNotNull(source.readParcelableViaExt(WarningType::class.java)),
    source.readString()
  )

  @Parcelize
  enum class WarningType : Parcelable {
    RECEIVED_SPF_SOFT_FAIL,
  }

  companion object : Parceler<SecurityWarningMsgBlock> {

    override fun SecurityWarningMsgBlock.write(parcel: Parcel, flags: Int) = with(parcel) {
      writeParcelable(warningType, flags)
      writeString(content)
    }

    override fun create(parcel: Parcel): SecurityWarningMsgBlock {
      return SecurityWarningMsgBlock(parcel)
    }
  }
}
