/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.gmail

import android.accounts.Account
import android.content.Context
import android.util.Base64
import android.util.Base64InputStream
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.api.GMailRawMIMEMessageFilterInputStream
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.ListMessagesResponse
import com.google.api.services.gmail.model.Message
import java.io.InputStream

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

    fun getWholeMimeMessageInputStream(context: Context, account: AccountEntity?, messageEntity: MessageEntity): InputStream {
      val msgId = messageEntity.gMailId
      val gmailApiService = generateGmailApiService(context, account)

      val message = gmailApiService
          .users()
          .messages()
          .get(DEFAULT_USER_ID, msgId)
          .setFormat(MESSAGE_RESPONSE_FORMAT_RAW)
      message.fields = "raw"

      return Base64InputStream(GMailRawMIMEMessageFilterInputStream(message.executeAsInputStream()), Base64.URL_SAFE)
    }

    fun loadMsgsBaseInfo(context: Context, accountEntity: AccountEntity, localFolder: LocalFolder, countOfAlreadyLoadedMsgs: Int, nextPageToken: String? = null): ListMessagesResponse {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      return gmailApiService
          .users()
          .messages()
          .list(DEFAULT_USER_ID)
          .setLabelIds(listOf(localFolder.fullName))
          .setMaxResults(JavaEmailConstants.COUNT_OF_LOADED_EMAILS_BY_STEP.toLong())
          .execute()
    }

    fun loadMsgsShortInfo(context: Context, accountEntity: AccountEntity, list: ListMessagesResponse): List<Message> {
      val gmailApiService = generateGmailApiService(context, accountEntity)
      val batch = gmailApiService.batch()

      val listResult = mutableListOf<Message>()

      for (message in list.messages) {
        val request = gmailApiService
            .users()
            .messages()
            .get(DEFAULT_USER_ID, message.id)
            .setFormat("METADATA")
        request.queue(batch, object : JsonBatchCallback<Message>() {
          override fun onSuccess(t: Message?, responseHeaders: HttpHeaders?) {
            if (t != null) {
              listResult.add(t)
            } else throw java.lang.NullPointerException()
          }

          override fun onFailure(e: GoogleJsonError?, responseHeaders: HttpHeaders?) {
            throw IllegalStateException()
          }
        })
      }

      batch.execute()


      return listResult
    }
  }
}
