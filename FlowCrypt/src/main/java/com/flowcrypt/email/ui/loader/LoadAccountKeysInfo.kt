/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailsResponse
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.io.IOException
import java.util.*

/**
 * This loader does job of receiving information about an array of public
 * keys from "https://flowcrypt.com/attester/lookup/email".
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:13
 * E-mail: DenBond7@gmail.com
 */

class LoadAccountKeysInfo(context: Context,
                          private val account: AccountDao?) : AsyncTaskLoader<LoaderResult>(context) {

  init {
    onContentChanged()
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  override fun loadInBackground(): LoaderResult? {
    if (account != null) {
      val emails = ArrayList<String>()
      return try {
        when (account.accountType) {
          AccountDao.ACCOUNT_TYPE_GOOGLE -> emails.addAll(getAvailableGmailAliases(account))

          else -> emails.add(account.email)
        }

        LoaderResult(getLookUpEmailsResponse(emails), null)
      } catch (e: IOException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        LoaderResult(null, e)
      }

    } else {
      return LoaderResult(null, NullPointerException("AccountDao is null!"))
    }
  }

  public override fun onStopLoading() {
    cancelLoad()
  }

  /**
   * Get available Gmail aliases for an input [AccountDao].
   *
   * @param account The [AccountDao] object which contains information about an email account.
   * @return The list of available Gmail aliases.
   */
  private fun getAvailableGmailAliases(account: AccountDao): Collection<String> {
    val emails = ArrayList<String>()
    emails.add(account.email)

    try {
      val gmail = GmailApiHelper.generateGmailApiService(context, account)
      val aliases = gmail.users().settings().sendAs().list(GmailApiHelper.DEFAULT_USER_ID).execute()
      for (alias in aliases.sendAs) {
        if (alias.verificationStatus != null) {
          emails.add(alias.sendAsEmail)
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }

    return emails
  }

  /**
   * Get [LookUpEmailsResponse] object which contain a remote information about
   * [PgpContact].
   *
   * @param emails Used to generate a request to the server.
   * @return [LookUpEmailsResponse]
   * @throws IOException
   */
  @Throws(IOException::class)
  private fun getLookUpEmailsResponse(emails: List<String>): List<LookUpEmailResponse>? {
    val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
    val response = apiService.postLookUpEmails(PostLookUpEmailsModel(emails)).execute()
    val lookUpEmailsResponse = response.body()

    return if (lookUpEmailsResponse != null) {
      lookUpEmailsResponse.results
    } else ArrayList()
  }
}
