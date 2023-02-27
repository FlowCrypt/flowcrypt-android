/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class Screenshot(val byteArray: ByteArray) : Parcelable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Screenshot

    if (!byteArray.contentEquals(other.byteArray)) return false

    return true
  }

  override fun hashCode(): Int {
    return byteArray.contentHashCode()
  }
}
