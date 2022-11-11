/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.accounts.Account
import android.os.Parcelable
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.util.FlavorSettings
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 *         Date: 12/4/19
 *         Time: 2:50 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(
  tableName = "accounts",
  indices = [
    Index(name = "email_account_type_in_accounts", value = ["email", "account_type"], unique = true)
  ]
)
@Parcelize
data class AccountEntity constructor(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val email: String,
  @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String? = null,
  @ColumnInfo(name = "display_name", defaultValue = "NULL") val displayName: String? = null,
  @ColumnInfo(name = "given_name", defaultValue = "NULL") val givenName: String? = null,
  @ColumnInfo(name = "family_name", defaultValue = "NULL") val familyName: String? = null,
  @ColumnInfo(name = "photo_url", defaultValue = "NULL") val photoUrl: String? = null,
  @ColumnInfo(name = "is_enabled", defaultValue = "1") val isEnabled: Boolean? = true,
  @ColumnInfo(name = "is_active", defaultValue = "0") val isActive: Boolean? = false,
  val username: String,
  val password: String,
  @ColumnInfo(name = "imap_server") val imapServer: String,
  @ColumnInfo(name = "imap_port", defaultValue = "143") val imapPort: Int? = 143,
  @ColumnInfo(name = "imap_use_ssl_tls", defaultValue = "0") val imapUseSslTls: Boolean? = false,
  @ColumnInfo(name = "imap_use_starttls", defaultValue = "0") val imapUseStarttls: Boolean? = false,
  @ColumnInfo(name = "imap_auth_mechanisms") val imapAuthMechanisms: String? = null,
  @ColumnInfo(name = "smtp_server") val smtpServer: String,
  @ColumnInfo(name = "smtp_port", defaultValue = "25") val smtpPort: Int? = 25,
  @ColumnInfo(name = "smtp_use_ssl_tls", defaultValue = "0") val smtpUseSslTls: Boolean? = false,
  @ColumnInfo(name = "smtp_use_starttls", defaultValue = "0") val smtpUseStarttls: Boolean? = false,
  @ColumnInfo(name = "smtp_auth_mechanisms") val smtpAuthMechanisms: String? = null,
  @ColumnInfo(
    name = "smtp_use_custom_sign",
    defaultValue = "0"
  ) val smtpUseCustomSign: Boolean? = false,
  @ColumnInfo(name = "smtp_username", defaultValue = "NULL") val smtpUsername: String? = null,
  @ColumnInfo(name = "smtp_password", defaultValue = "NULL") val smtpPassword: String? = null,
  @ColumnInfo(name = "contacts_loaded", defaultValue = "0") val contactsLoaded: Boolean? = false,
  @ColumnInfo(
    name = "show_only_encrypted",
    defaultValue = "0"
  ) val showOnlyEncrypted: Boolean? = false,
  @ColumnInfo(
    name = "client_configuration",
    defaultValue = "NULL"
  ) val clientConfiguration: OrgRules? = null,
  @ColumnInfo(name = "use_api", defaultValue = "0") val useAPI: Boolean = false,
  @ColumnInfo(name = "use_fes", defaultValue = "0") val useFES: Boolean = false
) : Parcelable {

  @IgnoredOnParcel
  @Ignore
  val account: Account = Account(
    this.email, accountType
      ?: this.email.substring(this.email.indexOf('@') + 1).lowercase()
  )

  @IgnoredOnParcel
  @Ignore
  val isGoogleSignInAccount: Boolean = ACCOUNT_TYPE_GOOGLE.equals(accountType, ignoreCase = true)

  val useOAuth2: Boolean
    get() = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2 == imapAuthMechanisms

  constructor(
    googleSignInAccount: GoogleSignInAccount,
    orgRules: OrgRules? = null,
    useFES: Boolean,
    useStartTlsForSmtp: Boolean = false,
  ) : this(
    email = requireNotNull(googleSignInAccount.email).lowercase(),
    accountType = googleSignInAccount.account?.type?.lowercase(),
    displayName = googleSignInAccount.displayName,
    givenName = googleSignInAccount.givenName,
    familyName = googleSignInAccount.familyName,
    photoUrl = googleSignInAccount.photoUrl?.toString(),
    isEnabled = true,
    isActive = false,
    username = requireNotNull(googleSignInAccount.email),
    password = "",
    imapServer = GmailConstants.GMAIL_IMAP_SERVER,
    imapPort = GmailConstants.GMAIL_IMAP_PORT,
    imapUseSslTls = true,
    imapUseStarttls = false,
    imapAuthMechanisms = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2,
    smtpServer = GmailConstants.GMAIL_SMTP_SERVER,
    smtpPort = if (useStartTlsForSmtp) {
      GmailConstants.GMAIL_SMTP_PORT_STARTTLS
    } else {
      GmailConstants.GMAIL_SMTP_PORT_SSL
    },
    smtpUseSslTls = !useStartTlsForSmtp,
    smtpUseStarttls = useStartTlsForSmtp,
    smtpAuthMechanisms = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2,
    smtpUseCustomSign = false,
    smtpUsername = null,
    smtpPassword = null,
    contactsLoaded = false,
    showOnlyEncrypted = false,
    clientConfiguration = orgRules,
    useAPI = FlavorSettings.isGMailAPIEnabled(),
    useFES = useFES
  )

  constructor(authCredentials: AuthCredentials, orgRules: OrgRules? = null) :
      this(
        email = authCredentials.email.lowercase(),
        accountType =
        authCredentials.email.substring(authCredentials.email.indexOf('@') + 1).lowercase(),
        displayName = authCredentials.displayName,
        givenName = null,
        familyName = null,
        photoUrl = null,
        isEnabled = true,
        isActive = false,
        username = authCredentials.username,
        password = authCredentials.password,
        imapServer = authCredentials.imapServer.lowercase(),
        imapPort = authCredentials.imapPort,
        imapUseSslTls = authCredentials.imapOpt === SecurityType.Option.SSL_TLS,
        imapUseStarttls = authCredentials.imapOpt === SecurityType.Option.STARTLS,
        imapAuthMechanisms = if (authCredentials.useOAuth2) JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2 else null,
        smtpServer = authCredentials.smtpServer.lowercase(),
        smtpPort = authCredentials.smtpPort,
        smtpUseSslTls = authCredentials.smtpOpt === SecurityType.Option.SSL_TLS,
        smtpUseStarttls = authCredentials.smtpOpt === SecurityType.Option.STARTLS,
        smtpAuthMechanisms = if (authCredentials.useOAuth2) JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2 else null,
        smtpUseCustomSign = authCredentials.hasCustomSignInForSmtp,
        smtpUsername = authCredentials.smtpSigInUsername,
        smtpPassword = authCredentials.smtpSignInPassword,
        contactsLoaded = false,
        showOnlyEncrypted = false,
        clientConfiguration = orgRules,
        useAPI = false,
        useFES = false
      )

  constructor(email: String) :
      this(
        email = email,
        accountType = null,
        displayName = null,
        givenName = null,
        familyName = null,
        photoUrl = null,
        isEnabled = true,
        isActive = false,
        username = "",
        password = "",
        imapServer = "",
        imapPort = 0,
        imapUseSslTls = true,
        imapUseStarttls = false,
        imapAuthMechanisms = null,
        smtpServer = "",
        smtpPort = 0,
        smtpUseSslTls = true,
        smtpUseStarttls = false,
        smtpAuthMechanisms = "",
        smtpUseCustomSign = false,
        smtpUsername = null,
        smtpPassword = null,
        contactsLoaded = false,
        showOnlyEncrypted = false,
        clientConfiguration = null,
        useAPI = false
      )

  fun imapOpt(): SecurityType.Option {
    return when {
      imapUseSslTls == true -> {
        SecurityType.Option.SSL_TLS
      }

      imapUseStarttls == true -> {
        SecurityType.Option.STARTLS
      }

      else -> SecurityType.Option.NONE
    }
  }

  fun smtpOpt(): SecurityType.Option {
    return when {
      smtpUseSslTls == true -> {
        SecurityType.Option.SSL_TLS
      }

      smtpUseStarttls == true -> {
        SecurityType.Option.STARTLS
      }

      else -> SecurityType.Option.NONE
    }
  }

  fun isRuleExist(domainRule: OrgRules.DomainRule): Boolean {
    return clientConfiguration?.hasRule(domainRule) ?: false
  }

  companion object {
    const val ACCOUNT_TYPE_GOOGLE = "com.google"
    const val ACCOUNT_TYPE_OUTLOOK = "outlook.com"
  }
}
