/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.protocol.PropertiesHelper
import com.flowcrypt.email.model.results.LoaderResult
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Session

/**
 * This loader does job of valid [AuthCredentials]. If incoming [AuthCredentials] is valid then this
 * loader returns `true`, otherwise returns false.
 *
 * @author DenBond7
 * Date: 14.09.2017.
 * Time: 15:08.
 * E-mail: DenBond7@gmail.com
 */
class CheckEmailSettingsAsyncTaskLoader(context: Context,
                                        private val authCreds: AuthCredentials)
  : AsyncTaskLoader<LoaderResult>(context) {

  init {
    onContentChanged()
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  override fun loadInBackground(): LoaderResult? {
    val sess = Session.getInstance(PropertiesHelper.genProps(authCreds))
    sess.debug = EmailUtil.hasEnabledDebug(context)

    try {
      testImapConn(sess)
    } catch (e: Exception) {
      e.printStackTrace()
      val exception = Exception("IMAP: " + e.message, e)
      return LoaderResult(null, exception)
    }

    try {
      testSmtpConn(sess)
    } catch (e: Exception) {
      e.printStackTrace()
      val exception = Exception("SMTP: " + e.message, e)
      return LoaderResult(null, exception)
    }

    return LoaderResult(true, null)
  }

  public override fun onStopLoading() {
    cancelLoad()
  }

  /**
   * Trying to connect to an IMAP server. If an exception will occur than that exception will be throw up.
   *
   * @param session The [Session] which will be used for the connection.
   * @throws MessagingException This operation can throw some exception.
   */
  @Throws(MessagingException::class)
  private fun testImapConn(session: Session) {
    val store = session.getStore(JavaEmailConstants.PROTOCOL_IMAP)
    store.connect(authCreds.imapServer, authCreds.imapPort, authCreds.username, authCreds.password)
    val folder = store.getFolder(JavaEmailConstants.FOLDER_INBOX)
    folder.open(Folder.READ_ONLY)
    folder.close(false)
    store.close()
  }

  /**
   * Trying to connect to the SMTP server. If an exception will occur than that exception will be throw up.
   *
   * @param session The [Session] which will be used for the connection.
   * @throws MessagingException This operation can throw some exception.
   */
  @Throws(MessagingException::class)
  private fun testSmtpConn(session: Session) {
    val transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP)
    val username: String?
    val password: String?

    if (authCreds.hasCustomSignInForSmtp) {
      username = authCreds.smtpSigInUsername
      password = authCreds.smtpSignInPassword
    } else {
      username = authCreds.username
      password = authCreds.password
    }

    transport.connect(authCreds.smtpServer, authCreds.smtpPort, username, password)
    transport.close()
  }
}
