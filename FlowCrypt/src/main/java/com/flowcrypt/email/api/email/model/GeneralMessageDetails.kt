/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcelable
import com.flowcrypt.email.database.MessageState
import jakarta.mail.internet.InternetAddress
import kotlinx.parcelize.Parcelize

/**
 * Simple POJO class which describe a general message details.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class GeneralMessageDetails constructor(
  val email: String,
  val label: String,
  val uid: Int = 0,
  val id: Int,
  val receivedDate: Long = 0,
  val sentDate: Long = 0,
  var from: List<@JvmSuppressWildcards InternetAddress>? = null,
  var replyTo: List<@JvmSuppressWildcards InternetAddress>? = null,
  var to: List<@JvmSuppressWildcards InternetAddress>? = null,
  var cc: List<@JvmSuppressWildcards InternetAddress>? = null,
  var subject: String? = null,
  var msgFlags: List<String> = arrayListOf(),
  var isRawMsgAvailable: Boolean = false,
  var hasAtts: Boolean = false,
  var isEncrypted: Boolean = false,
  var msgState: MessageState = MessageState.NONE,
  var attsDir: String? = null,
  var errorMsg: String? = null
) : Parcelable {

  fun isSeen(): Boolean {
    return msgFlags.contains(MessageFlag.SEEN.value)
  }

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  val allRecipients: List<String>
    get() {
      val emails = ArrayList<String>()

      if (to != null) {
        for (internetAddress in to!!) {
          emails.add(internetAddress.address)
        }
      }

      if (cc != null) {
        for (internetAddress in cc!!) {
          emails.add(internetAddress.address)
        }
      }

      return emails
    }
}
