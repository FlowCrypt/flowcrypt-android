/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The message encryption type.
 *
 * @author Denys Bondarenko
 */
@Parcelize
enum class MessageEncryptionType : Parcelable {
  ENCRYPTED, STANDARD;
}
