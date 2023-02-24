/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.content.Context
import android.os.Parcelable
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import kotlinx.parcelize.Parcelize

/**
 * This class describes settings for some security type.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class SecurityType constructor(
  val name: String = "",
  val opt: Option = Option.SSL_TLS,
  val defImapPort: Int = 993,
  val defSmtpPort: Int = 465
) : Parcelable {

  override fun toString(): String {
    return name
  }

  @Parcelize
  enum class Option : Parcelable {
    NONE, SSL_TLS, STARTLS;
  }

  companion object {
    /**
     * Generate a list which contains all available [SecurityType].
     *
     * @return The list of all available [SecurityType].
     */
    fun generateSecurityTypes(context: Context): MutableList<SecurityType> {
      val securityTypes = mutableListOf<SecurityType>()
      securityTypes.add(
        SecurityType(
          context.getString(R.string.ssl_tls), Option.SSL_TLS,
          JavaEmailConstants.SSL_IMAP_PORT, JavaEmailConstants.SSL_SMTP_PORT
        )
      )
      securityTypes.add(
        SecurityType(
          context.getString(R.string.startls), Option.STARTLS,
          JavaEmailConstants.DEFAULT_IMAP_PORT, JavaEmailConstants.STARTTLS_SMTP_PORT
        )
      )
      securityTypes.add(
        SecurityType(
          context.getString(R.string.none), Option.NONE,
          JavaEmailConstants.DEFAULT_IMAP_PORT, JavaEmailConstants.DEFAULT_SMTP_PORT
        )
      )
      return securityTypes
    }
  }
}
