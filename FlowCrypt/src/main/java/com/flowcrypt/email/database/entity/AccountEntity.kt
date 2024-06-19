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
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Entity(
  tableName = AccountEntity.TABLE_NAME,
  indices = [
    Index(name = "email_account_type_in_accounts", value = ["email", "account_type"], unique = true)
  ]
)
@Parcelize
data class AccountEntity(
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
  ) val clientConfiguration: ClientConfiguration? = null,
  @ColumnInfo(name = "use_api", defaultValue = "0") val useAPI: Boolean = false,
  @ColumnInfo(
    name = "use_customer_fes_url",
    defaultValue = "0"
  ) val useCustomerFesUrl: Boolean = false,
  @ColumnInfo(name = "service_pgp_passphrase") val servicePgpPassphrase: String,
  @ColumnInfo(name = "service_pgp_private_key") val servicePgpPrivateKey: ByteArray,
  @ColumnInfo(name = "signature", defaultValue = "NULL") val signature: String? = null,
  @ColumnInfo(name = "use_alias_signatures", defaultValue = "0") val useAliasSignatures: Boolean = false,
) : Parcelable {

  @IgnoredOnParcel
  @Ignore
  val account: Account = Account(this.email.ifEmpty { "unknown" },
    accountType ?: this.email.substring(this.email.indexOf('@') + 1).lowercase()
      .ifEmpty { "unknown" })

  @IgnoredOnParcel
  @Ignore
  val isGoogleSignInAccount: Boolean = ACCOUNT_TYPE_GOOGLE.equals(accountType, ignoreCase = true)

  @Ignore
  @IgnoredOnParcel
  val avatarResource: Any = photoUrl ?: R.drawable.ic_account_default_photo

  val useOAuth2: Boolean
    get() = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2 == imapAuthMechanisms

  constructor(
    googleSignInAccount: GoogleSignInAccount,
    clientConfiguration: ClientConfiguration? = null,
    useCustomerFesUrl: Boolean,
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
    clientConfiguration = clientConfiguration,
    useAPI = true,
    useCustomerFesUrl = useCustomerFesUrl,
    servicePgpPassphrase = "",
    servicePgpPrivateKey = byteArrayOf(),
    useAliasSignatures = true
  )

  constructor(authCredentials: AuthCredentials, clientConfiguration: ClientConfiguration? = null) :
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
        clientConfiguration = clientConfiguration,
        useAPI = false,
        useCustomerFesUrl = false,
        servicePgpPassphrase = "",
        servicePgpPrivateKey = byteArrayOf()
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
        useAPI = false,
        servicePgpPassphrase = "",
        servicePgpPrivateKey = byteArrayOf()
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

  fun hasClientConfigurationProperty(configurationProperty: ClientConfiguration.ConfigurationProperty): Boolean {
    return clientConfiguration?.hasProperty(configurationProperty) ?: false
  }

  fun isHandlingAttachmentRestricted(): Boolean {
    return hasClientConfigurationProperty(
      ClientConfiguration.ConfigurationProperty.RESTRICT_ANDROID_ATTACHMENT_HANDLING
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AccountEntity

    if (id != other.id) return false
    if (email != other.email) return false
    if (accountType != other.accountType) return false
    if (displayName != other.displayName) return false
    if (givenName != other.givenName) return false
    if (familyName != other.familyName) return false
    if (photoUrl != other.photoUrl) return false
    if (isEnabled != other.isEnabled) return false
    if (isActive != other.isActive) return false
    if (username != other.username) return false
    if (password != other.password) return false
    if (imapServer != other.imapServer) return false
    if (imapPort != other.imapPort) return false
    if (imapUseSslTls != other.imapUseSslTls) return false
    if (imapUseStarttls != other.imapUseStarttls) return false
    if (imapAuthMechanisms != other.imapAuthMechanisms) return false
    if (smtpServer != other.smtpServer) return false
    if (smtpPort != other.smtpPort) return false
    if (smtpUseSslTls != other.smtpUseSslTls) return false
    if (smtpUseStarttls != other.smtpUseStarttls) return false
    if (smtpAuthMechanisms != other.smtpAuthMechanisms) return false
    if (smtpUseCustomSign != other.smtpUseCustomSign) return false
    if (smtpUsername != other.smtpUsername) return false
    if (smtpPassword != other.smtpPassword) return false
    if (contactsLoaded != other.contactsLoaded) return false
    if (showOnlyEncrypted != other.showOnlyEncrypted) return false
    if (clientConfiguration != other.clientConfiguration) return false
    if (useAPI != other.useAPI) return false
    if (useCustomerFesUrl != other.useCustomerFesUrl) return false
    if (servicePgpPassphrase != other.servicePgpPassphrase) return false
    if (!servicePgpPrivateKey.contentEquals(other.servicePgpPrivateKey)) return false
    if (account != other.account) return false
    if (isGoogleSignInAccount != other.isGoogleSignInAccount) return false
    return avatarResource == other.avatarResource
  }

  override fun hashCode(): Int {
    var result = id?.hashCode() ?: 0
    result = 31 * result + email.hashCode()
    result = 31 * result + (accountType?.hashCode() ?: 0)
    result = 31 * result + (displayName?.hashCode() ?: 0)
    result = 31 * result + (givenName?.hashCode() ?: 0)
    result = 31 * result + (familyName?.hashCode() ?: 0)
    result = 31 * result + (photoUrl?.hashCode() ?: 0)
    result = 31 * result + (isEnabled?.hashCode() ?: 0)
    result = 31 * result + (isActive?.hashCode() ?: 0)
    result = 31 * result + username.hashCode()
    result = 31 * result + password.hashCode()
    result = 31 * result + imapServer.hashCode()
    result = 31 * result + (imapPort ?: 0)
    result = 31 * result + (imapUseSslTls?.hashCode() ?: 0)
    result = 31 * result + (imapUseStarttls?.hashCode() ?: 0)
    result = 31 * result + (imapAuthMechanisms?.hashCode() ?: 0)
    result = 31 * result + smtpServer.hashCode()
    result = 31 * result + (smtpPort ?: 0)
    result = 31 * result + (smtpUseSslTls?.hashCode() ?: 0)
    result = 31 * result + (smtpUseStarttls?.hashCode() ?: 0)
    result = 31 * result + (smtpAuthMechanisms?.hashCode() ?: 0)
    result = 31 * result + (smtpUseCustomSign?.hashCode() ?: 0)
    result = 31 * result + (smtpUsername?.hashCode() ?: 0)
    result = 31 * result + (smtpPassword?.hashCode() ?: 0)
    result = 31 * result + (contactsLoaded?.hashCode() ?: 0)
    result = 31 * result + (showOnlyEncrypted?.hashCode() ?: 0)
    result = 31 * result + (clientConfiguration?.hashCode() ?: 0)
    result = 31 * result + useAPI.hashCode()
    result = 31 * result + useCustomerFesUrl.hashCode()
    result = 31 * result + servicePgpPassphrase.hashCode()
    result = 31 * result + servicePgpPrivateKey.contentHashCode()
    result = 31 * result + account.hashCode()
    result = 31 * result + isGoogleSignInAccount.hashCode()
    result = 31 * result + avatarResource.hashCode()
    return result
  }

  suspend fun withDecryptedInfo(): AccountEntity =
    withContext(Dispatchers.IO) {
      return@withContext copy(
        password = KeyStoreCryptoManager.decryptSuspend(password),
        smtpPassword = KeyStoreCryptoManager.decryptSuspend(smtpPassword),
        servicePgpPassphrase = KeyStoreCryptoManager.decryptSuspend(servicePgpPassphrase),
        servicePgpPrivateKey = KeyStoreCryptoManager.decryptSuspend(servicePgpPrivateKey)
      )
    }

  fun toAccountSettingsEntity(): AccountSettingsEntity = AccountSettingsEntity(
    account = email,
    accountType = accountType
  )

  companion object {
    const val TABLE_NAME = "accounts"
    const val ACCOUNT_TYPE_GOOGLE = "com.google"
    const val ACCOUNT_TYPE_OUTLOOK = "outlook.com"
  }
}
