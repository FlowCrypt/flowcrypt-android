/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.content.Context
import android.content.Intent
import androidx.annotation.WorkerThread
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import org.eclipse.angus.mail.gimap.GmailSSLStore
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Session
import jakarta.mail.Store

/**
 * This util class help generate Store classes.
 *
 * @author Denys Bondarenko
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

    fun openStore(
      account: AccountEntity,
      authCredentials: AuthCredentials,
      session: Session
    ): Store {
      return getStore(account, session).apply {
        connect(
          authCredentials.imapServer,
          authCredentials.imapPort,
          authCredentials.username,
          authCredentials.peekPassword()
        )
      }
    }

    fun getStore(account: AccountEntity?, session: Session): Store {
      return if (account != null) {
        when (account.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> session.getStore(JavaEmailConstants.PROTOCOL_GIMAPS) as GmailSSLStore

          else -> when {
            account.imapOpt() === SecurityType.Option.NONE -> session.getStore(JavaEmailConstants.PROTOCOL_IMAP)
            else -> session.getStore(JavaEmailConstants.PROTOCOL_IMAPS)
          }
        }
      } else throw NullPointerException("AccountEntity must not be a null!")
    }

    fun openStore(context: Context, accountEntity: AccountEntity, store: Store) {
      when (accountEntity.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          connectToGimapsStore(context, accountEntity, false, store)
        }

        else -> {
          try {
            val password = if (accountEntity.useOAuth2) {
              val accountManager = AccountManager.get(context)
              val oauthAccount =
                accountManager.accounts.firstOrNull { it.name == accountEntity.email }
              if (oauthAccount != null && oauthAccount.type.equals(
                  FlowcryptAccountAuthenticator.ACCOUNT_TYPE,
                  ignoreCase = true
                )
              ) {
                val encryptedToken = accountManager.blockingGetAuthToken(
                  oauthAccount,
                  FlowcryptAccountAuthenticator.AUTH_TOKEN_TYPE_EMAIL, true
                )
                if (encryptedToken.isNullOrEmpty()) {
                  ExceptionUtil.handleError(NullPointerException("Warning. Encrypted token is null!"))
                  ""
                } else {
                  KeyStoreCryptoManager.decrypt(encryptedToken)
                }
              } else {
                accountEntity.password
              }
            } else {
              accountEntity.password
            }

            store.connect(accountEntity.imapServer, accountEntity.username, password)
          } catch (e: Exception) {
            handleConnectException(e, context, accountEntity, store)
          }
        }
      }
    }

    @WorkerThread
    fun openStore(context: Context, account: AccountEntity?, session: Session): Store {
      return if (account != null) {
        when (account.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> openAndConnectToGimapsStore(
            context,
            session,
            account
          )

          else -> {
            val store = when {
              account.imapOpt() === SecurityType.Option.NONE -> session.getStore(JavaEmailConstants.PROTOCOL_IMAP)
              else -> session.getStore(JavaEmailConstants.PROTOCOL_IMAPS)
            }
            openStore(context, account, store)
            store
          }
        }
      } else
        throw NullPointerException("AccountEntity must not be a null!")
    }

    private fun openAndConnectToGimapsStore(
      context: Context,
      session: Session,
      accountEntity: AccountEntity
    ): GmailSSLStore {
      val gmailSSLStore: GmailSSLStore =
        session.getStore(JavaEmailConstants.PROTOCOL_GIMAPS) as GmailSSLStore
      connectToGimapsStore(context, accountEntity, false, gmailSSLStore)
      return gmailSSLStore
    }

    private fun connectToGimapsStore(
      context: Context,
      accountEntity: AccountEntity,
      isResetTokenNeeded: Boolean,
      store: Store
    ) {
      try {
        var token = EmailUtil.getGmailAccountToken(context, accountEntity)

        if (isResetTokenNeeded) {
          LogsUtil.d(TAG, "Refresh Gmail token")
          GoogleAuthUtil.clearToken(context, token)
          token = EmailUtil.getGmailAccountToken(context, accountEntity)
        }

        store.connect(accountEntity.imapServer, accountEntity.email, token)
      } catch (e: Exception) {
        e.printStackTrace()
        if (isAuthException(e)) {
          val recoverableIntent: Intent? = when (e) {
            is UserRecoverableAuthException -> e.intent
            is UserRecoverableAuthIOException -> e.intent
            else -> null
          }

          if (!isResetTokenNeeded) {
            connectToGimapsStore(context, accountEntity, true, store)
          } else {
            ErrorNotificationManager(context).notifyUserAboutAuthFailure(
              accountEntity,
              recoverableIntent
            )
            throw e
          }
        } else throw e
      }
    }

    private fun isAuthException(e: Exception) =
      e is AuthenticationFailedException
          || e is UserRecoverableAuthException
          || e is UserRecoverableAuthIOException
          || e is AuthenticatorException

    private fun handleConnectException(
      e: Exception,
      context: Context,
      accountEntity: AccountEntity,
      store: Store
    ) {
      if (isAuthException(e)) {
        val activeAccount =
          FlowCryptRoomDatabase.getDatabase(context).accountDao().getActiveAccount()
        activeAccount?.let {
          if (activeAccount.id == accountEntity.id) {
            if (accountEntity.useOAuth2) {
              ErrorNotificationManager(context).notifyUserAboutAuthFailure(accountEntity)
              e.printStackTrace()
              throw e
            } else {
              try {
                val refreshedPassword = KeyStoreCryptoManager.decrypt(it.password)
                store.connect(accountEntity.imapServer, accountEntity.username, refreshedPassword)
              } catch (e: Exception) {
                if (isAuthException(e)) {
                  ErrorNotificationManager(context).notifyUserAboutAuthFailure(accountEntity)
                  e.printStackTrace()
                  throw e
                } else throw e
              }
            }
          }
        }
      } else throw e
    }
  }
}
