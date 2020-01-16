/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.protocol

import android.content.Context
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.util.LogsUtil
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
     * Open and connect to the store using gimaps protocol.
     *
     * @param context     Interface to global information about an application environment.
     * @param token       An OAuth2 access token;
     * @param accountName An account name which use to create connection;
     * @return <tt>GmailSSLStore</tt> A GmailSSLStore object based on properties for
     * gimaps.
     */
    @JvmStatic
    fun openAndConnectToGimapsStore(context: Context, token: String, accountName: String): GmailSSLStore {
      val gmailSSLStore = getGmailSess(context).getStore(JavaEmailConstants.PROTOCOL_GIMAPS) as GmailSSLStore
      gmailSSLStore.connect(GmailConstants.GMAIL_IMAP_SERVER, accountName, token)
      return gmailSSLStore
    }

    /**
     * Open and connect to the store using gimaps protocol.
     *
     * @param context            Interface to global information about an application environment.
     * @param session            The sess which will be used for connection.
     * @param accountDao            The object which contains information about an email accountDao.
     * @param isResetTokenNeeded True if need reset token.
     * @return <tt>GmailSSLStore</tt> A GmailSSLStore object based on properties for
     * gimaps.
     */
    @JvmStatic
    fun openAndConnectToGimapsStore(context: Context, session: Session, accountDao: AccountDao?,
                                    isResetTokenNeeded: Boolean): GmailSSLStore {
      val gmailSSLStore: GmailSSLStore = session.getStore(JavaEmailConstants.PROTOCOL_GIMAPS) as GmailSSLStore

      try {
        var token = EmailUtil.getGmailAccountToken(context, accountDao)

        if (isResetTokenNeeded) {
          LogsUtil.d(TAG, "Refresh Gmail token")
          GoogleAuthUtil.clearToken(context, token)
          token = EmailUtil.getGmailAccountToken(context, accountDao)
        }

        gmailSSLStore.connect(GmailConstants.GMAIL_IMAP_SERVER, accountDao?.email, token)
      } catch (e: AuthenticationFailedException) {
        e.printStackTrace()
        return if (!isResetTokenNeeded) {
          openAndConnectToGimapsStore(context, session, accountDao, true)
        } else {
          throw e
        }
      }

      return gmailSSLStore
    }

    /**
     * Generate a sess for gimaps protocol.
     *
     * @param context Interface to global information about an application environment;
     * @return <tt>Session</tt> A new sess for gimaps protocol based on properties for gimaps.
     */
    @JvmStatic
    fun getGmailSess(context: Context): Session {
      val session = Session.getInstance(PropertiesHelper.generateGmailProperties())
      session.debug = EmailUtil.hasEnabledDebug(context)
      return session
    }

    /**
     * Generate a sess which will be use for download attachments.
     *
     * @param context Interface to global information about an application environment;
     * @param account An input [AccountDao];
     * @return <tt>Session</tt> A new sess based on for download attachments.
     */
    @JvmStatic
    fun getAttsSess(context: Context, account: AccountDao?): Session {
      return if (account != null) {
        when (account.accountType) {
          AccountDao.ACCOUNT_TYPE_GOOGLE -> getAttGmailSess(context)

          else -> {
            val session = Session.getInstance(PropertiesHelper.genDownloadAttsProps(account.authCreds))
            session.debug = EmailUtil.hasEnabledDebug(context)
            session
          }
        }
      } else
        throw NullPointerException("AccountDao must not be a null!")
    }

    /**
     * Generate a sess for gimaps protocol which will be use for download attachments.
     *
     * @param context Interface to global information about an application environment;
     * @return <tt>Session</tt> A new sess for gimaps protocol based on properties for gimaps.
     */
    @JvmStatic
    fun getAttGmailSess(context: Context): Session {
      val session = Session.getInstance(PropertiesHelper.genGmailAttsProperties())
      session.debug = EmailUtil.hasEnabledDebug(context)
      return session
    }

    /**
     * Prepare [Session] object for the input [AccountDao].
     *
     * @param context Interface to global information about an application environment;
     * @param account An input [AccountDao];
     * @return A generated [Session]
     */
    @JvmStatic
    fun getAccountSess(context: Context, account: AccountDao?): Session {
      return if (account != null) {
        when (account.accountType) {
          AccountDao.ACCOUNT_TYPE_GOOGLE -> getGmailSess(context)

          else -> {
            val session = Session.getInstance(PropertiesHelper.genProps(account.authCreds))
            session.debug = EmailUtil.hasEnabledDebug(context)
            session
          }
        }
      } else
        throw NullPointerException("AccountDao must not be a null!")
    }

    @JvmStatic
    fun openStore(context: Context, account: AccountDao?, session: Session): Store {
      return if (account != null) {
        when (account.accountType) {
          AccountDao.ACCOUNT_TYPE_GOOGLE -> openAndConnectToGimapsStore(context, session, account, false)

          else -> {
            val authCreds = account.authCreds
                ?: throw java.lang.NullPointerException("Credentials are null!")
            val store = when {
              authCreds.imapOpt === SecurityType.Option.NONE -> session.getStore(JavaEmailConstants.PROTOCOL_IMAP)
              else -> session.getStore(JavaEmailConstants.PROTOCOL_IMAPS)
            }

            store.connect(authCreds.imapServer, authCreds.username, authCreds.password)
            store
          }
        }
      } else
        throw NullPointerException("AccountDao must not be a null!")
    }
  }
}
