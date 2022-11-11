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
 * @author Denis Bondarenko
 * Date: 28.07.2017
 * Time: 15:41
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
enum class MessageEncryptionType : Parcelable {
  ENCRYPTED, STANDARD;
}
