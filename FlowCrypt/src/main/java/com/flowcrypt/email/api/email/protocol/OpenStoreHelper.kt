/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import android.accounts.AccountManager
import android.content.Context
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.GoogleAuthUtil
import com.sun.mail.gimap.GmailSSLStore
import javax.mail.AuthenticationFailedException
import javax.mail.Session
import javax.mail.Store

/**
 * This util class help generate Store classes.
 *
 * @author DenBond7
 * Date: 03.05.2017
 * Time: 18:00
 * E-mail: DenBond7@gmail.com
 */
class OpenStoreHelper {
  companion object {
    private val TAG = OpenStoreHelper::class.java.simpleName

    /**
     * Generate a session which will be use to download attachments.
     *
     * @param context Interface to global information about an application environment;
     * @param account An input [AccountEntity];
     * @return <tt>Session</tt> A new sess based on for download attachments.
     */
    fun getAttsSess(context: Context, account: AccountEntity?): Session {
      return if (account != null) {
        Session.getInstance(PropertiesHelper.genDownloadAttsProps(account)).apply {
          debug = EmailUtil.hasEnabledDebug(context)
        }
      } else
        throw NullPointerException("AccountEntity must not be a null!")
    }

    /**
     * Prepare [Session] object for the input [AccountEntity].
     *
     * @param context Interface to global information about an application environment;
     * @param account An input [AccountEntity];
     * @return A generated [Session]
     */
    fun getAccountSess(context: Context, account: AccountEntity): Session {
      return Session.getInstance(PropertiesHelper.genProps(account)).apply {
        debug = EmailUtil.hasEnabledDebug(context)
      }
    }

    fun openStore(account: AccountEntity, authCredentials: AuthCredentials, session: Session): Store {
      val store = when (account.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> session.getStore(JavaEmailConstants.PROTOCOL_GIMAPS) as GmailSSLStore

        else -> {
          when {
            account.imapOpt() === SecurityType.Option.NONE -> session.getStore(JavaEmailConstants.PROTOCOL_IMAP)
            else -> session.getStore(JavaEmailConstants.PROTOCOL_IMAPS)
          }
        }
      }

      store.connect(authCredentials.imapServer, authCredentials.imapPort, authCredentials.username, authCredentials.peekPassword())
      return store
    }

    fun openStore(context: Context, account: AccountEntity?, session: Session): Store {
      return if (account != null) {
        when (account.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> openAndConnectToGimapsStore(context, session, account, false)

          else -> {
            val store = when {
              account.imapOpt() === SecurityType.Option.NONE -> session.getStore(JavaEmailConstants.PROTOCOL_IMAP)
              else -> session.getStore(JavaEmailConstants.PROTOCOL_IMAPS)
            }

            val password = if (account.useOAuth2) {
              val accountManager = AccountManager.get(context)
              val oauthAccount = accountManager.accounts.firstOrNull { it.name == account.email }
              if (oauthAccount != null && oauthAccount.type.equals(FlowcryptAccountAuthenticator.ACCOUNT_TYPE, ignoreCase = true)) {
                val encryptedToken = accountManager.blockingGetAuthToken(oauthAccount,
                    FlowcryptAccountAuthenticator.AUTH_TOKEN_TYPE_EMAIL, true)
                if (encryptedToken.isNullOrEmpty()) {
                  ExceptionUtil.handleError(NullPointerException("Warning. Encrypted token is null!"))
                  ""
                } else {
                  KeyStoreCryptoManager.decrypt(encryptedToken)
                }
              } else {
                account.password
              }
            } else {
              account.password
            }

            store.connect(account.imapServer, account.username, password)
            store
          }
        }
      } else
        throw NullPointerException("AccountEntity must not be a null!")
    }

    private fun openAndConnectToGimapsStore(context: Context, session: Session, accountEntity: AccountEntity,
                                            isResetTokenNeeded: Boolean): GmailSSLStore {
      val gmailSSLStore: GmailSSLStore = session.getStore(JavaEmailConstants.PROTOCOL_GIMAPS) as GmailSSLStore

      try {
        var token = EmailUtil.getGmailAccountToken(context, accountEntity)

        if (isResetTokenNeeded) {
          LogsUtil.d(TAG, "Refresh Gmail token")
          GoogleAuthUtil.clearToken(context, token)
          token = EmailUtil.getGmailAccountToken(context, accountEntity)
        }

        gmailSSLStore.connect(GmailConstants.GMAIL_IMAP_SERVER, accountEntity.email, token)
      } catch (e: AuthenticationFailedException) {
        e.printStackTrace()
        return if (!isResetTokenNeeded) {
          openAndConnectToGimapsStore(context, session, accountEntity, true)
        } else {
          throw e
        }
      }

      return gmailSSLStore
    }
  }
}
