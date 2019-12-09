/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = BaseColumns._ID) val id: Long?,
    val email: String,
    @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String?,
    @ColumnInfo(name = "display_name", defaultValue = "NULL") val displayName: String?,
    @ColumnInfo(name = "given_name", defaultValue = "NULL") val givenName: String?,
    @ColumnInfo(name = "family_name", defaultValue = "NULL") val familyName: String?,
    @ColumnInfo(name = "photo_url", defaultValue = "NULL") val photoUrl: String?,
    @ColumnInfo(name = "is_enable", defaultValue = "1") val isEnabled: Boolean?,
    @ColumnInfo(name = "is_active", defaultValue = "0") val isActive: Boolean?,
    val username: String,
    val password: String,
    @ColumnInfo(name = "imap_server") val imapServer: String,
    @ColumnInfo(name = "imap_port", defaultValue = "143") val imapPort: Int?,
    @ColumnInfo(name = "imap_is_use_ssl_tls", defaultValue = "0") val imapIsUseSslTls: Boolean?,
    @ColumnInfo(name = "imap_is_use_starttls", defaultValue = "0") val imapIsUseStarttls: Boolean?,
    @ColumnInfo(name = "imap_auth_mechanisms") val imapAuthMechanisms: String?,
    @ColumnInfo(name = "smtp_server") val smtpServer: String,
    @ColumnInfo(name = "smtp_port", defaultValue = "25") val smtpPort: Int?,
    @ColumnInfo(name = "smtp_is_use_ssl_tls", defaultValue = "0") val smtpIsUseSslTls: Boolean?,
    @ColumnInfo(name = "smtp_is_use_starttls", defaultValue = "0") val smtpIsUseStarttls: Boolean?,
    @ColumnInfo(name = "smtp_auth_mechanisms") val smtpAuthMechanisms: String?,
    @ColumnInfo(name = "smtp_is_use_custom_sign", defaultValue = "0") val smtpIsUseCustomSign: Boolean?,
    @ColumnInfo(name = "smtp_username", defaultValue = "NULL") val smtpUsername: String?,
    @ColumnInfo(name = "smtp_password", defaultValue = "NULL") val smtpPassword: String?,
    @ColumnInfo(name = "ic_contacts_loaded", defaultValue = "0") val areContactsLoaded: Boolean?,
    @ColumnInfo(name = "is_show_only_encrypted", defaultValue = "0") val isShowOnlyEncrypted: Boolean?,
    @ColumnInfo(defaultValue = "NULL") val uuid: String?,
    @ColumnInfo(name = "domain_rules", defaultValue = "NULL") val domainRules: String?,
    @ColumnInfo(name = "is_restore_access_required", defaultValue = "0") val isRestoreAccessRequired: Boolean?)