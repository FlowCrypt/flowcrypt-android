/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail

import android.accounts.Account
import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes

/**
 * This class helps to work with Gmail API.
 *
 * @author Denis Bondarenko
 * Date: 30.10.2017
 * Time: 14:35
 * E-mail: DenBond7@gmail.com
 */
class GmailApiHelper {
  companion object {
    const val DEFAULT_USER_ID = "me"
    const val MESSAGE_RESPONSE_FORMAT_RAW = "raw"
    private val SCOPES = arrayOf(GmailScopes.MAIL_GOOGLE_COM)

    /**
     * Generate [Gmail] using incoming [AccountEntity]. [Gmail] class is the main point of
     * Gmail API.
     *
     * @param context   Interface to global information about an application environment.
     * @param account   The [AccountEntity] object which contains information about an account.
     * @return Generated [Gmail].
     */
    @JvmStatic
    fun generateGmailApiService(context: Context, account: AccountEntity?): Gmail {
      requireNotNull(account)
      return generateGmailApiService(context, account.account)
    }

    fun generateGmailApiService(context: Context, account: Account?): Gmail {
      requireNotNull(account)

      val credential = generateGoogleAccountCredential(context, account)

      val transport = NetHttpTransport()
      val factory = JacksonFactory.getDefaultInstance()
      val appName = context.getString(R.string.app_name)
      return Gmail.Builder(transport, factory, credential).setApplicationName(appName).build()
    }

    /**
     * Generate [GoogleAccountCredential] which will be used with Gmail API.
     *
     * @param context Interface to global information about an application environment.
     * @param account The Gmail account.
     * @return Generated [GoogleAccountCredential].
     */
    private fun generateGoogleAccountCredential(context: Context, account: Account?): GoogleAccountCredential {
      return GoogleAccountCredential.usingOAuth2(context, listOf(*SCOPES)).setSelectedAccount(account)
    }
  }
}
