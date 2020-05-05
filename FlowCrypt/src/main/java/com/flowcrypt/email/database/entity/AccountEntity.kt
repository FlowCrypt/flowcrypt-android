/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.accounts.Account
import android.os.Parcel
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
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 12/4/19
 *         Time: 2:50 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "accounts",
    indices = [
      Index(name = "email_account_type_in_accounts", value = ["email", "account_type"], unique = true)
    ])
data class AccountEntity constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
    val email: String,
    @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String? = null,
    @ColumnInfo(name = "display_name", defaultValue = "NULL") val displayName: String? = null,
    @ColumnInfo(name = "given_name", defaultValue = "NULL") val givenName: String? = null,
    @ColumnInfo(name = "family_name", defaultValue = "NULL") val familyName: String? = null,
    @ColumnInfo(name = "photo_url", defaultValue = "NULL") val photoUrl: String? = null,
    @ColumnInfo(name = "is_enable", defaultValue = "1") val isEnabled: Boolean? = true,
    @ColumnInfo(name = "is_active", defaultValue = "0") val isActive: Boolean? = false,
    val username: String,
    val password: String,
    @ColumnInfo(name = "imap_server") val imapServer: String,
    @ColumnInfo(name = "imap_port", defaultValue = "143") val imapPort: Int? = 143,
    @ColumnInfo(name = "imap_is_use_ssl_tls", defaultValue = "0") val imapIsUseSslTls: Boolean? = false,
    @ColumnInfo(name = "imap_is_use_starttls", defaultValue = "0") val imapIsUseStarttls: Boolean? = false,
    @ColumnInfo(name = "imap_auth_mechanisms") val imapAuthMechanisms: String? = null,
    @ColumnInfo(name = "smtp_server") val smtpServer: String,
    @ColumnInfo(name = "smtp_port", defaultValue = "25") val smtpPort: Int? = 25,
    @ColumnInfo(name = "smtp_is_use_ssl_tls", defaultValue = "0") val smtpIsUseSslTls: Boolean? = false,
    @ColumnInfo(name = "smtp_is_use_starttls", defaultValue = "0") val smtpIsUseStarttls: Boolean? = false,
    @ColumnInfo(name = "smtp_auth_mechanisms") val smtpAuthMechanisms: String? = null,
    @ColumnInfo(name = "smtp_is_use_custom_sign", defaultValue = "0") val useCustomSignForSmtp: Boolean? = false,
    @ColumnInfo(name = "smtp_username", defaultValue = "NULL") val smtpUsername: String? = null,
    @ColumnInfo(name = "smtp_password", defaultValue = "NULL") val smtpPassword: String? = null,
    @ColumnInfo(name = "ic_contacts_loaded", defaultValue = "0") val areContactsLoaded: Boolean? = false,
    @ColumnInfo(name = "is_show_only_encrypted", defaultValue = "0") val isShowOnlyEncrypted: Boolean? = false,
    @ColumnInfo(defaultValue = "NULL") val uuid: String? = null,
    @ColumnInfo(name = "domain_rules", defaultValue = "NULL") val domainRules: String? = null,
    @ColumnInfo(name = "is_restore_access_required", defaultValue = "0") val isRestoreAccessRequired: Boolean? = false) : Parcelable {
  @Ignore
  val account: Account? = Account(this.email, accountType)

  constructor(googleSignInAccount: GoogleSignInAccount, uuid: String? = null,
              domainRules: List<String>? = null) :
      this(
          email = googleSignInAccount.email!!,
          accountType = googleSignInAccount.account?.type?.toLowerCase(Locale.getDefault()),
          displayName = googleSignInAccount.displayName,
          givenName = googleSignInAccount.givenName,
          familyName = googleSignInAccount.familyName,
          photoUrl = googleSignInAccount.photoUrl?.toString(),
          isEnabled = true,
          isActive = false,
          username = googleSignInAccount.email!!,
          password = "",
          imapServer = GmailConstants.GMAIL_IMAP_SERVER,
          imapPort = GmailConstants.GMAIL_IMAP_PORT,
          imapIsUseSslTls = true,
          imapIsUseStarttls = false,
          imapAuthMechanisms = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2,
          smtpServer = GmailConstants.GMAIL_SMTP_SERVER,
          smtpPort = GmailConstants.GMAIL_SMTP_PORT,
          smtpIsUseSslTls = true,
          smtpIsUseStarttls = false,
          smtpAuthMechanisms = JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2,
          useCustomSignForSmtp = false,
          smtpUsername = null,
          smtpPassword = null,
          areContactsLoaded = false,
          isShowOnlyEncrypted = false,
          uuid = uuid,
          domainRules = domainRules?.joinToString(),
          isRestoreAccessRequired = false
      )


  /*val contentValues = ContentValues()
    val email = authCreds.email
    if (!TextUtils.isEmpty(email)) {
      contentValues.put(COL_EMAIL, email.toLowerCase(Locale.getDefault()))
    } else
      return null

    contentValues.put(COL_ACCOUNT_TYPE, email.substring(email.indexOf('@') + 1))
    contentValues.put(COL_USERNAME, authCreds.username)
    contentValues.put(COL_PASSWORD, KeyStoreCryptoManager.encrypt(authCreds.password))
    contentValues.put(COL_IMAP_SERVER, authCreds.imapServer)
    contentValues.put(COL_IMAP_PORT, authCreds.imapPort)
    contentValues.put(COL_IMAP_IS_USE_SSL_TLS, authCreds.imapOpt === SecurityType.Option.SSL_TLS)
    contentValues.put(COL_IMAP_IS_USE_STARTTLS, authCreds.imapOpt === SecurityType.Option.STARTLS)
    contentValues.put(COL_SMTP_SERVER, authCreds.smtpServer)
    contentValues.put(COL_SMTP_PORT, authCreds.smtpPort)
    contentValues.put(COL_SMTP_IS_USE_SSL_TLS, authCreds.smtpOpt === SecurityType.Option.SSL_TLS)
    contentValues.put(COL_SMTP_IS_USE_STARTTLS, authCreds.smtpOpt === SecurityType.Option.STARTLS)
    contentValues.put(COL_SMTP_IS_USE_CUSTOM_SIGN, authCreds.hasCustomSignInForSmtp)
    contentValues.put(COL_SMTP_USERNAME, authCreds.smtpSigInUsername)
    contentValues.put(COL_SMTP_PASSWORD,
        authCreds.smtpSignInPassword?.let { KeyStoreCryptoManager.encrypt(it) })

    contentValues.put(COL_IS_ACTIVE, true)

    return contentValues*/
  constructor(authCredentials: AuthCredentials, uuid: String? = null, domainRules: List<String>? = null) :
      this(
          email = authCredentials.email,
          accountType = authCredentials.email.substring(authCredentials.email.indexOf('@') + 1).toLowerCase(Locale.getDefault()),
          displayName = null,
          givenName = null,
          familyName = null,
          photoUrl = null,
          isEnabled = true,
          isActive = false,
          username = authCredentials.username,
          password = authCredentials.password,
          imapServer = authCredentials.imapServer,
          imapPort = authCredentials.imapPort,
          imapIsUseSslTls = authCredentials.imapOpt === SecurityType.Option.SSL_TLS,
          imapIsUseStarttls = authCredentials.imapOpt === SecurityType.Option.STARTLS,
          imapAuthMechanisms = null,
          smtpServer = authCredentials.smtpServer,
          smtpPort = authCredentials.smtpPort,
          smtpIsUseSslTls = authCredentials.smtpOpt === SecurityType.Option.SSL_TLS,
          smtpIsUseStarttls = authCredentials.smtpOpt === SecurityType.Option.STARTLS,
          smtpAuthMechanisms = null,
          useCustomSignForSmtp = authCredentials.hasCustomSignInForSmtp,
          smtpUsername = authCredentials.smtpSigInUsername,
          smtpPassword = authCredentials.smtpSignInPassword,
          areContactsLoaded = false,
          isShowOnlyEncrypted = false,
          uuid = uuid,
          domainRules = domainRules?.joinToString(),
          isRestoreAccessRequired = false
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
          imapIsUseSslTls = true,
          imapIsUseStarttls = false,
          imapAuthMechanisms = null,
          smtpServer = "",
          smtpPort = 0,
          smtpIsUseSslTls = true,
          smtpIsUseStarttls = false,
          smtpAuthMechanisms = "",
          useCustomSignForSmtp = false,
          smtpUsername = null,
          smtpPassword = null,
          areContactsLoaded = false,
          isShowOnlyEncrypted = false,
          uuid = null,
          domainRules = null,
          isRestoreAccessRequired = false
      )

  constructor(source: Parcel) : this(
      source.readValue(Long::class.java.classLoader) as Long?,
      source.readString()!!,
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readString()!!,
      source.readString()!!,
      source.readString()!!,
      source.readValue(Int::class.java.classLoader) as Int,
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readString(),
      source.readString()!!,
      source.readValue(Int::class.java.classLoader) as Int,
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readString(),
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readString(),
      source.readString(),
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readString(),
      source.readString(),
      source.readValue(Boolean::class.java.classLoader) as Boolean?
  )

  fun domainRulesList(): List<String> {
    return if (domainRules.isNullOrEmpty()) {
      emptyList()
    } else {
      domainRules.split(",").map { it.trim() }
    }
  }

  fun imapOpt(): SecurityType.Option {
    return when {
      imapIsUseSslTls == true -> {
        SecurityType.Option.SSL_TLS
      }

      imapIsUseStarttls == true -> {
        SecurityType.Option.STARTLS
      }

      else -> SecurityType.Option.NONE
    }
  }

  fun smtpOpt(): SecurityType.Option {
    return when {
      smtpIsUseSslTls == true -> {
        SecurityType.Option.SSL_TLS
      }

      smtpIsUseStarttls == true -> {
        SecurityType.Option.STARTLS
      }

      else -> SecurityType.Option.NONE
    }
  }

  fun getAuthCredentials(): AuthCredentials {
    var imapOpt: SecurityType.Option = SecurityType.Option.NONE

    if (imapIsUseSslTls == true) {
      imapOpt = SecurityType.Option.SSL_TLS
    } else if (imapIsUseStarttls == true) {
      imapOpt = SecurityType.Option.STARTLS
    }

    var smtpOpt: SecurityType.Option = SecurityType.Option.NONE

    if (smtpIsUseSslTls == true) {
      smtpOpt = SecurityType.Option.SSL_TLS
    } else if (smtpIsUseStarttls == true) {
      smtpOpt = SecurityType.Option.STARTLS
    }

    var originalPassword = this.password

    //fixed a bug when try to decrypting the template password.
    // See https://github.com/FlowCrypt/flowcrypt-android/issues/168
    if ("password".equals(originalPassword, ignoreCase = true)) {
      originalPassword = ""
    }

    return AuthCredentials(
        email = email,
        username = username,
        password = KeyStoreCryptoManager.decrypt(originalPassword),
        imapServer = imapServer,
        imapPort = imapPort ?: JavaEmailConstants.DEFAULT_IMAP_PORT,
        imapOpt = imapOpt,
        smtpServer = smtpServer,
        smtpPort = smtpPort ?: JavaEmailConstants.DEFAULT_SMTP_PORT,
        smtpOpt = smtpOpt,
        hasCustomSignInForSmtp = true,
        smtpSigInUsername = smtpUsername,
        smtpSignInPassword = KeyStoreCryptoManager.decrypt(smtpPassword))
  }

  fun isRuleExist(domainRule: DomainRule): Boolean {
    val rules = domainRulesList()
    return domainRule.name in rules
  }

  enum class DomainRule {
    NO_PRV_CREATE,
    NO_PRV_BACKUP,
    ENFORCE_ATTESTER_SUBMIT
  }

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeValue(id)
    writeString(email)
    writeString(accountType)
    writeString(displayName)
    writeString(givenName)
    writeString(familyName)
    writeString(photoUrl)
    writeValue(isEnabled)
    writeValue(isActive)
    writeString(username)
    writeString(password)
    writeString(imapServer)
    writeValue(imapPort)
    writeValue(imapIsUseSslTls)
    writeValue(imapIsUseStarttls)
    writeString(imapAuthMechanisms)
    writeString(smtpServer)
    writeValue(smtpPort)
    writeValue(smtpIsUseSslTls)
    writeValue(smtpIsUseStarttls)
    writeString(smtpAuthMechanisms)
    writeValue(useCustomSignForSmtp)
    writeString(smtpUsername)
    writeString(smtpPassword)
    writeValue(areContactsLoaded)
    writeValue(isShowOnlyEncrypted)
    writeString(uuid)
    writeString(domainRules)
    writeValue(isRestoreAccessRequired)
  }

  companion object {
    const val ACCOUNT_TYPE_GOOGLE = "com.google"
    const val ACCOUNT_TYPE_OUTLOOK = "outlook.com"

    @JvmField
    val CREATOR: Parcelable.Creator<AccountEntity> = object : Parcelable.Creator<AccountEntity> {
      override fun createFromParcel(source: Parcel): AccountEntity = AccountEntity(source)
      override fun newArray(size: Int): Array<AccountEntity?> = arrayOfNulls(size)
    }
  }
}